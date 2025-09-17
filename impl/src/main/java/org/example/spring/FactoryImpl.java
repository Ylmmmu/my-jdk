package org.example.spring;


import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang3.StringUtils;
import org.example.collection.MyHashMap;
import org.example.spring.mvc.MethodHandler;
import org.example.spring.mvc.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class FactoryImpl implements Factory {
    Map<String,Class<?>> map = new HashMap<>();
    Map<String,Object> beanMap = new HashMap<>();
    List<PostProcessor> postProcessors = new ArrayList<>();

    private static File createTempDir(String prefix) {
        try {
            File tempDir = File.createTempFile(prefix + ".", "." + getPort());
            tempDir.delete();
            tempDir.mkdir();
            tempDir.deleteOnExit();
            return tempDir;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static int getPort() {
        return 8989;
    }



    @Override
    public void init(String packageName) throws Exception{
        String replace = packageName.replace(".", File.separator);
        URL resource = this.getClass().getClassLoader().getResource(replace);
        Path path = Paths.get(resource.toURI());
        Files.walkFileTree(path,new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String url = file.toString().replace(File.separator, ".");
                if(url.endsWith(".class")){
                    String name = url.substring(url.indexOf(packageName), url.lastIndexOf(".class"));
                    Class<?> aClass;
                    try {
                        aClass = Class.forName(name);
                        if(aClass.isAnnotationPresent(Component.class)){
                            Component annotation = aClass.getAnnotation(Component.class);
                            String beanName = annotation.name();
                            if(StringUtils.isBlank(beanName)){
                                beanName = name.substring(name.lastIndexOf(".")+1);
                            }
                            map.put(beanName,aClass);
                            System.out.println(file);
                        }



                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                }
                return FileVisitResult.CONTINUE;
            }
        });

        for (Map.Entry<String,Class<?>> entry : map.entrySet()){
            if (PostProcessor.class.isAssignableFrom(entry.getValue())) {
                Object bean = this.createBean(entry.getKey());
                postProcessors.add((PostProcessor) bean);
            }
        }


        for(Map.Entry<String,Class<?>> entry : map.entrySet()){
            this.createBean(entry.getKey());
        }

        int port = 8080;
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        tomcat.addServlet(contextPath, "dispatcherServlet", new HttpServlet(){

            @Override
            public void init() throws ServletException {
                FactoryImpl.this.map.forEach(
                        (name,clazz) -> {
                            if(clazz.isAnnotationPresent(RequestMapping.class)){
                                String mainPath =  clazz.getAnnotation(RequestMapping.class).path();
                                for (Method method : clazz.getDeclaredMethods()) {
                                    if(method.isAnnotationPresent(RequestMapping.class)){
                                        String path = mainPath + method.getAnnotation(RequestMapping.class).path();
                                        route.put(path,new MethodHandler(name,method,path));
                                    }
                                }
                            }
                        }
                );
            }


            Map<String, MethodHandler> route = new MyHashMap<>();

            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                String path = req.getPathInfo();

                MethodHandler methodHandler = route.get(path);
                if(methodHandler.getTarget() instanceof String){
                    methodHandler.setTarget(FactoryImpl.this.getBean((String) methodHandler.getTarget()));
                }

                if (methodHandler != null) {
                    try {
                        // 调用控制器方法并返回结果
                        String view = methodHandler.handleRequest(req, resp);

                    } catch (Exception e) {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "404 Not Found");
                }
            }
        });
        context.addServletMappingDecoded("/*", "dispatcherServlet");
        tomcat.start();
        tomcat.getServer().await();
    }

    private Object createBean(String key) {
        if (beanMap.containsKey(key)) {
            return beanMap.get(key);
        }
        try {
            Class<?> value = map.get(key);

            Constructor<?> constructor = value.getConstructor(null);
            Object o = constructor.newInstance();
            // beanInfo 里的一个标识 判断是否需要代理
            boolean flag = true;
            // beanInfo 里的信息 有哪些切面信息
            Object o1 = o;
            if (flag){
                o1 = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), value.getInterfaces(),new MyIvocationHandler(o));
            }
            beanMap.put(key,o1);


            Field[] fields = value.getDeclaredFields();
            List<Field> autowiredField = Arrays.stream(fields).filter(field -> field.isAnnotationPresent(Autowire.class)).collect(Collectors.toList());
            for (Field field : autowiredField) {
                field.setAccessible(true);
                field.set(o, this.getBean(field.getType()));
            }


            for(PostProcessor postProcessor : postProcessors){
                o = postProcessor.postProcessBeforeInitialization(o);
            }


            Optional<Method> first = Arrays.stream(value.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(PostConstruct.class)).findFirst();
            if(first.isPresent()){
                first.get().setAccessible(true);
                first.get().invoke(o);
            }

            for(PostProcessor postProcessor : postProcessors){
                o = postProcessor.postProcessAfterInitialization(o);
            }


            return o1;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    class MyIvocationHandler implements InvocationHandler {
        public Object target;
        public MyIvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("toString")){

                return proxy.getClass().getName();
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        }
    }



    @Override
    public Object getBean(String name)  {
        Object o = beanMap.get(name);
        if(o != null){
            return o;
        }
        return this.createBean(name);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        return map.entrySet().stream()
                .filter(entry -> (clazz.isAssignableFrom(entry.getValue())))
                .map(Map.Entry::getKey)
                .map(this::getBean)
                .map(bean -> (T)bean)
                .findFirst().orElse(null);
    }

    @Override
    public <T> List<T> getBeans(Class<T> clazz) {
        return map.entrySet().stream()
                .filter(entry -> (clazz.isAssignableFrom(entry.getValue())))
                .map(Map.Entry::getKey)
                .map(this::getBean)
                .map(bean -> (T)bean)
                .collect(Collectors.toList());
    }
}

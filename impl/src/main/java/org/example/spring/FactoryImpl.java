package org.example.spring;




import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang3.StringUtils;
import org.example.collection.MyArrayList;
import org.example.collection.MyHashMap;
import org.example.spring.mvc.Controller;
import org.example.spring.mvc.MethodHandler;
import org.example.spring.mvc.RequestMapping;

public class FactoryImpl implements Factory {
    Map<String,Class<?>> map = new MyHashMap<>();
    Map<String,Object> beanMap = new MyHashMap<>();
    List<PostProcessor> postProcessors = new MyArrayList<>();
    Map<String,MethodHandler> route = new MyHashMap<>();


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


                        if(aClass.isAnnotationPresent(Controller.class)){
                            Controller annotation = aClass.getAnnotation(Controller.class);
                            String path = annotation.path();
                            for(Method method : aClass.getMethods()){
                                if(method.isAnnotationPresent(RequestMapping.class)){
                                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                                    String methodPath = requestMapping.path();
                                    String fullPath = path + methodPath;
                                    route.put(fullPath,new MethodHandler(aClass,method));
                                }
                            }
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

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8888);
        tomcat.addServlet("/", "dispatcher", new HttpServlet(){
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                String path = req.getPathInfo();

                MethodHandler methodHandler = route.get(path);

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

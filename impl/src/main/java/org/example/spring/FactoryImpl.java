package org.example.spring;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.AnnotationUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

public class FactoryImpl implements Factory {
    Map<String,Class<?>> map = new HashMap<>();
    Map<String,Object> beanMap = new HashMap<>();
    List<PostProcessor> postProcessors = new ArrayList<>();

    private static Object instance(Class<?> value) throws Exception {
        Constructor<?> constructor = value.getConstructor();
        Object o = constructor.newInstance();
        return o;
    }

    @Override
    public void init(String packageName) throws Exception{
        scanPkg(packageName);
        initBeanPostProcessor();
        initRemainingBean();
    }

    private void initRemainingBean() {
        map.forEach((key, value) -> this.createBean(key));
    }

    private void initBeanPostProcessor() {
        map.entrySet().stream().filter(entry -> PostProcessor.class.isAssignableFrom(entry.getValue()))
                .map(entry -> this.createBean(entry.getKey())).map(PostProcessor.class::cast)
                .forEach(postProcessors::add);
    }

    private void scanPkg(String packageName) throws URISyntaxException, IOException {
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
                        if(aClass.isAnnotationPresent(Component.class) &&  !(java.lang.reflect.Modifier.isAbstract(aClass.getModifiers()) || aClass.isInterface())){
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
    }

    private Object createBean(String key) {
        if (beanMap.containsKey(key)) {
            return beanMap.get(key);
        }
        try {
            Class<?> value = map.get(key);
            Object o = instance(value);
            Object o1 = wrapIfNecessary(o, value);
            beanMap.put(key, o1);
            populateBean(value, o);
            initBean(o, value);
            return o1;
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }

    private Object wrapIfNecessary(Object o, Class<?> value) {
        boolean flag = false;
        Object o1 = o;
        if (flag) {
            o1 = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), value.getInterfaces(),
                    new MyIvocationHandler(o));
        }
        return o1;
    }

    private void populateBean(Class<?> value, Object o) throws IllegalAccessException {
        Field[] fields = value.getDeclaredFields();
        List<Field> autowiredField = Arrays.stream(fields).filter(field -> field.isAnnotationPresent(Autowired.class))
                .collect(Collectors.toList());
        for (Field field : autowiredField) {
            field.setAccessible(true);
            field.set(o, this.getBean(field.getType()));
        }
    }

    private Object initBean(Object o, Class<?> value) throws IllegalAccessException, InvocationTargetException {
        for (PostProcessor postProcessor : postProcessors) {
            o = postProcessor.postProcessBeforeInitialization(o);
        }
        Optional<Method> first =
                Arrays.stream(value.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(PostConstruct.class))
                        .findFirst();
        if (first.isPresent()) {
            first.get().setAccessible(true);
            first.get().invoke(o);
        }
        for (PostProcessor postProcessor : postProcessors) {
            o = postProcessor.postProcessAfterInitialization(o);
        }
        return o;
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

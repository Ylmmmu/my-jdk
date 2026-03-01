package org.example.spring;


import java.util.List;

public interface Factory {


    void init(String packageName) throws Exception;

    Object getBean(String name);

    <T> T getBean(Class<T> clazz);

    <T> List<T> getBeans(Class<T> clazz);
}

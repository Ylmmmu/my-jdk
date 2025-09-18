package org.example.spring;

public interface PostProcessor {
    default Object postProcessBeforeInitialization(Object bean){
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean){
        return bean;
    }
}

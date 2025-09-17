package org.example.spring;

public interface PostProcessor {
    Object postProcessBeforeInitialization(Object bean);

    Object postProcessAfterInitialization(Object bean);
}

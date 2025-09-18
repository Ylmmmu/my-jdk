package org.example.spring.mvc;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.PARAMETER)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface RequestParam  {
    String value() default "";

    boolean required() default true;
}

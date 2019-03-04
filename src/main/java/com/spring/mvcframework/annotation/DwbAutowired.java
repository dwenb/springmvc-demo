package com.spring.mvcframework.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DwbAutowired {
    String value() default "";
}

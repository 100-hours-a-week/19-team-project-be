package org.refit.refitbackend.global.swagger.annotation;

import org.refit.refitbackend.global.error.ExceptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiUnauthorizedError {
    String responseCode() default "401";
    String description() default "unauthorized";
    ExceptionType[] types() default {};
}

package org.refit.refitbackend.global.swagger.annotation;

import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ErrorResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SwaggerApiErrors.class)
public @interface SwaggerApiError {
    String responseCode();
    String description() default "error";
    Class<?> implementation() default ErrorResponse.class;
    ExceptionType[] types() default {};
}

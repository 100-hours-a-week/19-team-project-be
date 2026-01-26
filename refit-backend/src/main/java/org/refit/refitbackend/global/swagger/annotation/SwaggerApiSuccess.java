package org.refit.refitbackend.global.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiSuccess {
    String summary() default "";
    Class<?> implementation();
    String responseCode() default "200";
    String responseDescription() default "success";
    String operationDescription() default "";
    boolean wrapApiResponse() default true;
}

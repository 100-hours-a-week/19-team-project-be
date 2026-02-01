package org.refit.refitbackend.global.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiRequestBody {
    Class<?> implementation();
    boolean required() default true;
    String description() default "";
    String[] examples() default {};
    String[] exampleNames() default {};
}

package org.refit.refitbackend.global.error;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ExceptionType exceptionType;
    private final Object data;

    public CustomException(ExceptionType exceptionType) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
        this.data = null;
    }

    public CustomException(ExceptionType exceptionType, Object data) {
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
        this.data = data;
    }
}

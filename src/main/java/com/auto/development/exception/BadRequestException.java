package com.auto.development.exception;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自定义异常
 * @date 2018-12-17 14:47
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

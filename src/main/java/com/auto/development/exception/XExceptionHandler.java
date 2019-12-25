package com.auto.development.exception;

import com.baomidou.mybatisplus.extension.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Luchaoxin
 * @version V1.0
 * @Description: 异常处理类, 统一处理所有异常
 * @date 2018-08-11 15:43
 */
@Slf4j
@ControllerAdvice
public class XExceptionHandler {

    /**
     * 处理所有内部抛出的异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    // HttpStatus is 500
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    private R exceptionHandler(Exception ex) {
        Throwable throwable = getThrowable(ex);
        log.error("Unified exception handler caught exception." , throwable);
        return R.failed(throwable.getMessage());
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    // HttpStatus is 404
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    private R methodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException ex) {
        Throwable throwable = getThrowable(ex);
        log.error("Exception" , throwable);
        return R.failed(throwable.getMessage());
    }

    /**
     * 统一异常处理
     *
     * @param ex exception
     * @return
     */
    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ResponseEntity<R> processException(RuntimeException ex) {
        Throwable throwable = getThrowable(ex);
        log.error("自定义异常处理-RuntimeException" , throwable);
        R result = R.failed(throwable.getMessage());
        ResponseEntity<R> entity = new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        return entity;
    }

    /**
     * 处理自定义异常-没有访问权限
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = PermissionException.class)
    //HttpStatus is 401
    public ResponseEntity<R> permissionExceptionHandler(PermissionException ex) {
        Throwable throwable = getThrowable(ex);
        log.error("PermissionException" , throwable);
        R result = R.failed(throwable.getMessage());
        ResponseEntity<R> responseEntity = new ResponseEntity<>(result, ex.getHttpStatus());
        return responseEntity;
    }

    @ExceptionHandler(value = BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<R> badRequestExceptionHandler(BadRequestException ex) {
        return getResponseEntity(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R myErrorHandler(BindException ex) {
        StringBuilder message = new StringBuilder();
        for (FieldError fieldError : ex.getFieldErrors()) {
            String field = fieldError.getField();
            String errMsg = fieldError.getDefaultMessage();
            message.append("Field[").append(field).append("]wrong parameter value :").append(errMsg).append("  ");
        }
        log.error("参数绑定错误:" + message.toString());
        return R.failed(message.toString());
    }

    private Throwable getThrowable(Throwable ex) {
        Throwable throwable = ex.getCause();
        if (throwable == null) {
            throwable = ex;
        }
        return throwable;
    }

    private ResponseEntity<R> getResponseEntity(Exception ex, HttpStatus httpStatus) {
        Throwable throwable = getThrowable(ex);
        log.error("BadRequestException" , throwable);
        R result = R.failed(throwable.getMessage());
        ResponseEntity<R> responseEntity = new ResponseEntity<>(result, httpStatus);
        return responseEntity;
    }
}

package com.auto.development.exception;

import com.xin.utils.AssertUtil;
import org.springframework.http.HttpStatus;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自定义权限认证异常类
 * @date 2018-12-15 21:46
 */
public class PermissionException extends Exception {

    private HttpStatus httpStatus;

    private int status;

    public PermissionException(String message, HttpStatus httpStatus, int status) {
        super(message);
        AssertUtil.checkNotNull(httpStatus, "HttpStatus must not be null.");
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public PermissionException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        AssertUtil.checkNotNull(httpStatus, "HttpStatus must not be null.");
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

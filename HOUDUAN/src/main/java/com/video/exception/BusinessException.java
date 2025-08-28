package com.video.exception;

/**
 * 业务异常类
 *
 * @author video-parsing
 * @since 1.0.0
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 创建业务异常
     *
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    /**
     * 创建业务异常
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException of(String code, String message) {
        return new BusinessException(code, message);
    }

    /**
     * 创建业务异常
     *
     * @param message 错误信息
     * @param cause 原因
     * @return 业务异常
     */
    public static BusinessException of(String message, Throwable cause) {
        return new BusinessException(message, cause);
    }

    /**
     * 创建业务异常
     *
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原因
     * @return 业务异常
     */
    public static BusinessException of(String code, String message, Throwable cause) {
        return new BusinessException(code, message, cause);
    }
}
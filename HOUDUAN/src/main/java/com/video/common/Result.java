package com.video.common;

import lombok.Data;

/**
 * 通用响应结果类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Data
public class Result<T> {

    /**
     * 状态码
     */
    private Integer status;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer status, String message) {
        this();
        this.status = status;
        this.message = message;
    }

    public Result(Integer status, String message, T data) {
        this(status, message);
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败");
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    public static <T> Result<T> error(Integer status, String message) {
        return new Result<>(status, message);
    }

    /**
     * 自定义响应
     */
    public static <T> Result<T> build(Integer status, String message, T data) {
        return new Result<>(status, message, data);
    }
}
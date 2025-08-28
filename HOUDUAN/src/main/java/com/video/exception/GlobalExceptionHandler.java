package com.video.exception;

import com.video.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("参数校验失败，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        
        StringBuilder errorMsg = new StringBuilder("参数校验失败：");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("; ");
        }
        
        return Result.error(errorMsg.toString());
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("参数绑定失败，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        
        StringBuilder errorMsg = new StringBuilder("参数绑定失败：");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append(" ").append(fieldError.getDefaultMessage()).append("; ");
        }
        
        return Result.error(errorMsg.toString());
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("约束违反，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        
        StringBuilder errorMsg = new StringBuilder("参数校验失败：");
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            errorMsg.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
        }
        
        return Result.error(errorMsg.toString());
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数，请求路径：{}，参数名：{}", request.getRequestURI(), e.getParameterName());
        return Result.error("缺少必需参数：" + e.getParameterName());
    }

    /**
     * 处理方法参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配，请求路径：{}，参数名：{}，期望类型：{}", 
                request.getRequestURI(), e.getName(), e.getRequiredType());
        return Result.error("参数类型错误：" + e.getName());
    }

    /**
     * 处理REST客户端异常
     */
    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Object> handleRestClientException(RestClientException e, HttpServletRequest request) {
        log.error("外部服务调用失败，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
        
        if (e instanceof ResourceAccessException) {
            return Result.error("外部服务连接超时，请稍后重试");
        }
        
        return Result.error("外部服务调用失败，请稍后重试");
    }

    /**
     * 处理客户端断开连接异常
     */
    @ExceptionHandler(ClientAbortException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public void handleClientAbortException(ClientAbortException e, HttpServletRequest request) {
        // 客户端主动断开连接，这是正常现象，记录为INFO级别，不返回响应
        log.info("客户端断开连接，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        // 不返回Result，因为客户端已经断开连接
    }

    /**
     * 处理Socket连接异常
     */
    @ExceptionHandler(SocketException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public void handleSocketException(SocketException e, HttpServletRequest request) {
        // 网络连接异常，通常是客户端断开连接
        if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
            log.info("连接被重置，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        } else {
            log.warn("Socket连接异常，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        }
        // 不返回Result，因为连接可能已经断开
    }

    /**
     * 处理IO异常（包括大文件传输中断）
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public void handleIOException(IOException e, HttpServletRequest request) {
        // IO异常，可能是大文件传输中断
        if (e.getMessage() != null && (e.getMessage().contains("你的主机中的软件中止了一个已建立的连接") 
                || e.getMessage().contains("Connection aborted")
                || e.getMessage().contains("Broken pipe"))) {
            log.info("传输中断，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        } else {
            log.error("IO异常，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
        }
        // 不返回Result，因为连接可能已经断开
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常，请求路径：{}，错误码：{}，错误信息：{}", 
                request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统异常，请稍后重试");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统异常，请联系管理员");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
        return Result.error("参数错误：" + e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常，请求路径：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统异常，请稍后重试");
    }
}
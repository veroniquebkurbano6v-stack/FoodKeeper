package com.food.common.exception;

import com.food.common.result.ErrorCode;
import com.food.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e      业务异常
     * @param request HTTP请求
     * @return Result
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.warn("业务异常，URI={}，错误码={}，信息: {}", uri, e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理权限异常
     *
     * @param e      权限异常
     * @param request HTTP请求
     * @return Result
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.warn("权限异常，URI={}，错误码={}，信息: {}", uri, e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param e      参数校验异常
     * @param request HTTP请求
     * @return Result
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败，URI={}，信息: {}", uri, message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e      请求方法不支持异常
     * @param request HTTP请求
     * @return Result
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.warn("请求方法不支持，URI={}，方法: {}", uri, e.getMethod());
        return Result.fail(ErrorCode.METHOD_NOT_SUPPORTED);
    }

    /**
     * 处理其他未知异常
     *
     * @param e      异常
     * @param request HTTP请求
     * @return Result
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.error("系统异常，URI={}，异常: ", uri, e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常，请稍后重试");
    }

}
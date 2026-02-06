package com.lfs.service;

/**
 * Token过期异常
 * 当API返回401状态码时抛出此异常
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
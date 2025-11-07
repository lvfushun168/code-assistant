package com.lfs.domain;

import lombok.Data;

@Data
public class BackendResponse<T> {
    private int code;
    private String message;
    private T data;
}

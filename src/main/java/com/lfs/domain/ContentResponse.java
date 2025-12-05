package com.lfs.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档返回内容
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ContentResponse implements Serializable {

    /**
     * 文档id
     */
    private Long id;

    /**
     * 目录id
     */
    private Long dirId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档类型（txt,json,java,yml等等）
     */
    private String type;
}

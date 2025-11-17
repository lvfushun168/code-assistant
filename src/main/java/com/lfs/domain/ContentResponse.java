package com.lfs.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档返回内容
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ContentResponse {

    /**
     * 目录id
     */
    private Long dirId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    private String content;
}

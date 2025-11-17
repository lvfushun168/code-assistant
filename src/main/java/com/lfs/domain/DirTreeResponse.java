package com.lfs.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 目录树
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DirTreeResponse {
    /**
     * id
     */
    private Long id;

    /**
     * 父id
     */
    private Long parentId;

    /**
     * 目录名
     */
    private String name;

    /**
     * 目录下的文档内容
     */
    private List<ContentResponse> contents;

    /**
     * 子目录
     */
    private List<DirTreeResponse> children;

}

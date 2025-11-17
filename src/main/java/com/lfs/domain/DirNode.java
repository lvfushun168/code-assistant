package com.lfs.domain;

import java.util.List;

public class DirNode {
    private Long id;
    private Long parentId;
    private String name;
    private String contents;
    private List<DirNode> children;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public List<DirNode> getChildren() {
        return children;
    }

    public void setChildren(List<DirNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return name; // For display in JTree
    }
}

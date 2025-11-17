package com.lfs.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDirRequest {
    private Long id;
    private Long parentId;
    private String name;
}

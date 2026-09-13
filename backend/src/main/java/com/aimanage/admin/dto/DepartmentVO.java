package com.aimanage.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点。根节点是公司本身。
 */
@Data
public class DepartmentVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;

    /** 该部门直属人数（不含子部门，因为树只有两层） */
    private Integer memberCount;

    /** 公司根节点。可改名，但不可删除 */
    private Boolean isRoot;

    private List<DepartmentVO> children = new ArrayList<>();
}

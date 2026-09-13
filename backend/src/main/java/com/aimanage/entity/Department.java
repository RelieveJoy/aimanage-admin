package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门实体，对应表 {@code department}。
 *
 * <p><b>树是两层的：公司 → 部门</b>，其中根节点是公司本身（{@code parentId = 0}）。
 * 单企业形态下不需要"企业"这一层，也不需要无限递归。
 * 见 admin端_故事地图与任务分解.md AD3 的设计思考。
 */
@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 0 表示公司根节点 */
    private Long parentId;

    private Integer sort;

    private LocalDateTime createdAt;

    /** 是否是公司根节点 */
    public boolean isRoot() {
        return parentId != null && parentId == 0L;
    }
}

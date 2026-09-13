package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体，对应表 {@code project}。
 *
 * <p>单企业多项目形态：本表就是这家公司的全部项目，没有 tenant_id。
 */
@Data
@TableName("project")
public class Project {

    /** 进行中 */
    public static final int STATUS_ACTIVE = 1;

    /** 已归档 —— 数据全留，仅从默认视图隐藏 */
    public static final int STATUS_ARCHIVED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 项目编号，如 PRJ-001，系统自动生成 */
    private String code;

    private String description;

    /** 项目经理，必须是 role=PM 且启用的用户 */
    private Long pmId;

    /** 1 进行中 / 0 已归档 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

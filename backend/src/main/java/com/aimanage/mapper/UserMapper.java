package com.aimanage.mapper;

import com.aimanage.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户 Mapper。继承 BaseMapper 后即可获得基础 CRUD，
 * 复杂查询用 Wrapper 在 Service 层拼装。
 */
public interface UserMapper extends BaseMapper<User> {
}

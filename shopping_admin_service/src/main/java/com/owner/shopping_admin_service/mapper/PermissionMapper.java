package com.owner.shopping_admin_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.shopping_common.pojo.Permission;

public interface PermissionMapper extends BaseMapper<Permission> {
    void delete(Long pid);
}

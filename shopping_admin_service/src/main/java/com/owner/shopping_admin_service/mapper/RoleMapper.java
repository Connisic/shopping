package com.owner.shopping_admin_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.shopping_common.pojo.Role;
import org.apache.ibatis.annotations.Param;

public interface RoleMapper extends BaseMapper<Role> {
    //根据rid查询角色，以及角色权限
    Role findById(Long rid);
    //根据rid删除管理员角色表信息
    void deleteRoleAllAdmin(Long rid);

    //删除角色拥有的所有权限
    void deleteRoleAllPermission(Long rid);
    //给角色添加权限
    void addPermissionToRole(@Param("rid") Long rid, @Param("pid")Long pid);
}

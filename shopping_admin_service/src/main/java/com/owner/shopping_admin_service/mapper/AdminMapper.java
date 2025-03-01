package com.owner.shopping_admin_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.shopping_common.pojo.Admin;
import com.owner.shopping_common.pojo.Permission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminMapper extends BaseMapper<Admin> {
    //删除管理员以及管理员相关角色
    void deleteAdminAllRole(Long aid);

    //根据id查询管理员以及管理员角色和权限
    Admin findById(Long aid);

    //给管理员添加角色
    void addRoleToAdmin(@Param("aid") Long aid,@Param("rid") Long rid);

    //根据管理员名查询所有权限
    List<Permission> findAllPermission(String username);
}

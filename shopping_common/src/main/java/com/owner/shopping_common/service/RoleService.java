package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Role;

import java.util.List;

public interface RoleService {
    //新增角色
    void add(Role role);
    //删除角色
    void del(Long rid);
    //修改角色
    void update(Role role);
    //根据rid查询角色，以及角色所拥有的权限
    Role findById(Long rid);
    //查询所有角色
    List<Role> findAll();
    //分页查询角色
    Page<Role> search(int page,int size);
    //修改角色的权限
    void updatePermissionToRole(Long rid,Long[] pids);
}

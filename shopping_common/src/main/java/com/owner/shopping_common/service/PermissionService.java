package com.owner.shopping_common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Permission;

import java.util.List;

public interface PermissionService {
    //新增权限
    void add(Permission permission);
    //修改权限
    void update(Permission permission);
    //删除权限
    void del(Long pid);
    //根据pid查询权限
    Permission findById(Long pid);
    //分页查询权限
    Page<Permission> search(int page,int size);
    //查询所有权限
    List<Permission> findAll();
}

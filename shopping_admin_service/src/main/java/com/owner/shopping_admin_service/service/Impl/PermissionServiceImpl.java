package com.owner.shopping_admin_service.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_admin_service.mapper.PermissionMapper;
import com.owner.shopping_common.pojo.Permission;
import com.owner.shopping_common.service.PermissionService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@DubboService
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper mapper;
    @Override
    public void add(Permission permission) {
        //新增权限
        mapper.insert(permission);
    }

    @Override
    public void update(Permission permission) {
        //修改权限
        mapper.updateById(permission);
    }

    @Override
    public void del(Long pid) {
        //删除全西安
        mapper.deleteById(pid);
        //删除角色权限表
        mapper.delete(pid);
    }

    @Override
    public Permission findById(Long pid) {
        //根据pid查询权限
        return mapper.selectById(pid);
    }

    @Override
    public Page<Permission> search(int page, int size) {
        return mapper.selectPage(new Page<>(page,size),null);
    }

    @Override
    public List<Permission> findAll() {
        return mapper.selectList(null);
    }
}

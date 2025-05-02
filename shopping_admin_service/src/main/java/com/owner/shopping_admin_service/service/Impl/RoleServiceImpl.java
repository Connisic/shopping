package com.owner.shopping_admin_service.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_admin_service.mapper.RoleMapper;
import com.owner.shopping_common.pojo.Role;
import com.owner.shopping_common.service.RoleService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@DubboService
@Transactional
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleMapper roleMapper;

    @Override
    public void add(Role role) {
        roleMapper.insert(role);
    }

    @Override
    public void del(Long rid) {
        roleMapper.deleteById(rid);
        roleMapper.deleteRoleAllAdmin(rid);
        roleMapper.deleteRoleAllPermission(rid);
    }

    @Override
    public void update(Role role) {
        roleMapper.updateById(role);
    }

    @Override
    public Role findById(Long rid) {
        return roleMapper.findById(rid);
    }

    @Override
    public List<Role> findAll() {
        return roleMapper.selectList(null);
    }

    @Override
    public Page<Role> search(int page, int size) {
        return roleMapper.selectPage(new Page<>(page,size),null);
    }

    @Override
    public void updatePermissionToRole(Long rid, Long[] pids) {
        roleMapper.deleteRoleAllPermission(rid);
        for (Long pid:pids){
            roleMapper.addPermissionToRole(rid,pid);
        }
    }
}

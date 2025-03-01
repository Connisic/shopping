package com.owner.shopping_admin_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_admin_service.mapper.AdminMapper;
import com.owner.shopping_common.pojo.Admin;
import com.owner.shopping_common.pojo.Permission;
import com.owner.shopping_common.service.AdminService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@DubboService
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper mapper;

    private PasswordEncoder passwordEncoder;


    @Override
    public void add(Admin admin) {
        passwordEncoder=new BCryptPasswordEncoder();
        String newPassword = passwordEncoder.encode(admin.getPassword());
        admin.setPassword(newPassword);

        mapper.insert(admin);
    }

    @Override
    public void del(Long aid) {
        QueryWrapper<Admin> wrapper = new QueryWrapper<>();
        wrapper.eq("aid",aid);
        mapper.delete(wrapper);
        mapper.deleteAdminAllRole(aid);
    }

    @Override
    public void update(Admin admin) {
        mapper.updateById(admin);
    }

    @Override
    public Admin findById(Long aid) {
        return mapper.findById(aid);
    }

    @Override
    public Page<Admin> search(int page, int size) {
        Page<Admin> adminPage = mapper.selectPage(new Page<>(page, size), null);
        return adminPage;
    }

    @Override
    public void updateRoleToAdmin(Long aid, Long[] rids) {
        mapper.deleteAdminAllRole(aid);
        for (Long rid: rids){
            mapper.addRoleToAdmin(aid,rid);
        }
    }

    @Override
    public Admin findByAdminName(String username) {
        QueryWrapper<Admin> wrapper = new QueryWrapper<>();
        wrapper.eq("username",username);
        Admin admin = mapper.selectOne(wrapper);
        return admin;
    }

    @Override
    public List<Permission> findAllPermission(String username) {
        return mapper.findAllPermission(username);
    }
}

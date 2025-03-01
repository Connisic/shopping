package com.owner.shopping_manager_api.security;

import com.owner.shopping_common.pojo.Admin;
import com.owner.shopping_common.pojo.Permission;
import com.owner.shopping_common.service.AdminService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
//认证和授权逻辑
@Service
public class MyUserDetailService implements UserDetailsService {
    @DubboReference
    private AdminService adminService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //1认证
        Admin admin = adminService.findByAdminName(username);
        if(admin==null){
            throw new UsernameNotFoundException("用户名不存在");
        }
        //2授权
        List<Permission> all = adminService.findAllPermission(username);
        List<GrantedAuthority> grantedAuthorities=new ArrayList<>();
        if (all.get(0)!=null){
            all.forEach(permission -> {
                grantedAuthorities.add(new SimpleGrantedAuthority(permission.getUrl()));
            });
        }

        //3封装为Userdetails对象
        UserDetails userDetails = User.withUsername(admin.getUsername())
                .password(admin.getPassword())
                .authorities(grantedAuthorities).build();
        //4返回封装好的Userdetails对象
        return userDetails;
    }
}

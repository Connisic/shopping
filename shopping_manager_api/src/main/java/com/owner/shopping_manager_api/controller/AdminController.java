package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Admin;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.AdminService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理员
 *
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @DubboReference
    private AdminService adminService;

    @GetMapping("/getUsername")
    public BaseResult<String> getUsername(){
        //1获取会话对象
        SecurityContext context = SecurityContextHolder.getContext();
        //2获取认证对象
        Authentication authentication = context.getAuthentication();
        //3获取登录用户信息
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        return BaseResult.ok(username);
    }
    /**
     * 新增管理员
     * @param admin 管理员
     * @return
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody Admin admin){
        adminService.add(admin);
        //返回不带数据的成功结果
        return BaseResult.ok();
    }

    /**
     * 根据aid修改管理员
     * @param admin 管理员
     * @return
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody Admin admin){
        adminService.update(admin);
        return BaseResult.ok();
    }

    /**
     * 删除管理员，同时删除管理元角色信息
     * @param aid 管理员aid
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResult delete (Long aid){
        adminService.del(aid);
        return BaseResult.ok();
    }

    /**
     * 根据aid查询管理员，同时查询所拥有角色和权限信息
     * @param aid 管理员aid
     * @return
     */

    @GetMapping("/findById")
    public BaseResult<Admin> findById(Long aid){
        Admin admin = adminService.findById(aid);
        return BaseResult.ok(admin);
    }

    /**
     * 分页查询管理员控制器
     * @param page 页码
     * @param size 每页条数
     * @return
     */
    @PreAuthorize("hasAnyAuthority('/admin/search')")
    @GetMapping("/search")
    public BaseResult<Page<Admin>> search(int page, int size){
        Page<Admin> adminPage = adminService.search(page, size);
        return BaseResult.ok(adminPage);
    }

    /**
     * 修改管理员角色信息
     * @param aid 管理员aid
     * @param rids 要新增的角色rid集合
     * @return
     */
    @PutMapping("/updateRoleToAdmin")
    public BaseResult updateRoleToAdmin(Long aid,Long[] rids){
        adminService.updateRoleToAdmin(aid,rids);
        return BaseResult.ok();
    }
}

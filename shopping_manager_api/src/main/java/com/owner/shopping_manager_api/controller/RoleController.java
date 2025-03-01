package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Role;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.RoleService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {
    @DubboReference
    private RoleService roleService;

    /**
     * 新增juese
     * @param role 角色
     * @return
     */
    @PostMapping("/add")
    public BaseResult add(@RequestBody Role role){
        roleService.add(role);
        return BaseResult.ok();
    }

    /**
     * 修改角色
     * @param role 需要修改的角色
     * @return
     */
    @PutMapping("/update")
    public BaseResult update(@RequestBody Role role){
        roleService.update(role);
        return BaseResult.ok();
    }

    /**
     * 删除角色，以及角色权限，以及对应管理员角色表信息
     * @param rid 角色id
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long rid){
        roleService.del(rid);
        return BaseResult.ok();
    }

    /**
     * 根据rid查询角色，以及角色拥有的权限集合
     * @param rid 角色rid
     * @return
     */
    @GetMapping("/findById")
    public BaseResult<Role> findById(Long rid){
        Role role = roleService.findById(rid);
        return BaseResult.ok(role);
    }

    /**
     * 分页查询角色
     * @param page 页码
     * @param size 每页条数
     * @return
     */
    @PreAuthorize("hasAuthority('/role/search')")
    @GetMapping("/search")
    public BaseResult<Page<Role>> search(int page,int size){
        Page<Role> roles = roleService.search(page, size);
        return BaseResult.ok(roles);
    }

    /**
     * 查询所有角色
     * @return
     */
    @GetMapping("/findAll")
    public BaseResult<List<Role>> search(){
        List<Role> list = roleService.findAll();
        return BaseResult.ok(list);
    }

    /**
     * 修改角色权限，删除已有的角色权限信息，然后新增角色权限
     * @param rid 角色rid
     * @param pids 权限pid集合pids
     * @return
     */
    @PutMapping("/updatePermissionToRole")
    public BaseResult updatePermissionToRole(Long rid,Long[] pids){
        roleService.updatePermissionToRole(rid,pids);
        return BaseResult.ok();
    }

}

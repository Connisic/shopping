package com.owner.shopping_manager_api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.Permission;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.PermissionService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
public class PermissionController {
    @DubboReference
    private PermissionService service;

    /**
     * 新增权限
     * @param permission 要新增的权限
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add( @RequestBody Permission permission){
        service.add(permission);
        return BaseResult.ok();
    }

    /**
     * 修改权限
     * @param permission 修改后的权限
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update( @RequestBody Permission permission){
        service.update(permission);
        return BaseResult.ok();
    }

    /**
     * 删除权限
     * @param pid 要删除权限的pid
     * @return 执行结果
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long pid){
        service.del(pid);
        return BaseResult.ok();
    }

    /**
     * 根据pid查询权限
     * @param pid 权限id
     * @return 返回权限
     */
    @GetMapping("/findById")
    public BaseResult<Permission> findById(Long pid){
        Permission per = service.findById(pid);
        return BaseResult.ok(per);
    }

    /**
     * 分页查询权限
     * @param page 页码
     * @param size 每页条数
     * @return 分页查询结果
     */
    @GetMapping("/search")
    public BaseResult<Page<Permission>> search(int page,int size){
        Page<Permission> permissionPage = service.search(page, size);
        return BaseResult.ok(permissionPage);
    }

    /**
     * 查询所有权限
     * @return 所有权限集合
     */
    @GetMapping("/findAll")
    public BaseResult<List<Permission>> findAll(){
        List<Permission> all = service.findAll();
        return BaseResult.ok(all);
    }
}

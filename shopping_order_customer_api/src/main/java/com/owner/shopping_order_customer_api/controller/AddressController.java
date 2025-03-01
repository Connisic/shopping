package com.owner.shopping_order_customer_api.controller;

import com.owner.shopping_common.pojo.Address;
import com.owner.shopping_common.pojo.Area;
import com.owner.shopping_common.pojo.City;
import com.owner.shopping_common.pojo.Province;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.AddressService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/address")
public class AddressController {
    @DubboReference
    private AddressService addressService;

    /**
     * 查询所有省份
     * @return 返回省份列表
     */
    @GetMapping("/findAllProvince")
    public BaseResult<List<Province>> findAllProvince(){
        List<Province> allprovince = addressService.findAllprovince();
        return BaseResult.ok(allprovince);
    }

    /**
     * 根据省份id查询所有城市
     * @param provinceId 省份id
     * @return 返回该省管辖的所有城市
     */
    @GetMapping("/findCityByProvince")
    public BaseResult<List<City>> findCityByProvince(Long provinceId){
        List<City> cities = addressService.findCityByProvince(provinceId);
        return BaseResult.ok(cities);
    }

    /**
     * 根据城市id查询县区
     * @param cityId 城市id
     * @return 返回该城市以下的所有县区
     */
    @GetMapping("/findAreaByCity")
    public BaseResult<List<Area>> findAreaByCity(Long cityId){
        List<Area> areas = addressService.findAreaByCityId(cityId);
        return BaseResult.ok(areas);
    }

    /**
     * 新增地址
     * @param userId 用户id
     * @param address 用户新增的地址
     * @return 执行结果
     */
    @PostMapping("/add")
    public BaseResult add(@RequestHeader Long userId,@RequestBody Address address){
        address.setUserId(userId);
        addressService.add(address);
        return BaseResult.ok();
    }

    /**
     * 修改地址
     * @param userId 用户id：Higress自动解析JWT令牌，封装到请求头
     * @param address 用户修改之后的地址
     * @return 执行结果
     */
    @PutMapping("/update")
    public BaseResult update(@RequestHeader Long userId,@RequestBody Address address){
        address.setUserId(userId);
        addressService.update(address);
        return BaseResult.ok();
    }

    /**
     * 根据地址id查询地址详情
     * @param id 地址id
     * @return 返回地址详情对象
     */
    @GetMapping("/findById")
    public BaseResult<Address> findById(Long id){
        Address address = addressService.findById(id);
        return BaseResult.ok(address);
    }

    /**
     * 根据用户查询用户所有的地址
     * @param userId 用户Id
     * @return 返回所有已有地址
     */
    @GetMapping("/findByUser")
    public BaseResult<List<Address>> findByUser(@RequestHeader Long userId){
        List<Address> addressList = addressService.findByUser(userId);
        return BaseResult.ok(addressList);
    }

    /**
     * 根据地址id删除地址
     * @param id 地址id
     * @return 执行结果
     */
    @DeleteMapping("/delete")
    public BaseResult delete(Long id){
        addressService.delete(id);
        return BaseResult.ok();
    }
}

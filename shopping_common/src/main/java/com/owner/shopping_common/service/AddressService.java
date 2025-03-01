package com.owner.shopping_common.service;

import com.owner.shopping_common.pojo.Address;
import com.owner.shopping_common.pojo.Area;
import com.owner.shopping_common.pojo.City;
import com.owner.shopping_common.pojo.Province;

import java.util.List;

public interface AddressService {
    //查询所有省份
    List<Province> findAllprovince();
    //查询省份下的所有城市
    List<City> findCityByProvince(Long provinceId);
    //查询城市下的所有区县
    List<Area> findAreaByCityId(Long cityId);
    //新增地址
    void add(Address address);
    //修改地址
    void update(Address address);
    //根据id获取地址
    Address findById(Long id);
    //删除地址
    void delete(Long id);
    //查询用户下的所有地址
    List<Address> findByUser(Long userId);
}

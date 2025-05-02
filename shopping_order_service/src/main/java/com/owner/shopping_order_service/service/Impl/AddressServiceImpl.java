package com.owner.shopping_order_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.owner.shopping_common.pojo.Address;
import com.owner.shopping_common.pojo.Area;
import com.owner.shopping_common.pojo.City;
import com.owner.shopping_common.pojo.Province;
import com.owner.shopping_common.service.AddressService;
import com.owner.shopping_order_service.mapper.AddressMapper;
import com.owner.shopping_order_service.mapper.AreaMapper;
import com.owner.shopping_order_service.mapper.CityMapper;
import com.owner.shopping_order_service.mapper.ProvinceMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@DubboService
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ProvinceMapper provinceMapper;

    @Autowired
    private CityMapper cityMapper;

    @Autowired
    private AreaMapper areaMapper;

    @Override
    public List<Province> findAllprovince() {
        List<Province> provinces = provinceMapper.selectList(null);
        return provinces;
    }

    @Override
    public List<City> findCityByProvince(Long provinceId) {
        QueryWrapper<City> wrapper = new QueryWrapper<>();
        wrapper.eq("provinceid",provinceId);

        List<City> cities = cityMapper.selectList(wrapper);
        return cities;
    }

    @Override
    public List<Area> findAreaByCityId(Long cityId) {
        QueryWrapper<Area> wrapper = new QueryWrapper<>();
        wrapper.eq("cityId",cityId);

        List<Area> areas = areaMapper.selectList(wrapper);
        return areas;
    }

    @Override
    public void add(Address address) {
        addressMapper.insert(address);
    }

    @Override
    public void update(Address address) {
        addressMapper.updateById(address);
    }

    @Override
    public Address findById(Long id) {
        Address address = addressMapper.selectById(id);
        return address;
    }

    @Override
    public void delete(Long id) {
        addressMapper.deleteById(id);
    }

    @Override
    public List<Address> findByUser(Long userId) {
        QueryWrapper<Address> wrapper = new QueryWrapper<>();
        wrapper.eq("userId",userId);
        List<Address> addresses = addressMapper.selectList(wrapper);
        return addresses;
    }
}

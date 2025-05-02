package com.owner.shopping_user_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.owner.shopping_common.pojo.ShoppingUser;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.ShoppingUserService;
import com.owner.shopping_common.util.Md5Util;

import com.owner.shopping_user_service.mapper.ShoppingUserMapper;
import com.owner.shopping_user_service.util.JwtUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;

@DubboService
public class ShoppingUserServiceImpl implements ShoppingUserService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ShoppingUserMapper shoppingUserMapper;
    @Override
    public void saveRegisterCheckCode(String phoneNum, String code) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("registerCode:"+phoneNum,code);
    }

    @Override
    public void registerCheckCode(String phoneNum, String code) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String checkCode = (String) valueOperations.get("registerCode:" + phoneNum);
        if (!code.equals(checkCode)){
            throw new BusExceptiion(CodeEnum.REGISTER_CODE_ERROR);
        }
    }

    @Override
    public void register(ShoppingUser user) {
        //判断手机号是否存在
        String phone = user.getPhone();
        QueryWrapper<ShoppingUser> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("phone",phone);
        List<ShoppingUser> shoppingUsers1 = shoppingUserMapper.selectList(wrapper1);
        if (shoppingUsers1!=null&&shoppingUsers1.size()>0){//存在抛手机号重复异常
            throw new BusExceptiion(CodeEnum.REPEAT_PHONE_ERROR);
        }
        //判断用户名是否存在
        String username = user.getUsername();
        QueryWrapper<ShoppingUser> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("username",username);

        List<ShoppingUser> shoppingUsers2 = shoppingUserMapper.selectList(wrapper2);
        if (shoppingUsers2!=null&&shoppingUsers2.size()>0){//存在抛用户名重复异常
            throw new BusExceptiion(CodeEnum.REPEAT_USERNAME_ERROR);
        }

        //不存在，插入数据库
        String newPassword = Md5Util.encode(user.getPassword());//Md5加密
        user.setPassword(newPassword);

        //插入数据库
        shoppingUserMapper.insert(user);

    }

    @Override
    public String loginPassword(String username, String password) {
        //根据用户名从数据库中查用户
        QueryWrapper<ShoppingUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username",username);
        //查询user
        ShoppingUser shoppingUser = shoppingUserMapper.selectOne(wrapper);
        if(shoppingUser==null){
            throw new BusExceptiion(CodeEnum.USER_LOGIN_ERROR);
        }
        if (!Md5Util.verify(password,shoppingUser.getPassword())){
            throw new BusExceptiion(CodeEnum.USER_LOGIN_ERROR);
        }
        //返回生成令牌
        String jwt = JwtUtils.sign(shoppingUser.getId(), shoppingUser.getUsername());
        return jwt;
    }

    @Override
    public void saveLoginCheckCode(String phoneNum, String code) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("loginCode:"+phoneNum,code);
    }

    @Override
    public String loginCheckCode(String phoneNum, String code) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String checkCode = (String) valueOperations.get("loginCode:" + phoneNum);
        if (!code.equals(checkCode)){
            throw new BusExceptiion(CodeEnum.LOGIN_CODE_ERROR);
        }
        //手机号验证成功，根据手机号查询用户信息
        QueryWrapper<ShoppingUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone",phoneNum);
        ShoppingUser shoppingUser = shoppingUserMapper.selectOne(wrapper);
        //返回生成令牌
        String jwt = JwtUtils.sign(shoppingUser.getId(), shoppingUser.getUsername());
        return jwt;
    }

    @Override
    public String getName(String token) {
        //解析令牌
        Map<String, Object> verify = JwtUtils.verify(token);
        //获取用户名
        String username = (String) verify.get("username");
        return username;
    }

    @Override
    public ShoppingUser findById(String token) {
        //解析令牌
        Map<String, Object> verify = JwtUtils.verify(token);
        //获取用户ID
        Long userId = (Long) verify.get("userId");
        QueryWrapper<ShoppingUser> wrapper = new QueryWrapper<>();
        wrapper.eq("id",userId);

        ShoppingUser shoppingUser = shoppingUserMapper.selectOne(wrapper);
        return shoppingUser;
    }

    @Override
    public List<ShoppingUser> getAllUser() {
        return shoppingUserMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 判断手机号是否注册 用户状态是否正常
     * @param phone
     */
    public void checkPhone(String phone){
        QueryWrapper<ShoppingUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone",phone);

        ShoppingUser shoppingUser = shoppingUserMapper.selectOne(wrapper);
        if (shoppingUser==null){
            throw new BusExceptiion(CodeEnum.LOGIN_NOPHONE_ERROR);
        }
        if (!"Y".equals(shoppingUser.getStatus())){
            throw new BusExceptiion(CodeEnum.LOGIN_STATUS_ERROR);
        }
    }
}

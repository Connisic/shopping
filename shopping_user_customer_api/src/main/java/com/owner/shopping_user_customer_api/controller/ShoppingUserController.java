package com.owner.shopping_user_customer_api.controller;

import com.owner.shopping_common.pojo.ShoppingUser;
import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.MessageService;
import com.owner.shopping_common.service.ShoppingUserService;
import com.owner.shopping_common.util.RandomUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user/shoppingUser")
public class ShoppingUserController {
    //短信服务
    @DubboReference
    private MessageService messageService;

    @DubboReference
    private ShoppingUserService shoppingUserService;

    /**
     * 发送注册短信
     * @param phone 手机号
     * @return 执行结果
     */
    @GetMapping("/sendMessage")
    public BaseResult sendMessage(String phone){
        //1生成随机数
        String checkCode = RandomUtil.buildCheckCode();

        //2发送短信
        BaseResult result = messageService.sendMessage(phone, checkCode);
        //3保存短信+手机号到redis
        if (result.getCode()==200){
            //保存到redis
            shoppingUserService.saveRegisterCheckCode(phone,checkCode);
            return BaseResult.ok();
        }else{
            return result;
        }
    }

    /**
     * 验证用户注册验证码
     * @param phone 手机号
     * @param checkCode 用户输入的验证码
     * @return 执行结果
     */
    @GetMapping("/registerCheckCode")
    public BaseResult registerCheckCode(String phone,String checkCode){
        shoppingUserService.registerCheckCode(phone,checkCode);
        return BaseResult.ok();
    }

    /**
     * 用户注册
     * @param shoppingUser 注册用户对象
     * @return 执行结果
     *
     */
    @PostMapping("/register")
    public BaseResult register(@RequestBody ShoppingUser shoppingUser){
        shoppingUserService.register(shoppingUser);
        return BaseResult.ok();
    }

    /**
     * 用户名密码登录
     * @param user 请求体ShoppingUser对象
     * @return 执行结果，密码正确返回200成功，不正确service抛出BusException，code607用户名或密码不正确
     */
    @PostMapping("/loginPassword")
    public BaseResult<String> loginByPassword(@RequestBody ShoppingUser user){
        String jwt = shoppingUserService.loginPassword(user.getUsername(), user.getPassword());
        return BaseResult.ok(jwt);
    }

    /**
     * 用户手机验证码登录
     * @param phone 用户手机号
     * @param checkCode 用户输入的验证码
     * @return 执行结果，验证码正确返回200成功，不正确service抛出BusException，code608登录验证码错误
     */
    @PostMapping("/loginCheckCode")
    public BaseResult<String> loginCheckCode(String phone,String checkCode){
        //验证手机验证码，登陆成功生成jwt
        String jwt = shoppingUserService.loginCheckCode(phone, checkCode);
        return BaseResult.ok(jwt);
    }

    /**
     * 发送注册短信
     * @param phone 手机号
     * @return 执行结果
     */
    @GetMapping("/sendLoginCheckCode")
    public BaseResult sendLoginCheckCode(String phone){
        //优化手机号登录
            //判断手机号是否注册，用户状态是否正常
        //检查错误，service抛出异常返回Baseresult结果
        shoppingUserService.checkPhone(phone);
        //1生成随机数
        String checkCode = RandomUtil.buildCheckCode();
        //2发送短信
        BaseResult result = messageService.sendMessage(phone, checkCode);
        //3保存短信+手机号到redis
        if (result.getCode()==200){
            //保存到redis
            shoppingUserService.saveLoginCheckCode(phone,checkCode);
            return BaseResult.ok();
        }else{
            return result;
        }
    }

    /**
     * 获取登录用户名
     * @param authorization 请求头中Authorization对应的字符串
     * @return 返回用户名
     */
    @GetMapping("/getName")
    public BaseResult<String> getName(@RequestHeader("Authorization") String authorization){
        String token = authorization.replace("Bearer ", "");
        String username = shoppingUserService.getName(token);
        return BaseResult.ok(username);

    }


}

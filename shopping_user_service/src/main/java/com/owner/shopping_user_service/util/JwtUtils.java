package com.owner.shopping_user_service.util;

import lombok.SneakyThrows;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
  //  公钥
  public final static String PUBLIC_JSON = "{\"kty\":\"RSA\",\"n\":\"z2VKUV1_UlpXNTGf2IqKLu1FQALM5V1_AidaJ9_ehAjNPQhtlTVKpbUZOznzKP66g54TUUQYMiDfwV3irf5qy2lDVi_B9iID8AtGUaFkV00jRYqwfSm1aQADFcAZ6nIi4YuofGHu31QLOB6FEg3KJve1LbOMwhDjPm1PBK0M_6DxsEAMpXeSz5XynRHcELwBl0l1pB0F8RA_6VbDGamyKJch3jFCMXxDdTWXfu3548vzXA3AAC5QORaxvR4h2qNvAWi3SL_W-6IFVDwFN0ODCO64TmAx3JxyQoq3LwAlqUOK2LAbFfRMXe1aMMtCCXFlg_4hRvoojNbuBMhoVJK_mQ\",\"e\":\"AQAB\"}";
  //  私钥
  public final static String PRIVATE_JSON = "{\"kty\":\"RSA\",\"n\":\"z2VKUV1_UlpXNTGf2IqKLu1FQALM5V1_AidaJ9_ehAjNPQhtlTVKpbUZOznzKP66g54TUUQYMiDfwV3irf5qy2lDVi_B9iID8AtGUaFkV00jRYqwfSm1aQADFcAZ6nIi4YuofGHu31QLOB6FEg3KJve1LbOMwhDjPm1PBK0M_6DxsEAMpXeSz5XynRHcELwBl0l1pB0F8RA_6VbDGamyKJch3jFCMXxDdTWXfu3548vzXA3AAC5QORaxvR4h2qNvAWi3SL_W-6IFVDwFN0ODCO64TmAx3JxyQoq3LwAlqUOK2LAbFfRMXe1aMMtCCXFlg_4hRvoojNbuBMhoVJK_mQ\",\"e\":\"AQAB\",\"d\":\"OFslWTvHW8j5tpYwAecPipXmAfFg3RjNaPfpizlNnWJI5wNRx8BN8v5lh08FcMfbReWgCS6wRnrhYomRDce7HGATsKlUkv4GROXDa24oNoLbCUwZDkHVrl0W-YOUZz0xlklddMMl5mDPlaXRcij2HCWmgW8vWhK9tVp9pN8jT5jeOVqljgp779l7wvHf3F24LAjhakd3Um32z54sBfBXI2GpewEFcFQIn3BFW2djv1Q3_Lax6siN9K_4WpT53EP_EdkHgJXkLDPunbvuKi9eUxDWnP3AmF3ducAl1x_AwYIm7TAhU9NM9D3CCBLWCvmR45g1WDko2Rk0Y4Ti3aNm0w\",\"p\":\"0-Viv14-suAc6N-x7Kv4Wp3DxPBHWJ2oQC01U0L0yvz7VrMbjSTP6ETo19uoyx3kUS4kbYKgiu3AyQj687KhL6TrSmepVjjlquc4kPBq5PWnLzRg9yuRQ7M6MQsvftOlZ1r5z5Hww-jh59wydAINHcGpo49tTYV1HVr1_7mFmNs\",\"q\":\"-pAbnL-ilwjChbKgmsVSMEZ-Rfx3hLBQ4y9EyIn9B31c6y3sgkOk7qC8k2yppC3UnFKLPCUCqoCwps6j9gK4v-uqQ3omIAO3lyiSnFTasc5ggKxH0LzeKFVtd5EX-cwCXs_8zDeUSW-X7lybEwv2HkUg0gkUMIHLAWgEAwJGiZs\",\"dp\":\"yQAD_tHQme97nO9tILs0eAjFhtACba4HxvDbb6LJALCfXmvCC85KpeKQGQrV_-7YAKXLilHqJj9Hq6uoGXlt6vr-8vKIMDECs25oOxzD2XKo519BR9V9E4I1Bga9RApSEUu8QkkG4V_MwxsehOrZNEvbtjo8jde182Wwiqaacxc\",\"dq\":\"n9Z0Nt8syiplZMKZn4HGt1MQ1HaEi9mqD_MkbsVP64o2TzSBjmu2eB-DjIwh_8DYw0wI1DpL5BaqGg7yG0qLLqsYd2khjwSwBjZ0qFOdGxYrC4pbWbZSBerRJRgi8lAJJxCJFIFUEDCm_ACJUHIt9AtnL3VpMrRpmyEOhhjahIc\",\"qi\":\"eHzvG_E8-5W_xCCBnfEZJA1aP7BXT47AJm5hGRkcNkhYPxPZFfMfaXVhukHq-K13MFyc-eqk_U0Fn2U3Y-PakmFieJejRIWeBEQQqFS1oapimf-cpqTF4JJr1lHSzscFlD50mH7Ut-b1W5p4ld9p8_hhHTb2dCiWUZr0oB8LJfo\"}";


  /**
   * 生成token
   *
   * @param userId    用户id
   * @param username 用户名字
   * @return
   */
  @SneakyThrows
  public static String sign(Long userId, String username) {
    // 1、 创建jwtclaims  jwt内容载荷部分
    JwtClaims claims = new JwtClaims();
    // 是谁创建了令牌并且签署了它
    claims.setIssuer("owner");
    // 令牌将被发送给谁
    claims.setAudience("audience");
    // 失效时间长 （分钟）
    claims.setExpirationTimeMinutesInTheFuture(60 * 24);
    // 令牌唯一标识符
    claims.setGeneratedJwtId();
    // 当令牌被发布或者创建现在
    claims.setIssuedAtToNow();
    // 再次之前令牌无效
    claims.setNotBeforeMinutesInThePast(2);
    // 主题
    claims.setSubject("shopping");
    // 可以添加关于这个主题得声明属性
    claims.setClaim("userId", userId);
    claims.setClaim("username", username);


    // 2、签名
    JsonWebSignature jws = new JsonWebSignature();
    //赋值载荷
    jws.setPayload(claims.toJson());


    // 3、jwt使用私钥签署
    PrivateKey privateKey = new RsaJsonWebKey(JsonUtil.parseJson(PRIVATE_JSON)).getPrivateKey();
    jws.setKey(privateKey);


    // 4、设置关键 kid
    jws.setKeyIdHeaderValue("keyId");


    // 5、设置签名算法
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
    // 6、生成jwt
    String jwt = jws.getCompactSerialization();
    return jwt;
   }




  /**
   * 解密token，获取token中的信息
   *
   * @param token
   */
  @SneakyThrows
  public static Map<String, Object> verify(String token){
    // 1、引入公钥
    PublicKey publicKey = new RsaJsonWebKey(JsonUtil.parseJson(PUBLIC_JSON)).getPublicKey();
    // 2、使用jwtcoonsumer  验证和处理jwt
    JwtConsumer jwtConsumer = new JwtConsumerBuilder()
         .setRequireExpirationTime() //过期时间
         .setAllowedClockSkewInSeconds(30) //允许在验证得时候留有一些余地 计算时钟偏差  秒
         .setRequireSubject() // 主题生命
         .setExpectedIssuer("owner") // jwt需要知道谁发布得 用来验证发布人
         .setExpectedAudience("audience") //jwt目的是谁 用来验证观众
         .setVerificationKey(publicKey) // 用公钥验证签名  验证密钥
         .setJwsAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, AlgorithmIdentifiers.RSA_USING_SHA256))
         .build();
    // 3、验证jwt 并将其处理为 claims
    try {
      JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
      return jwtClaims.getClaimsMap();
     }catch (Exception e){
      return new HashMap();
     }
   }




  public static void main(String[] args){
    // 生成
    String baizhan = sign(1001L, "baizhan");
    System.out.println(baizhan);


    Map<String, Object> stringObjectMap = verify(baizhan);
    System.out.println(stringObjectMap.get("userId"));
    System.out.println(stringObjectMap.get("username"));
   }
}

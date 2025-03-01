package com.owner.shopping_seckill_service.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.owner.shopping_common.pojo.CartGoods;
import com.owner.shopping_common.pojo.Orders;
import com.owner.shopping_common.pojo.SeckillGoods;
import com.owner.shopping_common.result.BusExceptiion;
import com.owner.shopping_common.result.CodeEnum;
import com.owner.shopping_common.service.SeckillService;
import com.owner.shopping_seckill_service.mapper.SeckillMapper;
import com.owner.shopping_seckill_service.redis.RedissonLock;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DubboService
@Service
@Transactional
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillMapper seckillMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //redisson分布式事务，用于解决商品超卖现象
    //商品超卖现象就是在生成订单时，多个用户同时访问，此时都判断还有库存，生成订单信息
    //生成订单前首先要获取当前秒杀商品id对应的redis事务锁，成功则继续生成业务，反之中断请求，从而解决超卖现象
    @Autowired
    private RedissonLock redissonLock;

    //布隆过滤器，解决缓存穿透问题
    //缓存穿透问题：网站攻击者大量请求非法秒杀商品id，以至于redis查不到，查询请求全都压在数据库，导致数据库崩掉
    private BloomFilter<CharSequence> bloomFilter =
            BloomFilter.create(Funnels.stringFunnel(
                    Charset.forName("utf-8")),10000);

    /**
     * 每分钟查询一次数据库，更新redis中的秒杀商品
     * 条件为startTime<当前时间<endTime,库存大于零
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void refreshRedis(){
        System.out.println("同步mysql秒杀商品到redis");
        //MySQL数据同步到redis之前
        //将redis中的数据同步到MySQL
        List<SeckillGoods> seckillGoodsOld = redisTemplate.boundHashOps("seckillGoods").values();//获取redis中的商品数据
        //遍历
        for (SeckillGoods seckillGoods : seckillGoodsOld) {
            //根据商品id查询sql秒杀商品
            QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
            wrapper.eq("goodsId",seckillGoods.getGoodsId());
            SeckillGoods sqlSeckillGoods = seckillMapper.selectOne(wrapper);
            System.out.println(seckillGoods.getStockCount());
            //修改sql秒杀商品库存
            sqlSeckillGoods.setStockCount(seckillGoods.getStockCount());
            //同步到mysql
            seckillMapper.updateById(sqlSeckillGoods);
        }
        //1、查询所有正在秒杀的商品
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        //当前时间
        Date date = new Date();
        //转换成字符串
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        wrapper.le("startTime",now)
                .ge("endTime",now)
                .gt("stockCount",0);
        //查询符合条件的秒杀商品
        List<SeckillGoods> seckillGoods = seckillMapper.selectList(wrapper);

        //2、删除redis中原有的秒杀商品数据
        redisTemplate.delete("seckillGoods");

        //3、遍历正在秒杀的商品集合，加入redis中
        for (SeckillGoods seckillGood : seckillGoods) {
            redisTemplate.boundHashOps("seckillGoods").put(seckillGood.getGoodsId(),seckillGood);
            bloomFilter.put(seckillGood.getGoodsId().toString());
        }
    }

    @Override
    public Page<SeckillGoods> findPageByRedis(int page, int size) {
        //1、查询所有秒杀商品
        BoundHashOperations seckillGoods = redisTemplate.boundHashOps("seckillGoods");
        List<SeckillGoods> goods = seckillGoods.values();
        //2、获取当前页的秒杀商品列表
        //获取开始索引
        int start =(page-1)*size;
        //获取结束索引
        int end=start+size;
        if (end>goods.size())//如果结束索引超过秒杀商品集合大小
            end=goods.size();
        //截取当前页面的结果集
        List<SeckillGoods> subList = goods.subList(start, end);
        //手动生成page对象
        Page<SeckillGoods> seckillGoodsPage = new Page<>();
        seckillGoodsPage.setCurrent(page);//设置当前页数
        seckillGoodsPage.setSize(size);//设置当前页大小
        seckillGoodsPage.setTotal(goods.size());//设置总条数
        seckillGoodsPage.setRecords(subList);//设置结果集

        return seckillGoodsPage;
    }

    @Override
    public SeckillGoods findDescByRedis(Long goodsId) {
        //从布隆过滤器查询是否保存该秒杀商品id
        if (!bloomFilter.mightContain(goodsId.toString())){
            System.out.println("布隆过滤器判断商品不存在");
            return null;
        }
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(goodsId);
        if(seckillGoods!=null){
            System.out.println("----------------------------------------------------");
            return seckillGoods;
        }else{
            //从数据库查询秒杀商品
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
            wrapper.eq("goodsId",goodsId);

            seckillGoods = seckillMapper.selectOne(wrapper);
            Date date = new Date();
            if (seckillGoods==null
                ||date.before(seckillGoods.getStartTime())
                ||date.after(seckillGoods.getEndTime())){
                return null;
            }else{
                //秒杀商品正在秒杀，保存到redis
                addSeckillGoodsToRedis(seckillGoods);
                //保存到布隆过滤器
                bloomFilter.put(seckillGoods.getGoodsId().toString());
                return seckillGoods;
            }
        }
    }

    @Override
    public Orders createOrder(Orders orders) {
        CartGoods cartGoods = orders.getCartGoods().get(0);
        String lockKey = cartGoods.getGoodId().toString();
        if (redissonLock.lock(lockKey,10000)){
            try{
                //redisson根据当前秒杀商品名，对redis中的键上锁，
                //其他用户对同一秒杀商品下单获取对应redis的锁就会失败，这样就可以防止超卖现象

                //1、生成订单对象
                //获取秒杀商品
                SeckillGoods seckillGoods = findDescByRedis(cartGoods.getGoodId());
                cartGoods.setHeaderPic(seckillGoods.getHeaderPic());//商品图片

                orders.setId(IdWorker.getIdStr());//调用mybatisPlus生成id的雪花算法
                orders.setStatus(1);//未付款
                orders.setCreateTime(new Date()); //订单创建时间
                orders.setExpire(new Date(new Date().getTime()+1000*60*5));//设置订单过期时间

                //计算订单金额

                BigDecimal price = cartGoods.getPrice();
                Integer num = cartGoods.getNum();
                //price*num
                BigDecimal payment = price.multiply(BigDecimal.valueOf(num));
                orders.setPayment(payment);

                //2、减少秒杀商品库存
                //查询redis中秒杀商品
                Integer stockCount = seckillGoods.getStockCount();
                if (stockCount-num<0)        //库存不足抛出异常
                    throw new BusExceptiion(CodeEnum.NO_STOCK_ERROR);
                //减少秒杀商品数量
                seckillGoods.setStockCount(stockCount-cartGoods.getNum());
                redisTemplate.boundHashOps("seckillGoods").put(seckillGoods.getGoodsId(),seckillGoods);

                //3、保存订单数据到redis
                redisTemplate.setKeySerializer(new StringRedisSerializer());
                //设置订单过期时间
                redisTemplate.opsForValue().set(orders.getId(),orders,1, TimeUnit.MINUTES);

                //给订单创建副本，副本过期时间长于原订单，
                //由于订单过期后，拿不到orders对象，我们需要商品id和商品数量，去进行商品回退
                //所以通过副本过期时间长于原订单，以便于原订单过期触发过期事件时，可以从副本中拿到orders对象
                redisTemplate.opsForValue().set(orders.getId()+"_copy",orders,2,TimeUnit.MINUTES);
                return orders;
            }finally {
                redissonLock.unlock(lockKey);
            }
        }else
            return null;

    }

    @Override
    public Orders pay(String orderId) {
        //1、从redis查询订单数据，设置相关数据
        Orders orders = (Orders) redisTemplate.opsForValue().get(orderId);
        if(orders==null){
            throw new BusExceptiion(CodeEnum.ORDERS_EXPIRED_ERROR);
        }
        orders.getCartGoods().get(0).getGoodId();

        orders.setStatus(2);
        orders.setPaymentTime(new Date());
        orders.setPaymentType(2);
        //2、删除redis订单数据
        redisTemplate.delete(orderId);
        redisTemplate.delete(orderId+"_copy");
        //3、返回订单数据
        return orders;
    }

    @Override
    public Orders findOrder(String id) {
        Orders orders = (Orders) redisTemplate.opsForValue().get(id);
        if (orders==null)
            throw new BusExceptiion(CodeEnum.ORDERS_EXPIRED_ERROR);
        return orders;
    }

    @Override
    public void addSeckillGoodsToRedis(SeckillGoods seckillGoods) {
        redisTemplate.boundHashOps("seckillGoods").put(seckillGoods.getGoodsId(),seckillGoods);
    }
}

package com.zou.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zou.mapper.ShopMapper;
import com.zou.mapper.UserMapper;
import com.zou.pojo.User;
import com.zou.pojo.dto.LoginFormDTO;
import com.zou.pojo.dto.UserDTO;
import com.zou.pojo.entity.Shop;
import com.zou.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zou.utils.RedisConstants.*;

@Slf4j
@Service
public class AppService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private UserMapper userMapper;

    //mission 1: 短信登录，Redis的共享session应用逻辑
    public ResponseEntity<String> login(@RequestBody LoginFormDTO loginForm) {
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        if (!StrUtil.equals(loginForm.getCode(), cacheCode)) {
            return ResponseEntity.ok("fail");
        }

        User user = userMapper.getUserFromPhoneNumber(loginForm.getPhone());

        if (Objects.isNull(user)) {
            return ResponseEntity.ok("fail");
        }

        String token = UUID.randomUUID().toString(true);
        System.out.println("token: " + token);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, false, false);

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return ResponseEntity.ok("ok");
    }


    public ResponseEntity<String> sendCode(String phone) {
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到 session
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        System.out.println("发送短信验证码成功，验证码: " + code);
        return ResponseEntity.ok("ok");
    }


    //mission 2: 商户查询缓存，缓存穿透
    public ResponseEntity<Shop> queryShopById(Long id) {
        Shop shop = queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, shopMapper::selectById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return ResponseEntity.ok(shop);
    }

    public ResponseEntity<List<Shop>> queryShopList() {
        List<Shop> shops = shopMapper.selectList(new QueryWrapper<>());
        return ResponseEntity.ok(shops);
    }

    //mission 5: 查询附近的商户 Geospatial
    public ResponseEntity<List<Shop>> queryShopByType(Integer typeId, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            List<Shop> shops = shopMapper.selectList(new QueryWrapper<Shop>().eq("type_id", typeId));
            return ResponseEntity.ok(shops);
        }

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;

        //georadius key x y 5000 km withdist count end
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate
                .opsForGeo()
                .radius(key, new Circle(new Point(x, y), new Distance(1000))
                        , RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance());

        // 4.解析出id
        if (results == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = shopMapper.selectList(new QueryWrapper<Shop>().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")"));
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return ResponseEntity.ok(shops);
    }


    private <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(r), time, unit);
        return r;
    }

    public ResponseEntity<?> likeBlog(Long id) {
        // 1.获取登录用户
        String userId = RandomUtil.randomNumbers(1)+"";
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId);
        if (score == null) {
            stringRedisTemplate.opsForZSet().add(key, userId, System.currentTimeMillis());
        } else {
            stringRedisTemplate.opsForZSet().remove(key, userId);
        }
        return ResponseEntity.ok("");
    }

    // mission 7: 达人探店，点赞排行榜 ZSet
    public ResponseEntity<List<String>> queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<String> ids = top5.stream().map(m -> BLOG_LIKED_KEY + ":" + m).collect(Collectors.toList());
        return ResponseEntity.ok(ids);
    }
}

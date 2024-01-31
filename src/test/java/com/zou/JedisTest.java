package com.zou;

import cn.hutool.core.collection.CollectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanResult;

import java.util.List;


@SpringBootTest
public class JedisTest {

    public static final int STR_MAX_LEN = 1 * 1024;
    public static final int HASH_MAX_LEN = 1;

    JedisPool jedisPool;

    Jedis jedis;

    @BeforeEach
    void start() {
        jedisPool = new JedisPool("192.168.2.6", 6379);
        jedis = jedisPool.getResource();
    }

    @AfterEach
    void end() {
        if (jedis != null) {
            jedis.close();
        }
    }


    @Test
    void testScan() {
        System.out.println("Scan找出bigKeys优化");
        int maxLen = 0;
        long len = 0;
        String cursor = "0";

        while (true) {
            ScanResult<String> scanResult = jedis.scan(cursor);
            cursor = scanResult.getCursor();
            System.out.printf("cursor:%s %n", cursor);
            List<String> result = scanResult.getResult();
            if (CollectionUtil.isEmpty(result)) {
                break;
            }

            for (String key : result) {
                String type = jedis.type(key);
                switch (type) {
                    case "string":
                        len = jedis.strlen(key);
                        maxLen = STR_MAX_LEN;
                        break;
                    case "hash":
                        len = jedis.hlen(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    case "list":
                        len = jedis.llen(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    case "set":
                        len = jedis.scard(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    case "zset":
                        len = jedis.zcard(key);
                        maxLen = HASH_MAX_LEN;
                        break;
                    default:
                        break;
                }
                if (len >= maxLen) {
                    System.out.printf("Found big key:%s , type:%s , size: %d %n", key, type, len);
                }
            }
            if ("0".equals(cursor)) {
                break;
            }
        }
    }

    /**
     * 批处理string类型
     */
    @Test
    void testMset() {
        //数字起缓存作用
        String[] arr = new String[2000];
        int j = 0;
        long c = System.currentTimeMillis();
        for (int i = 1; i <= 100000; i++) {
            j = (i % 1000) << 1;
            arr[j] = "test:key_" + i;
            arr[j + 1] = "value" + i;
            if (j == 0) {
                jedis.mset(arr);
            }
        }
        System.out.println(System.currentTimeMillis() - c);
    }

    @Test
    void testPipeline() {
        long c = System.currentTimeMillis();
        Pipeline pipelined = jedis.pipelined();
        for (int i = 1; i <= 100000; i++) {
            pipelined.set("pipe:key_" + i, "value_" + i);
            if (i % 1000 == 0) {
                pipelined.sync();
            }
        }
        System.out.println(System.currentTimeMillis() - c);
    }

    @Test
    void test1() {
        Jedis jedis = new Jedis("192.168.2.6", 6379);
        System.out.println(jedis.ping());
        jedis.flushDB();//清空当前库的所有数据
        jedis.set("name", "leonard");
        jedis.set("age", "24");
        jedis.set("high", "170");
        System.out.println("name:" + jedis.get("name") + "\nage:" + jedis.get("age") + "\nhigh" + jedis.get("high"));
    }
}

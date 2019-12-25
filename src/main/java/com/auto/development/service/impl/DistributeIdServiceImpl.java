package com.auto.development.service.impl;

import com.auto.development.common.service.DistributeIdService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 分布式id服务实现类
 * @date 2019-01-16 9:58
 */
public class DistributeIdServiceImpl implements DistributeIdService {

    private JedisPool redisPool;

    private String databaseName;

    private String applicationName;

    public DistributeIdServiceImpl(String databaseName,
                                   String applicationName,
                                   JedisPool redisPool) {
        this.databaseName = databaseName;
        this.applicationName = applicationName;
        this.redisPool = redisPool;
    }

    @Override
    public Long nextId(String tableName, long step) {
        String key = getDistributeIdKey(databaseName + "_" + tableName);
        Jedis jedis = null;
        long ret = -1L;
        try {
            jedis = this.redisPool.getResource();
            if (jedis == null) {
                Long var7 = ret;
                return var7;
            }

            ret = jedis.incrBy(key, step);
        } finally {
            if (jedis != null) {
                jedis.close();
            }

        }

        return ret;
    }

    @Override
    public String getDistributeIdKey(String name) {
        return this.applicationName + ":" + name;
    }
}

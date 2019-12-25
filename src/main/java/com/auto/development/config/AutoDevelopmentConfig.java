package com.auto.development.config;

import com.auto.development.bean.BeanPostProcessorSelfCall;
import com.auto.development.bean.GeneratorCodeEngine;
import com.auto.development.bean.XMetaObjectHandler;
import com.auto.development.exception.XErrorPageRegistrar;
import com.auto.development.exception.XExceptionHandler;
import com.auto.development.security.SimpleCorsFilter;
import com.auto.development.service.impl.DistributeIdServiceImpl;
import com.auto.development.common.service.DistributeIdService;
import com.auto.development.common.util.JdbcUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.toolkit.AopUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自动配置类
 * @date 2019-05-10 21:23
 */
@Slf4j
@Configuration
@Import({AutoDevelopmentInitConfig.class, XWebMvcConfig.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties({RedisProperties.class})
public class AutoDevelopmentConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${ACCESS_CONTROL_ALLOW_ORIGIN:*}" )
    private String accessControlAllowOrigin;

    @Bean
    @ConditionalOnMissingBean(JedisPool.class)
    public JedisPool redisPool(RedisProperties redisProperties) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(32);
        int timeout = 1000 * 5;
        long maxWaitMillis = 1000 * 5;
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, redisProperties.getHost(), redisProperties.getPort(),
                timeout, redisProperties.getPassword());
        return jedisPool;
    }

    /**
     * 使用redis来生成分布式Long类型的数据库表主键
     *
     * @param redisPool
     * @param applicationName SpringBoot应用名称
     * @return
     * @throws SQLException
     */
    @Bean
    @SneakyThrows
    @ConditionalOnMissingBean(DistributeIdService.class)
    public DistributeIdService distributeIdService(JedisPool redisPool,
                                                   @Value("${spring.application.name}" ) String applicationName) {
        try (Connection connection = AopUtils.getTargetObject(dataSource).getConnection()) {
            DistributeIdService distributeIdService = new DistributeIdServiceImpl(
                    JdbcUtil.getSchemaName(connection.getMetaData().getURL()), applicationName, redisPool);
            return distributeIdService;
        }
    }

    @Bean
    public SimpleCorsFilter xCorsFilter() {
        return new SimpleCorsFilter(accessControlAllowOrigin);
    }

    @Bean
    public XExceptionHandler xExceptionHandler() {
        return new XExceptionHandler();
    }

    /**
     * 代码生成引擎
     *
     * @return
     */
    @Bean
    public GeneratorCodeEngine autoGeneratorCodeEngine() {
        return new GeneratorCodeEngine();
    }

    /**
     * MyBatis-Plus注解FieldFill，主动填充字段值实现接口
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler xMetaObjectHandler() {
        return new XMetaObjectHandler();
    }

    @Bean
    public XErrorPageRegistrar xErrorPageRegistrar() {
        return new XErrorPageRegistrar();
    }

    @Bean
    public BeanPostProcessorSelfCall beanPostProcessorSelfCall() {
        return new BeanPostProcessorSelfCall();
    }

}
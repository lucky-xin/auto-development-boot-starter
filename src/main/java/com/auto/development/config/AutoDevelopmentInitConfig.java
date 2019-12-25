package com.auto.development.config;

import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ClassUtil;
import com.auto.development.util.AutoDevelopmentHelper;
import com.auto.development.common.activerecord.XModel;
import com.auto.development.common.model.XTableInfo;
import com.auto.development.common.service.DistributeIdService;
import com.auto.development.common.util.JdbcUtil;
import com.auto.development.common.util.TableHelper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.toolkit.AopUtils;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.ITypeConvert;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.converts.PostgreSqlTypeConvert;
import com.auto.development.bean.GeneratorCodeEngine;
import com.auto.development.util.ResultSetConsumer;
import com.xin.utils.AssertUtil;
import com.xin.utils.CollectionUtil;
import com.xin.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自动初始化，为所连接的数据库初始化redis分布式的自增主键，把当前项目的pojo同步到数据库，
 * 并生成service,mapper...等类的代码
 * @date 2019-05-11 8:32
 */
@Slf4j
@Order
@Configuration
@AutoConfigureAfter(AutoDevelopmentConfig.class)
@EnableConfigurationProperties({RedisProperties.class, AutoGenDevProperties.class})
public class AutoDevelopmentInitConfig implements ApplicationContextAware {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    private DistributeIdService distributeIdService;

    @Autowired
    private AutoGenDevProperties autoGenDevProperties;

    @Autowired
    private GeneratorCodeEngine generatorCodeEngine;

    private BeanDefinitionRegistry beanDefinitionRegistry;

    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        if (autowireCapableBeanFactory instanceof BeanDefinitionRegistry) {
            this.beanDefinitionRegistry = (BeanDefinitionRegistry) autowireCapableBeanFactory;
        }
    }

    /**
     * 数据初始化
     */
    @PostConstruct
    public void doSync() throws SQLException {
        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        DbType dbType = null;
        String databaseName = null;
        try (Connection connection = AopUtils.getTargetObject(dataSource).getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            dbType = com.baomidou.mybatisplus.extension.toolkit.JdbcUtils.getDbType(databaseMetaData.getURL());
            databaseName = JdbcUtil.getSchemaName(databaseMetaData.getURL());
        }
        String tableSchema = databaseName;
        if (dbType == DbType.POSTGRE_SQL) {
            tableSchema = "public";
        }
        dsc.setDbType(dbType);
        ITypeConvert iTypeConvert = null;
        AutoDevelopmentHelper helper = new AutoDevelopmentHelper();
        Map<String, XTableInfo> tableInfoMap = getPojoTableInfo(autoGenDevProperties.getPojoPackage(), dbType);
        //2.获取所有pojo信息
        ResultSetConsumer<String> tableNameConsumer = getTableNameConsumer(tableSchema, helper, tableInfoMap);
        // 查询数据库获取当前所连接数据库所有表名
        String queryTableNamesSql = null;
        StrategyConfig strategyConfig = new StrategyConfig();
        strategyConfig.setInclude(tableInfoMap.keySet().toArray(new String[0]));
        switch (dbType) {
            case MYSQL:
                queryTableNamesSql = "select table_name from information_schema.tables where table_schema = ?";
                executeQuery(this.dataSource, tableNameConsumer, queryTableNamesSql, databaseName);
                iTypeConvert = new MySqlTypeConvert();
                break;
            case POSTGRE_SQL:
                queryTableNamesSql = "select tablename from pg_tables where schemaname='public'";
                executeQuery(this.dataSource, tableNameConsumer, queryTableNamesSql);
                iTypeConvert = new PostgreSqlTypeConvert();
                break;
            default:
                break;
        }
        dsc.setTypeConvert(iTypeConvert);
        dsc.setSchemaName(tableSchema);

        // 如果没有打开自动同步pojo功能则跳过
        if (autoGenDevProperties == null || !autoGenDevProperties.isEnableSyncPojo()) {
            return;
        }
        AssertUtil.checkNotEmpty(autoGenDevProperties.getPojoPackage(), "实体类包配置sync-pojo-package不能为空");
        AssertUtil.checkNotEmpty(autoGenDevProperties.getServicePackage(), "服务接口包配置service-package不能为空");
        AssertUtil.checkNotEmpty(autoGenDevProperties.getServiceImplPackage(), "服务实现类包配置service-impl-package不能为空");
        // 6.同步pojo对象到数据库
        helper.syncPojoInstance(dataSource, tableInfoMap);
        // 7.生成缺失的mapper.java以及mapper.xml
        helper.generateCode(generatorCodeEngine, tableInfoMap, autoGenDevProperties, getConsumer());
    }

    private ResultSetConsumer<String> getTableNameConsumer(String tableSchema,
                                                           AutoDevelopmentHelper automatedDevelopmentHelper,
                                                           Map<String, XTableInfo> tableInfoMap) {
        String queryColumnsSql = "select column_name from information_schema.columns where table_schema = ? and table_name = ?";
        ResultSetConsumer<String> tableNameConsumer = new ResultSetConsumer<String>() {
            @Override
            public String apply(ResultSet rs) throws SQLException {
                return rs.getString(1);
            }

            @Override
            public void accept(String tableName) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement(queryColumnsSql)) {
                    preparedStatement.setString(1, tableSchema);
                    preparedStatement.setString(2, tableName);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    List<String> columns = new ArrayList<>();
                    while (resultSet.next()) {
                        columns.add(resultSet.getString(1));
                    }
                    if (columns.isEmpty()) {
                        return;
                    }
                    tableName = tableName.toLowerCase();
                    // 3.同步分布式主键服务
                    automatedDevelopmentHelper.syncDistributeId(distributeIdService, dataSource, tableName, columns);
                    if (!autoGenDevProperties.isEnableSyncPojo() || CollectionUtil.isEmpty(tableInfoMap)) {
                        return;
                    }
                    // 4.同步pojo字段到数据库,根据数据库表名删除pojo信息，最后tableInfoMap之中剩下的TableInfo为数据库没有的表，下一步添加这些表
                    XTableInfo tableInfo = tableInfoMap.remove(tableName);
                    automatedDevelopmentHelper.syncTableField(dataSource, tableInfo, columns);
                } catch (SQLException e) {
                    log.warn(String.format("查询%s表获取字段异常", tableName), e);
                }
            }
        };
        return tableNameConsumer;
    }

    private Consumer<Class<?>> getConsumer() {
        Consumer<Class<?>> autowireBeanConsumer = (clazz) -> {
            if (beanDefinitionRegistry == null) {
                return;
            }
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
            beanDefinition.setBeanClass(MapperFactoryBean.class);
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(clazz);
            beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            beanDefinition.getPropertyValues().add("addToConfig", true);
            beanDefinition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
            beanDefinition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, clazz.getSimpleName());
            BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, beanDefinitionRegistry);
            // 初始化bean
            autowireCapableBeanFactory.getBean(clazz.getSimpleName());
        };
        return autowireBeanConsumer;
    }

    private Map<String, XTableInfo> getPojoTableInfo(String syncPojoPackage, DbType dbType) {
        if (StringUtil.isEmpty(syncPojoPackage)) {
            return new HashMap<>(0);
        }
        Filter<Class<?>> classFilter = (clazz) -> {
            TableName tableName = clazz.getAnnotation(TableName.class);
            return tableName != null && XModel.class.isAssignableFrom(clazz);
        };
        Set<Class<?>> classes = ClassUtil.scanPackage(syncPojoPackage, classFilter);
        Map<String, XTableInfo> tableInfoMap = new HashMap<>(classes.size());
        for (Class<?> clazz : classes) {
            XTableInfo tableInfo = TableHelper.getTableInfo(clazz);
            tableInfo.setDbType(dbType);
            tableInfoMap.put(tableInfo.getName(), tableInfo);
        }
        return tableInfoMap;
    }

    public <T> void executeQuery(DataSource dataSource
            , ResultSetConsumer<T> consumer
            , String sql
            , Object... parameters) throws SQLException {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);) {

            for (int i = 0; i < parameters.length; ++i) {
                stmt.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (consumer != null) {
                        T object = consumer.apply(rs);
                        consumer.accept(object);
                    }
                }
            }
        }
    }
}

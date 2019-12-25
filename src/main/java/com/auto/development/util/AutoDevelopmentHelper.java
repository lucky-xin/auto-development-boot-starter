package com.auto.development.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.func.VoidFunc1;
import cn.hutool.db.Db;
import com.auto.development.config.AutoGenDevProperties;
import com.auto.development.common.model.XTableInfo;
import com.auto.development.common.service.DistributeIdService;
import com.auto.development.common.util.AutoGenerateUtil;
import com.auto.development.common.util.DynamicClassLoader;
import com.auto.development.common.util.TableHelper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.auto.development.bean.GeneratorCodeEngine;
import com.xin.utils.CollectionUtil;
import com.xin.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: TODO
 * @date 2019-05-10 23:23
 */
@Slf4j
public class AutoDevelopmentHelper {
    /**
     * 同步pojo到数据库，以pojo为准，pojo有数据库没有则数据库添加该字段，数据库有该字段pojo没有则删除该数据库字段
     *
     * @param dataSource
     * @param tableInfo
     * @param columns
     */
    public void syncTableField(DataSource dataSource, XTableInfo tableInfo, List<String> columns) throws SQLException {
        if (tableInfo == null) {
            return;
        }
        Set<TableField> sameFields = new HashSet<>();
        Map<String, TableField> fieldMap = tableInfo.getFields().stream()
                .collect(Collectors.toMap(field -> field.getName(), field -> field));
        //2.遍历比较pojo和对应数据库表字段。获取删除，增加字段
        Iterator<String> iterator = columns.iterator();
        while (iterator.hasNext()) {
            String finalColumn = iterator.next();
            TableField tableFieldInfo = fieldMap.get(finalColumn);
            if (tableFieldInfo != null) {
                sameFields.add(tableFieldInfo);
                iterator.remove();
                fieldMap.remove(finalColumn);
            }
        }

        int fieldSize = fieldMap.size();
        int columnSize = columns.size();
        if (fieldSize > 0 || columnSize > 0) {
            VoidFunc1<Db> func = (db) -> {
                if (fieldSize > 0) {
                    //3.添加需要新增的字段
                    List<String> commentSqls = new ArrayList<>(fieldSize);
                    StringBuilder addFieldSql = new StringBuilder().append("ALTER TABLE ").append(tableInfo.getName());
                    Iterator<Map.Entry<String, TableField>> entryIterator = fieldMap.entrySet().iterator();
                    while (entryIterator.hasNext()) {
                        TableField field = entryIterator.next().getValue();
                        addFieldSql.append(TableHelper.getAddColumnSql(field, tableInfo.getDbType()));
                        if (entryIterator.hasNext()) {
                            addFieldSql.append(",");
                        } else {
                            addFieldSql.append(";");
                        }
                        if (tableInfo.getDbType() == DbType.POSTGRE_SQL) {
                            commentSqls.add(TableHelper.getAddCommentSql(tableInfo.getName(),
                                    field.getName(), field.getComment()));
                        }
                    }
                    db.execute(addFieldSql.toString());
                    for (String commentSql : commentSqls) {
                        db.execute(commentSql);
                    }
                }
                if (columnSize > 0) {
                    //4.删除字段
                    StringBuilder deleteFieldSql = new StringBuilder().append("ALTER TABLE ").append(tableInfo.getName());
                    for (int i = 0; i < columnSize; i++) {
                        String field = columns.get(i);
                        deleteFieldSql.append(" DROP COLUMN ").append(field);
                        if (i != columnSize - 1) {
                            deleteFieldSql.append(",");
                        } else {
                            deleteFieldSql.append(";");
                        }
                    }
                    db.execute(deleteFieldSql.toString());
                }
            };
            Db.use(dataSource).tx(func);
        }
    }

    public void syncDistributeId(DistributeIdService distributeIdService, DataSource dataSource, String tableName, List<String> columns) {
        String idName = "id";
        String tableNameIdField = tableName + "_id";
        boolean hasIdField = columns.contains("id");
        boolean hasTableNameIdField = columns.contains(tableNameIdField);
        if (hasIdField || hasTableNameIdField) {
            if (!hasIdField) {
                idName = tableNameIdField;
            }
            String queryTableCount = String.format("select max(%s) id from %s", idName, tableName);
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(queryTableCount)) {
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    Long maxId = resultSet.getLong(1);
                    //2.和Redis之中的id进行比较并设置
                    Long id = distributeIdService.nextId(tableName, 0);
                    Long diff = maxId - id;
                    if (diff > 0) {
                        distributeIdService.nextId(tableName, diff);
                    }
                }
            } catch (Exception e) {
                log.error("初始化分布式id异常", e);
            }
        }
    }

    /**
     * 根据pojo对象生成创表sql语句并执行
     *
     * @param dataSource
     * @param tableInfoMap
     * @throws SQLException
     */
    public void syncPojoInstance(DataSource dataSource, Map<String, XTableInfo> tableInfoMap) throws SQLException {
        if (CollectionUtil.isEmpty(tableInfoMap)) {
            return;
        }
        List<String> sqls = AutoGenerateUtil.getCreateTableSql(tableInfoMap);
        VoidFunc1<Db> txFunc = (db) -> {
            for (String sql : sqls) {
                db.execute(sql);
            }
        };
        Db.use(dataSource).tx(txFunc);
    }

    /**
     * 生成缺失的代码以及xml配置文件
     *
     * @param generatorCodeEngine
     * @param tableInfoMap
     * @param dataInsightsProperties
     * @param autowireBeanConsumer
     */
    public void generateCode(GeneratorCodeEngine generatorCodeEngine,
                             Map<String, XTableInfo> tableInfoMap,
                             AutoGenDevProperties dataInsightsProperties,
                             Consumer<Class<?>> autowireBeanConsumer) {
        String classPath = AutoDevelopmentHelper.class.getResource("/").getPath();
        String startPrefix = "file:";
        int index = classPath.indexOf("file:");
        if (index == 0) {
            classPath = classPath.substring(startPrefix.length());
        }
        int jarStrIndex = classPath.indexOf(".jar");
        if (jarStrIndex > 0) {
            return;
        }

        String mapperPackage = dataInsightsProperties.getMapperPackage();
        String servicePackage = dataInsightsProperties.getServicePackage();
        String serviceImplPackage = dataInsightsProperties.getServiceImplPackage();
        String mapperMiddlePath = mapperPackage.replace(".", "/");
        String serviceMiddlePath = servicePackage.replace(".", "/");
        String serviceImplMiddlePath = serviceImplPackage.replace(".", "/");

        Map<String, Object> objectMap = new HashMap<String, Object>(11) {{
            put("mapperPackage", dataInsightsProperties.getMapperPackage());
            put("entryPackage", dataInsightsProperties.getPojoPackage());
            put("servicePackage", dataInsightsProperties.getServicePackage());
            put("serviceImplPackage", dataInsightsProperties.getServiceImplPackage());
            put("author", "Luchaoxin 自动添加代码");
            put("date", DateUtil.getDate());
        }};

        String target = "/target";
        int targetIndex = classPath.indexOf(target);
        String temp = classPath;
        if (targetIndex > 0) {
            temp = temp.substring(0, targetIndex);
        }

        String mapperParentPath = temp + "/src/main/java/" + mapperMiddlePath;
        String serviceParentPath = temp + "/src/main/java/" + serviceMiddlePath;
        String serviceImplParentPath = temp + "/src/main/java/" + serviceImplMiddlePath;

        // jar   包之中 /D:/develop/workspace/idea/test/datainsights-edu/target/datainsights-edu-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/
        // 非jar 包   /D:/develop/workspace/idea/test/datainsights-edu/target/classes/

        String finalClassPath = classPath;
        tableInfoMap.forEach((tableName, tableInfo) -> {
            objectMap.put("comment", tableInfo.getComment());
            String entryName = TableHelper.getEntryName(tableInfo.getName());
            String mapperName = entryName + "Mapper";
            String mapperJavaPath = mapperParentPath + "/" + mapperName + ".java";
            String mapperXmlPath = mapperParentPath + "/" + mapperName + ".xml";
            String serviceName = entryName + "Service";
            String serviceImplName = entryName + "ServiceImpl";

            String serviceJavaPath = serviceParentPath + "/" + serviceName + ".java";
            String serviceImplJavaPath = serviceImplParentPath + "/" + serviceImplName + ".java";

            objectMap.put("entryName", entryName);
            objectMap.put("mapperName", mapperName);
            objectMap.put("serviceName", serviceName);
            objectMap.put("serviceImplName", serviceImplName);
            try {
                if (!FileUtil.exist(mapperJavaPath)) {
                    // 生成xxMapper.java文件
                    generatorCodeEngine.generatorSimpleMapper(objectMap, mapperJavaPath);
                    String classFilePath = finalClassPath + "/" + mapperMiddlePath + "/" + mapperName + ".class";
                    compileAndRegister(mapperJavaPath, finalClassPath, mapperName, classFilePath, autowireBeanConsumer, true);
                }

                if (!FileUtil.exist(mapperXmlPath)) {
                    //生成xxMapper.xml文件
                    generatorCodeEngine.generatorSimpleMapperXml(objectMap, mapperXmlPath);
                    try (InputStream is = new FileInputStream(mapperXmlPath)) {
                        FileUtil.writeFromStream(is, finalClassPath + "/" + mapperMiddlePath + "/" + entryName + "Mapper.xml");
                    } catch (Exception e) {
                        log.error(String.format("写入%s.xml异常", mapperName), e);
                    }
                }

                if (!FileUtil.exist(serviceJavaPath)) {
                    // 生成xxService.java文件
                    generatorCodeEngine.generatorSimpleService(objectMap, serviceJavaPath);
                    String classFilePath = finalClassPath + "/" + serviceMiddlePath + "/" + serviceName + ".class";
                    compileAndRegister(serviceJavaPath, finalClassPath, serviceName, classFilePath, autowireBeanConsumer, false);
                }

                if (!FileUtil.exist(serviceImplJavaPath)) {
                    // 生成xxServiceImpl.java文件
                    generatorCodeEngine.generatorSimpleServiceImpl(objectMap, serviceImplJavaPath);
                    String classFilePath = finalClassPath + "/" + serviceImplMiddlePath + "/" + serviceImplName + ".class";
                    compileAndRegister(serviceImplJavaPath, finalClassPath, serviceImplName, classFilePath, autowireBeanConsumer, true);
                }
            } catch (Exception e) {
                log.error(String.format("添加%s.java异常", mapperName), e);
            }
        });
    }

    private void compileAndRegister(String javaSourceCodePath, String classPath, String className, String classFilePath, Consumer<Class<?>> autowireBeanConsumer, boolean register) {
        try {
            // 编译自动生成的代码
            JavaCompileUtil.compiler(javaSourceCodePath, classPath);

            // 加载字节码文件到jvm
            Class<?> clazz = new DynamicClassLoader().loadClass(className, classFilePath);
            if (register) {
                // 注册当前对象到IoC之中
                autowireBeanConsumer.accept(clazz);
            }
        } catch (Exception e) {
            log.error("编译代码失败", e);
        }

    }
}

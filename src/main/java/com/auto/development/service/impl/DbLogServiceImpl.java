package com.auto.development.service.impl;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.RsHandler;
import com.alibaba.fastjson.JSON;
import com.auto.development.annotation.SysLog;
import com.auto.development.log.entity.LogInfo;
import com.auto.development.service.LogService;
import com.auto.development.common.model.XTableInfo;
import com.auto.development.common.util.JdbcUtil;
import com.auto.development.common.util.TableHelper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.toolkit.AopUtils;
import com.xin.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: log日志服务实现类。默认实现
 * @date 2019-05-24 22:20
 */
@Slf4j
public class DbLogServiceImpl implements LogService {

	private DataSource dataSource;
	private boolean dbLog;
	private boolean tableExist = false;

	public DbLogServiceImpl(DataSource dataSource, Environment environment) {
		this.dataSource = dataSource;
		this.dbLog = environment.getProperty("xin-cloud.db-log", Boolean.class, Boolean.FALSE);
	}

	@Override
	public void init() throws SQLException {
		if (!dbLog) {
			return;
		}
		Db db = Db.use(dataSource);
		XTableInfo xTableInfo = TableHelper.getTableInfo(SysLog.class);
		try (Connection connection = AopUtils.getTargetObject(dataSource).getConnection()) {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			String url = databaseMetaData.getURL();
			String dbName = JdbcUtil.getDbName(url);

			DbType dbType = com.baomidou.mybatisplus.extension.toolkit.JdbcUtils.getDbType(databaseMetaData.getURL());
			String sql = null;
			//判断表sys_log是否存在
			switch (dbType) {
				case MYSQL:
					sql = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = ? and TABLE_NAME = ?";
					RsHandler<String> rsh1 = (rs) -> {
						if (rs.next()) {
							return rs.getString(1);
						}
						return null;
					};
					String result = db.query(sql, rsh1, dbName, xTableInfo.getName());
					tableExist = !StringUtil.isEmpty(result);
					break;

				case POSTGRE_SQL:
					sql = "select count(*) from pg_class where relname = ?";
					RsHandler<Integer> rsh2 = (rs) -> {
						if (rs.next()) {
							return rs.getInt(1);
						}
						return 0;
					};
					int count = db.query(sql, rsh2, xTableInfo.getName());
					tableExist = count > 0;
					break;
				default:
					break;
			}
			if (!tableExist) {
				// 创建表以及修改主键为自增长
				List<String> sqls = new ArrayList<>(3);
				sqls.add(createTableSql(dbType));
				if (dbType == DbType.POSTGRE_SQL) {
					String sequenceSql = "CREATE SEQUENCE IF NOT EXISTS sys_log_id_seq increment by 1 minvalue 1 no maxvalue start with 1;";
					String alterSql = "alter table sys_log alter column id set default nextval('sys_log_id_seq');";
					sqls.add(sequenceSql);
					sqls.add(alterSql);
					tableExist = true;
				}
				db.tx((p) -> {
					p.executeBatch(sqls.toArray(new String[0]));
				});
			}
		}
	}

	@Override
	public R<Boolean> save(LogInfo logInfo) throws Exception {
		String json = JSON.toJSONString(logInfo);
		log.info(json);
		if (!dbLog) {
			return R.ok(true);
		}
		Db db = Db.use(dataSource);
		Entity entity = new Entity("sys_log").parseBean(logInfo, true, true);
		return R.ok(db.insert(entity) > 0);
	}


	public String createTableSql(DbType dbType) {
		switch (dbType) {
			case MYSQL:
				return "CREATE TABLE `sys_log` (\n"
						+ "  `id` bigint(64) NOT NULL AUTO_INCREMENT COMMENT '编号',\n"
						+ "  `type` char(1) DEFAULT '1' COMMENT '日志类型',\n"
						+ "  `title` varchar(255) DEFAULT '' COMMENT '日志标题',\n"
						+ "  `service_id` varchar(32) DEFAULT NULL COMMENT '服务ID',\n"
						+ "  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',\n"
						+ "  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n"
						+ "  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',\n"
						+ "  `remote_addr` varchar(255) DEFAULT NULL COMMENT '操作IP地址',\n"
						+ "  `user_agent` varchar(1000) DEFAULT NULL COMMENT '用户代理',\n"
						+ "  `request_uri` varchar(255) DEFAULT NULL COMMENT '请求URI',\n"
						+ "  `method` varchar(10) DEFAULT NULL COMMENT '操作方式',\n"
						+ "  `params` text COMMENT '操作提交的数据',\n"
						+ "  `time` mediumtext COMMENT '执行时间',\n"
						+ "  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记',\n"
						+ "  `exception` text COMMENT '异常信息',\n"
						+ "  `tenant_id` int(11) DEFAULT '0' COMMENT '所属租户',\n"
						+ "  PRIMARY KEY (`id`) USING BTREE,\n"
						+ "  KEY `sys_log_create_by` (`create_by`) USING BTREE,\n"
						+ "  KEY `sys_log_request_uri` (`request_uri`) USING BTREE,\n"
						+ "  KEY `sys_log_type` (`type`) USING BTREE,\n"
						+ "  KEY `sys_log_create_date` (`create_time`) USING BTREE\n"
						+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='日志表';";
			case POSTGRE_SQL:

				return "CREATE TABLE public.sys_log\n"
						+ "(\n"
						+ "    id bigint NOT NULL,\n"
						+ "    type character(1) COLLATE pg_catalog.\"default\",\n"
						+ "    title character varying(255) COLLATE pg_catalog.\"default\",\n"
						+ "    service_id character varying(32) COLLATE pg_catalog.\"default\",\n"
						+ "    create_by character varying(64) COLLATE pg_catalog.\"default\",\n"
						+ "    create_time timestamp without time zone,\n"
						+ "    update_time timestamp without time zone,\n"
						+ "    remote_addr character varying(255) COLLATE pg_catalog.\"default\",\n"
						+ "    user_agent character varying(1000) COLLATE pg_catalog.\"default\",\n"
						+ "    request_uri character varying(255) COLLATE pg_catalog.\"default\",\n"
						+ "    method character varying(10) COLLATE pg_catalog.\"default\",\n"
						+ "    params text COLLATE pg_catalog.\"default\",\n"
						+ "    \"time\" bigint,\n"
						+ "    del_flag character(1) COLLATE pg_catalog.\"default\",\n"
						+ "    exception text COLLATE pg_catalog.\"default\",\n"
						+ "    tenant_id integer,\n"
						+ "    CONSTRAINT sys_log_pkey PRIMARY KEY (id)\n"
						+ ")";
			default:
				return "";
		}
	}

}

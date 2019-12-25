package com.auto.development.service;

import com.auto.development.log.entity.LogInfo;
import com.baomidou.mybatisplus.extension.api.R;

import java.sql.SQLException;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: log日志服务接口
 * @date 2019-05-24 22:19
 */
public interface LogService {
	void init() throws SQLException;

	R<Boolean> save(LogInfo logInfo) throws Exception;
}

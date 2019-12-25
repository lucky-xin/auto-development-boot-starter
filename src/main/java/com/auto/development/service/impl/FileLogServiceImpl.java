package com.auto.development.service.impl;

import com.alibaba.fastjson.JSON;
import com.auto.development.log.entity.LogInfo;
import com.auto.development.service.LogService;
import com.baomidou.mybatisplus.extension.api.R;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: log日志服务实现类。默认实现
 * @date 2019-05-24 22:20
 */
@Slf4j
public class FileLogServiceImpl implements LogService {

	@Override
	public void init() throws SQLException {
	}

	@Override
	public R<Boolean> save(LogInfo logInfo) throws Exception {
		String json = JSON.toJSONString(logInfo);
		log.info(json);
		return R.ok(true);
	}

}

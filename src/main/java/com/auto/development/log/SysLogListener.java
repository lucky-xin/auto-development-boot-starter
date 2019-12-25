package com.auto.development.log;

import com.auto.development.log.entity.LogInfo;
import com.auto.development.service.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;

/**
 * @author luchaoxin
 * 异步监听日志事件
 */
@Slf4j
@AllArgsConstructor
public class SysLogListener {

	private final LogService logService;

	@Async
	@Order
	@EventListener(SysLogEvent.class)
	public void saveSysLog(SysLogEvent event) throws Exception {
		LogInfo logInfo = event.getLogInfo();
		logService.save(logInfo);
	}
}

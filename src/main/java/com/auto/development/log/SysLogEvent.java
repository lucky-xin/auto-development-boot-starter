package com.auto.development.log;

import com.auto.development.log.entity.LogInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author luchaoxin
 * 系统日志事件
 */
@Getter
@AllArgsConstructor
public class SysLogEvent {
	private final LogInfo logInfo;
}

package com.auto.development.log;

import com.auto.development.annotation.SysLog;
import com.auto.development.log.entity.LogInfo;
import com.auto.development.util.LogUtils;
import com.xin.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志使用spring event异步入库
 *
 * @author luchaoxin
 */
@Slf4j
@Aspect
@AllArgsConstructor
public class SysLogAspect {
	private final ApplicationEventPublisher publisher;

	@SneakyThrows
	@Around("@annotation(sysLog)")
	public Object around(ProceedingJoinPoint point, SysLog sysLog) {
		String strClassName = point.getTarget().getClass().getName();
		String strMethodName = point.getSignature().getName();
		log.debug("[类名]:{},[方法]:{}", strClassName, strMethodName);
		LogInfo logVo = null;
		try {
			logVo = LogUtils.getSysLog();
		} catch (Exception e) {
			logVo = new LogInfo();
			String className = point.getTarget().getClass().getName();
			String methodName = point.getSignature().getName();
			logVo.setDelFlag("0")
					.setCreateTime(LocalDateTime.now())
					.setMethod(className + "." + methodName)
					.setType("0");
		}
		logVo.setTitle(sysLog.value());
		// 发送异步日志事件
		Long startTime = System.currentTimeMillis();
		Object result = point.proceed();
		if (StringUtil.isEmpty(logVo.getParams()) || "{}".equals(logVo.getParams())) {
			logVo.setParams(Arrays.toString(point.getArgs()));
		}
		Long endTime = System.currentTimeMillis();
		logVo.setTime(endTime - startTime);
		publisher.publishEvent(new SysLogEvent(logVo));
		return result;
	}

}

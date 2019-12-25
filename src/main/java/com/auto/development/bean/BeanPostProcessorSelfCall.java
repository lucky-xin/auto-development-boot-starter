package com.auto.development.bean;

import com.auto.development.common.service.SelfBeanAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 用于设置自身aop的BeanPostProcessor
 * @date 2019-09-13 12:29
 */
public class BeanPostProcessorSelfCall implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SelfBeanAware) {
            ((SelfBeanAware) bean).setSelfObj(bean);
        }
        return bean;
    }
}

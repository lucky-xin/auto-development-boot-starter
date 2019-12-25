package com.auto.development.bean;

import com.xin.utils.DateUtil;
import org.springframework.core.convert.converter.Converter;

import java.util.Date;

/**
 * @version V1.0
 * @Description: 日期转换配置
 * @author Luchaoxin
 * @since 2019-05-07
 */

public class DateConverterConfig implements Converter<String, Date> {

    @Override
    public Date convert(String source) {
        try {
            return DateUtil.toDate(source);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式不对");
        }
    }
}
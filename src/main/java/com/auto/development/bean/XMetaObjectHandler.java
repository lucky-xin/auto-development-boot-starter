package com.auto.development.bean;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Date;
import java.util.Objects;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: MyBatis-plus自动填充
 * @date 2019-05-17 22:34
 */
@Slf4j
public class XMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        setTime(metaObject, "createTime");
        setTime(metaObject, "updateTime");
        setDelFlag(metaObject);
    }

    private void setDelFlag(MetaObject metaObject) {

        Class<?> clazz = getClazz(metaObject);
        if (Objects.nonNull(clazz)) {
            try {
                String delFlagFieldName = "delFlag";
                Field field = clazz.getDeclaredField(delFlagFieldName);
                Class<?> fieldClazz = field.getType();
                Object value = null;
                if (Integer.class.isAssignableFrom(fieldClazz)) {
                    value = 0;
                } else if (Long.class.isAssignableFrom(fieldClazz)) {
                    value = 0;
                } else if (String.class.isAssignableFrom(fieldClazz)) {
                    value = "0";
                }
                this.setFieldValByName("delFlag", value, metaObject);
            } catch (Exception e) {
                log.debug("not found field", e);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setTime(metaObject, "updateTime");
    }

    private void setTime(MetaObject metaObject, String fieldName) {
        Class<?> clazz = getClazz(metaObject);
        if (Objects.nonNull(clazz)) {
            Object value = null;
            try {
                Field field = clazz.getDeclaredField(fieldName);
                Class<?> fieldClazz = field.getType();
                if (Timestamp.class.isAssignableFrom(fieldClazz)) {
                    value = new Timestamp(System.currentTimeMillis());
                } else if (LocalTime.class.isAssignableFrom(fieldClazz)) {
                    value = LocalTime.now();
                } else if (Date.class.isAssignableFrom(fieldClazz)) {
                    value = new Date();
                }
            } catch (NoSuchFieldException e) {
                log.debug("not found field", e);
            }
            if (Objects.nonNull(value)) {
                this.setFieldValByName(fieldName, value, metaObject);
            }
        }
    }

    private Class<?> getClazz(MetaObject metaObject) {
        Class<?> clazz = null;
        try {
            clazz = metaObject.getGetterType("et");
        } catch (Exception ignore) {
            //ignore
        }

        if (Objects.isNull(clazz)) {
            Object object = metaObject.getOriginalObject();
            if (Objects.isNull(object)) {
                return null;
            }
            clazz = object.getClass();
        }
        return clazz;
    }
}

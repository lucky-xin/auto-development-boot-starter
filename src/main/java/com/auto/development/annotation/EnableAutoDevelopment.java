package com.auto.development.annotation;

import com.auto.development.config.AutoDevelopmentConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 自动化开发开关
 * @date 2019-05-10 22:29
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({AutoDevelopmentConfig.class})
public @interface EnableAutoDevelopment {

    String pojoPackage() default "";

    String mapperPackage() default "";

    String servicePackage() default "";

    String serviceImplPackage() default "";
}

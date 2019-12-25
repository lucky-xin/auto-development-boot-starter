package com.auto.development.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: Configuration properties
 * @date 2019-05-10 21:25
 */
@Data
@ConfigurationProperties(prefix = "xin.auto-dev")
public class AutoGenDevProperties {

    private boolean enableSyncPojo = false;

    /**
     * sync-pojo-package
     */
    private String pojoPackage;

    /**
     * mapper-package
     */
    private String mapperPackage;

    /**
     * service-package
     */
    private String servicePackage;

    /**
     * service-impl-package
     */
    private String serviceImplPackage;
}

package ${serviceImplPackage};

import ${entryPackage}.${entryName};
import ${mapperPackage}.${mapperName};
import ${servicePackage}.${serviceName};
import biz.datainsights.automation.development.common.service.impl.XServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.log4j.*;

/**
 * <p>
 * ${comment!} 服务实现类
 * </p>
 *
 * @author ${author}
 * @since ${date}
 */
@Log4j
@Service
public class ${serviceImplName} extends XServiceImpl<${mapperName}, ${entryName}> implements ${serviceName} {

}


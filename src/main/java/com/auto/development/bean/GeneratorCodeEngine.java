package com.auto.development.bean;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.config.ConstVal;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 代码生成引擎
 * @date 2019-05-11 16:32
 */
@Slf4j
public class GeneratorCodeEngine {

    private Configuration configuration;

    public GeneratorCodeEngine() {
        configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setDefaultEncoding(ConstVal.UTF8);
        configuration.setClassForTemplateLoading(FreemarkerTemplateEngine.class, StringPool.SLASH);
    }

    public void writer(Map<String, Object> objectMap, String templatePath, String outputFile) throws Exception {
        Template template = configuration.getTemplate(templatePath);
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, ConstVal.UTF8)) {
            template.process(objectMap, outputStreamWriter);

        }
        log.debug("模板:" + templatePath + ";  文件:" + outputFile);
    }

    public void generatorSimpleMapperXml(Map<String, Object> objectMap, String outputFile) throws Exception {
        String templatePath = "/templates/x-mapper.xml.ftl";
        writer(objectMap, templatePath, outputFile);
    }

    public void generatorSimpleMapper(Map<String, Object> objectMap, String outputFile) throws Exception {
        String templatePath = "/templates/x-mapper.java.ftl";
        writer(objectMap, templatePath, outputFile);
    }

    public void generatorSimpleService(Map<String, Object> objectMap, String outputFile) throws Exception {
        String templatePath = "/templates/x-service.java.ftl";
        writer(objectMap, templatePath, outputFile);
    }

    public void generatorSimpleServiceImpl(Map<String, Object> objectMap, String outputFile) throws Exception {
        String templatePath = "/templates/x-serviceImpl.java.ftl";
        writer(objectMap, templatePath, outputFile);
    }

}

package com.vita.config;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import com.vita.core.controller.BaseController;
import com.vita.core.entity.BaseEntity;
import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.utils.DbUtil;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-03-04  14:02:55
 * @Description: 代码生成器
 * @Version: 1.0
 */
public class CodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

    /**
     * 生成器配置 - DB HOST
     */
    private static final String DB_HOST = "106.54.205.33";
    /**
     * 生成器配置 - DB PORT
     */
    private static final Integer DB_PORT = 3306;
    /**
     * 生成器配置 - DB USER
     */
    private static final String DB_USER = "root";
    /**
     * 生成器配置 - DB PASSWORD
     */
    private static final String DB_PASSWORD = "WQO6D9ax#f*QPJup";
    /**
     * 生成器配置 - DB NAME
     */
    private static final String DB_NAME = "be_vita";
    /**
     * 生成器配置 - CODE AUTHOR
     */
    private static final String AUTHOR = "znk";
    /**
     * 生成器配置 - PROJECT NAME
     */
    private static final String PROJECT_NAME = "vita";

    /**
     * 生成器配置 - TABLE PREFIX
     */
    private static final String TABLE_PREFIX = "tb_";

    /**
     * 生成器配置 - TABLE NAME
     */
    private static final String[] TABLE_NAMES = {"flow_definition","flow_node","flow_skip","flow_instance","flow_task","flow_his_task","flow_user","flow_form"};

    /**
     * 默认目标模块（兼容旧配置）
     */
    private static final String DEFAULT_TARGET_MODULE = "vita-workflow";

    /**
     * 默认控制器模块（与当前 sysUser 模块保持一致）
     */
    private static final String DEFAULT_CONTROLLER_MODULE = "vita-workflow";

    /**
     * 默认业务模块（用于父包、路由和 mapper 目录）
     */
    private static final String DEFAULT_BUSINESS_MODULE = "workflow";

    /**
     * 默认领域模块（用于 packageConfig.moduleName）
     */
    private static final String DEFAULT_DOMAIN_MODULE = "flow";

    /**
     * 默认基础包（兼容旧配置 com.vita.system + sysUser）
     */
    private static final String DEFAULT_BASE_PACKAGE = "com.vita";

    /**
     * 生成器配置 - DIR NAME
     */
    public static void main(String[] args) {
        ModuleConfig moduleConfig = ModuleConfig.load();
        DataSourceConfig.Builder dataSourceConfig = DbUtil.getDataSourceConfig(DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD);
        logger.info("模块化生成配置: {}", moduleConfig);

        // 输出目录和包配置
        String domainOutputJavaDir = moduleConfig.targetModule() + "/src/main/java";
        String controllerOutputJavaDir = moduleConfig.controllerModule() + "/src/main/java";
        // mapper.xml 输出目录，放在 resources 目录下，按照业务模块进行分组
        String outputMapperDir = moduleConfig.targetModule() + "/src/main/resources/mapper/" + moduleConfig.businessModule();
        // 领域包配置，格式为 basePackage.businessModule.domainModule，如 com.vita.system.sysUser
        String domainPackagePrefix = moduleConfig.businessModule() + "." + moduleConfig.domainModule();
        String modulePackage = moduleConfig.basePackage() + "." + domainPackagePrefix;
        // 控制器包配置，格式为 basePackage.controller.businessModule，如 com.vita.controller.system
        String controllerPackage = "controller." + moduleConfig.businessModule();
        // 控制器输出目录需要指定到包路径，否则会落在 java 根目录
        String controllerOutputDir = controllerOutputJavaDir + "/" + moduleConfig.basePackage().replace(".", "/")
                + "/" + controllerPackage.replace(".", "/");

        // 1.配置数据源
        FastAutoGenerator.create(dataSourceConfig)
                // 2、全局配置
                .globalConfig(builder -> builder
                        // 输出目录
                        .outputDir(domainOutputJavaDir)
                        // 设置作者
                        .author(AUTHOR)
                        .disableOpenDir()
                        // .enableKotlin() //开启 kotlin 模式 默认false
                        // .enableSwagger()   // 开启 swagger 模式 默认false
                        .dateType(DateType.TIME_PACK) // 设置时间类型对应策略（默认值），数据库时间类型 到 实体类时间类型 对应策略
                        .commentDate(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))) // 设置注释日期（默认值）,example: () -> LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)
                // 3、包配置
                .packageConfig(builder -> {
                    Map<OutputFile, String> pathInfo = new HashMap<>();
                    pathInfo.put(OutputFile.xml, outputMapperDir);
                    pathInfo.put(OutputFile.controller, controllerOutputDir);

                    builder.parent(moduleConfig.basePackage())
                            .moduleName("")
                            .entity(domainPackagePrefix + ".entity")
                            .service(domainPackagePrefix + ".service")
                            .serviceImpl(domainPackagePrefix + ".service.impl")
                            .mapper(domainPackagePrefix + ".mapper")
                            .xml("mapper") // 设置 Mapper XML 包名（默认值）
                            .controller(controllerPackage)
                            .pathInfo(pathInfo);
                })
                //4、策略配置
                .strategyConfig(builder -> {
                    builder.addInclude(TABLE_NAMES) // 设置需要生成的数据表名
                            .addTablePrefix(TABLE_PREFIX.split(",")) // 设置过滤表前缀

                            //4.1、实体类策略配置
                            .entityBuilder()
                            .enableFileOverride() // 覆盖entity
                            .superClass(BaseEntity.class)
                            //.superClass("com.wiki.core.entity.BaseEntity") // 设置父类实体类
                            .disableSerialVersionUID() // 禁用生成 serialVersionUID 默认值 true
                            //.disableSerialVersionUID()  // 禁用生成 serialVersionUID 默认值 true
                            //.enableLombok() // 开启 Lombok 默认值:false
                            .enableLombok(new ClassAnnotationAttributes("@Data", "lombok.Data")) // 开启lombok模型（默认 false），会把注解属性都加入进去，无论是否启用GlobalConfig的isKotlin()，同时get,set,toString都将不会生成，需自行控制添加
                            .enableTableFieldAnnotation()       // 开启生成实体时生成字段注解 默认值 false
                            //.logicDeleteColumnName("deleted")   // 逻辑删除字段名
                            .naming(NamingStrategy.underline_to_camel)  //数据库表映射到实体的命名策略：下划线转驼峰命
                            .columnNaming(NamingStrategy.underline_to_camel)    // 数据库表字段映射到实体的命名策略：下划线转驼峰命
                            .javaTemplate("/templates/mybatis-plus-v1/entity.java.vm")
                            // .addSuperEntityColumns("creator", "create_time", "updater", "update_time")
                            // .addTableFills(
                            //  new Column("creator", FieldFill.INSERT),
                            //  new Column("updater", FieldFill.INSERT_UPDATE)
                            // )   // 添加表字段填充，"create_time"字段自动填充为插入时间，"modify_time"字段自动填充为插入修改时间
                            .formatFileName("%s")

                            //4.2、Mapper策略配置
                            .mapperBuilder()
                            .enableFileOverride() // 覆盖mapper
                            .superClass(BaseMapperX.class)   // 设置父类
                            .mapperAnnotation(Mapper.class)      // 开启 @Mapper 注解
                            .mapperTemplate("/templates/mybatis-plus-v1/mapper.java.vm")
                            .mapperXmlTemplate("/templates/mybatis-plus-v1/mapper.xml.vm")
                            // .enableBaseResultMap() //启用 BaseResultMap 生成
                            .formatMapperFileName("%sMapper")   // 格式化 mapper 文件名称
                            .formatXmlFileName("%sMapper") // 格式化 Xml 文件名称

                            //4.3、service 策略配置
                            .serviceBuilder()
                            .enableFileOverride() // 覆盖service
                            .serviceTemplate("/templates/mybatis-plus-v1/service.java.vm")
                            .serviceImplTemplate("/templates/mybatis-plus-v1/serviceImpl.java.vm")
                            .formatServiceFileName("I%sService") // 格式化 service 接口文件名称，%s进行匹配表名，如 UserService
                            .formatServiceImplFileName("%sServiceImpl") // 格式化 service 实现类文件名称，%s进行匹配表名，如 UserServiceImpl

                            //4.4、Controller策略配置
                            .controllerBuilder()
                            .enableFileOverride() // 覆盖controller
                            .template("/templates/mybatis-plus-v1/controller.java.vm")
                            .enableRestStyle()  // 开启生成 @RestController 控制器
                            .superClass(BaseController.class)
                            //.enableHyphenStyle() //开启驼峰转连字符 默认false
                            .formatFileName("%sController"); // 格式化 Controller 类文件名称，%s进行匹配表名，如 UserController
                })
                //6、自定义配置
                .injectionConfig(consumer -> {
                    Map<String, Object> customMap = new HashMap<>();
                    customMap.put("projectName", PROJECT_NAME);
                    customMap.put("basePackage", moduleConfig.basePackage());
                    customMap.put("moduleRoute", moduleConfig.routePrefix());
                    customMap.put("moduleName", moduleConfig.businessModule());
                    customMap.put("modulePackage", modulePackage);
                    customMap.put("entityPackage", modulePackage + ".entity");
                    customMap.put("dtoPackage", modulePackage + ".dto");
                    customMap.put("voPackage", modulePackage + ".vo");
                    customMap.put("modelPackage", modulePackage + ".model");
                    customMap.put("commonPackage", moduleConfig.basePackage() + ".core");
                    // 保留旧键，避免模板遗漏导致回退失败
                    customMap.put("dto", modulePackage + ".dto");
                    customMap.put("vo", modulePackage + ".vo");
                    customMap.put("model", modulePackage + ".model");
                    consumer.customMap(customMap);

                    List<CustomFile> customFiles = new ArrayList<>();
                    // 增加dto类
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".dto").fileName("CreateDto.java")
                            .templatePath("/templates/mybatis-plus-v1/dto/createDto.java.vm").enableFileOverride().build());
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".dto").fileName("UpdateDto.java")
                            .templatePath("/templates/mybatis-plus-v1/dto/updateDto.java.vm").enableFileOverride().build());
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".dto").fileName("DeletedDto.java")
                            .templatePath("/templates/mybatis-plus-v1/dto/deletedDto.java.vm").enableFileOverride().build());
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".dto").fileName("SearchDto.java")
                            .templatePath("/templates/mybatis-plus-v1/dto/searchDto.java.vm").enableFileOverride().build());
                    // 增加vo类
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".vo").fileName("DetailVo.java")
                            .templatePath("/templates/mybatis-plus-v1/vo/detailVo.java.vm").enableFileOverride().build());
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".vo").fileName("ListVo.java")
                            .templatePath("/templates/mybatis-plus-v1/vo/listVo.java.vm").enableFileOverride().build());
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".vo").fileName("PageVo.java")
                            .templatePath("/templates/mybatis-plus-v1/vo/pageVo.java.vm").enableFileOverride().build());

                    // 增加model类
                    customFiles.add(new CustomFile.Builder().packageName(domainPackagePrefix + ".model").fileName("Model.java")
                            .templatePath("/templates/mybatis-plus-v1/model/model.java.vm").enableFileOverride().build());
                    // 增加convert类
                    consumer.customFile(customFiles);

                })

                // 6、选择模板引擎
                .templateEngine(new VelocityTemplateEngine())
                // 7、执行
                .execute();
        logger.info("代码生成完成。");
    }

    /**
     * 读取配置的工具方法，优先级：系统属性 > 环境变量 > 默认值
     *
     * @param propertyKey  系统属性键
     * @param envKey       环境变量键
     * @param defaultValue 默认值，当系统属性和环境变量都未设置时使用
     * @return 最终的配置值，经过 trim 处理，确保没有多余的空白
     */
    private static String readConfig(String propertyKey, String envKey, String defaultValue) {
        String config = System.getProperty(propertyKey);
        if (config == null || config.isBlank()) {
            config = System.getenv(envKey);
        }
        if (config == null || config.isBlank()) {
            return defaultValue;
        }
        return config.trim();
    }

    /**
     * 规范化路由前缀，去除多余的斜杠，并确保不以斜杠开头或结尾
     *
     * @param routePrefix 原始路由前缀
     * @param fallback    当路由前缀为空时的回退值，通常使用业务模块名
     * @return 规范化后的路由前缀
     */
    private static String normalizeRoutePrefix(String routePrefix, String fallback) {
        String normalized = routePrefix == null ? "" : routePrefix.trim();
        if (normalized.isEmpty()) {
            normalized = fallback;
        }
        normalized = normalized.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 模块配置记录类，封装了生成器的核心配置项，并提供加载逻辑从系统属性或环境变量中读取配置
     *
     * @param targetModule     目标模块，生成代码的输出目录，默认为 "vita-service"
     * @param controllerModule 控制器模块，控制器输出目录，默认为 "vita-admin"
     * @param businessModule   业务模块，用于包结构和路由前缀，默认为 "system"
     * @param domainModule     领域模块，用于领域包名，默认为 "sysUser"
     * @param basePackage      基础包，用于生成代码的父包，默认为 "com.vita"
     * @param routePrefix      路由前缀，用于生成控制器的路由，默认为业务模块名，并会进行规范化处理
     */
    private record ModuleConfig(String targetModule, String controllerModule, String businessModule,
                                String domainModule, String basePackage, String routePrefix) {

        private static ModuleConfig load() {
            String targetModule = readConfig("vita.codegen.targetModule", "VITA_CODEGEN_TARGET_MODULE", DEFAULT_TARGET_MODULE);
            String controllerModule = readConfig("vita.codegen.controllerModule", "VITA_CODEGEN_CONTROLLER_MODULE", DEFAULT_CONTROLLER_MODULE);
            String businessModule = readConfig("vita.codegen.businessModule", "VITA_CODEGEN_BUSINESS_MODULE", DEFAULT_BUSINESS_MODULE);
            String domainModule = readConfig("vita.codegen.domainModule", "VITA_CODEGEN_DOMAIN_MODULE", DEFAULT_DOMAIN_MODULE);
            String basePackage = readConfig("vita.codegen.basePackage", "VITA_CODEGEN_BASE_PACKAGE", DEFAULT_BASE_PACKAGE);
            String routePrefix = normalizeRoutePrefix(
                    readConfig("vita.codegen.routePrefix", "VITA_CODEGEN_ROUTE_PREFIX", businessModule),
                    businessModule
            );
            return new ModuleConfig(
                    targetModule.trim(),
                    controllerModule.trim(),
                    businessModule.trim(),
                    domainModule.trim(),
                    basePackage.trim(),
                    routePrefix
            );
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.ROOT,
                    "targetModule=%s, controllerModule=%s, businessModule=%s, domainModule=%s, basePackage=%s, routePrefix=%s",
                    targetModule, controllerModule, businessModule, domainModule, basePackage, routePrefix
            );
        }
    }

}

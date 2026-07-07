package com.vita.workflow.config;

import com.vita.workflow.handler.WorkflowPermissionHandler;
import com.vita.workflow.service.IWorkflowDefinitionService;
import com.vita.workflow.support.WorkflowLoginUserSupport;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.invoker.FrameInvoker;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.config
 * @Author: Codex
 * @CreateTime: 2026-07-02
 * @Description: Warm-Flow 一期配置
 * @Version: 1.0
 */
@Configuration
public class WorkflowConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowConfiguration.class);

    private static final Set<String> WARNED_MISSING_TYPES = ConcurrentHashMap.newKeySet();

    private static final Set<String> WARNED_CONFLICT_TYPES = ConcurrentHashMap.newKeySet();

    private static final Set<String> WARNED_ERROR_TYPES = ConcurrentHashMap.newKeySet();

    /**
     * 注册当前办理人权限处理器，复用 be-vita 登录上下文。
     */
    @Bean
    public PermissionHandler workflowPermissionHandler(WorkflowLoginUserSupport workflowLoginUserSupport) {
        return new WorkflowPermissionHandler(workflowLoginUserSupport);
    }

    /**
     * 应用上下文初始化完成后，显式重绑 Warm-Flow 的 Bean/配置获取函数。
     */
    @Bean
    public SmartInitializingSingleton workflowFrameInvokerBridge(ApplicationContext applicationContext,
                                                                 Environment environment) {
        return () -> {
            FrameInvoker.setBeanFunction(beanType -> resolveBean(applicationContext, beanType));
            FrameInvoker.setCfgFunction(environment::getProperty);
            logWorkflowSpiSelfCheck(applicationContext);
        };
    }

    private void logWorkflowSpiSelfCheck(ApplicationContext applicationContext) {
        LOGGER.info("Warm-Flow SPI self-check: HandlerSelectService={}, CategoryService={}, PermissionHandler={}, IWorkflowDefinitionService={}",
                describeSelection(applicationContext, HandlerSelectService.class),
                describeSelection(applicationContext, CategoryService.class),
                describeSelection(applicationContext, PermissionHandler.class),
                describeSelection(applicationContext, IWorkflowDefinitionService.class));
    }

    private <T> T resolveBean(ApplicationContext applicationContext, Class<T> beanType) {
        try {
            BeanSelection<T> selection = selectBean(applicationContext, beanType);
            if (selection == null) {
                logMissingBean(beanType);
                return null;
            }
            if (selection.multiple()) {
                logMultipleBeans(beanType, selection);
            }
            return selection.bean();
        } catch (Exception ex) {
            logBeanError(beanType, ex);
            return null;
        }
    }

    private String describeSelection(ApplicationContext applicationContext, Class<?> beanType) {
        try {
            BeanSelection<?> selection = selectBean(applicationContext, beanType);
            if (selection == null) {
                return "missing";
            }
            if (selection.multiple()) {
                return selection.beanName() + " <- " + selection.candidates();
            }
            return selection.beanName();
        } catch (Exception ex) {
            return "error(" + ex.getClass().getSimpleName() + ")";
        }
    }

    private <T> BeanSelection<T> selectBean(ApplicationContext applicationContext, Class<T> beanType) {
        Map<String, T> beanMap = applicationContext.getBeansOfType(beanType);
        if (beanMap.isEmpty()) {
            return null;
        }
        if (beanMap.size() == 1) {
            Map.Entry<String, T> entry = beanMap.entrySet().iterator().next();
            return new BeanSelection<>(entry.getKey(), entry.getValue(), List.of(entry.getKey()));
        }

        List<String> candidateNames = new ArrayList<>(beanMap.keySet());
        String selectedBeanName = selectPreferredBeanName(applicationContext, candidateNames);
        return new BeanSelection<>(selectedBeanName, beanMap.get(selectedBeanName), candidateNames);
    }

    private String selectPreferredBeanName(ApplicationContext applicationContext, List<String> candidateNames) {
        List<String> primaryCandidates = candidateNames.stream()
                .filter(beanName -> applicationContext.findAnnotationOnBean(beanName, Primary.class) != null)
                .sorted()
                .toList();
        if (primaryCandidates.size() == 1) {
            return primaryCandidates.get(0);
        }

        String workflowCandidate = selectByPackagePrefix(applicationContext, candidateNames, "com.vita.workflow");
        if (workflowCandidate != null) {
            return workflowCandidate;
        }

        String vitaCandidate = selectByPackagePrefix(applicationContext, candidateNames, "com.vita");
        if (vitaCandidate != null) {
            return vitaCandidate;
        }

        List<String> sortedCandidates = new ArrayList<>(candidateNames);
        Collections.sort(sortedCandidates);
        return sortedCandidates.get(0);
    }

    private String selectByPackagePrefix(ApplicationContext applicationContext, List<String> candidateNames, String packagePrefix) {
        List<String> packageCandidates = candidateNames.stream()
                .filter(beanName -> {
                    Class<?> beanClass = applicationContext.getType(beanName);
                    return beanClass != null && beanClass.getName().startsWith(packagePrefix);
                })
                .sorted()
                .toList();
        return packageCandidates.size() == 1 ? packageCandidates.get(0) : null;
    }

    private void logMissingBean(Class<?> beanType) {
        if (WARNED_MISSING_TYPES.add(beanType.getName())) {
            LOGGER.warn("Warm-Flow bridge cannot resolve bean type: {}", beanType.getName());
        }
    }

    private void logMultipleBeans(Class<?> beanType, BeanSelection<?> selection) {
        if (WARNED_CONFLICT_TYPES.add(beanType.getName())) {
            LOGGER.warn("Warm-Flow bridge found multiple beans for type {}, selected '{}' from candidates {}",
                    beanType.getName(), selection.beanName(), selection.candidates());
        }
    }

    private void logBeanError(Class<?> beanType, Exception ex) {
        if (WARNED_ERROR_TYPES.add(beanType.getName())) {
            LOGGER.error("Warm-Flow bridge failed to resolve bean type: {}", beanType.getName(), ex);
        }
    }

    private record BeanSelection<T>(String beanName, T bean, List<String> candidates) {

        private boolean multiple() {
            return candidates.size() > 1;
        }
    }
}

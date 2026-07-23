package com.vita.workflow.reimbursement;

import com.vita.workflow.reimbursement.listener.ReimbursementWorkflowEventListener;
import com.vita.workflow.reimbursement.service.impl.WorkflowReimbursementServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement
 * @Author: znk
 * @CreateTime: 2026-07-23
 * @Description: 报销工作流总开关默认值测试
 * @Version: 1.0
 */
class WorkflowEnabledDefaultTest {

    /**
     * 报销服务和事件监听器应随工作流总开关默认开启。
     */
    @Test
    void reimbursementComponentsShouldBeEnabledByDefault() {
        assertEnabledByDefault(WorkflowReimbursementServiceImpl.class);
        assertEnabledByDefault(ReimbursementWorkflowEventListener.class);
    }

    private void assertEnabledByDefault(Class<?> componentType) {
        ConditionalOnProperty condition = componentType.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition).as(componentType.getSimpleName()).isNotNull();
        assertThat(condition.prefix()).isEqualTo("vita.workflow");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }
}

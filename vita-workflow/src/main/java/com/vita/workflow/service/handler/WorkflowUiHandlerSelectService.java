package com.vita.workflow.service.handler;

import com.vita.constant.AuthConstants;
import com.vita.workflow.constant.WorkflowConstants;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.dromara.warm.flow.ui.vo.HandlerAuth;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Warm-Flow 设计器办理人选择实现。
 *
 * @author Codex
 */
@Service
public class WorkflowUiHandlerSelectService implements HandlerSelectService {

    private static final String REIMBURSE_GROUP_NAME = "报销审批角色";

    private static final String SYSTEM_GROUP_NAME = "系统虚拟角色";

    private static final List<RoleOption> ROLE_OPTIONS = List.of(
            new RoleOption(WorkflowConstants.FINANCE_APPROVER_ROLE_CODE, "财务审批", REIMBURSE_GROUP_NAME),
            new RoleOption(WorkflowConstants.GENERAL_AFFAIRS_APPROVER_ROLE_CODE, "综合管理部审批", REIMBURSE_GROUP_NAME),
            new RoleOption(WorkflowConstants.GENERAL_MANAGER_APPROVER_ROLE_CODE, "总经理审批", REIMBURSE_GROUP_NAME),
            new RoleOption(AuthConstants.SUPER_ADMIN_ROLE_CODE, "超级管理员", SYSTEM_GROUP_NAME)
    );

    @Override
    public List<String> getHandlerType() {
        return List.of(WorkflowConstants.ROLE_HANDLER_TYPE);
    }

    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        if (query != null && query.getHandlerType() != null
                && !WorkflowConstants.ROLE_HANDLER_TYPE.equals(query.getHandlerType())) {
            return getResult(List.of(), 0);
        }

        List<HandlerAuth> filteredRoles = ROLE_OPTIONS.stream()
                .filter(role -> matches(query, role))
                .map(this::toHandlerAuth)
                .toList();

        int total = filteredRoles.size();
        return getResult(page(filteredRoles, query), total);
    }

    private HandlerAuth toHandlerAuth(RoleOption role) {
        return new HandlerAuth()
                .setStorageId(WorkflowConstants.buildRolePermission(role.roleCode()))
                .setHandlerCode(role.roleCode())
                .setHandlerName(role.roleName())
                .setGroupName(role.groupName());
    }

    private boolean matches(HandlerQuery query, RoleOption role) {
        if (query == null) {
            return true;
        }
        return containsIgnoreCase(role.roleCode(), query.getHandlerCode())
                && containsIgnoreCase(role.roleName(), query.getHandlerName());
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private List<HandlerAuth> page(List<HandlerAuth> roles, HandlerQuery query) {
        if (query == null || query.getPageNum() == null || query.getPageSize() == null || query.getPageSize() <= 0) {
            return roles;
        }
        int fromIndex = Math.max(query.getPageNum() - 1, 0) * query.getPageSize();
        if (fromIndex >= roles.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + query.getPageSize(), roles.size());
        return roles.subList(fromIndex, toIndex);
    }

    private record RoleOption(String roleCode, String roleName, String groupName) {
    }
}

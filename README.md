# Modular Hub

集团统一门户与部门业务平台。前端、后端、设计原型和跨工程文档统一由本仓库管理，各应用仍可独立构建和部署。

## 目录

| 目录 | 内容 |
| --- | --- |
| `backend/` | Java 平台后端与四个部门业务应用；开发规范见 `backend/AGENTS.md` |
| `frontend/` | Vben 前端，当前使用 `apps/web-ele` |
| `docs/` | 跨工程需求、架构、交付与 Git 管理说明 |
| `procurement-prototype/` | 可运行的采购管理交互原型 |
| `procurement-whiteboard.html` | 页面设计白板 |

## 主要入口

- [后端说明](backend/README.md)
- [前端说明](frontend/README.md)
- [后端需求与架构导航](backend/docs/README.md)
- [跨工程文档导航](docs/README.md)
- [统一仓库与历史迁移](docs/git-management.md)

## 日常 Git 操作

在本目录提交和管理分支。`backend/`、`frontend/` 是普通目录，不再拥有独立 Git 仓库，也不是 submodule。

```sh
git status
git diff --stat
git add backend/ frontend/ docs/
git diff --cached
git commit -m "feat: describe the change"
```

按实际改动选择暂存路径；一次功能变更可以同时包含前后端和文档。构建命令仍在各工程目录执行，见各自 README。仓库统一不要求所有应用同时发布。

本地数据库、Redis 等配置使用忽略文件；示例配置可以提交。依赖目录、构建产物、日志和 `.local-backups/` 不纳入版本控制。

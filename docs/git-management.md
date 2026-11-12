# 统一 Git 仓库管理

## 迁移决策（2026-11-13）

整个 `modular-hub` 使用一个 Git 仓库，直接管理后端、前端、跨工程文档和设计原型。`backend/` 与 `frontend/` 不再作为独立仓库或 submodule 使用。独立部署由应用构建与发布边界决定，不要求拆分 Git 仓库。

迁移导入的基线：

| 工程 | 原分支 | 原提交 |
| --- | --- | --- |
| 后端 | `codex/base-framework` | `1834b036df14c6824c8527b54682cfd538ec2831` |
| 前端 | `main` | `b1583f72ac19e36128ff96b9b7f8265cf2bb081f` |

统一仓库的当前开发分支继续使用 `codex/base-framework`，`main` 用作主分支。迁移时保留后端尚未提交的鉴权改造，不把这些改造混入仓库迁移提交；首次发布到新远程前将鉴权改造单独提交。前端迁移前为干净工作区。

## 历史保留方式

- 使用不压缩历史的 subtree 导入，把两个基线分别放到 `backend/`、`frontend/`。
- 原始提交对象、作者、时间、提交消息与哈希保持不变；历史文件路径仍是原仓库中的路径，不重写整个历史。
- 原有分支、标签和远程分支引用以 `archive/backend/*`、`archive/frontend/*` 标签归档，避免同名分支冲突；历史分支中未合入当前基线的提交也保留。
- 原仓库 Git 元数据、完整 bundle、未提交差异与未跟踪文件备份放在 `.local-backups/git/` 的迁移批次目录中，该目录仅保留在本机且被忽略。
- Git bundle 用于恢复提交历史；未提交文件与仓库配置需要配合元数据和工作区备份恢复。[Git 官方 bundle 文档](https://git-scm.com/docs/git-bundle)

查看原有历史：

```sh
git log --graph --oneline --all
git tag -l 'archive/*'
git log archive/backend/heads/codex/base-framework
git log archive/frontend/heads/main
```

归档标签仅用于定位历史，不要为了查看旧提交直接在有未提交修改的当前目录切换。恢复整个迁移前仓库时，应先另存当前工作区，再在独立目录使用 bundle 与备份重建。

## 提交与协作

- 项目根目录负责 Git 状态、分支、暂存与提交；在子目录执行 Git 也会定位到同一个根仓库。
- 后端工程规则仍在 `backend/AGENTS.md`，Java/Maven 命令仍从 `backend/` 执行；前端构建仍从 `frontend/` 执行。
- `.gitignore` 分层生效：根目录处理公共忽略规则，各子目录继续维护各自规则。
- 业务代码、文档、SQL 与配置示例可以同一次提交；真实数据库/Redis 配置、依赖、构建产物和备份不提交。
- 后续 CI 可按路径选择构建和发布范围，部门应用可以继续独立发布。

## 远程仓库

统一远程仓库：[Askwayer/modular-hub](https://github.com/Askwayer/modular-hub)。根目录 `origin` 使用 `https://github.com/Askwayer/modular-hub.git`，原前后端远程仓库不受影响。

首次发布包括 `main`、`codex/base-framework` 和 `archive/*` 标签，两个分支在首次发布时指向同一提交。后续在开发分支完成变更、验证后合入 `main`。

`archive/*` 标签保留原仓库中未合并的历史分支；只推当前分支不会上传那些仅由归档标签引用的提交。日常更新推送当前开发分支即可，无需反复创建或改写这些归档标签。

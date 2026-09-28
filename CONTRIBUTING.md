# 贡献指南

感谢关注本项目。欢迎通过 Issue / Pull Request 参与改进。

> English notes: please open PRs against the **default branch of the platform you forked** — GitHub uses `main`, Gitee uses `master`.

## 开发前准备

1. Fork 本仓库（[GitHub](https://github.com/Misterzb/personal-tech-blog) 或 [Gitee](https://gitee.com/bo_live/personal-tech-blog)）
2. 基于**该平台仓库的主分支**创建功能分支：  
   - GitHub：`git checkout main && git checkout -b feature/your-topic`  
   - Gitee：`git checkout master && git checkout -b feature/your-topic`
3. 按 [README](README.md) / [README_EN.md](README_EN.md) 完成本地环境

## 分支约定

| 平台 | 主分支 | 说明 |
|------|--------|------|
| GitHub | `main` | GitHub 仓库默认分支 |
| Gitee | `master` | Gitee 仓库默认分支 |
| 任意 | `feature/*` | 新功能 |
| 任意 | `fix/*` | 缺陷修复 |

两平台主分支应保持内容同步；维护者会定期双向推送。

## 提交说明

- 使用简洁的中文或英文提交信息，说明「为什么改」
- 示例：`fix: 文章详情侧栏滚动时保持可见`
- 不要提交密钥、`.env`、`application-local.yml`、上传文件、`node_modules`、`target`、`dist`

## Pull Request

1. 确保本地可编译/可运行：后端 `mvn -q -DskipTests package`，前台/后台 `npm run build`
2. 描述改动动机、影响范围、自测步骤
3. 若涉及数据库，附上 SQL 变更（`deploy/sql/`）说明
4. **向你所在平台的主分支发起 PR**（GitHub → `main`，Gitee → `master`）

## 问题反馈

提交 Issue 时请尽量包含：

- 环境（OS、JDK、Node、MySQL 版本）
- 复现步骤
- 期望行为与实际行为
- 相关日志（请脱敏）

## 行为准则

参与本项目即表示同意遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

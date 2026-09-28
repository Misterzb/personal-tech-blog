# 贡献指南

感谢关注本项目。欢迎通过 Issue / Pull Request 参与改进。

## 开发前准备

1. Fork 本仓库（GitHub 或 Gitee 均可）
2. 基于 `develop` 创建功能分支：`git checkout -b feature/your-topic`
3. 按 [README](README.md) 完成本地环境（MySQL、后端、前台、后台）

## 分支约定

| 分支 | 用途 |
|------|------|
| `develop` | 日常开发主分支（默认推送目标） |
| `main` / `master` | 稳定发布（如有需要再维护） |
| `feature/*` | 新功能 |
| `fix/*` | 缺陷修复 |

## 提交说明

- 使用简洁的中文或英文提交信息，说明「为什么改」
- 示例：`fix: 文章详情侧栏滚动时保持可见`
- 不要提交密钥、`.env`、`application-local.yml`、上传文件、`node_modules`、`target`、`dist`

## Pull Request

1. 确保本地可编译/可运行：后端 `mvn -q -DskipTests package`，前台/后台 `npm run build`
2. 描述改动动机、影响范围、自测步骤
3. 若涉及数据库，附上 SQL 变更（`deploy/sql/`）说明
4. 优先向 `develop` 发起 PR

## 问题反馈

提交 Issue 时请尽量包含：

- 环境（OS、JDK、Node、MySQL 版本）
- 复现步骤
- 期望行为与实际行为
- 相关日志（请脱敏）

## 行为准则

参与本项目即表示同意遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

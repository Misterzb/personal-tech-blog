# 安全策略

## 支持的版本

当前以 `develop` 分支为活跃开发线，安全修复优先合入该分支。

## 报告漏洞

请**不要**在公开 Issue 中披露未修复的安全漏洞。

建议通过以下方式私下联系维护者：

- GitHub：仓库 [Security Advisories](https://github.com/Misterzb/personal-tech-blog/security)（若已开启）或私信维护者
- Gitee：通过仓库私信 / 工单联系维护者

报告时请尽量包含：

- 影响组件（后端 API、前台、后台、部署配置等）
- 复现步骤与环境
- 潜在影响范围
- 如有，附带修复建议

我们会尽快确认并在修复后致谢（除非你希望匿名）。

## 请勿提交的内容

- 真实数据库密码、JWT 密钥、云厂商凭证
- 生产环境 `.env` / `application-local.yml`
- 用户隐私数据样本

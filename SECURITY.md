# 安全策略

## 支持的版本

安全修复优先合入各托管平台的**主分支**：

- GitHub：`main`
- Gitee：`master`

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

- 真实数据库密码、Redis 密码、JWT 密钥、云厂商凭证
- 生产环境 `.env` / `application-local.yml`
- 用户隐私数据样本

## 运维建议

- 首次部署后立即修改默认管理员密码（`admin` / `admin123`）；新装环境可能启用强制改密
- 生产环境使用强随机 `JWT_SECRET`，并限制 Redis / MySQL 仅内网可达
- 头像与上传目录权限最小化；勿将 `uploads/` 一并提交仓库
- **务必启用 HTTPS**（见 `deploy/nginx/https.conf.example`）：浏览器与服务器之间的加密依赖 TLS，而不是给公开 JSON 再套一层对称加密
- 生产可开启 `blog.security.enforce-origin=true`，并将 `blog.cors.allowed-origins` 配成你的正式域名；公开接口另有 IP 限流与响应脱敏
- 博客正文本身面向读者公开，限流/来源校验只能抑制滥用爬取，无法禁止合法阅读与复制

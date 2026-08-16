# LabVault Research Hub

LabVault Research Hub 是一个面向实验室与多课题组的科研资料管理平台，服务论文、专利、项目文档、组会纪要和实验记录等资料的上传、检索、摘要与智能问答。平台围绕课题组级数据隔离、内容溯源和生成安全控制设计，帮助科研成员复用资料，同时降低敏感材料被越权访问或被不安全生成内容误用的风险。

## 核心能力

- 科研资料管理：支持论文、专利、项目文档、组会纪要和实验记录等内容的结构化管理、附件上传与元数据检索。
- AI 摘要与单篇文档问答：基于 Spring AI 与 DeepSeek 构建文档分块、向量化、检索召回、Prompt 构造和流式生成流程；通过预索引降低首次问答等待时间。
- 权限隔离与 RAG 召回控制：围绕用户、课题组与文档权限维护访问元数据，并将权限校验覆盖到向量召回、原文查看和附件下载入口，避免跨课题组读取未授权内容。
- 内容安全与可追溯性：为论文草稿、专利材料和横向课题文档提供人工复核、风险标记、引用追踪与来源溯源能力，使摘要与问答结果可以回溯到原始文档和召回片段。
- Prompt 注入防护：对自由提问进行基础安全校验，识别越权读取、忽略权限约束和诱导泄露系统提示词等典型风险；工具调用层保留角色校验与路径白名单控制。
- 科研资料搜索：基于 Elasticsearch 支持关键词检索、资料类型筛选、标签过滤、高亮展示、前缀联想和稳定深分页；排序可结合浏览量、收藏量与发布时间等信号。
- 认证与会话安全：使用 Spring Security、RS256 JWT 双令牌与 Redis refresh token 白名单；access token 默认 20 分钟，refresh token 默认 10 天，支持登出、改密和管理员侧会话撤销。

## 技术栈

- 后端：Spring Boot 3.5、Spring Security、OAuth2 Resource Server、JWT RS256、MyBatis、MySQL、Redis、Kafka、Elasticsearch、Spring AI、Aliyun OSS。
- 前端：Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios、Sass。
- 基础设施：MySQL、Redis、Kafka、Elasticsearch、对象存储 OSS。

## AI 与安全设计

### 文档摘要与 RAG 问答

文档上传后可进入解析、分块、向量化与预索引流程。用户发起单篇文档问答时，服务先根据文档范围和访问权限检索相关片段，再将问题、受控上下文和引用信息组装为 Prompt，最后通过流式接口返回结果。回答应携带来源文档或片段标识，便于人工复核和回溯。

### 课题组隔离

科研资料、附件与向量分块需要关联用户 ID、课题组 ID、文档 ID 和权限元数据。检索层与文件访问层均以当前认证用户的可见范围为约束条件；不能只在前端隐藏入口，必须由后端在向量召回、原文查询和下载接口重复校验。

### 生成内容护栏

对敏感资料生成的摘要或问答结果，应支持人工复核状态、风险标记和引用链路。对于明显试图绕开权限、要求暴露系统提示词或诱导读取其他课题组内容的提问，系统应拒绝处理或返回受限提示。涉及外部工具时，还应进行角色校验与路径白名单校验。

## 敏感配置说明

仓库不会提交真实密钥、服务器 IP、数据库密码、OSS AccessKey、邮箱授权码、DeepSeek API Key 等信息。真实配置请放在以下位置之一：

- 后端本地文件：`backend/application-local.properties`，该文件已被 `.gitignore` 忽略。
- 系统环境变量：适合服务器部署。
- CI/CD Secret：适合自动部署。

可参考 `backend/application-example.properties` 或 `.env.example` 填写。

## 后端启动

1. 准备 JDK 17、Maven、MySQL、Redis、Kafka、Elasticsearch。
2. 创建数据库：

```sql
CREATE DATABASE hic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 导入表结构：

```bash
mysql -u root -p hic < backend/src/main/resources/schema.sql
```

4. 复制本地配置并填写真实值：

```bash
cd backend
copy application-example.properties application-local.properties
```

5. 生成 JWT RSA 密钥后写入 `JWT_PRIVATE_KEY` 和 `JWT_PUBLIC_KEY`：

```bash
openssl genrsa -out jwt_private.pem 2048
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem
```

把私钥和公钥内容转成单行后填入 `application-local.properties`。生产环境请固定使用同一对密钥，否则服务重启后旧 token 会全部失效。

6. 启动后端：

```bash
mvn spring-boot:run
```

默认地址为 `http://localhost:8081`。

## 前端启动

```bash
cd fronted
npm install
npm run dev
```

默认地址为 `http://localhost:3002`。开发环境下 `/api` 会通过 Vite 代理到后端。

如 OSS 已启用，可以在前端环境中配置：

```bash
VITE_OSS_PUBLIC_BASE_URL=https://your-public-domain/hczk
```

## 关键配置

| 配置项 | 说明 |
| --- | --- |
| `DB_HOST` / `DB_USER` / `DB_PASSWORD` | MySQL 连接信息 |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | RS256 签名私钥和验签公钥 |
| `REDIS_HOST` / `REDIS_PASSWORD` | Redis 地址与密码 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker 地址 |
| `SEARCH_ELASTICSEARCH_URIS` | Elasticsearch 地址 |
| `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS 配置 |
| `OSS_PUBLIC_DOMAIN` | OSS 文件公开访问域名，可使用 bucket 域名或 CDN 域名 |
| `OSS_FOLDER` | OSS 根目录，默认 `hczk` |
| `SPRING_AI_OPENAI_API_KEY` | DeepSeek API Key |
| `SPRING_AI_OPENAI_BASE_URL` | DeepSeek OpenAI-compatible 地址，默认 `https://api.deepseek.com` |

## OSS 资源迁移

项目当前只推荐使用 OSS，不再依赖后端本地 `uploads` 目录。旧文件批量上传到 OSS 后，需要保证数据库中保存的文件地址或 object key 能和 `OSS_PUBLIC_DOMAIN`、`OSS_FOLDER` 拼出真实可访问 URL。

例如科研文档附件的旧路径：

```text
/uploads/documents/papers/7.pdf
```

迁移到 OSS 后建议保存为：

```text
hczk/research/papers/7.pdf
```

或直接保存完整 URL：

```text
https://your-public-domain/hczk/research/papers/7.pdf
```

具体采用 object key 还是完整 URL，应与当前业务字段的读取逻辑保持一致。

## 部署建议

- Redis、Kafka、Elasticsearch 可以部署在远程 Docker 环境中，但不要把真实 IP 和密码写入仓库。
- Kafka 单机开发环境可以使用 `localhost:9094` 对外暴露；服务器部署时通过 `KAFKA_BOOTSTRAP_SERVERS` 指向真实地址。
- Elasticsearch 若未完成索引初始化，可先设置 `SEARCH_ENABLED=false` 启动核心业务，再补充索引数据。
- 邮箱验证码使用 QQ 邮箱时，`MAIL_HOST=smtp.qq.com`，`MAIL_USERNAME` 填完整 QQ 邮箱，`MAIL_PASSWORD` 填 QQ 邮箱生成的 SMTP 授权码。
- OSS 启用时设置 `OSS_ENABLED=true`，并配置 endpoint、bucket、AccessKey、public domain 和 folder。

## 验证命令

```bash
cd backend
mvn -DskipTests compile

cd ../fronted
npm run build
```

# AstraForge Knowledge Hub

AstraForge Knowledge Hub 是一个面向高校技术社团的知识库与项目协作平台。项目保留中文业务品牌“华创智库”，仓库工程名统一为 AstraForge Knowledge Hub，便于作为完整工程作品展示。

## 功能概览

- 知识库资源：资料分类、搜索、点赞、收藏、评论。
- 项目分享：项目列表、详情、发布、编辑与筛选。
- 竞赛与公告：竞赛信息、门户首页、活动公告。
- 华创推文：文章列表、详情、搜索与 OSS 图片资源。
- 师兄师姐说：优秀成员展示与经验分享。
- 全站搜索：基于 Elasticsearch 的统一检索与搜索建议。
- 认证系统：Spring Security + JWT 双令牌，access token 默认 20 分钟，refresh token 默认 10 天；refresh token 使用 Redis 白名单，支持登出、改密、管理员侧撤销会话。
- AI 能力：基于 Spring AI 的 OpenAI-compatible 配置接入 DeepSeek，用于摘要和 RAG 相关能力。

## 技术栈

- 后端：Spring Boot 3.5、Spring Security、OAuth2 Resource Server、JWT RS256、MyBatis、MySQL、Redis、Kafka、Elasticsearch、Spring AI、Aliyun OSS。
- 前端：Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios、Sass。
- 基础设施：MySQL、Redis、Kafka、Elasticsearch、对象存储 OSS。

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

例如本地旧路径：

```text
/uploads/articles/images/7.jpg
```

迁移到 OSS 后建议保存为：

```text
hczk/articles/images/7.jpg
```

或直接保存完整 URL：

```text
https://your-public-domain/hczk/articles/images/7.jpg
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

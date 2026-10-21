# 管理端后端 · Java 8

生产部署见 [Jenkins + Docker](../../docs/DEPLOYMENT.md)。运行镜像只打包 Jenkins 已构建的 JAR，Compose 仅管理当前应用服务。

独立项目：`D:/code/cy-pregnancy/cy-pregnancy-admin/server`。技术栈沿用用户已确认的Java 8、Spring Boot 2.7.18、MyBatis-Plus、MySQL、Redis、Flyway、Spring Security、JWT及springdoc，Maven版本由本机路径提供。

管理端采用账号密码登录，密码存BCrypt散列；微信登录只属于小程序用户端，本工程不引入微信SDK。框架版本为兼容Java 8，Boot 2.7.18已结束开源维护，正式生产前需评审依赖维护或升级方案。[官方支持结束说明](https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/)

## 已实现范围

- 管理员登录、当前账号/权限、退出、本人改密；短期JWT和数据库权威会话。
- 管理账号创建、分页、状态/角色调整、密码重置；最后一名安全管理员保护。
- 四个预置角色和权限清单；独立审核、自审拦截；不设自动拥有所有权限的超级角色。
- 知识条目与独立版本：创建/编辑草稿、送审、批准/拒绝、发布和下架；已送审内容不可改写。
- 成功管理写操作与登录事件审计；UUID幂等、If-Match乐观锁、稳定游标分页。
- 本机初始化/运行脚本、OpenAPI、Jenkins流水线及应用Compose配置。

**管理Web及小程序只读内容链路已接通。** 发布只暴露当前审核版本，下架后重新读取不可见；不更改医学计算参数。模型/参考包、反馈、任务、数据删除监管、MFA及发布回滚仍待开发，能力接口明确为false。

## 本机启动

使用本机JDK `C:/Program Files/Java/jdk-1.8`、Maven `D:/code/apache-maven-3.9.16`；MySQL为localhost:3306，local配置使用root/root；Redis为localhost:6379，无密码。

```powershell
Set-Location D:/code/cy-pregnancy/cy-pregnancy-admin/server
powershell -ExecutionPolicy Bypass -File ./scripts/init-local-db.ps1
powershell -ExecutionPolicy Bypass -File ./scripts/prepare-local.ps1
powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 -Task run
```

初始化脚本只创建独立库`cy_pregnancy_admin`，Flyway建表；不会改动小程序库。服务默认端口18732，不与用户端18731冲突。启动脚本分别生成随机管理JWT密钥和初始密码，保存在忽略目录`.local/`，不打印到日志、不打进镜像。

首次空库启动创建用户名`local_admin`，密码在本机`.local/bootstrap-password.txt`中自行查看。它只拥有security_admin角色，负责创建其他角色账号；不自动获得内容编辑/审核/发布权限。表里已有任何账号时不重建、不重置、不提升角色；之后密码文件不代表修改后的账号密码。

IDEA导入本目录pom.xml，SDK及Maven Runner均用Java 8。首次执行一次 `scripts/prepare-local.ps1`，复用或生成`.local/`中的随机签名密钥、初始密码和内部只读密钥，导出被git忽略的`.local/application-secrets.properties`。之后直接启动AdminServerApplication即可：默认local配置从server目录或管理项目根目录自动读取该文件，无需把密钥复制进IDEA。管理项目提供共享运行配置`AdminServerApplication (local)`，工作目录为server；单独导入server时使用默认server工作目录即可。

环境变量`ADMIN_JWT_SECRET`等优先于本地文件；若IDEA中留有空字符串环境变量，请删除空项。`dev.ps1`也会先准备同一份本地配置，重复执行不轮换原有密钥、不重置已有管理员密码。生产profile不导入本地文件，仍必须显式配置正式密钥；不要把`.local/`复制到生产服务器或打包进镜像。

### IDEA编译报TypeTag UNKNOWN

`java.lang.ExceptionInInitializerError / com.sun.tools.javac.code.TypeTag :: UNKNOWN` 通常发生在旧Lombok注解处理器遇到不兼容的新JDK时。本机曾将管理项目SDK选成`openjdk-26`，而工程要求Java 8；`pom.xml`中的`java.version=1.8`不会自动切换IDEA使用的JDK。

在IDEA中将以下位置统一设置为 `C:/Program Files/Java/jdk-1.8`：Project Structure → Project SDK及Modules的Module SDK；Settings → Build Tools → Maven的Importer JDK与Runner JRE；AdminServerApplication运行配置的JRE。语言级别及Java Compiler目标字节码均设为8，然后Reload All Maven Projects并Rebuild Project。当前机器已注册的该JDK名称是`account-book-jdk8`，名称不影响使用；换电脑时选择当地实际安装的JDK 8，不要照搬名称。

可先用 `powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 -Task test` 验证，该脚本明确使用JDK 8，不依赖IDEA SDK。不要通过关闭注解处理来绕过错误，实体和DTO需要Lombok生成方法；如将来升级JDK，应同时评估Lombok及Spring Boot的兼容版本。

本地入口：

- [健康检查](http://localhost:18732/actuator/health)
- [Swagger UI](http://localhost:18732/swagger-ui/index.html)
- [OpenAPI JSON](http://localhost:18732/v3/api-docs)

文档仅local开放。先调用POST `/admin/api/v1/auth/login`获取token，再在Swagger的Authorize中填入。服务使用Bearer头，不用Cookie；暂无刷新接口，默认30分钟后重新登录。

## 分层约定

```text
com.chunyu.pregnancy.admin
├── common         result / exception / filter / ratelimit / util
├── entity         持久化实体，不直接返回客户端
├── dto            request / response，逐类分文件
├── mapper         MyBatis-Plus及显式SQL
├── service        业务接口
│   ├── impl       具体业务实现
│   └── support    幂等事务、审计写入、密码策略
├── controller     参数接收、权限入口、响应包装
├── security       JWT及会话/实时权限过滤
└── config         组件装配、配置绑定、首次空库初始化
```

调用方向Controller → Service → Mapper。每个主业务有Service接口和impl；过滤器通过Service校验会话。控制器没有Mapper依赖；公共工具不依赖业务层。中文注释解释业务意图、字段含义、事务和并发约束，代码统一格式，便于IDE阅读。

## 测试和打包

```powershell
# 默认6项纯单元测试，不需要连接数据库。
powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 -Task test
# 需要MySQL/Redis的28项集成测试先手动创建专用测试库：
# CREATE DATABASE cy_pregnancy_admin_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
$env:RUN_ADMIN_MYSQL_IT = 'true'
powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 -Task package
Remove-Item Env:RUN_ADMIN_MYSQL_IT
```

测试以root/root连接**专用测试库**，每次清理账号和内容测试夹具；代码先校验SELECT DATABASE()，禁止指向开发/生产库。Redis只写测试随机计数键，不执行FLUSHDB。实际测试结果与限制见[验证记录](docs/05-验证记录.md)。

jar位于`target/cy-pregnancy-admin-server-0.1.0-SNAPSHOT.jar`。未在本机执行Docker构建或云端部署，生产配置步骤见[Jenkins 部署说明](../../docs/DEPLOYMENT.md)。

## 阅读顺序

先看[分层指南](docs/01-分层与代码阅读指南.md)，从AdminAuthController → AdminAuthService → AdminAuthServiceImpl → Mapper理解登录；从KnowledgeController → KnowledgeServiceImpl → CommandExecutor理解状态机和事务。接口字段/调用例子在[接口与业务流程](docs/02-接口与业务流程.md)，表字段/约束在[表结构与事务设计](docs/03-表结构与事务设计.md)。

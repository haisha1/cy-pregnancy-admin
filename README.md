# 管理后台

部署统一使用 [Jenkins + Docker](../docs/DEPLOYMENT.md)，按仓库内 Jenkinsfile 构建发布。

管理Web位于[web](../web/docs/项目说明.md)，Java管理服务位于[server](server/README.md)。两者已通过真实接口联调，数据库和JWT与用户服务独立。

已实现管理员/预置角色、改密/重置/停用、审计、知识双表达编辑、提交、独立审核、发布和下架。小程序通过用户服务读取当前发布内容，撤回后重新读取返回404；不发布医学计算参数。四项目当前范围见[联调记录](../docs/INTEGRATION.md)。

技术层阅读见[指南](server/docs/01-分层与代码阅读指南.md)，写操作流程见[接口](server/docs/02-接口与业务流程.md)，数据库见[事务设计](server/docs/03-表结构与事务设计.md)。后续模型、工单等规划保留在[长期需求](docs/管理后台后续开发设计.md)，不代表已接通。

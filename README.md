# PG-OJ
编程题目评测系统：Vue 前端、Spring Boot 后端、MySQL 持久化判题队列，以及独立 Docker 代码沙箱。管理员配置测试输入与预期输出，系统编译并运行用户提交的 Java 代码后返回判题结果。

上线配置、安全加固、数据库迁移、示例题目、测试命令与部署步骤见 [正式上线指南](docs/production-readiness.md)。生产环境使用 Redis 共享 Session 和限流；沙箱应部署在独立主机，不对公网暴露 Docker 接口。

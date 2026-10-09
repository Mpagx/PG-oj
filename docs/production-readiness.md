# 生产部署与验收

本次实现按先前上线清单覆盖题目数据校验、标准输入判题、明确的错误类型、密码升级、验证码、CSRF、CORS、封禁、限流、数据库迁移、共享登录态、服务托管、日志、监控与端到端测试。部署文件在 `deploy/`；它们是可配置的生产部署模板，需要实际服务器、域名、证书和密钥才能部署。

## 本地开发

JDK 17 用于后端构建；沙箱源码兼容 JDK 8，部署可用 JDK 17。`JAVA_HOME` 应指向 JDK 根目录，不能指向 `bin`。

```powershell
$env:JAVA_HOME='C:\Users\Legion\.jdks\ms-17.0.18'
$env:DB_PASSWORD='<开发数据库密码>'
cd pg-backend
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd -f ..\cookie-code-sandbox\pom.xml test package
cd ..\pg-frontend
npm ci
npm run lint
npm run build
```

本地做题需要三层服务同时就绪：后端 8121、沙箱 8090、沙箱虚拟机中的 Docker。Windows 每次启动开发环境时，从项目根目录执行 `powershell -File .\cookie-code-sandbox\scripts\start-sandbox-vm.ps1`，启动或连接已有 VM，并建立回环 Docker SSH 隧道（2375）。然后启动沙箱、后端和前端。只启动沙箱的 Java 进程并不等于 Docker 已就绪。访问 `http://127.0.0.1:8121/api/code-sandbox/health`，确认 `data.status` 为 `UP` 后再提交；开发环境不需要云服务器。SSH 隧道需要保持运行，重启 Windows 或关闭 VM 后应重新运行脚本。

远程沙箱不可用时，提交接口会明确拒绝并且不创建提交记录，避免离线 Docker 立即耗尽重试次数。健康检查使用短超时，不沿用长时间代码执行超时。已经进入执行后发生的临时故障仍保留最多三次重试；已重试耗尽的旧提交不会因服务恢复自动重判，需要重新提交。

开发环境为本地 Session 与内存限流；生产强制使用 Redis。登录先 GET `/api/security/captcha` 取得图片及 Session，然后 POST 中携带 `captchaAnswer`；注册不再使用图片验证码，启用邮箱功能时只校验邮箱验证码。写接口需要先 GET `/api/security/csrf`，保留 Cookie，并发送返回值到 `X-XSRF-TOKEN`。前端已自动完成此流程。旧 MD5 密码在一次成功登录时升级到 BCrypt；新密码不再保存 MD5。管理端创建用户必须显式提供密码，不再生成公开默认密码。

## 数据库迁移

空数据库由 Flyway 自动执行当前全部迁移（V1–V14）。生产默认启用迁移，禁用 `clean`，禁用自动 baseline。已有数据库接入前：

1. 用 `deploy/backup.sh` 生成事务一致的备份，复制到异机加密存储并验证恢复。
2. 核对已有数据库包含 `user`、`question`、`question_submit` 及 V1 的原始业务字段。升级前检查重复登录名：`SELECT userAccount, COUNT(*) FROM user GROUP BY userAccount HAVING COUNT(*) > 1;`。重复数据由管理员确认合并；程序不会删除用户。
3. 在备份的恢复库先运行 baseline：设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，从 `pg-backend` 执行 `./mvnw compile flyway:baseline -Dflyway.baselineVersion=1`。
4. 启动应用，依次执行后续迁移：补充队列字段和唯一约束、统一唯一 `userName`、增加用户名修改时间、题目难度和草稿/发布状态，以及唯一验证邮箱和密码修改时间。恢复库验证成功后，对目标库重复此过程。不要对未核对的库 baseline。

`CREATE TABLE IF NOT EXISTS` 无法升级已有表；现在由 Flyway 版本迁移管理升级。V4 将旧 `userAccount` 保留为唯一 `userName` 并删除重复的昵称列；迁移前必须先处理重复账号。V5 将既有题目标记为已发布，新建题目则由应用默认保存为草稿。V6 增加唯一验证邮箱；既有用户邮箱保持为空，登录后可在“我的”页面完成绑定。V7、V8 修复手工升级旧库并确保数据库只保留非空且唯一的 `userName`。V9 清理无代码引用的模板字段并校准题目统计。V10 增加题单、头像和用户名 30 天修改冷却所需结构。V11 增加题目包来源、许可证和标准答案字段；V12 移除不再使用的外部题目推荐目录；V13 增加 AC 后解锁的用户题解。迁移不猜测或覆盖旧题目的答案。管理端需修正原有 A+B 的期望输出为 `3`，并把内存设为 `262144 KB`、时间 `1000 ms`、栈 `65536 KB`。`sql/sample_a_plus_b.sql` 提供可显式运行的、不会覆盖既有题目的种子示例；先填真实管理员 ID。

测试用例存放 `question.judgeCase`，不是用户提交记录。公开接口隐藏用例；管理员编辑使用 `/api/question/get/admin`。空输入、空输出允许，缺失 input/output 不允许。输入用例逐个通过标准输入传入完整 `public class Main`；不再作为 `args`。忽略行尾空格、CRLF 差异及最终换行，但保留行首空格、内部空白行和单词间空格。

题目有 `DRAFT` 和 `PUBLISHED` 两种状态，以及简单、中等、困难三级难度。只有已发布题目会进入用户题库并接受提交；管理员可以在题目列表按状态和难度筛选。创建新题默认是草稿，完成题面、资源限制和隐藏用例检查后再发布。

管理员题目页支持一次选择多个 ZIP 顺序导入。Cookie OJ v1 格式、示例包和字段说明位于 `problem-packages/`。服务端会限制 ZIP 大小和解压体积、防止路径穿越，检查题面、限制、隐藏用例和 Java 标准答案，并让标准答案在真实沙箱通过全部用例；只有全部通过才写入数据库，而且固定保存为 `DRAFT`。`generator.yaml` 只使用内置的确定性整数对生成器，不执行 ZIP 内脚本。ICPC/DOMjudge 当前兼容普通 pass-fail、Markdown 题面、静态数据和 Java accepted solution；交互题及自定义 output validator 会拒绝导入，不能假装使用精确文本比对。

Cookie OJ 不同步外部平台题面或测试数据。扩充题库统一使用经过授权或自己编写的 Cookie OJ ZIP 包；导入、沙箱验证和人工发布形成同一套质量流程。

做题页不提供独立评论区。“题解”默认对该题至少有一次 Accepted 的用户和管理员开放。未通过的已登录用户也可确认“实在不会，查看题解”，V14 记录这一选择并解锁阅读；发布题解仍需要通过本题。每位用户每题最多维护一篇 Markdown 题解，可修改或逻辑删除；用户可以分页阅读，管理员可以删除不合规题解。题解不包含隐藏测试数据和管理员标准程序。

Java 提交增加基础入口校验：前端拦截纯数字、普通文字、缺少 Main 或 main 的代码；后端用 JDK Java 解析器检查顶层 `public class Main`、类内 `public static void main(String[] args)`，不允许 package 声明。参数也可使用 `String args[]`、`String... args` 或 `java.lang.String[] args`。注释、字符串和嵌套类不能伪造入口。后端只解析声明，不编译、不执行用户代码，也不运行注解处理器；完整编译与执行仍在 Docker 沙箱中完成。入口无效时不创建提交、不增加提交计数、不调用沙箱。类型等编译错误仍产生 CE 结果，提交详情页保留“编译错误”状态但隐藏原黄色编译器诊断条，诊断数据仍保留供排查。

做题页的“自定义测试”只把一组用户输入发送到隔离沙箱，沿用题目的时间、内存和堆栈限制；它不会写入 `question_submit`、不会增加题目提交数，也不会影响个人统计。接口按 IP 限制为每分钟 10 次，代码和输入分别限制为 64 KiB。

## 邮箱验证与账号恢复

开发环境默认 `EMAIL_ENABLED=false`，此时邮箱注册和邮件找回不可用，已有用户仍可登录。注册仅校验邮箱验证码，登录保留图片验证码。要启用完整流程，配置 `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM`，设置 `EMAIL_ENABLED=true`，并生成至少 32 字符的随机 `EMAIL_CODE_SECRET`。不要使用邮箱登录密码，应使用邮件服务商提供的 SMTP 应用专用密码。587 端口通常使用 `MAIL_STARTTLS=true`；465 端口通常使用 `MAIL_STARTTLS=false`、`MAIL_SSL_ENABLE=true`，具体以邮件服务商文档为准。

邮箱验证码有效期 10 分钟、发送冷却 60 秒、验证后立即作废；服务端只保存带密钥的摘要，不记录明文验证码。生产环境强制把验证码状态放入 Redis，并要求 SMTP 和密钥均已配置，否则后端拒绝启动。注册用户验证邮箱；既有用户可在个人主页绑定或更换邮箱；找回密码不会向访客泄露邮箱是否存在。密码修改后，旧 Session 在下一次访问受保护接口时失效。管理员只能向已验证邮箱发送重置码，不能查看或替用户指定新密码。

## Linux 部署

建议后端和判题执行在不同 Linux 主机或虚拟机。Docker 控制权等同于该执行主机 root；沙箱主机不能包含数据库密钥、业务文件或其它敏感服务。仅沙箱服务账号加入该主机 docker 组。不要把 Docker API 或沙箱端口映射到公网。

- 后端主机安装 JDK 17、Nginx、MySQL 8 与带密码的 Redis（或使用私网托管实例）。为数据库建立最低业务权限账号，迁移阶段才赋予 DDL 权限。创建 `poj` 用户、`/opt/poj/backend`、`/var/log/poj`，赋予正确所有权。
- 复制 backend jar 为 `/opt/poj/backend/backend.jar`，前端 dist 为 `/opt/poj/frontend`。将 `deploy/.env.example` 配置为 `/etc/poj/backend.env`，权限 600。不要继续使用示例占位密钥。
- 判题 VM 安装 Docker、JDK 17；新环境使用 `eclipse-temurin:8-jdk-alpine`，不要再拉取已失效的 `openjdk:8-alpine`。CI 已固定可用的镜像 digest，见 `.github/workflows/verify.yml` 的 `SANDBOX_IMAGE`。实际生产应自行维护经过扫描的 Java 执行镜像并固定 digest；沙箱支持 `-Dcodesandbox.docker.image=镜像名`，需要放在 `-jar` 前，也可在 `/etc/poj/sandbox.env` 中设置 `JAVA_TOOL_OPTIONS=-Dcodesandbox.docker.image=镜像名`。旧默认镜像仅兼容已缓存的本地环境。创建 `poj-sandbox` 用户及 `/var/lib/poj-sandbox`、`/var/log/poj`，复制 sandbox jar 到 `/opt/poj/sandbox/sandbox.jar`。
- 沙箱 VM 将 `deploy/sandbox.env.example` 保存为 `/etc/poj/sandbox.env`，权限 600。HTTP 监听回环地址，Docker 使用 Unix socket，无明文网络 API。
- 后端和沙箱位于不同主机时，将 `SANDBOX_SSH_TARGET=用户名@沙箱地址` 保存到 `/etc/poj/tunnel.env`；配置专用 SSH 密钥 `/etc/poj/sandbox_ed25519` 和经过人工核验的 `/etc/poj/sandbox_known_hosts`。使用限制为仅转发 `127.0.0.1:8090` 的 SSH 登录账号。
- 安装对应 systemd 单元：后端主机 `poj-backend.service`、`poj-sandbox-tunnel.service`；判题 VM `poj-sandbox.service`。`systemctl daemon-reload` 后启用服务。模板使用固定路径，按上述布局安装。
- 将 `deploy/nginx.conf` 中域名与证书路径替换为实际值，`nginx -t` 验证后 reload。后端只接受本机反向代理；代理覆盖客户端传入的转发头。公网通过 HTTPS 访问，Cookie 设置 Secure、HttpOnly、SameSite=Lax。

生产禁止通配 CORS、弱默认数据库密码、空 Redis 密码和空沙箱令牌；文档接口默认关闭。令牌必须在后端与沙箱一致。默认判题 HTTP 读取超时 660 秒，租约 720 秒，覆盖最多 50 用例各 10 秒加编译与容器开销。调整最大用例数/时间或慢机器部署时需同步调整超时。关停服务时仍在执行的任务依赖租约恢复，最多重试三次。

## 健康、监控与备份

后端 `/api/code-sandbox/health` 应为 UP；沙箱 VM `/health` 检查 Docker 和执行镜像。仅管理回环端口 9121 提供 `/actuator/health/readiness` 与 `/actuator/prometheus`。不要公开管理端口。

Prometheus 以 `deploy/prometheus.yml` 为模板，告警规则包含后端宕机、沙箱异常、队列积压、判题系统错误率。部署在后端主机时可直接抓取回环地址；远程监控需私网代理和鉴权。接入自己的 Alertmanager 及接收渠道后才会真正发送告警。指标 `poj_judge_duration_seconds` 记录完成判题耗时，`poj_judge_attempts_total` 记录每次执行结果。日志轮转保留 14 天，请求日志不保存密码或完整提交代码。

复制 `deploy/backup.sh` 和 `deploy/restore.sh` 到 `/opt/poj/deploy/`。配置 `/etc/poj/backup.env` 中 `MYSQL_CNF`、`BACKUP_DIR`、`DB_NAME`；MYSQL_CNF 为权限 600 的 `[client]` 凭据文件。创建 `poj-backup` 用户和独立备份目录，再安装 `poj-backup.service`、`poj-backup.timer`。每天北京时间 02:00 备份。备份留存/异机传输由实际存储策略管理，脚本不会自动删除备份。

恢复用 `RESTORE_DB=poj_restore_日期 bash deploy/restore.sh 备份.sql.gz`，只允许恢复到新库，不能直接覆盖业务库。验证用户数、题目数、提交数及端到端测试后，再决定切换。

## 真实 Docker 与端到端验收

普通测试不需要启动 Docker。真实恶意代码回归：

```powershell
cd cookie-code-sandbox
.\scripts\start-sandbox-vm.ps1
cd ..\pg-backend
.\mvnw.cmd -f ..\cookie-code-sandbox\pom.xml '-Dsandbox.docker.tests=true' test
```

Windows 脚本仅用于本地开发 VM；可用 `POJ_SANDBOX_VM_PATH`、`POJ_SANDBOX_VM_USER` 覆盖默认值，生产使用 systemd。完整 HTTP 测试必须连接隔离测试库，库名以 `poj_e2e` 开头：

```powershell
$env:E2E_DB_URL='jdbc:mysql://127.0.0.1:3306/poj_e2e_test'
$env:E2E_DB_USERNAME='<测试库账号>'
$env:E2E_DB_PASSWORD='<测试库密码>'
$env:CODE_SANDBOX_BASE_URL='http://127.0.0.1:8090'
$env:CODE_SANDBOX_TOKEN='<沙箱令牌>'
.\mvnw.cmd '-Dpoj.e2e=true' '-Dtest=JudgeEndToEndTest' test
.\mvnw.cmd '-Dpoj.migration.tests=true' '-Dtest=LegacyMigrationTest' test
```

先创建空测试库并赋予测试账号权限。测试创建独立随机管理员与题目，经过登录、CSRF、创建题目、提交、轮询，验证 AC、WA、CE、TLE；测试库保留测试数据便于排查。验证码仅在该测试配置关闭。旧库迁移测试使用具备建库权限的测试账号，创建随机 `poj_e2e_migration_*` 临时库，验证旧表补列、账号唯一约束与保留原始数据后只删除自己创建的临时库。常规后端单元测试独立于数据库；旧云存储/数据库示例测试以 `-Dpoj.external.tests=true` 显式开启，不进入普通 CI。

上线验收还应在实际部署环境验证 HTTPS、Redis 跨实例登录与限流、服务器重启恢复、备份恢复、告警接收。源码测试和模板生成不等同于这些部署验收已经通过。

实现参考：[Spring BCrypt API](https://docs.spring.io/spring-security/site/docs/5.7.8/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)、[Flyway baseline](https://documentation.red-gate.com/fd/baseline-277578867.html)。

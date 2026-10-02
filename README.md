# PG-OJ

基于 Spring Boot、消息队列和 Docker 的在线编程题目评测系统。项目采用单仓结构，前端和后端保持为两个独立项目。

## Repository layout

```text
Poj/
├── pg-frontend/  # Vue 3 frontend
└── pg-backend/   # Spring Boot backend
```

## Frontend

```bash
cd pg-frontend
npm install
npm run serve
```

## Backend

The backend requires Java 17.

```bash
cd pg-backend
./mvnw spring-boot:run
```

The supported code submission language values are `java`, `cpp`, and `go`.

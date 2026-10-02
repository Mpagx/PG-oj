# POJ

POJ is organized as a single repository with independent frontend and backend projects.

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

# Cookie code sandbox HTTP protocol v1

The main backend reuses the repository's existing `cookie-code-sandbox` service.
The sandbox compiles and executes code; expected-output comparison remains in
the main backend.

## Authentication

Set the same optional `CODE_SANDBOX_TOKEN` in both processes. When configured,
requests must include the value in the `X-Sandbox-Token` header.

## Health check

`GET /health`

```json
{
  "status": "UP",
  "service": "cookie-code-sandbox",
  "version": "1.0.0",
  "supportedLanguages": ["java"],
  "message": "ok"
}
```

The backend exposes the aggregated result at
`GET /api/code-sandbox/health` and returns HTTP 503 when the sandbox cannot be
reached.

## Execute code

`POST /executeCode`

```json
{
  "protocolVersion": "1.0",
  "requestId": "question-submit-123",
  "language": "java",
  "code": "public class Main { ... }",
  "inputList": ["1 2", "3 4"],
  "timeLimitMs": 1000,
  "memoryLimitKb": 262144,
  "stackLimitKb": 65536
}
```

The sandbox echoes `protocolVersion` and `requestId`. Existing response fields
are retained for compatibility:

```json
{
  "protocolVersion": "1.0",
  "requestId": "question-submit-123",
  "outputList": ["3", "7"],
  "message": null,
  "status": "1",
  "judgeInfo": {
    "message": "执行成功",
    "memory": 24576,
    "time": 35
  }
}
```

Sandbox status values are `1` (execution completed), `2` (sandbox/system
failure), and `3` (user-code compilation or runtime failure).

## Submission state machine

```text
WAITING (0) -> RUNNING (1) -> COMPLETED (2)
                         \-> SYSTEM_FAILED (3)
```

Every transition is a conditional database update. `COMPLETED` means judging
finished normally; the verdict is stored in `judgeInfo.message`.

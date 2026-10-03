# Cookie code sandbox HTTP protocol v1

The main backend reuses the repository's existing `cookie-code-sandbox` service.
The sandbox compiles and executes code; expected-output comparison remains in
the main backend.

## Authentication

Set the same optional `CODE_SANDBOX_TOKEN` in both processes. When configured,
requests must include the value in the `X-Sandbox-Token` header. The sandbox
listens on `127.0.0.1` by default; keep this binding when the backend runs on
the same host. A token is strongly recommended if the service must be exposed
through a VM port forward or another network interface.

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
reached. The sandbox health endpoint also verifies that the Docker Engine and
the configured execution image are available; an HTTP listener by itself is
not considered healthy.

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

## Runtime hardening

Each execution container runs as UID/GID `65534`, with networking disabled,
all Linux capabilities dropped, `no-new-privileges`, a read-only root file
system, a read-only code volume, and a small `noexec` temporary file system.
Memory, swap, CPU, process count, execution time, source/input size, output
size, and service concurrency are bounded independently of the limits supplied
by a request. Compilation also has a timeout and bounded diagnostic output.

The service defaults can be tightened with these environment variables:

- `CODE_SANDBOX_MAX_CODE_BYTES`
- `CODE_SANDBOX_MAX_TEST_CASES`
- `CODE_SANDBOX_MAX_INPUT_BYTES_PER_CASE`
- `CODE_SANDBOX_MAX_TOTAL_INPUT_BYTES`
- `CODE_SANDBOX_MIN_TIME_LIMIT_MS` / `CODE_SANDBOX_MAX_TIME_LIMIT_MS`
- `CODE_SANDBOX_MIN_MEMORY_LIMIT_KB` / `CODE_SANDBOX_MAX_MEMORY_LIMIT_KB`
- `CODE_SANDBOX_MIN_STACK_LIMIT_KB` / `CODE_SANDBOX_MAX_STACK_LIMIT_KB`
- `CODE_SANDBOX_MAX_OUTPUT_BYTES`
- `CODE_SANDBOX_COMPILE_TIMEOUT_MS`
- `CODE_SANDBOX_MAX_CONCURRENT_EXECUTIONS`
- `CODE_SANDBOX_QUEUE_WAIT_TIMEOUT_MS`

## Test suites

Run unit tests and the HTTP controller integration tests without starting the
VM or Docker Engine:

```bash
mvn test
```

After starting the VM Docker Engine, exposing it through the configured
`DOCKER_HOST`, and preparing the `openjdk:8-alpine` image, run the real
malicious-code regression suite explicitly:

```bash
mvn -Dsandbox.docker.tests=true test
```

The malicious suite verifies deadline termination, bounded output, read-only
root-file-system enforcement, disabled outbound networking, container memory
limits, and PID limits. It is disabled during ordinary builds so a stopped VM
cannot be mistaken for a code regression; when explicitly enabled, unavailable
Docker dependencies fail the suite with a setup message.

## Submission state machine

```text
WAITING (0) -> RUNNING (1) -> COMPLETED (2)
                         \-> SYSTEM_FAILED (3)
```

Every transition is a conditional database update. `COMPLETED` means judging
finished normally; the verdict is stored in `judgeInfo.message`.

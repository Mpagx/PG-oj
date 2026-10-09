package com.poj.poj.judge;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.poj.poj.model.entity.User;
import com.poj.poj.service.UserService;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP, MySQL, Flyway and Docker; never use the development database. */
@EnabledIfSystemProperty(named = "poj.e2e", matches = "true")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=true", "management.server.port=0", "app.captcha.enabled=false",
    "judge.queue.recovery-enabled=true", "judge.queue.scan-interval-ms=100", "app.rate-limit.submit=100"
})
class JudgeEndToEndTest {
    @LocalServerPort int port;
    @Resource UserService users;
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("E2E_DB_URL");
        if (url == null || !url.matches("jdbc:mysql://[^/]+/poj_e2e[a-zA-Z0-9_]*(\\?.*)?")) {
            throw new IllegalStateException("E2E_DB_URL must identify an isolated poj_e2e database");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("E2E_DB_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("E2E_DB_PASSWORD"));
    }
    @Test void createProblemSubmitAndPollAcWaCeTle() throws Exception {
        JSONObject health = get("/code-sandbox/health");
        assertEquals("UP", health.getJSONObject("data").getStr("status"));
        String account = "e2e_" + UUID.randomUUID().toString().replace("-", "");
        long userId = users.userRegister(account, "e2e-password-123", "e2e-password-123");
        User admin = new User(); admin.setId(userId); admin.setUserRole("admin"); users.updateById(admin);
        post("/user/login", Map.of("userName", account, "userPassword", "e2e-password-123"));
        long questionId = post("/question/add", Map.of(
                "title", "E2E A+B " + account, "content", "Read two longs from stdin and print their sum",
                "tags", new String[]{"e2e"},
                "judgeCase", new Object[]{Map.of("input", "1 2", "output", "3"), Map.of("input", "-1 2", "output", "1")},
                "judgeConfig", Map.of("timeLimit", 1000, "memoryLimit", 262144, "stackLimit", 1024))).getLong("data");
        verdict(questionId, "import java.util.*; public class Main { public static void main(String[] a) { Scanner s=new Scanner(System.in); System.out.println(s.nextLong()+s.nextLong()); }}", "Accepted");
        verdict(questionId, "public class Main { public static void main(String[] a) {System.out.println(0);}}", "Wrong Answer");
        verdict(questionId, "public class Main { public static void main(String[] a) { int n = \"wrong\"; } }", "编译错误");
        verdict(questionId, "public class Main { public static void main(String[] a) {while(true){}}}", "超时");
    }
    private void verdict(long question, String code, String expected) throws Exception {
        long submission = post("/question_submit/", Map.of("questionId", question, "language", "java", "code", code)).getLong("data");
        long deadline = System.currentTimeMillis() + 90000;
        while (System.currentTimeMillis() < deadline) {
            JSONObject data = get("/question_submit/get?id=" + submission).getJSONObject("data");
            if (data.getInt("status") >= 2) {
                assertEquals(2, data.getInt("status"), data.toString());
                assertEquals(expected, data.getJSONObject("judgeInfo").getStr("message"), data.toString()); return;
            }
            Thread.sleep(500);
        }
        fail("Submission did not reach a terminal state: " + submission);
    }
    private JSONObject get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }
    private JSONObject post(String path, Object body) throws Exception {
        String token = get("/security/csrf").getStr("data");
        return send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", token).POST(HttpRequest.BodyPublishers.ofString(JSONUtil.toJsonStr(body))).build());
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + "/api" + path); }
    private JSONObject send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        JSONObject body = JSONUtil.parseObj(response.body()); assertEquals(0, body.getInt("code"), body.toString());
        return body;
    }
}

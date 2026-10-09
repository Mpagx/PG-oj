package com.poj.poj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.poj.poj.exception.BusinessException;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.vo.ProblemImportResultVO;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ProblemPackageImportServiceTest {
    private ProblemPackageImportService service;
    private QuestionService questions;
    private RemoteCodeSandbox sandbox;

    @BeforeEach
    void setUp() {
        service = new ProblemPackageImportService();
        questions = mock(QuestionService.class);
        sandbox = mock(RemoteCodeSandbox.class);
        ReflectionTestUtils.setField(service, "questionService", questions);
        ReflectionTestUtils.setField(service, "remoteCodeSandbox", sandbox);
        when(questions.save(any(Question.class))).thenAnswer(call -> {
            call.getArgument(0, Question.class).setId(99L);
            return true;
        });
        when(sandbox.executeCode(any(ExecuteCodeRequest.class))).thenAnswer(call -> {
            ExecuteCodeRequest request = call.getArgument(0);
            ExecuteCodeResponse response = new ExecuteCodeResponse();
            response.setStatus("1");
            response.setOutputList(request.getInputList().stream().map(input -> {
                long[] values = Arrays.stream(input.trim().split("\\s+")).mapToLong(Long::parseLong).toArray();
                return String.valueOf(values[0] + values[1]);
            }).collect(Collectors.toList()));
            return response;
        });
    }

    @Test
    void importsVerifiedPackageOnlyAsDraft() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("cookie-oj.yaml", "title: A + B\ndifficulty: EASY\ntags: [入门]\nlimits:\n  timeLimit: 1000\n  memoryLimit: 262144\n  stackLimit: 65536\n");
        files.put("statement.md", "# A + B\n求两个整数之和。");
        files.put("solution/Main.java", "public class Main { public static void main(String[] a) {} }");
        files.put("data/secret/01.in", "1 2");
        files.put("data/secret/01.ans", "3");
        files.put("generator.yaml", "type: integer-pair\ncount: 2\nmin: -10\nmax: 10\noperation: sum\nseed: 1\n");

        ProblemImportResultVO result = service.importPackage(zip(files), 7L);

        assertEquals(99L, result.getQuestionId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(3, result.getCaseCount());
        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        org.mockito.Mockito.verify(questions).save(saved.capture());
        assertEquals("DRAFT", saved.getValue().getStatus());
        assertEquals("COOKIE_OJ_V1", saved.getValue().getPackageType());
    }

    @Test
    void rejectsZipSlipBeforeAnyImport() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../outside.txt", "blocked");
        BusinessException error = assertThrows(BusinessException.class, () -> service.importPackage(zip(files), 7L));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("非法路径"));
    }

    @Test
    void rejectsPackageWithoutHiddenOrGeneratedCases() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("problem.yaml", "name: Sample only\ntype: pass-fail\n");
        files.put("statement/problem.en.md", "# Sample only");
        files.put("submissions/accepted/Main.java", "public class Main { public static void main(String[] a) {} }");
        files.put("data/sample/01.in", "1 2");
        files.put("data/sample/01.ans", "3");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.importPackage(zip(files), 7L));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("隐藏用例"));
    }

    @Test
    void rejectsDomjudgeCustomOutputValidator() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("problem.yaml", "name: Special judge\ntype: pass-fail\n");
        files.put("domjudge-problem.ini", "timelimit=1\n");
        files.put("statement/problem.en.md", "# Special judge");
        files.put("submissions/accepted/Main.java", "public class Main { public static void main(String[] a) {} }");
        files.put("data/secret/01.in", "1 2");
        files.put("data/secret/01.ans", "3");
        files.put("output_validators/check.cpp", "int main() {}\n");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.importPackage(zip(files), 7L));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("自定义输出判题器"));
    }

    private MockMultipartFile zip(Map<String, String> files) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return new MockMultipartFile("file", "problem.zip", "application/zip", bytes.toByteArray());
    }
}

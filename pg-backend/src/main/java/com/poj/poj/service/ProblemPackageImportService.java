package com.poj.poj.service;

import cn.hutool.json.JSONUtil;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.judge.QuestionJudgeValidator;
import com.poj.poj.judge.codesandbox.CodeSandboxProxy;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.judge.strategy.DefaultJudgeStrategy;
import com.poj.poj.model.dto.question.JudgeCase;
import com.poj.poj.model.dto.question.JudgeConfig;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.vo.ProblemImportResultVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** 安全导入 Cookie OJ v1 与 ICPC/DOMjudge 题目 ZIP。 */
@Service
public class ProblemPackageImportService {
    private static final int MAX_ENTRIES = 300;
    private static final int MAX_ENTRY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 20 * 1024 * 1024;
    private static final int MAX_CASES = 100;
    private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    @Resource private QuestionService questionService;
    @Resource private RemoteCodeSandbox remoteCodeSandbox;

    @Transactional(rollbackFor = Exception.class)
    public ProblemImportResultVO importPackage(MultipartFile zip, long userId) {
        if (zip == null || zip.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择题目 ZIP");
        if (zip.getSize() > 25L * 1024 * 1024) throw new BusinessException(ErrorCode.PARAMS_ERROR, "ZIP 不能超过 25MB");
        String filename = StringUtils.defaultString(zip.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".zip")) throw new BusinessException(ErrorCode.PARAMS_ERROR, "只支持 ZIP 题目包");

        Map<String, byte[]> files = readZip(zip);
        PackageData data = parse(files);
        validate(data);
        verifyReferenceSolution(data);

        Question question = new Question();
        question.setTitle(data.title);
        question.setDifficulty(data.difficulty);
        question.setStatus("DRAFT");
        question.setContent(data.statement);
        question.setTags(JSONUtil.toJsonStr(data.tags));
        question.setAnswer(data.explanation);
        question.setSource(data.source);
        question.setSourceUrl(data.sourceUrl);
        question.setLicense(data.license);
        question.setPackageType(data.packageType);
        question.setReferenceLanguage(data.language);
        question.setReferenceSolution(data.referenceSolution);
        question.setJudgeCase(JSONUtil.toJsonStr(data.cases));
        question.setJudgeConfig(JSONUtil.toJsonStr(data.config));
        question.setSubmitNum(0);
        question.setAcceptedNum(0);
        question.setUserId(userId);
        questionService.validQuestion(question, true);
        if (!questionService.save(question)) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目保存失败");

        ProblemImportResultVO result = new ProblemImportResultVO();
        result.setQuestionId(question.getId());
        result.setTitle(question.getTitle());
        result.setPackageType(data.packageType);
        result.setCaseCount(data.cases.size());
        result.setGeneratedCaseCount(data.generatedCases);
        result.setStatus("DRAFT");
        result.getChecks().add("ZIP 安全检查通过");
        result.getChecks().add("题面、限制和测试用例检查通过");
        result.getChecks().add("标准答案已在沙箱通过全部 " + data.cases.size() + " 个用例");
        result.getChecks().add("已保存为草稿，需管理员确认后发布");
        return result;
    }

    private Map<String, byte[]> readZip(MultipartFile source) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        int total = 0;
        try (ZipInputStream input = new ZipInputStream(source.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (files.size() >= MAX_ENTRIES) throw invalid("ZIP 文件数量超过 " + MAX_ENTRIES);
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.matches("^[A-Za-z]:.*") || List.of(name.split("/")).contains("..")) {
                    throw invalid("ZIP 包含非法路径");
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read == 0) continue;
                    output.write(buffer, 0, read);
                    if (output.size() > MAX_ENTRY_BYTES) throw invalid("单个文件不能超过 2MB：" + name);
                }
                total += output.size();
                if (total > MAX_TOTAL_BYTES) throw invalid("ZIP 解压后不能超过 20MB");
                files.put(name, output.toByteArray());
            }
        } catch (IOException error) {
            throw invalid("ZIP 无法读取或已经损坏");
        }
        if (files.isEmpty()) throw invalid("ZIP 中没有文件");
        return stripSingleRoot(files);
    }

    private Map<String, byte[]> stripSingleRoot(Map<String, byte[]> files) {
        if (files.containsKey("problem.yaml") || files.containsKey("cookie-oj.yaml")) return files;
        String root = files.keySet().iterator().next().split("/", 2)[0] + "/";
        if (files.keySet().stream().allMatch(name -> name.startsWith(root))) {
            return files.entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey().substring(root.length()), Map.Entry::getValue,
                    (left, right) -> left, LinkedHashMap::new));
        }
        return files;
    }

    @SuppressWarnings("unchecked")
    private PackageData parse(Map<String, byte[]> files) {
        String metadataName = files.containsKey("cookie-oj.yaml") ? "cookie-oj.yaml" : "problem.yaml";
        byte[] metadataBytes = files.get(metadataName);
        if (metadataBytes == null) throw invalid("缺少 cookie-oj.yaml 或 problem.yaml");
        if (metadataBytes.length > 64 * 1024) throw invalid("题目配置文件过大");
        Object loaded;
        try {
            loaded = yaml.load(text(metadataBytes));
        } catch (RuntimeException error) {
            throw invalid("YAML 配置格式错误");
        }
        if (!(loaded instanceof Map)) throw invalid("YAML 根节点必须是对象");
        Map<String, Object> metadata = (Map<String, Object>) loaded;
        PackageData data = new PackageData();
        data.packageType = "cookie-oj.yaml".equals(metadataName) ? "COOKIE_OJ_V1" : "ICPC";
        if (files.containsKey("domjudge-problem.ini")) data.packageType = "DOMJUDGE";
        data.hasSecretCases = files.keySet().stream().anyMatch(name ->
                name.startsWith("data/secret/") && name.endsWith(".in"));
        data.hasCustomValidator = files.keySet().stream().anyMatch(name ->
                name.startsWith("output_validators/") || name.startsWith("output_validator/"));
        String problemType = string(metadata.get("type"), "pass-fail");
        data.unsupportedProblemType = !"pass-fail".equalsIgnoreCase(problemType)
                && !"standard".equalsIgnoreCase(problemType);
        data.title = localized(metadata.get("title"));
        if (StringUtils.isBlank(data.title)) data.title = localized(metadata.get("name"));
        data.difficulty = upper(string(metadata.get("difficulty"), "MEDIUM"));
        data.tags = strings(metadata.containsKey("tags") ? metadata.get("tags") : metadata.get("keywords"));
        data.source = localized(metadata.get("source"));
        data.sourceUrl = string(metadata.get("sourceUrl"), string(metadata.get("source_url"), null));
        data.license = string(metadata.get("license"), "unknown");
        data.statement = firstText(files, name -> name.equals("statement.md"),
                name -> name.matches("statement/problem\\.(zh|zh-cn|en)\\.md"),
                name -> name.startsWith("statement/") && name.endsWith(".md"),
                name -> name.equals("problem.md"));
        data.explanation = firstText(files, name -> name.equals("solution.md"),
                name -> name.startsWith("solution/solution.") && name.endsWith(".md"));
        data.referenceSolution = firstText(files, name -> name.equals("solution/Main.java"),
                name -> name.startsWith("submissions/accepted/") && name.endsWith(".java"),
                name -> name.startsWith("solution/") && name.endsWith(".java"));
        data.language = "java";
        data.config = limits(metadata, files);
        data.cases.addAll(readCases(files));
        data.generatedCases = generateCases(files, data.cases);
        return data;
    }

    @SuppressWarnings("unchecked")
    private JudgeConfig limits(Map<String, Object> metadata, Map<String, byte[]> files) {
        JudgeConfig config = new JudgeConfig();
        Object value = metadata.get("limits");
        Map<String, Object> limits = value instanceof Map ? (Map<String, Object>) value : Map.of();
        config.setTimeLimit(number(limits, "timeLimit", "time_limit", 1000L));
        config.setMemoryLimit(number(limits, "memoryLimit", "memory_limit", 262144L));
        config.setStackLimit(number(limits, "stackLimit", "stack_limit", 65536L));
        byte[] ini = files.get("domjudge-problem.ini");
        if (ini != null) {
            for (String line : text(ini).split("\\R")) {
                String[] pair = line.split("=", 2);
                if (pair.length == 2 && "timelimit".equalsIgnoreCase(pair[0].trim())) {
                    try { config.setTimeLimit((long) (Double.parseDouble(pair[1].trim()) * 1000)); }
                    catch (NumberFormatException ignored) { throw invalid("domjudge-problem.ini 的 timelimit 无效"); }
                }
            }
            dataPackageMarker(files);
        }
        return config;
    }

    private void dataPackageMarker(Map<String, byte[]> files) {
        // 方法存在是为了明确 DOMjudge 扩展文件已被识别，无需执行其中的程序配置。
    }

    private List<JudgeCase> readCases(Map<String, byte[]> files) {
        List<String> inputs = files.keySet().stream()
                .filter(name -> name.startsWith("data/sample/") || name.startsWith("data/secret/"))
                .filter(name -> name.endsWith(".in")).sorted().collect(Collectors.toList());
        List<JudgeCase> cases = new ArrayList<>();
        for (String inputName : inputs) {
            String base = inputName.substring(0, inputName.length() - 3);
            byte[] answer = files.get(base + ".ans");
            if (answer == null && inputName.startsWith("data/sample/")) answer = files.get(base + ".out");
            if (answer == null) throw invalid("测试输入缺少对应 .ans：" + inputName);
            JudgeCase item = new JudgeCase();
            item.setInput(text(files.get(inputName)));
            item.setOutput(text(answer));
            cases.add(item);
        }
        return cases;
    }

    @SuppressWarnings("unchecked")
    private int generateCases(Map<String, byte[]> files, List<JudgeCase> cases) {
        byte[] bytes = files.get("generator.yaml");
        if (bytes == null) return 0;
        Object loaded = yaml.load(text(bytes));
        if (!(loaded instanceof Map)) throw invalid("generator.yaml 格式错误");
        Map<String, Object> generator = (Map<String, Object>) loaded;
        String type = string(generator.get("type"), "");
        if (!"integer-pair".equals(type)) throw invalid("当前只支持安全生成器 type: integer-pair");
        int count = Math.toIntExact(longValue(generator.get("count"), 20));
        if (count < 1 || count > 80) throw invalid("生成用例数量必须为 1 到 80");
        long min = longValue(generator.get("min"), -1000);
        long max = longValue(generator.get("max"), 1000);
        if (min > max) throw invalid("生成器 min 不能大于 max");
        String operation = string(generator.get("operation"), "sum");
        Random random = new Random(longValue(generator.get("seed"), 20261008L));
        for (int i = 0; i < count; i++) {
            long a = randomLong(random, min, max), b = randomLong(random, min, max);
            BigInteger left = BigInteger.valueOf(a), right = BigInteger.valueOf(b);
            BigInteger answer;
            if ("sum".equals(operation)) answer = left.add(right);
            else if ("subtract".equals(operation)) answer = left.subtract(right);
            else if ("multiply".equals(operation)) answer = left.multiply(right);
            else throw invalid("生成器 operation 仅支持 sum、subtract、multiply");
            JudgeCase item = new JudgeCase();
            item.setInput(a + " " + b);
            item.setOutput(answer.toString());
            cases.add(item);
        }
        return count;
    }

    private void validate(PackageData data) {
        if (StringUtils.isBlank(data.title) || data.title.length() > 512) throw invalid("题目标题为空或过长");
        if (!List.of("EASY", "MEDIUM", "HARD").contains(data.difficulty)) throw invalid("difficulty 必须是 EASY/MEDIUM/HARD");
        if (StringUtils.isBlank(data.statement)) throw invalid("缺少 Markdown 题面");
        if (StringUtils.isBlank(data.referenceSolution)) throw invalid("缺少 Java 标准答案");
        if (!data.referenceSolution.matches("(?s).*public\\s+class\\s+Main\\b.*")) throw invalid("Java 标准答案必须包含 public class Main");
        if (data.hasCustomValidator) throw invalid("当前不支持 DOMjudge 自定义输出判题器，请改为标准答案精确比对题");
        if (data.unsupportedProblemType) throw invalid("当前只支持 ICPC/DOMjudge 的 pass-fail 标准题，不支持交互题或特殊题型");
        if (data.cases.isEmpty()) throw invalid("至少需要一个测试用例");
        if (!data.hasSecretCases && data.generatedCases == 0) throw invalid("至少需要一个 data/secret 隐藏用例或 generator.yaml 生成用例");
        if (data.cases.size() > MAX_CASES) throw invalid("测试用例不能超过 " + MAX_CASES);
        for (JudgeCase item : data.cases) {
            if (item.getInput().getBytes(StandardCharsets.UTF_8).length > 128 * 1024
                    || item.getOutput().getBytes(StandardCharsets.UTF_8).length > 128 * 1024) {
                throw invalid("单个测试输入或输出不能超过 128KB");
            }
        }
        QuestionJudgeValidator.validate(JSONUtil.toJsonStr(data.cases), JSONUtil.toJsonStr(data.config));
    }

    private void verifyReferenceSolution(PackageData data) {
        ExecuteCodeRequest request = ExecuteCodeRequest.builder()
                .protocolVersion("1.0").requestId("problem-import-" + UUID.randomUUID())
                .language(data.language).code(data.referenceSolution)
                .inputList(data.cases.stream().map(JudgeCase::getInput).collect(Collectors.toList()))
                .timeLimitMs(data.config.getTimeLimit()).memoryLimitKb(data.config.getMemoryLimit())
                .stackLimitKb(data.config.getStackLimit()).build();
        ExecuteCodeResponse response;
        try {
            response = new CodeSandboxProxy(remoteCodeSandbox).executeCode(request);
        } catch (RuntimeException error) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标准答案验证失败：判题沙箱不可用");
        }
        if (!"1".equals(response.getStatus()) || response.getOutputList() == null
                || response.getOutputList().size() != data.cases.size()) {
            throw invalid("标准答案编译或执行失败：" + StringUtils.defaultString(response.getMessage(), response.getVerdict()));
        }
        for (int i = 0; i < data.cases.size(); i++) {
            if (!DefaultJudgeStrategy.normalize(data.cases.get(i).getOutput())
                    .equals(DefaultJudgeStrategy.normalize(response.getOutputList().get(i)))) {
                throw invalid("标准答案未通过第 " + (i + 1) + " 个测试用例");
            }
        }
    }

    @SafeVarargs
    private final String firstText(Map<String, byte[]> files, Predicate<String>... choices) {
        for (Predicate<String> choice : choices) {
            String name = files.keySet().stream().filter(choice).sorted().findFirst().orElse(null);
            if (name != null) return text(files.get(name));
        }
        return null;
    }

    private List<String> strings(Object value) {
        if (!(value instanceof Iterable)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object item : (Iterable<?>) value) if (item != null && result.size() < 20) result.add(String.valueOf(item));
        return result;
    }

    @SuppressWarnings("unchecked")
    private String localized(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (String key : List.of("zh-CN", "zh", "en")) if (map.get(key) != null) return String.valueOf(map.get(key));
            return map.values().stream().findFirst().map(String::valueOf).orElse(null);
        }
        return value == null ? null : String.valueOf(value);
    }

    private long number(Map<String, Object> map, String camel, String snake, long fallback) {
        Object value = map.containsKey(camel) ? map.get(camel) : map.get(snake);
        return longValue(value, fallback);
    }

    private long longValue(Object value, long fallback) {
        if (value == null) return fallback;
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException error) { throw invalid("数值配置无效：" + value); }
    }

    private long randomLong(Random random, long min, long max) {
        if (min == max) return min;
        long bound = max - min + 1;
        if (bound > 0) return min + Math.floorMod(random.nextLong(), bound);
        long value;
        do value = random.nextLong(); while (value < min || value > max);
        return value;
    }

    private String text(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8).replace("\u0000", ""); }
    private String string(Object value, String fallback) { return value == null ? fallback : String.valueOf(value).trim(); }
    private String upper(String value) { return StringUtils.defaultString(value).toUpperCase(Locale.ROOT); }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.PARAMS_ERROR, message); }

    private static class PackageData {
        String title, difficulty, statement, explanation, source, sourceUrl, license;
        String packageType, language, referenceSolution;
        JudgeConfig config;
        List<String> tags = new ArrayList<>();
        List<JudgeCase> cases = new ArrayList<>();
        int generatedCases;
        boolean hasSecretCases, hasCustomValidator, unsupportedProblemType;
    }
}

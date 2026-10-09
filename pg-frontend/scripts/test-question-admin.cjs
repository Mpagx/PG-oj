const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const ts = require("typescript");
const source = fs.readFileSync(
  path.join(__dirname, "../src/utils/questionAdmin.ts"),
  "utf8"
);
const moduleUnderTest = { exports: {} };
const js = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS },
}).outputText;
new Function("module", "exports", js)(moduleUnderTest, moduleUnderTest.exports);
const {
  DEFAULT_JUDGE_CONFIG,
  parseTags,
  parseCases,
  parseConfig,
  judgeDataError,
  questionSummary,
} = moduleUnderTest.exports;
const cases = [{ input: "1 2\n", output: "3\n" }];
assert.deepEqual(parseTags('["入门",null,4,"数学"]'), ["入门", "数学"]);
assert.deepEqual(parseTags("broken"), []);
assert.equal(parseCases("broken"), null);
assert.equal(parseCases('[{"input":4,"output":"3"}]'), null);
assert.deepEqual(parseCases(JSON.stringify(cases)), cases);
assert.equal(parseConfig('{"timeLimit":"1000"}'), null);
assert.equal(judgeDataError(cases, DEFAULT_JUDGE_CONFIG), "");
assert.equal(
  judgeDataError([{ input: "", output: "" }], DEFAULT_JUDGE_CONFIG),
  ""
);
assert.ok(judgeDataError([], DEFAULT_JUDGE_CONFIG));
assert.ok(judgeDataError(Array(51).fill(cases[0]), DEFAULT_JUDGE_CONFIG));
for (const config of [
  { ...DEFAULT_JUDGE_CONFIG, timeLimit: 99 },
  { ...DEFAULT_JUDGE_CONFIG, timeLimit: 10000.5 },
  { ...DEFAULT_JUDGE_CONFIG, memoryLimit: 1000 },
  { ...DEFAULT_JUDGE_CONFIG, stackLimit: 255 },
])
  assert.ok(judgeDataError(cases, config));
assert.ok(
  judgeDataError(
    [{ input: "汉".repeat(6000), output: "" }],
    DEFAULT_JUDGE_CONFIG
  )
);
assert.ok(
  judgeDataError(
    Array(17).fill({ input: "a".repeat(16384), output: "" }),
    DEFAULT_JUDGE_CONFIG
  )
);
assert.ok(
  judgeDataError(
    [{ input: "", output: "a".repeat(1048577) }],
    DEFAULT_JUDGE_CONFIG
  )
);
const summary = questionSummary({
  title: "A+B",
  content: "statement",
  tags: '["入门"]',
  judgeCase: JSON.stringify(cases),
  judgeConfig: JSON.stringify(DEFAULT_JUDGE_CONFIG),
  submitNum: 16,
  acceptedNum: 2,
});
assert.equal(summary.issue, "");
assert.equal(summary.passRate, "12.5%");
assert.equal(summary.caseCount, 1);
assert.equal(questionSummary({}).passRate, "—");
assert.ok(questionSummary({}).issue);
console.log(
  "PASS: admin JSON parsing, readiness, pass rate, config boundaries and UTF-8 case budgets"
);

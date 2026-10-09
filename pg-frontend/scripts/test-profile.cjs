const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const ts = require("typescript");
const source = fs.readFileSync(
  path.join(__dirname, "../src/utils/profile.ts"),
  "utf8"
);
const js = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS },
}).outputText;
const moduleUnderTest = { exports: {} };
new Function("module", "exports", "require", js)(
  moduleUnderTest,
  moduleUnderTest.exports,
  require
);
const { buildActivity, submissionVerdict } = moduleUnderTest.exports;
assert.deepEqual(submissionVerdict({ status: 0 }), {
  text: "等待判题",
  color: "gray",
});
assert.deepEqual(
  submissionVerdict({ status: 2, judgeInfo: { message: "Accepted" } }),
  {
    text: "已通过",
    color: "green",
  }
);
assert.equal(
  submissionVerdict({ status: 2, judgeInfo: { message: "Wrong Answer" } }).text,
  "答案错误"
);
const activity = buildActivity(
  [{ day: "2026-10-07", submissionCount: 7 }],
  new Date(2026, 9, 7)
);
assert.equal(activity.length, 84);
assert.equal(activity[0].day, "2026-07-16");
assert.deepEqual(activity.at(-1), { day: "2026-10-07", count: 7, level: 4 });
console.log("PASS: profile verdict labels and 12-week activity grid");

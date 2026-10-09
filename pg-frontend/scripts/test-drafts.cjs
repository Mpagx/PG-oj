const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const ts = require("typescript");
function load(file, directory = "utils") {
  const source = fs.readFileSync(
    path.join(__dirname, "../src", directory, file),
    "utf8"
  );
  const js = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS },
  }).outputText;
  const module = { exports: {} };
  new Function("module", "exports", "require", js)(
    module,
    module.exports,
    (name) =>
      name.startsWith("@/utils/") ? load(name.slice(8) + ".ts") : require(name)
  );
  return module.exports;
}
const { codeTemplate } = load("codeTemplate.ts");
const { javaSubmissionError } = load("javaSubmission.ts");
const { draftKey, readDraft, writeDraft, DRAFT_TTL } = load("codeDraft.ts");
class MemoryStorage {
  data = new Map();
  get length() {
    return this.data.size;
  }
  key(index) {
    return [...this.data.keys()][index] ?? null;
  }
  getItem(key) {
    return this.data.get(key) ?? null;
  }
  setItem(key, value) {
    this.data.set(key, value);
  }
  removeItem(key) {
    this.data.delete(key);
  }
}
const storage = new MemoryStorage();
const key = draftKey("1", "15", "java");
assert.equal(javaSubmissionError(codeTemplate("java")), null);
assert.ok(
  codeTemplate("java").includes("public static void main(String[] args)")
);
assert.equal(readDraft(storage, key), null);
writeDraft(storage, key, "my code", 100);
assert.equal(readDraft(storage, key, 101).code, "my code");
assert.equal(readDraft(storage, draftKey("2", "15", "java"), 101), null);
assert.equal(readDraft(storage, draftKey("1", "16", "java"), 101), null);
assert.equal(readDraft(storage, draftKey("1", "15", "python"), 101), null);
writeDraft(storage, key, "", 102);
assert.equal(readDraft(storage, key, 103).code, "");
assert.equal(readDraft(storage, key, 102 + DRAFT_TTL + 1), null);
storage.setItem(key, "invalid json");
assert.equal(readDraft(storage, key), null);
assert.throws(() => writeDraft(storage, key, "a".repeat(65537)));
const quotaStorage = new MemoryStorage();
quotaStorage.setItem = () => {
  throw Error("quota");
};
assert.throws(() => writeDraft(quotaStorage, key, "x"), /quota/);
storage.setItem("unrelated", "keep");
for (let i = 0; i < 70; i++)
  writeDraft(storage, draftKey("1", String(i), "java"), String(i), i + 1000);
assert.equal(storage.length, 51);
assert.equal(storage.getItem("unrelated"), "keep");
assert.equal(readDraft(storage, draftKey("1", "0", "java"), 2000), null);
assert.equal(readDraft(storage, draftKey("1", "69", "java"), 2000).code, "69");
console.log(
  "PASS: Java template, draft roundtrip, isolation, empty draft, expiry, corruption, size/quota and retention"
);

// Exercise Vue watchers and real mount/unmount hooks without a browser dependency.
const { createRenderer, ref } = require("vue");
const listeners = new Map();
const localStorage = new MemoryStorage();
global.window = {
  localStorage,
  addEventListener: (name, fn) => listeners.set(name, fn),
  removeEventListener: (name) => listeners.delete(name),
};
global.document = { ...global.window, visibilityState: "hidden" };
const { useCodeDraft } = load("useCodeDraft.ts", "composables");
const renderer = createRenderer({
  createComment: () => ({}),
  insert() {},
  remove() {},
  parentNode() {},
  nextSibling() {},
});
const user = ref("1"),
  question = ref("15"),
  language = ref("java"),
  code = ref("");
let controls;
const app = renderer.createApp({
  setup() {
    controls = useCodeDraft(
      () => [user.value, question.value, language.value],
      code
    );
    return () => null;
  },
});
app.mount({});
assert.equal(code.value, codeTemplate("java"));
code.value = "draft 15";
question.value = "16";
assert.equal(readDraft(localStorage, key).code, "draft 15");
assert.equal(code.value, codeTemplate("java"));
code.value = "draft 16";
question.value = "15";
assert.equal(code.value, "draft 15");
assert.equal(controls.draftStatus.value, "已恢复本机草稿");
code.value = "account 1";
user.value = "2";
assert.equal(code.value, codeTemplate("java"));
assert.equal(readDraft(localStorage, key).code, "account 1");
code.value = "account 2";
listeners.get("pagehide")();
assert.equal(
  readDraft(localStorage, draftKey("2", "15", "java")).code,
  "account 2"
);
code.value = "final draft";
app.unmount();
assert.equal(
  readDraft(localStorage, draftKey("2", "15", "java")).code,
  "final draft"
);
assert.equal(listeners.size, 0);
console.log(
  "PASS: Vue template hydration, question/account switching, pagehide and unmount saving"
);

const refreshedCode = ref("");
const refreshedApp = renderer.createApp({
  setup() {
    useCodeDraft(() => ["2", "15", "java"], refreshedCode);
    return () => null;
  },
});
refreshedApp.mount({});
assert.equal(refreshedCode.value, "final draft");
refreshedCode.value = "debounced draft";
setTimeout(() => {
  try {
    assert.equal(
      readDraft(localStorage, draftKey("2", "15", "java")).code,
      "debounced draft"
    );
    console.log(
      "PASS: remount restores draft and typing auto-saves after debounce"
    );
  } finally {
    refreshedApp.unmount();
  }
}, 600);

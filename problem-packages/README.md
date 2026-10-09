# Cookie OJ 题目包

每道题使用一个 ZIP。ZIP 根目录（或唯一的一级目录）必须包含 `cookie-oj.yaml`。

```text
cookie-oj.yaml
statement.md
solution/Main.java
solution.md                  # 可选，管理员题解
data/sample/01.in
data/sample/01.ans
data/secret/01.in
data/secret/01.ans
generator.yaml              # 可选，安全内置生成器
```

导入限制：ZIP 25MB、解压后 20MB、最多 300 个文件、最多 100 个用例。导入时会拒绝危险路径和压缩炸弹。必须包含至少一个 `data/secret` 隐藏用例，或使用 `generator.yaml` 生成隐藏用例。标准答案必须是 Java `public class Main`，并在当前代码沙箱通过全部用例后，题目才会以 `DRAFT` 保存。

`generator.yaml` 当前提供不执行外部脚本的安全生成器 `integer-pair`：

```yaml
type: integer-pair
count: 20
min: -1000000
max: 1000000
operation: sum        # sum / subtract / multiply
seed: 20261008
```

也兼容 ICPC Problem Package Format 的 `problem.yaml`、Markdown 题面、`data/sample`、`data/secret` 与 Java accepted submission，并识别 DOMjudge 的 `domjudge-problem.ini`。当前只接收普通 `pass-fail` 题；交互题和带自定义 output validator 的特殊判题题会明确拒绝，避免被错误地按文本精确比对导入。

## 原创起步题库

`starter/` 提供 6 道 Cookie OJ 原创题：N 个整数的和、最大公约数、回文字符串、整数排序、二分查找和最大连续子数组和。`starter-zips/` 是可直接在管理员“批量导入 ZIP”中多选上传的成品包。导入后全部进入草稿，管理员检查题面和用例数量后再逐题发布。

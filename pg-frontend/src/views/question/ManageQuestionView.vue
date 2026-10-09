<template>
  <div id="manageQuestionView">
    <header class="page-heading">
      <div>
        <span class="eyebrow">PROBLEM MANAGEMENT</span>
        <h1>题目管理</h1>
        <p>维护题面、判题配置和测试用例，让每一道题都能正确评测。</p>
      </div>
      <a-space wrap>
        <input
          ref="packageInput"
          type="file"
          accept=".zip,application/zip"
          multiple
          class="hidden-input"
          @change="selectPackages"
        />
        <a-button size="large" @click="packageInput?.click()"
          >批量导入 ZIP</a-button
        >
        <a-button
          type="primary"
          size="large"
          @click="router.push('/add/question')"
          >＋ 新建题目</a-button
        >
      </a-space>
    </header>
    <div class="overview">
      <div>
        <span>筛选结果</span><strong>{{ total }}<small> 道题目</small></strong>
      </div>
      <div>
        <span>本页配置完整</span
        ><strong class="positive"
          >{{ readyCount }}<small> / {{ rows.length }}</small></strong
        >
      </div>
      <div>
        <span>本页待完善</span
        ><strong :class="{ warning: rows.length - readyCount > 0 }"
          >{{ rows.length - readyCount }}<small> 道题目</small></strong
        >
      </div>
      <div>
        <span>本页草稿</span
        ><strong :class="{ warning: draftCount > 0 }"
          >{{ draftCount }}<small> 道题目</small></strong
        >
      </div>
    </div>
    <a-card :bordered="false" class="list-card">
      <a-form
        :model="filters"
        layout="vertical"
        class="filter-form"
        @submit="search"
      >
        <a-form-item label="题目名称" field="title"
          ><a-input
            v-model="filters.title"
            placeholder="搜索题目名称"
            allow-clear
        /></a-form-item>
        <a-form-item label="题目 ID" field="id"
          ><a-input v-model="filters.id" placeholder="精确查找 ID" allow-clear
        /></a-form-item>
        <a-form-item label="题目标签" field="tag"
          ><a-input
            v-model="filters.tag"
            placeholder="如：入门、数组"
            allow-clear
        /></a-form-item>
        <a-form-item label="发布状态" field="status"
          ><a-select
            v-model="filters.status"
            placeholder="全部状态"
            allow-clear
          >
            <a-option value="DRAFT">草稿</a-option>
            <a-option value="PUBLISHED">已发布</a-option>
          </a-select></a-form-item
        >
        <a-form-item label="题目难度" field="difficulty"
          ><a-select
            v-model="filters.difficulty"
            placeholder="全部难度"
            allow-clear
          >
            <a-option value="EASY">简单</a-option>
            <a-option value="MEDIUM">中等</a-option>
            <a-option value="HARD">困难</a-option>
          </a-select></a-form-item
        >
        <a-space class="filter-actions"
          ><a-button type="primary" html-type="submit">查询</a-button
          ><a-button @click="resetFilters">重置</a-button></a-space
        >
      </a-form>
      <div class="list-heading">
        <div>
          <h2>题目列表</h2>
          <span>完整题面与用例在「详情」中查看，不再显示原始 JSON。</span>
        </div>
        <a-button :loading="loading" @click="loadData">刷新列表</a-button>
      </div>
      <a-alert
        v-if="loadError"
        type="error"
        :show-icon="true"
        class="load-error"
        >{{ loadError }}，请点击刷新重试。</a-alert
      >
      <a-table
        :columns="columns"
        :data="rows"
        row-key="id"
        :loading="loading"
        :scroll="{ x: 1200 }"
        :pagination="{
          showTotal: true,
          showPageSize: true,
          pageSizeOptions: [10, 20, 50],
          current: query.current,
          pageSize: query.pageSize,
          total,
        }"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      >
        <template #title="{ record }"
          ><button
            class="title-link"
            :title="record.title"
            @click="openDetails(record)"
          >
            {{ record.title || "未命名题目" }}
          </button>
          <div class="tag-list">
            <a-tag
              size="small"
              :color="record.status === 'PUBLISHED' ? 'green' : 'gray'"
              >{{ record.status === "PUBLISHED" ? "已发布" : "草稿" }}</a-tag
            >
            <a-tag
              size="small"
              :color="difficultyMeta(record.difficulty).color"
              >{{ difficultyMeta(record.difficulty).label }}</a-tag
            >
            <a-tag
              v-for="tag in record.summary.tags.slice(0, 3)"
              :key="tag"
              size="small"
              color="arcoblue"
              >{{ tag }}</a-tag
            ><span v-if="record.summary.tags.length > 3"
              >＋{{ record.summary.tags.length - 3 }}</span
            ><span v-if="!record.summary.tags.length" class="muted"
              >未设置标签</span
            >
          </div></template
        >
        <template #readiness="{ record }"
          ><a-tag :color="record.summary.issue ? 'orange' : 'green'">{{
            record.summary.issue ? "待完善" : "配置完整"
          }}</a-tag>
          <div
            v-if="record.summary.issue"
            class="cell-note"
            :title="record.summary.issue"
          >
            {{ record.summary.issue }}
          </div></template
        >
        <template #cases="{ record }"
          ><strong>{{ record.summary.caseCount }}</strong
          ><span class="muted"> 个用例</span></template
        >
        <template #limits="{ record }"
          ><template v-if="record.summary.config"
            ><div>{{ record.summary.config.timeLimit }} ms</div>
            <div class="cell-note">
              {{ memoryLabel(record.summary.config.memoryLimit) }} MB 内存
            </div></template
          ><span v-else class="warning">未配置</span></template
        >
        <template #statistics="{ record }"
          ><strong>{{ record.summary.passRate }}</strong>
          <div class="cell-note">
            {{ record.summary.accepted }} 通过 /
            {{ record.summary.submitted }} 提交
          </div></template
        >
        <template #updated="{ record }"
          ><span class="date-cell">{{
            formatDate(record.updateTime || record.createTime) || "—"
          }}</span></template
        >
        <template #operations="{ record }"
          ><a-space :size="4"
            ><a-button size="small" @click="openDetails(record)">详情</a-button
            ><a-button type="outline" size="small" @click="edit(record)"
              >编辑</a-button
            ><a-button
              status="danger"
              size="small"
              :loading="deletingId === record.id"
              :disabled="deletingId !== null"
              @click="confirmDelete(record)"
              >删除</a-button
            ></a-space
          ></template
        >
        <template #empty
          ><div class="empty-state">
            <h3>{{ loadError ? "题目暂时无法加载" : "没有找到题目" }}</h3>
            <p>
              {{
                loadError
                  ? "请检查服务后刷新列表。"
                  : "调整搜索条件，或创建第一道题目。"
              }}
            </p>
            <a-button
              v-if="!loadError"
              type="outline"
              @click="router.push('/add/question')"
              >新建题目</a-button
            >
          </div></template
        >
      </a-table>
    </a-card>
    <a-modal
      v-model:visible="detailsVisible"
      :width="960"
      :title="
        selected ? '#' + selected.id + ' · ' + selected.title : '题目详情'
      "
      :footer="false"
      modal-class="question-detail-modal"
    >
      <div v-if="selected" class="detail-body">
        <a-alert v-if="selected.summary.issue" type="warning"
          >{{ selected.summary.issue }}，请编辑完善后再用于判题。</a-alert
        >
        <div class="detail-meta">
          <a-tag :color="selected.status === 'PUBLISHED' ? 'green' : 'gray'">{{
            selected.status === "PUBLISHED" ? "已发布" : "草稿"
          }}</a-tag>
          <a-tag :color="difficultyMeta(selected.difficulty).color">{{
            difficultyMeta(selected.difficulty).label
          }}</a-tag>
          <a-tag v-for="tag in selected.summary.tags" :key="tag">{{
            tag
          }}</a-tag
          ><span>创建者 #{{ selected.userId ?? "—" }}</span
          ><span>{{ selected.summary.caseCount }} 个用例</span
          ><span v-if="selected.summary.config"
            >时间 {{ selected.summary.config.timeLimit }} ms · 内存
            {{ memoryLabel(selected.summary.config.memoryLimit) }} MB · 栈
            {{ selected.summary.config.stackLimit }} KB</span
          >
        </div>
        <a-tabs v-model:active-key="detailTab">
          <a-tab-pane key="statement" title="题目预览"
            ><MdViewer
              v-if="detailTab === 'statement'"
              :value="selected.content || '暂无题面'"
          /></a-tab-pane>
          <a-tab-pane key="answer" title="参考题解（管理员）"
            ><MdViewer
              v-if="detailTab === 'answer'"
              :value="selected.answer || '暂无参考题解'"
          /></a-tab-pane>
          <a-tab-pane
            key="cases"
            :title="'判题用例（' + selected.summary.caseCount + '）'"
            ><p class="muted">
              这是实际用于评测的隐藏数据，并非题面中的公开样例。
            </p>
            <div
              v-for="(item, index) in selected.summary.cases || []"
              :key="index"
              class="case-preview"
            >
              <h3>用例 {{ index + 1 }}</h3>
              <div class="io-grid">
                <div>
                  <label>标准输入</label>
                  <pre>{{ item.input || "（空输入）" }}</pre>
                </div>
                <div>
                  <label>期望输出</label>
                  <pre>{{ item.output || "（空输出）" }}</pre>
                </div>
              </div>
            </div>
            <p v-if="!selected.summary.caseCount" class="warning">
              暂无有效用例，请编辑补充。
            </p></a-tab-pane
          >
        </a-tabs>
        <div class="detail-footer">
          <a-button
            status="danger"
            :loading="deletingId === selected.id"
            :disabled="deletingId !== null"
            @click="confirmDelete(selected)"
            >删除题目</a-button
          ><span class="detail-footer-spacer"></span
          ><a-button @click="detailsVisible = false">关闭</a-button
          ><a-button @click="router.push('/view/question/' + selected.id)"
            >打开做题页</a-button
          ><a-button type="primary" @click="edit(selected)"
            >编辑这道题</a-button
          >
        </div>
      </div>
    </a-modal>
    <a-modal
      v-model:visible="importVisible"
      title="批量导入题目包"
      :width="720"
      :footer="false"
    >
      <a-alert type="info" class="modal-alert">
        支持 Cookie OJ v1、ICPC 与 DOMjudge
        ZIP。系统会进行安全检查、校验用例并让标准答案通过沙箱后保存为草稿。
      </a-alert>
      <div v-for="item in importQueue" :key="item.file.name" class="import-row">
        <div>
          <strong>{{ item.file.name }}</strong
          ><small>{{ fileSize(item.file.size) }}</small>
        </div>
        <a-tag
          :color="
            item.state === 'success'
              ? 'green'
              : item.state === 'error'
              ? 'red'
              : 'gray'
          "
        >
          {{
            item.message || (item.state === "pending" ? "等待导入" : "正在验证")
          }}
        </a-tag>
      </div>
      <a-empty v-if="!importQueue.length">请选择一个或多个题目 ZIP</a-empty>
      <div class="modal-actions">
        <a-button @click="packageInput?.click()">继续选择</a-button>
        <a-button
          type="primary"
          :loading="importing"
          :disabled="!importQueue.length"
          @click="runImports"
        >
          开始校验并导入
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref } from "vue";
import {
  Question,
  QuestionQueryRequest,
  QuestionControllerService,
} from "@/generated";
import message from "@arco-design/web-vue/es/message";
import Modal from "@arco-design/web-vue/es/modal";
import { useRouter } from "vue-router";
import { formatDate } from "@/utils/date";
import { questionSummary } from "@/utils/questionAdmin";
import { importProblemZip } from "@/api/problemCatalog";

const MdViewer = defineAsyncComponent(
  () => import("@/components/MdViewer.vue")
);
const router = useRouter();
type QuestionRow = Question & { summary: ReturnType<typeof questionSummary> };
const rows = ref<QuestionRow[]>([]);
const total = ref(0);
const loading = ref(false);
const loadError = ref("");
const deletingId = ref<number | null>(null);
const filters = ref({
  title: "",
  id: "",
  tag: "",
  status: undefined as string | undefined,
  difficulty: undefined as string | undefined,
});
const query = ref<QuestionQueryRequest>({ current: 1, pageSize: 10 });
const readyCount = computed(
  () => rows.value.filter((row) => !row.summary.issue).length
);
const draftCount = computed(
  () => rows.value.filter((row) => row.status !== "PUBLISHED").length
);
const selected = ref<QuestionRow>();
const detailsVisible = ref(false);
const detailTab = ref("statement");
const packageInput = ref<HTMLInputElement>();
const importVisible = ref(false);
const importing = ref(false);
type ImportQueueItem = {
  file: File;
  state: "pending" | "running" | "success" | "error";
  message: string;
};
const importQueue = ref<ImportQueueItem[]>([]);
const fileSize = (bytes: number) => `${(bytes / 1024 / 1024).toFixed(2)} MB`;
const selectPackages = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  input.value = "";
  if (!files.length) return;
  const known = new Set(
    importQueue.value.map((item) => item.file.name + item.file.size)
  );
  for (const file of files) {
    if (!file.name.toLowerCase().endsWith(".zip")) continue;
    if (!known.has(file.name + file.size))
      importQueue.value.push({ file, state: "pending", message: "" });
  }
  importVisible.value = true;
};
const runImports = async () => {
  importing.value = true;
  for (const item of importQueue.value) {
    if (item.state === "success") continue;
    item.state = "running";
    item.message = "安全检查和标准答案验证中";
    try {
      const res = await importProblemZip(item.file);
      if (res.code !== 0 || !res.data)
        throw new Error(res.message || "导入失败");
      item.state = "success";
      item.message = `草稿 #${res.data.questionId} · ${res.data.caseCount} 个用例`;
    } catch (error) {
      item.state = "error";
      item.message = error instanceof Error ? error.message : "导入失败";
    }
  }
  importing.value = false;
  await loadData();
};
const memoryLabel = (kb: number) => Number((kb / 1024).toFixed(1));
const difficultyMeta = (difficulty?: string) =>
  difficulty === "EASY"
    ? { label: "简单", color: "green" }
    : difficulty === "HARD"
    ? { label: "困难", color: "red" }
    : { label: "中等", color: "orange" };
let requestVersion = 0;
const loadData = async () => {
  const version = ++requestVersion;
  loading.value = true;
  loadError.value = "";
  try {
    const res = await QuestionControllerService.listQuestionByPageUsingPost({
      ...query.value,
    });
    if (version !== requestVersion) return;
    if (res.code !== 0) throw Error(res.message || "加载失败");
    rows.value = (res.data?.records ?? []).map((question: Question) => ({
      ...question,
      summary: questionSummary(question),
    }));
    total.value = res.data?.total ?? 0;
  } catch (error) {
    if (version === requestVersion) {
      rows.value = [];
      total.value = 0;
      loadError.value = error instanceof Error ? error.message : "题目加载失败";
    }
  } finally {
    if (version === requestVersion) loading.value = false;
  }
};
const search = () => {
  const id = filters.value.id.trim();
  if (id && (!/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id)))) {
    message.warning("请输入有效的题目 ID");
    return;
  }
  query.value = {
    current: 1,
    pageSize: query.value.pageSize,
    title: filters.value.title.trim() || undefined,
    id: id ? Number(id) : undefined,
    tags: filters.value.tag.trim() ? [filters.value.tag.trim()] : undefined,
    status: filters.value.status,
    difficulty: filters.value.difficulty,
  };
  loadData();
};
const resetFilters = () => {
  filters.value = {
    title: "",
    id: "",
    tag: "",
    status: undefined,
    difficulty: undefined,
  };
  search();
};
const onPageChange = (current: number) => {
  query.value.current = current;
  loadData();
};
const onPageSizeChange = (pageSize: number) => {
  query.value.pageSize = pageSize;
  query.value.current = 1;
  loadData();
};
const openDetails = (question: QuestionRow) => {
  selected.value = question;
  detailTab.value = "statement";
  detailsVisible.value = true;
};
const edit = (question: Question) =>
  router.push({ path: "/update/question", query: { id: question.id } });
const confirmDelete = (question: Question) => {
  Modal.confirm({
    title: "删除题目？",
    content: `即将删除 #${question.id}「${question.title}」，删除后将不再出现在题库中。请确认不是只需要修改题目。`,
    okText: "确认删除",
    cancelText: "取消",
    okButtonProps: { status: "danger" },
    onOk: async () => {
      if (!question.id || deletingId.value !== null) return false;
      deletingId.value = question.id;
      try {
        const res = await QuestionControllerService.deleteQuestionUsingPost({
          id: question.id,
        });
        if (res.code !== 0) {
          message.error(res.message || "删除失败");
          return false;
        }
        message.success("题目已删除");
        if (selected.value?.id === question.id) {
          detailsVisible.value = false;
          selected.value = undefined;
        }
        if (rows.value.length === 1 && (query.value.current ?? 1) > 1)
          query.value.current = (query.value.current ?? 1) - 1;
        await loadData();
        return true;
      } catch {
        message.error("删除失败，请检查服务后重试");
        return false;
      } finally {
        deletingId.value = null;
      }
    },
  });
};
const columns = [
  { title: "ID", dataIndex: "id", width: 84 },
  { title: "题目 / 标签", slotName: "title", width: 270 },
  { title: "配置检查", slotName: "readiness", width: 145 },
  { title: "测试用例", slotName: "cases", width: 105 },
  { title: "资源限制", slotName: "limits", width: 135 },
  { title: "通过率", slotName: "statistics", width: 150 },
  { title: "最近更新", slotName: "updated", width: 115 },
  {
    title: "操作",
    slotName: "operations",
    width: 190,
    fixed: "right" as const,
  },
];
onMounted(loadData);
</script>

<style scoped>
#manageQuestionView {
  width: 100%;
  color: var(--color-text-1);
}
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}
.hidden-input {
  display: none;
}
.modal-alert {
  margin-bottom: 16px;
}
.detail-footer-spacer {
  flex: 1;
}
.import-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border-2);
}
.import-row small {
  display: block;
  margin-top: 4px;
  color: var(--color-text-3);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
.eyebrow {
  color: #165dff;
  letter-spacing: 1.8px;
  font-size: 11px;
  font-weight: 600;
}
h1 {
  font-size: 28px;
  margin: 7px 0 8px;
}
.page-heading p,
.muted {
  color: var(--color-text-3);
}
.page-heading p {
  margin: 0;
  line-height: 1.6;
}
.overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.overview > div {
  background: var(--color-bg-2);
  padding: 20px 24px;
  border: 1px solid var(--color-border-2);
  border-radius: 12px;
}
.overview span {
  display: block;
  font-size: 13px;
  color: var(--color-text-3);
}
.overview strong {
  display: block;
  font-size: 28px;
  margin-top: 10px;
}
.overview small {
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-3);
}
.positive {
  color: #00a870;
}
.warning {
  color: #d46b08;
}
.list-card {
  border-radius: 12px;
}
.filter-form {
  display: grid;
  grid-template-columns: 1.4fr 0.7fr 1fr 1fr 1fr auto;
  gap: 16px;
  border-bottom: 1px solid var(--color-border-2);
  margin-bottom: 20px;
}
.filter-actions {
  align-self: start;
  margin-top: 29px;
}
.list-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
h2 {
  font-size: 16px;
  margin: 0 0 8px;
}
.list-heading span {
  color: var(--color-text-3);
  font-size: 12px;
}
.title-link {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 0;
  padding: 0;
  background: transparent;
  color: #165dff;
  font: inherit;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}
.title-link:hover {
  text-decoration: underline;
}
.tag-list {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
}
.cell-note {
  font-size: 12px;
  color: var(--color-text-3);
  margin-top: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.date-cell {
  white-space: nowrap;
}
.load-error {
  margin-bottom: 16px;
}
:deep(.arco-table-th),
:deep(.arco-table-td) {
  white-space: nowrap;
}
:deep(.arco-table-cell) {
  overflow: hidden;
}
.empty-state {
  padding: 32px 16px;
  text-align: center;
}
.empty-state p {
  color: var(--color-text-3);
}
.detail-body {
  max-height: 65vh;
  overflow-y: auto;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin: 16px 0;
  color: var(--color-text-3);
  font-size: 12px;
}
.case-preview {
  margin: 16px 0;
  padding: 16px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
}
.case-preview h3 {
  margin: 0 0 14px;
  font-size: 14px;
}
.io-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.io-grid > div {
  min-width: 0;
}
.io-grid label {
  color: var(--color-text-3);
  font-size: 12px;
}
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 220px;
  overflow: auto;
  padding: 12px;
  background: var(--color-bg-2);
  border-radius: 6px;
  font-family: Consolas, monospace;
}
.detail-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 16px 0 0;
  background: var(--color-bg-2);
  border-top: 1px solid var(--color-border-2);
}
@media (max-width: 760px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .overview {
    gap: 8px;
  }
  .overview > div {
    padding: 14px 10px;
  }
  .overview strong {
    font-size: 23px;
  }
  .filter-form {
    grid-template-columns: 1fr 1fr;
  }
  .filter-actions {
    margin-top: 29px;
  }
  .list-heading {
    align-items: flex-start;
  }
  .io-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 480px) {
  .filter-form {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .filter-actions {
    margin: 0 0 20px;
  }
  .overview {
    grid-template-columns: 1fr;
  }
  .list-heading {
    flex-direction: column;
  }
}
</style>
<style>
.question-detail-modal {
  max-width: calc(100vw - 32px);
}
</style>

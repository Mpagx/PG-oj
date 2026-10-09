<template>
  <div id="questionsView">
    <section
      v-if="showGuestGuide"
      class="guest-guide"
      aria-label="登录注册提示"
    >
      <div class="guide-copy">
        <span class="guide-eyebrow">WELCOME TO COOKIE OJ</span>
        <h1>登录后开始你的做题之旅</h1>
        <p>
          你可以先浏览公开题目；登录或注册后即可运行代码、正式提交，并保存做题记录与通过数据。
        </p>
      </div>
      <div class="guide-actions">
        <a-button type="primary" size="large" @click="goLogin">
          立即登录
        </a-button>
        <a-button size="large" @click="goRegister">注册账号</a-button>
      </div>
    </section>
    <a-card :bordered="false" class="page-card">
      <template #title>题目列表</template>
      <a-form :model="searchParams" layout="inline">
        <a-form-item field="title" label="名称" style="min-width: 240px">
          <a-input v-model="searchParams.title" placeholder="请输入名称" />
        </a-form-item>
        <a-form-item field="tags" label="标签" style="min-width: 240px">
          <a-input-tag
            v-model="searchParams.tags"
            placeholder="输入标签后回车或失焦"
            @blur="handleBlur"
          />
        </a-form-item>
        <a-form-item field="difficulty" label="难度" style="min-width: 160px">
          <a-select
            v-model="searchParams.difficulty"
            placeholder="全部难度"
            allow-clear
          >
            <a-option value="EASY">简单</a-option>
            <a-option value="MEDIUM">中等</a-option>
            <a-option value="HARD">困难</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="doSubmit">搜索</a-button>
        </a-form-item>
      </a-form>
      <a-divider :size="0" />
      <a-table
        :ref="tableRef"
        :columns="columns"
        :data="dataList"
        :pagination="{
          showTotal: true,
          pageSize: searchParams.pageSize,
          current: searchParams.current,
          total,
        }"
        @page-change="onPageChange"
      >
        <template #tags="{ record }">
          <a-space wrap>
            <a-tag
              v-for="(tag, index) of record.tags"
              :key="index"
              color="green"
              >{{ tag }}
            </a-tag>
          </a-space>
        </template>
        <template #difficulty="{ record }">
          <a-tag :color="difficultyMeta(record.difficulty).color">{{
            difficultyMeta(record.difficulty).label
          }}</a-tag>
        </template>
        <template #acceptedRate="{ record }">
          {{
            record.submitNum
              ? Math.floor((record.acceptedNum / record.submitNum) * 100)
              : 0
          }}%
        </template>
        <template #createTime="{ record }">
          {{ formatTime(record.createTime) }}
        </template>
        <template #optional="{ record }">
          <a-space>
            <a-button type="primary" @click="toQuestionPage(record)">
              做题</a-button
            >
          </a-space>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { QuestionVO, QuestionControllerService } from "@/generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import { watch } from "vue";
import { formatDate as formatTime } from "@/utils/date";
import { QuestionQueryRequest } from "@/generated";
import ACCESS_ENUM from "@/access/accessEnum";

const tableRef = ref();

const dataList = ref<QuestionVO[]>([]);
const total = ref(0);
const searchParams = ref<QuestionQueryRequest>({
  title: "",
  difficulty: undefined,
  tags: [] as string[],
  pageSize: 10,
  current: 1,
});
watch(
  () => [searchParams.value.current, searchParams.value.pageSize],
  () => {
    loadData();
  },
  { deep: true }
);
const loadData = async () => {
  const res = await QuestionControllerService.listQuestionVoByPageUsingPost(
    searchParams.value
  );
  if (res.code === 0) {
    dataList.value = res.data.records;
    total.value = res.data.total;
  } else {
    message.error("加载失败，" + res.message);
  }
};
/**
 * 监听 searchParams 变量，改变时触发页面的重新加载
 */

/**
 * 页面加载时，请求数据
 */
onMounted(() => {
  loadData();
});

// {id: "1", title: "A+ D", content: "新的题目内容", tags: "["二叉树"]", answer: "新的答案", submitNum: 0,…}

const columns = [
  {
    title: "题号",
    dataIndex: "id",
  },
  {
    title: "题目名称",
    dataIndex: "title",
  },
  {
    title: "难度",
    slotName: "difficulty",
    width: 90,
  },
  {
    title: "标签",
    slotName: "tags",
  },
  {
    title: "通过率",
    slotName: "acceptedRate",
  },
  {
    title: "创建时间",
    slotName: "createTime",
  },
  {
    title: "",
    slotName: "optional",
  },
];
const difficultyMeta = (difficulty?: string) =>
  difficulty === "EASY"
    ? { label: "简单", color: "green" }
    : difficulty === "HARD"
    ? { label: "困难", color: "red" }
    : { label: "中等", color: "orange" };
const onPageChange = (page: number) => {
  searchParams.value = {
    ...searchParams.value,
    current: page,
  };
};
// const doDelete = async (question: Question) => {
//   const res = await QuestionControllerService.deleteQuestionUsingPost({
//     id: question.id,
//   });
//   if (res.code === 0) {
//     message.success("删除成功");
//     loadData();
//   } else {
//     message.error("删除失败");
//   }
// };

const router = useRouter();
const store = useStore();
const showGuestGuide = computed(
  () =>
    store.state.user.initialized &&
    store.state.user.loginUser?.userRole === ACCESS_ENUM.NOT_LOGIN
);

const goLogin = () =>
  router.push({ path: "/user/login", query: { redirect: "/" } });
const goRegister = () => router.push("/user/register");

const toQuestionPage = (question: QuestionVO) => {
  router.push({
    path: `/view/question/${question.id}`,
  });
};

const doSubmit = () => {
  searchParams.value.current = 1;
  loadData();
};
</script>

<style scoped>
#questionsView {
  width: 100%;
}

.guest-guide {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 20px;
  padding: 28px 32px;
  overflow: hidden;
  border: 1px solid rgba(22, 93, 255, 0.14);
  border-radius: 14px;
  background: radial-gradient(
      circle at 88% 18%,
      rgba(22, 93, 255, 0.16),
      transparent 32%
    ),
    linear-gradient(135deg, #ffffff 0%, #f2f7ff 100%);
  box-shadow: 0 10px 30px rgba(22, 93, 255, 0.08);
}

.guide-copy {
  max-width: 720px;
}

.guide-eyebrow {
  color: #165dff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.guide-copy h1 {
  margin: 8px 0;
  color: #1d2129;
  font-size: 26px;
  line-height: 1.35;
}

.guide-copy p {
  margin: 0;
  color: #4e5969;
  font-size: 15px;
  line-height: 1.7;
}

.guide-actions {
  display: flex;
  flex-shrink: 0;
  gap: 12px;
}

@media (max-width: 720px) {
  .guest-guide {
    align-items: flex-start;
    flex-direction: column;
    gap: 20px;
    padding: 24px;
  }

  .guide-actions {
    width: 100%;
  }

  .guide-actions :deep(.arco-btn) {
    flex: 1;
  }
}
</style>

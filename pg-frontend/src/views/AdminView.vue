<template>
  <div id="adminUserView">
    <header class="page-heading">
      <div>
        <span>ACCOUNT MANAGEMENT</span>
        <h1>用户与账号恢复</h1>
        <p>查看验证邮箱状态，并安全发送密码重置验证码。</p>
      </div>
      <a-button :loading="loading" @click="loadUsers">刷新</a-button>
    </header>
    <a-card :bordered="false">
      <a-form :model="query" layout="inline" @submit="search">
        <a-form-item label="用户名">
          <a-input
            v-model="query.userName"
            placeholder="搜索唯一用户名"
            allow-clear
          />
        </a-form-item>
        <a-form-item
          ><a-button type="primary" html-type="submit"
            >查询</a-button
          ></a-form-item
        >
      </a-form>
      <a-table
        :columns="columns"
        :data="users"
        row-key="id"
        :loading="loading"
        :pagination="{ current, pageSize, total, showTotal: true }"
        @page-change="changePage"
      >
        <template #email="{ record }">
          <template v-if="record.emailVerifiedAt && record.userEmail">
            <span>{{ maskEmail(record.userEmail) }}</span>
            <div class="verified">已验证</div>
          </template>
          <span v-else class="muted">未绑定</span>
        </template>
        <template #role="{ record }">
          <a-tag :color="record.userRole === 'admin' ? 'purple' : 'blue'">{{
            record.userRole === "admin" ? "管理员" : "用户"
          }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-button
            type="text"
            :loading="sendingId === record.id"
            :disabled="!record.emailVerifiedAt || sendingId !== null"
            @click="sendReset(record)"
            >发送重置码</a-button
          >
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import message from "@arco-design/web-vue/es/message";
import Modal from "@arco-design/web-vue/es/modal";
import { User, UserControllerService } from "@/generated";

const query = reactive({ userName: "" });
const users = ref<User[]>([]);
const loading = ref(false);
const sendingId = ref<number | null>(null);
const current = ref(1);
const pageSize = 20;
const total = ref(0);
const columns = [
  { title: "ID", dataIndex: "id", width: 100 },
  { title: "用户名", dataIndex: "userName" },
  { title: "验证邮箱", slotName: "email" },
  { title: "角色", slotName: "role", width: 100 },
  { title: "操作", slotName: "action", width: 150 },
];
const maskEmail = (email: string) => {
  const [name, domain] = email.split("@");
  return domain ? `${name.slice(0, 2)}***@${domain}` : email;
};
const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await UserControllerService.listUserByPageUsingPost({
      current: current.value,
      pageSize,
      userName: query.userName.trim() || undefined,
    });
    if (res.code !== 0) throw Error(res.message || "加载失败");
    users.value = res.data?.records ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "用户列表加载失败");
  } finally {
    loading.value = false;
  }
};
const search = () => {
  current.value = 1;
  loadUsers();
};
const changePage = (page: number) => {
  current.value = page;
  loadUsers();
};
const sendReset = (user: User) => {
  Modal.confirm({
    title: "发送密码重置验证码？",
    content: `系统会向 ${maskEmail(
      user.userEmail ?? ""
    )} 发送验证码，管理员无法看到或修改用户密码。`,
    onOk: async () => {
      if (!user.id) return false;
      sendingId.value = user.id;
      try {
        const res = await UserControllerService.adminSendPasswordResetUsingPost(
          {
            userId: user.id,
          }
        );
        if (res.code !== 0) throw Error(res.message || "发送失败");
        message.success("密码重置验证码已发送");
        return true;
      } catch (error) {
        message.error(error instanceof Error ? error.message : "发送失败");
        return false;
      } finally {
        sendingId.value = null;
      }
    },
  });
};
onMounted(loadUsers);
</script>

<style scoped>
#adminUserView {
  width: 100%;
}
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}
.page-heading span {
  color: #165dff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
}
.page-heading h1 {
  margin: 6px 0;
  font-size: 28px;
}
.page-heading p,
.muted {
  margin: 0;
  color: var(--color-text-3);
}
.verified {
  margin-top: 3px;
  color: #00a870;
  font-size: 12px;
}
@media (max-width: 600px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }
}
</style>

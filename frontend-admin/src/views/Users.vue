<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">
          创建并维护本公司的项目经理与团队成员账号（AD2）
        </p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
    </div>

    <!-- 检索区 -->
    <el-card shadow="never" class="filter-bar">
      <el-form :inline="true" :model="query" @submit.prevent="onSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="用户名或姓名"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
        </el-form-item>

        <el-form-item label="角色">
          <el-select
            v-model="query.role"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="项目经理" value="PM" />
            <el-option label="团队成员" value="MEMBER" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="100" />

        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'PM' ? 'warning' : 'primary'" effect="light">
              {{ row.role === 'PM' ? '项目经理' : '团队成员' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="部门" min-width="120">
          <template #default="{ row }">
            <span v-if="row.deptName">{{ row.deptName }}</span>
            <span v-else class="muted">未分配</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row._switching"
              @change="(v) => onToggleStatus(row, v)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="170" />

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openReset(row)">重置密码</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无用户" />
        </template>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="load"
        @size-change="onSearch"
      />
    </el-card>

    <!-- 新建 / 编辑 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑用户' : '新建用户'"
      width="460px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item v-if="!dialog.isEdit" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用的账号，创建后不可改" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="真实姓名" />
        </el-form-item>

        <el-form-item v-if="!dialog.isEdit" label="初始密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6-32 位，建议 123456，登录后自行修改"
          />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="PM">项目经理</el-radio>
            <el-radio value="MEMBER">团队成员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-alert
          v-if="!dialog.isEdit"
          type="info"
          :closable="false"
          class="role-hint"
          title="管理员账号不能通过此处创建"
        />
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="reset.visible" title="重置密码" width="420px">
      <p class="reset-tip">
        将为 <b>{{ reset.name }}</b> 设置新密码。重置后该用户的登录状态会立即失效。
      </p>
      <el-form ref="resetFormRef" :model="reset" :rules="resetRules">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="reset.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reset.visible = false">取消</el-button>
        <el-button type="primary" :loading="reset.saving" @click="onResetSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  fetchUsers,
  createUser,
  updateUser,
  resetPassword,
} from '@/api/admin-user'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  role: '',
  status: '',
  page: 1,
  size: 20,
})

/* ---------------- 列表 ---------------- */
async function load() {
  loading.value = true
  try {
    // 空字符串会被后端当作有效筛选值，这里剔除掉
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.role) params.role = query.role
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await fetchUsers(params)
    rows.value = data.records
    total.value = data.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  query.keyword = ''
  query.role = ''
  query.status = ''
  onSearch()
}

/* ---------------- 启停 ---------------- */
async function onToggleStatus(row, checked) {
  row._switching = true
  try {
    await updateUser(row.id, { status: checked ? 1 : 0 })
    row.status = checked ? 1 : 0
    ElMessage.success(checked ? '已启用' : '已停用')
  } catch {
    // 失败时不动 row.status，开关会自动回到原位
  } finally {
    row._switching = false
  }
}

/* ---------------- 新建 / 编辑 ---------------- */
const formRef = ref()
const dialog = reactive({ visible: false, isEdit: false, saving: false, id: null })

const form = reactive({ username: '', name: '', password: '', role: 'MEMBER' })

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名过长', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, { username: '', name: '', password: '', role: 'MEMBER' })
  dialog.id = null
}

function openCreate() {
  dialog.isEdit = false
  dialog.visible = true
}

function openEdit(row) {
  dialog.isEdit = true
  dialog.id = row.id
  Object.assign(form, {
    username: row.username,
    name: row.name,
    password: '',
    role: row.role,
  })
  dialog.visible = true
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  dialog.saving = true
  try {
    if (dialog.isEdit) {
      await updateUser(dialog.id, { name: form.name, role: form.role })
      ElMessage.success('已保存')
    } else {
      await createUser({
        username: form.username,
        name: form.name,
        password: form.password,
        role: form.role,
      })
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    load()
  } catch {
    // 拦截器已提示（如用户名重复的 409）
  } finally {
    dialog.saving = false
  }
}

/* ---------------- 重置密码 ---------------- */
const resetFormRef = ref()
const reset = reactive({ visible: false, saving: false, id: null, name: '', newPassword: '' })

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' },
  ],
}

function openReset(row) {
  reset.id = row.id
  reset.name = row.name
  reset.newPassword = ''
  reset.visible = true
}

async function onResetSubmit() {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return

  reset.saving = true
  try {
    await resetPassword(reset.id, reset.newPassword)
    ElMessage.success('密码已重置')
    reset.visible = false
  } catch {
    // 拦截器已提示
  } finally {
    reset.saving = false
  }
}

onMounted(load)
</script>

<style scoped>
.muted {
  color: #c0c4cc;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.role-hint {
  margin-top: 4px;
}

.reset-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
</style>

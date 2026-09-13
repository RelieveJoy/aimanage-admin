<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">项目管理</h2>
        <p class="page-desc">
          新建项目、指定项目经理、维护成员（AD4 / AD5）
        </p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建项目</el-button>
    </div>

    <el-card shadow="never" class="filter-bar">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="项目名称或编号"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="进行中" clearable style="width: 140px">
            <el-option label="进行中" :value="1" />
            <el-option label="已归档" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="code" label="编号" width="100" />

        <el-table-column label="项目名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row)">{{ row.name }}</el-link>
          </template>
        </el-table-column>

        <el-table-column label="项目经理" width="130">
          <template #default="{ row }">
            <span v-if="row.pmName">{{ row.pmName }}</span>
            <el-tag v-else type="danger" size="small" effect="plain">未指定</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="成员" width="90">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row)">{{ row.memberCount }} 人</el-link>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" size="small">
              {{ row.status === 1 ? '进行中' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="170" />

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">成员</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="onToggleArchive(row)"
            >
              {{ row.status === 1 ? '归档' : '恢复' }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="还没有项目" />
        </template>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="load"
        @size-change="onSearch"
      />
    </el-card>

    <!-- 新建 / 编辑 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑项目' : '新建项目'"
      width="480px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="如：爱管理项目" />
        </el-form-item>

        <el-form-item label="项目经理" prop="pmId">
          <el-select v-model="form.pmId" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="pm in pmOptions"
              :key="pm.id"
              :label="`${pm.name}（${pm.username}）`"
              :value="pm.id"
            />
          </el-select>
          <div v-if="!pmOptions.length" class="form-hint warn">
            还没有「项目经理」角色的用户，请先到用户管理里创建一个
          </div>
        </el-form-item>

        <el-form-item label="项目描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="选填"
          />
        </el-form-item>

        <el-form-item v-if="dialog.isEdit && dialog.pmChanged" label=" ">
          <el-alert
            type="warning"
            :closable="false"
            title="更换项目经理后，原项目经理会降为本项目的普通成员"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { fetchProjects, createProject, updateProject } from '@/api/admin-project'
import { fetchUsers } from '@/api/admin-user'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const pmOptions = ref([])

const query = reactive({ keyword: '', status: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await fetchProjects(params)
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
  query.status = ''
  onSearch()
}

/** 项目经理下拉：只取 PM 角色且启用的用户 */
async function loadPmOptions() {
  try {
    const data = await fetchUsers({ role: 'PM', status: 1, page: 1, size: 200 })
    pmOptions.value = data.records
  } catch {
    // 拦截器已提示
  }
}

function goDetail(row) {
  router.push({ name: 'project-detail', params: { id: row.id } })
}

/* ---------------- 新建 / 编辑 ---------------- */
const formRef = ref()
const dialog = reactive({ visible: false, isEdit: false, saving: false, id: null, originalPmId: null })
const form = reactive({ name: '', description: '', pmId: null })

const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  pmId: [{ required: true, message: '请指定项目经理', trigger: 'change' }],
}

const pmChanged = computed(
  () => dialog.isEdit && form.pmId !== null && form.pmId !== dialog.originalPmId,
)
watch(pmChanged, (v) => (dialog.pmChanged = v))

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, { name: '', description: '', pmId: null })
  dialog.id = null
  dialog.originalPmId = null
  dialog.pmChanged = false
}

function openCreate() {
  dialog.isEdit = false
  dialog.visible = true
}

function openEdit(row) {
  dialog.isEdit = true
  dialog.id = row.id
  dialog.originalPmId = row.pmId
  dialog.pmChanged = false
  Object.assign(form, {
    name: row.name,
    description: row.description || '',
    pmId: row.pmId,
  })
  dialog.visible = true
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  dialog.saving = true
  try {
    if (dialog.isEdit) {
      await updateProject(dialog.id, {
        name: form.name,
        description: form.description,
        pmId: form.pmId,
      })
      ElMessage.success('已保存')
    } else {
      await createProject({
        name: form.name,
        description: form.description,
        pmId: form.pmId,
      })
      ElMessage.success('项目已创建')
    }
    dialog.visible = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    dialog.saving = false
  }
}

/* ---------------- 归档 / 恢复 ---------------- */
async function onToggleArchive(row) {
  const archiving = row.status === 1

  try {
    await ElMessageBox.confirm(
      archiving
        ? `归档后「${row.name}」将不再出现在成员的项目列表里，数据全部保留。确定归档吗？`
        : `确定恢复「${row.name}」吗？`,
      archiving ? '归档确认' : '恢复确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await updateProject(row.id, { status: archiving ? 0 : 1 })
    ElMessage.success(archiving ? '已归档' : '已恢复')
    load()
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  await loadPmOptions()
  load()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.form-hint.warn {
  color: #e6a23c;
}
</style>

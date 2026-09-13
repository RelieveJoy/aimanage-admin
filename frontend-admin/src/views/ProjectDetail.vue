<template>
  <div v-loading="loading">
    <div class="page-header">
      <div>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ name: 'projects' }">项目管理</el-breadcrumb-item>
          <el-breadcrumb-item>{{ project.name || '项目详情' }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h2 class="page-title title-with-tag">
          {{ project.name }}
          <el-tag v-if="project.code" size="small" type="info" effect="plain">
            {{ project.code }}
          </el-tag>
          <el-tag
            v-if="project.status !== undefined"
            size="small"
            :type="project.status === 1 ? 'success' : 'info'"
            effect="plain"
          >
            {{ project.status === 1 ? '进行中' : '已归档' }}
          </el-tag>
        </h2>
      </div>
      <el-button :icon="Back" @click="$router.push({ name: 'projects' })">返回列表</el-button>
    </div>

    <!-- 项目概况 -->
    <el-descriptions :column="3" border class="info-card">
      <el-descriptions-item label="项目经理">
        <b>{{ project.pmName || '未指定' }}</b>
      </el-descriptions-item>
      <el-descriptions-item label="成员总数">{{ project.memberCount || 0 }} 人</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ project.createdAt || '—' }}</el-descriptions-item>
      <el-descriptions-item label="项目描述" :span="3">
        {{ project.description || '—' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-tabs v-model="activeTab" class="tabs">
      <!-- ---------------- 成员 ---------------- -->
      <el-tab-pane name="members">
        <template #label>
          <span>项目成员 ({{ members.length }})</span>
        </template>

        <div class="tab-bar">
          <el-button type="primary" :icon="Plus" @click="openAdd">加入成员</el-button>
        </div>

        <el-table :data="members" stripe>
          <el-table-column prop="name" label="姓名" min-width="110" />
          <el-table-column prop="username" label="用户名" min-width="120" />

          <el-table-column label="项目内角色" width="130">
            <template #default="{ row }">
              <el-tag
                :type="row.roleInProject === 'PM' ? 'warning' : 'primary'"
                effect="light"
              >
                {{ row.roleInProject === 'PM' ? '项目经理' : '成员' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="系统角色" width="120">
            <template #default="{ row }">
              {{ row.systemRole === 'PM' ? '项目经理' : row.systemRole === 'ADMIN' ? '管理员' : '团队成员' }}
            </template>
          </el-table-column>

          <el-table-column label="部门" min-width="110">
            <template #default="{ row }">
              <span v-if="row.deptName">{{ row.deptName }}</span>
              <span v-else class="muted">未分配</span>
            </template>
          </el-table-column>

          <el-table-column label="账号状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
                {{ row.status === 1 ? '启用' : '已停用' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="joinedAt" label="加入时间" width="170" />

          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="danger"
                :disabled="row.roleInProject === 'PM'"
                @click="onRemove(row)"
              >
                移出
              </el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="项目还没有成员" />
          </template>
        </el-table>
      </el-tab-pane>

      <!-- ---------------- 成员变更历史（AD5.3） ---------------- -->
      <el-tab-pane name="history">
        <template #label>
          <span>成员变更历史</span>
        </template>

        <el-alert
          type="info"
          :closable="false"
          class="history-tip"
          title="这份记录由系统自动生成，无需人工填写，也不可修改"
          description="每次成员进出都会留下一行：谁、什么时候、做了什么。用它可以回答「这个人是什么时候进项目的」这类问题。"
        />

        <el-table :data="history" stripe>
          <el-table-column prop="createdAt" label="时间" width="180" />
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ roleText(row.operatorRole) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetName" label="涉及成员" width="120" />
          <el-table-column label="动作" width="100">
            <template #default="{ row }">
              <el-tag :type="actionType(row.action)" size="small">
                {{ actionLabel(row.action) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="220">
            <template #default="{ row }">
              {{ row.remark || '—' }}
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无成员变更记录" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 加入成员 -->
    <el-dialog v-model="add.visible" title="加入项目成员" width="440px">
      <el-select
        v-model="add.userId"
        filterable
        placeholder="搜索并选择用户"
        style="width: 100%"
      >
        <el-option
          v-for="u in candidates"
          :key="u.id"
          :label="`${u.name}（${u.username}）`"
          :value="u.id"
        />
      </el-select>
      <p v-if="!candidates.length" class="form-hint">
        没有可加入的用户 —— 要么全部已在项目中，要么还没有创建账号
      </p>

      <template #footer>
        <el-button @click="add.visible = false">取消</el-button>
        <el-button type="primary" :loading="add.saving" :disabled="!add.userId" @click="onAdd">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Back } from '@element-plus/icons-vue'
import {
  fetchProject,
  fetchProjectMembers,
  addProjectMember,
  removeProjectMember,
  fetchMemberHistory,
} from '@/api/admin-project'
import { fetchUsers } from '@/api/admin-user'
import { ACTION_LABELS, ACTION_TAG_TYPES } from '@/api/admin-audit'

const route = useRoute()
const projectId = Number(route.params.id)

const loading = ref(false)
const activeTab = ref('members')

const project = ref({})
const members = ref([])
const history = ref([])
const allUsers = ref([])

async function loadAll() {
  loading.value = true
  try {
    const [p, m, h, u] = await Promise.all([
      fetchProject(projectId),
      fetchProjectMembers(projectId),
      fetchMemberHistory(projectId),
      fetchUsers({ status: 1, page: 1, size: 500 }),
    ])
    project.value = p
    members.value = m
    history.value = h
    allUsers.value = u.records
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

/** 候选 = 启用用户 - 已在项目中的人 */
const candidates = computed(() => {
  const inProject = new Set(members.value.map((m) => m.userId))
  return allUsers.value.filter((u) => !inProject.has(u.id) && u.role !== 'ADMIN')
})

const roleText = (r) => (r === 'ADMIN' ? '管理员' : r === 'PM' ? '项目经理' : '团队成员')
const actionLabel = (a) => ACTION_LABELS[a] || a
const actionType = (a) => ACTION_TAG_TYPES[a] || 'info'

/* ---------------- 加入 / 移出 ---------------- */
const add = reactive({ visible: false, saving: false, userId: null })

function openAdd() {
  add.userId = null
  add.visible = true
}

async function onAdd() {
  add.saving = true
  try {
    await addProjectMember(projectId, add.userId)
    ElMessage.success('已加入项目')
    add.visible = false
    loadAll()
  } catch {
    // 拦截器已提示
  } finally {
    add.saving = false
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确定把「${row.name}」移出本项目吗？这次操作会记入变更历史。`,
      '移出确认',
      { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await removeProjectMember(projectId, row.userId)
    ElMessage.success('已移出项目')
    loadAll()
  } catch {
    // 拦截器已提示
  }
}

onMounted(loadAll)
</script>

<style scoped>
.title-with-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.info-card {
  margin-bottom: 16px;
}

.tabs {
  background: #fff;
  padding: 0 16px 16px;
  border-radius: 4px;
}

.tab-bar {
  margin-bottom: 12px;
}

.history-tip {
  margin-bottom: 14px;
}

.muted {
  color: #c0c4cc;
}

.form-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>

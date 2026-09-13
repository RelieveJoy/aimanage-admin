<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">概览</h2>
        <p class="page-desc">
          {{ auth.displayName }}，欢迎回到爱管理管理员端
        </p>
      </div>
    </div>

    <!-- 统计卡：数据来自用户列表接口的 total，不额外占后端接口 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">
            {{ card.value }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 功能进度：把 Epic AD 的边界显式摆出来 -->
    <el-card shadow="never" class="progress-card">
      <template #header>
        <span class="card-title">管理员端功能进度</span>
      </template>

      <el-table :data="stories" size="small">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="name" label="功能" min-width="220" />
        <el-table-column prop="sprint" label="Sprint" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.done ? 'success' : 'info'" effect="plain" size="small">
              {{ row.done ? '已完成' : '待开发' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <p class="hint">
        进度与《管理员端_故事地图与任务分解》保持一致；完整规划见该文档。
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchUsers } from '@/api/admin-user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const counts = ref({ total: 0, pm: 0, member: 0, disabled: 0 })

const cards = computed(() => [
  { label: '账号总数', value: counts.value.total, color: '#303133' },
  { label: '项目经理', value: counts.value.pm, color: '#e6a23c' },
  { label: '团队成员', value: counts.value.member, color: '#409eff' },
  { label: '已停用', value: counts.value.disabled, color: '#f56c6c' },
])

/**
 * 复用用户列表接口，只取 total 字段。
 * 这样概览页不需要任何新增后端接口 —— AD9 是 Could 级，不该占用后端工时。
 */
async function loadCounts() {
  loading.value = true
  try {
    const [all, pm, member, disabled] = await Promise.all([
      fetchUsers({ page: 1, size: 1 }),
      fetchUsers({ page: 1, size: 1, role: 'PM' }),
      fetchUsers({ page: 1, size: 1, role: 'MEMBER' }),
      fetchUsers({ page: 1, size: 1, status: 0 }),
    ])
    counts.value = {
      total: all.total,
      pm: pm.total,
      member: member.total,
      disabled: disabled.total,
    }
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const stories = [
  { id: 'AD1', name: '管理员登录与入口隔离', sprint: 'Sprint 1', done: true },
  { id: 'AD2', name: '用户账号管理', sprint: 'Sprint 1', done: true },
  { id: 'AD3', name: '组织架构', sprint: 'Sprint 1', done: true },
  { id: 'AD4', name: '项目管理', sprint: 'Sprint 2', done: true },
  { id: 'AD5', name: '项目成员与成员变化', sprint: 'Sprint 2', done: true },
  { id: 'AD7', name: '只读观测（看板 / 甘特）', sprint: 'Sprint 2', done: false },
  { id: 'AD6', name: '全局审计账本检索', sprint: 'Sprint 3', done: true },
  { id: 'AD8', name: '加人申请审批 + 通知中心', sprint: 'Sprint 3', done: false },
]

onMounted(loadCounts)
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  margin-top: 8px;
  line-height: 1.2;
}

.progress-card {
  margin-top: 16px;
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.hint {
  margin: 14px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>

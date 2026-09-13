<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">审计账本</h2>
        <p class="page-desc">
          全链路变更记录，自动生成、不可篡改（AD6）。「这个需求什么时候被改的」—— 在这里三秒出示证据
        </p>
      </div>
    </div>

    <!-- 检索区 -->
    <el-card shadow="never" class="filter-bar">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="项目">
          <el-select
            v-model="query.projectId"
            placeholder="全部"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="p in projectOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="操作人">
          <el-select
            v-model="query.operatorId"
            placeholder="全部"
            clearable
            filterable
            style="width: 160px"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="u.name"
              :value="u.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="对象类型">
          <el-select v-model="query.targetType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="t in TARGET_TYPES"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="对象名 / 备注 / 前后值"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 结果 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        @row-click="openDetail"
        class="audit-table"
      >
        <el-table-column prop="createdAt" label="时间" width="170" />

        <el-table-column label="操作人" width="140">
          <template #default="{ row }">
            <div class="operator">
              <span>{{ row.operatorName || '—' }}</span>
              <el-tag size="small" effect="plain">{{ roleText(row.operatorRole) }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="项目" min-width="130">
          <template #default="{ row }">
            <span v-if="row.projectName">{{ row.projectName }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="对象" min-width="150">
          <template #default="{ row }">
            <span class="target-type">{{ targetTypeLabel(row.targetType) }}</span>
            <span>{{ row.targetName || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="动作" width="90">
          <template #default="{ row }">
            <el-tag :type="actionType(row.action)" size="small" effect="light">
              {{ actionLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- ★ 前后值：方案点名竞品缺的就是这一列 -->
        <el-table-column label="变更内容" min-width="300">
          <template #default="{ row }">
            <div class="change-cell">
              <span class="field-name">{{ row.field || '整体' }}</span>
              <span class="value before">{{ row.beforeValue ?? '空' }}</span>
              <el-icon class="arrow"><Right /></el-icon>
              <span class="value after">{{ row.afterValue ?? '空' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="remark" label="备注" min-width="180">
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有符合条件的变更记录" />
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

    <!-- 详情抽屉：完整证据链 -->
    <el-drawer v-model="detail.visible" title="变更详情" size="520px">
      <div v-if="detail.row" class="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="发生时间">{{ detail.row.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ detail.row.operatorName }}（{{ roleText(detail.row.operatorRole) }}）
          </el-descriptions-item>
          <el-descriptions-item label="所属项目">
            {{ detail.row.projectName || '不属于具体项目' }}
          </el-descriptions-item>
          <el-descriptions-item label="对象">
            {{ targetTypeLabel(detail.row.targetType) }} · {{ detail.row.targetName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="动作">
            <el-tag :type="actionType(detail.row.action)" size="small">
              {{ actionLabel(detail.row.action) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="detail.row.field">
          <h4 class="section-title">变更对比</h4>
          <div class="compare">
            <div class="compare-box before-box">
              <div class="compare-label">变更前</div>
              <div class="compare-value">{{ detail.row.beforeValue ?? '（空）' }}</div>
            </div>
            <el-icon class="compare-arrow"><Right /></el-icon>
            <div class="compare-box after-box">
              <div class="compare-label">变更后</div>
              <div class="compare-value">{{ detail.row.afterValue ?? '（空）' }}</div>
            </div>
          </div>
          <p class="compare-field">字段：<b>{{ detail.row.field }}</b></p>
        </template>

        <template v-if="detail.row.remark">
          <h4 class="section-title">备注</h4>
          <el-alert type="info" :closable="false" :title="detail.row.remark" />
        </template>

        <p class="record-id">记录编号 #{{ detail.row.id }}　·　此记录由系统自动生成，不可修改或删除</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { Search, Refresh, Right } from '@element-plus/icons-vue'
import {
  fetchAudits,
  TARGET_TYPES,
  ACTION_LABELS,
  ACTION_TAG_TYPES,
  targetTypeLabel,
} from '@/api/admin-audit'
import { fetchProjects } from '@/api/admin-project'
import { fetchUsers } from '@/api/admin-user'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const timeRange = ref(null)
const projectOptions = ref([])
const userOptions = ref([])
const detail = reactive({ visible: false, row: null })

const query = reactive({
  projectId: '',
  operatorId: '',
  targetType: '',
  keyword: '',
  page: 1,
  size: 20,
})

const roleText = (r) => (r === 'ADMIN' ? '管理员' : r === 'PM' ? '项目经理' : '团队成员')
const actionLabel = (a) => ACTION_LABELS[a] || a
const actionType = (a) => ACTION_TAG_TYPES[a] || 'info'

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.projectId) params.projectId = query.projectId
    if (query.operatorId) params.operatorId = query.operatorId
    if (query.targetType) params.targetType = query.targetType
    if (query.keyword) params.keyword = query.keyword
    if (timeRange.value?.length === 2) {
      params.startTime = timeRange.value[0]
      params.endTime = timeRange.value[1]
    }

    const data = await fetchAudits(params)
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
  query.projectId = ''
  query.operatorId = ''
  query.targetType = ''
  query.keyword = ''
  timeRange.value = null
  onSearch()
}

function openDetail(row) {
  detail.row = row
  detail.visible = true
}

// 清空筛选下拉时，el-select 会置成 '' 或 null，统一归一化
watch(() => query.projectId, (v) => { if (v === null) query.projectId = '' })
watch(() => query.operatorId, (v) => { if (v === null) query.operatorId = '' })

async function loadOptions() {
  try {
    const [p, u] = await Promise.all([
      fetchProjects({ page: 1, size: 200 }),
      fetchUsers({ page: 1, size: 500 }),
    ])
    projectOptions.value = p.records
    userOptions.value = u.records
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  await loadOptions()
  load()
})
</script>

<style scoped>
.audit-table :deep(.el-table__row) {
  cursor: pointer;
}

.operator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.target-type {
  color: #909399;
  font-size: 12px;
  margin-right: 4px;
}

/* 前后值对比 —— 本页的视觉重点 */
.change-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.field-name {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 1px 6px;
  border-radius: 3px;
  white-space: nowrap;
}

.value {
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 13px;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value.before {
  background: #fef0f0;
  color: #c45656;
  text-decoration: line-through;
}

.value.after {
  background: #f0f9eb;
  color: #529b2e;
  font-weight: 500;
}

.arrow {
  color: #c0c4cc;
}

.muted {
  color: #c0c4cc;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 抽屉 */
.section-title {
  margin: 22px 0 12px;
  font-size: 14px;
  color: #303133;
}

.compare {
  display: flex;
  align-items: center;
  gap: 12px;
}

.compare-box {
  flex: 1;
  border-radius: 6px;
  padding: 12px;
  min-height: 70px;
}

.before-box {
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.after-box {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.compare-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.compare-value {
  font-size: 15px;
  color: #303133;
  word-break: break-all;
}

.compare-arrow {
  color: #c0c4cc;
  font-size: 20px;
  flex-shrink: 0;
}

.compare-field {
  margin: 12px 0 0;
  font-size: 13px;
  color: #606266;
}

.record-id {
  margin-top: 28px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}
</style>

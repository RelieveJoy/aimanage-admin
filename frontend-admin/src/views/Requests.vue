<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">审批与通知</h2>
        <p class="page-desc">
          项目经理提交的加人申请在这里拍板；处理结果会写进审计账本（AD8）
        </p>
      </div>
      <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="tabs">
      <!-- ================= 加人申请 ================= -->
      <el-tab-pane name="requests">
        <template #label>
          <span>
            加人申请
            <el-badge v-if="pendingCount" :value="pendingCount" class="tab-badge" />
          </span>
        </template>

        <div class="tab-bar">
          <el-radio-group v-model="query.status" @change="onSearch">
            <el-radio-button value="PENDING">待审批</el-radio-button>
            <el-radio-button value="APPROVED">已批准</el-radio-button>
            <el-radio-button value="REJECTED">已拒绝</el-radio-button>
            <el-radio-button value="">全部</el-radio-button>
          </el-radio-group>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="项目" min-width="140">
            <template #default="{ row }">
              {{ row.projectName || '—' }}
            </template>
          </el-table-column>

          <el-table-column label="申请人" width="140">
            <template #default="{ row }">
              <div>{{ row.applicantName || '—' }}</div>
              <div class="sub">项目经理</div>
            </template>
          </el-table-column>

          <el-table-column label="申请加入" min-width="160">
            <template #default="{ row }">
              <div>{{ row.targetName || '—' }}</div>
              <div class="sub">
                {{ row.targetDeptName || '未分配部门' }}
                <el-tag
                  v-if="row.targetAlreadyInProject"
                  type="warning"
                  size="small"
                  effect="plain"
                  class="inline-tag"
                >
                  已在项目中
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="reason" label="申请理由" min-width="180">
            <template #default="{ row }">
              <span v-if="row.reason">{{ row.reason }}</span>
              <span v-else class="muted">未填写</span>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="提交时间" width="170" />

          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusOf(row.status).type" effect="plain" size="small">
                {{ statusOf(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="审批信息" min-width="180">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <span class="muted">待处理</span>
              </template>
              <template v-else>
                <div>{{ row.reviewerName || '—' }} · {{ row.reviewedAt }}</div>
                <div class="sub">{{ row.reviewComment || '（未填写意见）' }}</div>
              </template>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button link type="success" @click="openReview(row, 'approve')">
                  批准
                </el-button>
                <el-button link type="danger" @click="openReview(row, 'reject')">
                  拒绝
                </el-button>
              </template>
              <span v-else class="muted">已处理</span>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="没有符合条件的申请" />
          </template>
        </el-table>

        <el-pagination
          v-if="total > 0"
          class="pager"
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </el-tab-pane>

      <!-- ================= 通知 ================= -->
      <el-tab-pane name="notifications">
        <template #label>
          <span>通知</span>
        </template>

        <div class="tab-bar">
          <el-button
            :icon="Check"
            :disabled="!unreadCount"
            @click="onMarkAllRead"
          >
            全部标为已读{{ unreadCount ? ` (${unreadCount})` : '' }}
          </el-button>
        </div>

        <el-empty v-if="!notifications.length" description="暂无通知" />

        <div v-else class="notice-list">
          <div
            v-for="n in notifications"
            :key="n.id"
            class="notice"
            :class="{ unread: !n.read }"
            @click="onReadNotification(n)"
          >
            <div class="notice-main">
              <span class="dot" v-if="!n.read" />
              <span class="notice-title">{{ n.title }}</span>
            </div>
            <div class="notice-time">{{ n.createdAt }}</div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 审批弹窗 -->
    <el-dialog
      v-model="review.visible"
      :title="review.mode === 'approve' ? '批准加人申请' : '拒绝加人申请'"
      width="480px"
    >
      <div v-if="review.row" class="review-summary">
        <p>
          项目：<b>{{ review.row.projectName }}</b>
        </p>
        <p>
          申请加入：<b>{{ review.row.targetName }}</b>
          （{{ review.row.targetDeptName || '未分配部门' }}）
        </p>
        <p>申请人：{{ review.row.applicantName }}</p>
        <p v-if="review.row.reason">理由：{{ review.row.reason }}</p>
        <el-alert
          v-if="review.mode === 'approve'"
          type="info"
          :closable="false"
          title="批准后该成员会立即加入项目，并写入一条审计记录"
        />
        <el-alert
          v-else
          type="warning"
          :closable="false"
          title="拒绝理由会通知申请人，请填写清楚"
        />
      </div>

      <el-form ref="formRef" :model="review" :rules="reviewRules" class="review-form">
        <el-form-item label="审批意见" prop="comment">
          <el-input
            v-model="review.comment"
            type="textarea"
            :rows="3"
            :placeholder="review.mode === 'approve' ? '选填' : '必填，说明拒绝原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="review.visible = false">取消</el-button>
        <el-button
          :type="review.mode === 'approve' ? 'success' : 'danger'"
          :loading="review.saving"
          @click="onReviewSubmit"
        >
          确认{{ review.mode === 'approve' ? '批准' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import {
  fetchRequests,
  fetchPendingCount,
  approveRequest,
  rejectRequest,
  fetchNotifications,
  fetchUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
  REQUEST_STATUS,
} from '@/api/admin-request'

const activeTab = ref('requests')
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const pendingCount = ref(0)

const notifications = ref([])
const unreadCount = ref(0)

const query = reactive({ status: 'PENDING', page: 1, size: 20 })

const statusOf = (s) => REQUEST_STATUS[s] || { label: s, type: 'info' }

async function load() {
  loading.value = true
  try {
    const data = await fetchRequests({
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    })
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

async function loadBadges() {
  try {
    const [p, u] = await Promise.all([fetchPendingCount(), fetchUnreadCount()])
    pendingCount.value = p.count
    unreadCount.value = u.count
  } catch {
    // 拦截器已提示
  }
}

async function loadNotifications() {
  try {
    notifications.value = await fetchNotifications({ unread: false })
  } catch {
    // 拦截器已提示
  }
}

async function refreshAll() {
  await Promise.all([load(), loadBadges(), loadNotifications()])
}

/* ---------------- 审批 ---------------- */
const formRef = ref()
const review = reactive({
  visible: false,
  mode: 'approve',
  saving: false,
  id: null,
  comment: '',
  row: null,
})

const reviewRules = {
  comment: [
    {
      validator: (_rule, value, cb) => {
        // 拒绝必须给理由 —— 和后端同一条规则，前端先挡一道提升体验
        if (review.mode === 'reject' && !value?.trim()) {
          cb(new Error('拒绝时必须填写理由'))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
}

function openReview(row, mode) {
  review.row = row
  review.mode = mode
  review.id = row.id
  review.comment = ''
  review.visible = true
}

async function onReviewSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  review.saving = true
  try {
    if (review.mode === 'approve') {
      await approveRequest(review.id, review.comment)
      ElMessage.success('已批准，成员已加入项目')
    } else {
      await rejectRequest(review.id, review.comment)
      ElMessage.success('已拒绝')
    }
    review.visible = false
    await refreshAll()
  } catch {
    // 拦截器已提示（如 409 已在项目中）
  } finally {
    review.saving = false
  }
}

/* ---------------- 通知 ---------------- */
async function onReadNotification(n) {
  if (n.read) return
  try {
    await markNotificationRead(n.id)
    n.read = true
    await loadBadges()
  } catch {
    // 拦截器已提示
  }
}

async function onMarkAllRead() {
  try {
    await markAllNotificationsRead()
    ElMessage.success('已全部标为已读')
    await Promise.all([loadNotifications(), loadBadges()])
  } catch {
    // 拦截器已提示
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.tabs {
  background: #fff;
  padding: 0 16px 16px;
  border-radius: 4px;
}

.tab-bar {
  margin-bottom: 14px;
}

.tab-badge {
  margin-left: 6px;
}

.sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.inline-tag {
  margin-left: 6px;
}

.muted {
  color: #c0c4cc;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 通知列表 */
.notice-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f2f6fc;
  cursor: pointer;
  transition: background 0.15s;
}

.notice:last-child {
  border-bottom: none;
}

.notice:hover {
  background: #f5f7fa;
}

.notice.unread {
  background: #fdf6ec;
}

.notice.unread:hover {
  background: #faecd8;
}

.notice-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e6a23c;
  flex-shrink: 0;
}

.notice-title {
  color: #303133;
}

.notice-time {
  font-size: 12px;
  color: #909399;
}

/* 审批弹窗 */
.review-summary p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #606266;
}

.review-form {
  margin-top: 16px;
}
</style>

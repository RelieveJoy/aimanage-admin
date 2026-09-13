<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">组织架构</h2>
        <p class="page-desc">
          维护本公司的部门划分与人员归属（AD3）。树是两层的：公司 → 部门
        </p>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 左：部门树 -->
      <el-col :span="8">
        <el-card shadow="never" class="tree-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">部门</span>
              <el-button
                type="primary"
                size="small"
                :icon="Plus"
                :disabled="!companyId"
                @click="openCreate()"
              >
                新建部门
              </el-button>
            </div>
          </template>

          <el-tree
            v-loading="loadingTree"
            ref="treeRef"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="onSelectNode"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span class="node-label">
                  <el-icon v-if="data.isRoot" class="node-icon"><OfficeBuilding /></el-icon>
                  {{ node.label }}
                  <span class="count" v-if="data.memberCount">
                    ({{ data.memberCount }})
                  </span>
                </span>

                <span class="node-actions" @click.stop>
                  <el-button
                    v-if="!data.isRoot"
                    link
                    type="primary"
                    :icon="Edit"
                    @click="openRename(data)"
                  />
                  <el-button
                    v-if="!data.isRoot"
                    link
                    type="danger"
                    :icon="Delete"
                    @click="onDelete(data)"
                  />
                </span>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右：该部门成员 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title">
                {{ currentName || '请选择部门' }}
                <el-tag v-if="currentId" size="small" type="info" effect="plain" class="total-tag">
                  共 {{ memberTotal }} 人
                </el-tag>
              </span>
            </div>
          </template>

          <el-table v-loading="loadingMembers" :data="members" stripe>
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

            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMove(row)">调整部门</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty :description="currentId ? '该部门暂无成员' : '请先在左侧选择部门'" />
            </template>
          </el-table>

          <el-pagination
            v-if="memberTotal > 0"
            class="pager"
            v-model:current-page="memberQuery.page"
            v-model:page-size="memberQuery.size"
            :total="memberTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadMembers"
            @size-change="onSearchMembers"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建 / 重命名部门 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="如：研发部" @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="form-hint">数字越小越靠前</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整成员部门 -->
    <el-dialog v-model="move.visible" title="调整部门" width="420px">
      <p class="move-tip">
        将 <b>{{ move.name }}</b> 调整到：
      </p>
      <el-select v-model="move.deptId" placeholder="请选择部门" style="width: 100%">
        <!-- 0 是"不分配"的哨兵值，后端据此清空部门 -->
        <el-option label="不分配（移出所有部门）" :value="0" />
        <el-option
          v-for="d in deptOptions"
          :key="d.id"
          :label="d.name"
          :value="d.id"
        />
      </el-select>

      <template #footer>
        <el-button @click="move.visible = false">取消</el-button>
        <el-button type="primary" :loading="move.saving" @click="onMoveSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, OfficeBuilding } from '@element-plus/icons-vue'
import {
  fetchDepartmentTree,
  createDepartment,
  updateDepartment,
  deleteDepartment,
} from '@/api/admin-department'
import { fetchUsers, updateUser } from '@/api/admin-user'

/* ---------------- 部门树 ---------------- */
const loadingTree = ref(false)
const tree = ref([])
const treeRef = ref()

const companyId = computed(() => tree.value[0]?.id ?? null)
const companyName = computed(() => tree.value[0]?.name ?? '')

/** 扁平化的部门列表（不含公司根节点），供下拉选择用 */
const deptOptions = computed(() => {
  const root = tree.value[0]
  return root?.children ?? []
})

async function loadTree(keepSelection = false) {
  loadingTree.value = true
  try {
    tree.value = await fetchDepartmentTree()
    if (!keepSelection) {
      // 默认选中公司根节点，右侧展示全公司成员
      const root = tree.value[0]
      if (root) {
        treeRef.value?.setCurrentKey(root.id)
        currentId.value = root.id
        currentName.value = root.name
      }
    }
  } catch {
    // 拦截器已提示
  } finally {
    loadingTree.value = false
  }
}

/* ---------------- 右侧成员 ---------------- */
const loadingMembers = ref(false)
const members = ref([])
const memberTotal = ref(0)
const currentId = ref(null)
const currentName = ref('')

const memberQuery = reactive({ page: 1, size: 10 })

function onSelectNode(data) {
  currentId.value = data.id
  currentName.value = data.name
  onSearchMembers()
}

async function loadMembers() {
  if (!currentId.value) return

  loadingMembers.value = true
  try {
    const params = { page: memberQuery.page, size: memberQuery.size }

    // 选中的是公司根节点 → 展示全公司；否则按部门过滤
    if (currentId.value !== companyId.value) {
      params.deptId = currentId.value
    }

    const data = await fetchUsers(params)
    members.value = data.records
    memberTotal.value = data.total
  } catch {
    // 拦截器已提示
  } finally {
    loadingMembers.value = false
  }
}

function onSearchMembers() {
  memberQuery.page = 1
  loadMembers()
}

/* ---------------- 新建 / 重命名 ---------------- */
const formRef = ref()
const dialog = reactive({ visible: false, title: '', saving: false, mode: 'create', id: null })
const form = reactive({ name: '', sort: 0 })

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

function openCreate() {
  dialog.mode = 'create'
  dialog.title = '新建部门'
  dialog.id = null
  form.name = ''
  form.sort = deptOptions.value.length
  dialog.visible = true
}

function openRename(data) {
  dialog.mode = 'rename'
  dialog.title = '重命名部门'
  dialog.id = data.id
  form.name = data.name
  form.sort = data.sort ?? 0
  dialog.visible = true
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  dialog.saving = true
  try {
    if (dialog.mode === 'create') {
      await createDepartment({ name: form.name, sort: form.sort, parentId: companyId.value })
      ElMessage.success('部门已创建')
    } else {
      await updateDepartment(dialog.id, { name: form.name, sort: form.sort })
      ElMessage.success('已保存')
    }
    dialog.visible = false
    await loadTree(true)
    // 重新拉一次右侧，成员数可能变了
    currentName.value = findName(tree.value, currentId.value) || currentName.value
    loadMembers()
  } catch {
    // 拦截器已提示
  } finally {
    dialog.saving = false
  }
}

function findName(nodes, id) {
  for (const n of nodes) {
    if (n.id === id) return n.name
    const hit = findName(n.children || [], id)
    if (hit) return hit
  }
  return null
}

/* ---------------- 删除部门 ---------------- */
async function onDelete(data) {
  try {
    await ElMessageBox.confirm(
      `确定删除部门「${data.name}」吗？部门下有成员或子部门时无法删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await deleteDepartment(data.id)
    ElMessage.success('已删除')

    // 若删的正是当前选中项，退回公司根节点
    if (currentId.value === data.id) {
      currentId.value = companyId.value
      currentName.value = companyName.value
    }
    await loadTree(true)
    treeRef.value?.setCurrentKey(currentId.value)
    loadMembers()
  } catch {
    // 拦截器已提示（409 部门非空）
  }
}

/* ---------------- 调整成员部门 ---------------- */
const move = reactive({ visible: false, saving: false, userId: null, name: '', deptId: 0 })

function openMove(row) {
  move.userId = row.id
  move.name = row.name
  move.deptId = row.deptId ?? 0
  move.visible = true
}

async function onMoveSubmit() {
  move.saving = true
  try {
    await updateUser(move.userId, { deptId: move.deptId })
    ElMessage.success('已调整')
    move.visible = false
    await loadTree(true)
    loadMembers()
  } catch {
    // 拦截器已提示
  } finally {
    move.saving = false
  }
}

onMounted(async () => {
  await loadTree()
  await loadMembers()
})
</script>

<style scoped>
.tree-card :deep(.el-card__body) {
  padding: 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.total-tag {
  margin-left: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.node-icon {
  color: #e6a23c;
}

.count {
  color: #909399;
  font-size: 12px;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.15s;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

.muted {
  color: #c0c4cc;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.move-tip {
  margin: 0 0 14px;
  font-size: 13px;
  color: #606266;
}
</style>

<template>
  <el-container class="layout">
    <!-- 侧边菜单 -->
    <el-aside width="210px" class="aside">
      <div class="logo">
        <span class="logo-main">爱管理</span>
        <span class="logo-sub">管理员端</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.name"
          :index="item.path"
          :disabled="item.disabled"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
          <el-tag
            v-if="item.disabled"
            size="small"
            type="info"
            effect="plain"
            class="soon-tag"
          >
            待开发
          </el-tag>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>

        <el-dropdown @command="onCommand">
          <span class="user">
            <el-avatar :size="28" class="avatar">
              {{ auth.displayName.charAt(0) }}
            </el-avatar>
            {{ auth.displayName }}
            <el-tag size="small" type="danger" effect="plain" class="role-tag">
              管理员
            </el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Odometer,
  User,
  OfficeBuilding,
  Folder,
  Document,
  View,
  Bell,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/**
 * 菜单按 Epic AD 的故事编号排列。
 * disabled 的项是已经在 Sprint 计划里、但尚未实现的故事 —— 先摆出来让边界可见，
 * 也方便演示时说明"这些都规划好了"。
 */
const menus = [
  { name: 'dashboard', path: '/dashboard', title: '概览', icon: Odometer, disabled: false },
  { name: 'users', path: '/users', title: '用户管理', icon: User, disabled: false },
  { name: 'org', path: '/org', title: '组织架构', icon: OfficeBuilding, disabled: true },
  { name: 'projects', path: '/projects', title: '项目管理', icon: Folder, disabled: true },
  { name: 'audit', path: '/audit', title: '审计账本', icon: Document, disabled: true },
  { name: 'observe', path: '/observe', title: '只读观测', icon: View, disabled: true },
  { name: 'notifications', path: '/notifications', title: '审批与通知', icon: Bell, disabled: true },
]

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

async function onCommand(command) {
  if (command !== 'logout') return

  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }

  await auth.logout()
  ElMessage.success('已退出登录')
  router.replace({ name: 'login' })
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #1f2d3d;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-main {
  font-size: 17px;
  letter-spacing: 3px;
  font-weight: 600;
}

.logo-sub {
  font-size: 11px;
  letter-spacing: 2px;
  color: #8a99ab;
  margin-top: 2px;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.soon-tag {
  margin-left: auto;
  transform: scale(0.85);
}

.header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e8eb;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
  color: #606266;
}

.avatar {
  background-color: #3a6ea5;
}

.role-tag {
  margin-left: 2px;
}

.main {
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}
</style>

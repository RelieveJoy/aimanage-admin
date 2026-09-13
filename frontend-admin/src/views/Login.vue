<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <h1>爱管理</h1>
        <p>管理员端</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="onSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="submit-btn"
          :loading="loading"
          @click="onSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <p class="tip">仅管理员账号可登录本端</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const user = await auth.login({ ...form })

    // 后端已经拦住了越权接口，这里再挡一次是为了不让非管理员
    // 进到一个处处报 403 的空壳界面里
    if (user.role !== 'ADMIN') {
      auth.clear()
      ElMessage.error('该账号不是管理员，无法登录管理端')
      return
    }

    ElMessage.success(`欢迎回来，${user.name || user.username}`)
    router.replace(route.query.redirect || { name: 'dashboard' })
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b4a6f 55%, #3a6ea5 100%);
}

.login-card {
  width: 380px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.22);
}

.brand {
  text-align: center;
  margin-bottom: 30px;
}

.brand h1 {
  margin: 0;
  font-size: 26px;
  letter-spacing: 3px;
  color: #1f2d3d;
}

.brand p {
  margin: 8px 0 0;
  font-size: 13px;
  letter-spacing: 2px;
  color: #909399;
}

.submit-btn {
  width: 100%;
  letter-spacing: 4px;
  margin-top: 4px;
}

.tip {
  margin: 20px 0 0;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>

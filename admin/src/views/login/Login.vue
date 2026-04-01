<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 style="text-align: center; margin-bottom: 24px">管理后台{{ isRegister ? '注册' : '登录' }}</h2>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleSubmit">
        <el-form-item v-if="isRegister" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（可选）" />
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="手机号" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
            {{ isRegister ? '注 册' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="switch-box">
        <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
        <el-button type="primary" text @click="toggleMode">{{ isRegister ? '去登录' : '去注册' }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store'
import { adminRegister } from '../../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const isRegister = ref(false)

const form = reactive({ nickname: '', username: '', password: '' })
const rules = computed(() => ({
  username: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}))

function toggleMode() {
  isRegister.value = !isRegister.value
  form.nickname = ''
  form.username = ''
  form.password = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (isRegister.value) {
      const res = await adminRegister(form)
      const payload = {
        username: form.username,
        password: form.password
      }
      await userStore.login(payload)
      ElMessage.success(res.message || '注册成功')
    } else {
      await userStore.login({
        username: form.username,
        password: form.password
      })
      ElMessage.success('登录成功')
    }
    router.push('/')
  } catch (e) {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #2d3a4b;
}

.login-card {
  width: 420px;
}

.switch-box {
  margin-top: 6px;
  text-align: right;
}
</style>


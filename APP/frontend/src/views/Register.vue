<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()

const form = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await register(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    ElMessage.error('注册失败')
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>食库管家</h2>
        <p>注册新账户</p>
      </div>
      <el-form :model="form" :rules="rules" class="login-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称（选填）" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号（选填）" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱（选填）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" block @click="handleSubmit">注 册</el-button>
        </el-form-item>
        <el-form-item class="register-link">
          <span>已有账户？</span>
          <a href="#" @click.prevent="goLogin">立即登录</a>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
}

.login-form {
  padding: 0 20px;
}

.login-form :deep(.el-button) {
  height: 44px;
  font-size: 16px;
}

.register-link {
  text-align: center;
  margin-top: 10px;
}

.register-link span {
  color: #909399;
  font-size: 14px;
}

.register-link a {
  color: #409eff;
  font-size: 14px;
  margin-left: 4px;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
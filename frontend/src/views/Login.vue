<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>北京中考东城区<br />志愿填报与录取模拟系统</h2>

      <!-- 登录 -->
      <el-form v-if="mode === 'login'" :model="loginForm" label-width="80px" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="admin123" @keyup.enter="login" />
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="login">登录</el-button>
        <p class="tip">默认管理员：admin / admin123</p>
        <p class="switch" @click="mode = 'register'">没有账号？立即注册</p>
      </el-form>

      <!-- 注册 -->
      <el-form v-else :model="regForm" label-width="80px" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="regForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="regForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="regForm.role" style="width:100%">
            <el-option label="考生（STUDENT）" value="STUDENT" />
            <el-option label="管理员（ADMIN）" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="regForm.role === 'STUDENT'" label="考生ID">
          <el-input v-model="regForm.studentId" placeholder="关联的考生ID（可选）" />
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="register">注册</el-button>
        <p class="switch" @click="mode = 'login'">已有账号？返回登录</p>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuth } from '../store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuth()
const mode = ref('login')

const loginForm = reactive({ username: 'admin', password: 'admin123' })
const regForm = reactive({ username: '', password: '', role: 'STUDENT', studentId: '' })

async function login() {
  const data = await request.post('/auth/login', loginForm)
  auth.setAuth(data)
  ElMessage.success('登录成功')
  router.push(data.role === 'ADMIN' ? '/admin' : '/student')
}

async function register() {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  const payload = {
    username: regForm.username,
    password: regForm.password,
    role: regForm.role,
    studentId: regForm.role === 'STUDENT' && regForm.studentId ? Number(regForm.studentId) : null
  }
  await request.post('/auth/register', payload)
  ElMessage.success('注册成功，请登录')
  loginForm.username = regForm.username
  mode.value = 'login'
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 380px; }
.login-card h2 { text-align: center; margin-bottom: 20px; font-size: 18px; line-height: 1.5; }
.tip { text-align: center; color: #999; margin-top: 12px; font-size: 12px; }
.switch { text-align: center; color: #409eff; margin-top: 10px; font-size: 13px; cursor: pointer; }
</style>

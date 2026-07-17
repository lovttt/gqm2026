<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>北京中考东城区<br />志愿填报与录取模拟系统</h2>
      <el-form :model="form" label-width="80px" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="admin123" @keyup.enter="login" />
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="login">登录</el-button>
      </el-form>
      <p class="tip">默认管理员：admin / admin123</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuth } from '../store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuth()
const form = reactive({ username: 'admin', password: 'admin123' })

async function login() {
  const data = await request.post('/auth/login', form)
  auth.setAuth(data)
  ElMessage.success('登录成功')
  router.push(data.role === 'ADMIN' ? '/admin' : '/student')
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 380px; }
.login-card h2 { text-align: center; margin-bottom: 20px; font-size: 18px; line-height: 1.5; }
.tip { text-align: center; color: #999; margin-top: 12px; font-size: 12px; }
</style>

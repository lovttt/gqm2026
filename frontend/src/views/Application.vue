<template>
  <el-container style="height:100vh">
    <el-header style="display:flex;align-items:center;justify-content:space-between;background:#001529;color:#fff">
      <span>北京中考东城区 · 志愿填报查询（{{ username }}）</span>
      <el-button size="small" @click="logout">退出登录</el-button>
    </el-header>
    <el-main>
      <el-form :inline="true" style="margin-bottom:10px" @submit.prevent>
        <el-form-item label="考生姓名">
          <el-input v-model="filters.studentName" placeholder="模糊匹配" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="初中校">
          <el-select v-model="filters.juniorSchoolId" placeholder="全部初中" clearable filterable style="width:160px">
            <el-option v-for="j in juniorSchools" :key="j.id" :label="j.name" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次">
          <el-select v-model="filters.batch" placeholder="全部" clearable style="width:120px">
            <el-option label="校额到校" value="QUOTA" />
            <el-option label="统招" value="TONGZHAO" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-tabs v-model="tab">
        <el-tab-pane label="校额到校志愿" name="QUOTA">
          <el-table :data="quotaRows" border v-loading="loading">
            <el-table-column prop="studentId" label="考生ID" width="90" />
            <el-table-column prop="studentName" label="考生" width="110" />
            <el-table-column label="初中校" :formatter="fJs" />
            <el-table-column prop="priority" label="志愿序" width="80" sortable />
            <el-table-column label="填报高中" :formatter="fHs" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="统招志愿" name="TONGZHAO">
          <el-table :data="tongzhaoRows" border v-loading="loading">
            <el-table-column prop="studentId" label="考生ID" width="90" />
            <el-table-column prop="studentName" label="考生" width="110" />
            <el-table-column label="初中校" :formatter="fJs" />
            <el-table-column prop="priority" label="志愿序" width="80" sortable />
            <el-table-column label="填报高中" :formatter="fHs" />
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <el-pagination style="margin-top:10px" layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="page" :page-size="size" :page-sizes="[50, 100, 200]"
        @current-change="(p) => { page = p; load() }"
        @size-change="(s) => { size = s; page = 1; load() }" />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuth } from '../store/auth'

const router = useRouter()
const auth = useAuth()
const username = auth.username

const juniorSchools = ref([])
const highSchools = ref([])
const rows = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(50)
const tab = ref('QUOTA')

const filters = reactive({ studentName: '', juniorSchoolId: null, batch: '' })

const quotaRows = computed(() => rows.value.filter(r => r.batch === 'QUOTA'))
const tongzhaoRows = computed(() => rows.value.filter(r => r.batch === 'TONGZHAO'))

const fJs = (r) => juniorSchools.value.find(j => j.id === r.juniorSchoolId)?.name || r.juniorSchoolId
const fHs = (r) => highSchools.value.find(h => h.id === r.highSchoolId)?.name || r.highSchoolId

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.set('page', String(page.value - 1))
    params.set('size', String(size.value))
    if (filters.studentName && filters.studentName.trim()) params.set('studentName', filters.studentName.trim())
    if (filters.juniorSchoolId != null) params.set('juniorSchoolId', String(filters.juniorSchoolId))
    if (filters.batch) params.set('batch', filters.batch)
    const r = await request.get(`/student/applications?${params.toString()}`)
    rows.value = r.content
    total.value = r.totalElements
  } finally {
    loading.value = false
  }
}
function search() { page.value = 1; load() }
function resetFilters() {
  filters.studentName = ''
  filters.juniorSchoolId = null
  filters.batch = ''
  page.value = 1
  load()
}

onMounted(async () => {
  const [js, hs] = await Promise.all([
    request.get('/school/junior-schools?size=1000'),
    request.get('/school/high-schools?size=1000')
  ])
  juniorSchools.value = Array.isArray(js) ? js : js.content
  highSchools.value = Array.isArray(hs) ? hs : hs.content
  await load()
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

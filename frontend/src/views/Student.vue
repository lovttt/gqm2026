<template>
  <el-container style="height:100vh">
    <el-header style="display:flex;align-items:center;justify-content:space-between;background:#001529;color:#fff">
      <span>北京中考东城区 · 考生端（{{ username }}）</span>
      <div>
        <el-button size="small" type="warning" plain @click="router.push('/generator')">志愿生成器</el-button>
        <el-button size="small" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main v-if="!studentId">
      <el-alert type="info" title="当前账号未关联考生" description="请用已关联考生ID的学生账号登录，或由管理员在用户中绑定 studentId。" />
    </el-main>
    <el-main v-else>
      <el-card v-if="me" style="margin-bottom:16px">
        <template #header>我的成绩</template>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="姓名">{{ me.name }}</el-descriptions-item>
          <el-descriptions-item label="初中校">{{ me ? fJs(me) : '' }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ me.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="语文">{{ me.chinese }}</el-descriptions-item>
          <el-descriptions-item label="数学">{{ me.math }}</el-descriptions-item>
          <el-descriptions-item label="英语">{{ me.english }}</el-descriptions-item>
          <el-descriptions-item label="物理">{{ me.physics }}</el-descriptions-item>
          <el-descriptions-item label="道法">{{ me.politics }}</el-descriptions-item>
          <el-descriptions-item label="体育">{{ me.pe }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card style="margin-bottom:16px">
        <template #header>填报志愿</template>
        <el-form label-width="140px">
          <el-divider content-position="left">校额到校志愿（按优先级 1~10 排序，同一学校分数靠前者先挑）</el-divider>
          <el-form-item v-for="(p, i) in quotaApps" :key="'q' + i" :label="'校额志愿 ' + (i + 1)">
            <el-select v-model="quotaApps[i]" placeholder="选择高中" clearable filterable style="width:300px">
              <el-option v-for="h in highSchools" :key="h.id" :label="h.name" :value="h.id" />
            </el-select>
          </el-form-item>
          <el-divider content-position="left">统招志愿（平行志愿，按优先级排序）</el-divider>
          <el-form-item v-for="(p, i) in tongzhaoApps" :key="'t' + i" :label="'统招志愿 ' + (i + 1)">
            <el-select v-model="tongzhaoApps[i]" placeholder="选择高中" clearable filterable style="width:300px">
              <el-option v-for="h in highSchools" :key="h.id" :label="h.name" :value="h.id" />
            </el-select>
          </el-form-item>
          <el-button type="primary" @click="saveApps">保存志愿</el-button>
        </el-form>
      </el-card>

      <el-card>
        <template #header>我的录取结果</template>
        <el-table :data="results" border v-if="results.length">
          <el-table-column label="批次" width="120">
            <template #default="{ row }">{{ row.batch === 'QUOTA' ? '校额到校' : '统招' }}</template>
          </el-table-column>
          <el-table-column prop="highSchoolName" label="录取高中" />
          <el-table-column prop="juniorSchoolName" label="来源初中" width="150" />
          <el-table-column prop="totalScore" label="总分" width="80" />
          <el-table-column label="校内排名" width="90">
            <template #default="{ row }">
              <span v-if="row.batch === 'QUOTA'">{{ row.schoolRank }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="chinese" label="语文" width="56" />
          <el-table-column prop="math" label="数学" width="56" />
          <el-table-column prop="english" label="英语" width="56" />
          <el-table-column prop="physics" label="物理" width="56" />
          <el-table-column prop="politics" label="道法" width="56" />
          <el-table-column prop="pe" label="体育" width="56" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ADMITTED' ? 'success' : 'danger'">
                {{ row.status === 'ADMITTED' ? '录取' : '未录取' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="说明" />
        </el-table>
        <el-empty v-else description="暂无录取结果，等待管理员模拟录取" />
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuth } from '../store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuth()
const username = auth.username
const studentId = auth.studentId

const highSchools = ref([])
const juniorSchools = ref([])
const fJs = (r) => juniorSchools.value.find(j => j.id === r.juniorSchoolId)?.name || r.juniorSchoolId
const me = ref(null)
const quotaApps = ref(Array(10).fill(null))
const tongzhaoApps = ref(Array(8).fill(null))
const results = ref([])

onMounted(async () => {
  const hsRes = await request.get('/school/high-schools?size=1000')
  highSchools.value = Array.isArray(hsRes) ? hsRes : hsRes.content
  const jsRes = await request.get('/school/junior-schools?size=1000')
  juniorSchools.value = Array.isArray(jsRes) ? jsRes : jsRes.content
  if (studentId) {
    const stuRes = await request.get('/student/students?size=1000')
    const all = Array.isArray(stuRes) ? stuRes : stuRes.content
    me.value = all.find(s => String(s.id) === String(studentId)) || null
    const apps = (await request.get('/student/applications?studentId=' + studentId + '&size=1000')).content
    const quota = apps.filter(a => a.batch === 'QUOTA').sort((a, b) => a.priority - b.priority)
    quotaApps.value = Array(10).fill(null)
    quota.forEach(a => { if (a.priority >= 1 && a.priority <= 10) quotaApps.value[a.priority - 1] = a.highSchoolId })
    const tz = apps.filter(a => a.batch === 'TONGZHAO').sort((a, b) => a.priority - b.priority)
    tongzhaoApps.value = Array(8).fill(null)
    tz.forEach(a => { if (a.priority >= 1 && a.priority <= 8) tongzhaoApps.value[a.priority - 1] = a.highSchoolId })
    results.value = await request.get('/admission/results/student/' + studentId)
  }
})

async function saveApps() {
  const list = []
  quotaApps.value.forEach((hs, i) => {
    if (hs) list.push({ studentId: Number(studentId), batch: 'QUOTA', priority: i + 1, highSchoolId: hs })
  })
  tongzhaoApps.value.forEach((hs, i) => {
    if (hs) list.push({ studentId: Number(studentId), batch: 'TONGZHAO', priority: i + 1, highSchoolId: hs })
  })
  await request.post('/student/applications/student/' + studentId, list)
  ElMessage.success('志愿已保存')
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

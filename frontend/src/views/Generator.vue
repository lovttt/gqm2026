<template>
  <el-container style="height:100vh">
    <el-header style="display:flex;align-items:center;justify-content:space-between;background:#001529;color:#fff">
      <span>北京中考东城区 · 志愿生成器（{{ username }}）</span>
      <div>
        <el-button size="small" @click="goBack">返回</el-button>
        <el-button size="small" @click="logout">退出登录</el-button>
      </div>
    </el-header>

    <el-main>
      <!-- 参数面板 -->
      <el-card style="margin-bottom:16px">
        <template #header>参数设置</template>

        <el-alert v-if="!studentId" type="warning" :closable="false" style="margin-bottom:14px"
          title="当前账号未关联考生，请从下方选择一名考生后再生成志愿" />

        <el-form label-width="150px" :disabled="loading">
          <el-form-item label="目标考生">
            <el-select v-model="selStudentId" filterable clearable placeholder="选择考生" style="width:320px"
              @change="onStudentChange">
              <el-option v-for="s in students" :key="s.id" :label="`${s.name}（总分 ${s.totalScore}）`" :value="s.id" />
            </el-select>
          </el-form-item>

          <el-divider content-position="left">政策规则类</el-divider>

          <el-form-item label="通勤时长上限(分钟)">
            <el-input-number v-model="form.commuteCapMinutes" :min="0" :max="180" :step="5" controls-position="right" />
            <span class="hint">留空=不限制；超出阈值的学校将被自动过滤</span>
          </el-form-item>

          <el-form-item label="高考出口梯队">
            <el-select v-model="form.gaokaoTierPref" clearable placeholder="不限制" style="width:200px">
              <el-option label="TOP级（顶尖出口）" value="TOP" />
              <el-option label="头部（优质出口）" value="HEAD" />
              <el-option label="中上游（稳定出口）" value="MID" />
            </el-select>
            <span class="hint">按用户选择档位匹配对应高考成绩梯队学校</span>
          </el-form-item>

          <el-form-item label="含跨区投放校">
            <el-switch v-model="form.includeCrossDistrict" />
            <span class="hint">是否纳入跨区投放东城计划的学校</span>
          </el-form-item>

          <el-form-item label="综合素质评价">
            <el-select v-model="form.comprehensiveEval" clearable placeholder="使用默认(B等)" style="width:200px">
              <el-option label="A 等" value="A" />
              <el-option label="B 等" value="B" />
              <el-option label="C 等" value="C" />
              <el-option label="D 等" value="D" />
            </el-select>
            <span class="hint">低于 B 等将锁定校额到校批次（仅演示覆盖）</span>
          </el-form-item>

          <el-divider content-position="left">梯度权重（三者之和须为 100%）</el-divider>

          <el-form-item label="冲刺 / 稳妥 / 兜底">
            <el-input-number v-model="form.sprintWeight" :min="0" :max="100" controls-position="right" />
            <span class="wt">冲刺</span>
            <el-input-number v-model="form.steadyWeight" :min="0" :max="100" controls-position="right" />
            <span class="wt">稳妥</span>
            <el-input-number v-model="form.safetyWeight" :min="0" :max="100" controls-position="right" />
            <span class="wt">兜底</span>
            <el-tag :type="weightSum === 100 ? 'success' : 'danger'" style="margin-left:12px">
              合计 {{ weightSum }}%
            </el-tag>
          </el-form-item>
          <el-alert v-if="weightSum !== 100" type="error" :closable="false"
            title="三类志愿权重总和需为 100%，请调整数值" />

          <el-divider content-position="left">偏好权重</el-divider>

          <el-form-item label="通勤距离权重">
            <el-slider v-model="form.commuteWeight" :min="0" :max="100" show-input style="width:320px" />
          </el-form-item>
          <el-form-item label="高考出口权重">
            <el-slider v-model="form.gaokaoOutputWeight" :min="0" :max="100" show-input style="width:320px" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :disabled="weightSum !== 100" :loading="loading" @click="generate">
              生成志愿
            </el-button>
            <el-button @click="resetForm">重置参数</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <template v-if="resp">
        <!-- 校验结果 -->
        <el-card style="margin-bottom:16px">
          <template #header>校验结果</template>
          <el-empty v-if="!resp.issues.length" description="全部校验通过，无阻断/提示项" />
          <el-alert v-for="(it, i) in resp.issues" :key="i" :type="levelType(it.level)" :closable="false"
            show-icon :title="`[${it.code}] ${it.batch || '全局'}`" :description="it.message" style="margin-bottom:8px" />
        </el-card>

        <!-- 批次方案 -->
        <el-card style="margin-bottom:16px">
          <template #header>志愿方案</template>
          <el-alert v-if="resp.guantongHidden" type="warning" :closable="false"
            title="总分低于 380 分，贯通培养项目志愿选项已自动屏蔽" style="margin-bottom:12px" />
          <el-tabs v-model="planTab">
            <el-tab-pane label="校额到校（≤8）" name="QUOTA">
              <plan-table :rows="resp.quotaPlan" :zone-map="zoneMap" />
            </el-tab-pane>
            <el-tab-pane label="统一招生（≤12）" name="TONGZHAO">
              <plan-table :rows="resp.tongzhaoPlan" :zone-map="zoneMap" />
            </el-tab-pane>
            <el-tab-pane label="贯通培养（≤8）" name="GUANTONG">
              <plan-table :rows="resp.guantongPlan" :zone-map="zoneMap" />
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 过滤信息 -->
        <el-card style="margin-bottom:16px" v-if="hasFilters">
          <template #header>已过滤学校</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="通勤超限" v-if="resp.filteredByCommute.length">
              {{ nameList(resp.filteredByCommute) }}
            </el-descriptions-item>
            <el-descriptions-item label="梯队不符" v-if="resp.filteredByGaokaoTier.length">
              {{ nameList(resp.filteredByGaokaoTier) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 偏好权重联动对比 -->
        <el-card v-if="resp.comparisons && resp.comparisons.length">
          <template #header>偏好权重联动对比（上一轮 → 本轮）</template>
          <el-alert type="info" :closable="false" style="margin-bottom:12px"
            :title="`上一轮权重：通勤 ${prev.commuteWeight} / 出口 ${prev.gaokaoOutputWeight} → 本轮：通勤 ${form.commuteWeight} / 出口 ${form.gaokaoOutputWeight}`" />
          <el-collapse v-model="cmpActive">
            <el-collapse-item v-for="c in resp.comparisons" :key="c.batch" :name="c.batch">
              <template #title>
                {{ batchLabel(c.batch) }}（新增 {{ c.added.length }} · 移除 {{ c.removed.length }} · 调序 {{ c.reordered.length }}）
              </template>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="上一轮顺序">{{ nameList(c.before) }}</el-descriptions-item>
                <el-descriptions-item label="本轮顺序">{{ nameList(c.after) }}</el-descriptions-item>
                <el-descriptions-item label="新增" v-if="c.added.length">{{ nameList(c.added) }}</el-descriptions-item>
                <el-descriptions-item label="移除" v-if="c.removed.length">{{ nameList(c.removed) }}</el-descriptions-item>
                <el-descriptions-item label="调序" v-if="c.reordered.length">{{ nameList(c.reordered) }}</el-descriptions-item>
              </el-descriptions>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </template>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuth } from '../store/auth'
import { ElMessage } from 'element-plus'

// 内联子组件：方案表格
const PlanTable = {
  props: { rows: { type: Array, default: () => [] }, zoneMap: { type: Object, default: () => ({}) } },
  setup(props) {
    const bandLabel = (b) => ({ SPRINT: '冲刺', STEADY: '稳妥', SAFETY: '兜底', NONE: '区间外' }[b] || b)
    const tierLabel = (t) => ({ KEY: '重点', NORMAL: '普通' }[t] || t)
    const deltaText = (d) => (d > 0 ? '+' + d : String(d))
    return () => h('el-table', { data: props.rows, border: true, emptyText: '本批次无可选学校' }, () => [
      h('el-table-column', { type: 'index', label: '序', width: 60 }),
      h('el-table-column', { prop: 'highSchoolName', label: '高中' }),
      h('el-table-column', { label: '层次', width: 90, formatter: (r) => tierLabel(r.tier) }),
      h('el-table-column', { prop: 'gaokaoTier', label: '高考梯队', width: 100 }),
      h('el-table-column', { prop: 'referenceScore', label: '参考分', width: 90 }),
      h('el-table-column', { label: '分差', width: 80, formatter: (r) => deltaText(r.delta) }),
      h('el-table-column', { label: '梯度', width: 90, formatter: (r) => bandLabel(r.scoreBand) }),
      h('el-table-column', { prop: 'commuteMinutes', label: '通勤(分)', width: 100 }),
      h('el-table-column', { prop: 'preferenceScore', label: '偏好分', width: 90 })
    ])
  }
}

const router = useRouter()
const auth = useAuth()
const username = auth.username
const studentId = auth.studentId

const loading = ref(false)
const resp = ref(null)
const students = ref([])
const highSchools = ref([])
const zoneMap = computed(() => Object.fromEntries(highSchools.value.map(h => [h.id, h.name])))
const planTab = ref('QUOTA')
const cmpActive = ref([])

const selStudentId = ref(studentId ? Number(studentId) : null)

const form = reactive({
  commuteCapMinutes: null,
  gaokaoTierPref: '',
  includeCrossDistrict: false,
  comprehensiveEval: '',
  sprintWeight: 30,
  steadyWeight: 40,
  safetyWeight: 30,
  commuteWeight: 50,
  gaokaoOutputWeight: 50
})

// 上一轮权重（用于联动对比）
const prev = reactive({ commuteWeight: 50, gaokaoOutputWeight: 50 })

const weightSum = computed(() => form.sprintWeight + form.steadyWeight + form.safetyWeight)
const hasFilters = computed(() => resp.value &&
  (resp.value.filteredByCommute.length || resp.value.filteredByGaokaoTier.length))

function nameList(ids) {
  if (!ids || !ids.length) return '—'
  return ids.map(id => zoneMap.value[id] || id).join('、')
}
function batchLabel(b) {
  return { QUOTA: '校额到校', TONGZHAO: '统一招生', GUANTONG: '贯通培养' }[b] || b
}
function levelType(l) {
  return { BLOCK: 'error', WARN: 'warning', INFO: 'info' }[l] || 'info'
}

function resetForm() {
  form.commuteCapMinutes = null
  form.gaokaoTierPref = ''
  form.includeCrossDistrict = false
  form.comprehensiveEval = ''
  form.sprintWeight = 30
  form.steadyWeight = 40
  form.safetyWeight = 30
  form.commuteWeight = 50
  form.gaokaoOutputWeight = 50
}

function onStudentChange() {
  resp.value = null
}

async function generate() {
  if (!selStudentId.value) {
    ElMessage.warning('请先选择目标考生')
    return
  }
  if (weightSum.value !== 100) {
    ElMessage.error('三类志愿权重总和需为 100%')
    return
  }
  loading.value = true
  try {
    const payload = {
      studentId: Number(selStudentId.value),
      commuteCapMinutes: form.commuteCapMinutes,
      gaokaoTierPref: form.gaokaoTierPref || null,
      includeCrossDistrict: form.includeCrossDistrict,
      comprehensiveEval: form.comprehensiveEval || null,
      sprintWeight: form.sprintWeight,
      steadyWeight: form.steadyWeight,
      safetyWeight: form.safetyWeight,
      commuteWeight: form.commuteWeight,
      gaokaoOutputWeight: form.gaokaoOutputWeight,
      prevCommuteWeight: prev.commuteWeight,
      prevGaokaoOutputWeight: prev.gaokaoOutputWeight
    }
    const data = await request.post('/student/generator/generate', payload)
    resp.value = data
    // 展开全部对比面板
    cmpActive.value = (data.comparisons || []).map(c => c.batch)
    // 记录本轮权重，供下一轮对比
    prev.commuteWeight = form.commuteWeight
    prev.gaokaoOutputWeight = form.gaokaoOutputWeight
    ElMessage.success('志愿方案已生成')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push(auth.role === 'ADMIN' ? '/admin' : '/student')
}
function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  const [hs, stus] = await Promise.all([
    request.get('/school/high-schools?size=1000'),
    request.get('/student/students?size=1000')
  ])
  highSchools.value = Array.isArray(hs) ? hs : hs.content
  const all = Array.isArray(stus) ? stus : stus.content
  students.value = all
})
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; margin-left: 12px; }
.wt { color: #606266; font-size: 13px; margin: 0 8px; }
</style>

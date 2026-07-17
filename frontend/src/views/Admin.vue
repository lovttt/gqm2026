<template>
  <el-container style="height:100vh">
    <el-header style="display:flex;align-items:center;justify-content:space-between;background:#001529;color:#fff">
      <span>北京中考东城区 · 管理员后台（{{ username }}）</span>
      <div>
        <el-button size="small" type="warning" plain @click="goApplications">志愿填报查询</el-button>
        <el-button size="small" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main>
      <el-tabs v-model="active">
        <!-- 高中 -->
        <el-tab-pane label="高中管理" name="hs">
          <el-button type="primary" size="small" @click="openHsDialog()">新增高中</el-button>
          <el-alert v-if="hasQuotaMismatch" type="error" show-icon :closable="false"
            style="margin:10px 0"
            :title="`有 ${quotaMismatchList.length} 所高中的「校额到校录取数」与「校额到校分配数」不一致（已用红色标注），请核对校额名额数据或高中招生计划设置`" />
          <el-table :data="highSchools" border style="margin-top:10px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="高中名称" />
            <el-table-column prop="district" label="区县" width="100" />
            <el-table-column prop="tongzhaoQuota" label="统招计划" width="100" />
            <el-table-column label="校额到校分配数" width="110">
              <template #default="{ row }">{{ quotaForHs(row.id) }}</template>
            </el-table-column>
            <el-table-column label="校额录取数" width="100">
              <template #default="{ row }">
                <span :style="{ color: mismatchHs(row.id) ? '#f56c6c' : 'inherit', fontWeight: mismatchHs(row.id) ? 'bold' : 'normal' }">
                  {{ row.quotaAdmitted }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" @click="openHsDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="delHs(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 初中校 -->
        <el-tab-pane label="初中校管理" name="js">
          <el-button type="primary" size="small" @click="openJsDialog()">新增初中校</el-button>
          <el-table :data="juniorSchools" border style="margin-top:10px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="初中校名称" />
            <el-table-column prop="district" label="区县" width="100" />
            <el-table-column prop="classCount" label="班数" width="80" />
            <el-table-column prop="gradCount" label="毕业生数" width="100" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" @click="openJsDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="delJs(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 校额名额 -->
        <el-tab-pane label="校额到校名额" name="qs">
          <el-button type="primary" size="small" @click="openQsDialog()">新增名额</el-button>
          <el-form :inline="true" style="margin-top:10px">
            <el-form-item label="初中校">
              <el-select v-model="qsFilters.juniorSchoolId" placeholder="全部" clearable filterable style="width:180px" @change="onPickJunior">
                <el-option v-for="j in juniorSchools" :key="j.id" :label="j.name" :value="j.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="高中">
              <el-select v-model="qsFilters.highSchoolId" placeholder="全部" clearable filterable style="width:180px" @change="onPickHigh">
                <el-option v-for="h in highSchools" :key="h.id" :label="h.name" :value="h.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="searchQuotaSeats">查询</el-button>
              <el-button size="small" @click="resetQsFilters">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="quotaSeats" border style="margin-top:10px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="初中校" :formatter="fJs" />
            <el-table-column label="高中" :formatter="fHs" />
            <el-table-column prop="quota" label="名额" width="80" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" :disabled="!(row.juniorSchoolId && row.highSchoolId)" @click="openQsDialog(row)">维护</el-button>
                <el-button size="small" type="danger" :disabled="!row.id" @click="delQs(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:10px;font-size:14px;color:#303133">
            共 <b>{{ quotaSeats.length }}</b> 条记录，合计 <b style="color:#409eff">{{ qsTotalQuota }}</b> 个校额到校名额
          </div>
        </el-tab-pane>

        <!-- 控制线 -->
        <el-tab-pane label="校额控制线" name="cl">
          <el-form label-width="160px" style="max-width:400px;margin-top:10px">
            <el-form-item label="全区最低控制线(QUOTA)">
              <el-input-number v-model="controlLine.value" :min="0" :max="660" />
            </el-form-item>
            <el-button type="primary" @click="saveControlLine">保存控制线</el-button>
          </el-form>
        </el-tab-pane>

        <!-- 考生 -->
        <el-tab-pane label="考生管理" name="stu">
          <el-form :inline="true" style="margin-bottom:10px" @submit.prevent>
            <el-form-item label="初中校">
              <el-select v-model="studentFilters.juniorSchoolId" placeholder="全部初中" clearable filterable style="width:160px">
                <el-option v-for="j in juniorSchools" :key="j.id" :label="j.name" :value="j.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="总分范围">
              <el-input-number v-model="studentFilters.minTotal" :min="0" :max="660" controls-position="right" placeholder="最低" style="width:96px" />
              <span style="margin:0 6px">~</span>
              <el-input-number v-model="studentFilters.maxTotal" :min="0" :max="660" controls-position="right" placeholder="最高" style="width:96px" />
            </el-form-item>
            <el-form-item label="校额分配">
              <el-select v-model="studentFilters.quotaEligibility" placeholder="全部" clearable style="width:120px">
                <el-option label="已获校额资格" :value="true" />
                <el-option label="无校额资格" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchStudents">查询</el-button>
              <el-button @click="resetStudentFilters">重置</el-button>
            </el-form-item>
          </el-form>
          <div style="margin-bottom:10px">
            <el-button type="success" size="small" @click="csvDialog = true">CSV 导入考生</el-button>
            <el-button type="primary" size="small" @click="generateStudents">按班数生成考生</el-button>
            <el-input-number v-model="perClass" :min="20" :max="60" size="small" style="width:120px;margin-left:8px" />
            <span class="tip" style="margin-left:8px">每校考生数 = 班数 × 班额(默认40)</span>
            <el-button type="warning" size="small" @click="recomputeEligibility">按名额重算校额资格</el-button>
            <span class="tip" style="margin-left:8px">校额资格 = 各初中校按名额总数取前 N 名（且过控制线 430）</span>
          </div>
          <el-alert v-if="eligibilityMsg" :title="eligibilityMsg" type="success" show-icon
                    :closable="false" style="margin-top:8px" />
          <el-table ref="studentTable" :data="students" border style="margin-top:10px" @sort-change="onStudentSort">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column label="初中校" :formatter="fJsStu" width="160" />
            <el-table-column prop="chinese" label="语文" width="70" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="math" label="数学" width="70" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="english" label="英语" width="70" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="physics" label="物理" width="70" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="politics" label="道法" width="70" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="pe" label="体育" width="70" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="totalScore" label="总分" width="80" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="hasQuotaEligibility" label="校额资格" width="100" sortable="custom" :sort-orders="['ascending','descending']">
              <template #default="{ row }">{{ row.hasQuotaEligibility ? '具备' : '无' }}</template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top:10px" layout="total, sizes, prev, pager, next, jumper"
            :total="studentTotal" :current-page="studentPage" :page-size="studentSize" :page-sizes="[50, 100, 200]"
            @current-change="(p) => { studentPage = p; loadStudents() }"
            @size-change="(s) => { studentSize = s; studentPage = 1; loadStudents() }" />
        </el-tab-pane>

        <!-- 模拟录取 -->
        <el-tab-pane label="模拟录取" name="sim">
          <el-form :inline="true" style="margin-bottom:10px" @submit.prevent>
            <el-form-item label="毕业学校">
              <el-select v-model="filters.juniorSchoolId" placeholder="全部初中" clearable filterable style="width:160px">
                <el-option v-for="j in juniorSchools" :key="j.id" :label="j.name" :value="j.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="录取学校">
              <el-select v-model="filters.highSchoolId" placeholder="全部高中" clearable filterable style="width:160px">
                <el-option v-for="h in highSchools" :key="h.id" :label="h.name" :value="h.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="分数范围">
              <el-input-number v-model="filters.minScore" :min="0" :max="660" controls-position="right" placeholder="最低" style="width:96px" />
              <span style="margin:0 6px">~</span>
              <el-input-number v-model="filters.maxScore" :min="0" :max="660" controls-position="right" placeholder="最高" style="width:96px" />
            </el-form-item>
            <el-form-item label="录取状态">
              <el-select v-model="filters.status" placeholder="全部" clearable style="width:110px">
                <el-option label="已录取" value="ADMITTED" />
                <el-option label="未录取" value="NOT_ADMITTED" />
              </el-select>
            </el-form-item>
            <el-form-item label="考生姓名">
              <el-input v-model="filters.studentName" placeholder="模糊匹配" clearable style="width:130px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchResults">查询</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
          <div style="margin-bottom:10px">
            <el-button type="primary" @click="runSim('full')">一键顺序模拟</el-button>
            <el-button @click="runSim('quota')">仅校额到校</el-button>
            <el-button @click="runSim('tongzhao')">仅统招</el-button>
            <el-button @click="loadResults">刷新结果</el-button>
          </div>
          <el-descriptions v-if="stats" border style="margin-bottom:10px">
            <el-descriptions-item label="考生总数">{{ stats.total }}</el-descriptions-item>
            <el-descriptions-item label="已录取">{{ stats.admitted }}</el-descriptions-item>
            <el-descriptions-item label="未录取/滑档">{{ stats.notAdmitted }}</el-descriptions-item>
            <el-descriptions-item label="校额到校录取">{{ stats.quotaAdmitted }}</el-descriptions-item>
            <el-descriptions-item label="统招录取">{{ stats.tongzhaoAdmitted }}</el-descriptions-item>
          </el-descriptions>
          <el-table ref="resultTable" :data="results" border @sort-change="onResultSort">
            <el-table-column prop="studentName" label="考生" width="100" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column label="批次" width="100" prop="batch" sortable="custom" :sort-orders="['ascending','descending']">
              <template #default="{ row }">{{ row.batch === 'QUOTA' ? '校额到校' : '统招' }}</template>
            </el-table-column>
            <el-table-column prop="highSchoolName" label="录取高中" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="juniorSchoolName" label="来源初中" width="150" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="totalScore" label="总分" width="80" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column label="校内排名" width="90" prop="schoolRank" sortable="custom" :sort-orders="['ascending','descending']">
              <template #default="{ row }">
                <span v-if="row.batch === 'QUOTA'">{{ row.schoolRank }}</span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="chinese" label="语文" width="56" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="math" label="数学" width="56" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="english" label="英语" width="56" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="physics" label="物理" width="56" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="politics" label="道法" width="56" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column prop="pe" label="体育" width="56" sortable="custom" :sort-orders="['ascending','descending']" />
            <el-table-column label="状态" width="100" prop="status" sortable="custom" :sort-orders="['ascending','descending']">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ADMITTED' ? 'success' : 'danger'">
                  {{ row.status === 'ADMITTED' ? '录取' : '未录取' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="note" label="说明" sortable="custom" :sort-orders="['ascending','descending']" />
          </el-table>
          <el-pagination style="margin-top:10px" layout="total, prev, pager, next" :total="resultTotal"
            :current-page="resultPage" :page-size="resultSize"
            @current-change="(p) => { resultPage = p; loadResults() }"
            @size-change="(s) => { resultSize = s; loadResults() }" />
        </el-tab-pane>

        <!-- 各校录取可视化 -->
        <el-tab-pane label="各校录取可视化" name="viz">
          <div style="margin-bottom:10px">
            <el-button @click="loadViz">刷新可视化</el-button>
            <span class="tip" style="margin-left:8px">基于最近一次模拟运行（GET /admission/results/summary-by-school）</span>
          </div>
          <el-row :gutter="16">
            <el-col :span="14">
              <h4 style="margin:4px 0 10px">各校录取人数（校额 + 统招，按各校录取合计相对缩放）</h4>
              <div v-for="s in viz" :key="s.highSchoolId" style="margin-bottom:12px">
                <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:2px">
                  <span>{{ s.name }}</span>
                  <span>录取 {{ s.admitted }}（校额 {{ s.quotaAdmitted }} / 统招 {{ s.tongzhaoAdmitted }}）</span>
                </div>
                <div style="background:#f0f0f0;height:18px;border-radius:4px;overflow:hidden;display:flex">
                  <div :style="{ width: (s.quotaAdmitted / vizMax * 100) + '%', background:'#409eff' }" :title="'校额 ' + s.quotaAdmitted"></div>
                  <div :style="{ width: (s.tongzhaoAdmitted / vizMax * 100) + '%', background:'#67c23a' }" :title="'统招 ' + s.tongzhaoAdmitted"></div>
                </div>
              </div>
              <div v-if="!viz.length" class="tip">暂无录取数据，请先运行模拟录取</div>
            </el-col>
            <el-col :span="10">
              <h4 style="margin:4px 0 10px">两批次占比（全部高中合计）</h4>
              <div v-if="vizTotalQuota + vizTotalTong > 0">
                <div style="background:#f0f0f0;height:24px;border-radius:4px;overflow:hidden;display:flex">
                  <div :style="{ width: (vizTotalQuota / (vizTotalQuota + vizTotalTong) * 100) + '%', background:'#409eff' }"></div>
                  <div :style="{ width: (vizTotalTong / (vizTotalQuota + vizTotalTong) * 100) + '%', background:'#67c23a' }"></div>
                </div>
                <div style="margin-top:8px;font-size:13px">
                  <span style="color:#409eff">■ 校额到校 {{ vizTotalQuota }}（{{ Math.round(vizTotalQuota / (vizTotalQuota + vizTotalTong) * 100) }}%）</span>
                  &nbsp;
                  <span style="color:#67c23a">■ 统招 {{ vizTotalTong }}（{{ Math.round(vizTotalTong / (vizTotalQuota + vizTotalTong) * 100) }}%）</span>
                </div>
              </div>
              <div v-else class="tip">暂无录取数据</div>
              <h4 style="margin:16px 0 8px">各校明细</h4>
              <el-table :data="viz" border size="small">
                <el-table-column prop="name" label="高中" />
                <el-table-column prop="tongzhaoPlan" label="统招计划" width="72" />
                <el-table-column prop="quotaPlan" label="校额计划" width="72" />
                <el-table-column prop="tongzhaoAdmitted" label="统招录" width="64" />
                <el-table-column prop="quotaAdmitted" label="校额录" width="64" />
                <el-table-column prop="admitted" label="合计" width="56" />
                <el-table-column prop="fillRate" label="满额率%" width="70" />
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- 高中弹窗 -->
    <el-dialog v-model="hsDialog" :title="hsForm.id ? '编辑高中' : '新增高中'">
      <el-form :model="hsForm" label-width="100px">
        <el-form-item label="名称"><el-input v-model="hsForm.name" /></el-form-item>
        <el-form-item label="区县"><el-input v-model="hsForm.district" /></el-form-item>
        <el-form-item label="统招计划"><el-input-number v-model="hsForm.tongzhaoQuota" :min="0" /></el-form-item>
        <el-form-item label="校额录取数"><el-input-number v-model="hsForm.quotaAdmitted" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hsDialog = false">取消</el-button>
        <el-button type="primary" @click="saveHs">保存</el-button>
      </template>
    </el-dialog>

    <!-- 初中校弹窗 -->
    <el-dialog v-model="jsDialog" :title="jsForm.id ? '编辑初中校' : '新增初中校'">
      <el-form :model="jsForm" label-width="100px">
        <el-form-item label="名称"><el-input v-model="jsForm.name" /></el-form-item>
        <el-form-item label="区县"><el-input v-model="jsForm.district" /></el-form-item>
        <el-form-item label="班数"><el-input-number v-model="jsForm.classCount" :min="0" /></el-form-item>
        <el-form-item label="毕业生数"><el-input-number v-model="jsForm.gradCount" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jsDialog = false">取消</el-button>
        <el-button type="primary" @click="saveJs">保存</el-button>
      </template>
    </el-dialog>

    <!-- 校额名额弹窗 -->
    <el-dialog v-model="qsDialog" :title="qsForm.id ? '维护校额名额' : '新增校额名额'">
      <el-form :model="qsForm" label-width="100px">
        <el-form-item label="初中校">
          <el-select v-model="qsForm.juniorSchoolId" placeholder="选择初中校">
            <el-option v-for="j in juniorSchools" :key="j.id" :label="j.name" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="高中">
          <el-select v-model="qsForm.highSchoolId" placeholder="选择高中">
            <el-option v-for="h in highSchools" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名额"><el-input-number v-model="qsForm.quota" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qsDialog = false">取消</el-button>
        <el-button type="primary" @click="saveQs">保存</el-button>
      </template>
    </el-dialog>

    <!-- 校额名额（分组初中校）维护弹窗 -->
    <el-dialog v-model="qsGroupDialog" :title="`维护校额名额（${qsGroupHighName}）`" width="520px">
      <p class="tip">该名额为共享校额池（{{ qsGroupLabel }}），请分别填写各初中校到「{{ qsGroupHighName }}」的名额：</p>
      <el-table :data="qsGroupItems" border>
        <el-table-column prop="juniorName" label="初中校" />
        <el-table-column label="名额" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.quota" :min="0" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="qsGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="saveQsGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- CSV 导入 -->
    <el-dialog v-model="csvDialog" title="CSV 导入考生">
      <p class="tip">格式：姓名,初中校ID,语文,数学,英语,物理,道法,体育,资格(true/false)</p>
      <el-input v-model="csvText" type="textarea" :rows="8" placeholder="考生A,1,90,95,88,75,70,38,true" />
      <template #footer>
        <el-button @click="csvDialog = false">取消</el-button>
        <el-button type="primary" @click="importCsv">导入</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuth } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuth()
const username = auth.username
const active = ref('hs')

const highSchools = ref([])
const juniorSchools = ref([])
const quotaSeats = ref([])
// 全量校额数据，专供「高中管理」页汇总使用，不受校额 Tab 搜索筛选影响
const allQuotaSeats = ref([])
const qsFilters = reactive({ juniorSchoolId: null, highSchoolId: null })
// 两种搜索方式互斥：选了初中校就清空高中，选了高中就清空初中校
function onPickJunior() { qsFilters.highSchoolId = null }
function onPickHigh() { qsFilters.juniorSchoolId = null }
// 当前校额列表中名额总数（含搜索筛选结果）
const qsTotalQuota = computed(() =>
  quotaSeats.value.reduce((a, q) => a + (q.quota || 0), 0)
)
const controlLine = reactive({ type: 'QUOTA', value: 430 })
const students = ref([])
const results = ref([])
const stats = ref(null)
const eligibilityMsg = ref('')

// 列表分页状态（后端返回 Spring Page<T> 信封，前端 MUST 读 content 数组）
const studentPage = ref(1)
const studentSize = ref(50)
const studentTotal = ref(0)
// 考生表排序（服务端排序）：prop=字段名，order=ascending/descending/null
const studentSort = reactive({ prop: '', order: '' })
const studentTable = ref(null)
const perClass = ref(40)
// 考生搜索条件
const studentFilters = reactive({
  juniorSchoolId: null,
  minTotal: null,
  maxTotal: null,
  quotaEligibility: null
})
const resultPage = ref(1)
const resultSize = ref(20)
const resultTotal = ref(0)
// 模拟录取结果表排序（服务端排序）：prop=字段名，order=ascending/descending/null
const resultSort = reactive({ prop: '', order: '' })
const resultTable = ref(null)

// 模拟录取结果筛选条件
const filters = reactive({
  juniorSchoolId: null,
  highSchoolId: null,
  minScore: null,
  maxScore: null,
  status: null,
  studentName: null
})

// 各校录取可视化数据（GET /admission/results/summary-by-school）
const viz = ref([])
const vizMax = ref(1)
const vizTotalQuota = ref(0)
const vizTotalTong = ref(0)

const hsDialog = ref(false)
const jsDialog = ref(false)
const qsDialog = ref(false)
// 分组初中校（如 一中/五中分校）的校额维护弹窗状态
const qsGroupDialog = ref(false)
const qsGroupLabel = ref('')
const qsGroupHighName = ref('')
const qsGroupItems = ref([])
const csvDialog = ref(false)
const csvText = ref('')

const hsForm = reactive({ id: null, name: '', district: '东城区', tongzhaoQuota: 100, quotaAdmitted: 0 })
const jsForm = reactive({ id: null, name: '', district: '东城区', classCount: 0, gradCount: 0 })
const qsForm = reactive({ id: null, juniorSchoolId: null, highSchoolId: null, quota: 1 })

const fHs = (r) => highSchools.value.find(h => h.id === r.highSchoolId)?.name || r.highSchoolId
const fJs = (r) => r.juniorSchoolNames || juniorSchools.value.find(j => j.id === r.juniorSchoolId)?.name || r.juniorSchoolId
// 某高中校额到校总名额 = 全量 quotaSeats 中 highSchoolId 命中该高中的名额合计（不随搜索筛选变化）
const quotaForHs = (hsId) => allQuotaSeats.value
  .filter(q => q.highSchoolId === hsId)
  .reduce((a, q) => a + (q.quota || 0), 0)
// 某高中校额录取数 = 高中自身维护的 quotaAdmitted（计划口径，可在编辑弹窗修改）
const quotaAdmittedForHs = (hsId) => {
  const h = highSchools.value.find(s => s.id === hsId)
  return h ? (h.quotaAdmitted || 0) : 0
}
// 比对「校额录取数（计划）」与「校额分配数（校额到校总额）」，不一致则红色标注
const mismatchHs = (hsId) => quotaForHs(hsId) !== quotaAdmittedForHs(hsId)
const quotaMismatchList = computed(() => highSchools.value.filter(h => mismatchHs(h.id)))
const hasQuotaMismatch = computed(() => quotaMismatchList.value.length > 0)
const fJsStu = (r) => juniorSchools.value.find(j => j.id === r.juniorSchoolId)?.name || r.juniorSchoolId

async function loadStudents() {
  const params = new URLSearchParams()
  params.set('page', String(studentPage.value - 1))
  params.set('size', String(studentSize.value))
  if (studentFilters.juniorSchoolId != null) params.set('juniorSchoolId', String(studentFilters.juniorSchoolId))
  if (studentFilters.minTotal != null) params.set('minTotal', String(studentFilters.minTotal))
  if (studentFilters.maxTotal != null) params.set('maxTotal', String(studentFilters.maxTotal))
  if (studentFilters.quotaEligibility != null) params.set('quotaEligibility', String(studentFilters.quotaEligibility))
  if (studentSort.prop && studentSort.order) {
    const dir = studentSort.order === 'ascending' ? 'asc' : 'desc'
    params.set('sort', `${studentSort.prop},${dir}`)
  }
  const r = await request.get(`/student/students?${params.toString()}`)
  // student-service 列表端点返回分页对象；兼容历史数组返回
  students.value = Array.isArray(r) ? r : r.content
  studentTotal.value = Array.isArray(r) ? r.length : r.totalElements
}
function searchStudents() {
  studentPage.value = 1
  loadStudents()
}
function resetStudentFilters() {
  studentFilters.juniorSchoolId = null
  studentFilters.minTotal = null
  studentFilters.maxTotal = null
  studentFilters.quotaEligibility = null
  studentSort.prop = ''
  studentSort.order = ''
  studentTable.value && studentTable.value.clearSort()
  studentPage.value = 1
  loadStudents()
}
// 考生表列排序变化时，重新请求服务端排序
function onStudentSort({ prop, order }) {
  studentSort.prop = prop || ''
  studentSort.order = order || ''
  studentPage.value = 1
  loadStudents()
}

async function loadAll() {
  const [hs, js, qs, cl] = await Promise.all([
    request.get('/school/high-schools?size=1000'),
    request.get('/school/junior-schools?size=1000'),
    request.get('/school/quota-seats?size=1000'),
    request.get('/school/control-line')
  ])
  // school-service 列表接口返回数组（非分页），做兼容处理
  highSchools.value = Array.isArray(hs) ? hs : hs.content
  juniorSchools.value = Array.isArray(js) ? js : js.content
  const allQs = Array.isArray(qs) ? qs : qs.content
  allQuotaSeats.value = allQs
  quotaSeats.value = allQs
  if (cl && cl.value !== undefined) controlLine.value = cl.value
  await loadStudents()
  await loadViz()
}

async function loadQuotaSeats() {
  const params = new URLSearchParams()
  if (qsFilters.juniorSchoolId != null) params.append('juniorSchoolId', qsFilters.juniorSchoolId)
  if (qsFilters.highSchoolId != null) params.append('highSchoolId', qsFilters.highSchoolId)
  const qs = await request.get('/school/quota-seats?size=1000' + (params.toString() ? '&' + params.toString() : ''))
  quotaSeats.value = Array.isArray(qs) ? qs : qs.content
}

function searchQuotaSeats() {
  loadQuotaSeats()
}

function resetQsFilters() {
  qsFilters.juniorSchoolId = null
  qsFilters.highSchoolId = null
  loadQuotaSeats()
}
onMounted(loadAll)

async function loadViz() {
  try {
    const data = await request.get('/admission/results/summary-by-school')
    viz.value = data || []
    vizMax.value = Math.max(1, ...viz.value.map(s => s.admitted || 0))
    vizTotalQuota.value = viz.value.reduce((a, s) => a + (s.quotaAdmitted || 0), 0)
    vizTotalTong.value = viz.value.reduce((a, s) => a + (s.tongzhaoAdmitted || 0), 0)
  } catch (e) {
    viz.value = []
  }
}

function openHsDialog(row) {
  if (row) Object.assign(hsForm, row)
  else Object.assign(hsForm, { id: null, name: '', district: '东城区', tongzhaoQuota: 100 })
  hsDialog.value = true
}
async function saveHs() {
  if (hsForm.id != null) {
    await request.put('/school/high-schools/' + hsForm.id, hsForm)
  } else {
    await request.post('/school/high-schools', hsForm)
  }
  hsDialog.value = false
  ElMessage.success('已保存')
  loadAll()
}
async function delHs(row) {
  await ElMessageBox.confirm('确认删除？')
  await request.delete('/school/high-schools/' + row.id)
  loadAll()
}

function openJsDialog(row) {
  if (row) Object.assign(jsForm, row)
  else Object.assign(jsForm, { id: null, name: '', district: '东城区', classCount: 0, gradCount: 0 })
  jsDialog.value = true
}
async function saveJs() {
  await request.post('/school/junior-schools', jsForm)
  jsDialog.value = false
  ElMessage.success('已保存')
  loadAll()
}
async function delJs(row) {
  await ElMessageBox.confirm('确认删除？')
  await request.delete('/school/junior-schools/' + row.id)
  loadAll()
}

function openQsDialog(row) {
  // 分组合并行（juniorSchoolNames 非空、无真实 id）→ 打开分组维护弹窗
  if (row && row.juniorSchoolNames) {
    openQsGroupDialog(row)
    return
  }
  if (row) {
    Object.assign(qsForm, {
      id: row.id,
      juniorSchoolId: row.juniorSchoolId,
      highSchoolId: row.highSchoolId,
      quota: row.quota
    })
  } else {
    Object.assign(qsForm, { id: null, juniorSchoolId: null, highSchoolId: null, quota: 1 })
  }
  qsDialog.value = true
}
// 打开分组维护弹窗：从全量明细中拆出各成员到该高中的名额，逐条可编辑
function openQsGroupDialog(row) {
  const names = (row.juniorSchoolNames || '').split('/').map(s => s.trim()).filter(Boolean)
  const hsId = row.highSchoolId
  qsGroupItems.value = names.map(name => {
    const js = juniorSchools.value.find(j => j.name === name)
    const jsId = js ? js.id : null
    const existing = jsId != null
      ? allQuotaSeats.value.find(q => q.juniorSchoolId === jsId && q.highSchoolId === hsId)
      : null
    return {
      juniorSchoolId: jsId,
      juniorName: name,
      highSchoolId: hsId,
      id: existing ? existing.id : null,
      quota: existing ? existing.quota : 0
    }
  })
  qsGroupLabel.value = row.juniorSchoolNames || ''
  qsGroupHighName.value = fHs({ highSchoolId: hsId })
  qsGroupDialog.value = true
}
async function saveQsGroup() {
  for (const it of qsGroupItems.value) {
    if (it.juniorSchoolId == null) {
      ElMessage.warning(`未找到初中校「${it.juniorName}」，已跳过`)
      continue
    }
    const payload = { juniorSchoolId: it.juniorSchoolId, highSchoolId: it.highSchoolId, quota: it.quota }
    if (it.id != null && it.quota > 0) {
      await request.put('/school/quota-seats/' + it.id, payload)
    } else if (it.id != null && it.quota === 0) {
      await request.delete('/school/quota-seats/' + it.id)
    } else if (it.id == null && it.quota > 0) {
      await request.post('/school/quota-seats', payload)
    }
  }
  qsGroupDialog.value = false
  ElMessage.success('已保存')
  await refreshQuotaView()
}
// 保存后刷新全量数据，并保留当前校额页的筛选视图（分组行会重新合并）
async function refreshQuotaView() {
  await loadAll()
  if (qsFilters.juniorSchoolId != null || qsFilters.highSchoolId != null) await loadQuotaSeats()
}
async function saveQs() {
  if (qsForm.id != null) {
    await request.put('/school/quota-seats/' + qsForm.id, qsForm)
  } else {
    await request.post('/school/quota-seats', qsForm)
  }
  qsDialog.value = false
  ElMessage.success('已保存')
  await refreshQuotaView()
}
async function delQs(row) {
  await request.delete('/school/quota-seats/' + row.id)
  loadAll()
}

async function saveControlLine() {
  await request.post('/school/control-line', { type: 'QUOTA', value: controlLine.value })
  ElMessage.success('已保存')
}

async function recomputeEligibility() {
  const r = await request.post('/student/quota-eligibility/recompute')
  eligibilityMsg.value = `已重算：具校额资格 ${r.eligibleTotal} / 考生 ${r.studentTotal} 人（按各初中校名额总数取前 N 名）`
  ElMessage.success('校额资格已按名额重算')
  await loadAll()
}

async function generateStudents() {
  await ElMessageBox.confirm(
    `将按「班数 × 班额(${perClass.value})」清空并重新生成全部考生（同时清空志愿），确定？`,
    '按班数生成考生', { type: 'warning' }
  )
  const r = await request.post('/student/generate?perClass=' + perClass.value)
  ElMessage.success(`已生成 ${r.generated} 名考生（班额 ${r.perClass}）`)
  await loadAll()
}

async function importCsv() {
  await request.post('/student/import/csv', csvText.value, { headers: { 'Content-Type': 'text/plain' } })
  csvDialog.value = false
  ElMessage.success('导入完成')
  loadAll()
}

async function runSim(kind) {
  const url = kind === 'full' ? '/admission/run/full' : kind === 'quota' ? '/admission/run/quota' : '/admission/run/tongzhao'
  stats.value = await request.post(url)
  resultPage.value = 1
  await loadResults()
  await loadViz()
  ElMessage.success('模拟完成')
}
async function loadResults() {
  const params = new URLSearchParams()
  params.set('page', String(resultPage.value - 1))
  params.set('size', String(resultSize.value))
  if (filters.juniorSchoolId != null) params.set('juniorSchoolId', String(filters.juniorSchoolId))
  if (filters.highSchoolId != null) params.set('highSchoolId', String(filters.highSchoolId))
  if (filters.minScore != null) params.set('minScore', String(filters.minScore))
  if (filters.maxScore != null) params.set('maxScore', String(filters.maxScore))
  if (filters.status) params.set('status', filters.status)
  if (filters.studentName && filters.studentName.trim()) params.set('studentName', filters.studentName.trim())
  if (resultSort.prop && resultSort.order) {
    const dir = resultSort.order === 'ascending' ? 'asc' : 'desc'
    params.set('sort', `${resultSort.prop},${dir}`)
  }
  const r = await request.get(`/admission/results?${params.toString()}`)
  results.value = r.content
  resultTotal.value = r.totalElements
  stats.value = await request.get('/admission/stats')
}
// 模拟录取结果表列排序变化时，重新请求服务端排序
function onResultSort({ prop, order }) {
  resultSort.prop = prop || ''
  resultSort.order = order || ''
  resultPage.value = 1
  loadResults()
}
function searchResults() {
  resultPage.value = 1
  loadResults()
}
function resetFilters() {
  filters.juniorSchoolId = null
  filters.highSchoolId = null
  filters.minScore = null
  filters.maxScore = null
  filters.status = null
  filters.studentName = null
  resultSort.prop = ''
  resultSort.order = ''
  if (resultTable.value) resultTable.value.clearSort()
  resultPage.value = 1
  loadResults()
}

function logout() {
  auth.logout()
  router.push('/login')
}
function goApplications() {
  router.push('/applications')
}
</script>

<style scoped>
.tip { color: #999; font-size: 12px; margin-bottom: 8px; }
</style>

<template>
  <div class="teacher-course-manage-container" v-loading="pageLoading">
    <el-card>
      <template #header>
        <div class="course-header">
          <div>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>课程管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ courseInfo.courseName || '加载中...' }}</el-breadcrumb-item>
            </el-breadcrumb>
            <h2 class="course-title">{{ courseInfo.courseName }}</h2>
            <div class="course-meta">
              <span>课程代码：{{ courseInfo.courseCode }}</span>
              <span>学分：{{ courseInfo.credits }}</span>
              <span>授课教师：{{ courseInfo.teacherName }}</span>
              <span>学年：{{ courseInfo.academicYear }} {{ courseInfo.semester }}</span>
            </div>
          </div>
          <el-tag type="primary" size="large">
            {{ courseInfo.academicYear || '' }}
          </el-tag>
        </div>
      </template>

      <div class="summary-grid">
        <div class="summary-item">
          <p class="summary-label">学生人数</p>
          <p class="summary-value">{{ studentCount }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">课程目标数</p>
          <p class="summary-value">{{ objectiveCount }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">考核环节数</p>
          <p class="summary-value">{{ assessmentCount }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">资源数</p>
          <p class="summary-value">{{ resourceCount }}</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ========== 课程目标 Tab ========== -->
        <el-tab-pane label="课程目标" name="objectives">
          <div class="toolbar">
            <el-button type="primary" :icon="Plus" @click="openObjectiveDialog()">新增课程目标</el-button>
          </div>
          <el-table :data="objectives" border style="width: 100%" v-loading="objectivesLoading">
            <el-table-column prop="code" label="编号" width="100" />
            <el-table-column prop="description" label="课程目标描述" min-width="300" />
            <el-table-column prop="weight" label="权重" width="100" />
            <el-table-column label="操作" width="160">
              <template #default="scope">
                <el-button link type="primary" @click="openObjectiveDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteObjective(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 课程目标-毕业要求指标点支撑矩阵 -->
          <div class="matrix-section" v-if="objectives.length > 0">
            <div class="tab-header">
              <div class="header-left">
                <span class="tab-title">课程目标-毕业要求指标点支撑矩阵</span>
                <el-tag v-if="matrixUnsaved" type="warning" size="small" effect="plain">未保存</el-tag>
              </div>
              <el-button type="primary" size="small" @click="saveMatrix" :loading="matrixSaving">
                {{ matrixUnsaved ? '保存矩阵 *' : '保存矩阵' }}
              </el-button>
            </div>

            <el-alert v-if="matrixIndicators.length === 0" type="info" :closable="false" show-icon style="margin-bottom:12px">
              该专业暂无毕业要求指标点数据，请先联系管理员在培养方案中配置指标点
            </el-alert>

            <el-table v-if="matrixIndicators.length > 0" :data="matrixRows" border style="width: 100%" max-height="500" v-loading="matrixLoading">
              <el-table-column prop="code" label="课程目标" width="160" fixed>
                <template #default="scope">
                  <span class="obj-label">{{ scope.row.code }}</span>
                </template>
              </el-table-column>
              <el-table-column
                  v-for="ind in matrixIndicators" :key="ind.indicatorId"
                  :label="ind.indicatorCode" width="100" align="center"
              >
                <template #header>
                  <div class="indicator-header">
                    <el-tooltip :content="ind.indicatorDesc" placement="top" effect="dark">
                      <span class="indicator-code">{{ ind.indicatorCode }}</span>
                    </el-tooltip>
                    <span class="indicator-req">{{ ind.requirementCode }}</span>
                  </div>
                </template>
                <template #default="scope">
                  <el-select
                      v-model="scope.row.supportMap[ind.indicatorId]"
                      placeholder="-" size="small" style="width:70px"
                      @change="onMatrixChange"
                      :class="getSelectClass(scope.row.supportMap[ind.indicatorId])"
                  >
                    <el-option value="H"><span class="so so-h">H</span></el-option>
                    <el-option value="M"><span class="so so-m">M</span></el-option>
                    <el-option value="L"><span class="so so-l">L</span></el-option>
                    <el-option value=""><span class="so so-none">-</span></el-option>
                  </el-select>
                </template>
              </el-table-column>
            </el-table>

            <div class="matrix-legend" v-if="matrixIndicators.length > 0">
              <span class="legend-title">支撑强度：</span>
              <span class="legend-item sh">■ H 强支撑</span>
              <span class="legend-item sm">■ M 中支撑</span>
              <span class="legend-item sl">■ L 弱支撑</span>
              <span class="legend-item sn">■ - 无支撑</span>
            </div>
          </div>
        </el-tab-pane>

        <!-- ========== 教学资源 Tab ========== -->
        <el-tab-pane label="教学资源" name="resources">
          <div class="toolbar">
            <el-button type="primary" :icon="Upload" @click="openResourceUpload">上传资源</el-button>
          </div>
          <el-table :data="resources" border style="width: 100%" v-loading="resourcesLoading">
            <el-table-column prop="fileName" label="文件名称" min-width="200" />
            <el-table-column prop="resourceType" label="类型" width="120">
              <template #default="scope">
                <el-tag size="small" :type="getResourceTypeTag(scope.row.resourceType)">
                  {{ scope.row.resourceType || '其他' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" label="上传时间" width="170" />
            <el-table-column label="操作" width="180">
              <template #default="scope">
                <el-button link type="primary" @click="downloadResource(scope.row)">下载</el-button>
                <el-button link type="danger" @click="deleteResource(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 学生名单 Tab ========== -->
        <el-tab-pane label="学生名单" name="students">
          <StudentImport
              ref="studentImportRef"
              :offering-id="offeringId"
              @refresh="handleStudentRefresh"
              @student-count-change="handleStudentCountChange"
          />
        </el-tab-pane>

        <!-- ========== 成绩录入 Tab ========== -->
        <el-tab-pane label="成绩录入" name="grades">
          <div class="toolbar">
            <div>
              <el-button type="primary" :icon="Upload" @click="openGradeImport">批量导入</el-button>
              <el-button :icon="Download" @click="exportStudentGrades">导出成绩</el-button>
              <el-button link type="warning" @click="openWeightDialog">配置权重</el-button>
            </div>
            <div class="weight-info" v-if="gradeWeights">
              <span class="weight-tag">平时 {{ pct(gradeWeights.dailyWeight) }}%</span>
              <span class="weight-tag">实验报告 {{ pct(gradeWeights.reportWeight) }}%</span>
              <span class="weight-tag">期中 {{ pct(gradeWeights.midtermWeight) }}%</span>
              <span class="weight-tag">期末 {{ pct(gradeWeights.finalWeight) }}%</span>
            </div>
            <el-button type="success" :icon="Check" @click="submitAllGrades" :loading="submittingGrades">
              提交全部成绩
            </el-button>
          </div>
          <div class="grade-table-wrap" ref="gradeTableWrapRef">
            <el-table :data="gradeEntryData" border style="width: 100%" v-loading="gradesLoading"
                      :table-layout="'auto'">
              <!-- 固定列：学号、姓名 -->
              <el-table-column prop="studentNo" label="学号" width="110" />
              <el-table-column prop="studentName" label="姓名" width="90" />

              <!-- 多级表头：成绩明细 -->
              <el-table-column label="成绩明细" align="center">
                <el-table-column label="平时成绩" :width="flexColWidth" align="center">
                  <template #default="scope">
                    <template v-if="scope.row.editing">
                      <el-input-number v-model="scope.row.dailyScore" :min="0" :max="100"
                                       :precision="1" size="small" controls-position="right" style="width:100%" />
                    </template>
                    <template v-else>
                      <span class="score-cell">{{ scope.row.dailyScore != null ? scope.row.dailyScore : '-' }}</span>
                    </template>
                  </template>
                </el-table-column>
                <el-table-column label="实验报告" :width="flexColWidth" align="center">
                  <template #default="scope">
                    <template v-if="scope.row.editing">
                      <el-input-number v-model="scope.row.reportScore" :min="0" :max="100"
                                       :precision="1" size="small" controls-position="right" style="width:100%" />
                    </template>
                    <template v-else>
                      <span class="score-cell">{{ scope.row.reportScore != null ? scope.row.reportScore : '-' }}</span>
                    </template>
                  </template>
                </el-table-column>
                <el-table-column label="期中考试" :width="flexColWidth" align="center">
                  <template #default="scope">
                    <template v-if="scope.row.editing">
                      <el-input-number v-model="scope.row.midtermScore" :min="0" :max="100"
                                       :precision="1" size="small" controls-position="right" style="width:100%" />
                    </template>
                    <template v-else>
                      <span class="score-cell">{{ scope.row.midtermScore != null ? scope.row.midtermScore : '-' }}</span>
                    </template>
                  </template>
                </el-table-column>
                <el-table-column label="期末考试" :width="flexColWidth" align="center">
                  <template #default="scope">
                    <template v-if="scope.row.editing">
                      <el-input-number v-model="scope.row.finalScore" :min="0" :max="100"
                                       :precision="1" size="small" controls-position="right" style="width:100%" />
                    </template>
                    <template v-else>
                      <span class="score-cell">{{ scope.row.finalScore != null ? scope.row.finalScore : '-' }}</span>
                    </template>
                  </template>
                </el-table-column>
              </el-table-column>

              <!-- 最终成绩 -->
              <el-table-column label="最终成绩" :width="flexColWidth" align="center">
                <template #default="scope">
                  <span v-if="scope.row.computedTotal != null" class="score-cell total-score">
                    {{ scope.row.computedTotal }}
                  </span>
                  <span v-else class="score-cell empty">-</span>
                </template>
              </el-table-column>

              <!-- 操作列 -->
              <el-table-column label="操作" width="100" align="center">
                <template #default="scope">
                  <el-button v-if="scope.row.editing" link type="primary" @click="finishEditRow(scope.$index)">
                    完成
                  </el-button>
                  <el-button v-else link type="primary" @click="startEditRow(scope.$index)">
                    编辑
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 课程目标弹窗 -->
    <el-dialog v-model="objectiveDialogVisible" :title="objectiveDialogTitle" width="500px">
      <el-form ref="objFormRef" :model="objForm" label-width="100px">
        <el-form-item label="编号" required>
          <el-input v-model="objForm.code" placeholder="如 OBJ1" />
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="objForm.description" type="textarea" :rows="3" placeholder="课程目标描述" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="objForm.weight" :min="0" :max="1" :step="0.05" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="objectiveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitObjective" :loading="objSubmitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 资源上传弹窗 -->
    <el-dialog v-model="resourceDialogVisible" title="上传教学资源" width="480px">
      <el-form ref="resourceFormRef" :model="resourceForm" label-width="90px">
        <el-form-item label="资源类型" required>
          <el-select
              v-model="resourceForm.resourceType"
              placeholder="请选择资源类型"
              style="width:100%"
          >
            <el-option label="课件" value="课件" />
            <el-option label="教案" value="教案" />
            <el-option label="参考资料" value="参考资料" />
            <el-option label="习题" value="习题" />
            <el-option label="实验指导" value="实验指导" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
              ref="uploadRef"
              drag
              :action="`/api/teacher/resources/upload/${offeringId}`"
              :headers="uploadHeaders"
              :data="uploadData"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              accept=".pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx"
          >
            <el-icon><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF/PPT/DOC/XLS 格式，最大 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpload" :loading="uploading">
          确认上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导入成绩弹窗 -->
    <el-dialog v-model="importDialogVisible" title="批量导入成绩" width="450px" @close="handleImportDialogClose">
      <el-upload
          ref="gradeUploadRef"
          drag
          :action="`/api/teacher/student-grades/import/${offeringId}`"
          :headers="uploadHeaders"
          :on-success="handleGradeImportSuccess"
          :on-error="handleGradeImportError"
          accept=".xlsx,.xls"
      >
        <el-icon><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            Excel格式：第1行表头，列顺序为 学号、平时成绩、实验报告、期中考试、期末考试
          </div>
        </template>
      </el-upload>
    </el-dialog>

    <!-- 权重配置弹窗 -->
    <el-dialog v-model="weightDialogVisible" title="成绩权重配置" width="480px">
      <el-form ref="weightFormRef" :model="weightForm" label-width="110px">
        <el-form-item label="平时成绩权重">
          <el-input-number v-model="weightForm.dailyWeight" :min="0" :max="100" :step="5" :precision="0" size="default" style="width:200px" />
          <span style="margin-left:8px;color:#909399">%</span>
        </el-form-item>
        <el-form-item label="实验报告权重">
          <el-input-number v-model="weightForm.reportWeight" :min="0" :max="100" :step="5" :precision="0" size="default" style="width:200px" />
          <span style="margin-left:8px;color:#909399">%</span>
        </el-form-item>
        <el-form-item label="期中考试权重">
          <el-input-number v-model="weightForm.midtermWeight" :min="0" :max="100" :step="5" :precision="0" size="default" style="width:200px" />
          <span style="margin-left:8px;color:#909399">%</span>
        </el-form-item>
        <el-form-item label="期末考试权重">
          <el-input-number v-model="weightForm.finalWeight" :min="0" :max="100" :step="5" :precision="0" size="default" style="width:200px" />
          <span style="margin-left:8px;color:#909399">%</span>
        </el-form-item>
        <el-form-item>
          <span :style="{ color: weightSum === 100 ? '#67c23a' : '#f56c6c', fontSize: '14px' }">
            合计：{{ weightSum }}%（{{ weightSum === 100 ? '✓ 有效' : '✗ 必须等于100%' }}）
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="weightDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWeights" :loading="weightSubmitting" :disabled="weightSum !== 100">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Upload, UploadFilled, Check } from '@element-plus/icons-vue'
import request from '@/api/request'
import StudentImport from '@/views/teacher/components/StudentImport.vue'

const route = useRoute()
const offeringId = computed(() => route.params.id)

// ============ 状态 ============
const activeTab = ref('objectives')
const pageLoading = ref(false)
const studentKeyword = ref('')

// ============ 课程信息 ============
const courseInfo = ref({
  courseName: '',
  courseCode: '',
  credits: '',
  teacherName: '',
  academicYear: '',
  semester: '',
  programId: null,  // 添加 programId 字段
  courseId: null    // 添加 courseId 字段
})

// ============ 学生名单 ============
const studentsLoading = ref(false)
const rawStudents = ref([])

const studentCount = computed(() => rawStudents.value.length)
const filteredStudents = computed(() => {
  const kw = studentKeyword.value.trim().toLowerCase()
  if (!kw) return rawStudents.value
  return rawStudents.value.filter(s =>
      String(s.studentNo).includes(kw) || (s.studentName && s.studentName.toLowerCase().includes(kw))
  )
})

// ============ 成绩录入 ============
const gradesLoading = ref(false)
const gradeEntryData = ref([])
const submittingGrades = ref(false)
const importDialogVisible = ref(false)
const gradeUploadRef = ref(null)
const gradeTableWrapRef = ref(null)
const flexColWidth = ref(130)

// 权重配置
const gradeWeights = ref(null)
const weightDialogVisible = ref(false)
const weightFormRef = ref(null)
const weightSubmitting = ref(false)
const weightForm = ref({ dailyWeight: 25, reportWeight: 25, midtermWeight: 25, finalWeight: 25 })

const weightSum = computed(() =>
    weightForm.value.dailyWeight + weightForm.value.reportWeight +
    weightForm.value.midtermWeight + weightForm.value.finalWeight
)

const hasEdits = computed(() => gradeEntryData.value.some(row => row.editing))

function computeRowTotal(row) {
  if (!gradeWeights.value) return null
  let sum = 0, wSum = 0
  const w = gradeWeights.value
  if (row.dailyScore != null)  { sum += row.dailyScore * w.dailyWeight;  wSum += w.dailyWeight }
  if (row.reportScore != null)  { sum += row.reportScore * w.reportWeight;  wSum += w.reportWeight }
  if (row.midtermScore != null) { sum += row.midtermScore * w.midtermWeight; wSum += w.midtermWeight }
  if (row.finalScore != null)   { sum += row.finalScore * w.finalWeight;   wSum += w.finalWeight }
  return wSum > 0 ? (sum / wSum).toFixed(1) : null
}

function pct(val) { return val != null ? (val * 100).toFixed(0) : '0' }

function recalcColumnWidth() {
  nextTick(() => {
    const wrap = gradeTableWrapRef.value
    if (wrap) {
      const total = wrap.clientWidth
      const fixed = 110 + 90 + 100
      const w = Math.floor((total - fixed) / 5)
      flexColWidth.value = Math.max(w, 80)
    }
  })
}

let resizeObserver = null

// ============ 考核环节（仅用于计数） ============
const assessments = ref([])
const assessmentCount = computed(() => assessments.value.length)

// ============ 课程目标 ============
const objectives = ref([])
const objectivesLoading = ref(false)
const objectiveDialogVisible = ref(false)
const objFormRef = ref(null)
const objSubmitting = ref(false)
const editingObjectiveId = ref(null)
const objForm = ref({ code: '', description: '', weight: 0.5 })

const objectiveCount = computed(() => objectives.value.length)
const objectiveDialogTitle = computed(() => editingObjectiveId.value ? '编辑课程目标' : '新增课程目标')

// ============ 课程目标-指标点矩阵 ============
const matrixLoading = ref(false)
const matrixSaving = ref(false)
const matrixUnsaved = ref(false)
const matrixIndicators = ref([])
const matrixRows = ref([])

const getSelectClass = (val) => {
  const m = { H: 'ss-h', M: 'ss-m', L: 'ss-l', '': 'ss-n' }
  return m[val] || ''
}

const onMatrixChange = () => { matrixUnsaved.value = true }

const fetchIndicatorsByProgram = async (programId) => {
  try {
    const res = await request.get(`/admin/program/requirements/detail-list/${programId}`)
    const details = res.data || []

    const allIndicators = []
    for (const detail of details) {
      // 注意字段名是 indicatorPoints
      const inds = (detail.indicatorPoints || []).map(ind => ({
        indicatorId: ind.id,
        indicatorCode: ind.code,
        indicatorDesc: ind.description,
        requirementCode: detail.code,
        requirementId: detail.id
      }))
      allIndicators.push(...inds)
    }

    console.log('获取到的指标点数量:', allIndicators.length)
    return allIndicators
  } catch (error) {
    console.error('获取指标点失败:', error)
    return []
  }
}

// ============ 【核心修复】加载矩阵数据 ============
const fetchMatrixData = async () => {
  if (objectives.value.length === 0) {
    matrixRows.value = []
    matrixIndicators.value = []
    return
  }

  // 通过 courseInfo 获取 programId
  const programId = courseInfo.value?.programId
  console.log('当前课程的 programId:', programId)

  if (!programId) {
    console.warn('课程未关联培养方案，跳过加载指标点')
    matrixIndicators.value = []
    matrixRows.value = []
    return
  }

  matrixLoading.value = true
  try {
    // 【核心修复】使用与管理员端相同的方式获取指标点
    const indicators = await fetchIndicatorsByProgram(programId)

    if (indicators.length === 0) {
      matrixIndicators.value = []
      matrixRows.value = []
      ElMessage.info('该专业暂无毕业要求指标点数据，请先联系管理员在培养方案中配置指标点')
      return
    }

    matrixIndicators.value = indicators

    // 获取已保存的矩阵数据
    let savedItems = []
    try {
      const matRes = await request.get(`/teacher/objectives/matrix/${offeringId.value}`)
      if (matRes.status === 'success' && matRes.data) {
        savedItems = Array.isArray(matRes.data) ? matRes.data : []
      }
    } catch (e) {
      console.warn('获取已保存矩阵数据失败:', e)
      savedItems = []
    }

    // 构建矩阵数据
    const itemMap = {}
    for (const it of savedItems) {
      if (!itemMap[it.objectiveId]) itemMap[it.objectiveId] = {}
      itemMap[it.objectiveId][it.indicatorId] = it.supportLevel
    }

    matrixRows.value = objectives.value.map(obj => {
      const row = {
        objectiveId: obj.id,
        code: obj.code,
        supportMap: {}
      }
      for (const ind of matrixIndicators.value) {
        const existing = (itemMap[obj.id] && itemMap[obj.id][ind.indicatorId]) || ''
        row.supportMap[ind.indicatorId] = existing
      }
      return row
    })
    matrixUnsaved.value = false

    console.log('矩阵数据加载完成，行数:', matrixRows.value.length)

  } catch (e) {
    console.error('加载矩阵数据异常:', e)
    matrixIndicators.value = []
    matrixRows.value = []
  } finally {
    matrixLoading.value = false
  }
}

// ============ 保存矩阵 ============
const saveMatrix = async () => {
  if (matrixRows.value.length === 0) {
    ElMessage.warning('没有可保存的矩阵数据')
    return
  }

  matrixSaving.value = true
  try {
    const items = []
    for (const row of matrixRows.value) {
      for (const ind of matrixIndicators.value) {
        const val = row.supportMap[ind.indicatorId]
        if (val && ['H', 'M', 'L'].includes(val)) {
          items.push({
            objectiveId: row.objectiveId,
            indicatorId: ind.indicatorId,
            supportLevel: val
          })
        }
      }
    }

    await request.post(`/teacher/objectives/matrix/${offeringId.value}`, items)
    ElMessage.success('矩阵保存成功')
    matrixUnsaved.value = false
  } catch (e) {
    ElMessage.error(e.message || '矩阵保存失败')
  } finally {
    matrixSaving.value = false
  }
}

// ============ 资源 ============
const resources = ref([])
const resourcesLoading = ref(false)
const resourceDialogVisible = ref(false)
const uploadRef = ref(null)
const uploading = ref(false)
const resourceFormRef = ref(null)

const resourceForm = ref({
  resourceType: ''
})

const uploadData = computed(() => ({
  resourceType: resourceForm.value.resourceType
}))

const resourceCount = computed(() => resources.value.length)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

// 资源类型标签颜色映射
const getResourceTypeTag = (type) => {
  const map = {
    '课件': 'primary',
    '教案': 'success',
    '参考资料': 'warning',
    '习题': 'danger',
    '实验指导': 'info',
    '其他': 'info'
  }
  return map[type] || 'info'
}

// 上传前校验
const beforeUpload = (file) => {
  if (!resourceForm.value.resourceType) {
    ElMessage.warning('请先选择资源类型')
    return false
  }
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

// 确认上传
const confirmUpload = () => {
  if (!resourceForm.value.resourceType) {
    ElMessage.warning('请选择资源类型')
    return
  }
  const uploadComponent = uploadRef.value
  if (uploadComponent) {
    const files = uploadComponent.uploadFiles
    if (files.length === 0) {
      ElMessage.warning('请选择要上传的文件')
      return
    }
    uploading.value = true
    uploadComponent.submit()
  }
}

const handleUploadSuccess = (response) => {
  uploading.value = false
  if (response.status === 'success') {
    ElMessage.success('上传成功')
    resourceDialogVisible.value = false
    resourceForm.value.resourceType = ''
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }
    fetchResources()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = () => {
  uploading.value = false
  ElMessage.error('上传失败，请检查文件格式或网络')
}

const openResourceUpload = () => {
  resourceForm.value.resourceType = ''
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  resourceDialogVisible.value = true
}

// ============ 加载课程信息 ============
const fetchCourseInfo = async () => {
  pageLoading.value = true
  try {
    const res = await request.get(`/teacher/offering/detail/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      courseInfo.value = res.data
      console.log('课程信息加载完成:', {
        programId: res.data.programId,
        courseId: res.data.courseId,
        offeringId: offeringId.value
      })
    }
  } catch (e) {
    console.error('加载课程信息失败:', e)
  } finally {
    pageLoading.value = false
  }
}

// ============ 加载学生名单（用于统计人数） ============
const fetchStudents = async () => {
  studentsLoading.value = true
  try {
    const res = await request.get(`/teacher/student-course/list/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      if (Array.isArray(res.data)) {
        rawStudents.value = res.data
      } else {
        const list = res.data.list || res.data.records || []
        rawStudents.value = Array.isArray(list) ? list : []
      }
    }
  } catch (e) {
    rawStudents.value = []
  } finally {
    studentsLoading.value = false
  }
}

// ============ 成绩录入 ============
const fetchStudentGrades = async () => {
  gradesLoading.value = true
  try {
    const res = await request.get(`/teacher/student-grades/offering/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      gradeEntryData.value = res.data.map(item => ({
        studentId: item.studentId,
        studentNo: item.studentNo,
        studentName: item.studentName,
        dailyScore: item.dailyScore,
        reportScore: item.reportScore,
        midtermScore: item.midtermScore,
        finalScore: item.finalScore,
        editing: false,
        computedTotal: null
      }))
      recalcAllTotals()
    }
  } catch (e) {
    gradeEntryData.value = []
  } finally {
    gradesLoading.value = false
  }
}

function recalcAllTotals() {
  gradeEntryData.value.forEach(row => {
    row.computedTotal = computeRowTotal(row)
  })
}

// ============ 权重配置 ============
const fetchWeights = async () => {
  try {
    const res = await request.get(`/teacher/student-grades/weights/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      gradeWeights.value = res.data
    }
  } catch (e) {
    gradeWeights.value = {dailyWeight: 0.25, reportWeight: 0.25, midtermWeight: 0.25, finalWeight: 0.25}
  }
}

const openWeightDialog = () => {
  if (gradeWeights.value) {
    weightForm.value = {
      dailyWeight: (gradeWeights.value.dailyWeight * 100),
      reportWeight: (gradeWeights.value.reportWeight * 100),
      midtermWeight: (gradeWeights.value.midtermWeight * 100),
      finalWeight: (gradeWeights.value.finalWeight * 100)
    }
  }
  weightDialogVisible.value = true
}

const submitWeights = async () => {
  weightSubmitting.value = true
  try {
    await request.post('/teacher/student-grades/weights', {
      offeringId: Number(offeringId.value),
      dailyWeight: weightForm.value.dailyWeight / 100,
      reportWeight: weightForm.value.reportWeight / 100,
      midtermWeight: weightForm.value.midtermWeight / 100,
      finalWeight: weightForm.value.finalWeight / 100
    })
    ElMessage.success('权重配置已保存')
    weightDialogVisible.value = false
    await fetchWeights()
    recalcAllTotals()
  } catch (e) {
    // 拦截器已处理
  } finally {
    weightSubmitting.value = false
  }
}

const startEditRow = (index) => {
  gradeEntryData.value[index].editing = true
}

const finishEditRow = async (index) => {
  const row = gradeEntryData.value[index]
  row.computedTotal = computeRowTotal(row)
  row.editing = false
  await saveSingleRow(row)
}

const saveSingleRow = async (row) => {
  try {
    await request.post('/teacher/student-grades/batch', {
      offeringId: Number(offeringId.value),
      grades: [{
        studentId: row.studentId,
        dailyScore: row.dailyScore,
        reportScore: row.reportScore,
        midtermScore: row.midtermScore,
        finalScore: row.finalScore
      }]
    })
  } catch (e) {
    ElMessage.error('成绩保存失败')
  }
}

const submitAllGrades = async () => {
  submittingGrades.value = true
  try {
    const grades = gradeEntryData.value.map(row => ({
      studentId: row.studentId,
      dailyScore: row.dailyScore,
      reportScore: row.reportScore,
      midtermScore: row.midtermScore,
      finalScore: row.finalScore
    }))

    await request.post('/teacher/student-grades/batch', {
      offeringId: Number(offeringId.value),
      grades
    })
    ElMessage.success('全部成绩已保存')
    await fetchStudentGrades()
  } catch (e) {
    // 拦截器已处理
  } finally {
    submittingGrades.value = false
  }
}

const openGradeImport = () => {
  importDialogVisible.value = true
}

const handleImportDialogClose = () => {
  // 清理上传组件
}

const handleGradeImportSuccess = (response) => {
  if (response.status === 'success') {
    ElMessage.success(response.info || '导入成功')
    importDialogVisible.value = false
    fetchWeights().then(() => fetchStudentGrades())
    fetchStudents()
  } else {
    ElMessage.error(response.info || '导入失败')
  }
}

const handleGradeImportError = () => {
  ElMessage.error('导入失败，请检查文件格式')
}

const exportStudentGrades = () => {
  const token = localStorage.getItem('token')
  const url = `/api/teacher/student-grades/export/${offeringId.value}`
  fetch(url, {headers: {Authorization: `Bearer ${token}`}})
      .then(res => {
        if (!res.ok) throw new Error('导出失败')
        return res.blob()
      })
      .then(blob => {
        const blobUrl = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = blobUrl
        a.download = `学生成绩表_${offeringId.value}.xlsx`
        a.click()
        window.URL.revokeObjectURL(blobUrl)
        ElMessage.success('导出成功')
      })
      .catch(() => ElMessage.error('导出失败'))
}

// ============ 考核环节（仅用于计数） ============
const fetchAssessments = async () => {
  try {
    const res = await request.get(`/teacher/assessments/offering/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      assessments.value = res.data
    }
  } catch (e) {
    assessments.value = []
  }
}

// ============ 课程目标 ============
const fetchObjectives = async () => {
  objectivesLoading.value = true
  try {
    const res = await request.get(`/teacher/offering/${offeringId.value}/objectives`)
    if (res.status === 'success' && res.data) {
      objectives.value = res.data
      fetchMatrixData()
    }
  } catch (e) {
    console.error('获取课程目标失败:', e)
    objectives.value = []
  } finally {
    objectivesLoading.value = false  // ← 课程目标表格先显示
  }
}

const openObjectiveDialog = (row) => {
  if (row) {
    editingObjectiveId.value = row.id
    objForm.value = {code: row.code || '', description: row.description || '', weight: row.weight || 0.5}
  } else {
    editingObjectiveId.value = null
    objForm.value = {code: '', description: '', weight: 0.5}
  }
  objectiveDialogVisible.value = true
}

const submitObjective = async () => {
  objSubmitting.value = true
  try {
    const body = {...objForm.value}
    if (editingObjectiveId.value) {
      await request.put(`/teacher/objectives/${editingObjectiveId.value}`, body)
      ElMessage.success('课程目标已更新')
    } else {
      await request.post(`/teacher/offering/${offeringId.value}/objectives`, body)
      ElMessage.success('课程目标已添加')
    }
    objectiveDialogVisible.value = false
    fetchObjectives()
  } catch (e) {
    // 已提示
  } finally {
    objSubmitting.value = false
  }
}

const deleteObjective = (row) => {
  ElMessageBox.confirm(`确定删除课程目标 "${row.code}" 吗？`, '确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/teacher/objectives/${row.id}`)
      ElMessage.success('已删除')
      fetchObjectives()
    } catch (e) {
      // 已提示
    }
  }).catch(() => {
  })
}

// ============ 资源 ============
const fetchResources = async () => {
  resourcesLoading.value = true
  try {
    const res = await request.get(`/teacher/resources/offering/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      resources.value = res.data
    }
  } catch (e) {
    resources.value = []
  } finally {
    resourcesLoading.value = false
  }
}

const downloadResource = (row) => {
  const token = localStorage.getItem('token')
  const url = `/api/teacher/resources/download/${row.id}`
  fetch(url, {headers: {Authorization: `Bearer ${token}`}})
      .then(res => res.blob())
      .then(blob => {
        const blobUrl = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = blobUrl
        a.download = row.fileName || 'download'
        a.click()
        window.URL.revokeObjectURL(blobUrl)
      })
      .catch(() => ElMessage.error('下载失败'))
}

const deleteResource = (row) => {
  ElMessageBox.confirm(`确定删除资源 "${row.fileName}" 吗？`, '确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/teacher/resources/${row.id}`)
      ElMessage.success('已删除')
      fetchResources()
    } catch (e) {
      // 已提示
    }
  }).catch(() => {
  })
}

// ============ 学生名单组件引用 ============
const studentImportRef = ref(null)

// ============ 学生名单刷新回调 ============
const handleStudentRefresh = () => {
  fetchStudents()
}

const handleStudentCountChange = (count) => {
  console.log('学生人数已更新:', count)
}

// ============ Tab 切换 ============
const handleTabChange = (tab) => {
  switch (tab) {
    case 'students':
      if (studentImportRef.value) {
        studentImportRef.value.loadStudents()
      } else {
        fetchStudents()
      }
      break
    case 'grades':
      fetchWeights().then(() => {
        fetchStudentGrades().then(() => recalcColumnWidth())
      })
      if (assessments.value.length === 0) {
        fetchAssessments()
      }
      break
    case 'objectives':
      if (objectives.value.length === 0) {
        fetchObjectives()
      }
      break
    case 'resources':
      if (resources.value.length === 0) {
        fetchResources()
      }
      break
  }
}

// ============ 初始化 ============
onMounted(async () => {
  // 1. 先加载课程基本信息（必须，因为其他接口需要 programId）
  await fetchCourseInfo()
  console.log('courseInfo 加载完成:', courseInfo.value)

  // 2. 并行加载所有数据，互不阻塞
  await Promise.all([
    fetchObjectives(),   // 课程目标 + 矩阵数据
    fetchStudents(),     // 学生人数（与矩阵同时加载）
    fetchResources(),    // 教学资源
    fetchAssessments()   // 考核环节
  ])

  console.log('所有数据加载完成')
  console.log('objectives 数量:', objectives.value.length)
  console.log('学生人数:', rawStudents.value.length)

  // 3. 成绩表格列宽自适应
  resizeObserver = new ResizeObserver(() => recalcColumnWidth())
  const wrap = gradeTableWrapRef.value
  if (wrap) resizeObserver.observe(wrap)
  window.addEventListener('resize', recalcColumnWidth)
})
</script>

<style scoped>
.teacher-course-manage-container {
  padding: 20px;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.course-title {
  margin: 12px 0 8px;
  font-size: 24px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #666;
  font-size: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-item {
  padding: 16px 20px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.summary-label {
  margin: 0 0 8px;
  color: #909399;
  font-size: 14px;
}

.summary-value {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 700;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

/* 权重显示 */
.weight-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.weight-tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #faecd8;
}

/* 成绩表格容器 */
.grade-table-wrap {
  width: 100%;
  overflow: hidden;
}

/* 成绩单元格居中 */
.score-cell {
  display: inline-block;
  width: 100%;
  text-align: center;
}

.score-cell.empty {
  color: #c0c4cc;
}

.score-cell.total-score {
  font-weight: 600;
  color: #303133;
}

/* ========== 矩阵样式 ========== */
.matrix-section {
  margin-top: 24px;
}

.matrix-section .tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.matrix-section .header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.matrix-section .tab-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.obj-label {
  font-weight: 600;
  color: #409EFF;
  font-size: 13px;
}

.indicator-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.indicator-code {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
  cursor: help;
}

.indicator-req {
  font-size: 11px;
  color: #909399;
}

/* 下拉框选中的颜色 */
:deep(.ss-h .el-input__wrapper) {
  background-color: #fef0f0 !important;
  border-color: #f56c6c !important;
}

:deep(.ss-h .el-input__wrapper:hover) {
  border-color: #f56c6c !important;
}

:deep(.ss-h .el-select__selected-item) {
  color: #f56c6c !important;
  font-weight: 700;
}

:deep(.ss-m .el-input__wrapper) {
  background-color: #fdf6ec !important;
  border-color: #e6a23c !important;
}

:deep(.ss-m .el-input__wrapper:hover) {
  border-color: #e6a23c !important;
}

:deep(.ss-m .el-select__selected-item) {
  color: #e6a23c !important;
  font-weight: 700;
}

:deep(.ss-l .el-input__wrapper) {
  background-color: #f0f9eb !important;
  border-color: #67c23a !important;
}

:deep(.ss-l .el-input__wrapper:hover) {
  border-color: #67c23a !important;
}

:deep(.ss-l .el-select__selected-item) {
  color: #67c23a !important;
  font-weight: 700;
}

:deep(.ss-n .el-input__wrapper) {
  background-color: #f5f7fa !important;
  border-color: #dcdfe6 !important;
}

:deep(.ss-n .el-input__wrapper:hover) {
  border-color: #dcdfe6 !important;
}

:deep(.ss-n .el-select__selected-item) {
  color: #c0c4cc !important;
}

.so {
  font-weight: 700;
  font-size: 14px;
}

.so-h {
  color: #f56c6c;
}

.so-m {
  color: #e6a23c;
}

.so-l {
  color: #67c23a;
}

.so-none {
  color: #c0c4cc;
}

.matrix-legend {
  margin-top: 12px;
  padding: 10px 16px;
  background: #f7f8fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
}

.matrix-legend .legend-title {
  font-weight: 600;
  color: #333;
}

.sh {
  color: #f56c6c;
  font-weight: 500;
}

.sm {
  color: #e6a23c;
  font-weight: 500;
}

.sl {
  color: #67c23a;
  font-weight: 500;
}

.sn {
  color: #c0c4cc;
  font-weight: 500;
}
</style>
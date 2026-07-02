<template>
  <div class="course-detail-container" v-loading="loading">
    <el-card>
      <div class="course-header">
        <div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>我的课程</el-breadcrumb-item>
            <el-breadcrumb-item>{{ courseInfo.courseName || '加载中...' }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h2 class="course-title">{{ courseInfo.courseName }}</h2>
          <div class="course-meta">
            <span>课程代码：{{ courseInfo.courseCode }}</span>
            <span>学分：{{ courseInfo.credits }}</span>
            <span>授课教师：{{ courseInfo.teacherName }}</span>
          </div>
        </div>
        <el-tag type="primary" size="large">
          {{ courseInfo.academicYear }} {{ courseInfo.semester }}
        </el-tag>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="课程名称">{{ courseInfo.courseName }}</el-descriptions-item>
            <el-descriptions-item label="课程代码">{{ courseInfo.courseCode }}</el-descriptions-item>
            <el-descriptions-item label="授课教师">{{ courseInfo.teacherName }}</el-descriptions-item>
            <el-descriptions-item label="学分">{{ courseInfo.credits }}</el-descriptions-item>
            <el-descriptions-item label="课程类型">{{ courseInfo.courseType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否必修">
              <el-tag :type="courseInfo.isRequired ? 'danger' : 'info'">{{ courseInfo.isRequired ? '必修' : '选修' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="课程目标" name="objectives">
          <div class="objectives-content" v-loading="objLoading">
            <ul v-if="objectives.length">
              <li v-for="obj in objectives" :key="obj.id">
                <strong>{{ obj.code }}</strong>：{{ obj.description }}
                <el-tag size="small" style="margin-left: 8px">权重: {{ obj.weight }}</el-tag>
              </li>
            </ul>
            <el-empty v-else description="暂无课程目标" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="课程资源" name="resources">
          <div class="resources-content" v-loading="resLoading">
            <el-table :data="resources" border v-if="resources.length">
              <el-table-column prop="fileName" label="资源名称" min-width="200" />
              <el-table-column prop="resourceType" label="类型" width="120">
                <template #default="scope">
                  <el-tag size="small" :type="getResourceTypeTag(scope.row.resourceType)">
                    {{ scope.row.resourceType || '其他' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="170" />
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button link type="primary" @click="downloadResource(scope.row)">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无课程资源" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="成绩明细" name="grades">
          <div class="grades-content" v-loading="gradeLoading">
            <el-table :data="gradeTableData" border v-if="myGrade">
              <el-table-column label="考核项目" width="150">
                <template #default="scope">{{ scope.row.label }}</template>
              </el-table-column>
              <el-table-column label="成绩" width="120" align="center">
                <template #default="scope">
                  <span v-if="scope.row.value != null" :style="scope.row.label === '最终成绩' ? 'font-weight:600' : ''">
                    {{ scope.row.value }}
                  </span>
                  <span v-else style="color: #999">-</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无成绩" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const route = useRoute()
const courseId = route.params.id

const activeTab = ref('basic')
const loading = ref(false)
const objLoading = ref(false)
const resLoading = ref(false)
const gradeLoading = ref(false)

const courseInfo = ref({
  courseName: '',
  courseCode: '',
  credits: '',
  teacherName: '',
  academicYear: '',
  semester: '',
  courseType: '',
  isRequired: false
})

const objectives = ref([])
const resources = ref([])
const myGrade = ref(null)

const gradeTableData = computed(() => {
  if (!myGrade.value) return []
  return [
    { label: '平时成绩', value: myGrade.value.dailyScore },
    { label: '实验报告', value: myGrade.value.reportScore },
    { label: '期中考试', value: myGrade.value.midtermScore },
    { label: '期末考试', value: myGrade.value.finalScore },
    { label: '最终成绩', value: myGrade.value.totalScore },
  ]
})

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

// 从学生课程列表中找到对应课程信息，并获取详情
const loadCourseInfo = async () => {
  loading.value = true
  try {
    // 获取学生课程列表，找到对应的课程和开课记录
    const res = await request.get('/student/courses')
    if (res.status === 'success' && res.data) {
      const course = res.data.find(c => String(c.courseId) === String(courseId))
      if (course) {
        courseInfo.value = course
        // 如果有开课记录，加载更多信息
        if (course.offeringId) {
          loadObjectives(course.offeringId)
          loadResources(course.offeringId)
          loadMyGrades(course.offeringId)
        }
      }
    }
  } catch (e) {
    // 忽略
  } finally {
    loading.value = false
  }
}

const loadObjectives = async (offeringId) => {
  objLoading.value = true
  try {
    const res = await request.get(`/student/objectives/${offeringId}`)
    if (res.status === 'success' && res.data) {
      objectives.value = res.data
    }
  } catch (e) {
    objectives.value = []
  } finally {
    objLoading.value = false
  }
}

const loadResources = async (offeringId) => {
  resLoading.value = true
  try {
    const res = await request.get(`/teacher/resources/offering/${offeringId}`)
    if (res.status === 'success' && res.data) {
      resources.value = res.data
    }
  } catch (e) {
    resources.value = []
  } finally {
    resLoading.value = false
  }
}

const loadMyGrades = async (offeringId) => {
  gradeLoading.value = true
  try {
    const res = await request.get(`/student/grades/${offeringId}`)
    if (res.status === 'success' && res.data) {
      myGrade.value = res.data
    }
  } catch (e) {
    myGrade.value = null
  } finally {
    gradeLoading.value = false
  }
}

const downloadResource = (row) => {
  const token = localStorage.getItem('token')
  const url = `/api/teacher/resources/download/${row.id}`
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
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

onMounted(() => {
  loadCourseInfo()
})
</script>

<style scoped>
.course-detail-container { padding: 20px; }
.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 20px;
}
.course-title { font-size: 24px; margin: 12px 0 8px; }
.course-meta {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}
.objectives-content, .resources-content, .grades-content {
  padding: 10px 0;
}
.objectives-content ul {
  list-style: none;
  padding: 0;
}
.objectives-content li {
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
  line-height: 1.8;
}
</style>
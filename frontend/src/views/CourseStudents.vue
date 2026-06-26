<template>
  <div class="course-students-container" v-loading="loading">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>课程管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ courseName }}</el-breadcrumb-item>
              <el-breadcrumb-item>学生名单</el-breadcrumb-item>
            </el-breadcrumb>
          </span>
          <span class="student-count">共 {{ students.length }} 名学生</span>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索姓名/学号" style="width: 300px" clearable />
      </div>

      <el-table :data="filteredStudents" border style="width: 100%">
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column label="成绩概况" min-width="250">
          <template #default="scope">
            <el-tag
              v-for="g in scope.row.grades"
              :key="g.assessmentId"
              size="small"
              style="margin-right: 4px; margin-bottom: 2px"
            >
              {{ g.assessmentName }}: {{ g.score }}
            </el-tag>
            <span v-if="!scope.row.grades || scope.row.grades.length === 0" style="color: #999">未录入</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="filteredStudents.length"
          :page-size="10"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'

const route = useRoute()
const offeringId = route.params.id

const loading = ref(false)
const courseName = ref('加载中...')
const keyword = ref('')
const students = ref([])

const filteredStudents = computed(() => {
  if (!keyword.value) return students.value
  const k = keyword.value.toLowerCase()
  return students.value.filter(s =>
    String(s.studentId).includes(k) || (s.studentName && s.studentName.includes(k))
  )
})

const loadData = async () => {
  loading.value = true
  try {
    // 加载开课信息
    const offeringRes = await request.get(`/teacher/offering/detail/${offeringId}`)
    if (offeringRes.status === 'success' && offeringRes.data) {
      courseName.value = offeringRes.data.courseName || '未知课程'
    }

    // 加载成绩（含学生信息）
    const gradesRes = await request.get('/teacher/grades/list', {
      params: { offeringId, pageSize: 999 }
    })
    if (gradesRes.status === 'success' && gradesRes.data && gradesRes.data.list) {
      const studentMap = new Map()
      gradesRes.data.list.forEach(g => {
        if (!studentMap.has(g.studentId)) {
          studentMap.set(g.studentId, {
            studentId: g.studentId,
            studentName: g.studentName,
            grades: []
          })
        }
        studentMap.get(g.studentId).grades.push({
          assessmentId: g.assessmentId,
          assessmentName: g.assessmentName,
          score: g.score
        })
      })
      students.value = Array.from(studentMap.values())
    }
  } catch (e) {
    students.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.course-students-container { padding: 20px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.student-count { color: #666; font-size: 14px; }
.search-bar { margin-bottom: 16px; }
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

<template>
  <div class="teacher-course-manage-container">
    <el-card>
      <template #header>
        <div class="course-header">
          <div>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/my-courses' }">课程管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ courseInfo.name }}</el-breadcrumb-item>
            </el-breadcrumb>
            <h2 class="course-title">{{ courseInfo.name }}</h2>
            <div class="course-meta">
              <span>课程代码：{{ courseInfo.code }}</span>
              <span>学分：{{ courseInfo.credits }}</span>
              <span>授课教师：{{ courseInfo.teacher }}</span>
              <span>班级：{{ courseInfo.className }}</span>
            </div>
          </div>
          <el-tag :type="getStatusType(courseInfo.status)" size="large">
            {{ courseInfo.status }}
          </el-tag>
        </div>
      </template>

      <div class="summary-grid">
        <div class="summary-item">
          <p class="summary-label">学生人数</p>
          <p class="summary-value">{{ students.length }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">作业数</p>
          <p class="summary-value">{{ assignments.length }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">平均成绩</p>
          <p class="summary-value">{{ averageScore }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">出勤率</p>
          <p class="summary-value">{{ attendanceRate }}%</p>
        </div>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="学生名单" name="students">
          <div class="toolbar">
            <el-input
              v-model="studentKeyword"
              placeholder="搜索姓名/学号"
              style="width: 260px"
            />
            <el-button type="primary" :icon="Download" @click="handleExportStudents">导出名单</el-button>
          </div>
          <el-table :data="filteredStudents" border style="width: 100%">
            <el-table-column prop="id" label="学号" width="120" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="class" label="班级" width="100" />
            <el-table-column prop="phone" label="联系方式" width="150" />
            <el-table-column prop="attendance" label="出勤率" width="100" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="成绩录入" name="grades">
          <el-table :data="gradeRows" border style="width: 100%">
            <el-table-column prop="id" label="学号" width="120" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column label="平时成绩" width="150">
              <template #default="scope">
                <el-input-number v-model="scope.row.daily" :min="0" :max="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="实验成绩" width="150">
              <template #default="scope">
                <el-input-number v-model="scope.row.lab" :min="0" :max="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="期末成绩" width="150">
              <template #default="scope">
                <el-input-number v-model="scope.row.final" :min="0" :max="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="总评" width="100">
              <template #default="scope">
                {{ calcTotal(scope.row) }}
              </template>
            </el-table-column>
          </el-table>
          <div class="form-actions">
            <el-button type="primary" @click="handleSaveGrades">保存成绩</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="考勤管理" name="attendance">
          <el-table :data="attendanceRows" border style="width: 100%">
            <el-table-column prop="date" label="日期" width="140" />
            <el-table-column prop="topic" label="授课内容" />
            <el-table-column prop="present" label="出勤" width="100" />
            <el-table-column prop="absent" label="缺勤" width="100" />
            <el-table-column prop="late" label="迟到" width="100" />
            <el-table-column label="操作" width="120">
              <template #default>
                <el-button link type="primary">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="作业与资源" name="resources">
          <div class="toolbar">
            <el-button type="primary" :icon="Plus" @click="handleCreateAssignment">发布作业</el-button>
            <el-button :icon="Upload" @click="handleUploadResource">上传资源</el-button>
          </div>
          <el-table :data="assignments" border style="width: 100%">
            <el-table-column prop="title" label="名称" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止时间" width="160" />
            <el-table-column prop="submitCount" label="提交情况" width="120" />
            <el-table-column label="操作" width="160">
              <template #default>
                <el-button link type="primary">查看</el-button>
                <el-button link type="primary">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Plus, Upload } from '@element-plus/icons-vue'

const route = useRoute()
const activeTab = ref('students')
const studentKeyword = ref('')

const courseMap = {
  '1': { name: '数据结构与算法', code: 'CS301', teacher: '王教授', credits: 3, className: '计算机科学与技术 2024级1班', status: '进行中' },
  '2': { name: '操作系统', code: 'CS302', teacher: '李教授', credits: 3, className: '软件工程 2024级1班', status: '进行中' },
  '3': { name: '数据库系统', code: 'CS303', teacher: '张教授', credits: 2, className: '数据科学 2024级1班', status: '已结束' },
  '4': { name: '计算机网络', code: 'CS201', teacher: '赵教授', credits: 3, className: '计算机科学与技术 2023级1班', status: '已结束' },
  '5': { name: '软件工程', code: 'CS202', teacher: '刘教授', credits: 2, className: '软件工程 2023级2班', status: '已结束' }
}

const courseInfo = computed(() => courseMap[route.params.id] || courseMap['1'])

const students = ref([
  { id: '2024001', name: '张明', major: '计算机科学与技术', class: '1班', phone: '138****1234', attendance: '96%' },
  { id: '2024002', name: '李红', major: '计算机科学与技术', class: '1班', phone: '138****1235', attendance: '92%' },
  { id: '2024003', name: '王强', major: '软件工程', class: '2班', phone: '138****1236', attendance: '88%' },
  { id: '2024004', name: '赵丽', major: '数据科学', class: '1班', phone: '138****1237', attendance: '100%' }
])

const gradeRows = ref([
  { id: '2024001', name: '张明', daily: 88, lab: 92, final: 86 },
  { id: '2024002', name: '李红', daily: 91, lab: 89, final: 90 },
  { id: '2024003', name: '王强', daily: 82, lab: 84, final: 78 },
  { id: '2024004', name: '赵丽', daily: 95, lab: 93, final: 94 }
])

const attendanceRows = ref([
  { date: '2024-09-02', topic: '课程导论', present: 42, absent: 1, late: 2 },
  { date: '2024-09-09', topic: '线性表', present: 44, absent: 0, late: 1 },
  { date: '2024-09-16', topic: '栈和队列', present: 43, absent: 1, late: 1 }
])

const assignments = ref([
  { title: '线性表课后作业', type: '作业', deadline: '2024-09-20', submitCount: '40/45' },
  { title: '实验一：顺序表实现', type: '实验', deadline: '2024-09-28', submitCount: '38/45' },
  { title: '第一章课件', type: '资源', deadline: '-', submitCount: '-' }
])

const filteredStudents = computed(() => {
  const keyword = studentKeyword.value.trim()
  if (!keyword) return students.value
  return students.value.filter(item => item.id.includes(keyword) || item.name.includes(keyword))
})

const averageScore = computed(() => {
  const total = gradeRows.value.reduce((sum, item) => sum + Number(calcTotal(item)), 0)
  return (total / gradeRows.value.length).toFixed(1)
})

const attendanceRate = computed(() => {
  const totalPresent = attendanceRows.value.reduce((sum, item) => sum + item.present, 0)
  const total = attendanceRows.value.reduce((sum, item) => sum + item.present + item.absent + item.late, 0)
  return Math.round((totalPresent / total) * 100)
})

const calcTotal = (row) => {
  return (row.daily * 0.3 + row.lab * 0.3 + row.final * 0.4).toFixed(1)
}

const getStatusType = (status) => {
  const map = { '进行中': 'primary', '已结束': 'info', '未开始': 'warning' }
  return map[status] || 'info'
}

const handleExportStudents = () => {
  ElMessage.success('学生名单已导出')
}

const handleSaveGrades = () => {
  ElMessage.success('成绩保存成功')
}

const handleCreateAssignment = () => {
  ElMessage.info('打开作业发布窗口')
}

const handleUploadResource = () => {
  ElMessage.info('打开资源上传窗口')
}
</script>

<style scoped>
.teacher-course-manage-container { padding: 20px; }

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

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

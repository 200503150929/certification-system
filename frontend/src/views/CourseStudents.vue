<template>
  <div class="course-students-container">
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

      <!-- 搜索 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名/学号"
          prefix-icon="Search"
          style="width: 300px"
        />
      </div>

      <!-- 学生列表 -->
      <el-table :data="filteredStudents" border style="width: 100%">
        <el-table-column prop="id" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column prop="class" label="班级" width="100" />
        <el-table-column prop="phone" label="联系方式" width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewStudent(scope.row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';

const route = useRoute();
const courseId = route.params.id;
const courseName = ref('数据结构与算法');
const keyword = ref('');

const students = ref([
  { id: '2024001', name: '张明', major: '计算机科学与技术', grade: '2024级', class: '1班', phone: '138****1234' },
  { id: '2024002', name: '李红', major: '计算机科学与技术', grade: '2024级', class: '1班', phone: '138****1235' },
  { id: '2024003', name: '王强', major: '软件工程', grade: '2024级', class: '2班', phone: '138****1236' },
  { id: '2024004', name: '赵丽', major: '数据科学', grade: '2024级', class: '1班', phone: '138****1237' },
]);

const filteredStudents = computed(() => {
  if (!keyword.value) return students.value;
  const k = keyword.value.toLowerCase();
  return students.value.filter(s =>
    s.name.includes(k) || s.id.includes(k)
  );
});

const viewStudent = (student) => {
  ElMessage.info(`查看学生：${student.name}`);
  // 跳转到学生详情或弹出对话框
};
</script>

<style scoped>
.course-students-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.student-count {
  color: #666;
  font-size: 14px;
}
.search-bar {
  margin-bottom: 16px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
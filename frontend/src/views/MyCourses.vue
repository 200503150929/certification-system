<template>
  <div class="my-courses-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ pageTitle }}</span>
          <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 150px">
            <el-option label="全部学期" value="all" />
            <el-option label="2024-2025学年第一学期" value="2024-1" />
            <el-option label="2024-2025学年第二学期" value="2024-2" />
            <el-option label="2023-2024学年第一学期" value="2023-1" />
          </el-select>
        </div>
      </template>

      <!-- 按学期分组 -->
      <div v-for="group in filteredCourses" :key="group.semester" class="course-group">
        <h3 class="semester-title">{{ group.semester }}</h3>
        <div class="course-grid">
          <el-card
            v-for="course in group.courses"
            :key="course.id"
            class="course-card"
            @click="goToDetail(course.id)"
          >
            <div class="course-info">
              <h4>{{ course.name }}</h4>
              <p class="course-code">课程代码：{{ course.code }}</p>
              <p class="course-teacher">授课教师：{{ course.teacher }}</p>
              <div class="course-meta">
                <el-tag size="small" type="info">{{ course.credits }} 学分</el-tag>
                <el-tag size="small" :type="getStatusType(course.status)">
                  {{ course.status }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="filteredCourses.length === 0" description="暂无课程" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const selectedSemester = ref('all');
const userRole = ref(localStorage.getItem('userRole') || '学生');
const pageTitle = computed(() => userRole.value === '教师' ? '课程管理' : '我的课程');

// 模拟课程数据
const courseData = ref([
  {
    semester: '2024-2025学年第一学期',
    courses: [
      { id: 1, name: '数据结构与算法', code: 'CS301', teacher: '王教授', credits: 3, status: '进行中' },
      { id: 2, name: '操作系统', code: 'CS302', teacher: '李教授', credits: 3, status: '进行中' },
      { id: 3, name: '数据库系统', code: 'CS303', teacher: '张教授', credits: 2, status: '已结束' },
    ]
  },
  {
    semester: '2023-2024学年第二学期',
    courses: [
      { id: 4, name: '计算机网络', code: 'CS201', teacher: '赵教授', credits: 3, status: '已结束' },
      { id: 5, name: '软件工程', code: 'CS202', teacher: '刘教授', credits: 2, status: '已结束' },
    ]
  }
]);

const filteredCourses = computed(() => {
  const currentTeacher = localStorage.getItem('username') || '';
  const groups = userRole.value === '教师'
    ? courseData.value
        .map(group => ({
          ...group,
          courses: group.courses.filter(course => course.teacher === currentTeacher)
        }))
        .filter(group => group.courses.length > 0)
    : courseData.value;

  if (selectedSemester.value === 'all') return groups;
  return groups.filter(g => g.semester === selectedSemester.value);
});

const getStatusType = (status) => {
  const map = { '进行中': 'primary', '已结束': 'info', '未开始': 'warning' };
  return map[status] || 'info';
};

const goToDetail = (courseId) => {
  if (userRole.value === '教师') {
    router.push(`/teacher/course/${courseId}`);
    return;
  }
  router.push(`/course/${courseId}`);
};
</script>

<style scoped>
.my-courses-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.course-group {
  margin-bottom: 30px;
}
.semester-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f2f5;
  margin-bottom: 16px;
}
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.course-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
}
.course-info h4 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #333;
}
.course-code, .course-teacher {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}
.course-meta {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
</style>

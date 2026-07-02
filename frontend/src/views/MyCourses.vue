<template>
  <div class="my-courses-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ pageTitle }}</span>
          <el-select v-model="selectedSemester" placeholder="选择学期" style="width: 200px" clearable>
            <el-option label="全部学期" value="all" />
            <el-option
              v-for="sem in semesterOptions"
              :key="sem.value"
              :label="sem.label"
              :value="sem.value"
            />
          </el-select>
        </div>
      </template>

      <!-- 按学期分组 -->
      <div v-for="group in groupedCourses" :key="group.key" class="course-group">
        <h3 class="semester-title">{{ group.label }}</h3>
        <div class="course-grid">
          <el-card
            v-for="course in group.courses"
            :key="course.offeringId"
            class="course-card"
            @click="goToDetail(course)"
          >
            <div class="course-info">
              <h4>{{ course.courseName }}</h4>
              <p class="course-code">课程代码：{{ course.courseCode }}</p>
              <p class="course-teacher">授课教师：{{ course.teacherName || '-' }}</p>
              <div class="course-meta">
                <el-tag size="small" type="info">{{ course.credits }} 学分</el-tag>
                <el-tag size="small" :type="course.isRequired ? 'danger' : 'warning'">
                  {{ course.isRequired ? '必修' : '选修' }}
                </el-tag>
                <el-tag size="small" v-if="course.courseType" type="success">
                  {{ course.courseType }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && groupedCourses.length === 0" description="暂无课程数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import request from '@/api/request';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const selectedSemester = ref('all');

const pageTitle = computed(() => {
  if (authStore.permissions.includes('course:teach')) return '课程管理';
  return '我的课程';
});

/** 判断当前用户是否为教师身份（拥有授课权限） */
const isTeacher = computed(() => authStore.permissions.includes('course:teach'));

// 从后端获取的课程数据
const courses = ref([]);

// 学期选项（从数据中提取）
const semesterOptions = computed(() => {
  const semSet = new Set();
  courses.value.forEach(c => {
    if (c.academicYear && c.semester) {
      semSet.add(c.academicYear + ' ' + c.semester);
    }
  });
  return Array.from(semSet).sort().reverse().map(s => ({
    label: s,
    value: s
  }));
});

// 按学期分组并筛选
const groupedCourses = computed(() => {
  const groups = {};
  courses.value.forEach(course => {
    const key = (course.academicYear || '') + ' ' + (course.semester || '');
    if (selectedSemester.value !== 'all' && key !== selectedSemester.value) return;
    if (!groups[key]) {
      groups[key] = [];
    }
    groups[key].push(course);
  });
  // 转换为数组并按学期反序排列
  return Object.entries(groups)
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([key, courses]) => ({
      key,
      label: key || '未知学期',
      courses
    }));
});

// 从后端加载课程（学生和教师调用不同API）
const loadCourses = async () => {
  loading.value = true;
  try {
    if (isTeacher.value) {
      // 教师端：调用教师开课列表API
      const res = await request.get('/teacher/offering/list', {
        params: { pageNum: 1, pageSize: 100 }
      });
      if (res.data && res.data.list) {
        // 将教师开课数据映射为统一格式
        courses.value = res.data.list.map(item => ({
          offeringId: item.id,
          courseId: item.courseId,
          courseCode: item.courseCode,
          courseName: item.courseName,
          credits: item.credits,
          teacherName: item.teacherName,
          academicYear: item.academicYear,
          semester: item.semester,
          courseType: item.courseType,
          isRequired: item.isRequired
        }));
      }
    } else {
      // 学生端：调用学生课程API
      const res = await request.get('/student/courses');
      if (res.data) {
        courses.value = res.data;
      }
    }
  } catch (error) {
    console.error('加载课程列表失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadCourses();
});

const goToDetail = (course) => {
  if (isTeacher.value) {
    router.push(`/app/teacher/course/${course.offeringId}`);
    return;
  }
  router.push(`/app/course/${course.courseId}`);
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
  flex-wrap: wrap;
}
</style>
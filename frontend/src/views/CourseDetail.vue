<template>
  <div class="course-detail-container">
    <el-card>
      <!-- 顶部课程标题 -->
      <div class="course-header">
        <div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/my-courses' }">我的课程</el-breadcrumb-item>
            <el-breadcrumb-item>{{ courseInfo.name }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h2 class="course-title">{{ courseInfo.name }}</h2>
          <div class="course-meta">
            <span>课程代码：{{ courseInfo.code }}</span>
            <span>学分：{{ courseInfo.credits }}</span>
            <span>授课教师：{{ courseInfo.teacher }}</span>
          </div>
        </div>
        <el-tag :type="getStatusType(courseInfo.status)" size="large">
          {{ courseInfo.status }}
        </el-tag>
      </div>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="课程名称">{{ courseInfo.name }}</el-descriptions-item>
            <el-descriptions-item label="课程代码">{{ courseInfo.code }}</el-descriptions-item>
            <el-descriptions-item label="授课教师">{{ courseInfo.teacher }}</el-descriptions-item>
            <el-descriptions-item label="学分">{{ courseInfo.credits }}</el-descriptions-item>
            <el-descriptions-item label="学时">{{ courseInfo.hours }}</el-descriptions-item>
            <el-descriptions-item label="上课时间">{{ courseInfo.schedule }}</el-descriptions-item>
            <el-descriptions-item label="上课地点">{{ courseInfo.location }}</el-descriptions-item>
            <el-descriptions-item label="课程简介" :span="2">
              {{ courseInfo.description }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- 教学大纲 -->
        <el-tab-pane label="教学大纲" name="syllabus">
          <div class="syllabus-content" v-html="courseInfo.syllabus"></div>
        </el-tab-pane>

        <!-- 课程目标 -->
        <el-tab-pane label="课程目标" name="objectives">
          <div class="objectives-content">
            <ul>
              <li v-for="(obj, index) in courseInfo.objectives" :key="index">
                {{ obj }}
              </li>
            </ul>
          </div>
        </el-tab-pane>

        <!-- 考核要求 -->
        <el-tab-pane label="考核要求" name="assessment">
          <div class="assessment-content">
            <el-table :data="courseInfo.assessment" border>
              <el-table-column prop="item" label="考核项目" />
              <el-table-column prop="weight" label="权重" width="120" />
              <el-table-column prop="description" label="说明" />
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 课程资源 -->
        <el-tab-pane label="课程资源" name="resources">
          <div class="resources-content">
            <el-table :data="courseInfo.resources" border>
              <el-table-column prop="name" label="资源名称" />
              <el-table-column prop="type" label="类型" width="120">
                <template #default="scope">
                  <el-tag size="small">{{ scope.row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="size" label="大小" width="120" />
              <el-table-column label="操作" width="120">
                <template #default>
                  <el-button link type="primary">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 成绩明细 -->
        <el-tab-pane label="成绩明细" name="grades">
          <div class="grades-content">
            <el-table :data="courseInfo.grades" border>
              <el-table-column prop="item" label="考核项目" />
              <el-table-column prop="score" label="成绩" width="120" />
              <el-table-column prop="weight" label="权重" width="120" />
              <el-table-column prop="weightedScore" label="加权成绩" width="120" />
            </el-table>
            <div class="total-score">
              <span>总评成绩：</span>
              <span class="score-value">{{ courseInfo.totalScore }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const courseId = route.params.id;

const activeTab = ref('basic');

// 模拟课程数据
const courseInfo = ref({
  name: '数据结构与算法',
  code: 'CS301',
  teacher: '王教授',
  credits: 3,
  hours: 48,
  schedule: '周一 1-2节，周三 3-4节',
  location: '教学楼A-301',
  status: '进行中',
  description: '本课程系统介绍数据结构与算法的基本概念、原理和实现方法...',
  syllabus: '<p>第一章：绪论</p><p>第二章：线性表</p><p>第三章：栈和队列</p>...',
  objectives: [
    '掌握基本数据结构的原理和实现',
    '能够分析算法的时间复杂度和空间复杂度',
    '具备解决实际问题的编程能力'
  ],
  assessment: [
    { item: '平时作业', weight: '20%', description: '每章课后作业' },
    { item: '期中考试', weight: '30%', description: '期中笔试' },
    { item: '期末考试', weight: '50%', description: '期末笔试' }
  ],
  resources: [
    { name: '课件-第一章.ppt', type: 'PPT', size: '2.4MB' },
    { name: '课件-第二章.ppt', type: 'PPT', size: '1.8MB' },
    { name: '实验指导书.pdf', type: 'PDF', size: '3.2MB' }
  ],
  grades: [
    { item: '平时作业', score: 92, weight: '20%', weightedScore: 18.4 },
    { item: '期中考试', score: 85, weight: '30%', weightedScore: 25.5 },
    { item: '期末考试', score: 88, weight: '50%', weightedScore: 44.0 }
  ],
  totalScore: 87.9
});

const getStatusType = (status) => {
  const map = { '进行中': 'primary', '已结束': 'info', '未开始': 'warning' };
  return map[status] || 'info';
};

onMounted(() => {
  // 实际从 API 获取课程详情
  // courseInfo.value = await getCourseDetail(courseId)
});
</script>

<style scoped>
.course-detail-container {
  padding: 20px;
}
.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 20px;
}
.course-title {
  font-size: 24px;
  margin: 12px 0 8px;
}
.course-meta {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}
.total-score {
  margin-top: 20px;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 6px;
  text-align: right;
  font-size: 16px;
}
.score-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}
.syllabus-content, .objectives-content {
  padding: 10px 0;
  line-height: 2;
}
.resources-content, .grades-content {
  padding: 10px 0;
}
</style>
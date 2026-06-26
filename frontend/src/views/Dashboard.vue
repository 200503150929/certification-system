<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div>
        <h2>工程教育认证数据看板</h2>
        <p>面向管理员查看的全周期教学数据</p>
      </div>
      <div>
        <el-select v-model="semester" placeholder="选择学期" style="width: 200px; margin-right: 10px;">
          <el-option label="2024-2025学年 第二学期" value="1"></el-option>
        </el-select>
        <el-button :icon="Refresh" circle @click="refreshData" />
      </div>
    </div>

    <el-row :gutter="20" class="kpi-cards">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="kpi-card-item">
            <div class="kpi-icon" style="background-color: #e6f7ff;"><el-icon color="#1890ff"><Menu /></el-icon></div>
            <div class="kpi-text">
              <p class="kpi-label">培养方案版本数</p>
              <p class="kpi-value">8 <small>个</small></p>
              <p class="kpi-change increase">+ 12.5%</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="kpi-card-item">
            <div class="kpi-icon" style="background-color: #e6f7ff;"><el-icon color="#1890ff"><Collection /></el-icon></div>
            <div class="kpi-text">
              <p class="kpi-label">课程总数</p>
              <p class="kpi-value">156 <small>门</small></p>
              <p class="kpi-change increase">+ 3.2%</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="kpi-card-item">
            <div class="kpi-icon" style="background-color: #fffbe6;"><el-icon color="#faad14"><Trophy /></el-icon></div>
            <div class="kpi-text">
              <p class="kpi-label">当前学期教学任务</p>
              <p class="kpi-value">42 <small>个</small></p>
              <p class="kpi-change">~ 0%</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="kpi-card-item">
            <div class="kpi-icon" style="background-color: #f6ffed;"><el-icon color="#52c41a"><Aim /></el-icon></div>
            <div class="kpi-text">
              <p class="kpi-label">达成度达标率</p>
              <p class="kpi-value">87.6<small>%</small></p>
              <p class="kpi-change increase">+ 5.1%</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>毕业要求指标点达成度柱状图</template>
          <div ref="barChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>毕业要求达成度雷达图</template>
          <div ref="radarChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>全院课程成绩分布分析</template>
          <div ref="pieChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>各考核环节平均得分率</template>
          <div ref="horizontalBarChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import * as echarts from 'echarts';
import { Refresh, Menu, Collection, Trophy, Aim } from '@element-plus/icons-vue';

const semester = ref('1');
const barChart = ref(null);
const radarChart = ref(null);
const pieChart = ref(null);
const horizontalBarChart = ref(null);

const refreshData = () => {
  // 重新获取数据并刷新图表
};

onMounted(() => {
  // Bar Chart
  const barChartInstance = echarts.init(barChart.value);
  barChartInstance.setOption({
    xAxis: {
      type: 'category',
      data: ['1.1', '1.2', '1.3', '2.1', '2.2', '3.1', '3.2', '4.1', '4.2', '5.1', '5.2', '6.1']
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [{
      data: [78, 82, 69, 88, 52, 75, 81, 62, 58, 78, 48, 72],
      type: 'bar',
      itemStyle: {
        color: (params) => params.value < 60 ? '#f56c6c' : '#409eff'
      }
    }],
    tooltip: { trigger: 'axis' },
    grid: { top: '10%', left: '3%', right: '4%', bottom: '3%', containLabel: true }
  });

  // Radar Chart
  const radarChartInstance = echarts.init(radarChart.value);
  radarChartInstance.setOption({
    radar: {
      indicator: [
        { name: '工程知识', max: 100 }, { name: '问题分析', max: 100 }, { name: '设计/开发解决方案', max: 100 },
        { name: '研究', max: 100 }, { name: '使用现代工具', max: 100 }, { name: '个人和团队', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [
        { value: [85, 90, 80, 70, 88, 95], name: '当前值' },
        { value: [70, 70, 70, 70, 70, 70], name: '目标值' }
      ]
    }],
    tooltip: { trigger: 'item' }
  });

  // Pie Chart
  const pieChartInstance = echarts.init(pieChart.value);
  pieChartInstance.setOption({
    xAxis: {
        type: 'category',
        data: ['0-59', '60-69', '70-79', '80-89', '90-100']
    },
    yAxis: {
        type: 'value'
    },
    series: [{
        data: [12, 45, 156, 210, 85],
        type: 'bar'
    }],
    tooltip: { trigger: 'axis' }
  });

  // Horizontal Bar Chart
  const horizontalBarChartInstance = echarts.init(horizontalBarChart.value);
  horizontalBarChartInstance.setOption({
    yAxis: {
      type: 'category',
      data: ['平时作业', '期中考试', '实验报告', '随堂表现', '期末考试']
    },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      data: [92, 88, 85, 76, 72],
      type: 'bar',
      barWidth: '20px',
      itemStyle: { color: '#f9a825' }
    }],
    tooltip: { trigger: 'axis' },
    grid: { top: '10%', left: '3%', right: '4%', bottom: '3%', containLabel: true }
  });
});
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #e6f7ff;
}
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.dashboard-header h2 {
  margin: 0;
}
.dashboard-header p {
  color: #909399;
  margin: 5px 0 0;
}
.kpi-cards {
  margin-bottom: 20px;
}
.kpi-cards .el-card {
  background-color: white;
  border: none;
}
.kpi-card-item {
  display: flex;
  align-items: center;
}
.kpi-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  margin-right: 20px;
}
.kpi-label {
  color: #909399;
  margin: 0;
}
.kpi-value {
  font-size: 24px;
  font-weight: bold;
  margin: 5px 0;
}
.kpi-change {
  margin: 0;
  font-size: 14px;
}
.kpi-change.increase {
  color: #67c23a;
}
.kpi-change.decrease {
  color: #f56c6c;
}
.el-card {
  background-color: white;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

<template>
  <div class="curriculum-view-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>人才培养方案</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
            v-model="searchName"
            placeholder="搜索专业名称"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch" style="margin-left: 12px">搜索</el-button>
      </div>

      <!-- 专业列表（仅已发布） -->
      <el-table :data="programList" border style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="majorName" label="专业名称" min-width="250" />
        <el-table-column prop="version" label="版本号" width="120">
          <template #default="scope">
            <el-tag>v{{ scope.row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'published' ? 'success' : 'warning'">
              {{ scope.row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button
                link
                type="primary"
                @click="goToDetail(scope.row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadPrograms"
            @current-change="loadPrograms"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

defineOptions({
  name: 'CurriculumView'
})

const router = useRouter()

// ============ 列表数据 ============
const programList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchName = ref('')

// ============ 加载专业列表（仅已发布） ============
const loadPrograms = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: page.value,
      pageSize: pageSize.value,
      status: 'published'  // 教师和学生只看已发布的方案
    }
    if (searchName.value) params.majorNameFuzzy = searchName.value
    const res = await request.get('/admin/program/list', { params })
    programList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载专业列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadPrograms()
}

// ============ 跳转到只读详情页 ============
const goToDetail = (row) => {
  router.push({
    path: `/app/curriculum/view/${row.id}`
  })
}

onMounted(() => {
  loadPrograms()
})
</script>

<style scoped>
.curriculum-view-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}
.search-bar {
  display: flex;
  align-items: center;
  padding: 16px 0;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

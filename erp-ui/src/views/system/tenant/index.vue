<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-header">
        <el-button v-hasPermi="['system:tenant:add']" type="primary" icon="Plus" @click="handleAdd">新增租户</el-button>
        <el-input
          v-model="queryParams.name"
          placeholder="请输入租户名称"
          clearable
          style="width: 200px; margin-left: 10px"
          @keyup.enter="getList"
        />
        <el-button type="primary" icon="Search" style="margin-left: 10px" @click="getList">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="tenantList" border style="width: 100%; margin-top: 20px">
        <el-table-column label="租户ID" align="center" prop="tenantId" />
        <el-table-column label="租户名称" align="center" prop="name" />
        <el-table-column label="联系人" align="center" prop="contactUser" />
        <el-table-column label="联系电话" align="center" prop="contactPhone" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" />
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button v-hasPermi="['system:tenant:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button v-hasPermi="['system:tenant:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改租户对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="tenantRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="租户编号" prop="tenantId">
          <el-input v-model="form.tenantId" placeholder="请输入租户编号" :disabled="form.id !== undefined" />
        </el-form-item>
        <el-form-item label="租户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactUser">
          <el-input v-model="form.contactUser" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">取 消</el-button>
          <el-button v-hasPermi="['system:tenant:add', 'system:tenant:edit']" type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listTenant, getTenant, addTenant, updateTenant, delTenant } from '@/api/system/tenant'
import { useDict } from '@/utils/dict'
import DictTag from '@/components/DictTag/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { sys_normal_disable } = useDict('sys_normal_disable')

const loading = ref(true)
const tenantList = ref([])
const open = ref(false)
const title = ref('')
const tenantRef = ref()

const queryParams = reactive({
  name: undefined
})

const form = ref({
  id: undefined,
  tenantId: undefined,
  name: undefined,
  contactUser: undefined,
  contactPhone: undefined,
  status: '0',
  remark: undefined
})

const rules = {
  tenantId: [{ required: true, message: '租户编号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '租户名称不能为空', trigger: 'blur' }]
}

/** 查询租户列表 */
async function getList() {
  loading.value = true
  try {
    const response: any = await listTenant()
    tenantList.value = response.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

/** 重置表单 */
function reset() {
  form.value = {
    id: undefined,
    tenantId: undefined,
    name: undefined,
    contactUser: undefined,
    contactPhone: undefined,
    status: '0',
    remark: undefined
  }
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加租户'
}

/** 修改按钮操作 */
async function handleUpdate(row: any) {
  reset()
  const response: any = await getTenant(row.id)
  form.value = response.data
  open.value = true
  title.value = '修改租户'
}

/** 提交按钮 */
async function submitForm() {
  try {
    await tenantRef.value.validate()
    if (form.value.id !== undefined) {
      await updateTenant(form.value)
      ElMessage.success('修改成功')
    } else {
      await addTenant(form.value)
      ElMessage.success('新增成功')
    }
    open.value = false
    getList()
  } catch (error) {
    console.warn('表单提交中断:', error)
  }
}

/** 删除按钮操作 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除租户名称为"' + row.name + '"的数据项？', '提示', {
    type: 'warning'
  }).then(async () => {
    await delTenant(row.id)
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.table-header {
  display: flex;
  align-items: center;
}
</style>

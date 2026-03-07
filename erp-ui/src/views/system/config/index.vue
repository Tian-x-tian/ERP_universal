<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>参数配置管理</span>
          <el-button type="primary" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-table :data="configList" v-loading="loading">
        <el-table-column label="参数名称" prop="config_name" />
        <el-table-column label="参数键名" prop="config_key" />
        <el-table-column label="参数键值" prop="config_value" />
        <el-table-column label="系统内置" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.config_type === 'Y' ? 'warning' : 'info'">
              {{ scope.row.config_type === 'Y' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="160">
          <template #default="scope">
            <el-button link type="primary" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="参数名称" prop="config_name">
          <el-input v-model="form.config_name" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="config_key">
          <el-input v-model="form.config_key" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数键值" prop="config_value">
          <el-input v-model="form.config_value" placeholder="请输入参数键值" />
        </el-form-item>
        <el-form-item label="系统内置" prop="config_type">
          <el-radio-group v-model="form.config_type">
            <el-radio label="Y">是</el-radio>
            <el-radio label="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listConfig, getConfig, addConfig, updateConfig, delConfig } from '@/api/system/config'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(true)
const configList = ref([])
const open = ref(false)
const title = ref("")
const formRef = ref()

const form = reactive({
  config_id: undefined,
  config_name: "",
  config_key: "",
  config_value: "",
  config_type: "N",
  remark: ""
})

const rules = {
  config_name: [{ required: true, message: "参数名称不能为空", trigger: "blur" }],
  config_key: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
  config_value: [{ required: true, message: "参数键值不能为空", trigger: "blur" }]
}

const getList = () => {
  loading.value = true
  listConfig().then(response => {
    configList.value = response.data
    loading.value = false
  })
}

const handleAdd = () => {
  Object.assign(form, {
    config_id: undefined,
    config_name: "",
    config_key: "",
    config_value: "",
    config_type: "N",
    remark: ""
  })
  title.value = "添加参数配置"
  open.value = true
}

const handleUpdate = (row: any) => {
  getConfig(row.config_id).then(response => {
    Object.assign(form, response.data)
    title.value = "修改参数配置"
    open.value = true
  })
}

const submitForm = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.config_id !== undefined) {
        updateConfig(form).then(() => {
          ElMessage.success("修改成功")
          open.value = false
          getList()
        })
      } else {
        addConfig(form).then(() => {
          ElMessage.success("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('是否确认删除参数名称为"' + row.config_name + '"的数据项？', '提示', {
    type: 'warning'
  }).then(() => {
    return delConfig(row.config_id)
  }).then(() => {
    getList()
    ElMessage.success("删除成功")
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>字典类型管理</span>
          <el-button v-hasPermi="['system:dict:add']" type="primary" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <el-table :data="typeList" v-loading="loading">
        <el-table-column label="字典名称" prop="dict_name" />
        <el-table-column label="字典类型" prop="dict_type" />
        <el-table-column label="状态" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="240">
          <template #default="scope">
            <el-button v-hasPermi="['system:dict:query']" link type="primary" @click="handleData(scope.row)">数据</el-button>
            <el-button v-hasPermi="['system:dict:edit']" link type="primary" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button v-hasPermi="['system:dict:remove']" link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改字典类型对话框 -->
    <el-dialog :title="title" v-model="open" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="字典名称" prop="dict_name">
          <el-input v-model="form.dict_name" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dict_type">
          <el-input v-model="form.dict_type" placeholder="请输入字典类型" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取 消</el-button>
        <el-button v-hasPermi="['system:dict:add', 'system:dict:edit']" type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据列表抽屉 -->
    <el-drawer title="字典数据列表" v-model="dataOpen" size="50%">
      <div class="drawer-header">
        <el-button v-hasPermi="['system:dict:add']" type="primary" size="small" @click="handleDataAdd">新增数据</el-button>
      </div>
      <el-table :data="dataList" v-loading="dataLoading">
        <el-table-column label="标签" prop="dict_label" />
        <el-table-column label="键值" prop="dict_value" />
        <el-table-column label="排序" prop="dict_sort" width="80" />
        <el-table-column label="操作" align="center" width="160">
          <template #default="scope">
            <el-button v-hasPermi="['system:dict:edit']" link type="primary" @click="handleDataUpdate(scope.row)">修改</el-button>
            <el-button v-hasPermi="['system:dict:remove']" link type="danger" @click="handleDataDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 添加或修改字典数据对话框 -->
    <el-dialog :title="dataTitle" v-model="dataFormOpen" width="500px" append-to-body>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="80px">
        <el-form-item label="字典标签" prop="dict_label">
          <el-input v-model="dataForm.dict_label" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dict_value">
          <el-input v-model="dataForm.dict_value" placeholder="请输入字典键值" />
        </el-form-item>
        <el-form-item label="字典排序" prop="dict_sort">
          <el-input-number v-model="dataForm.dict_sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dataForm.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dataForm.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataFormOpen = false">取 消</el-button>
        <el-button v-hasPermi="['system:dict:add', 'system:dict:edit']" type="primary" @click="submitDataForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { 
  listType, getType, addType, updateType, delType,
  getDicts, getData, addData, updateData, delData 
} from '@/api/system/dict'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(true)
const typeList = ref([])
const open = ref(false)
const title = ref("")
const formRef = ref()

// 字典数据相关
const dataOpen = ref(false)
const dataLoading = ref(false)
const dataList = ref([])
const dataFormOpen = ref(false)
const dataTitle = ref("")
const dataFormRef = ref()
const currentDictType = ref("")

const form = reactive({
  dict_id: undefined,
  dict_name: "",
  dict_type: "",
  status: "0",
  remark: ""
})

const dataForm = reactive({
  dict_code: undefined,
  dict_label: "",
  dict_value: "",
  dict_sort: 0,
  dict_type: "",
  status: "0",
  remark: ""
})

const rules = {
  dict_name: [{ required: true, message: "字典名称不能为空", trigger: "blur" }],
  dict_type: [{ required: true, message: "字典类型不能为空", trigger: "blur" }]
}

const dataRules = {
  dict_label: [{ required: true, message: "字典标签不能为空", trigger: "blur" }],
  dict_value: [{ required: true, message: "字典键值不能为空", trigger: "blur" }]
}

const getList = () => {
  loading.value = true
  listType().then(response => {
    typeList.value = response.data
    loading.value = false
  })
}

const handleAdd = () => {
  Object.assign(form, {
    dict_id: undefined,
    dict_name: "",
    dict_type: "",
    status: "0",
    remark: ""
  })
  title.value = "添加字典类型"
  open.value = true
}

const handleUpdate = (row: any) => {
  getType(row.dict_id).then(response => {
    Object.assign(form, response.data)
    title.value = "修改字典类型"
    open.value = true
  })
}

const handleData = (row: any) => {
  currentDictType.value = row.dict_type
  getDataList()
  dataOpen.value = true
}

const getDataList = () => {
  dataLoading.value = true
  getDicts(currentDictType.value).then(response => {
    dataList.value = response.data
    dataLoading.value = false
  })
}

const handleDataAdd = () => {
  Object.assign(dataForm, {
    dict_code: undefined,
    dict_label: "",
    dict_value: "",
    dict_sort: 0,
    dict_type: currentDictType.value,
    status: "0",
    remark: ""
  })
  dataTitle.value = "添加字典数据"
  dataFormOpen.value = true
}

const handleDataUpdate = (row: any) => {
  getData(row.dict_code).then(response => {
    Object.assign(dataForm, response.data)
    dataTitle.value = "修改字典数据"
    dataFormOpen.value = true
  })
}

const submitForm = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid) {
      if (form.dict_id !== undefined) {
        updateType(form).then(() => {
          ElMessage.success("修改成功")
          open.value = false
          getList()
        })
      } else {
        addType(form).then(() => {
          ElMessage.success("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

const submitDataForm = () => {
  dataFormRef.value.validate((valid: boolean) => {
    if (valid) {
      if (dataForm.dict_code !== undefined) {
        updateData(dataForm).then(() => {
          ElMessage.success("修改成功")
          dataFormOpen.value = false
          getDataList()
        })
      } else {
        addData(dataForm).then(() => {
          ElMessage.success("新增成功")
          dataFormOpen.value = false
          getDataList()
        })
      }
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('是否确认删除字典名称为"' + row.dict_name + '"的数据项？', '提示', {
    type: 'warning'
  }).then(() => {
    return delType(row.dict_id)
  }).then(() => {
    getList()
    ElMessage.success("删除成功")
  })
}

const handleDataDelete = (row: any) => {
  ElMessageBox.confirm('是否确认删除字典标签为"' + row.dict_label + '"的数据项？', '提示', {
    type: 'warning'
  }).then(() => {
    return delData(row.dict_code)
  }).then(() => {
    getDataList()
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
.drawer-header {
  padding: 0 20px 20px;
}
</style>

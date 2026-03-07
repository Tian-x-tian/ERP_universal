<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-header">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名"
          clearable
          style="width: 200px; margin-left: 10px"
          @keyup.enter="getList"
        />
        <el-button type="primary" icon="Search" style="margin-left: 10px" @click="getList">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="userList" border style="width: 100%; margin-top: 20px">
        <el-table-column label="用户账号" align="center" prop="userName" />
        <el-table-column label="用户昵称" align="center" prop="nickName" />
        <el-table-column label="租户编号" align="center" prop="tenantId" />
        <el-table-column label="手机号码" align="center" prop="phonenumber" />
        <el-table-column label="性别" align="center" prop="sex">
          <template #default="scope">
            <dict-tag :options="sys_user_sex" :value="scope.row.sex" />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" />
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改用户对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="userRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属租户" prop="tenantId">
              <el-input v-model="form.tenantId" placeholder="请输入租户编号" :disabled="form.userId !== undefined" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户账号" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户账号" :disabled="form.userId !== undefined" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password" v-if="form.userId === undefined">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择">
                <el-option
                  v-for="dict in sys_user_sex"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listUser, getUser, addUser, updateUser, delUser } from '@/api/system/user'
import { useDict } from '@/utils/dict'
import DictTag from '@/components/DictTag/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { sys_user_sex, sys_normal_disable } = useDict('sys_user_sex', 'sys_normal_disable')

const loading = ref(true)
const userList = ref([])
const open = ref(false)
const title = ref('')
const userRef = ref()

const queryParams = reactive({
  userName: undefined
})

const form = ref({
  userId: undefined,
  tenantId: undefined,
  userName: undefined,
  nickName: undefined,
  password: undefined,
  phonenumber: undefined,
  email: undefined,
  sex: '0',
  status: '0',
  remark: undefined
})

const rules = {
  userName: [{ required: true, message: '用户账号不能为空', trigger: 'blur' }],
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '用户密码不能为空', trigger: 'blur' }],
  tenantId: [{ required: true, message: '租户编号不能为空', trigger: 'blur' }]
}

/** 查询用户列表 */
async function getList() {
  loading.value = true
  try {
    const response: any = await listUser()
    userList.value = response.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

/** 重置表单 */
function reset() {
  form.value = {
    userId: undefined,
    tenantId: undefined,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    sex: '0',
    status: '0',
    remark: undefined
  }
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加用户'
}

/** 修改按钮操作 */
async function handleUpdate(row: any) {
  reset()
  const response: any = await getUser(row.userId)
  form.value = response.data
  open.value = true
  title.value = '修改用户'
}

/** 提交按钮 */
async function submitForm() {
  await userRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (form.value.userId !== undefined) {
        await updateUser(form.value)
        ElMessage.success('修改成功')
      } else {
        await addUser(form.value)
        ElMessage.success('新增成功')
      }
      open.value = false
      getList()
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除用户账号为"' + row.userName + '"的数据项？', '提示', {
    type: 'warning'
  }).then(async () => {
    await delUser(row.userId)
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

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-header">
        <el-button v-hasPermi="['system:user:add']" type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
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
        <el-table-column label="所属部门" align="center" min-width="140">
          <template #default="scope">
            {{ deptNameMap[scope.row.deptId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="租户编号" align="center" prop="tenantId" width="120" />
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
            <el-button v-hasPermi="['system:user:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button v-hasPermi="['system:user:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="userRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属租户" prop="tenantId">
              <el-select
                v-model="form.tenantId"
                placeholder="请选择所属租户"
                filterable
                style="width: 100%;"
                :disabled="form.userId !== undefined"
              >
                <el-option
                  v-for="item in tenantOptions"
                  :key="item.tenantId"
                  :label="item.optionLabel"
                  :value="item.tenantId"
                />
              </el-select>
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
            <el-form-item label="所属部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :props="{ value: 'deptId', label: 'deptName', children: 'children' }"
                value-key="deptId"
                placeholder="请选择所属部门"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple collapse-tags collapse-tags-tooltip placeholder="请选择角色" style="width: 100%;">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="岗位" prop="postIds">
              <el-select v-model="form.postIds" multiple collapse-tags collapse-tags-tooltip placeholder="请选择岗位" style="width: 100%;">
                <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" />
              </el-select>
            </el-form-item>
          </el-col>
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
          <el-button v-hasPermi="['system:user:add', 'system:user:edit']" type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { addUser, delUser, getUser, listUser, updateUser } from '@/api/system/user'
import { listRole } from '@/api/system/role'
import { listPost } from '@/api/system/post'
import { deptTree } from '@/api/system/dept'
import { listTenant } from '@/api/system/tenant'
import { useDict } from '@/utils/dict'
import DictTag from '@/components/DictTag/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { sys_user_sex, sys_normal_disable } = useDict('sys_user_sex', 'sys_normal_disable')

const loading = ref(true)
const userList = ref<any[]>([])
const open = ref(false)
const title = ref('')
const userRef = ref()

const roleOptions = ref<any[]>([])
const postOptions = ref<any[]>([])
const deptOptions = ref<any[]>([])
const tenantOptions = ref<any[]>([])

const queryParams = reactive({
  userName: undefined
})

const form = ref<any>({
  userId: undefined,
  tenantId: localStorage.getItem('tenantId') || '000000',
  deptId: undefined,
  roleIds: [],
  postIds: [],
  userName: undefined,
  nickName: undefined,
  password: undefined,
  phonenumber: undefined,
  email: undefined,
  sex: '0',
  status: '0',
  remark: undefined
})

const deptNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  const walk = (nodes: any[]) => {
    nodes.forEach((node) => {
      map[String(node.deptId)] = node.deptName
      if (Array.isArray(node.children) && node.children.length > 0) {
        walk(node.children)
      }
    })
  }
  walk(deptOptions.value)
  return map
})

const rules = {
  userName: [{ required: true, message: '用户账号不能为空', trigger: 'blur' }],
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '用户密码不能为空', trigger: 'blur' }],
  tenantId: [{ required: true, message: '所属租户不能为空', trigger: 'change' }]
}

/**
 * 获取默认租户编号。
 */
function getDefaultTenantId() {
  return localStorage.getItem('tenantId') || '000000'
}

/**
 * 查询用户列表。
 */
async function getList() {
  loading.value = true
  try {
    const response: any = await listUser()
    userList.value = Array.isArray(response.data) ? response.data : []
  } finally {
    loading.value = false
  }
}

/**
 * 构建租户下拉显示名称。
 * @param tenant 租户对象
 * @returns 租户显示名称
 */
function getTenantOptionLabel(tenant: any) {
  const tenantId = String(tenant?.tenantId || '').trim()
  const tenantName = String(tenant?.name || '').trim()
  if (tenantId && tenantName) {
    return `${tenantId} - ${tenantName}`
  }
  return tenantId || tenantName || '-'
}

/**
 * 加载租户下拉选项。
 */
async function loadTenantOptions() {
  const tenantRes: any = await listTenant()
  tenantOptions.value = Array.isArray(tenantRes.data)
    ? tenantRes.data
      .filter((item: any) => String(item?.tenantId || '').trim().length > 0)
      .map((item: any) => ({
        ...item,
        tenantId: String(item.tenantId).trim(),
        optionLabel: getTenantOptionLabel(item)
      }))
    : []
}

/**
 * 加载角色、岗位、部门选项。
 */
async function loadOptions() {
  const [roleRes, postRes, deptRes]: any[] = await Promise.all([listRole(), listPost(), deptTree(), loadTenantOptions()])
  roleOptions.value = Array.isArray(roleRes.data) ? roleRes.data : []
  postOptions.value = Array.isArray(postRes.data) ? postRes.data : []
  deptOptions.value = Array.isArray(deptRes.data) ? deptRes.data : []
}

/**
 * 重置表单。
 */
function reset() {
  form.value = {
    userId: undefined,
    tenantId: getDefaultTenantId(),
    deptId: undefined,
    roleIds: [],
    postIds: [],
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

/**
 * 新增用户。
 */
async function handleAdd() {
  reset()
  await loadTenantOptions()
  open.value = true
  title.value = '添加用户'
}

/**
 * 修改用户。
 * @param row 行数据
 */
async function handleUpdate(row: any) {
  reset()
  await loadTenantOptions()
  const response: any = await getUser(row.userId)
  form.value = {
    ...response.data,
    tenantId: String(response.data?.tenantId || getDefaultTenantId()).trim(),
    roleIds: Array.isArray(response.data?.roleIds) ? response.data.roleIds : [],
    postIds: Array.isArray(response.data?.postIds) ? response.data.postIds : []
  }
  open.value = true
  title.value = '修改用户'
}

/**
 * 提交用户表单。
 */
async function submitForm() {
  await userRef.value.validate(async (valid: boolean) => {
    if (!valid) {
      return
    }
    const payload = {
      ...form.value,
      tenantId: String(form.value.tenantId || getDefaultTenantId()).trim(),
      userName: form.value.userName?.trim(),
      nickName: form.value.nickName?.trim(),
      password: form.value.password?.trim(),
      roleIds: Array.isArray(form.value.roleIds) ? form.value.roleIds : [],
      postIds: Array.isArray(form.value.postIds) ? form.value.postIds : []
    }
    if (form.value.userId !== undefined) {
      await updateUser(payload)
      ElMessage.success('修改成功')
    } else {
      await addUser(payload)
      ElMessage.success('新增成功')
    }
    open.value = false
    getList()
  })
}

/**
 * 删除用户。
 * @param row 行数据
 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除用户账号为"' + row.userName + '"的数据项？', '提示', {
    type: 'warning'
  }).then(async () => {
    await delUser(row.userId)
    await getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(async () => {
  await Promise.all([loadOptions(), getList()])
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

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-header">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增角色</el-button>
        <el-input
          v-model="queryParams.roleName"
          placeholder="请输入角色名称"
          clearable
          style="width: 200px; margin-left: 10px"
          @keyup.enter="getList"
        />
        <el-button type="primary" icon="Search" style="margin-left: 10px" @click="getList">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="roleList" border style="width: 100%; margin-top: 20px">
        <el-table-column label="角色名称" align="center" prop="roleName" />
        <el-table-column label="权限字符" align="center" prop="roleKey" />
        <el-table-column label="显示顺序" align="center" prop="roleSort" width="100" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" />
        <el-table-column label="操作" align="center" width="220">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button link type="primary" icon="Checked" @click="handleDataScope(scope.row)">分配权限</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改角色对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="roleRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
        </el-form-item>
        <el-form-item label="角色顺序" prop="roleSort">
          <el-input-number v-model="form.roleSort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">展开/折叠</el-checkbox>
          <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">全选/全不选</el-checkbox>
          <el-tree
            class="tree-border"
            :data="menuOptions"
            show-checkbox
            ref="menuRef"
            node-key="menuId"
            :props="defaultProps"
            empty-text="加载中，请稍候"
          ></el-tree>
        </el-form-item>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { listRole, getRole, addRole, updateRole, delRole } from '@/api/system/role'
import { listMenu } from '@/api/system/menu'
import { useDict } from '@/utils/dict'
import DictTag from '@/components/DictTag/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { sys_normal_disable } = useDict('sys_normal_disable')

const loading = ref(true)
const roleList = ref<any[]>([])
const menuOptions = ref<any[]>([])
const open = ref(false)
const title = ref('')
const roleRef = ref()
const menuRef = ref()

const menuExpand = ref(false)
const menuNodeAll = ref(false)

const queryParams = reactive({
  roleName: undefined
})

const form = ref({
  roleId: undefined,
  roleName: undefined,
  roleKey: undefined,
  roleSort: 0,
  status: '0',
  menuIds: [],
  remark: undefined
})

const rules = {
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleKey: [{ required: true, message: '权限字符不能为空', trigger: 'blur' }],
  roleSort: [{ required: true, message: '角色顺序不能为空', trigger: 'blur' }]
}

const defaultProps = {
  children: 'children',
  label: 'menuName'
}

/** 查询角色列表 */
async function getList() {
  loading.value = true
  try {
    const response: any = await listRole()
    roleList.value = response.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

/** 查询菜单树结构 */
async function getMenuTree() {
  const response: any = await listMenu()
  menuOptions.value = handleTree(response.data, 'menuId')
}

/** 树处理函数 (简化版) */
function handleTree(data: any[], id: string) {
  const config = {
    id: id || 'id',
    parentId: 'parentId',
    childrenList: 'children'
  }
  const childrenListMap: any = {}
  const nodeIds: any = {}
  const tree: any[] = []

  for (const d of data) {
    const parentId = d[config.parentId]
    if (childrenListMap[parentId] == null) {
      childrenListMap[parentId] = []
    }
    nodeIds[d[config.id]] = d
    childrenListMap[parentId].push(d)
  }

  for (const d of data) {
    const parentId = d[config.parentId]
    if (nodeIds[parentId] == null) {
      tree.push(d)
    }
  }

  for (const t of tree) {
    adaptToChildrenList(t)
  }

  function adaptToChildrenList(o: any) {
    if (childrenListMap[o[config.id]] !== null) {
      o[config.childrenList] = childrenListMap[o[config.id]]
    }
    if (o[config.childrenList]) {
      for (const c of o[config.childrenList]) {
        adaptToChildrenList(c)
      }
    }
  }
  return tree
}

/** 重置表单 */
function reset() {
  if (menuRef.value) {
    menuRef.value.setCheckedKeys([])
  }
  form.value = {
    roleId: undefined,
    roleName: undefined,
    roleKey: undefined,
    roleSort: 0,
    status: '0',
    menuIds: [],
    remark: undefined
  }
}

/** 新增按钮操作 */
async function handleAdd() {
  reset()
  await getMenuTree()
  open.value = true
  title.value = '添加角色'
}

/** 修改按钮操作 */
async function handleUpdate(row: any) {
  reset()
  await getMenuTree()
  const response: any = await getRole(row.roleId)
  form.value = response.data
  open.value = true
  title.value = '修改角色'
  nextTick(() => {
    // 假设后端返回了绑定的 menuIds
    if (form.value.menuIds) {
      form.value.menuIds.forEach((v) => {
        menuRef.value.setChecked(v, true, false)
      })
    }
  })
}

/** 提交按钮 */
async function submitForm() {
  await roleRef.value.validate(async (valid: boolean) => {
    if (valid) {
      form.value.menuIds = menuRef.value.getCheckedKeys()
      if (form.value.roleId !== undefined) {
        await updateRole(form.value)
        ElMessage.success('修改成功')
      } else {
        await addRole(form.value)
        ElMessage.success('新增成功')
      }
      open.value = false
      getList()
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除角色名称为"' + row.roleName + '"的数据项？', '提示', {
    type: 'warning'
  }).then(async () => {
    await delRole(row.roleId)
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 分配数据权限操作 */
function handleDataScope(row: any) {
  handleUpdate(row)
  title.value = '分配数据权限'
}

/** 树展开/折叠 */
function handleCheckedTreeExpand(value: any, type: string) {
  if (type === 'menu') {
    const nodesMap = menuRef.value.store.nodesMap
    for (const key in nodesMap) {
      nodesMap[key].expanded = value
    }
  }
}

/** 树全选/全不选 */
function handleCheckedTreeNodeAll(value: any, type: string) {
  if (type === 'menu') {
    menuRef.value.setCheckedNodes(value ? menuOptions.value : [])
  }
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
.tree-border {
  margin-top: 5px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>

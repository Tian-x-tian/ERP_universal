<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-header">
        <el-button v-hasPermi="['system:role:add']" type="primary" icon="Plus" @click="handleAdd">新增角色</el-button>
        <el-input
          v-model="queryParams.roleName"
          placeholder="请输入角色名称"
          clearable
          style="width: 200px; margin-left: 10px"
          @keyup.enter="getList"
        />
        <el-button type="primary" icon="Search" style="margin-left: 10px" @click="getList">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="filteredRoleList" border style="width: 100%; margin-top: 20px">
        <el-table-column label="角色名称" align="center" prop="roleName" />
        <el-table-column label="权限字符" align="center" prop="roleKey" />
        <el-table-column label="数据权限" align="center" min-width="180">
          <template #default="scope">
            <el-tag type="info">{{ resolveDataScopeDisplay(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="显示顺序" align="center" prop="roleSort" width="100" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" />
        <el-table-column label="操作" align="center" width="240">
          <template #default="scope">
            <el-button v-hasPermi="['system:role:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button v-hasPermi="['system:role:edit']" link type="primary" icon="Checked" @click="handleDataScope(scope.row)">数据权限</el-button>
            <el-button v-hasPermi="['system:role:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="roleRef" :model="form" :rules="rules" label-width="90px">
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
          <div class="menu-permission-entry">
            <el-badge :value="selectedMenuCount" :hidden="selectedMenuCount === 0">
              <el-button class="menu-permission-btn" type="primary" icon="Setting" @click="openMenuPermissionDialog">
                {{ selectedMenuCount > 0 ? '已配置菜单权限' : '配置菜单权限' }}
              </el-button>
            </el-badge>
            <el-button v-if="selectedMenuCount > 0" link type="danger" @click="clearMenuPermission">清空</el-button>
            <span class="menu-permission-tip">
              {{ isNormalRole ? '普通角色默认包含“消息待办中心”，且不可取消' : (selectedMenuCount > 0 ? '可继续调整权限范围' : '点击配置当前角色可访问菜单') }}
            </span>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">取 消</el-button>
          <el-button v-hasPermi="['system:role:add', 'system:role:edit']" type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="菜单权限配置" v-model="menuDialogOpen" width="560px" append-to-body>
      <div class="menu-tree-toolbar">
        <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">展开/折叠</el-checkbox>
        <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">全选/全不选</el-checkbox>
      </div>
      <el-tree
        ref="menuRef"
        class="tree-border permission-tree"
        :data="menuOptions"
        show-checkbox
        node-key="menuId"
        :props="defaultProps"
        empty-text="加载中，请稍候"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="menuDialogOpen = false">取 消</el-button>
          <el-button type="primary" @click="confirmMenuPermission">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="分配数据权限" v-model="dataScopeOpen" width="560px" append-to-body>
      <el-form ref="dataScopeRef" :model="dataScopeForm" label-width="110px">
        <el-form-item label="角色名称">
          <el-input v-model="dataScopeForm.roleName" disabled />
        </el-form-item>
        <el-form-item label="数据权限范围">
          <el-select v-model="dataScopeForm.dataScope" style="width: 100%;">
            <el-option label="全部数据权限" value="1" />
            <el-option label="自定数据权限" value="2" />
            <el-option label="本部门数据权限" value="3" />
            <el-option label="本部门及以下" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dataScopeForm.dataScope === '2'" label="自定义部门">
          <div class="menu-permission-entry">
            <el-badge :value="selectedDeptCount" :hidden="selectedDeptCount === 0">
              <el-button class="menu-permission-btn" type="primary" icon="Setting" @click="openDeptPermissionDialog">
                {{ selectedDeptCount > 0 ? '已配置自定义部门' : '配置自定义部门' }}
              </el-button>
            </el-badge>
            <el-button v-if="selectedDeptCount > 0" link type="danger" @click="clearDeptPermission">清空</el-button>
            <span class="menu-permission-tip">
              {{ selectedDeptCount > 0 ? '可继续调整自定义部门范围' : '点击配置当前角色可访问部门' }}
            </span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dataScopeOpen = false">取 消</el-button>
          <el-button v-hasPermi="['system:role:edit']" type="primary" @click="submitDataScope">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="自定义部门配置" v-model="deptDialogOpen" width="560px" append-to-body>
      <div class="menu-tree-toolbar">
        <el-checkbox v-model="deptExpand" @change="handleCheckedTreeExpand($event, 'dept')">展开/折叠</el-checkbox>
        <el-checkbox v-model="deptNodeAll" @change="handleCheckedTreeNodeAll($event, 'dept')">全选/全不选</el-checkbox>
      </div>
      <el-tree
        ref="deptRef"
        class="tree-border permission-tree"
        :data="deptOptions"
        show-checkbox
        node-key="deptId"
        :props="{ children: 'children', label: 'deptName' }"
        empty-text="加载中，请稍候"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="deptDialogOpen = false">取 消</el-button>
          <el-button type="primary" @click="confirmDeptPermission">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { addRole, delRole, getRole, listRole, updateRole, updateRoleDataScope } from '@/api/system/role'
import { listMenu } from '@/api/system/menu'
import { deptTree } from '@/api/system/dept'
import { useDict } from '@/utils/dict'
import DictTag from '@/components/DictTag/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MENU_BLUEPRINT_GROUPS } from '@/constants/default-menu'

const { sys_normal_disable } = useDict('sys_normal_disable')
const TODO_CENTER_PATHS = ['/workbench/system-notice', '/workbench/process-todo']
const ADMIN_ROLE_KEY = 'admin'
const LEGACY_MENU_ROOT_PATHS = ['/system', '/platform']
const LEGACY_ROOT_NAMES = ['系统管理', '平台底座']

const loading = ref(true)
const roleList = ref<any[]>([])
const menuOptions = ref<any[]>([])
const deptOptions = ref<any[]>([])
const selectedMenuIds = ref<Array<number | string>>([])
const requiredMenuIds = ref<Array<number | string>>([])

const open = ref(false)
const menuDialogOpen = ref(false)
const dataScopeOpen = ref(false)
const deptDialogOpen = ref(false)
const title = ref('')
const roleRef = ref()
const menuRef = ref()
const deptRef = ref()

const menuExpand = ref(false)
const menuNodeAll = ref(false)
const deptExpand = ref(false)
const deptNodeAll = ref(false)
const selectedDeptIds = ref<Array<number | string>>([])

const queryParams = reactive({
  roleName: ''
})

const form = ref<any>({
  roleId: undefined,
  tenantId: localStorage.getItem('tenantId') || '000000',
  roleName: undefined,
  roleKey: undefined,
  roleSort: 0,
  status: '0',
  menuIds: [],
  remark: undefined
})

const dataScopeForm = reactive({
  roleId: undefined as number | undefined,
  roleName: '',
  dataScope: '1',
  deptIds: [] as number[]
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

/**
 * 判断是否为前端虚拟分组菜单 ID。
 * @param menuId 菜单ID
 * @returns 是否为虚拟分组节点
 */
function isVirtualMenuId(menuId: unknown): boolean {
  return typeof menuId === 'string' && menuId.startsWith('virtual_group_')
}

const filteredRoleList = computed(() => {
  const key = queryParams.roleName.trim().toLowerCase()
  if (!key) {
    return roleList.value
  }
  return roleList.value.filter((item) => String(item.roleName || '').toLowerCase().includes(key))
})
const selectedMenuCount = computed(() => selectedMenuIds.value.length)
const selectedDeptCount = computed(() => selectedDeptIds.value.length)
const isNormalRole = computed(
  () => String(form.value.roleKey || '').trim().toLowerCase() !== ADMIN_ROLE_KEY
)

/**
 * 递归遍历树节点。
 * @param nodes 树节点集合
 * @param callback 节点回调
 */
function walkNodes(nodes: any[], callback: (node: any) => void) {
  for (const node of nodes || []) {
    callback(node)
    if (Array.isArray(node?.children) && node.children.length > 0) {
      walkNodes(node.children, callback)
    }
  }
}

/**
 * 确保必选菜单始终处于勾选状态，并同步到树组件。
 * @param syncTree 是否同步树组件勾选状态
 */
function ensureRequiredMenuSelected(syncTree = true) {
  if (!requiredMenuIds.value.length) {
    return
  }
  selectedMenuIds.value = Array.from(new Set([...(selectedMenuIds.value || []), ...requiredMenuIds.value]))
  if (!syncTree || !menuRef.value) {
    return
  }
  nextTick(() => {
    if (menuRef.value) {
      menuRef.value.setCheckedKeys(selectedMenuIds.value)
    }
  })
}

/**
 * 同步“消息待办中心”在普通角色下的强制必选规则。
 * 普通角色：必选且禁用取消；管理员角色：允许自由勾选。
 */
function syncTodoCenterRequiredRule() {
  const todoIds: Array<number | string> = []
  walkNodes(menuOptions.value, (node) => {
    if (TODO_CENTER_PATHS.includes(String(node?.path || ''))) {
      todoIds.push(node.menuId)
      node.__todoLocked = true
      node.disabled = isNormalRole.value
      return
    }
    if (node?.__todoLocked) {
      node.disabled = false
      delete node.__todoLocked
    }
  })
  requiredMenuIds.value = isNormalRole.value ? todoIds : []
  ensureRequiredMenuSelected(false)
}

/**
 * 深拷贝菜单树，避免重排时污染原始对象引用。
 * @param source 原始菜单树
 * @returns 深拷贝后的菜单树
 */
function cloneMenuTree(source: any[]): any[] {
  if (!Array.isArray(source)) {
    return []
  }
  return source.map((item) => ({
    ...item,
    children: Array.isArray(item.children) ? cloneMenuTree(item.children) : []
  }))
}

/**
 * 标准化菜单路径。
 * @param path 原始路径
 * @returns 标准化路径
 */
function normalizeMenuPath(path: unknown) {
  return String(path || '').trim().toLowerCase().replace(/\/+$/, '')
}

/**
 * 判断是否为历史遗留分组节点（系统管理/平台底座）。
 * @param node 菜单节点
 * @returns 是否历史分组节点
 */
function isLegacyRootNode(node: any) {
  const path = normalizeMenuPath(node?.path)
  const name = String(node?.menuName || '').trim()
  return LEGACY_MENU_ROOT_PATHS.includes(path) || LEGACY_ROOT_NAMES.includes(name)
}

/**
 * 过滤历史遗留分组及其子树，避免权限树中出现旧结构干扰。
 * @param nodes 菜单树节点
 * @returns 过滤后的菜单树
 */
function pruneLegacyMenuTreeNodes(nodes: any[]): any[] {
  const sourceNodes = Array.isArray(nodes) ? nodes : []
  const result: any[] = []
  sourceNodes.forEach((node) => {
    if (isLegacyRootNode(node)) {
      return
    }
    result.push({
      ...node,
      children: Array.isArray(node?.children) ? pruneLegacyMenuTreeNodes(node.children) : []
    })
  })
  return result
}

/**
 * 按候选路径在树中查找并拆离首个节点。
 * @param nodes 节点集合
 * @param sourcePaths 候选路径集合
 * @returns 拆离出的节点
 */
function detachNodeBySourcePaths(nodes: any[], sourcePaths: string[]): any | undefined {
  for (let index = 0; index < (nodes || []).length; index += 1) {
    const currentNode = nodes[index]
    const currentPath = String(currentNode?.path || '')
    if (sourcePaths.includes(currentPath)) {
      nodes.splice(index, 1)
      return currentNode
    }
    if (Array.isArray(currentNode?.children) && currentNode.children.length > 0) {
      const detachedNode: any = detachNodeBySourcePaths(currentNode.children, sourcePaths)
      if (detachedNode) {
        return detachedNode
      }
    }
  }
  return undefined
}

/**
 * 将菜单树重排为与左侧导航一致的分组顺序，便于权限配置定位。
 * @param treeSource 原始菜单树
 * @returns 重排后的菜单树
 */
function normalizeMenuPermissionTree(treeSource: any[]) {
  const sourceTree = pruneLegacyMenuTreeNodes(cloneMenuTree(treeSource))
  const groupedTree: any[] = []
  const homeNode = detachNodeBySourcePaths(sourceTree, ['/home', '/'])

  MENU_BLUEPRINT_GROUPS.forEach((group) => {
    const children: any[] = []
    ;(group.children || []).forEach((child) => {
      const targetNode = detachNodeBySourcePaths(sourceTree, child.sourcePaths || [])
      if (!targetNode) {
        return
      }
      targetNode.menuName = child.menuName
      targetNode.icon = targetNode.icon || child.icon
      children.push(targetNode)
    })
    if (children.length === 0) {
      return
    }
    groupedTree.push({
      menuId: `virtual_group_${group.menuId}`,
      menuName: group.menuName,
      children,
    })
  })

  const normalizedTree: any[] = []
  if (homeNode) {
    normalizedTree.push({
      ...homeNode,
      menuName: '首页',
      children: Array.isArray(homeNode.children) ? homeNode.children : []
    })
  }
  // 菜单权限弹窗仅展示标准化菜单体系，避免遗留分组（如“系统管理/平台底座”）造成认知干扰。
  return [...normalizedTree, ...groupedTree]
}

/**
 * 获取当前菜单树中可勾选节点 ID 列表。
 * 仅包含未禁用节点，避免全选操作被虚拟分组或禁用节点干扰。
 * @returns 可勾选菜单 ID 集合
 */
function collectCheckableMenuIds(): Array<number | string> {
  const ids: Array<number | string> = []
  walkNodes(menuOptions.value, (node) => {
    const menuId = node?.menuId
    if (menuId === undefined || menuId === null) {
      return
    }
    if (isVirtualMenuId(menuId)) {
      return
    }
    if (node?.disabled) {
      return
    }
    ids.push(menuId)
  })
  return ids
}

/**
 * 解析数据权限范围显示名称。
 * @param dataScope 数据权限编码
 */
function resolveDataScopeLabel(dataScope: string) {
  if (dataScope === '1') return '全部数据权限'
  if (dataScope === '2') return '自定数据权限'
  if (dataScope === '3') return '本部门数据权限'
  if (dataScope === '4') return '本部门及以下'
  return '-'
}

/**
 * 解析数据权限展示文案。
 * 自定数据权限时补充显示已关联部门数量，便于快速识别配置是否生效。
 * @param row 角色行数据
 */
function resolveDataScopeDisplay(row: any) {
  const label = resolveDataScopeLabel(row?.dataScope)
  if (row?.dataScope !== '2') {
    return label
  }
  if (!Array.isArray(row?.deptIds)) {
    return label
  }
  return `${label}(${row.deptIds.length})`
}

/**
 * 查询角色列表。
 */
async function getList() {
  loading.value = true
  try {
    const response: any = await listRole()
    roleList.value = Array.isArray(response.data) ? response.data : []
  } finally {
    loading.value = false
  }
}

/**
 * 查询菜单树结构。
 */
async function getMenuTree() {
  const response: any = await listMenu()
  const treeSource = handleTree(response.data, 'menuId')
  menuOptions.value = normalizeMenuPermissionTree(treeSource)
  syncTodoCenterRequiredRule()
}

/**
 * 查询部门树结构。
 */
async function getDeptTree() {
  const response: any = await deptTree()
  deptOptions.value = Array.isArray(response.data) ? response.data : []
}

/**
 * 树处理函数。
 * @param data 原始数据
 * @param id 主键字段名
 */
function handleTree(data: any[], id: string) {
  const config = {
    id: id || 'id',
    parentId: 'parentId',
    childrenList: 'children'
  }
  const childrenListMap: any = {}
  const nodeIds: any = {}
  const tree: any[] = []

  for (const d of data || []) {
    const parentId = d[config.parentId]
    if (childrenListMap[parentId] == null) {
      childrenListMap[parentId] = []
    }
    nodeIds[d[config.id]] = d
    childrenListMap[parentId].push(d)
  }

  for (const d of data || []) {
    const parentId = d[config.parentId]
    if (nodeIds[parentId] == null) {
      tree.push(d)
    }
  }

  for (const t of tree) {
    adaptToChildrenList(t)
  }

  function adaptToChildrenList(o: any) {
    if (childrenListMap[o[config.id]] !== undefined) {
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

/**
 * 重置角色表单。
 */
function reset() {
  selectedMenuIds.value = []
  requiredMenuIds.value = []
  menuExpand.value = false
  menuNodeAll.value = false
  if (menuRef.value) {
    menuRef.value.setCheckedKeys([])
  }
  form.value = {
    roleId: undefined,
    tenantId: localStorage.getItem('tenantId') || '000000',
    roleName: undefined,
    roleKey: undefined,
    roleSort: 0,
    status: '0',
    menuIds: [],
    remark: undefined
  }
}

/**
 * 重置数据权限表单。
 */
function resetDataScope() {
  dataScopeForm.roleId = undefined
  dataScopeForm.roleName = ''
  dataScopeForm.dataScope = '1'
  dataScopeForm.deptIds = []
  selectedDeptIds.value = []
  deptExpand.value = false
  deptNodeAll.value = false
  deptDialogOpen.value = false
  if (deptRef.value) {
    deptRef.value.setCheckedKeys([])
  }
}

/**
 * 新增角色。
 */
async function handleAdd() {
  reset()
  await getMenuTree()
  ensureRequiredMenuSelected(false)
  open.value = true
  title.value = '添加角色'
}

/**
 * 修改角色。
 * @param row 行数据
 */
async function handleUpdate(row: any) {
  reset()
  await getMenuTree()
  const response: any = await getRole(row.roleId)
  form.value = {
    ...response.data,
    tenantId: response.data?.tenantId || localStorage.getItem('tenantId') || '000000'
  }
  selectedMenuIds.value = Array.isArray(form.value.menuIds) ? [...form.value.menuIds] : []
  syncTodoCenterRequiredRule()
  ensureRequiredMenuSelected(false)
  open.value = true
  title.value = '修改角色'
}

/**
 * 打开菜单权限弹框。
 */
function openMenuPermissionDialog() {
  menuDialogOpen.value = true
  ensureRequiredMenuSelected(false)
  nextTick(() => {
    if (!menuRef.value) {
      return
    }
    menuRef.value.setCheckedKeys(selectedMenuIds.value)
  })
}

/**
 * 确认菜单权限选择。
 */
function confirmMenuPermission() {
  if (!menuRef.value) {
    menuDialogOpen.value = false
    return
  }
  const checkedKeys = menuRef.value.getCheckedKeys()
  const halfCheckedKeys = menuRef.value.getHalfCheckedKeys()
  const mergedKeys = Array.from(new Set([
    ...(checkedKeys || []),
    ...(halfCheckedKeys || []),
    ...requiredMenuIds.value
  ]))
  selectedMenuIds.value = mergedKeys.filter((menuId) => !isVirtualMenuId(menuId))
  menuDialogOpen.value = false
}

/**
 * 清空菜单权限。
 */
function clearMenuPermission() {
  selectedMenuIds.value = [...requiredMenuIds.value]
  menuNodeAll.value = false
  if (menuRef.value) {
    menuRef.value.setCheckedKeys(selectedMenuIds.value)
  }
}

/**
 * 提交角色表单。
 */
async function submitForm() {
  await roleRef.value.validate(async (valid: boolean) => {
    if (!valid) {
      return
    }
    form.value.menuIds = Array.from(new Set([...(selectedMenuIds.value || []), ...requiredMenuIds.value]))
      .filter((menuId) => !isVirtualMenuId(menuId))
    if (form.value.roleId !== undefined) {
      await updateRole(form.value)
      ElMessage.success('修改成功')
    } else {
      await addRole(form.value)
      ElMessage.success('新增成功')
    }
    open.value = false
    getList()
  })
}

/**
 * 删除角色。
 * @param row 行数据
 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除角色名称为"' + row.roleName + '"的数据项？', '提示', {
    type: 'warning'
  }).then(async () => {
    await delRole(row.roleId)
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/**
 * 打开分配数据权限弹窗。
 * @param row 行数据
 */
async function handleDataScope(row: any) {
  resetDataScope()
  await getDeptTree()
  const response: any = await getRole(row.roleId)
  dataScopeForm.roleId = response.data?.roleId
  dataScopeForm.roleName = response.data?.roleName || ''
  dataScopeForm.dataScope = response.data?.dataScope || '1'
  dataScopeForm.deptIds = Array.isArray(response.data?.deptIds) ? response.data.deptIds : []
  selectedDeptIds.value = [...dataScopeForm.deptIds]
  dataScopeOpen.value = true
}

/**
 * 打开自定义部门配置弹窗。
 * 将当前已选择的部门回显至部门树，便于继续调整。
 */
function openDeptPermissionDialog() {
  deptDialogOpen.value = true
  nextTick(() => {
    if (!deptRef.value) {
      return
    }
    deptRef.value.setCheckedKeys(selectedDeptIds.value)
  })
}

/**
 * 确认自定义部门选择。
 * 合并半选节点，避免界面与提交数据不一致。
 */
function confirmDeptPermission() {
  if (!deptRef.value) {
    deptDialogOpen.value = false
    return
  }
  const checkedKeys = deptRef.value.getCheckedKeys() || []
  const halfCheckedKeys = deptRef.value.getHalfCheckedKeys() || []
  selectedDeptIds.value = Array.from(new Set([...(checkedKeys || []), ...(halfCheckedKeys || [])]))
  deptDialogOpen.value = false
}

/**
 * 清空自定义部门配置。
 */
function clearDeptPermission() {
  selectedDeptIds.value = []
  deptNodeAll.value = false
  if (deptRef.value) {
    deptRef.value.setCheckedKeys([])
  }
}

/**
 * 提交数据权限分配。
 */
async function submitDataScope() {
  if (!dataScopeForm.roleId) {
    ElMessage.warning('角色ID不能为空')
    return
  }
  let deptIds: Array<number | string> = []
  if (dataScopeForm.dataScope === '2') {
    deptIds = [...selectedDeptIds.value]
  }
  await updateRoleDataScope({
    roleId: dataScopeForm.roleId,
    dataScope: dataScopeForm.dataScope,
    deptIds: deptIds
  })
  ElMessage.success('分配成功')
  dataScopeOpen.value = false
  getList()
}

/**
 * 树展开/折叠。
 * @param value 是否展开
 * @param type 树类型
 */
function handleCheckedTreeExpand(value: boolean, type: string) {
  if (type === 'menu' && menuRef.value) {
    const nodesMap = menuRef.value.store.nodesMap
    for (const key in nodesMap) {
      nodesMap[key].expanded = value
    }
    return
  }
  if (type === 'dept' && deptRef.value) {
    const nodesMap = deptRef.value.store.nodesMap
    for (const key in nodesMap) {
      nodesMap[key].expanded = value
    }
  }
}

/**
 * 树全选/全不选。
 * @param value 是否全选
 * @param type 树类型
 */
function handleCheckedTreeNodeAll(value: boolean, type: string) {
  if (type === 'menu' && menuRef.value) {
    const keys = value ? collectCheckableMenuIds() : []
    menuRef.value.setCheckedKeys(keys)
    ensureRequiredMenuSelected()
    return
  }
  if (type === 'dept' && deptRef.value) {
    deptRef.value.setCheckedNodes(value ? deptOptions.value : [])
  }
}

watch(
  () => String(form.value.roleKey || '').trim().toLowerCase(),
  () => {
    syncTodoCenterRequiredRule()
    if (menuDialogOpen.value) {
      ensureRequiredMenuSelected()
    }
  }
)

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
  border: 1px solid var(--erp-c-border-strong);
  border-radius: 4px;
}
.menu-permission-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.menu-permission-btn {
  min-width: 150px;
}
.menu-permission-tip {
  color: var(--erp-c-text-2);
  font-size: calc(13px * var(--erp-font-scale, 1));
}
.menu-tree-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.permission-tree {
  max-height: 420px;
  overflow: auto;
  padding: 6px 8px;
}
</style>



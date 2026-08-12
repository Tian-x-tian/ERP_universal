<template>
  <div class="app-container menu-page">
    <el-card shadow="never" class="menu-preview-card" v-loading="previewLoading">
      <template #header>
        <div class="menu-preview-header">
          <div class="menu-preview-title">当前菜单样式同步视图</div>
          <div class="menu-preview-metrics">
            <el-tag type="success">已同步 {{ syncOverview.synced }}</el-tag>
            <el-tag type="warning">待升级 {{ syncOverview.legacy }}</el-tag>
            <el-tag type="danger">未入库 {{ syncOverview.missing }}</el-tag>
          </div>
        </div>
      </template>
      <div class="menu-preview-hint">
        菜单管理已对齐当前左侧菜单蓝图；出现“待升级/未入库”时，可点击“同步当前页面菜单”一键落库。
      </div>
      <div class="menu-blueprint-grid">
        <div class="menu-group-card" v-for="group in blueprintViewList" :key="group.path">
          <div class="menu-group-header">
            <div class="menu-group-title">
              <el-icon><component :is="resolveMenuIcon(group.icon)" /></el-icon>
              <span>{{ group.menuName }}</span>
            </div>
            <el-tag size="small" :type="group.pendingCount === 0 ? 'success' : 'warning'">
              {{ group.pendingCount === 0 ? '已同步' : '待同步' }}
            </el-tag>
          </div>
          <div class="menu-child-list">
            <div
              class="menu-child-item"
              v-for="child in group.nodes"
              :key="`${group.path}-${child.path}`"
              :style="{ paddingLeft: `${16 + child.depth * 18}px` }"
            >
              <div class="menu-child-main">
                <div class="menu-child-left">
                  <el-icon><component :is="resolveMenuIcon(child.icon)" /></el-icon>
                  <div class="menu-child-text">
                    <span class="menu-child-name">{{ child.menuName }}</span>
                    <span class="menu-child-path">{{ child.path }}</span>
                  </div>
                </div>
                <el-tag size="small" :type="child.syncTagType">{{ child.syncLabel }}</el-tag>
              </div>
              <div class="menu-child-extra" v-if="child.matchedPath && child.matchedPath !== child.path">
                库路径：{{ child.matchedPath }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="menu-table-card">
      <div class="table-header">
        <el-button v-hasPermi="['system:menu:add']" type="primary" icon="Plus" @click="handleAdd">新增菜单</el-button>
        <el-button
          v-hasPermi="['system:menu:edit']"
          :type="syncOverview.pending > 0 ? 'warning' : 'success'"
          icon="Refresh"
          :loading="syncLoading"
          style="margin-left: 10px"
          @click="handleSyncCurrentMenu"
        >
          同步当前页面菜单
        </el-button>
        <el-input
          v-model="queryParams.menuName"
          placeholder="请输入菜单名称/路径"
          clearable
          style="width: 220px; margin-left: 10px"
          @keyup.enter="applyTreeFilter"
          @clear="applyTreeFilter"
        />
        <el-button type="primary" icon="Search" style="margin-left: 10px" @click="applyTreeFilter">搜索</el-button>
        <el-button type="info" icon="RefreshRight" style="margin-left: 10px" @click="getList">刷新</el-button>
      </div>

      <el-table
        v-loading="tableLoading"
        :data="menuList"
        row-key="menuId"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        lazy
        :load="loadMenuChildren"
        border
        style="width: 100%; margin-top: 20px"
      >
        <el-table-column prop="menuName" label="菜单名称" :show-overflow-tooltip="true" width="180" />
        <el-table-column prop="icon" label="图标" align="center" width="90">
          <template #default="scope">
            <el-icon><component :is="resolveMenuIcon(scope.row.icon)" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" :show-overflow-tooltip="true" />
        <el-table-column prop="orderNum" label="排序" width="70" />
        <el-table-column prop="perms" label="权限标识" :show-overflow-tooltip="true" />
        <el-table-column prop="component" label="组件路径" :show-overflow-tooltip="true" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
        <el-table-column label="操作" align="center" width="220">
          <template #default="scope">
            <el-button v-hasPermi="['system:menu:add']" link type="primary" icon="Plus" @click="handleAdd(scope.row)">新增</el-button>
            <el-button v-hasPermi="['system:menu:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button v-hasPermi="['system:menu:remove']" link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="menuRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="menuOptions"
                :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
                value-key="menuId"
                placeholder="选择上级菜单"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType">
                <el-radio label="M">目录</el-radio>
                <el-radio label="C">菜单</el-radio>
                <el-radio label="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单图标" prop="icon" v-if="form.menuType != 'F'">
              <el-input v-model="form.icon" placeholder="请输入图标名称 (如 Setting)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != 'F'">
            <el-form-item label="是否外链">
              <el-radio-group v-model="form.isFrame">
                <el-radio :label="0">是</el-radio>
                <el-radio :label="1">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != 'F'">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="form.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType == 'C'">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="请输入组件路径" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限标识" v-if="form.menuType != 'M'">
              <el-input v-model="form.perms" placeholder="请输入权限标识" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">取 消</el-button>
          <el-button v-hasPermi="['system:menu:add', 'system:menu:edit']" type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listMenu, listMenuTree, getMenu, addMenu, updateMenu, delMenu, syncMenu } from '@/api/system/menu'
import { HOME_MENU_PATH, type MenuBlueprintNode, MENU_BLUEPRINT_GROUPS } from '@/constants/default-menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableLoading = ref(false)
const previewLoading = ref(false)
const syncLoading = ref(false)
const menuList = ref<any[]>([])
const menuOptions = ref<any[]>([])
const rawMenuFlat = ref<any[]>([])
const blueprintViewList = ref<BlueprintGroupView[]>([])
const open = ref(false)
const title = ref('')
const menuRef = ref()

const loadedMenuNodeMap = new Map<string, any>()
const loadedParentIdSet = new Set<string>()

const queryParams = reactive({
  menuName: undefined
})

const syncOverview = reactive({
  synced: 0,
  legacy: 0,
  missing: 0,
  pending: 0
})
const LEGACY_MENU_ROOT_PATHS = ['/system', '/platform']
const LEGACY_ROOT_NAMES = ['系统管理', '平台底座']

const form = ref({
  menuId: undefined,
  parentId: 0,
  menuName: undefined,
  icon: undefined,
  menuType: 'M',
  orderNum: 0,
  isFrame: 1,
  path: undefined,
  component: undefined,
  perms: undefined,
  status: '0'
})

const rules = {
  menuName: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
  path: [{ required: true, message: '路由地址不能为空', trigger: 'blur' }]
}

const MENU_COMPONENT_MAP: Record<string, string> = {
  '/home': '/views/home/index',
  '/workbench/system-notice': '/views/platform/todo-center/index',
  '/workbench/process-todo': '/views/platform/todo-center/index',
  '/workbench/attendance': '/views/workbench/attendance/index',
  '/business/hr/attendance': '/views/business/hr/attendance/index',
  '/business/hr/payroll': '/views/business/hr/payroll/index',
  '/business/hr/performance': '/views/business/hr/performance/index',
  '/business/hr/integration': '/views/business/hr/payroll/index',
  '/business/hr/integration/salary': '/views/business/hr/payroll/index',
  '/business/hr/integration/attendance': '/views/business/hr/attendance/index',
  '/business/hr/integration/performance': '/views/business/hr/performance/index',
  '/business/hr/employee': '/views/business/hr/employee/index',
  '/business/inventory/kpi': '/views/inventory/kpi/index',
  '/system/org-structure': '/views/system/dept/index',
  '/system/company': '/views/system/company/index',
  '/system/dept': '/views/system/dept/index',
  '/system/post': '/views/system/post/index',
  '/system/user': '/views/system/user/index',
  '/system/role': '/views/system/role/index',
  '/system/data-permission': '/views/platform/data-scope/index',
  '/workflow-center/definition': '/views/platform/workflow/index',
  '/workflow-center/instance': '/views/platform/workflow/index',
  '/system/menu': '/views/system/menu/index',
  '/system/dict': '/views/system/dict/index',
  '/system/config': '/views/system/config/index',
  '/system/ai-config': '/views/system/ai-config/index',
  '/system/code-rule': '/views/platform/code-rule/index',
  '/system/notice-manage': '/views/system/notice/index',
  '/system/tenant': '/views/system/tenant/index',
  '/platform/region-data': '/views/system/region/index',
  '/platform/org-enhance': '/views/platform/org/index',
  '/platform/org-enhance/company': '/views/platform/org/index',
  '/platform/org-enhance/department': '/views/platform/org/index',
  '/platform/org-enhance/position': '/views/platform/org/index',
  '/system/mdm/customer': '/views/system/mdm/customer/index',
  '/system/mdm/supplier': '/views/system/mdm/supplier/index',
  '/system/mdm/item': '/views/system/mdm/item/index',
  '/system/mdm/warehouse': '/views/system/mdm/warehouse/index',
  '/system/mdm/employee': '/views/system/mdm/employee/index',
  '/system/mdm/dimension': '/views/system/mdm/dimension/index',
  '/system/mdm/dimension/org': '/views/system/mdm/dimension/index',
  '/system/mdm/dimension/cost-center': '/views/system/mdm/dimension/index',
  '/system/mdm/dimension/project': '/views/system/mdm/dimension/index',
  '/system/mdm/dict': '/views/system/mdm/dict/index',
  '/system/mdm/trace': '/views/system/mdm/trace/index',
  '/monitor/oper-log': '/views/system/oper-log/index',
  '/monitor/login-log': '/views/system/login-log/index',
  '/monitor/audit-log': '/views/platform/audit-log/index'
}

const MENU_PERMISSION_MAP: Record<string, string> = {
  '/home': 'system:home:view',
  '/workbench/system-notice': 'system:message:list',
  '/workbench/process-todo': 'workflow:todo:list',
  '/workbench/attendance': 'business:hr:attendance:list',
  '/business/hr/attendance': 'business:hr:attendance:list',
  '/business/hr/payroll': 'business:hr:payroll:list',
  '/business/hr/performance': 'business:hr:performance:list',
  '/business/hr/integration': 'business:hr:integration:salary',
  '/business/hr/integration/salary': 'business:hr:integration:salary',
  '/business/hr/integration/attendance': 'business:hr:attendance:list',
  '/business/hr/integration/performance': 'business:hr:performance:list',
  '/business/hr/employee': 'business:hr:employee:list',
  '/business/inventory/kpi': 'business:inventory:report:list',
  '/system/org-structure': 'system:dept:list',
  '/system/company': 'system:company:list',
  '/system/dept': 'system:dept:list',
  '/system/post': 'system:post:list',
  '/system/user': 'system:user:list',
  '/system/role': 'system:role:list',
  '/system/data-permission': 'system:dataScope:view',
  '/workflow-center/definition': 'workflow:definition:list',
  '/workflow-center/instance': 'workflow:instance:list',
  '/system/menu': 'system:menu:list',
  '/system/dict': 'system:dict:list',
  '/system/config': 'system:config:list',
  '/system/ai-config': 'system:ai:config:list',
  '/system/code-rule': 'system:codeRule:list',
  '/system/notice-manage': 'system:notice:list',
  '/system/tenant': 'system:tenant:list',
  '/platform/region-data': 'system:region:list',
  '/platform/org-enhance': 'system:org:view',
  '/platform/org-enhance/company': 'system:company:list',
  '/platform/org-enhance/department': 'system:dept:list',
  '/platform/org-enhance/position': 'system:post:list',
  '/system/mdm/customer': 'system:mdm:customer:list',
  '/system/mdm/supplier': 'system:mdm:supplier:list',
  '/system/mdm/item': 'system:mdm:item:list',
  '/system/mdm/warehouse': 'system:mdm:warehouse:list',
  '/system/mdm/employee': 'system:mdm:employee:list',
  '/system/mdm/dimension': 'system:mdm:org:list',
  '/system/mdm/dimension/org': 'system:mdm:org:list',
  '/system/mdm/dimension/cost-center': 'system:mdm:cc:list',
  '/system/mdm/dimension/project': 'system:mdm:project:list',
  '/system/mdm/dict': 'system:mdm:dict:list',
  '/system/mdm/trace': 'system:mdm:trace:list',
  '/monitor/oper-log': 'system:oper:list',
  '/monitor/login-log': 'system:loginLog:list',
  '/monitor/audit-log': 'system:audit:list'
}

interface SyncMenuNode {
  menuName: string
  path: string
  component?: string
  isFrame: number
  menuType: string
  visible: string
  status: string
  perms?: string
  icon?: string
  orderNum: number
  sourcePaths: string[]
  children: SyncMenuNode[]
}

type SyncState = 'synced' | 'legacy' | 'missing'

interface BlueprintPreviewNode {
  menuName: string
  path: string
  icon?: string
  sourcePaths: string[]
  syncState: SyncState
  syncLabel: string
  syncTagType: 'success' | 'warning' | 'danger'
  matchedPath?: string
  depth: number
}

interface BlueprintGroupView {
  menuName: string
  path: string
  icon?: string
  nodes: BlueprintPreviewNode[]
  pendingCount: number
}

/** 查询菜单表格数据 */
async function getList() {
  await applyTreeFilter()
  void loadPreviewData()
}

/** 树处理函数 */
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
    if (childrenListMap[o[config.id]] != null) {
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
  form.value = {
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    icon: undefined,
    menuType: 'M',
    orderNum: 0,
    isFrame: 1,
    path: undefined,
    component: undefined,
    perms: undefined,
    status: '0'
  }
}

/** 查询或恢复菜单树表格 */
async function applyTreeFilter() {
  const keyword = getSearchKeyword()
  if (keyword) {
    await searchMenuTable(keyword)
    return
  }
  await loadRootMenuRows()
}

/** 获取当前搜索关键字 */
function getSearchKeyword() {
  return String(queryParams.menuName || '').trim()
}

/** 是否处于搜索模式 */
function isSearchMode() {
  return !!getSearchKeyword()
}

/** 统一返回菜单图标名称，过滤后端 '#' 占位符，防止运行时崩溃 */
function resolveMenuIcon(icon: string | undefined) {
  const iconName = String(icon || '').trim()
  return (iconName && iconName !== '#') ? iconName : 'Grid'
}

/** 标准化路由路径 */
function normalizePath(path: string | undefined) {
  return String(path || '').trim().toLowerCase().replace(/\/+$/, '')
}

/** 标准化菜单ID，避免 number/string 类型差异导致误判 */
function normalizeMenuId(menuId: unknown) {
  return String(menuId ?? '')
}

/** 判断菜单路径是否属于历史分组根路径 */
function isLegacyRootPath(path: unknown) {
  return LEGACY_MENU_ROOT_PATHS.includes(normalizePath(String(path || '')))
}

/** 判断菜单名称是否属于历史分组名称 */
function isLegacyRootName(menuName: unknown) {
  return LEGACY_ROOT_NAMES.includes(String(menuName || '').trim())
}

/** 过滤历史分组（系统管理/平台底座）及其所有子节点 */
function pruneLegacyMenuBranches(data: any[]) {
  const sourceList = Array.isArray(data) ? data : []
  if (!sourceList.length) {
    return []
  }

  const legacyNodeIds = new Set<string>()
  sourceList.forEach((item) => {
    if (isLegacyRootPath(item?.path) || isLegacyRootName(item?.menuName)) {
      legacyNodeIds.add(normalizeMenuId(item?.menuId))
    }
  })

  if (!legacyNodeIds.size) {
    return sourceList
  }

  let updated = true
  while (updated) {
    updated = false
    sourceList.forEach((item) => {
      const parentId = normalizeMenuId(item?.parentId)
      const currentId = normalizeMenuId(item?.menuId)
      if (!legacyNodeIds.has(currentId) && legacyNodeIds.has(parentId)) {
        legacyNodeIds.add(currentId)
        updated = true
      }
    })
  }

  return sourceList.filter((item) => !legacyNodeIds.has(normalizeMenuId(item?.menuId)))
}

/** 过滤树形结构中的历史分组节点 */
function pruneLegacyMenuTreeNodes(nodes: any[]): any[] {
  return (Array.isArray(nodes) ? nodes : [])
    .filter((node) => !isLegacyRootPath(node?.path) && !isLegacyRootName(node?.menuName))
    .map((node) => {
      const normalizedNode = { ...node }
      if (Array.isArray(normalizedNode.children) && normalizedNode.children.length > 0) {
        normalizedNode.children = pruneLegacyMenuTreeNodes(normalizedNode.children)
      }
      return normalizedNode
    })
}

/** 规范化懒加载菜单节点 */
function normalizeLazyMenuNodes(nodes: any[]): any[] {
  return (Array.isArray(nodes) ? nodes : []).map((node) => {
    const normalizedNode = { ...node }
    if (Array.isArray(normalizedNode.children) && normalizedNode.children.length > 0) {
      normalizedNode.children = normalizeLazyMenuNodes(normalizedNode.children)
    } else {
      delete normalizedNode.children
    }
    return normalizedNode
  })
}

/** 规范化搜索结果树节点 */
function normalizeSearchMenuNodes(nodes: any[]): any[] {
  return (Array.isArray(nodes) ? nodes : []).map((node) => {
    const normalizedNode = { ...node }
    const children = Array.isArray(normalizedNode.children)
      ? normalizeSearchMenuNodes(normalizedNode.children)
      : []
    if (children.length > 0) {
      normalizedNode.children = children
    } else {
      delete normalizedNode.children
    }
    normalizedNode.hasChildren = children.length > 0
    return normalizedNode
  })
}

/** 重置已加载节点缓存 */
function resetLoadedMenuCache() {
  loadedMenuNodeMap.clear()
  loadedParentIdSet.clear()
}

/** 递归注册已加载节点 */
function registerLoadedMenuNodes(nodes: any[]) {
  for (const node of nodes || []) {
    loadedMenuNodeMap.set(normalizeMenuId(node?.menuId), node)
    if (Array.isArray(node?.children) && node.children.length > 0) {
      registerLoadedMenuNodes(node.children)
    }
  }
}

/** 递归移除旧子节点缓存 */
function unregisterLoadedMenuNodes(nodes: any[]) {
  for (const node of nodes || []) {
    loadedParentIdSet.delete(normalizeMenuId(node?.menuId))
    loadedMenuNodeMap.delete(normalizeMenuId(node?.menuId))
    if (Array.isArray(node?.children) && node.children.length > 0) {
      unregisterLoadedMenuNodes(node.children)
    }
  }
}

/** 查询指定父菜单的直接子菜单 */
async function fetchMenuChildren(parentId: number | string) {
  const response: any = await listMenuTree({ parentId })
  const sourceData = Array.isArray(response.data) ? response.data : []
  return pruneLegacyMenuTreeNodes(normalizeLazyMenuNodes(sourceData))
}

/** 加载顶级菜单 */
async function loadRootMenuRows() {
  tableLoading.value = true
  try {
    const rootMenuList = await fetchMenuChildren(0)
    menuList.value = rootMenuList
    resetLoadedMenuCache()
    loadedParentIdSet.add(normalizeMenuId(0))
    registerLoadedMenuNodes(rootMenuList)
  } catch (error) {
    console.error(error)
    menuList.value = []
    resetLoadedMenuCache()
  } finally {
    tableLoading.value = false
  }
}

/** 搜索菜单树 */
async function searchMenuTable(keyword: string) {
  tableLoading.value = true
  try {
    const response: any = await listMenuTree({ keyword })
    const sourceTree = Array.isArray(response.data) ? response.data : []
    const searchTree = pruneLegacyMenuTreeNodes(normalizeSearchMenuNodes(sourceTree))
    menuList.value = searchTree
    resetLoadedMenuCache()
    registerLoadedMenuNodes(searchTree)
  } catch (error) {
    console.error(error)
    menuList.value = []
    resetLoadedMenuCache()
  } finally {
    tableLoading.value = false
  }
}

/** 表格懒加载子菜单 */
async function loadMenuChildren(row: any, _treeNode: unknown, resolve: (data: any[]) => void) {
  if (isSearchMode()) {
    resolve(Array.isArray(row?.children) ? row.children : [])
    return
  }

  const parentKey = normalizeMenuId(row?.menuId)
  if (loadedParentIdSet.has(parentKey) && Array.isArray(row?.children)) {
    resolve(row.children)
    return
  }

  try {
    const childMenuList = await fetchMenuChildren(row?.menuId)
    if (Array.isArray(row?.children) && row.children.length > 0) {
      unregisterLoadedMenuNodes(row.children)
    }
    row.children = childMenuList
    row.hasChildren = childMenuList.length > 0
    loadedParentIdSet.add(parentKey)
    registerLoadedMenuNodes(childMenuList)
    resolve(childMenuList)
  } catch (error) {
    console.error(error)
    resolve([])
  }
}

/** 刷新指定父节点层级 */
async function refreshMenuLevel(parentId: unknown) {
  if (isSearchMode()) {
    await applyTreeFilter()
    return
  }

  const parentKey = normalizeMenuId(parentId ?? 0)
  if (parentKey === normalizeMenuId(0)) {
    await loadRootMenuRows()
    return
  }

  const parentNode = loadedMenuNodeMap.get(parentKey)
  if (!parentNode || !loadedParentIdSet.has(parentKey)) {
    await loadRootMenuRows()
    return
  }

  try {
    const childMenuList = await fetchMenuChildren(parentNode.menuId)
    if (Array.isArray(parentNode.children) && parentNode.children.length > 0) {
      unregisterLoadedMenuNodes(parentNode.children)
    }
    parentNode.children = childMenuList
    parentNode.hasChildren = childMenuList.length > 0
    loadedParentIdSet.add(parentKey)
    registerLoadedMenuNodes(childMenuList)
    menuList.value = [...menuList.value]
  } catch (error) {
    console.error(error)
    await loadRootMenuRows()
  }
}

/** 构建路径索引 */
function buildPathMenuIndex(data: any[]) {
  const pathMenuIndex = new Map<string, any[]>()
  for (const item of data || []) {
    const normalizedPath = normalizePath(item?.path)
    if (!normalizedPath) {
      continue
    }
    const list = pathMenuIndex.get(normalizedPath) || []
    list.push(item)
    pathMenuIndex.set(normalizedPath, list)
  }
  return pathMenuIndex
}

/** 从路径索引中按候选路径选取菜单 */
function pickMenuByPaths(pathMenuIndex: Map<string, any[]>, paths: string[]) {
  for (const path of paths || []) {
    const normalizedPath = normalizePath(path)
    if (!normalizedPath) {
      continue
    }
    const matched = pathMenuIndex.get(normalizedPath)
    if (matched && matched.length > 0) {
      return matched[0]
    }
  }
  return undefined
}

/** 获取同步状态显示配置 */
function resolveSyncMeta(syncState: SyncState) {
  if (syncState === 'synced') {
    return { syncLabel: '已同步', syncTagType: 'success' as const }
  }
  if (syncState === 'legacy') {
    return { syncLabel: '待升级', syncTagType: 'warning' as const }
  }
  return { syncLabel: '未入库', syncTagType: 'danger' as const }
}

/**
 * 根据蓝图节点与当前数据库索引构建单个预览节点。
 *
 * @param node 蓝图节点
 * @param depth 当前层级深度
 * @param pathMenuIndex 路径索引
 * @returns 预览节点
 */
function buildBlueprintPreviewNode(
  node: MenuBlueprintNode,
  depth: number,
  pathMenuIndex: Map<string, any[]>
): BlueprintPreviewNode {
  const sourcePaths = node.sourcePaths || [node.path]
  const canonicalMenu = pickMenuByPaths(pathMenuIndex, [node.path])
  const legacyCandidates = sourcePaths.filter((path) => normalizePath(path) !== normalizePath(node.path))
  const legacyMenu = canonicalMenu ? undefined : pickMenuByPaths(pathMenuIndex, legacyCandidates)

  let syncState: SyncState = 'missing'
  let matchedPath: string | undefined = undefined
  if (canonicalMenu) {
    syncState = 'synced'
    matchedPath = canonicalMenu.path
  } else if (legacyMenu) {
    syncState = 'legacy'
    matchedPath = legacyMenu.path
  }

  const syncMeta = resolveSyncMeta(syncState)
  return {
    menuName: node.menuName,
    path: node.path,
    icon: node.icon,
    sourcePaths,
    syncState,
    syncLabel: syncMeta.syncLabel,
    syncTagType: syncMeta.syncTagType,
    matchedPath,
    depth,
  }
}

/**
 * 递归拍平蓝图节点，供同步视图展示。
 *
 * @param node 蓝图节点
 * @param depth 当前层级深度
 * @param pathMenuIndex 路径索引
 * @returns 预览节点列表
 */
function flattenBlueprintPreviewNodes(
  node: MenuBlueprintNode,
  depth: number,
  pathMenuIndex: Map<string, any[]>
): BlueprintPreviewNode[] {
  const currentNode = buildBlueprintPreviewNode(node, depth, pathMenuIndex)
  const childNodes = Array.isArray(node.children)
    ? node.children.flatMap((child) => flattenBlueprintPreviewNodes(child, depth + 1, pathMenuIndex))
    : []
  return [currentNode, ...childNodes]
}

/** 按当前蓝图刷新菜单同步视图 */
function refreshBlueprintView(data: any[]) {
  const pathMenuIndex = buildPathMenuIndex(data)
  let synced = 0
  let legacy = 0
  let missing = 0
  const homeNode: MenuBlueprintNode = {
    menuId: 1000,
    menuName: '首页',
    path: HOME_MENU_PATH,
    icon: 'House',
    sourcePaths: [HOME_MENU_PATH]
  }
  const previewGroupList: MenuBlueprintNode[] = [homeNode, ...MENU_BLUEPRINT_GROUPS]
  blueprintViewList.value = previewGroupList.map((group) => {
    const nodes = Array.isArray(group.children)
      ? group.children.flatMap((child) => flattenBlueprintPreviewNodes(child, 0, pathMenuIndex))
      : [buildBlueprintPreviewNode(group, 0, pathMenuIndex)]

    nodes.forEach((node) => {
      if (node.syncState === 'synced') {
        synced += 1
        return
      }
      if (node.syncState === 'legacy') {
        legacy += 1
        return
      }
      missing += 1
    })

    return {
      menuName: group.menuName,
      path: group.path,
      icon: group.icon,
      nodes,
      pendingCount: nodes.filter((item) => item.syncState !== 'synced').length
    }
  })

  syncOverview.synced = synced
  syncOverview.legacy = legacy
  syncOverview.missing = missing
  syncOverview.pending = legacy + missing
}

/** 异步加载同步视图所需的完整菜单清单 */
async function loadPreviewData() {
  previewLoading.value = true
  try {
    const response: any = await listMenu()
    rawMenuFlat.value = Array.isArray(response.data) ? response.data : []
    refreshBlueprintView(rawMenuFlat.value)
  } catch (error) {
    console.error(error)
    rawMenuFlat.value = []
    refreshBlueprintView([])
  } finally {
    previewLoading.value = false
  }
}

/** 查询菜单下拉树 */
async function getMenuTree() {
  if (!rawMenuFlat.value.length) {
    const response: any = await listMenu()
    rawMenuFlat.value = Array.isArray(response.data) ? response.data : []
  }
  const tree = handleTree(pruneLegacyMenuBranches(rawMenuFlat.value), 'menuId')
  menuOptions.value = [{ menuId: 0, menuName: '主类目', children: tree }]
}

/** 新增按钮操作 */
async function handleAdd(row: any) {
  reset()
  await getMenuTree()
  if (row != null && row.menuId) {
    form.value.parentId = row.menuId
  } else {
    form.value.parentId = 0
  }
  open.value = true
  title.value = '添加菜单'
}

/** 修改按钮操作 */
async function handleUpdate(row: any) {
  reset()
  await getMenuTree()
  const response: any = await getMenu(row.menuId)
  form.value = response.data
  open.value = true
  title.value = '修改菜单'
}

/** 提交按钮 */
async function submitForm() {
  const refreshParentId = form.value.parentId
  await menuRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (form.value.menuId !== undefined) {
        await updateMenu(form.value)
        ElMessage.success('修改成功')
      } else {
        await addMenu(form.value)
        ElMessage.success('新增成功')
      }
      open.value = false
      await refreshMenuLevel(refreshParentId)
      void loadPreviewData()
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除菜单名称为"' + row.menuName + '"的数据项？', '提示', {
    type: 'warning'
  }).then(async () => {
    await delMenu(row.menuId)
    await refreshMenuLevel(row?.parentId)
    void loadPreviewData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 组装单个菜单蓝图同步节点 */
function buildSyncNode(node: MenuBlueprintNode, orderNum: number): SyncMenuNode {
  const childNodes = Array.isArray(node.children)
    ? node.children.map((child, index) => buildSyncNode(child, index + 1))
    : []
  const isMenuDirectory = childNodes.length > 0
  return {
    menuName: node.menuName,
    path: node.path,
    component: isMenuDirectory ? undefined : MENU_COMPONENT_MAP[node.path],
    isFrame: 1,
    menuType: isMenuDirectory ? 'M' : 'C',
    visible: '0',
    status: '0',
    perms: isMenuDirectory ? undefined : MENU_PERMISSION_MAP[node.path],
    icon: node.icon,
    orderNum,
    sourcePaths: node.sourcePaths || [node.path],
    children: childNodes
  }
}

/** 组装增量菜单同步请求体 */
function buildSyncMenuPayload(): SyncMenuNode[] {
  const pendingGroupPathSet = new Set(
    blueprintViewList.value
      .filter((group) => group.pendingCount > 0)
      .map((group) => normalizePath(group.path))
  )

  const payload: SyncMenuNode[] = []
  if (pendingGroupPathSet.has(normalizePath(HOME_MENU_PATH))) {
    payload.push({
      menuName: '首页',
      path: HOME_MENU_PATH,
      component: MENU_COMPONENT_MAP[HOME_MENU_PATH],
      isFrame: 1,
      menuType: 'C',
      visible: '0',
      status: '0',
      perms: MENU_PERMISSION_MAP[HOME_MENU_PATH],
      icon: 'House',
      orderNum: 1,
      sourcePaths: [HOME_MENU_PATH],
      children: []
    })
  }

  MENU_BLUEPRINT_GROUPS.forEach((group, groupIndex) => {
    if (!pendingGroupPathSet.has(normalizePath(group.path))) {
      return
    }
    payload.push(buildSyncNode(group, groupIndex + 2))
  })

  return payload
}

/** 同步当前页面菜单结构 */
function handleSyncCurrentMenu() {
  ElMessageBox.confirm('将把当前页面菜单结构同步到菜单管理并写入数据库，是否继续？', '提示', {
    type: 'warning'
  }).then(async () => {
    syncLoading.value = true
    try {
      if (!blueprintViewList.value.length) {
        await loadPreviewData()
      }

      const payload = buildSyncMenuPayload()
      if (!payload.length) {
        ElMessage.success('当前菜单已同步，无需重复处理')
        return
      }

      const response: any = await syncMenu(payload)
      ElMessage.success('菜单同步成功，实际变更记录数：' + (response.data || 0))
      await applyTreeFilter()
      await loadPreviewData()
    } finally {
      syncLoading.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  void applyTreeFilter()
  void loadPreviewData()
})
</script>

<style scoped>
.menu-page {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.menu-preview-card,
.menu-table-card {
  border-radius: 16px;
}

.menu-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.menu-preview-title {
  font-size: calc(16px * var(--erp-font-scale, 1));
  font-weight: 700;
  color: var(--erp-c-text);
}

.menu-preview-metrics {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-preview-hint {
  color: var(--erp-c-text-3);
  font-size: calc(13px * var(--erp-font-scale, 1));
  line-height: 20px;
  margin-bottom: 12px;
}

.menu-blueprint-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.menu-group-card {
  border: 1px solid rgba(21, 91, 164, 0.14);
  border-radius: 12px;
  padding: 12px;
  background: linear-gradient(180deg, var(--erp-c-glass-strong), var(--erp-c-glass-strong));
}

.menu-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.menu-group-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: calc(14px * var(--erp-font-scale, 1));
  font-weight: 700;
  color: #2c415c;
}

.menu-child-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-child-item {
  border: 1px solid rgba(24, 101, 176, 0.12);
  border-radius: 10px;
  padding: 8px 10px;
  background: var(--erp-c-surface);
}

.menu-child-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.menu-child-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.menu-child-left > .el-icon {
  color: #39628d;
}

.menu-child-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.menu-child-name {
  font-size: calc(13px * var(--erp-font-scale, 1));
  font-weight: 600;
  color: #314860;
}

.menu-child-path {
  font-size: calc(12px * var(--erp-font-scale, 1));
  color: var(--erp-c-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

.menu-child-extra {
  font-size: calc(12px * var(--erp-font-scale, 1));
  color: #8a6f31;
  margin-top: 4px;
  line-height: 18px;
}

.table-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 8px;
}

@media (max-width: 768px) {
  .menu-preview-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .menu-preview-metrics {
    flex-wrap: wrap;
  }

  .menu-child-path {
    max-width: 180px;
  }
}
</style>


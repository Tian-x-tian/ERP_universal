<template>
  <div class="org-page">
    <!-- ===== Executive 头部标题与全局操作 ===== -->
    <header class="org-header">
      <div class="org-header-left">
        <h1 class="org-module-title">组织架构管理</h1>
        <p class="org-module-subtitle">定义、维护企业内部多级组织实体与层级关系</p>
      </div>
      <div class="org-header-actions">
        <el-button class="org-btn-ghost">导出架构</el-button>
        <el-button
          v-hasPermi="['system:dept:add']"
          type="primary"
          class="org-btn-primary"
          @click="handleAdd()"
        >
          <el-icon><Plus /></el-icon>
          <span>新增部门</span>
        </el-button>
      </div>
    </header>

    <!-- ===== Bento 栅格布局 ===== -->
    <div class="org-bento-grid">
      <!-- 左侧：组织目录树 (Span 4) -->
      <aside class="org-bento-side">
        <div class="org-card-hd">
          <h3 class="org-card-title">组织目录树</h3>
          <el-button link @click="getList"><el-icon><Refresh /></el-icon></el-button>
        </div>
        
        <div class="org-search-box">
          <el-input
            v-model="deptName"
            placeholder="搜索部门或公司"
            :prefix-icon="Search"
            clearable
          />
        </div>

        <div class="org-tree-wrap" v-loading="loading">
          <el-tree
            ref="treeRef"
            :data="deptOptions"
            :props="{ label: 'deptName', children: 'children' }"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            node-key="deptId"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <div class="org-tree-node" :class="{ 'is-active': selectedDept?.deptId === data.deptId }">
                <el-icon class="org-node-icon">
                  <OfficeBuilding v-if="data.parentId === 0" />
                  <FolderOpened v-else-if="data.children && data.children.length > 0" />
                  <Files v-else />
                </el-icon>
                <span class="org-node-label">{{ data.deptName }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </aside>

      <!-- 右侧：详情看板区 (Span 8) -->
      <main class="org-bento-main">
        <!-- 部门 Hero 看板 -->
        <div class="org-hero-card">
          <div class="org-hero-header">
            <div class="org-hero-title-group">
              <div class="org-hero-top">
                <h2 class="org-hero-name">{{ selectedDept?.deptName || '战略咨询部' }}</h2>
                <el-tag :type="selectedDept?.status === '0' ? 'success' : 'info'" effect="light" round size="small">
                  {{ selectedDept?.status === '0' ? '运行中' : '已停用' }}
                </el-tag>
              </div>
              <p class="org-hero-location">
                <el-icon><Location /></el-icon>
                <span>上海市浦东新区陆家嘴环路 2000 号</span>
              </p>
            </div>
            <div class="org-hero-actions">
              <el-button
                v-hasPermi="['system:dept:edit']"
                circle
                :icon="Edit"
                class="org-icon-btn"
                @click="handleUpdate(selectedDept || {})"
              />
              <el-button
                v-hasPermi="['system:dept:remove']"
                circle
                :icon="Delete"
                class="org-icon-btn is-danger"
                @click="handleDelete(selectedDept || {})"
              />
            </div>
          </div>

          <!-- 指标行 (Stats) -->
          <div class="org-hero-stats">
            <div class="org-stat-item">
              <p class="org-stat-label">总编制人数</p>
              <div class="org-stat-val">
                <span class="org-stat-num">{{ selectedDept?.personCount || 42 }}</span>
                <span class="org-stat-trend up"><el-icon><Top /></el-icon> 12%</span>
              </div>
            </div>
            <div class="org-stat-item">
              <p class="org-stat-label">下属组织实体</p>
              <div class="org-stat-val">
                <span class="org-stat-num">{{ selectedDept?.children?.length || '08' }}</span>
                <span class="org-stat-unit">包含 2 个虚拟组</span>
              </div>
            </div>
            <div class="org-stat-item">
              <p class="org-stat-label">年度预算使用率</p>
              <div class="org-stat-val">
                <span class="org-stat-num tertiary">{{ selectedDept?.budgetRate || '74%' }}</span>
                <div class="org-stat-progress">
                  <div class="org-progress-bar" :style="{ width: selectedDept?.budgetRate || '74%' }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 关键岗位负责人表格 -->
        <div class="org-personnel-card">
          <div class="org-card-hd has-border">
            <h3 class="org-card-title">关键岗位负责人</h3>
            <el-button link type="primary" class="org-link-btn">查看全部 {{ selectedDept?.personTotal || 42 }} 位员工</el-button>
          </div>
          <el-table :data="personList" class="org-premium-table">
            <el-table-column label="姓名" min-width="160">
              <template #default="scope">
                <div class="org-user-cell">
                  <div class="org-avatar-sm" :class="scope.row.avatarClass">{{ scope.row.initial }}</div>
                  <div class="org-user-info">
                    <p class="org-user-name">{{ scope.row.nickName }}</p>
                    <p class="org-user-email">{{ scope.row.email }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="postName" label="岗位" min-width="130" />
            <el-table-column prop="userName" label="工号" width="100" class-name="org-code-cell" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === '0' ? 'success' : 'warning'" size="small" effect="plain" round>
                  {{ scope.row.status === '0' ? '在职' : '请假中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default>
                <el-button link :icon="MoreFilled" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </main>
    </div>

    <!-- 添加或修改部门对话框 -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body class="wf-exec-dialog">
      <el-form ref="deptRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24" v-if="form.parentId !== 0">
            <el-form-item label="上级部门" prop="parentId">
              <el-tree-select
                v-model="form.parentId"
                :data="deptOptions"
                :props="{ value: 'deptId', label: 'deptName', children: 'children' }"
                value-key="deptId"
                placeholder="选择上级部门"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="example@executive.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门状态">
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
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" class="org-btn-submit" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, toRefs, watch } from 'vue'
import { listDept, deptTree, getDept, addDept, updateDept, delDept } from '@/api/system/dept'
import { useDict } from '@/utils/dict'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Refresh, OfficeBuilding, FolderOpened, Files,
  Location, Edit, Delete, Top, MoreFilled
} from '@element-plus/icons-vue'
import { hasPermi } from '@/utils/permission'

const { sys_normal_disable } = useDict('sys_normal_disable')

const loading = ref(true)
const deptList = ref<any[]>([])
const deptOptions = ref<any[]>([])
const deptName = ref('')
const selectedDept = ref<any>(null)
const personList = ref<any[]>([])
const open = ref(false)
const title = ref('')
const deptRef = ref()
const treeRef = ref()

const data = reactive({
  form: {} as any,
  rules: {
    parentId: [{ required: true, message: '上级部门不能为空', trigger: 'blur' }],
    deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
    orderNum: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
    phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
  }
})

const { form, rules } = toRefs(data)

/** 样板负责人数据注入 */
const SAMPLE_PERSONS: any[] = [
  { initial: 'LW', nickName: '李文君', email: 'wenjun.li@executive.com', postName: '高级战略顾问', userName: 'EX-9021', status: '0', avatarClass: 'is-primary' },
  { initial: 'ZH', nickName: '张昊天', email: 'haotian.z@executive.com', postName: '项目经理', userName: 'EX-9044', status: '0', avatarClass: 'is-tertiary' },
  { initial: 'WY', nickName: '王雅婷', email: 'yating.w@executive.com', postName: '数据分析专家', userName: 'EX-8812', status: '1', avatarClass: 'is-secondary' }
]

/** 节点过滤 */
const filterNode = (value: string, data: any) => {
  if (!value) return true
  return data.deptName.includes(value)
}

/** 树节点点击 */
function handleNodeClick(data: any) {
  selectedDept.value = data
  personList.value = SAMPLE_PERSONS
}

/** 监视搜索关键字 */
watch(deptName, (val) => {
  treeRef.value.filter(val)
})

/** 查询部门列表 */
function getList() {
  if (!hasPermi('system:dept:list')) {
    deptList.value = []
    loading.value = false
    return
  }
  loading.value = true
  listDept().then(response => {
    deptList.value = handleTree(response.data, 'deptId')
    deptOptions.value = deptList.value
    if (deptList.value.length > 0) {
      handleNodeClick(deptList.value[0])
    }
    loading.value = false
  })
}

/** 查询部门下拉树结构 */
function getTreeselect() {
  if (!hasPermi('system:dept:list')) {
    deptOptions.value = []
    return
  }
  deptTree().then(response => {
    deptOptions.value = response.data
  })
}

/** 构造树形结构方法 */
function handleTree(data: any[], idKey: string, parentIdKey: string = 'parentId') {
  const childrenListMap: any = {}
  const nodeIds: any = {}
  const tree: any[] = []

  for (const d of data) {
    const parentId = d[parentIdKey]
    if (childrenListMap[parentId] == null) {
      childrenListMap[parentId] = []
    }
    nodeIds[d[idKey]] = d
    childrenListMap[parentId].push(d)
  }

  for (const d of data) {
    const parentId = d[parentIdKey]
    if (nodeIds[parentId] == null) {
      tree.push(d)
    }
  }

  for (const t of tree) {
    adaptToChildrenList(t)
  }

  function adaptToChildrenList(o: any) {
    if (childrenListMap[o[idKey]] !== undefined) {
      o.children = childrenListMap[o[idKey]]
    }
    if (o.children) {
      for (const c of o.children) {
        adaptToChildrenList(c)
      }
    }
  }
  return tree
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    deptId: undefined,
    tenantId: localStorage.getItem('tenantId') || '000000',
    companyId: undefined,
    parentId: 0,
    deptName: undefined,
    orderNum: 0,
    leader: undefined,
    phone: undefined,
    email: undefined,
    status: '0'
  }
}

/** 新增按钮操作 */
function handleAdd(row?: any) {
  reset()
  getTreeselect()
  if (row != undefined) {
    form.value.parentId = row.deptId
    form.value.companyId = row.companyId
  }
  open.value = true
  title.value = '添加部门'
}

/** 修改按钮操作 */
function handleUpdate(row: any) {
  reset()
  getTreeselect()
  getDept(row.deptId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改部门'
  })
}

/** 提交按钮 */
function submitForm() {
  deptRef.value.validate((valid: boolean) => {
    if (valid) {
      const payload = {
        ...form.value,
        tenantId: form.value.tenantId || localStorage.getItem('tenantId') || '000000',
        parentId: Number(form.value.parentId ?? 0),
      }
      if (form.value.deptId != undefined) {
        updateDept(payload).then(() => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDept(payload).then(() => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除名称为"' + row.deptName + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delDept(row.deptId)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.org-page {
  padding: 0;
  background-color: var(--erp-c-surface-2);
  min-height: calc(100vh - 84px);
}

/* ===== Executive 头部标题 ===== */
.org-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
}

.org-module-title {
  font-size: calc(28px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: var(--erp-c-text-strong);
  letter-spacing: -0.025em;
  margin: 0;
}

.org-module-subtitle {
  font-size: calc(14px * var(--erp-font-scale, 1));
  color: var(--erp-c-text-3);
  margin: 8px 0 0 0;
}

.org-header-actions {
  display: flex;
  gap: 12px;
}

.org-btn-ghost {
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  font-weight: 600;
  background: var(--erp-c-fill);
  color: var(--erp-c-text-2);
  border: none;
  &:hover { background: var(--erp-c-fill-strong); }
}

.org-btn-primary {
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(135deg, #0057c2 0%, #006ef2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 87, 194, 0.25);
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== Bento Grid Layout ===== */
.org-bento-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 24px;
}

.org-bento-side {
  grid-column: span 4;
  background: var(--erp-c-surface);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(25, 28, 30, 0.04);
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.org-bento-main {
  grid-column: span 8;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.org-card-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  &.has-border {
    padding: 20px 24px;
    border-bottom: 1px solid var(--erp-c-border);
    margin-bottom: 0;
  }
}

.org-card-title {
  font-size: calc(18px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: var(--erp-c-text);
  margin: 0;
}

.org-search-box {
  margin-bottom: 20px;
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    background-color: var(--erp-c-surface-2);
    box-shadow: none !important;
    border: 1px solid var(--erp-c-border-strong);
  }
}

.org-tree-wrap {
  flex: 1;
  overflow-y: auto;
  
  :deep(.el-tree) {
    --el-tree-node-hover-bg-color: var(--erp-c-fill);
    background: transparent;
  }
  
  :deep(.el-tree-node__content) {
    height: 42px;
    border-radius: 8px;
    margin-bottom: 2px;
  }
}

.org-tree-node {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  
  &.is-active {
    position: relative;
    color: #0057c2;
    font-weight: 700;
    
    &::before {
      content: "";
      position: absolute;
      left: -20px;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 18px;
      background: #0057c2;
      border-radius: 0 4px 4px 0;
    }
  }
}

.org-node-icon {
  font-size: calc(18px * var(--erp-font-scale, 1));
  color: var(--erp-c-text-4);
}

.org-node-label {
  font-size: calc(14px * var(--erp-font-scale, 1));
}

/* ===== Details Area ===== */
.org-hero-card {
  background: var(--erp-c-surface);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(25, 28, 30, 0.04);
  padding: 32px;
}

.org-hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.org-hero-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.org-hero-name {
  font-size: calc(24px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: var(--erp-c-text);
  margin: 0;
}

.org-hero-location {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: calc(13px * var(--erp-font-scale, 1));
  color: var(--erp-c-text-3);
  margin: 0;
}

.org-hero-actions {
  display: flex;
  gap: 10px;
}

.org-icon-btn {
  border: 1px solid var(--erp-c-border-strong);
  color: var(--erp-c-text-3);
  &:hover { background: var(--erp-c-surface-2); color: var(--erp-c-text); }
  
  &.is-danger {
    &:hover { background: var(--erp-c-tint-red); color: #ef4444; border-color: var(--erp-c-tint-red); }
  }
}

.org-hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.org-stat-item {
  background: var(--erp-c-surface-2);
  padding: 20px;
  border-radius: 12px;
}

.org-stat-label {
  font-size: calc(11px * var(--erp-font-scale, 1));
  font-weight: 700;
  color: var(--erp-c-text-4);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}

.org-stat-val {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.org-stat-num {
  font-size: calc(32px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: #0057c2;
  line-height: 1;
  
  &.tertiary { color: #9e3d00; }
}

.org-stat-trend {
  font-size: calc(12px * var(--erp-font-scale, 1));
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  
  &.up { background: var(--erp-c-tint-green); color: #16a34a; }
}

.org-stat-unit {
  font-size: calc(12px * var(--erp-font-scale, 1));
  color: var(--erp-c-text-4);
  padding-bottom: 4px;
}

.org-stat-progress {
  flex: 1;
  height: 6px;
  background: var(--erp-c-fill-strong);
  border-radius: 3px;
  margin-bottom: 8px;
  overflow: hidden;
  
  .org-progress-bar {
    height: 100%;
    background: #9e3d00;
    border-radius: 3px;
  }
}

/* ===== Personnel Table ===== */
.org-personnel-card {
  background: var(--erp-c-surface);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(25, 28, 30, 0.04);
  overflow: hidden;
}

.org-link-btn {
  font-size: calc(13px * var(--erp-font-scale, 1));
  font-weight: 700;
}

.org-premium-table {
  :deep(.el-table__header-wrapper) th {
    background: var(--erp-c-surface-2);
    color: var(--erp-c-text-4);
    font-size: calc(11px * var(--erp-font-scale, 1));
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: 12px 24px;
  }
  
  :deep(.el-table__row) td {
    padding: 16px 24px;
  }
}

.org-user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.org-avatar-sm {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: calc(12px * var(--erp-font-scale, 1));
  font-weight: 800;
  
  &.is-primary { background: var(--erp-c-tint-blue); color: #0369a1; }
  &.is-tertiary { background: var(--erp-c-tint-orange); color: #c2410c; }
  &.is-secondary { background: var(--erp-c-fill); color: var(--erp-c-text-2); }
}

.org-user-info {
  .org-user-name { font-size: calc(14px * var(--erp-font-scale, 1)); font-weight: 700; color: var(--erp-c-text); margin: 0; }
  .org-user-email { font-size: calc(12px * var(--erp-font-scale, 1)); color: var(--erp-c-text-4); margin: 0; }
}

.org-code-cell {
  font-family: inherit;
  color: var(--erp-c-text-3);
}

/* ===== Dialog Styles ===== */
.wf-exec-dialog {
  :deep(.el-dialog) {
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  }
  
  :deep(.el-dialog__header) {
    padding: 24px 32px;
    margin: 0;
    border-bottom: 1px solid var(--erp-c-border);
    .el-dialog__title { font-weight: 800; font-size: calc(18px * var(--erp-font-scale, 1)); }
  }
  
  :deep(.el-dialog__body) {
    padding: 32px;
  }
  
  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--erp-c-text-2);
  }
}

.org-btn-submit {
  height: 40px;
  padding: 0 28px;
  border-radius: 10px;
  font-weight: 700;
  background: #0057c2;
  border: none;
}

@media (max-width: 1024px) {
  .org-bento-side { grid-column: span 12; }
  .org-bento-main { grid-column: span 12; }
  .org-hero-stats { grid-template-columns: 1fr; }
}
</style>

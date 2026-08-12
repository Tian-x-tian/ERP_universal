import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useUiPreferenceStore } from '@/store/uiPreference'
import { HOME_MENU_PATH } from '@/constants/default-menu'

const routes: RouteRecordRaw[] = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/index.vue'),
        meta: { title: '登录', isPublic: true },
    },
    {
        path: '/',
        name: 'Layout',
        component: () => import('@/layout/index.vue'),
        redirect: HOME_MENU_PATH,
        children: [
            {
                path: 'home',
                name: 'Home',
                component: () => import('@/views/home/index.vue'),
                meta: { title: '首页' },
            },
            {
                path: 'workbench/system-notice',
                name: 'WorkbenchSystemNotice',
                component: () => import('@/views/platform/todo-center/index.vue'),
                meta: { title: '系统消息', todoCenterTab: 'message' },
            },
            {
                path: 'workbench/process-todo',
                name: 'WorkbenchProcessTodo',
                component: () => import('@/views/platform/todo-center/index.vue'),
                meta: { title: '待办事项', todoCenterTab: 'todo' },
            },
            {
                path: 'workbench/attendance',
                name: 'WorkbenchAttendance',
                component: () => import('@/views/workbench/attendance/index.vue'),
                meta: { title: '签到' },
            },
            {
                path: 'platform',
                redirect: '/platform/overview',
            },
            {
                path: 'platform/overview',
                name: 'PlatformOverview',
                component: () => import('@/views/platform/overview/index.vue'),
                meta: { title: '底座概览' },
            },
            {
                path: 'workflow-center',
                redirect: '/workflow-center/definition',
            },
            {
                path: 'workflow-center/definition',
                name: 'WorkflowDefinition',
                component: () => import('@/views/platform/workflow/index.vue'),
                meta: { title: '流程定义', workflowTab: 'definition' },
            },
            {
                path: 'workflow-center/instance',
                name: 'WorkflowInstance',
                component: () => import('@/views/platform/workflow/index.vue'),
                meta: { title: '流程实例', workflowTab: 'instance' },
            },
            {
                path: 'platform/attachment',
                name: 'PlatformAttachment',
                component: () => import('@/views/platform/attachment/index.vue'),
                meta: { title: '附件中心' },
            },
            {
                path: 'platform/imex',
                name: 'PlatformImex',
                component: () => import('@/views/platform/imex/index.vue'),
                meta: { title: '导入导出中心' },
            },
            {
                path: 'platform/print-template',
                name: 'PlatformPrintTemplate',
                component: () => import('@/views/platform/print-template/index.vue'),
                meta: { title: '打印模板' },
            },
            {
                path: 'platform/report-center',
                name: 'PlatformReportCenter',
                component: () => import('@/views/platform/report-center/index.vue'),
                meta: { title: '报表中心' },
            },
            {
                path: 'system/tenant',
                name: 'Tenant',
                component: () => import('@/views/system/tenant/index.vue'),
                meta: { title: '租户管理' },
            },
            {
                path: 'system/org-structure',
                name: 'SystemOrgStructure',
                component: () => import('@/views/system/dept/index.vue'),
                meta: { title: '组织架构' },
            },
            {
                path: 'system/data-permission',
                name: 'SystemDataPermission',
                component: () => import('@/views/platform/data-scope/index.vue'),
                meta: { title: '数据权限' },
            },
            {
                path: 'system/code-rule',
                name: 'SystemCodeRule',
                component: () => import('@/views/platform/code-rule/index.vue'),
                meta: { title: '编号规则' },
            },
            {
                path: 'system/notice-manage',
                name: 'SystemNoticeManage',
                component: () => import('@/views/system/notice/index.vue'),
                meta: { title: '通知管理' },
            },
            {
                path: 'platform/region-data',
                name: 'PlatformRegionData',
                component: () => import('@/views/system/region/index.vue'),
                meta: { title: '区域数据' },
            },
            {
                path: 'platform/org-enhance',
                redirect: '/platform/org-enhance/company',
            },
            {
                path: 'platform/org-enhance/company',
                name: 'PlatformOrgEnhanceCompany',
                component: () => import('@/views/platform/org/index.vue'),
                meta: { title: '公司', orgSection: 'company' },
            },
            {
                path: 'platform/org-enhance/department',
                name: 'PlatformOrgEnhanceDepartment',
                component: () => import('@/views/platform/org/index.vue'),
                meta: { title: '部门', orgSection: 'department' },
            },
            {
                path: 'platform/org-enhance/position',
                name: 'PlatformOrgEnhancePosition',
                component: () => import('@/views/platform/org/index.vue'),
                meta: { title: '岗位', orgSection: 'position' },
            },
            {
                path: 'monitor/oper-log',
                name: 'MonitorOperLog',
                component: () => import('@/views/system/oper-log/index.vue'),
                meta: { title: '操作日志' },
            },
            {
                path: 'monitor/login-log',
                name: 'MonitorLoginLog',
                component: () => import('@/views/system/login-log/index.vue'),
                meta: { title: '登录日志' },
            },
            {
                path: 'monitor/audit-log',
                name: 'MonitorAuditLog',
                component: () => import('@/views/platform/audit-log/index.vue'),
                meta: { title: '审计日志' },
            },
                        {
                path: 'system/user',
                name: 'User',
                component: () => import('@/views/system/user/index.vue'),
                meta: { title: '用户管理' },
            },
            {
                path: 'system/company',
                name: 'Company',
                component: () => import('@/views/system/company/index.vue'),
                meta: { title: '公司管理' },
            },
            {
                path: 'system/post',
                name: 'Post',
                component: () => import('@/views/system/post/index.vue'),
                meta: { title: '岗位管理' },
            },
            {
                path: 'system/profile',
                name: 'Profile',
                component: () => import('@/views/system/profile/index.vue'),
                meta: { title: '个人中心' },
            },
            {
                path: 'system/role',
                name: 'Role',
                component: () => import('@/views/system/role/index.vue'),
                meta: { title: '角色管理' },
            },
            {
                path: 'system/menu',
                name: 'Menu',
                component: () => import('@/views/system/menu/index.vue'),
                meta: { title: '菜单管理' },
            },
            {
                path: 'system/dict',
                name: 'Dict',
                component: () => import('@/views/system/dict/index.vue'),
                meta: { title: '字典管理' },
            },
            {
                path: 'system/config',
                name: 'Config',
                component: () => import('@/views/system/config/index.vue'),
                meta: { title: '参数管理' },
            },
            {
                path: 'system/theme',
                name: 'SystemTheme',
                component: () => import('@/views/system/theme/index.vue'),
                meta: { title: 'UI设置' },
            },
            {
                path: 'system/ai-config',
                name: 'SystemAiConfig',
                component: () => import('@/views/system/ai-config/index.vue'),
                meta: { title: 'AI配置' },
            },
            {
                path: 'system/ai-panel',
                name: 'SystemAiPanel',
                component: () => import('@/views/system/ai-panel/index.vue'),
                meta: { title: 'AI面板' },
            },
            {
                path: 'system/mdm/customer',
                name: 'SystemMdmCustomer',
                component: () => import('@/views/system/mdm/customer/index.vue'),
                meta: { title: 'MDM-客户主数据' },
            },
            {
                path: 'system/mdm/supplier',
                name: 'SystemMdmSupplier',
                component: () => import('@/views/system/mdm/supplier/index.vue'),
                meta: { title: 'MDM-供应商主数据' },
            },
            {
                path: 'system/mdm/item',
                name: 'SystemMdmItem',
                component: () => import('@/views/system/mdm/item/index.vue'),
                meta: { title: 'MDM-物料主数据' },
            },
            {
                path: 'system/mdm/warehouse',
                name: 'SystemMdmWarehouse',
                component: () => import('@/views/system/mdm/warehouse/index.vue'),
                meta: { title: 'MDM-仓库主数据' },
            },
            {
                path: 'system/mdm/warehouse-area',
                name: 'SystemMdmWarehouseArea',
                component: () => import('@/views/system/mdm/warehouse-area/index.vue'),
                meta: { title: 'MDM-库区主数据' },
            },
            {
                path: 'system/mdm/warehouse-location',
                name: 'SystemMdmWarehouseLocation',
                component: () => import('@/views/system/mdm/warehouse-location/index.vue'),
                meta: { title: 'MDM-库位主数据' },
            },
            {
                path: 'system/mdm/employee',
                name: 'SystemMdmEmployee',
                component: () => import('@/views/system/mdm/employee/index.vue'),
                meta: { title: '员工档案' },
            },
            {
                path: 'system/mdm/dimension',
                redirect: '/system/mdm/dimension/org',
            },
            {
                path: 'system/mdm/dimension/org',
                name: 'SystemMdmDimensionOrg',
                component: () => import('@/views/system/mdm/dimension/index.vue'),
                meta: { title: '组织维度', dimensionSection: 'org' },
            },
            {
                path: 'system/mdm/dimension/cost-center',
                name: 'SystemMdmDimensionCostCenter',
                component: () => import('@/views/system/mdm/dimension/index.vue'),
                meta: { title: '成本中心', dimensionSection: 'cc' },
            },
            {
                path: 'system/mdm/dimension/project',
                name: 'SystemMdmDimensionProject',
                component: () => import('@/views/system/mdm/dimension/index.vue'),
                meta: { title: '项目维度', dimensionSection: 'project' },
            },
            {
                path: 'system/mdm/dict',
                name: 'SystemMdmDict',
                component: () => import('@/views/system/mdm/dict/index.vue'),
                meta: { title: 'MDM-基础字典' },
            },
            {
                path: 'system/mdm/trace',
                name: 'SystemMdmTrace',
                component: () => import('@/views/system/mdm/trace/index.vue'),
                meta: { title: 'MDM-变更追踪' },
            },
            {
                path: 'business/hr/employee',
                name: 'BusinessHrEmployee',
                component: () => import('@/views/business/hr/employee/index.vue'),
                meta: { title: '员工档案' },
            },
            {
                path: 'business/hr/contract',
                name: 'BusinessHrContract',
                component: () => import('@/views/business/hr/contract/index.vue'),
                meta: { title: '合同管理' },
            },
            {
                path: 'business/hr/document',
                name: 'BusinessHrDocument',
                component: () => import('@/views/business/hr/document/index.vue'),
                meta: { title: '电子档案' },
            },
            {
                path: 'business/hr/attendance',
                name: 'BusinessHrAttendance',
                component: () => import('@/views/business/hr/attendance/index.vue'),
                meta: { title: '出勤管理' },
            },
            {
                path: 'business/hr/payroll',
                name: 'BusinessHrPayroll',
                component: () => import('@/views/business/hr/payroll/index.vue'),
                meta: { title: '薪酬核算' },
            },
            {
                path: 'business/hr/performance',
                name: 'BusinessHrPerformance',
                component: () => import('@/views/business/hr/performance/index.vue'),
                meta: { title: '绩效考核' },
            },
            {
                path: 'business/hr/warning',
                name: 'BusinessHrWarning',
                component: () => import('@/views/business/hr/warning/index.vue'),
                meta: { title: '预警中心' },
            },
            {
                path: 'business/inventory/ledger',
                name: 'BusinessInventoryLedger',
                component: () => import('@/views/inventory/ledger/index.vue'),
                meta: { title: '库存台账' },
            },
            {
                path: 'business/inventory/inbound',
                name: 'BusinessInventoryInbound',
                component: () => import('@/views/inventory/inbound/index.vue'),
                meta: { title: '入库管理' },
            },
            {
                path: 'business/inventory/outbound',
                name: 'BusinessInventoryOutbound',
                component: () => import('@/views/inventory/outbound/index.vue'),
                meta: { title: '出库管理' },
            },
            {
                path: 'business/inventory/transfer',
                name: 'BusinessInventoryTransfer',
                component: () => import('@/views/inventory/transfer/index.vue'),
                meta: { title: '调拨管理' },
            },
            {
                path: 'business/inventory/move',
                name: 'BusinessInventoryMove',
                component: () => import('@/views/inventory/move/index.vue'),
                meta: { title: '移库管理' },
            },
            {
                path: 'business/inventory/freeze',
                name: 'BusinessInventoryFreeze',
                component: () => import('@/views/inventory/freeze/index.vue'),
                meta: { title: '冻结解冻' },
            },
            {
                path: 'business/inventory/adjust',
                name: 'BusinessInventoryAdjust',
                component: () => import('@/views/inventory/adjust/index.vue'),
                meta: { title: '库存调整' },
            },
            {
                path: 'business/inventory/stocktake',
                name: 'BusinessInventoryStocktake',
                component: () => import('@/views/inventory/stocktake/index.vue'),
                meta: { title: '盘点管理' },
            },
            {
                path: 'business/inventory/batch',
                name: 'BusinessInventoryBatch',
                component: () => import('@/views/inventory/batch/index.vue'),
                meta: { title: '批次查询' },
            },
            {
                path: 'business/inventory/serial',
                name: 'BusinessInventorySerial',
                component: () => import('@/views/inventory/serial/index.vue'),
                meta: { title: '序列号查询' },
            },
            {
                path: 'business/inventory/policy',
                name: 'BusinessInventoryPolicy',
                component: () => import('@/views/inventory/policy/index.vue'),
                meta: { title: '库存策略' },
            },
            {
                path: 'business/inventory/warning',
                name: 'BusinessInventoryWarning',
                component: () => import('@/views/inventory/warning/index.vue'),
                meta: { title: '预警中心' },
            },
            {
                path: 'business/inventory/report',
                name: 'BusinessInventoryReport',
                component: () => import('@/views/inventory/report/index.vue'),
                meta: { title: '库存报表' },
            },
            {
                path: 'business/inventory/kpi',
                name: 'BusinessInventoryKpi',
                component: () => import('@/views/inventory/kpi/index.vue'),
                meta: { title: '库存驾驶舱' },
            },
            {
                path: 'business/inventory/integration',
                name: 'BusinessInventoryIntegration',
                component: () => import('@/views/inventory/integration/index.vue'),
                meta: { title: '集成事件' },
            },
        ],
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const token = userStore.token
    const redirectToEntryHome = () => {
        const targetHomePath = userStore.homeEntryPath || HOME_MENU_PATH
        if (to.path === HOME_MENU_PATH && targetHomePath !== HOME_MENU_PATH) {
            next({ path: targetHomePath, replace: true })
            return true
        }
        return false
    }

    if (token) {
        if (to.path === '/login') {
            next({ path: '/' })
        } else {
            if (!userStore.infoLoaded) {
                try {
                    await userStore.getInfo()
                    await userStore.getRouters()
                    // 拉取服务端 UI 偏好（失败已在 store 内兜底，不阻断登录流程）
                    await useUiPreferenceStore().loadFromServer()
                    if (redirectToEntryHome()) {
                        return
                    }
                    next({ ...to, replace: true })
                } catch (err) {
                    userStore.logout()
                    next({ path: '/login' })
                }
            } else {
                if (redirectToEntryHome()) {
                    return
                }
                next()
            }
        }
    } else {
        if (to.path === '/login') {
            next()
        } else {
            next(`/login?redirect=${to.fullPath}`)
        }
    }
})

export default router

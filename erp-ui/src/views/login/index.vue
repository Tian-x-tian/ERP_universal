<template>
  <div class="login-page">
    <div class="login-page__overlay"></div>
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-panel__tag">ERP · Digital Governance</div>
        <h1>数智协同管理平台</h1>
        <p>聚焦合规治理、协同运营与绿色发展，打造可信、透明、高效的一体化管理体验。</p>
        <ul class="value-list">
          <li>合规稳健</li>
          <li>协同高效</li>
          <li>创新驱动</li>
          <li>绿色可持续</li>
        </ul>
      </section>

      <el-card class="login-card">
        <template #header>
          <div class="login-header">
            <h2>账号登录</h2>
            <p>欢迎进入 ERP 管理系统</p>
          </div>
        </template>
        <el-form :model="loginForm" :rules="loginRules" ref="loginRef" label-position="top">
          <el-form-item label="租户编号" prop="tenantId">
            <el-input v-model="loginForm.tenantId" placeholder="请输入租户编号" prefix-icon="OfficeBuilding" />
          </el-form-item>
          <el-form-item label="账号" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入账号" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
              立即登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const loginRef = ref()
const loading = ref(false)

const loginForm = reactive({
  tenantId: '',
  username: '',
  password: ''
})

const loginRules = {
  tenantId: [{ required: true, message: '请输入租户编号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginRef.value) return
  await loginRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        // 调用后端登录接口 (erp-auth)
        // 注意：此处 tenantId 会被 request.ts 拦截器注入请求头，但登录时由于未存 localStorage，需手动处理或后端接口参数包含
        const res: any = await request.post('/login', loginForm, {
            headers: { 'tenantId': loginForm.tenantId }
        })
        
        userStore.setToken(res.data.token)
        userStore.setTenantId(loginForm.tenantId)
        
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error: any) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: clamp(16px, 3vw, 32px);
  overflow: hidden;
  background-image:
    linear-gradient(118deg, rgba(6, 37, 89, 0.88) 0%, rgba(9, 73, 138, 0.74) 48%, rgba(12, 116, 156, 0.66) 100%),
    url('../../assets/login-values-bg.svg');
  background-size: cover;
  background-position: center;
}

.login-page__overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 12%, rgba(255, 143, 79, 0.2), transparent 32%),
    radial-gradient(circle at 18% 84%, rgba(255, 255, 255, 0.14), transparent 40%);
}

.login-shell {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: 1.08fr minmax(340px, 420px);
  gap: clamp(16px, 2.8vw, 36px);
  align-items: stretch;
}

.brand-panel {
  border-radius: 20px;
  padding: clamp(24px, 4vw, 48px);
  color: #f2f8ff;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: linear-gradient(145deg, rgba(6, 34, 80, 0.64), rgba(18, 82, 141, 0.4));
  backdrop-filter: blur(8px);
  box-shadow: 0 28px 64px rgba(0, 21, 59, 0.35);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24px;

  .brand-panel__tag {
    display: inline-flex;
    width: fit-content;
    padding: 6px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.16);
    color: #f8fbff;
    font-size: 12px;
    letter-spacing: 0.06em;
  }

  h1 {
    margin: 0;
    font-size: clamp(30px, 4vw, 46px);
    line-height: 1.2;
    letter-spacing: 0.01em;
    text-shadow: 0 6px 22px rgba(1, 24, 56, 0.42);
  }

  p {
    margin: 0;
    max-width: 520px;
    font-size: 16px;
    line-height: 1.8;
    color: rgba(242, 249, 255, 0.92);
  }
}

.value-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 10px 12px;

  li {
    padding: 10px 14px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.12);
    font-size: 14px;
    letter-spacing: 0.03em;
  }
}

.login-card {
  width: 100%;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 56px rgba(0, 21, 59, 0.3);
  align-self: center;
}

.login-header {
  text-align: center;

  h2 {
    margin: 0;
    color: #1f2f46;
    font-size: 30px;
  }

  p {
    margin: 10px 0 0;
    color: #5d6f85;
    font-size: 14px;
  }
}

:deep(.el-card__header) {
  border-bottom-color: rgba(27, 62, 108, 0.12);
}

:deep(.el-form-item__label) {
  color: #2f425b;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
}

.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 8px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 0.04em;
}

@media (max-width: 1100px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 640px;
  }

  .brand-panel {
    order: 2;
    gap: 16px;
  }

  .login-card {
    order: 1;
  }
}

@media (max-width: 768px) {
  .login-page {
    padding: 12px;
  }

  .login-shell {
    max-width: 460px;
    gap: 12px;
  }

  .brand-panel {
    padding: 16px;

    h1 {
      font-size: 24px;
    }

    p {
      font-size: 14px;
      line-height: 1.7;
    }
  }

  .value-list {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
}

@media (max-width: 540px) {
  .brand-panel {
    display: none;
  }

  .login-shell {
    max-width: 420px;
  }
}
</style>

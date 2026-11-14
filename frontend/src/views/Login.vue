<template>
  <div class="login-wrap">
    <div class="brand-side">
      <div class="brand-inner">
        <div class="mark">校园二手集市</div>
        <h1>把宿舍里闲置的东西<br />交给下一个需要它的人</h1>
        <p>校内实名认证 · 平台担保交易 · 7 天自动确认收货</p>
        <ul>
          <li>商家资质审核，商品人工过审</li>
          <li>货款平台托管，确认收货才打款</li>
          <li>收货 24 小时内可申请退货</li>
        </ul>
      </div>
    </div>

    <div class="form-side">
      <div class="form-box">
        <div class="tabs">
          <span :class="{ on: mode === 'login' }" @click="mode = 'login'">登录</span>
          <span :class="{ on: mode === 'register' }" @click="mode = 'register'">注册</span>
        </div>

        <!-- 登录 -->
        <el-form v-if="mode === 'login'" :model="loginForm" label-position="top" @submit.prevent>
          <el-form-item label="账号">
            <el-input v-model="loginForm.username" placeholder="请输入账号" size="large" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="doLogin" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
          <div class="tips">
            演示账号：买家 liming / 商家 wangji / 管理员 admin，密码均为 123456
          </div>
        </el-form>

        <!-- 注册 -->
        <el-form v-else :model="regForm" label-position="top">
          <el-form-item label="注册身份">
            <el-radio-group v-model="regForm.role">
              <el-radio-button label="USER">普通用户</el-radio-button>
              <el-radio-button label="MERCHANT">商家</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <div class="grid2">
            <el-form-item label="账号">
              <el-input v-model="regForm.username" placeholder="至少3位" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" show-password placeholder="至少6位" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="regForm.name" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="regForm.phone" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="regForm.email" />
            </el-form-item>
            <el-form-item label="城市">
              <el-input v-model="regForm.city" />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="regForm.gender" style="width:100%">
                <el-option :value="0" label="不透露" />
                <el-option :value="1" label="男" />
                <el-option :value="2" label="女" />
              </el-select>
            </el-form-item>
            <el-form-item label="银行账号（16位）">
              <el-input v-model="regForm.bankAccount" maxlength="16" />
            </el-form-item>
          </div>

          <template v-if="regForm.role === 'MERCHANT'">
            <el-form-item label="店铺名称">
              <el-input v-model="regForm.shopName" placeholder="例如：王记数码" />
            </el-form-item>
            <div class="grid2">
              <el-form-item label="营业执照">
                <el-upload :show-file-list="false" :http-request="(o) => upload(o, 'licenseImg')" accept="image/*">
                  <img v-if="regForm.licenseImg" :src="regForm.licenseImg" class="thumb" />
                  <div v-else class="uploader"><el-icon><Plus /></el-icon>上传</div>
                </el-upload>
              </el-form-item>
              <el-form-item label="身份证照片">
                <el-upload :show-file-list="false" :http-request="(o) => upload(o, 'idCardImg')" accept="image/*">
                  <img v-if="regForm.idCardImg" :src="regForm.idCardImg" class="thumb" />
                  <div v-else class="uploader"><el-icon><Plus /></el-icon>上传</div>
                </el-upload>
              </el-form-item>
            </div>
          </template>

          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input v-model="regForm.captchaCode" placeholder="不区分大小写" style="width:150px" />
              <img :src="captcha.image" class="captcha" title="点击刷新" @click="loadCaptcha" />
            </div>
          </el-form-item>

          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doRegister">提交注册</el-button>
          <div class="tips">注册后需管理员审核通过才能登录，商家还需上传营业执照与身份证。</div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'

const router = useRouter()
const route = useRoute()
const mode = ref('login')
const loading = ref(false)
const captcha = reactive({ key: '', image: '' })

const loginForm = reactive({ username: 'liming', password: '123456' })
const regForm = reactive({
  role: 'USER', username: '', password: '', name: '', phone: '', email: '', city: '北京',
  gender: 0, bankAccount: '', shopName: '', licenseImg: '', idCardImg: '', captchaKey: '', captchaCode: ''
})

async function loadCaptcha() {
  const data = await http.get('/auth/captcha')
  captcha.key = data.captchaKey
  captcha.image = data.image
  regForm.captchaKey = data.captchaKey
}

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await http.post('/auth/login', loginForm)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    ElMessage.success('欢迎回来，' + data.user.name)
    const role = data.user.role
    if (role === 'ADMIN') router.push('/admin')
    else if (role === 'MERCHANT') router.push('/merchant')
    else router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

async function upload(option, field) {
  const form = new FormData()
  form.append('file', option.file)
  const data = await http.post('/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  regForm[field] = data.url
  ElMessage.success('上传成功')
}

async function doRegister() {
  if (!regForm.username || !regForm.password || !regForm.name) {
    ElMessage.warning('账号、密码、姓名为必填')
    return
  }
  if (!regForm.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await http.post('/auth/register', regForm)
    ElMessage.success('注册成功，请等待管理员审核')
    mode.value = 'login'
    loginForm.username = regForm.username
    loginForm.password = ''
    regForm.captchaCode = ''
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-wrap {
  display: flex;
  min-height: 100vh;
}
.brand-side {
  width: 46%;
  background: linear-gradient(150deg, #0f766e 0%, #115e59 55%, #134e4a 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 60px;
}
.mark {
  display: inline-block;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 13px;
  letter-spacing: 2px;
  margin-bottom: 28px;
}
.brand-inner h1 {
  font-size: 34px;
  line-height: 1.5;
  margin: 0 0 20px;
  font-weight: 600;
}
.brand-inner p {
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 30px;
}
.brand-inner ul {
  padding-left: 18px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 2;
}
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.form-box {
  width: 460px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 12px 40px rgba(15, 118, 110, 0.08);
}
.tabs {
  display: flex;
  gap: 28px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--line);
}
.tabs span {
  padding: 10px 0;
  font-size: 18px;
  color: var(--muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
}
.tabs span.on {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 600;
}
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.captcha {
  height: 40px;
  border-radius: 6px;
  border: 1px solid var(--line);
  cursor: pointer;
}
.uploader {
  width: 120px;
  height: 76px;
  border: 1px dashed #cfd6dd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  font-size: 12px;
  gap: 4px;
}
.thumb {
  width: 120px;
  height: 76px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--line);
}
.tips {
  margin-top: 14px;
  font-size: 12px;
  color: var(--muted);
  line-height: 1.7;
}
</style>

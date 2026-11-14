<template>
  <header class="header">
    <div class="inner">
      <div class="logo" @click="$router.push('/')">
        <span class="dot"></span>
        校园二手集市
      </div>
      <nav class="nav">
        <router-link to="/" :class="{ on: $route.path === '/' }">首页</router-link>
        <router-link to="/cart" :class="{ on: $route.path === '/cart' }">
          购物车
          <em v-if="cartCount">{{ cartCount }}</em>
        </router-link>
        <router-link to="/orders" :class="{ on: $route.path === '/orders' }">我的订单</router-link>
        <router-link to="/wallet" :class="{ on: $route.path === '/wallet' }">钱包</router-link>
        <router-link v-if="user && user.role === 'MERCHANT'" to="/merchant" :class="{ on: $route.path === '/merchant' }">商家中心</router-link>
        <router-link v-if="user && user.role === 'ADMIN'" to="/admin" :class="{ on: $route.path === '/admin' }">后台管理</router-link>
      </nav>
      <div class="right">
        <template v-if="user">
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="28" style="background:#0f766e">{{ user.name.slice(0, 1) }}</el-avatar>
              <span class="uname">{{ user.name }}</span>
              <el-tag size="small" effect="plain" style="margin-left:6px">{{ roleText }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="wallet">我的钱包</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" round @click="$router.push('/login')">登录 / 注册</el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import http from '../api'

const router = useRouter()
const route = useRoute()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const cartCount = ref(0)

const roleText = computed(() => {
  const map = { USER: '买家', MERCHANT: '商家', ADMIN: '管理员' }
  return map[user.value?.role] || ''
})

async function loadCart() {
  user.value = JSON.parse(localStorage.getItem('user') || 'null')
  if (!user.value) {
    cartCount.value = 0
    return
  }
  try {
    const list = await http.get('/cart')
    cartCount.value = list.length
  } catch (e) {
    cartCount.value = 0
  }
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    user.value = null
    router.push('/login')
  } else if (cmd === 'wallet') {
    router.push('/wallet')
  }
}

watch(() => route.fullPath, loadCart)
onMounted(loadCart)
</script>

<style scoped>
.header {
  background: #fff;
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 20;
}
.inner {
  max-width: 1180px;
  margin: 0 auto;
  height: 64px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 28px;
}
.logo {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--brand);
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  background: var(--accent);
  display: inline-block;
  transform: rotate(45deg);
}
.nav {
  display: flex;
  gap: 22px;
  flex: 1;
}
.nav a {
  color: var(--muted);
  padding: 6px 0;
  border-bottom: 2px solid transparent;
  font-size: 15px;
  position: relative;
}
.nav a.on {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 600;
}
.nav em {
  position: absolute;
  top: -4px;
  right: -14px;
  background: #e5484d;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  border-radius: 8px;
  padding: 0 5px;
  line-height: 15px;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.uname { font-size: 14px; }
</style>

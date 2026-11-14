<template>
  <div class="page">
    <h3 class="section-title">后台管理</h3>

    <div class="stats">
      <div class="s-card"><span>注册用户</span><b>{{ stats.userCount }}</b></div>
      <div class="s-card"><span>入驻商家</span><b>{{ stats.merchantCount }}</b></div>
      <div class="s-card warn"><span>待审核用户</span><b>{{ stats.pendingUsers }}</b></div>
      <div class="s-card warn"><span>待审核商品</span><b>{{ stats.pendingProducts }}</b></div>
      <div class="s-card"><span>累计交易额</span><b>¥{{ stats.gmv }}</b></div>
      <div class="s-card"><span>平台服务费收入</span><b>¥{{ stats.feeIncome }}</b></div>
    </div>

    <el-tabs v-model="tab">
      <!-- 用户审核 -->
      <el-tab-pane label="注册审核" name="audit">
        <el-table :data="pendingUsers" stripe>
          <el-table-column prop="username" label="账号" width="110" />
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column label="身份" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.role === 'MERCHANT' ? 'warning' : 'info'">
                {{ row.role === 'MERCHANT' ? '商家' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="shopName" label="店铺" width="120" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="bankAccount" label="银行账号" width="150" />
          <el-table-column label="资质材料" width="150">
            <template #default="{ row }">
              <template v-if="row.licenseImg">
                <el-button link type="primary" @click="preview(row.licenseImg)">营业执照</el-button>
                <el-button link type="primary" @click="preview(row.idCardImg)">身份证</el-button>
              </template>
              <span v-else class="muted">个人用户无需资质</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
              <el-button size="small" type="danger" plain @click="audit(row, false)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!pendingUsers.length" description="没有待审核的注册申请" />
      </el-tab-pane>

      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索账号 / 姓名 / 店铺" style="width:240px" clearable @change="loadUsers" />
          <el-select v-model="roleFilter" placeholder="全部身份" clearable style="width:140px" @change="loadUsers">
            <el-option value="USER" label="普通用户" />
            <el-option value="MERCHANT" label="商家" />
            <el-option value="ADMIN" label="管理员" />
          </el-select>
        </div>
        <el-table :data="users" stripe>
          <el-table-column prop="username" label="账号" width="110" />
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column label="身份" width="100">
            <template #default="{ row }">{{ roleText(row.role) }}</template>
          </el-table-column>
          <el-table-column prop="shopName" label="店铺" width="120" />
          <el-table-column label="等级" width="90">
            <template #default="{ row }">
              <span v-if="row.role === 'MERCHANT'">Lv.{{ row.merchantLevel }}</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="balance" label="余额" width="100" />
          <el-table-column prop="points" label="积分" width="80" />
          <el-table-column prop="creditScore" label="好评率" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openRecharge(row)">充值</el-button>
              <el-button v-if="row.role === 'MERCHANT'" link type="warning" @click="openLevel(row)">设等级</el-button>
              <el-button link type="danger" @click="ban(row)">{{ row.status === 'BANNED' ? '解封' : '封禁' }}</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 商品审核 -->
      <el-tab-pane label="商品审核" name="products">
        <el-radio-group v-model="productStatus" style="margin-bottom:14px" @change="loadProducts">
          <el-radio-button label="PENDING">待审核</el-radio-button>
          <el-radio-button label="ON_SALE">在售</el-radio-button>
          <el-radio-button label="OFF_SHELF">已下架</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
        </el-radio-group>
        <el-table :data="products" stripe>
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="p-cell">
                <img :src="row.imageList[0]" />
                <div>
                  <div>{{ row.name }}</div>
                  <div class="muted">{{ row.shopName }} · {{ row.conditionLevel }} · 库存 {{ row.stock }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'PENDING' ? 'warning' : 'success'">{{ pStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="auditProduct(row, true)">通过</el-button>
                <el-button size="small" type="danger" plain @click="auditProduct(row, false)">驳回</el-button>
              </template>
              <el-button v-else-if="row.status === 'ON_SALE'" link type="danger" @click="auditProduct(row, false)">强制下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="editVisible" title="编辑用户" width="460px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.city" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rechargeVisible" title="账户充值" width="380px">
      <div class="muted" style="margin-bottom:10px">给 {{ current?.name }} 的钱包增加余额</div>
      <el-input-number v-model="rechargeAmount" :min="1" :step="100" style="width:100%" />
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="levelVisible" title="设置商家等级" width="380px">
      <el-rate v-model="levelValue" :max="5" show-score score-template="Lv.{value}" />
      <div class="muted" style="margin-top:10px">
        等级决定平台服务费：Lv.1 0.1% / Lv.2 0.2% / Lv.3 0.5% / Lv.4 0.75% / Lv.5 1%
      </div>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLevel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const tab = ref('audit')
const stats = ref({ userCount: 0, merchantCount: 0, pendingUsers: 0, pendingProducts: 0, gmv: 0, feeIncome: 0 })
const pendingUsers = ref([])
const users = ref([])
const products = ref([])
const keyword = ref('')
const roleFilter = ref('')
const productStatus = ref('PENDING')
const editVisible = ref(false)
const rechargeVisible = ref(false)
const levelVisible = ref(false)
const current = ref(null)
const rechargeAmount = ref(100)
const levelValue = ref(1)
const editForm = reactive({ id: null, name: '', phone: '', email: '', city: '' })

const roleText = (r) => ({ USER: '普通用户', MERCHANT: '商家', ADMIN: '管理员' }[r] || r)
const statusText = (s) => ({ PENDING: '待审核', ACTIVE: '正常', REJECTED: '已驳回', BANNED: '已封禁' }[s] || s)
const statusType = (s) => ({ PENDING: 'warning', ACTIVE: 'success', REJECTED: 'info', BANNED: 'danger' }[s] || '')
const pStatus = (s) => ({ PENDING: '待审核', ON_SALE: '在售', OFF_SHELF: '已下架', SOLD: '已售出' }[s] || s)

async function loadStats() {
  stats.value = await http.get('/admin/stats')
}

async function loadPending() {
  pendingUsers.value = await http.get('/admin/users', { params: { status: 'PENDING' } })
}

async function loadUsers() {
  users.value = await http.get('/admin/users', { params: { role: roleFilter.value, keyword: keyword.value } })
}

async function loadProducts() {
  products.value = await http.get('/admin/products', { params: { status: productStatus.value } })
}

async function audit(row, pass) {
  await http.post(`/admin/users/${row.id}/audit`, { pass })
  ElMessage.success(pass ? '已通过' : '已驳回')
  loadStats(); loadPending(); loadUsers()
}

async function auditProduct(row, pass) {
  await http.post(`/admin/products/${row.id}/audit`, { pass })
  ElMessage.success(pass ? '已上架' : '已下架')
  loadStats(); loadProducts()
}

function openEdit(row) {
  current.value = row
  Object.assign(editForm, { id: row.id, name: row.name, phone: row.phone, email: row.email, city: row.city })
  editVisible.value = true
}

async function saveEdit() {
  await http.put('/admin/users', editForm)
  ElMessage.success('已保存')
  editVisible.value = false
  loadUsers()
}

function openRecharge(row) {
  current.value = row
  rechargeAmount.value = 100
  rechargeVisible.value = true
}

async function doRecharge() {
  await http.post(`/admin/users/${current.value.id}/recharge`, { amount: rechargeAmount.value })
  ElMessage.success('充值成功')
  rechargeVisible.value = false
  loadUsers()
}

function openLevel(row) {
  current.value = row
  levelValue.value = row.merchantLevel
  levelVisible.value = true
}

async function saveLevel() {
  await http.put(`/admin/users/${current.value.id}/level`, { level: levelValue.value })
  ElMessage.success('等级已更新')
  levelVisible.value = false
  loadUsers()
}

async function ban(row) {
  const days = row.status === 'BANNED' ? 0 : 7
  await ElMessageBox.confirm(days ? `封禁 ${row.name} 7 天？封禁期内不能发布商品和下单。` : `解除 ${row.name} 的封禁？`, '操作确认')
  await http.post(`/admin/users/${row.id}/ban`, { days })
  ElMessage.success('操作成功')
  loadUsers()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户 ${row.name}？该操作不可恢复。`, '危险操作', { type: 'warning' })
  await http.delete(`/admin/users/${row.id}`)
  ElMessage.success('已删除')
  loadUsers(); loadStats()
}

function preview(url) {
  window.open(url, '_blank')
}

onMounted(() => {
  loadStats(); loadPending(); loadUsers(); loadProducts()
})
</script>

<style scoped>
.stats { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; margin-bottom: 20px; }
.s-card { background: #fff; border: 1px solid var(--line); border-radius: 12px; padding: 14px 16px; }
.s-card span { color: var(--muted); font-size: 12px; }
.s-card b { display: block; font-size: 21px; margin-top: 6px; color: var(--brand); }
.s-card.warn b { color: #e5484d; }
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; }
.p-cell { display: flex; align-items: center; gap: 12px; }
.p-cell img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; }
</style>

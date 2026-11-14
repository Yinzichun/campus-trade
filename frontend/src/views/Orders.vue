<template>
  <div class="page">
    <h3 class="section-title">我的订单</h3>
    <el-tabs v-model="status" @tab-change="load">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待收货" name="PAID" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="退货申请中" name="RETURN_REQUESTED" />
      <el-tab-pane label="已退货" name="RETURNED" />
    </el-tabs>

    <div v-if="!orders.length" class="card" style="text-align:center;padding:50px">
      <el-empty description="暂时没有订单" />
    </div>

    <div v-for="o in orders" :key="o.id" class="card order">
      <div class="o-head">
        <span class="o-no">订单号 {{ o.orderNo }}</span>
        <span class="muted">{{ o.createdAt }}</span>
        <span class="muted">店铺：{{ o.shopName }}</span>
        <el-tag :type="statusType(o.status)" size="small">{{ statusText(o.status) }}</el-tag>
      </div>
      <div class="o-body">
        <img :src="o.productImage" class="thumb" @click="$router.push('/product/' + o.productId)" />
        <div class="o-info">
          <div class="o-name">{{ o.productName }}</div>
          <div class="muted">数量 {{ o.quantity }} · 手续费 {{ o.fee }} 元
            <span v-if="Number(o.pointsDeduct) > 0"> · 积分抵扣 {{ o.pointsDeduct }} 元</span>
          </div>
          <div class="muted" v-if="o.status === 'PAID'">卖家 7 天内未确认将自动结算：{{ o.confirmDeadline }}</div>
          <div class="muted" v-if="o.status === 'COMPLETED' && o.returnDeadline">退货截止：{{ o.returnDeadline }}</div>
          <div class="muted" v-if="o.returnReason">退货原因：{{ o.returnReason }}</div>
        </div>
        <div class="o-amount">¥{{ o.amount }}</div>
      </div>
      <div class="o-actions">
        <el-button v-if="o.status === 'PAID'" type="primary" size="small" @click="confirm(o)">确认收货</el-button>
        <el-button v-if="o.status === 'COMPLETED' && !o.reviewed" type="primary" size="small" plain @click="openReview(o)">评价</el-button>
        <el-button v-if="o.status === 'COMPLETED' && canReturn(o)" size="small" @click="openReturn(o)">申请退货</el-button>
        <el-button size="small" link @click="$router.push('/product/' + o.productId)">再次购买</el-button>
      </div>
    </div>

    <el-dialog v-model="reviewVisible" title="评价这笔交易" width="480px">
      <div class="rv-line">
        <span>商品评分</span>
        <el-rate v-model="review.productRating" />
      </div>
      <el-input v-model="review.productContent" type="textarea" :rows="3" placeholder="说说商品的实际情况，给后面的同学一个参考" />
      <div class="rv-line" style="margin-top:16px">
        <span>商家服务</span>
        <el-rate v-model="review.merchantRating" />
      </div>
      <el-input v-model="review.merchantContent" type="textarea" :rows="2" placeholder="发货速度、沟通态度怎么样" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const status = ref('')
const orders = ref([])
const reviewVisible = ref(false)
const current = ref(null)
const review = reactive({ productRating: 5, productContent: '', merchantRating: 5, merchantContent: '' })

const STATUS = {
  UNPAID: '待付款', PAID: '待收货', COMPLETED: '已完成',
  RETURN_REQUESTED: '退货申请中', RETURNED: '已退货', CANCELLED: '已取消'
}
const statusText = (s) => STATUS[s] || s
const statusType = (s) => ({ PAID: 'warning', COMPLETED: 'success', RETURN_REQUESTED: 'danger', RETURNED: 'info' }[s] || '')

async function load() {
  orders.value = await http.get('/orders', { params: { status: status.value } })
}

function canReturn(o) {
  if (!o.returnDeadline) return false
  return new Date(o.returnDeadline.replace(' ', 'T')).getTime() > Date.now()
}

async function confirm(o) {
  await ElMessageBox.confirm('确认已经拿到商品？确认后货款会结算给商家，24 小时内仍可申请退货。', '确认收货')
  await http.post(`/orders/${o.id}/confirm`)
  ElMessage.success('已确认收货')
  load()
}

function openReturn(o) {
  ElMessageBox.prompt('请填写退货原因（例如：实物与描述不符）', '申请退货', {
    inputPlaceholder: '退货原因'
  }).then(async ({ value }) => {
    await http.post(`/orders/${o.id}/return`, { reason: value })
    ElMessage.success('退货申请已提交，等待商家处理')
    load()
  }).catch(() => {})
}

function openReview(o) {
  current.value = o
  review.productRating = 5
  review.merchantRating = 5
  review.productContent = ''
  review.merchantContent = ''
  reviewVisible.value = true
}

async function submitReview() {
  await http.post(`/orders/${current.value.id}/review`, review)
  ElMessage.success('评价成功')
  reviewVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.order { margin-bottom: 14px; padding: 0; }
.o-head {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--line);
  background: #fafbfc;
  font-size: 13px;
}
.o-no { font-weight: 600; }
.o-head .el-tag { margin-left: auto; }
.o-body { display: flex; align-items: center; gap: 16px; padding: 16px 18px; }
.thumb { width: 70px; height: 70px; border-radius: 8px; object-fit: cover; cursor: pointer; }
.o-info { flex: 1; }
.o-name { font-size: 15px; margin-bottom: 6px; }
.o-amount { font-size: 20px; color: #e5484d; font-weight: 700; }
.o-actions { padding: 0 18px 14px; display: flex; justify-content: flex-end; gap: 8px; }
.rv-line { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; }
</style>

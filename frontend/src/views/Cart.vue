<template>
  <div class="page">
    <h3 class="section-title">购物车</h3>
    <div v-if="!groups.length" class="card" style="text-align:center;padding:60px">
      <el-empty description="购物车还是空的">
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>

    <template v-else>
      <div v-for="g in groups" :key="g.merchantId" class="card group">
        <div class="g-head">
          <el-icon><Shop /></el-icon>
          <span class="g-name">{{ g.shopName }}</span>
          <el-tag size="small" effect="plain">{{ g.items.length }} 件商品</el-tag>
        </div>
        <div v-for="item in g.items" :key="item.id" class="row">
          <el-checkbox v-model="selected" :label="item.id" :disabled="item.product.status !== 'ON_SALE'">
            <span></span>
          </el-checkbox>
          <img :src="item.product.imageList[0]" class="thumb" @click="$router.push('/product/' + item.productId)" />
          <div class="r-info">
            <div class="r-name">{{ item.product.name }}</div>
            <div class="muted">{{ item.product.conditionLevel }} · {{ item.product.size || '标准规格' }}</div>
            <el-tag v-if="item.product.status !== 'ON_SALE'" size="small" type="info">已下架</el-tag>
          </div>
          <div class="r-price">¥{{ item.product.price }}</div>
          <el-input-number v-model="item.quantity" :min="1" :max="item.product.stock" size="small" @change="(v) => changeQty(item, v)" />
          <div class="r-sub">¥{{ (item.product.price * item.quantity).toFixed(2) }}</div>
          <el-button link type="danger" @click="remove(item)">删除</el-button>
        </div>
      </div>

      <div class="card bar">
        <div class="bar-left">
          <el-checkbox v-model="allChecked" @change="toggleAll">全选</el-checkbox>
          <span class="muted">已选 {{ selected.length }} 件</span>
        </div>
        <div class="bar-right">
          <div class="points">
            <span class="muted">使用积分</span>
            <el-input-number v-model="usePoints" :min="0" :max="maxPoints" :step="100" size="small" controls-position="right" />
            <span class="muted">共 {{ points }} 分，100 分抵 1 元</span>
          </div>
          <div class="total">
            合计：<span class="price">¥{{ payable }}</span>
            <span class="muted" v-if="usePoints">（已抵 ¥{{ (usePoints / 100).toFixed(2) }}）</span>
          </div>
          <el-button type="primary" size="large" :disabled="!selected.length" @click="checkout">去结算</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'
import http from '../api'

const router = useRouter()
const items = ref([])
const selected = ref([])
const points = ref(0)
const usePoints = ref(0)
const allChecked = ref(false)

const groups = computed(() => {
  const map = new Map()
  for (const it of items.value) {
    const key = it.product.merchantId
    if (!map.has(key)) {
      map.set(key, { merchantId: key, shopName: it.product.shopName, items: [] })
    }
    map.get(key).items.push(it)
  }
  return Array.from(map.values())
})

const total = computed(() =>
  items.value.filter(i => selected.value.includes(i.id))
    .reduce((s, i) => s + i.product.price * i.quantity, 0)
)
const maxPoints = computed(() => Math.min(points.value, Math.floor(total.value * 100)))
const payable = computed(() => {
  const p = Math.min(usePoints.value, maxPoints.value)
  return Math.max(0.01, total.value - p / 100).toFixed(2)
})

async function load() {
  items.value = await http.get('/cart')
  const wallet = await http.get('/wallet')
  points.value = wallet.points
  selected.value = items.value.filter(i => i.product.status === 'ON_SALE').map(i => i.id)
  allChecked.value = selected.value.length > 0 && selected.value.length === items.value.filter(i => i.product.status === 'ON_SALE').length
}

async function changeQty(item, v) {
  await http.put('/cart/' + item.id, { quantity: v })
}

async function remove(item) {
  await http.delete('/cart/' + item.id)
  items.value = items.value.filter(i => i.id !== item.id)
  selected.value = selected.value.filter(id => id !== item.id)
  ElMessage.success('已移除')
}

function toggleAll(v) {
  selected.value = v ? items.value.filter(i => i.product.status === 'ON_SALE').map(i => i.id) : []
}

async function checkout() {
  await ElMessageBox.confirm(`本次将支付 ¥${payable.value}，确认下单？`, '确认结算', { type: 'warning' })
  await http.post('/orders/checkout', { cartItemIds: selected.value, usePoints: usePoints.value })
  ElMessage.success('下单成功')
  router.push('/orders')
}

onMounted(load)
</script>

<style scoped>
.group { margin-bottom: 16px; padding: 0; overflow: hidden; }
.g-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: #f8fafb;
  border-bottom: 1px solid var(--line);
}
.g-name { font-weight: 600; }
.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #f2f4f5;
}
.row:last-child { border-bottom: none; }
.thumb { width: 76px; height: 76px; border-radius: 8px; object-fit: cover; cursor: pointer; }
.r-info { flex: 1; }
.r-name { font-size: 15px; margin-bottom: 6px; }
.r-price { width: 90px; color: var(--muted); }
.r-sub { width: 100px; text-align: right; font-weight: 600; color: #e5484d; }
.bar { display: flex; justify-content: space-between; align-items: center; position: sticky; bottom: 16px; }
.bar-left { display: flex; align-items: center; gap: 16px; }
.bar-right { display: flex; align-items: center; gap: 24px; }
.points { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.total { font-size: 15px; }
.total .price { font-size: 24px; }
</style>

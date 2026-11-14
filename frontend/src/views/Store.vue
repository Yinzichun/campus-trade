<template>
  <div class="page" v-if="merchant.id">
    <div class="store-head card">
      <el-avatar :size="64" style="background:#0f766e;font-size:24px">{{ merchant.shopName?.slice(0, 1) }}</el-avatar>
      <div class="meta">
        <h2>{{ merchant.shopName }}
          <el-tag size="small" type="warning" effect="plain">Lv.{{ merchant.merchantLevel }} 商家</el-tag>
        </h2>
        <div class="muted">店主 {{ merchant.name }} · {{ merchant.city }} · 好评率 {{ merchant.creditScore }}%</div>
      </div>
      <div class="nums">
        <div><b>{{ productCount }}</b><span>在售商品</span></div>
        <div><b>{{ totalSales }}</b><span>累计销量</span></div>
      </div>
    </div>

    <h3 class="section-title" style="margin-top:24px">店铺商品</h3>
    <div class="grid">
      <div v-for="p in products" :key="p.id" class="p-card" @click="$router.push('/product/' + p.id)">
        <img :src="p.imageList[0]" />
        <div class="p-body">
          <div class="p-name">{{ p.name }}</div>
          <div class="price">¥{{ p.price }}</div>
          <div class="muted">{{ p.conditionLevel }} · 已售 {{ p.sales }}</div>
        </div>
      </div>
    </div>
    <el-empty v-if="!products.length" description="店铺暂时没有商品" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api'

const route = useRoute()
const merchant = ref({})
const products = ref([])
const totalSales = ref(0)
const productCount = ref(0)

async function load() {
  const data = await http.get('/merchant/store/' + route.params.id)
  merchant.value = data.merchant
  products.value = data.products
  totalSales.value = data.totalSales
  productCount.value = data.productCount
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<style scoped>
.store-head { display: flex; align-items: center; gap: 20px; }
.meta { flex: 1; }
.meta h2 { margin: 0 0 8px; display: flex; align-items: center; gap: 10px; }
.nums { display: flex; gap: 40px; text-align: center; }
.nums b { display: block; font-size: 22px; color: var(--brand); }
.nums span { font-size: 12px; color: var(--muted); }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.p-card { background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid var(--line); cursor: pointer; }
.p-card img { width: 100%; height: 170px; object-fit: cover; }
.p-body { padding: 10px 12px 14px; }
.p-name { font-size: 14px; height: 40px; overflow: hidden; }
.p-body .price { font-size: 18px; margin: 6px 0 4px; }
</style>

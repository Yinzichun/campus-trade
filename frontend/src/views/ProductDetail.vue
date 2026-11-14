<template>
  <div class="page" v-if="product.id">
    <el-breadcrumb separator="/" style="margin-bottom:16px">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>{{ product.categoryName }}</el-breadcrumb-item>
      <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="detail card">
      <div class="gallery">
        <img :src="activeImg" class="main-img" />
        <div class="thumbs">
          <img v-for="(img, i) in product.imageList" :key="i" :src="img" :class="{ on: activeImg === img }" @click="activeImg = img" />
        </div>
      </div>

      <div class="info">
        <h1>{{ product.name }}</h1>
        <div class="price-row">
          <span class="price">¥{{ product.price }}</span>
          <s v-if="product.originalPrice">原价 ¥{{ product.originalPrice }}</s>
          <el-tag type="warning" effect="plain" v-if="product.negotiable">允许议价</el-tag>
        </div>
        <div class="attrs">
          <div><span>成色</span>{{ product.conditionLevel }}</div>
          <div><span>规格</span>{{ product.size || '—' }}</div>
          <div><span>库存</span>{{ product.stock }} 件</div>
          <div><span>销量</span>{{ product.sales }} 件</div>
          <div><span>分类</span>{{ product.categoryName }}</div>
          <div><span>评分</span>{{ product.ratingAvg }} 分（{{ product.ratingCount }} 条评价）</div>
        </div>

        <div class="shop" @click="$router.push('/store/' + product.merchantId)">
          <el-avatar :size="40" style="background:#0f766e">{{ product.shopName?.slice(0, 1) }}</el-avatar>
          <div class="shop-info">
            <div class="shop-name">{{ product.shopName }}</div>
            <div class="muted">店主 {{ product.merchantName }} · 点击进入店铺</div>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>

        <div class="buy-row">
          <el-input-number v-model="quantity" :min="1" :max="product.stock" />
          <el-button size="large" :icon="ShoppingCart" @click="addCart">加入购物车</el-button>
          <el-button size="large" type="primary" :disabled="product.status !== 'ON_SALE'" @click="buyNow">
            {{ product.status === 'ON_SALE' ? '立即购买' : '已售出' }}
          </el-button>
        </div>
        <div class="tips">校内面交，下单后货款由平台托管，确认收货后才会结算给商家。</div>
      </div>
    </div>

    <div class="card" style="margin-top:18px">
      <h3 class="section-title">商品说明</h3>
      <p class="desc">{{ product.description || '卖家没有填写说明' }}</p>
    </div>

    <div class="card" style="margin-top:18px">
      <h3 class="section-title">买家评价（{{ reviews.length }}）</h3>
      <div v-if="!reviews.length" class="muted">还没有评价，买下它你就是第一个。</div>
      <div v-for="r in reviews" :key="r.id" class="review">
        <div class="r-head">
          <span class="r-user">{{ r.buyerName }}</span>
          <el-rate :model-value="r.rating" disabled size="small" />
          <span class="muted">{{ r.createdAt }}</span>
        </div>
        <p>{{ r.content }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, ArrowRight } from '@element-plus/icons-vue'
import http from '../api'

const route = useRoute()
const router = useRouter()
const product = ref({})
const reviews = ref([])
const activeImg = ref('')
const quantity = ref(1)

async function load() {
  product.value = await http.get('/products/' + route.params.id)
  activeImg.value = product.value.imageList?.[0]
  reviews.value = await http.get('/products/' + route.params.id + '/reviews')
}

async function addCart() {
  await http.post('/cart', { productId: product.value.id, quantity: quantity.value })
  ElMessage.success('已加入购物车')
}

async function buyNow() {
  if (!localStorage.getItem('token')) {
    router.push('/login')
    return
  }
  await http.post('/orders/checkout', { productId: product.value.id, quantity: quantity.value, usePoints: 0 })
  ElMessage.success('下单成功，等待商家发货')
  router.push('/orders')
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<style scoped>
.detail { display: flex; gap: 32px; }
.gallery { width: 440px; flex-shrink: 0; }
.main-img {
  width: 440px;
  height: 400px;
  object-fit: cover;
  border-radius: 12px;
  background: #f1f3f4;
}
.thumbs { display: flex; gap: 8px; margin-top: 10px; }
.thumbs img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
}
.thumbs img.on { border-color: var(--brand); }
.info { flex: 1; }
.info h1 { font-size: 22px; margin: 0 0 14px; line-height: 1.5; }
.price-row { display: flex; align-items: center; gap: 12px; }
.price-row .price { font-size: 30px; }
.price-row s { color: #b0b6bd; font-size: 13px; }
.attrs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 20px;
  margin: 18px 0;
  padding: 16px;
  background: #f8fafb;
  border-radius: 10px;
  font-size: 13px;
}
.attrs span { color: var(--muted); margin-right: 10px; }
.shop {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  cursor: pointer;
}
.shop-info { flex: 1; }
.shop-name { font-weight: 600; }
.buy-row { display: flex; gap: 12px; margin: 22px 0 12px; align-items: center; }
.tips { font-size: 12px; color: var(--muted); }
.desc { line-height: 2; color: #374151; white-space: pre-wrap; }
.review { border-bottom: 1px dashed var(--line); padding: 14px 0; }
.review:last-child { border-bottom: none; }
.r-head { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.r-user { font-weight: 600; }
.review p { margin: 8px 0 0; color: #374151; line-height: 1.8; }
</style>

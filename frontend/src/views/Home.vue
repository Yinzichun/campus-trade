<template>
  <div>
    <div class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h2>校内闲置，当天就能出手</h2>
          <p>校内实名店铺 · 商品人工审核 · 平台担保交易 · 收货 24 小时内可退货</p>
        </div>
        <div class="search-box">
          <el-input v-model="keyword" size="large" placeholder="搜一搜：耳机、教材、自行车……" clearable @keyup.enter="search">
            <template #append>
              <el-button :icon="Search" @click="search">搜索</el-button>
            </template>
          </el-input>
          <div class="hot">
            热门：
            <a v-for="k in hotWords" :key="k" @click="quickSearch(k)">{{ k }}</a>
          </div>
        </div>
      </div>
    </div>

    <div class="page">
      <!-- 推荐位轮播 -->
      <div class="carousel card" v-if="recommend.length">
        <div class="carousel-left">
          <h3>本周热销</h3>
          <p>同学们最近都在买这些</p>
        </div>
        <el-carousel height="180px" :interval="4000" indicator-position="none" arrow="always" class="carousel-body">
          <el-carousel-item v-for="item in recommend" :key="item.id">
            <div class="slide" @click="$router.push('/product/' + item.id)">
              <img :src="item.imageList[0]" />
              <div class="slide-info">
                <div class="slide-name">{{ item.name }}</div>
                <div class="price">¥{{ item.price }}<small> / 原价 {{ item.originalPrice }}</small></div>
                <div class="muted">已售 {{ item.sales }} 件 · {{ item.shopName }}</div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 筛选 -->
      <div class="filter card">
        <div class="cats">
          <span :class="{ on: categoryId === null }" @click="selectCategory(null)">全部</span>
          <span v-for="c in categories" :key="c.id" :class="{ on: categoryId === c.id }" @click="selectCategory(c.id)">
            {{ c.name }}
          </span>
        </div>
        <div class="sort">
          <span :class="{ on: sort === '' }" @click="setSort('')">综合</span>
          <span :class="{ on: sort === 'sales_desc' }" @click="setSort('sales_desc')">销量</span>
          <span :class="{ on: sort === 'rating_desc' }" @click="setSort('rating_desc')">好评率</span>
          <span :class="{ on: sort === 'price_asc' }" @click="setSort('price_asc')">价格 ↑</span>
          <span :class="{ on: sort === 'price_desc' }" @click="setSort('price_desc')">价格 ↓</span>
        </div>
      </div>

      <div class="result-bar">
        <span v-if="keyword">“{{ keyword }}” 的搜索结果</span>
        <span v-else>全部商品</span>
        <span class="muted">共 {{ total }} 件</span>
      </div>

      <div class="grid" v-loading="loading">
        <div v-for="p in list" :key="p.id" class="p-card" @click="$router.push('/product/' + p.id)">
          <div class="p-img">
            <img :src="p.imageList[0]" />
            <span class="cond">{{ p.conditionLevel }}</span>
            <span v-if="p.stock <= 1" class="only">仅剩 {{ p.stock }} 件</span>
          </div>
          <div class="p-body">
            <div class="p-name">{{ p.name }}</div>
            <div class="p-price">
              <span class="price">¥{{ p.price }}</span>
              <s v-if="p.originalPrice">¥{{ p.originalPrice }}</s>
              <el-tag v-if="p.negotiable" size="small" type="warning" effect="plain">可议价</el-tag>
            </div>
            <div class="p-meta">
              <span>{{ p.shopName }}</span>
              <span><el-icon><Star /></el-icon> {{ p.ratingAvg }} · 已售 {{ p.sales }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" description="没有找到相关商品" style="grid-column: 1/-1" />
      </div>

      <div class="pager" v-if="total > size">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="onPage" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Star } from '@element-plus/icons-vue'
import http from '../api'

const keyword = ref('')
const categories = ref([])
const categoryId = ref(null)
const sort = ref('')
const list = ref([])
const recommend = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const hotWords = ['机械键盘', '教材', '自行车', '耳机']

async function load() {
  loading.value = true
  try {
    const data = await http.get('/products', {
      params: { keyword: keyword.value, categoryId: categoryId.value, sort: sort.value, page: page.value, size }
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function quickSearch(k) {
  keyword.value = k
  search()
}

function selectCategory(id) {
  categoryId.value = id
  page.value = 1
  load()
}

function setSort(s) {
  sort.value = s
  page.value = 1
  load()
}

function onPage(p) {
  page.value = p
  load()
  window.scrollTo({ top: 300, behavior: 'smooth' })
}

onMounted(async () => {
  categories.value = await http.get('/products/categories')
  recommend.value = await http.get('/products/recommend')
  load()
})
</script>

<style scoped>
.hero {
  background: linear-gradient(120deg, #0f766e, #0d9488 60%, #14b8a6);
  color: #fff;
  padding: 44px 0 38px;
}
.hero-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
}
.hero-text h2 {
  margin: 0 0 10px;
  font-size: 30px;
  letter-spacing: 1px;
}
.hero-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
}
.search-box { width: 480px; }
.hot {
  margin-top: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}
.hot a {
  margin-right: 12px;
  cursor: pointer;
  text-decoration: underline;
}
.carousel {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 18px;
  padding: 16px 20px;
}
.carousel-left { width: 160px; flex-shrink: 0; }
.carousel-left h3 { margin: 0 0 8px; font-size: 18px; }
.carousel-left p { margin: 0; color: var(--muted); font-size: 13px; }
.carousel-body { flex: 1; }
.slide {
  display: flex;
  gap: 20px;
  align-items: center;
  height: 100%;
  cursor: pointer;
}
.slide img {
  width: 200px;
  height: 150px;
  object-fit: cover;
  border-radius: 10px;
}
.slide-name { font-size: 17px; font-weight: 600; margin-bottom: 10px; }
.slide-info .price { font-size: 22px; display: block; margin-bottom: 8px; }
.filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  margin-bottom: 14px;
}
.cats { display: flex; gap: 6px; flex-wrap: wrap; }
.cats span {
  padding: 6px 14px;
  border-radius: 16px;
  cursor: pointer;
  color: var(--muted);
  font-size: 14px;
}
.cats span.on {
  background: var(--brand-light);
  color: var(--brand);
  font-weight: 600;
}
.sort { display: flex; gap: 16px; color: var(--muted); font-size: 13px; }
.sort span { cursor: pointer; }
.sort span.on { color: var(--brand); font-weight: 600; }
.result-bar {
  display: flex;
  justify-content: space-between;
  padding: 4px 4px 14px;
  font-size: 14px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 200px;
}
.p-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  cursor: pointer;
  transition: all 0.2s;
}
.p-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(15, 118, 110, 0.12);
  border-color: #cfe6e2;
}
.p-img { position: relative; height: 190px; background: #f1f3f4; }
.p-img img { width: 100%; height: 100%; object-fit: cover; }
.cond {
  position: absolute;
  left: 10px;
  top: 10px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--brand);
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}
.only {
  position: absolute;
  right: 10px;
  bottom: 10px;
  background: rgba(229, 72, 77, 0.9);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}
.p-body { padding: 12px 14px 14px; }
.p-name {
  font-size: 14px;
  line-height: 1.5;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.p-price {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 6px;
}
.p-price .price { font-size: 20px; }
.p-price s { color: #b0b6bd; font-size: 12px; }
.p-meta {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
}
.pager { display: flex; justify-content: center; margin-top: 26px; }
</style>

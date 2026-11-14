<template>
  <div class="page">
    <div class="head">
      <h3 class="section-title">商家中心 · {{ user.shopName }}</h3>
      <el-tag type="warning" effect="plain">Lv.{{ user.merchantLevel }} 商家，平台服务费 {{ feeRate }}</el-tag>
    </div>

    <div class="stats">
      <div class="s-card"><span>在售商品</span><b>{{ stats.onSaleCount }}</b></div>
      <div class="s-card"><span>全部商品</span><b>{{ stats.productCount }}</b></div>
      <div class="s-card"><span>订单数</span><b>{{ stats.orderCount }}</b></div>
      <div class="s-card"><span>累计收入（已结算）</span><b>¥{{ stats.totalIncome }}</b></div>
      <div class="s-card warn"><span>待处理退货</span><b>{{ stats.pendingReturn }}</b></div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="商品管理" name="products">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openPublish()">发布商品</el-button>
          <span class="muted">新发布的商品需要管理员审核通过后才会出现在商城中</span>
        </div>
        <el-table :data="products" stripe>
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="p-cell">
                <img :src="row.imageList[0]" />
                <div>
                  <div>{{ row.name }}</div>
                  <div class="muted">{{ row.conditionLevel }} · {{ row.size }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="售价" width="90" />
          <el-table-column prop="stock" label="库存" width="70" />
          <el-table-column prop="sales" label="销量" width="70" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPublish(row)">编辑</el-button>
              <el-button v-if="row.status === 'ON_SALE'" link type="warning" @click="changeStatus(row, 'OFF_SHELF')">下架</el-button>
              <el-button v-if="row.status === 'OFF_SHELF'" link type="success" @click="changeStatus(row, 'ON_SALE')">重新上架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单管理" name="orders">
        <el-table :data="orders" stripe>
          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column prop="productName" label="商品" min-width="200" />
          <el-table-column prop="buyerName" label="买家" width="90" />
          <el-table-column prop="amount" label="金额" width="90" />
          <el-table-column prop="fee" label="服务费" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" align="right">
            <template #default="{ row }">
              <template v-if="row.status === 'RETURN_REQUESTED'">
                <el-button link type="success" @click="handleReturn(row, true)">同意退货</el-button>
                <el-button link type="danger" @click="handleReturn(row, false)">拒绝</el-button>
              </template>
              <el-button v-if="row.status === 'COMPLETED'" link type="primary" @click="openReviewBuyer(row)">评价买家</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="买家评价" name="reviews">
        <div v-for="r in reviews" :key="r.id" class="review card">
          <div class="r-head">
            <b>{{ r.buyerName }}</b>
            <el-rate :model-value="r.rating" disabled size="small" />
            <span class="muted">{{ r.type === 'PRODUCT' ? '商品评价' : '服务评价' }} · {{ r.productName }}</span>
            <span class="muted" style="margin-left:auto">{{ r.createdAt }}</span>
          </div>
          <p>{{ r.content }}</p>
        </div>
        <el-empty v-if="!reviews.length" description="还没有收到评价" />
      </el-tab-pane>
    </el-tabs>

    <!-- 发布/编辑商品 -->
    <el-dialog v-model="publishVisible" :title="form.id ? '编辑商品' : '发布商品'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" maxlength="60" show-word-limit />
        </el-form-item>
        <div class="two">
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" style="width:100%">
              <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="成色">
            <el-select v-model="form.conditionLevel" style="width:100%">
              <el-option v-for="c in ['全新', '九成新', '八成新', '七成新', '六成新及以下']" :key="c" :value="c" :label="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="售价">
            <el-input-number v-model="form.price" :min="0.01" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="原价">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="form.stock" :min="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="尺寸规格">
            <el-input v-model="form.size" placeholder="如 26寸 / 均码 / 标准版" />
          </el-form-item>
        </div>
        <el-form-item label="允许议价">
          <el-switch v-model="form.negotiable" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="商品图片">
          <div class="img-list">
            <div v-for="(img, i) in form.images" :key="i" class="img-item">
              <img :src="img" />
              <el-icon class="del" @click="form.images.splice(i, 1)"><Close /></el-icon>
            </div>
            <el-upload :show-file-list="false" :http-request="uploadImage" accept="image/*" multiple>
              <div class="uploader"><el-icon><Plus /></el-icon></div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="评价买家" width="420px">
      <el-rate v-model="buyerReview.rating" />
      <el-input v-model="buyerReview.content" type="textarea" :rows="3" style="margin-top:12px" placeholder="买家付款是否及时、沟通是否顺畅" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBuyerReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import http from '../api'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const tab = ref('products')
const stats = ref({ productCount: 0, onSaleCount: 0, orderCount: 0, totalIncome: 0, pendingReturn: 0 })
const products = ref([])
const orders = ref([])
const reviews = ref([])
const categories = ref([])
const publishVisible = ref(false)
const reviewVisible = ref(false)
const currentOrder = ref(null)
const buyerReview = reactive({ rating: 5, content: '' })

const form = reactive({
  id: null, name: '', categoryId: 1, price: 0, originalPrice: 0, stock: 1,
  size: '', conditionLevel: '九成新', negotiable: 0, description: '', images: []
})

const FEE = ['0.1%', '0.2%', '0.5%', '0.75%', '1%']
const feeRate = computed(() => FEE[(user.value.merchantLevel || 1) - 1])

const STATUS = { PENDING: '待审核', ON_SALE: '在售', OFF_SHELF: '已下架', SOLD: '已售出', LOCKED: '已锁定' }
const statusText = (s) => STATUS[s] || s
const statusType = (s) => ({ PENDING: 'warning', ON_SALE: 'success', OFF_SHELF: 'info', SOLD: 'danger' }[s] || '')
const OSTATUS = { PAID: '待收货', COMPLETED: '已完成', RETURN_REQUESTED: '退货申请中', RETURNED: '已退货' }
const orderStatusText = (s) => OSTATUS[s] || s
const orderStatusType = (s) => ({ PAID: 'warning', COMPLETED: 'success', RETURN_REQUESTED: 'danger', RETURNED: 'info' }[s] || '')

async function loadAll() {
  stats.value = await http.get('/merchant/stats')
  products.value = await http.get('/merchant/products')
  orders.value = await http.get('/merchant/orders')
  reviews.value = await http.get('/merchant/reviews')
  categories.value = await http.get('/products/categories')
}

function openPublish(row) {
  Object.assign(form, {
    id: row?.id || null,
    name: row?.name || '',
    categoryId: row?.categoryId || 1,
    price: row ? Number(row.price) : 0,
    originalPrice: row ? Number(row.originalPrice || 0) : 0,
    stock: row?.stock || 1,
    size: row?.size || '',
    conditionLevel: row?.conditionLevel || '九成新',
    negotiable: row?.negotiable || 0,
    description: row?.description || '',
    images: row ? [...row.imageList] : []
  })
  publishVisible.value = true
}

async function uploadImage(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  const data = await http.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  form.images.push(data.url)
}

async function save() {
  if (!form.name || !form.price) {
    ElMessage.warning('商品名称和价格必填')
    return
  }
  if (form.id) {
    await http.put('/merchant/products', form)
    ElMessage.success('已保存')
  } else {
    await http.post('/merchant/products', form)
    ElMessage.success('已提交，等待管理员审核')
  }
  publishVisible.value = false
  loadAll()
}

async function changeStatus(row, status) {
  await http.put(`/merchant/products/${row.id}/status`, { status })
  ElMessage.success('操作成功')
  loadAll()
}

async function handleReturn(row, approve) {
  await ElMessageBox.confirm(approve ? '同意退货后货款将退还给买家，商品库存恢复。' : '拒绝后订单回到已完成状态。', '处理退货')
  await http.post(`/merchant/orders/${row.id}/return`, { approve })
  ElMessage.success('已处理')
  loadAll()
}

function openReviewBuyer(row) {
  currentOrder.value = row
  buyerReview.rating = 5
  buyerReview.content = ''
  reviewVisible.value = true
}

async function submitBuyerReview() {
  await http.post(`/merchant/orders/${currentOrder.value.id}/review`, buyerReview)
  ElMessage.success('已评价')
  reviewVisible.value = false
  loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 16px; }
.stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 20px; }
.s-card { background: #fff; border: 1px solid var(--line); border-radius: 12px; padding: 16px 18px; }
.s-card span { color: var(--muted); font-size: 13px; }
.s-card b { display: block; font-size: 24px; margin-top: 6px; color: var(--brand); }
.s-card.warn b { color: #e5484d; }
.toolbar { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.p-cell { display: flex; align-items: center; gap: 12px; }
.p-cell img { width: 54px; height: 54px; border-radius: 8px; object-fit: cover; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.img-list { display: flex; gap: 10px; flex-wrap: wrap; }
.img-item { position: relative; }
.img-item img { width: 84px; height: 84px; border-radius: 8px; object-fit: cover; }
.img-item .del {
  position: absolute; top: -6px; right: -6px; background: #fff; border-radius: 50%;
  cursor: pointer; color: #e5484d; font-size: 16px;
}
.uploader {
  width: 84px; height: 84px; border: 1px dashed #cfd6dd; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; color: var(--muted);
}
.review { margin-bottom: 12px; }
.r-head { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.review p { margin: 8px 0 0; color: #374151; }
</style>

<template>
  <div class="page">
    <h3 class="section-title">我的钱包</h3>
    <div class="cards">
      <div class="w-card main">
        <div class="label">账户余额（元）</div>
        <div class="num">{{ wallet.balance }}</div>
        <div class="hint">余额可用于下单，管理员可代为充值</div>
      </div>
      <div class="w-card">
        <div class="label">积分</div>
        <div class="num">{{ wallet.points }}</div>
        <div class="hint">消费 1 元得 1 分，100 分抵 1 元</div>
      </div>
      <div class="w-card">
        <div class="label">好评率</div>
        <div class="num">{{ wallet.creditScore }}<small>%</small></div>
        <div class="hint">来自商家对你交易行为的评价</div>
      </div>
    </div>

    <div class="card" style="margin-top:20px">
      <h3 class="section-title">资金明细</h3>
      <el-table :data="txns" stripe>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.amount > 0 ? 'success' : 'info'">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="260" />
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">
            <span :style="{ color: Number(row.amount) > 0 ? '#0f766e' : '#e5484d' }">
              {{ Number(row.amount) > 0 ? '+' : '' }}{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="余额" width="120" align="right" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api'

const wallet = ref({ balance: 0, points: 0, creditScore: 100 })
const txns = ref([])
const TYPE = {
  RECHARGE: '充值', PAY: '支付', INCOME: '货款入账', FEE: '手续费', REFUND: '退款', POINTS_DEDUCT: '积分抵扣'
}
const typeText = (t) => TYPE[t] || t

onMounted(async () => {
  wallet.value = await http.get('/wallet')
  txns.value = await http.get('/wallet/txns')
})
</script>

<style scoped>
.cards { display: grid; grid-template-columns: 1.4fr 1fr 1fr; gap: 16px; }
.w-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid var(--line);
  padding: 22px;
}
.w-card.main {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: #fff;
  border: none;
}
.w-card .label { font-size: 13px; opacity: 0.8; }
.w-card .num { font-size: 34px; font-weight: 700; margin: 10px 0 6px; }
.w-card .num small { font-size: 16px; }
.w-card .hint { font-size: 12px; opacity: 0.7; }
</style>

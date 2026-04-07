<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { formationApi } from '@/api/formation'
import type { StaticFormationConfig, StaticFormationResult } from '@/model/formation'
import { useResStore } from '@/stores/resource'

const props = withDefaults(defineProps<{
  defaultTab?: 'allocation' | 'plans'
}>(), {
  defaultTab: 'allocation',
})

const resStore = useResStore()
const loading = ref(false)
const error = ref('')
const activeTab = ref<'allocation' | 'plans'>(props.defaultTab)
const result = ref<StaticFormationResult | null>(null)

const config = reactive<StaticFormationConfig>({
  algorithmType: 'psoFormationStrategy',
  distanceWeight: 0.34,
  firepowerWeight: 0.38,
  defenseWeight: 0.28,
  maxGroupSize: 6,
  maxIterations: 80,
  particleCount: 40,
  planCount: 3,
  allowedWeaponTypes: [],
  selectedParadigms: ['AIR_AIR', 'AIR_GROUND', 'GROUND_GROUND'],
})

const paradigmOptions = [
  { label: '空空联合范式', value: 'AIR_AIR' },
  { label: '空地联合范式', value: 'AIR_GROUND' },
  { label: '地地联合范式', value: 'GROUND_GROUND' },
]
const algorithmOptions = [
  { label: '粒子群算法', value: 'psoFormationStrategy' },
  { label: '贪心算法', value: 'greedyFormationStrategy' },
  { label: '遗传算法', value: 'geneticFormationStrategy' },
  { label: '蚁群算法', value: 'antColonyFormationStrategy' },
]

const weaponTypes = computed(() => Array.from(resStore.weaponTypeMap.values()))
const summaryCards = computed(() => {
  if (!result.value) return []
  return [
    { label: '敌方目标', value: result.value.enemyCount },
    { label: '参演武器', value: result.value.weaponCount },
    { label: '保护区', value: result.value.zoneCount },
    { label: '弹药总量', value: result.value.totalAmmo },
    { label: '火力通道', value: result.value.totalChannels },
    { label: '联合范式', value: result.value.supportedParadigms.length },
  ]
})

const bestPlanId = computed(() => result.value?.recommendedPlanId ?? '')
const sortedPlans = computed(() => result.value?.plans ?? [])
const planBars = computed(() => {
  const plans = sortedPlans.value
  const maxScore = Math.max(...plans.map(item => item.fitnessScore || 0), 1)
  return plans.map(item => ({
    ...item,
    width: `${Math.max((item.fitnessScore / maxScore) * 100, 6)}%`,
  }))
})

function paradigmLabel(value: string) {
  return {
    AIR_AIR: '空空联合范式',
    AIR_GROUND: '空地联合范式',
    GROUND_GROUND: '地地联合范式',
  }[value] ?? value
}

function domainLabel(value: string) {
  return {
    AIR: '空中',
    GROUND: '地面',
    SEA: '海上',
    SPACE: '太空',
    UNKNOWN: '未知',
  }[value] ?? value
}

const domainBars = computed(() => {
  const summaries = result.value?.domainResourceSummaries ?? []
  const maxAmmo = Math.max(...summaries.map(item => item.ammoCount || 0), 1)
  return summaries.map(item => ({
    ...item,
    width: `${Math.max((item.ammoCount / maxAmmo) * 100, 8)}%`,
  }))
})

watch(() => props.defaultTab, value => {
  activeTab.value = value
})

async function generate() {
  loading.value = true
  error.value = ''
  try {
    const response = await formationApi.generateStaticFormation({ ...config })
    if (response?.code !== 200 || !response?.data) {
      result.value = null
      error.value = response?.message || '未获取到静态编组结果，请检查敌方目标、武器节点和保护区数据。'
      return
    }
    result.value = response.data
  } catch (err: any) {
    result.value = null
    error.value = err?.message || '静态编组方案生成失败'
  } finally {
    loading.value = false
  }
}

function toggleParadigm(paradigm: string) {
  const set = new Set(config.selectedParadigms)
  if (set.has(paradigm)) {
    set.delete(paradigm)
  } else {
    set.add(paradigm)
  }
  config.selectedParadigms = Array.from(set)
}

function toggleWeaponType(type: string) {
  const set = new Set(config.allowedWeaponTypes)
  if (set.has(type)) {
    set.delete(type)
  } else {
    set.add(type)
  }
  config.allowedWeaponTypes = Array.from(set)
}

onMounted(() => {
  generate()
})
</script>

<template>
  <div class="formation-panel">
    <div class="panel-shell">
      <aside class="config-card">
        <div class="section-head">
          <p class="eyebrow">算法参数配置</p>
          <h3>静态编组生成</h3>
        </div>

        <label class="field">
          <span>算法类型</span>
          <select v-model="config.algorithmType">
            <option v-for="item in algorithmOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <div class="field-grid">
          <label class="field">
            <span>距离权重</span>
            <input v-model.number="config.distanceWeight" type="number" min="0" max="1" step="0.01" />
          </label>
          <label class="field">
            <span>火力权重</span>
            <input v-model.number="config.firepowerWeight" type="number" min="0" max="1" step="0.01" />
          </label>
          <label class="field">
            <span>防御权重</span>
            <input v-model.number="config.defenseWeight" type="number" min="0" max="1" step="0.01" />
          </label>
        </div>

        <div class="field-grid">
          <label class="field">
            <span>最大编组人数</span>
            <input v-model.number="config.maxGroupSize" type="number" min="1" step="1" />
          </label>
          <label class="field">
            <span>迭代次数</span>
            <input v-model.number="config.maxIterations" type="number" min="1" step="1" />
          </label>
          <label class="field">
            <span>粒子数量</span>
            <input v-model.number="config.particleCount" type="number" min="1" step="1" />
          </label>
        </div>

        <div class="choice-group">
          <span class="group-title">联合编组范式</span>
          <button
            v-for="item in paradigmOptions"
            :key="item.value"
            type="button"
            class="tag-btn"
            :class="{ active: config.selectedParadigms.includes(item.value) }"
            @click="toggleParadigm(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="choice-group">
          <span class="group-title">武器类型限制</span>
          <button
            v-for="item in weaponTypes"
            :key="item.type"
            type="button"
            class="tag-btn subtle"
            :class="{ active: config.allowedWeaponTypes.includes(item.type) }"
            @click="toggleWeaponType(item.type)"
          >
            {{ item.type }} · {{ item.deployDomain }}
          </button>
        </div>

        <button class="generate-btn" type="button" :disabled="loading" @click="generate">
          {{ loading ? '方案生成中...' : '生成静态编组方案' }}
        </button>
        <p v-if="error" class="error-text">{{ error }}</p>
      </aside>

      <section class="result-card">
        <div class="summary-grid">
          <article v-for="card in summaryCards" :key="card.label" class="summary-item">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </article>
        </div>

        <div class="tab-row">
          <button type="button" class="tab-btn" :class="{ active: activeTab === 'allocation' }" @click="activeTab = 'allocation'">
            静态火力分配
          </button>
          <button type="button" class="tab-btn" :class="{ active: activeTab === 'plans' }" @click="activeTab = 'plans'">
            静态编组对比
          </button>
        </div>

        <div v-if="!result" class="empty-state">当前没有可展示的静态编组结果。</div>

        <template v-else-if="activeTab === 'allocation'">
          <div class="chart-card">
            <div class="section-head compact">
              <p class="eyebrow">火力资源库管理</p>
              <h3>域内弹药分布</h3>
            </div>
            <div class="bar-stack">
              <div v-for="item in domainBars" :key="item.deployDomain" class="bar-row">
                <span class="bar-label">{{ domainLabel(item.deployDomain) }}</span>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: item.width }"></div>
                </div>
                <strong>{{ item.ammoCount }}</strong>
              </div>
            </div>
          </div>

          <div class="table-card">
            <div class="section-head compact">
              <p class="eyebrow">固定态势火力资源</p>
              <h3>静态火力分配基础表</h3>
            </div>
            <table>
              <thead>
                <tr>
                  <th>部署域</th>
                  <th>武器类型</th>
                  <th>火力类型</th>
                  <th>节点数</th>
                  <th>弹药量</th>
                  <th>通道数</th>
                  <th>平均拦截率</th>
                  <th>最大射程(km)</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in result.firepowerResourceSummaries" :key="`${item.deployDomain}-${item.weaponType}-${item.fireType}`">
                  <td>{{ domainLabel(item.deployDomain) }}</td>
                  <td>{{ item.weaponType }}</td>
                  <td>{{ item.fireType }}</td>
                  <td>{{ item.weaponNodeCount }}</td>
                  <td>{{ item.ammoCount }}</td>
                  <td>{{ item.channelCount }}</td>
                  <td>{{ item.averageInterceptionRate }}%</td>
                  <td>{{ item.maxRange }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <template v-else>
          <div class="chart-card">
            <div class="section-head compact">
              <p class="eyebrow">多方案对比分析</p>
              <h3>联合编组适应度</h3>
            </div>
            <div class="bar-stack">
              <div v-for="plan in planBars" :key="plan.planId" class="bar-row">
                <span class="bar-label">{{ paradigmLabel(plan.paradigm) }}</span>
                <div class="bar-track">
                  <div class="bar-fill accent" :style="{ width: plan.width }"></div>
                </div>
                <strong>{{ plan.fitnessScore }}</strong>
              </div>
            </div>
          </div>

          <div class="plan-grid">
            <article v-for="plan in sortedPlans" :key="plan.planId" class="plan-card" :class="{ recommended: plan.planId === bestPlanId }">
              <header class="plan-head">
                <div>
                  <p class="plan-name">{{ plan.planName }}</p>
                  <h4>{{ paradigmLabel(plan.paradigm) }}</h4>
                </div>
                <strong>{{ plan.fitnessScore }}</strong>
              </header>

              <p class="plan-summary">{{ plan.summary }}</p>

              <div class="metrics">
                <span>距离 {{ plan.distanceScore }}%</span>
                <span>火力 {{ plan.firepowerScore }}%</span>
                <span>防御 {{ plan.defenseScore }}%</span>
                <span>覆盖 {{ plan.coverageScore }}%</span>
              </div>

              <div class="chips">
                <span v-for="domain in plan.participatingDomains" :key="domain" class="chip">{{ domainLabel(domain) }}</span>
                <span class="chip outline">节点 {{ plan.groupSize }}</span>
                <span class="chip outline">目标 {{ plan.allocatedEnemyCount }}</span>
              </div>

              <p v-if="plan.warnings?.length" class="warning-text">{{ plan.warnings.join('；') }}</p>

              <table>
                <thead>
                  <tr>
                    <th>武器</th>
                    <th>目标</th>
                    <th>保护区</th>
                    <th>距离</th>
                    <th>分配得分</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="detail in plan.details" :key="`${plan.planId}-${detail.weaponNodeId}-${detail.targetEnemyId}-${detail.fireType}`">
                    <td>{{ detail.weaponType }} / {{ detail.fireType }}</td>
                    <td>{{ detail.targetEnemyType }} ({{ detail.targetEnemyId }})</td>
                    <td>{{ detail.sourceZoneId }}</td>
                    <td>{{ detail.distanceKm }} km</td>
                    <td>{{ detail.assignmentScore }}</td>
                  </tr>
                </tbody>
              </table>
            </article>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.formation-panel {
  min-height: 100%;
  color: #10243a;
}

.panel-shell {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
}

.config-card,
.result-card,
.chart-card,
.table-card,
.plan-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(242, 247, 252, 0.98));
  border: 1px solid rgba(16, 36, 58, 0.08);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(17, 45, 78, 0.08);
}

.config-card {
  padding: 20px;
  position: sticky;
  top: 0;
  align-self: start;
}

.result-card {
  padding: 20px;
}

.section-head h3,
.plan-head h4 {
  margin: 0;
  font-size: 1.15rem;
}

.section-head.compact {
  margin-bottom: 14px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #5c728c;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.field,
.choice-group {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.field span,
.group-title {
  font-size: 0.9rem;
  font-weight: 600;
}

.field input,
.field select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #c7d4e2;
  border-radius: 12px;
  background: #f8fbff;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.tag-btn {
  border: 1px solid #bfd1e2;
  background: #f4f8fc;
  color: #1a3854;
  border-radius: 999px;
  padding: 8px 12px;
  margin-right: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.tag-btn.active {
  background: linear-gradient(135deg, #0f5c8c, #1698b9);
  border-color: transparent;
  color: #fff;
}

.tag-btn.subtle.active {
  background: linear-gradient(135deg, #355c7d, #2b8a84);
}

.generate-btn,
.tab-btn {
  border: none;
  cursor: pointer;
}

.generate-btn {
  width: 100%;
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0d4f78, #0f8aa9);
  color: #fff;
  font-weight: 700;
}

.generate-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.error-text,
.warning-text {
  color: #b64533;
  margin-top: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-item {
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0f2438, #174e71);
  color: #e8f4ff;
  display: grid;
  gap: 6px;
}

.summary-item strong {
  font-size: 1.4rem;
}

.tab-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.tab-btn {
  padding: 10px 16px;
  border-radius: 999px;
  background: #e6eef6;
  color: #16314c;
  font-weight: 700;
}

.tab-btn.active {
  background: linear-gradient(135deg, #133b59, #1d7a8c);
  color: #fff;
}

.chart-card,
.table-card {
  padding: 16px;
  margin-bottom: 16px;
}

.bar-stack {
  display: grid;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 60px;
  gap: 10px;
  align-items: center;
}

.bar-label {
  font-weight: 700;
  color: #21425f;
}

.bar-track {
  height: 12px;
  border-radius: 999px;
  background: #d8e5ef;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #3d7ea6, #69c4b8);
}

.bar-fill.accent {
  background: linear-gradient(90deg, #144a72, #1aa3b8);
}

.plan-grid {
  display: grid;
  gap: 16px;
}

.plan-card {
  padding: 16px;
}

.plan-card.recommended {
  border-color: rgba(12, 108, 141, 0.35);
  box-shadow: 0 20px 44px rgba(9, 88, 120, 0.14);
}

.plan-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
}

.plan-name {
  margin: 0 0 4px;
  color: #5f748a;
}

.plan-head strong {
  font-size: 1.6rem;
  color: #0a607e;
}

.plan-summary {
  margin: 12px 0;
  color: #27455f;
}

.metrics,
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #e6f3f7;
  color: #145d72;
  font-size: 0.86rem;
}

.chip.outline {
  background: transparent;
  border: 1px solid #bfd1e2;
  color: #2a4965;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem;
}

th,
td {
  padding: 10px 8px;
  border-bottom: 1px solid #dce6ef;
  text-align: left;
}

th {
  color: #567086;
  font-weight: 700;
}

.empty-state {
  padding: 36px 0;
  text-align: center;
  color: #6e8193;
}

@media (max-width: 1100px) {
  .panel-shell {
    grid-template-columns: 1fr;
  }

  .config-card {
    position: static;
  }

  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .field-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .bar-row {
    grid-template-columns: 72px minmax(0, 1fr) 54px;
  }
}
</style>

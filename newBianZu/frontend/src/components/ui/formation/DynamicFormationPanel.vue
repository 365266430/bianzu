<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { formationApi } from '@/api/formation'
import { simApi } from '@/api/simulation'
import { useResStore } from '@/stores/resource'
import { wsClient } from '@/utils/websocket'
import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'
import type { WeaponNode } from '@/model/weaponNode'
import type {
  DynamicAlgorithmConfig,
  DynamicEnemyNode,
  DynamicFormationConstraints,
  DynamicFormationRequest,
  DynamicFormationResult,
  DynamicProtectionZone,
} from '@/model/formation'

const resStore = useResStore()

const loading = ref(false)
const resetLoading = ref(false)
const dataLoading = ref(false)
const weaponStatusLoading = ref(false)
const zoneAssignLoading = ref(false)
const error = ref('')
const notice = ref('')
const dqnStatus = ref<Record<string, any> | null>(null)
const activeTab = ref<'request' | 'result'>('request')
const result = ref<DynamicFormationResult | null>(null)

const enemyNodes = ref<EnemyNode[]>([])
const zones = ref<ProtectionZone[]>([])
const weaponNodes = ref<WeaponNode[]>([])

const paradigmOptions = ref<string[]>([
  'ALL',
  'AIR_AIR',
  'GROUND_GROUND',
  'SEA_SEA',
  'SPACE_SPACE',
  'AIR_GROUND',
  'AIR_SEA',
  'AIR_SPACE',
  'GROUND_SEA',
  'GROUND_SPACE',
  'SEA_SPACE',
  'AIR_SEA_GROUND',
  'AIR_SPACE_GROUND',
  'AIR_SEA_SPACE',
  'GROUND_SEA_SPACE',
])

const form = reactive({
  selectedWeaponTypes: [] as string[],
  selectedEnemyIds: [] as string[],
  paradigm: 'ALL',
  constraints: createDefaultConstraints(),
  config: createDefaultConfig(),
})

const weaponTypeRows = computed(() => Array.from(resStore.weaponTypeMap.values()))
const fireTypeRows = computed(() => Array.from(resStore.fireTypeMap.values()))
const enemyTypeRows = computed(() => Array.from(resStore.enemyTypeMap.values()))

const enemyRows = computed(() => enemyNodes.value.map(item => ({
  id: String(item.id),
  type: item.type,
  altitude: Number(item.altitude ?? 0),
  speed: Number(item.speed ?? 0),
})))

const zoneRows = computed(() => zones.value.map(item => ({
  id: String(item.id),
  size: Number(item.size ?? 0),
  value: Number(item.value ?? 0),
  stationedWeaponCount: Array.isArray(item.stationedWeaponIds) ? item.stationedWeaponIds.length : 0,
})))

const weaponNodeRows = computed(() => weaponNodes.value.map(item => ({
  id: String(item.id),
  type: String(item.type ?? ''),
  status: Number(item.status ?? 0),
  statusText: weaponStatusLabel(Number(item.status ?? 0)),
  ammoText: Array.isArray(item.ammoStates)
    ? item.ammoStates.map(ammo => `${ammo.fireUnitType}:${ammo.currentCount}`).join(', ')
    : '',
})))

const idleWeaponCount = computed(() => weaponNodeRows.value.filter(item => item.status === 0).length)

const summaryCards = computed(() => {
  if (!result.value) {
    return []
  }
  return [
    { label: '敌方目标', value: result.value.enemyCount },
    { label: '参与武器类型展开后节点', value: result.value.weaponCount },
    { label: '弹药总量', value: result.value.totalAmmo },
    { label: '火力通道', value: result.value.totalChannels },
    { label: '估算成本', value: result.value.estimatedCost },
    { label: '方案数量', value: result.value.plans.length },
  ]
})

const planBars = computed(() => {
  const plans = result.value?.plans ?? []
  const maxScore = Math.max(...plans.map(item => item.fitnessScore || 0), 1)
  return plans.map(item => ({
    ...item,
    width: `${Math.max((item.fitnessScore / maxScore) * 100, 6)}%`,
  }))
})

const bestPlanId = computed(() => result.value?.recommendedPlanId ?? '')

const algorithmTypeDisplayMap: Record<string, string> = {
  DQN: 'DQN网络',
}

const algorithmTypeDisplay = computed({
  get: () => algorithmTypeDisplayMap[form.config.algorithmType] ?? form.config.algorithmType,
  set: (value: string) => {
    const normalized = value.trim()
    const matched = Object.entries(algorithmTypeDisplayMap).find(([, label]) => label === normalized)
    form.config.algorithmType = matched?.[0] ?? normalized
  },
})

function createDefaultConstraints(): DynamicFormationConstraints {
  return {
    requireAmmoSufficiency: false,
    ammoThreshold: 0.3,
    requireFirepowerMatch: true,
    considerDispatchCost: true,
    dispatchCostWeight: 0.2,
    considerPositionRelation: true,
    prioritizeInZoneEnemies: true,
    minInterceptionRate: 0.3,
    maxGroupSize: 6,
  }
}

function createDefaultConfig(): DynamicAlgorithmConfig {
  return {
    algorithmType: 'DQN',
    learningRate: 0.001,
    gamma: 0.95,
    epsilon: 0.1,
    targetUpdateFreq: 10,
    replayBufferSize: 10000,
    epochs: 100,
    stepsPerEpoch: 200,
    batchSize: 32,
    distanceWeight: 0.34,
    firepowerWeight: 0.38,
    defenseWeight: 0.28,
    maxGroupSize: 6,
    planCount: 3,
  }
}

function paradigmLabel(value: string) {
  const map: Record<string, string> = {
    ALL: '全域编组',
    AIR_AIR: '空空',
    GROUND_GROUND: '陆陆',
    SEA_SEA: '海海',
    SPACE_SPACE: '天天',
    AIR_GROUND: '空地',
    AIR_SEA: '空海',
    AIR_SPACE: '空天',
    GROUND_SEA: '陆海',
    GROUND_SPACE: '陆天',
    SEA_SPACE: '海天',
    AIR_SEA_GROUND: '空海地',
    AIR_SPACE_GROUND: '空天地',
    AIR_SEA_SPACE: '空海天',
    GROUND_SEA_SPACE: '陆海天',
  }
  return map[value] ?? value
}

function domainLabel(value: string) {
  return {
    AIR: '空域',
    GROUND: '地面',
    SEA: '海域',
    SPACE: '天域',
    UNKNOWN: '未知',
  }[value] ?? value
}

function weaponStatusLabel(status: number) {
  if (status === 0) return '待命'
  if (status === 1) return '分配中'
  if (status === 2) return '被调度分配'
  return `Unknown(${status})`
}

function keepSelectionValid() {
  const weaponTypeSet = new Set(weaponTypeRows.value.map(item => String(item.type)))
  const enemySet = new Set(enemyRows.value.map(item => item.id))
  form.selectedWeaponTypes = form.selectedWeaponTypes.filter(type => weaponTypeSet.has(type))
  form.selectedEnemyIds = form.selectedEnemyIds.filter(id => enemySet.has(id))
}

function selectAllWeaponTypes() {
  form.selectedWeaponTypes = weaponTypeRows.value.map(item => String(item.type))
}

function selectAllEnemies() {
  form.selectedEnemyIds = enemyRows.value.map(item => item.id)
}

function clearSelection() {
  form.selectedWeaponTypes = []
  form.selectedEnemyIds = []
}

function applySnapshot(payload: {
  weaponNodes?: WeaponNode[]
  enemyNodes?: EnemyNode[]
  zones?: ProtectionZone[]
}, autoSelect = false) {
  weaponNodes.value = Array.isArray(payload.weaponNodes) ? payload.weaponNodes : []
  enemyNodes.value = Array.isArray(payload.enemyNodes) ? payload.enemyNodes : []
  zones.value = Array.isArray(payload.zones) ? payload.zones : []
  keepSelectionValid()

  if (autoSelect) {
    if (!form.selectedEnemyIds.length) {
      selectAllEnemies()
    }
    if (!form.selectedWeaponTypes.length && weaponTypeRows.value.length) {
      selectAllWeaponTypes()
    }
  }
}

async function loadSnapshot() {
  dataLoading.value = true
  error.value = ''
  try {
    const response = await simApi.getSimulationSnapshot()
    if (response?.code !== 200 || !response?.data) {
      error.value = response?.message || '加载战场快照失败'
      applySnapshot({}, false)
      return
    }
    applySnapshot(response.data, true)
  } catch (requestError: any) {
    error.value = requestError?.message || '加载战场快照失败'
  } finally {
    dataLoading.value = false
  }
}

function normalizeEnemyNodes(items: EnemyNode[]): DynamicEnemyNode[] {
  return (items ?? [])
    .filter(Boolean)
    .map(item => ({
      id: String(item.id ?? ''),
      type: String(item.type ?? ''),
      longitude: Number(item.longitude ?? 0),
      latitude: Number(item.latitude ?? 0),
      altitude: Number(item.altitude ?? 0),
      heading: Number(item.heading ?? 0),
      speed: Number(item.speed ?? 0),
    }))
    .filter(item => item.id)
}

function normalizeZones(items: ProtectionZone[]): DynamicProtectionZone[] {
  return (items ?? [])
    .filter(Boolean)
    .map(item => ({
      id: String(item.id ?? ''),
      location: [Number(item.location?.[0] ?? 0), Number(item.location?.[1] ?? 0)] as [number, number],
      size: Number(item.size ?? 0),
      value: Number(item.value ?? 2),
      health: Number(item.health ?? 10),
      stationedWeaponIds: Array.isArray(item.stationedWeaponIds)
        ? item.stationedWeaponIds.map(value => String(value))
        : [],
    }))
    .filter(item => item.id)
}

function buildPayload(): DynamicFormationRequest | null {
  const selectedWeaponTypes = Array.from(new Set(form.selectedWeaponTypes.map(String)))
  const normalizedEnemyNodes = normalizeEnemyNodes(enemyNodes.value)
  const normalizedZones = normalizeZones(zones.value)
  const selectedEnemyIds = Array.from(new Set(
    (form.selectedEnemyIds.length ? form.selectedEnemyIds : normalizedEnemyNodes.map(item => item.id)).map(String),
  ))

  if (!selectedWeaponTypes.length) {
    error.value = '请至少选择一个武器类型'
    return null
  }
  if (!normalizedEnemyNodes.length) {
    error.value = '当前没有敌方节点数据，请先在地图中部署敌方目标'
    return null
  }
  if (!normalizedZones.length) {
    error.value = '当前没有保护区数据，请先在地图中部署保护区'
    return null
  }

  return {
    selectedWeaponTypes,
    selectedEnemyIds,
    paradigm: form.paradigm,
    enemyNodes: normalizedEnemyNodes,
    zones: normalizedZones,
    weaponTypes: weaponTypeRows.value,
    fireTypes: fireTypeRows.value,
    enemyTypes: enemyTypeRows.value,
    constraints: { ...form.constraints },
    config: {
      ...form.config,
      maxGroupSize: Number(form.constraints.maxGroupSize ?? form.config.maxGroupSize),
    },
  }
}

async function loadParadigms() {
  try {
    const response = await formationApi.getParadigms()
    if (response?.code === 200 && Array.isArray(response.data) && response.data.length) {
      paradigmOptions.value = response.data
      if (!paradigmOptions.value.includes(form.paradigm)) {
        form.paradigm = paradigmOptions.value[0]
      }
    }
  } catch {
    // keep fallback values
  }
}

async function generate() {
  error.value = ''
  notice.value = ''
  const payload = buildPayload()
  if (!payload) {
    return
  }

  loading.value = true
  try {
    const response = await formationApi.generateDynamicFormation(payload)
    if (response?.code !== 200 || !response?.data) {
      result.value = null
      error.value = response?.message || '动态编组方案生成失败'
      return
    }
    result.value = response.data
    activeTab.value = 'result'
  } catch (requestError: any) {
    result.value = null
    error.value = requestError?.message || '动态编组方案生成失败'
  } finally {
    loading.value = false
  }
}

async function resetDqn() {
  error.value = ''
  notice.value = ''
  resetLoading.value = true
  try {
    const response = await formationApi.resetDqn(true)
    if (response?.code !== 200) {
      error.value = response?.message || 'DQN 重置失败'
      return
    }
    result.value = null
    dqnStatus.value = response.data ?? null
    notice.value = 'DQN 已重置，经验池和本地模型已清空。'
  } catch (requestError: any) {
    error.value = requestError?.message || 'DQN 重置失败'
  } finally {
    resetLoading.value = false
  }
}

async function loadDqnStatus() {
  error.value = ''
  notice.value = ''
  resetLoading.value = true
  try {
    const response = await formationApi.getDqnStatus()
    if (response?.code !== 200) {
      error.value = response?.message || 'DQN 状态读取失败'
      return
    }
    dqnStatus.value = response.data ?? null
    const replaySize = dqnStatus.value?.training?.replaySize ?? 0
    const trainStep = dqnStatus.value?.training?.trainStep ?? 0
    const modelExists = dqnStatus.value?.modelFile?.exists ? '已保存' : '未保存'
    notice.value = `DQN 状态：经验 ${replaySize}，训练步 ${trainStep}，模型${modelExists}。`
  } catch (requestError: any) {
    error.value = requestError?.message || 'DQN 状态读取失败'
  } finally {
    resetLoading.value = false
  }
}

async function updateAllWeaponStatus(status: 0 | 1 | 2) {
  error.value = ''
  notice.value = ''
  weaponStatusLoading.value = true
  try {
    const response = await simApi.updateAllWeaponStatus(status)
    if (response?.code !== 200 || !Array.isArray(response.data)) {
      error.value = response?.message || '武器状态更新失败'
      return
    }
    weaponNodes.value = response.data
    notice.value = `武器状态已设为 ${weaponStatusLabel(status)}.`
  } catch (requestError: any) {
    error.value = requestError?.message || '武器状态更新失败'
  } finally {
    weaponStatusLoading.value = false
  }
}

async function updateWeaponStatus(weaponId: string, status: 0 | 1 | 2) {
  error.value = ''
  notice.value = ''
  weaponStatusLoading.value = true
  try {
    const response = await simApi.updateWeaponStatus(weaponId, status)
    if (response?.code !== 200 || !Array.isArray(response.data)) {
      error.value = response?.message || '武器状态更新失败'
      return
    }
    weaponNodes.value = response.data
    notice.value = `Weapon ${weaponId} set to ${weaponStatusLabel(status)}.`
  } catch (requestError: any) {
    error.value = requestError?.message || '武器状态更新失败'
  } finally {
    weaponStatusLoading.value = false
  }
}

async function autoAssignWeaponsToZones() {
  error.value = ''
  notice.value = ''
  zoneAssignLoading.value = true
  try {
    const response = await simApi.autoAssignWeaponsToZones()
    if (!Array.isArray(response)) {
      error.value = '自动分配失败'
      return
    }
    zones.value = response
    notice.value = '武器已分配给保护区.'
  } catch (requestError: any) {
    error.value = requestError?.message || '自动分配失败'
  } finally {
    zoneAssignLoading.value = false
  }
}

function handleEnemyUpdate(enemies: EnemyNode[]) {
  enemyNodes.value = Array.isArray(enemies) ? enemies : []
  keepSelectionValid()
  if (!form.selectedEnemyIds.length && enemyRows.value.length) {
    selectAllEnemies()
  }
}

function handleZoneUpdate(zoneList: ProtectionZone[]) {
  zones.value = Array.isArray(zoneList) ? zoneList : []
}

onMounted(async () => {
  await resStore.loadAllDictionaries()
  keepSelectionValid()
  if (!form.selectedWeaponTypes.length && weaponTypeRows.value.length) {
    selectAllWeaponTypes()
  }

  wsClient.subscribe('ENEMY_UPDATE', handleEnemyUpdate)
  wsClient.subscribe('ZONE_UPDATE', handleZoneUpdate)

  const latestEnemies = wsClient.getLatest<EnemyNode[]>('ENEMY_UPDATE')
  const latestZones = wsClient.getLatest<ProtectionZone[]>('ZONE_UPDATE')
  if (latestEnemies) {
    enemyNodes.value = latestEnemies
  }
  if (latestZones) {
    zones.value = latestZones
  }

  await Promise.all([loadParadigms(), loadSnapshot()])
})

onUnmounted(() => {
  wsClient.unsubscribe('ENEMY_UPDATE', handleEnemyUpdate)
  wsClient.unsubscribe('ZONE_UPDATE', handleZoneUpdate)
})
</script>

<template>
  <div class="dynamic-panel">
    <div class="panel-shell">
      <aside class="left-card">
        <div class="section-head">
          <p class="eyebrow">Dynamic Formation</p>
          <h3>生成动态编组方案</h3>
        </div>

        <div class="live-chip-row">
          <span class="live-chip">武器类型: {{ weaponTypeRows.length }}</span>
          <span class="live-chip">弹药类型: {{ fireTypeRows.length }}</span>
          <span class="live-chip">敌方类型: {{ enemyTypeRows.length }}</span>
          <span class="live-chip">敌方节点: {{ enemyRows.length }}</span>
          <span class="live-chip">保护区: {{ zoneRows.length }}</span>
        </div>

        <label class="field">
          <span>编组范式</span>
          <select v-model="form.paradigm">
            <option v-for="item in paradigmOptions" :key="item" :value="item">
              {{ paradigmLabel(item) }} ({{ item }})
            </option>
          </select>
        </label>

        <div class="field-grid">
          <label class="field">
            <span>最小拦截率</span>
            <input v-model.number="form.constraints.minInterceptionRate" type="number" min="0" max="1" step="0.01" />
          </label>
          <label class="field">
            <span>最大编组数</span>
            <input v-model.number="form.constraints.maxGroupSize" type="number" min="1" step="1" />
          </label>
          <label class="field">
            <span>调度成本权重</span>
            <input v-model.number="form.constraints.dispatchCostWeight" type="number" min="0" max="1" step="0.01" />
          </label>
        </div>

        <div class="switch-list">
          <label><input v-model="form.constraints.requireAmmoSufficiency" type="checkbox" /> 要求弹药充足</label>
          <label><input v-model="form.constraints.requireFirepowerMatch" type="checkbox" /> 要求火力匹配</label>
          <label><input v-model="form.constraints.considerDispatchCost" type="checkbox" /> 考虑调度成本</label>
          <label><input v-model="form.constraints.considerPositionRelation" type="checkbox" /> 考虑空间关系</label>
          <label><input v-model="form.constraints.prioritizeInZoneEnemies" type="checkbox" /> 优先保护区内目标</label>
        </div>

        <div class="field-grid">
          <label class="field">
            <span>算法类型</span>
            <input v-model="algorithmTypeDisplay" />
          </label>
          <label class="field">
            <span>方案数量</span>
            <input v-model.number="form.config.planCount" type="number" min="1" step="1" />
          </label>
          <label class="field">
            <span>训练轮次</span>
            <input v-model.number="form.config.epochs" type="number" min="1" step="1" />
          </label>
        </div>

        <div class="field-grid">
          <label class="field">
            <span>距离权重</span>
            <input v-model.number="form.config.distanceWeight" type="number" min="0" max="1" step="0.01" />
          </label>
          <label class="field">
            <span>火力权重</span>
            <input v-model.number="form.config.firepowerWeight" type="number" min="0" max="1" step="0.01" />
          </label>
          <label class="field">
            <span>防御权重</span>
            <input v-model.number="form.config.defenseWeight" type="number" min="0" max="1" step="0.01" />
          </label>
        </div>

        <div class="primary-actions">
          <button class="generate-btn" type="button" :disabled="loading || resetLoading" @click="generate">
            {{ loading ? '生成中...' : '生成动态编组方案' }}
          </button>
          <button class="reset-btn" type="button" :disabled="loading || resetLoading" @click="resetDqn">
            {{ resetLoading ? '重置中...' : '重置 DQN' }}
          </button>
          <button class="status-btn" type="button" :disabled="loading || resetLoading" @click="loadDqnStatus">
            状态
          </button>
        </div>
        <div class="primary-actions secondary-actions">
          <button class="status-btn" type="button" :disabled="loading || weaponStatusLoading" @click="updateAllWeaponStatus(0)">
            全部待命
          </button>
          <button class="status-btn" type="button" :disabled="loading || weaponStatusLoading" @click="updateAllWeaponStatus(1)">
            全部分配中
          </button>
          <button class="status-btn" type="button" :disabled="loading || zoneAssignLoading" @click="autoAssignWeaponsToZones">
            自动驻防
          </button>
        </div>
        <p v-if="notice" class="notice-text">{{ notice }}</p>
        <p v-if="error" class="error-text">{{ error }}</p>
      </aside>

      <section class="right-card">
        <div class="tab-row">
          <button type="button" class="tab-btn" :class="{ active: activeTab === 'request' }" @click="activeTab = 'request'">
            可视化选择
          </button>
          <button type="button" class="tab-btn" :class="{ active: activeTab === 'result' }" @click="activeTab = 'result'">
            结果查看
          </button>
        </div>

        <template v-if="activeTab === 'request'">
          <div class="action-row">
            <button type="button" class="plain-btn" :disabled="dataLoading" @click="loadSnapshot">
              {{ dataLoading ? '刷新中...' : '刷新战场快照' }}
            </button>
            <button type="button" class="plain-btn" @click="selectAllWeaponTypes">全选武器类型</button>
            <button type="button" class="plain-btn" @click="selectAllEnemies">全选敌方节点</button>
            <button type="button" class="plain-btn" @click="clearSelection">清空选择</button>
          </div>

          <div class="select-grid">
            <article class="select-card">
              <header>
                <h4>武器节点</h4>
                <span>{{ idleWeaponCount }} / {{ weaponNodeRows.length }} Idle</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>类型</th>
                      <th>状态</th>
                      <th>弹药</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in weaponNodeRows" :key="row.id">
                      <td>{{ row.id }}</td>
                      <td>{{ row.type }}</td>
                      <td>{{ row.statusText }}</td>
                      <td>{{ row.ammoText }}</td>
                      <td>
                        <button type="button" class="mini-btn" :disabled="weaponStatusLoading" @click="updateWeaponStatus(row.id, 0)">0</button>
                        <button type="button" class="mini-btn" :disabled="weaponStatusLoading" @click="updateWeaponStatus(row.id, 1)">1</button>
                        <button type="button" class="mini-btn" :disabled="weaponStatusLoading" @click="updateWeaponStatus(row.id, 2)">2</button>
                      </td>
                    </tr>
                    <tr v-if="!weaponNodeRows.length">
                      <td colspan="5" class="empty-row">暂无武器节点</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>
            <article class="select-card">
              <header>
                <h4>武器类型</h4>
                <span>{{ form.selectedWeaponTypes.length }} / {{ weaponTypeRows.length }}</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>选中</th>
                      <th>类型</th>
                      <th>部署域</th>
                      <th>功能</th>
                      <th>通道</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in weaponTypeRows" :key="String(row.type)">
                      <td><input v-model="form.selectedWeaponTypes" type="checkbox" :value="String(row.type)" /></td>
                      <td>{{ row.type }}</td>
                      <td>{{ row.deployDomain }}</td>
                      <td>{{ row.function }}</td>
                      <td>{{ row.channelCount }}</td>
                    </tr>
                    <tr v-if="!weaponTypeRows.length">
                      <td colspan="5" class="empty-row">暂无武器类型</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>

            <article class="select-card">
              <header>
                <h4>敌方节点</h4>
                <span>{{ form.selectedEnemyIds.length }} / {{ enemyRows.length }}</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>选中</th>
                      <th>ID</th>
                      <th>类型</th>
                      <th>高度</th>
                      <th>速度</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in enemyRows" :key="row.id">
                      <td><input v-model="form.selectedEnemyIds" type="checkbox" :value="row.id" /></td>
                      <td>{{ row.id }}</td>
                      <td>{{ row.type }}</td>
                      <td>{{ row.altitude }}</td>
                      <td>{{ row.speed }}</td>
                    </tr>
                    <tr v-if="!enemyRows.length">
                      <td colspan="5" class="empty-row">暂无敌方节点</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>

            <article class="select-card">
              <header>
                <h4>保护区（自动带入）</h4>
                <span>{{ zoneRows.length }}</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>半径</th>
                      <th>价值</th>
                      <th>驻防武器数</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in zoneRows" :key="row.id">
                      <td>{{ row.id }}</td>
                      <td>{{ row.size }}</td>
                      <td>{{ row.value }}</td>
                      <td>{{ row.stationedWeaponCount }}</td>
                    </tr>
                    <tr v-if="!zoneRows.length">
                      <td colspan="4" class="empty-row">暂无保护区</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>
          </div>
        </template>

        <template v-else>
          <div v-if="!result" class="empty-state">当前暂无动态编组结果，请先点击“生成动态编组方案”。</div>
          <template v-else>
            <div class="summary-grid">
              <article v-for="card in summaryCards" :key="card.label" class="summary-item">
                <span>{{ card.label }}</span>
                <strong>{{ card.value }}</strong>
              </article>
            </div>

            <div class="chart-card">
              <div class="section-head compact">
                <p class="eyebrow">Plan Score</p>
                <h3>方案得分对比</h3>
              </div>
              <div class="bar-stack">
                <div v-for="plan in planBars" :key="plan.planId" class="bar-row">
                  <span class="bar-label">{{ plan.planName }}</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: plan.width }"></div>
                  </div>
                  <strong>{{ plan.fitnessScore }}</strong>
                </div>
              </div>
            </div>

            <div class="plan-grid">
              <article
                v-for="plan in result.plans"
                :key="plan.planId"
                class="plan-card"
                :class="{ recommended: plan.planId === bestPlanId }"
              >
                <header class="plan-head">
                  <div>
                    <p class="plan-name">{{ plan.planName }}</p>
                    <h4>{{ paradigmLabel(result.paradigm) }}</h4>
                  </div>
                  <strong>{{ plan.fitnessScore }}</strong>
                </header>

                <div class="chips">
                  <span class="chip">group {{ plan.groupSize }}</span>
                  <span class="chip">enemy {{ plan.allocatedEnemyCount }}</span>
                  <span
                    v-for="domain in plan.participatingDomains"
                    :key="`${plan.planId}-${domain}`"
                    class="chip outline"
                  >
                    {{ domainLabel(domain) }}
                  </span>
                </div>

                <p v-if="plan.warnings?.length" class="warning-text">{{ plan.warnings.join('；') }}</p>

                <table>
                  <thead>
                    <tr>
                      <th>武器节点</th>
                      <th>敌方目标</th>
                      <th>弹药前后</th>
                      <th>拦截率</th>
                      <th>距离 km</th>
                      <th>分配得分</th>
                      <th>成本</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(detail, index) in plan.details"
                      :key="`${plan.planId}-${detail.weaponNodeId}-${detail.targetEnemyId}-${index}`"
                    >
                      <td>{{ detail.weaponNodeId }} / {{ detail.fireType }}</td>
                      <td>{{ detail.targetEnemyType }} ({{ detail.targetEnemyId }})</td>
                      <td>{{ detail.ammoBefore }} -> {{ detail.ammoAfter }}</td>
                      <td>{{ detail.interceptionRate }}%</td>
                      <td>{{ detail.distanceKm }}</td>
                      <td>{{ detail.assignmentScore }}</td>
                      <td>{{ detail.dispatchCost }}</td>
                    </tr>
                  </tbody>
                </table>
              </article>
            </div>
          </template>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dynamic-panel {
  --bg-card: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(241, 247, 252, 0.98));
  --text-main: #0f2940;
  --text-sub: #5e758b;
  --line: rgba(16, 36, 58, 0.12);
  --blue-1: #0f3f67;
  --blue-2: #1382a5;
  --ink: #081d30;
  min-height: 100%;
  color: var(--text-main);
}

.panel-shell {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
}

.left-card,
.right-card,
.select-card,
.chart-card,
.plan-card {
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(10, 37, 62, 0.08);
}

.left-card {
  padding: 18px;
  position: sticky;
  top: 0;
  align-self: start;
}

.right-card {
  padding: 18px;
}

.section-head h3,
.plan-head h4 {
  margin: 0;
  font-size: 1.08rem;
}

.section-head.compact {
  margin-bottom: 12px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--text-sub);
  font-size: 0.76rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.live-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 6px;
}

.live-chip {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid #b7c9da;
  background: #eff5fa;
  color: #1f4a6b;
  font-size: 0.8rem;
}

.field {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.field span {
  font-size: 0.88rem;
  font-weight: 700;
}

.field input,
.field select {
  width: 100%;
  min-height: 42px;
  padding: 8px 12px;
  border: 1px solid #c4d3e1;
  border-radius: 10px;
  background: #f8fbff;
  font-size: 0.92rem;
  line-height: 1.2;
  box-sizing: border-box;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.field-grid .field {
  gap: 6px;
  margin-top: 12px;
}

.field-grid .field span {
  font-size: 0.86rem;
}

.field-grid .field input,
.field-grid .field select {
  min-height: 40px;
  padding: 6px 10px;
  border-radius: 9px;
}

.switch-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  color: #28465f;
  font-size: 0.9rem;
}

.switch-list label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.generate-btn,
.reset-btn,
.status-btn,
.tab-btn {
  border: none;
  cursor: pointer;
}

.primary-actions {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.generate-btn,
.reset-btn,
.status-btn {
  width: 100%;
  padding: 12px 16px;
  border-radius: 14px;
  font-weight: 700;
}

.generate-btn {
  color: #fff;
  background: linear-gradient(135deg, var(--blue-1), var(--blue-2));
}

.reset-btn,
.status-btn {
  min-width: 104px;
  color: #17384f;
  border: 1px solid #b8cad8;
  background: #f2f7fb;
}

.status-btn {
  min-width: 72px;
}

.secondary-actions {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 10px;
}

.mini-btn {
  margin-right: 6px;
  border: 1px solid #bfd0df;
  background: #f4f8fc;
  color: #22445f;
  border-radius: 8px;
  cursor: pointer;
  padding: 4px 8px;
  font-weight: 700;
}

.mini-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.generate-btn:disabled,
.reset-btn:disabled,
.status-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.notice-text {
  margin-top: 10px;
  color: #1e7d55;
}

.error-text,
.warning-text {
  margin-top: 10px;
  color: #b53c2b;
}

.tab-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.tab-btn {
  padding: 10px 16px;
  border-radius: 999px;
  background: #e5eef6;
  color: #183550;
  font-weight: 700;
}

.tab-btn.active {
  background: linear-gradient(135deg, #133f63, #178cab);
  color: #fff;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.plain-btn {
  border: 1px solid #bfd0df;
  background: #f4f8fc;
  color: #22445f;
  border-radius: 10px;
  cursor: pointer;
  padding: 8px 10px;
}

.select-grid {
  display: grid;
  gap: 14px;
}

.select-card {
  padding: 12px;
}

.select-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.select-card h4 {
  margin: 0;
}

.table-wrap {
  max-height: 220px;
  overflow: auto;
  border: 1px solid #d5e1ec;
  border-radius: 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

th,
td {
  padding: 9px 8px;
  border-bottom: 1px solid #dce7ef;
  text-align: left;
}

th {
  color: #537084;
  background: #f2f7fb;
  position: sticky;
  top: 0;
}

.empty-row {
  text-align: center;
  color: #6b8194;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--ink), #17597f);
  color: #edf6ff;
  display: grid;
  gap: 6px;
}

.summary-item strong {
  font-size: 1.32rem;
}

.chart-card {
  padding: 14px;
  margin-bottom: 14px;
}

.bar-stack {
  display: grid;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr) 70px;
  gap: 10px;
  align-items: center;
}

.bar-label {
  font-weight: 700;
  color: #274963;
}

.bar-track {
  height: 12px;
  border-radius: 999px;
  overflow: hidden;
  background: #d9e6ef;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2e6d96, #28a5b6);
}

.plan-grid {
  display: grid;
  gap: 14px;
}

.plan-card {
  padding: 14px;
}

.plan-card.recommended {
  border-color: rgba(15, 121, 152, 0.32);
  box-shadow: 0 16px 34px rgba(9, 91, 122, 0.16);
}

.plan-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
}

.plan-name {
  margin: 0 0 4px;
  color: #637a90;
}

.plan-head strong {
  font-size: 1.4rem;
  color: #0b5f80;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 12px 0;
}

.chip {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.84rem;
  color: #17566f;
  background: #e4f2f7;
}

.chip.outline {
  border: 1px solid #bccfdd;
  color: #2b4862;
  background: transparent;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
  color: #678093;
}

@media (max-width: 1260px) {
  .panel-shell {
    grid-template-columns: 1fr;
  }

  .left-card {
    position: static;
  }
}

@media (max-width: 860px) {
  .field-grid,
  .summary-grid,
  .action-row {
    grid-template-columns: 1fr;
  }

  .bar-row {
    grid-template-columns: 112px minmax(0, 1fr) 62px;
  }
}
</style>

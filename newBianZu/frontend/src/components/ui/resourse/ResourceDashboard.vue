<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { resApi } from '@/api/resource';
import { simApi } from '@/api/simulation';
import { useResStore } from '@/stores/resource';
import type { EnemyType } from '@/model/enemyType';
import type { FireType } from '@/model/fireType';
import type { ProtectionZone } from '@/model/protectionZone';
import type { WeaponNode } from '@/model/weaponNode';
import type { WeaponType } from '@/model/weaponType';
import EnemyList from './list/EnemyTypes.vue';
import WeaponList from './list/WeaponTypes.vue';
import FireUnitList from './list/FireTypes.vue';
import AddEnemyType from './add/AddEnemyType.vue';
import AddWeaponType from './add/AddWeaponType.vue';
import AddFireType from './add/AddFireType.vue';

type TabKey = 'enemy' | 'weapon' | 'fire';
type FormMode = 'add' | 'edit';

interface FormChild {
  submitForm: () => Promise<boolean>;
  resetForm?: () => void;
}

const store = useResStore();
const activeTab = ref<TabKey>('enemy');
const isRefreshing = ref(false);
const isInitNodesLoading = ref(false);
const deletingWeaponNodeId = ref('');
const isListMode = ref(true);
const formMode = ref<FormMode>('add');
const notice = ref('');
const error = ref('');
const zones = ref<ProtectionZone[]>([]);
const weaponNodes = ref<WeaponNode[]>([]);
const showNodeForm = ref(false);
const showNodeDrawer = ref(false);
const nodeForm = ref({
  type: '',
  count: 1,
  status: 0,
  zoneId: '',
  ammoStates: [] as Array<{ fireUnitType: string; currentCount: number }>,
});
const editingEnemyType = ref<EnemyType | null>(null);
const editingWeaponType = ref<WeaponType | null>(null);
const editingFireType = ref<FireType | null>(null);

const addEnemyTypeRef = ref<FormChild | null>(null);
const addWeaponTypeRef = ref<FormChild | null>(null);
const addFireTypeRef = ref<FormChild | null>(null);

const tabRefMap = {
  enemy: addEnemyTypeRef,
  weapon: addWeaponTypeRef,
  fire: addFireTypeRef,
};

const weaponNodeRows = computed(() => weaponNodes.value.map(node => ({
  id: node.id,
  type: node.type,
  statusText: weaponStatusLabel(Number(node.status ?? 0)),
  ammoText: (node.ammoStates || [])
    .map(ammo => `${ammo.fireUnitType}: ${ammo.currentCount}`)
    .join('；') || '-',
})));

onMounted(() => {
  store.loadAllDictionaries();
  loadSnapshotData();
});

function weaponStatusLabel(status: number) {
  if (status === 0) return '待命';
  if (status === 1) return '分配中';
  if (status === 2) return '交战中';
  return `未知(${status})`;
}

function clearEditingState() {
  editingEnemyType.value = null;
  editingWeaponType.value = null;
  editingFireType.value = null;
}

function openAdd(tab: TabKey) {
  activeTab.value = tab;
  formMode.value = 'add';
  clearEditingState();
  isListMode.value = false;
}

function openEditEnemy(item: EnemyType) {
  activeTab.value = 'enemy';
  formMode.value = 'edit';
  editingEnemyType.value = { ...item };
  editingWeaponType.value = null;
  editingFireType.value = null;
  isListMode.value = false;
}

function openEditWeapon(item: WeaponType) {
  activeTab.value = 'weapon';
  formMode.value = 'edit';
  editingWeaponType.value = {
    ...item,
    fireTypes: item.fireTypes ? item.fireTypes.map(fireType => ({ ...fireType })) : [],
  };
  editingEnemyType.value = null;
  editingFireType.value = null;
  isListMode.value = false;
}

function openEditFire(item: FireType) {
  activeTab.value = 'fire';
  formMode.value = 'edit';
  editingFireType.value = { ...item };
  editingEnemyType.value = null;
  editingWeaponType.value = null;
  isListMode.value = false;
}

function backToList() {
  isListMode.value = true;
  formMode.value = 'add';
  clearEditingState();
}

function resetCurrentForm() {
  tabRefMap[activeTab.value].value?.resetForm?.();
}

async function loadSnapshotData() {
  try {
    const response = await simApi.getSimulationSnapshot();
    zones.value = response?.data?.zones || [];
    weaponNodes.value = response?.data?.weaponNodes || [];
  } catch {
    zones.value = [];
    weaponNodes.value = [];
  }
}

async function onRefresh() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    await Promise.all([store.reloadDictionaries(), loadSnapshotData()]);
  } finally {
    isRefreshing.value = false;
  }
}

async function initWeaponNodes() {
  notice.value = '';
  error.value = '';
  const ok = window.confirm('初始化演示节点会覆盖当前 weapon_nodes，仅适合演示数据。确定继续吗？');
  if (!ok) return;

  isInitNodesLoading.value = true;
  try {
    await simApi.initWeaponNodes();
    await loadSnapshotData();
    notice.value = '演示武器节点已初始化。';
  } catch (requestError: any) {
    error.value = requestError?.message || '初始化演示节点失败';
  } finally {
    isInitNodesLoading.value = false;
  }
}

function openNodeForm() {
  const firstWeapon = Array.from(store.weaponTypeMap.values())[0];
  nodeForm.value = {
    type: firstWeapon?.type || '',
    count: 1,
    status: 0,
    zoneId: '',
    ammoStates: (firstWeapon?.fireTypes || []).map(item => ({
      fireUnitType: String(item.fireType || ''),
      currentCount: Number(item.quantity || 0),
    })),
  };
  showNodeForm.value = true;
  loadSnapshotData();
}

function openNodeDrawer() {
  showNodeDrawer.value = true;
  loadSnapshotData();
}

function syncNodeAmmoFromType() {
  const weaponType = store.weaponTypeMap.get(nodeForm.value.type);
  nodeForm.value.ammoStates = (weaponType?.fireTypes || []).map(item => ({
    fireUnitType: String(item.fireType || ''),
    currentCount: Number(item.quantity || 0),
  }));
}

async function createWeaponNodes() {
  notice.value = '';
  error.value = '';
  if (!nodeForm.value.type) {
    error.value = '请选择武器类型';
    return;
  }

  try {
    const response = await resApi.createWeaponNodes({
      type: nodeForm.value.type,
      count: Number(nodeForm.value.count || 1),
      status: Number(nodeForm.value.status || 0),
      zoneId: nodeForm.value.zoneId || undefined,
      ammoStates: nodeForm.value.ammoStates.map(item => ({
        fireUnitType: item.fireUnitType,
        currentCount: Number(item.currentCount || 0),
      })),
    });
    if (response?.code !== 200) {
      error.value = response?.message || '新增武器节点失败';
      return;
    }
    const createdCount = response.data?.created?.length || 0;
    notice.value = `已新增 ${createdCount} 个实际武器节点。`;
    showNodeForm.value = false;
    await loadSnapshotData();
  } catch (requestError: any) {
    error.value = requestError?.message || '新增武器节点失败';
  }
}

async function deleteWeaponNode(weaponId: string) {
  notice.value = '';
  error.value = '';
  const ok = window.confirm(`确定删除武器节点 ${weaponId} 吗？`);
  if (!ok) return;

  deletingWeaponNodeId.value = weaponId;
  try {
    const response = await resApi.deleteWeaponNode(weaponId);
    if (response?.code !== 200) {
      error.value = response?.message || '删除武器节点失败';
      return;
    }
    weaponNodes.value = response.data || [];
    await loadSnapshotData();
    notice.value = `已删除武器节点 ${weaponId}。`;
  } catch (requestError: any) {
    error.value = requestError?.message || '删除武器节点失败';
  } finally {
    deletingWeaponNodeId.value = '';
  }
}

async function handlePrimaryAction() {
  const currentRef = tabRefMap[activeTab.value];
  if (!currentRef.value) {
    console.warn('当前表单未加载。');
    return;
  }

  const submitResult = await currentRef.value.submitForm();
  if (submitResult) {
    await store.reloadDictionaries();
    backToList();
  }
}
</script>

<template>
  <div v-if="isListMode" class="dashboard-container">
    <div class="tabs">
      <button :class="{ active: activeTab === 'enemy' }" @click="activeTab = 'enemy'">敌方类型</button>
      <button :class="{ active: activeTab === 'weapon' }" @click="activeTab = 'weapon'">武器装备</button>
      <button :class="{ active: activeTab === 'fire' }" @click="activeTab = 'fire'">弹药类型</button>
      <header class="refresh-header">
        <button v-if="activeTab === 'weapon'" class="refresh-btn" type="button" @click="openNodeForm">
          新增武器节点
        </button>
        <button v-if="activeTab === 'weapon'" class="refresh-btn" type="button" @click="openNodeDrawer">
          查看已有节点
        </button>
        <button
          v-if="activeTab === 'weapon'"
          class="refresh-btn"
          type="button"
          @click="initWeaponNodes"
          :disabled="isInitNodesLoading"
        >
          {{ isInitNodesLoading ? '初始化中...' : '初始化演示节点' }}
        </button>
        <button class="refresh-btn" :class="{ loading: isRefreshing }" type="button" @click="onRefresh" :disabled="isRefreshing">
          <span v-if="isRefreshing" class="spinner" aria-hidden="true"></span>
          <span>{{ isRefreshing ? '刷新中...' : '刷新' }}</span>
        </button>
      </header>
    </div>

    <div class="content-area">
      <section v-if="activeTab === 'weapon' && showNodeForm" class="node-form">
        <label>
          <span>武器类型</span>
          <select v-model="nodeForm.type" @change="syncNodeAmmoFromType">
            <option v-for="item in Array.from(store.weaponTypeMap.values())" :key="item.type" :value="item.type">
              {{ item.type }}
            </option>
          </select>
        </label>
        <label>
          <span>数量</span>
          <input v-model.number="nodeForm.count" type="number" min="1" max="100" />
        </label>
        <label>
          <span>初始状态</span>
          <select v-model.number="nodeForm.status">
            <option :value="0">待命</option>
            <option :value="1">分配中</option>
            <option :value="2">交战中</option>
          </select>
        </label>
        <label>
          <span>所属保护区</span>
          <select v-model="nodeForm.zoneId">
            <option value="">不分配</option>
            <option v-for="zone in zones" :key="zone.id" :value="String(zone.id)">{{ zone.id }}</option>
          </select>
        </label>
        <div class="ammo-editor">
          <span>弹药数量</span>
          <div v-for="ammo in nodeForm.ammoStates" :key="ammo.fireUnitType" class="ammo-row">
            <span>{{ ammo.fireUnitType }}</span>
            <input v-model.number="ammo.currentCount" type="number" min="0" />
          </div>
        </div>
        <div class="node-form-actions">
          <button type="button" class="btn primary" @click="createWeaponNodes">生成节点</button>
          <button type="button" class="btn ghost" @click="showNodeForm = false">取消</button>
        </div>
      </section>

      <EnemyList v-if="activeTab === 'enemy'" @add-enemy="openAdd('enemy')" @edit-enemy="openEditEnemy" />
      <WeaponList v-if="activeTab === 'weapon'" @add-weapon="openAdd('weapon')" @edit-weapon="openEditWeapon" />
      <FireUnitList v-if="activeTab === 'fire'" @add-firetype="openAdd('fire')" @edit-firetype="openEditFire" />
    </div>
    <p v-if="notice" class="notice-text">{{ notice }}</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <div v-if="showNodeDrawer" class="drawer-mask" @click.self="showNodeDrawer = false">
      <aside class="node-drawer">
        <header>
          <h3>已有武器节点</h3>
          <button type="button" @click="showNodeDrawer = false">关闭</button>
        </header>
        <div class="drawer-list">
          <article v-for="node in weaponNodeRows" :key="node.id" class="node-row">
            <div class="node-main">
              <strong>{{ node.id }}</strong>
              <button
                type="button"
                class="danger-btn"
                :disabled="deletingWeaponNodeId === node.id"
                @click="deleteWeaponNode(node.id)"
              >
                {{ deletingWeaponNodeId === node.id ? '删除中...' : '删除' }}
              </button>
            </div>
            <span>{{ node.type }}</span>
            <span>{{ node.statusText }}</span>
            <small>{{ node.ammoText }}</small>
          </article>
          <p v-if="!weaponNodeRows.length" class="empty-text">暂无武器节点</p>
        </div>
      </aside>
    </div>
  </div>

  <div v-else class="addresource-container">
    <AddEnemyType
      v-if="activeTab === 'enemy'"
      ref="addEnemyTypeRef"
      :mode="formMode"
      :initial-value="editingEnemyType"
    />
    <AddWeaponType
      v-if="activeTab === 'weapon'"
      ref="addWeaponTypeRef"
      :mode="formMode"
      :initial-value="editingWeaponType"
    />
    <AddFireType
      v-if="activeTab === 'fire'"
      ref="addFireTypeRef"
      :mode="formMode"
      :initial-value="editingFireType"
    />
    <div class="actions full">
      <button type="button" class="btn primary" @click="handlePrimaryAction">保存</button>
      <button type="button" class="btn reset" @click="resetCurrentForm">重置</button>
      <button type="button" class="btn ghost" @click="backToList">返回</button>
    </div>
  </div>
</template>

<style scoped>
.dashboard-container { padding: 20px; }
.tabs { display: flex; gap: 10px; margin-bottom: 20px; }
.tabs button.active { background: #007bff; color: white; }
.refresh-header { margin-left: auto; display: inline-flex; gap: 8px; }
.refresh-btn {
  background: transparent;
  border: 1px solid #e6e6e6;
  padding: 6px 10px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.refresh-btn[disabled] { opacity: 0.7; cursor: not-allowed; }
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0,0,0,0.12);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}
@keyframes spin { to { transform: rotate(360deg); } }
.refresh-btn.loading { background: rgba(37,99,235,0.06); border-color: rgba(37,99,235,0.12); }
.node-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 12px;
  padding: 14px;
  margin-bottom: 14px;
  border: 1px solid #e6e9ef;
  border-radius: 8px;
  background: #fff;
}
.node-form label,
.ammo-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.node-form input,
.node-form select {
  padding: 7px 9px;
  border: 1px solid #e6e9ef;
  border-radius: 6px;
}
.ammo-editor { grid-column: 1 / -1; }
.ammo-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 120px;
  align-items: center;
  gap: 8px;
}
.node-form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  z-index: 30;
  display: flex;
  justify-content: flex-end;
}
.node-drawer {
  width: min(420px, 92vw);
  height: 100%;
  background: #fff;
  box-shadow: -8px 0 24px rgba(15, 23, 42, 0.18);
  padding: 18px;
  overflow: auto;
}
.node-drawer header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.node-drawer header h3 { margin: 0; }
.node-drawer header button {
  border: 1px solid #e6e9ef;
  background: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
}
.drawer-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.node-row {
  display: grid;
  gap: 4px;
  border: 1px solid #e6e9ef;
  border-radius: 8px;
  padding: 10px;
}
.node-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.danger-btn {
  border: 1px solid #fecaca;
  color: #dc2626;
  background: #fff;
  border-radius: 6px;
  padding: 4px 8px;
  cursor: pointer;
}
.danger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.node-row small { color: #64748b; }
.empty-text { color: #64748b; }
.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 0 18px 18px;
}
.btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #e6e9ef;
  cursor: pointer;
  font-weight: 600;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.reset { background: #f8fafc; color: #374151; }
.btn.ghost { background: transparent; color: #374151; }
.notice-text { color: #047857; margin: 8px 12px; }
.error-text { color: #dc2626; margin: 8px 12px; }
</style>

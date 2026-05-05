<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useResStore } from '@/stores/resource';

// 引入三个子组件
// import EnemyList from './components/EnemyList.vue';
// import WeaponList from './components/WeaponList.vue';
// import FireUnitList from './components/FireUnitList.vue';
import EnemyList from './list/EnemyTypes.vue';
import WeaponList from './list/WeaponTypes.vue';
import FireUnitList from './list/FireTypes.vue';
import AddEnemyType from './add/AddEnemyType.vue';
import AddWeaponType from './add/AddWeaponType.vue';
import AddFireType from './add/AddFireType.vue';

// 1. 定义合法的 tab 名称类型（字面量联合类型）
type TabKey = 'enemy' | 'weapon' | 'fire';

const activeTab = ref<TabKey>('enemy');
const store = useResStore();

onMounted(() => {
  store.loadAllDictionaries();
});

// 是否处于刷新动画中
const isRefreshing = ref(false);

// 占位刷新函数，当前仅作日志记录，具体逻辑后续实现
function onRefresh() {
  if (isRefreshing.value) return; // 防止重复点击
  isRefreshing.value = true;
  store.reloadDictionaries().finally(() => {
    isRefreshing.value = false;
  });
}

const isListMode = ref(true);

// 定义子组件的类型：包含暴露的 submitForm 方法
interface FormChild {
  // 提交方法：返回 Promise<boolean> 表示成功/失败
  submitForm: () => Promise<boolean>;
  // 可选：如果有重置方法也可以加
  resetForm?: () => void;
}

const addEnemyTypeRef = ref<FormChild | null>(null);
const addWeaponTypeRef = ref<FormChild | null>(null);
const addFireTypeRef = ref<FormChild | null>(null);


// 3. 映射关系：activeTab → 子组件 ref 实例
const tabRefMap = {
  enemy: addEnemyTypeRef,
  weapon: addWeaponTypeRef,
  fire: addFireTypeRef
};


const handPrimaryAction = async() => {
  const currentRef = tabRefMap[activeTab.value];
  // 第二步：校验子组件是否挂载（避免 v-if 未渲染时调用）
  if (!currentRef || !currentRef.value) {
    console.warn('当前表单未加载，无法提交！');
    return;
  }

  // 第三步：调用子组件的 submitForm 方法
  const submitResult = await currentRef.value.submitForm();


  // 第四步：父组件统一处理提交结果（比如刷新列表、关闭弹窗）
  if (submitResult) {
    console.log('父组件确认：当前表单提交成功');
    // 这里可以加父组件的后续逻辑，比如跳转、重置等
  }
}; 

</script>

<template>
  <div class="dashboard-container" v-if="isListMode">
    <div class="tabs">
      <button :class="{ active: activeTab === 'enemy' }" @click="activeTab = 'enemy'">敌方武器</button>
      <button :class="{ active: activeTab === 'weapon' }" @click="activeTab = 'weapon'">武器装备</button>
      <button :class="{ active: activeTab === 'fire' }" @click="activeTab = 'fire'">弹药类型</button>
      <header class="refresh-header">
        <button class="refresh-btn" :class="{ loading: isRefreshing }" title="刷新" @click="onRefresh" :disabled="isRefreshing">
          <span v-if="isRefreshing" class="spinner" aria-hidden="true"></span>
          <span>{{ isRefreshing ? '刷新中...' : '↻ 刷新' }}</span>
        </button>
      </header>
    </div>
    <div class="content-area">
      <!-- 动态组件切换，或者用 v-if -->
      <EnemyList v-if="activeTab === 'enemy'" @add-enemy="isListMode=false" />
      <WeaponList v-if="activeTab === 'weapon'" @add-weapon="isListMode=false" />
      <FireUnitList v-if="activeTab === 'fire'" @add-firetype="isListMode=false" />
    </div>
  </div>

  <div v-else class="addresource-container">
    <!-- <button @click="isListMode=true">返回</button> -->
    <AddEnemyType v-if="activeTab === 'enemy'" ref="addEnemyTypeRef">  </AddEnemyType>
    <AddWeaponType v-if="activeTab === 'weapon'" ref="addWeaponTypeRef">  </AddWeaponType>
    <AddFireType v-if="activeTab === 'fire'" ref="addFireTypeRef">  </AddFireType>
    <div class="actions full">
      <button type="submit" class="btn primary" @click="handPrimaryAction">保存</button>
      <button type="button" class="btn reset" >重置</button>
      <button type="button" class="btn ghost" @click="isListMode=true" >返回</button>
    </div>
  </div>
</template>

<style scoped>
/* 简单的样式布局 */
.dashboard-container { padding: 20px; }
.tabs { display: flex; gap: 10px; margin-bottom: 20px; }
.tabs button.active { background: #007bff; color: white; }

/* refresh button minimal style to match existing look */
.refresh-header {
  margin-left: auto;
}

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

/* 小型圆形加载指示器 */
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

/* 加 loading 类时可添加额外视觉 */
.refresh-btn.loading { background: rgba(37,99,235,0.06); border-color: rgba(37,99,235,0.12); }
</style>
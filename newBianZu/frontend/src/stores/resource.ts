import { defineStore } from "pinia";
import { ref } from "vue";
import type { WeaponType } from "@/model/weaponType";
import type { FireType } from "@/model/fireType";
import type { EnemyType } from "@/model/enemyType";
import { resApi } from "@/api/resource";

export const useResStore = defineStore('resource',()=>{
    const weaponTypeMap = ref<Map<string, WeaponType>>(new Map());
    const fireTypeMap = ref<Map<string, FireType>>(new Map());
    const enemyTypeMap = ref<Map<string, EnemyType>>(new Map());

    // 加载状态标记 (防止重复加载)
    const isLoaded = ref(false);

    // ================= Actions (动作) =================

    /**
     * 初始化加载所有字典
     * 建议在 App.vue 的 onMounted 中调用
     */
    async function loadAllDictionaries() {
    if (isLoaded.value) return; // 避免重复请求
    try {
      console.log('store/resource.ts:正在加载静态资源字典...');
      
      // 并行请求后端接口
      const [weapons, fires, enemies] = await Promise.all([
        resApi.getWeaponTypes(), // 需确保 api/resource.ts 已实现
        resApi.getFireTypes(),
        resApi.getEnemyTypes()   // 如果还没实现，可以先注释
      ]);

      // console.log(weapons);
      // 转换 List -> Map
      if (weapons) {
        weaponTypeMap.value = new Map(weapons.map(w => [String(w.type), w]));
      }
      if (fires) {
        fireTypeMap.value = new Map(fires.map(f => [String(f.type), f]));
      }
      if (enemies) {
        enemyTypeMap.value = new Map(enemies.map(e => [String(e.type), e]));
      }

      isLoaded.value = true;
      console.log(`store/resource.ts:资源加载完毕: 武器(${weaponTypeMap.value.size}), 弹药(${fireTypeMap.value.size}), 敌军(${enemyTypeMap.value.size})`);
      
    } catch (error) {
      console.error('store/resource.ts:资源字典加载失败:', error);
      // 这里可以根据情况决定是否重置 isLoaded
    }
  }

  async function reloadDictionaries() {
    isLoaded.value = false;
    await loadAllDictionaries();
  }

  async function reLoadEnemyTypes() {
    try {
      const enemies = await resApi.getEnemyTypes();
      enemyTypeMap.value = new Map(enemies.map(e => [String(e.type), e]));
      console.log('store/resource.ts:敌方类型重新加载完成，当前数量:', enemyTypeMap.value.size);
    } catch (error) {
      console.error('store/resource.ts:敌方类型加载失败:', error);
    }
  }

  async function reLoadWeaponTypes() {
    try {
      const weapons = await resApi.getWeaponTypes(); 
      weaponTypeMap.value = new Map(weapons.map(w => [String(w.type), w]));
    } catch (error) {
      console.error('store/resource.ts:武器类型加载失败:', error);
    }
  }

  async function reLoadFireTypes() {
    try {
      const fires = await resApi.getFireTypes(); 
      fireTypeMap.value = new Map(fires.map(f => [String(f.type), f]));
    } catch (error) {
      console.error('store/resource.ts:火力类型加载失败:', error);
    }
  }

    return {
      // 响应式变量
      weaponTypeMap,
      fireTypeMap,
      enemyTypeMap,
      isLoaded,
      // 方法
      loadAllDictionaries,
      reloadDictionaries,
      reLoadEnemyTypes,
      reLoadWeaponTypes,
      reLoadFireTypes,
    };

})


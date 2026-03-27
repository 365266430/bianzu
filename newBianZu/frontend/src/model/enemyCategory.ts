export enum EnemyCategory {
  FIGHTER = 'FIGHTER',
  BOMBER = 'BOMBER',
  MISSILE = 'MISSILE',
  BALLISTIC = 'BALLISTIC',
  UAV = 'UAV'
}

export const EnemyCategoryLabel: Record<EnemyCategory, string> = {
  [EnemyCategory.FIGHTER]: '战斗机',
  [EnemyCategory.BOMBER]: '轰炸机',
  [EnemyCategory.MISSILE]: '导弹',
  [EnemyCategory.BALLISTIC]: '弹道导弹',
  [EnemyCategory.UAV]: '无人机'
};

// 类型别名，表示允许的枚举值
export type EnemyCategoryValue = EnemyCategory;

// 供表单下拉使用的静态选项
export const ENEMY_CATEGORY_OPTIONS: Array<{ value: EnemyCategoryValue; label: string }> = (
  Object.values(EnemyCategory) as EnemyCategoryValue[]
).map((v) => ({ value: v, label: EnemyCategoryLabel[v] }));

export default EnemyCategory;
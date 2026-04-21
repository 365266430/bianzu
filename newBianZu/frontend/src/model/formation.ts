import type { EnemyType } from '@/model/enemyType'
import type { FireType } from '@/model/fireType'
import type { WeaponType } from '@/model/weaponType'

export interface StaticFormationConfig {
  algorithmType: string
  distanceWeight: number
  firepowerWeight: number
  defenseWeight: number
  maxGroupSize: number
  maxIterations: number
  particleCount: number
  planCount: number
  allowedWeaponTypes: string[]
  selectedParadigms: string[]
}

export interface StaticFormationResult {
  algorithmType: string
  recommendedPlanId: string | null
  enemyCount: number
  weaponCount: number
  zoneCount: number
  totalAmmo: number
  totalChannels: number
  supportedParadigms: string[]
  domainResourceSummaries: DomainResourceSummary[]
  firepowerResourceSummaries: FirepowerResourceSummary[]
  plans: FormationPlan[]
}

export interface DomainResourceSummary {
  deployDomain: string
  weaponNodeCount: number
  ammoCount: number
  channelCount: number
}

export interface FirepowerResourceSummary {
  deployDomain: string
  weaponType: string
  fireType: string
  weaponNodeCount: number
  ammoCount: number
  channelCount: number
  averageInterceptionRate: number
  maxRange: number
}

export interface FormationPlan {
  planId: string
  planName: string
  paradigm: string
  feasible: boolean
  summary: string
  fitnessScore: number
  distanceScore: number
  firepowerScore: number
  defenseScore: number
  coverageScore: number
  expectedInterceptionRate: number
  estimatedCost: number
  groupSize: number
  allocatedEnemyCount: number
  participatingDomains: string[]
  warnings: string[]
  details: FormationAllocationDetail[]
}

export interface FormationAllocationDetail {
  weaponNodeId: string
  weaponType: string
  fireType: string
  targetEnemyId: string
  targetEnemyType: string
  sourceZoneId: string
  deployDomain: string
  distanceKm: number
  assignmentScore: number
  estimatedInterceptionRate: number
}

export interface DynamicFormationRequest {
  selectedWeaponTypes: string[]
  selectedEnemyIds: string[]
  paradigm: string
  zones: DynamicProtectionZone[]
  enemyNodes: DynamicEnemyNode[]
  weaponTypes: WeaponType[]
  fireTypes: FireType[]
  enemyTypes: EnemyType[]
  constraints: DynamicFormationConstraints
  config: DynamicAlgorithmConfig
}

export interface DynamicFormationConstraints {
  requireAmmoSufficiency: boolean
  ammoThreshold: number
  requireFirepowerMatch: boolean
  considerDispatchCost: boolean
  dispatchCostWeight: number
  considerPositionRelation: boolean
  prioritizeInZoneEnemies: boolean
  minInterceptionRate: number
  maxGroupSize: number
}

export interface DynamicAlgorithmConfig {
  algorithmType: string
  learningRate: number
  gamma: number
  epsilon: number
  targetUpdateFreq: number
  replayBufferSize: number
  epochs: number
  stepsPerEpoch: number
  batchSize: number
  distanceWeight: number
  firepowerWeight: number
  defenseWeight: number
  maxGroupSize: number
  planCount: number
}

export interface DynamicProtectionZone {
  id: string
  location: [number, number]
  size: number
  value: number
  health: number
  stationedWeaponIds: string[]
}

export interface DynamicEnemyNode {
  id: string
  type: string
  longitude: number
  latitude: number
  altitude: number
  heading: number
  speed: number
}

export interface DynamicFormationResult {
  algorithmType: string
  recommendedPlanId: string | null
  paradigm: string
  enemyCount: number
  weaponCount: number
  totalAmmo: number
  totalChannels: number
  estimatedCost: number
  plans: DynamicFormationPlan[]
}

export interface DynamicFormationPlan {
  planId: string
  planName: string
  feasible: boolean
  fitnessScore: number
  groupSize: number
  allocatedEnemyCount: number
  participatingDomains: string[]
  warnings: string[]
  details: DynamicAllocationDetail[]
}

export interface DynamicAllocationDetail {
  weaponNodeId: string
  weaponType: string
  fireType: string
  deployDomain: string
  targetEnemyId: string
  targetEnemyType: string
  sourceZoneId: string
  targetInZone: boolean
  distanceKm: number
  assignmentScore: number
  interceptionRate: number
  ammoSufficient: boolean
  ammoBefore: number
  ammoAfter: number
  dispatchCost: number
}

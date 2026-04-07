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

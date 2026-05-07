# DQN 动态编组建模方案

本文档说明如何结合项目现有数据结构，将动态编组算法升级为基于 DQN（Deep Q-Network）的强化学习决策方案。

当前项目已经具备动态编组所需的核心数据与仿真流程：

- 后端动态编组入口：`bianzu-backend/src/main/java/com/bianzu/bianzu_backend/service/DynamicFormationPlanningService.java`
- 仿真动态编组处理器：`bianzu-backend/src/main/java/com/bianzu/bianzu_backend/processor/DynamicFormationProcessor.java`
- 仿真流水线：`bianzu-backend/src/main/java/com/bianzu/bianzu_backend/service/SimulationEngineService.java`
- 仿真上下文：`bianzu-backend/src/main/java/com/bianzu/bianzu_backend/model/SimulationContext.java`

## 当前实现状态

截至当前版本，项目已经完成了 DQN 动态编组的第一阶段和第二阶段雏形：

```text
已完成：DQN 候选动作建模
已完成：候选动作特征向量构造
已完成：即时奖励计算器
已完成：经验回放缓冲区 ReplayBuffer
已完成：神经网络 Q 估计器
已完成：在线 TD 更新
已完成：Target Network
已完成：epsilon-greedy 在线探索
已完成：模型持久化
已完成：跨 tick 的 s -> s' episode 经验链
已完成：动态方案生成接口接入
已完成：仿真 tick 动态编组接入
未完成：离线训练任务与评估报表
```

需要特别说明：当前版本还不是严格意义上的深度神经网络 DQN。

当前实现可以描述为：

```text
当前版本 = DQN 工程结构 + 神经网络 Q 模型 + 经验回放 + 启发式冷启动
```

它已经不再是纯固定启发式评分，也不再是线性 Q 学习模型。当前 `DqnNeuralQModel` 使用 Java 实现了一个小型 MLP 网络：

```text
input feature vector -> hidden layer(32, ReLU) -> output(sigmoid Q)
```

当前已经补齐 DQN 的核心在线训练结构，包括 Target Network、epsilon-greedy 探索、模型持久化，以及跨 tick 的 `s -> s'` episode 经验链。后续仍需要更完整的离线训练任务和评估报表。

当前新增的核心类：

| 类 | 作用 |
| --- | --- |
| `algorithm/dqn/model/DqnAction.java` | 定义动作 `weaponId + fireType + enemyId` |
| `algorithm/dqn/model/DqnFeatureVector.java` | 保存候选动作特征向量 |
| `algorithm/dqn/model/DqnScoredAction.java` | 保存候选动作、特征、启发式分数和 Q 值 |
| `algorithm/dqn/model/DqnExperience.java` | 定义经验样本 `(s, a, r, s', done)` |
| `algorithm/dqn/DqnFeatureBuilder.java` | 从敌方、武器、弹种、保护区构造特征 |
| `algorithm/dqn/DqnRewardCalculator.java` | 计算即时奖励 |
| `algorithm/dqn/DqnReplayBuffer.java` | 保存经验回放样本 |
| `algorithm/dqn/DqnNeuralQModel.java` | 当前神经网络 Q 估计器，结构为 input -> hidden(32, ReLU) -> output(sigmoid) |
| `algorithm/dqn/DqnTrainingService.java` | 负责写入经验并执行 TD 更新 |
| `algorithm/dqn/DqnPolicyService.java` | 对候选动作输出最终 Q 值 |
| `algorithm/dqn/DqnModelPersistenceService.java` | 保存和加载神经网络参数 |
| `algorithm/dqn/model/DqnModelSnapshot.java` | 模型权重快照 DTO |

当前接入点：

| 文件 | 接入内容 |
| --- | --- |
| `service/DynamicFormationPlanningService.java` | 接口生成动态编组方案时，候选动作通过 `DqnPolicyService` 评分，并调用 `DqnTrainingService` 在线更新 |
| `processor/DynamicFormationProcessor.java` | 仿真 tick 中，候选动作通过 `DqnPolicyService` 评分；排名最高动作接入跨 tick episode 经验链，其他候选作为即时经验补充训练 |

## 1. 建模目标

第一阶段不建议让 DQN 直接生成完整编组方案，而是让 DQN 替换当前的启发式候选排序逻辑。

现有流程可以概括为：

```text
敌方目标 + 武器资源 + 火力类型 + 保护区
-> 生成可行候选 weapon-fire-enemy
-> 按启发式分数排序
-> 按约束贪心生成动态编组方案
```

DQN 接入后的流程：

```text
敌方目标 + 武器资源 + 火力类型 + 保护区
-> 生成可行候选 weapon-fire-enemy
-> DQN 计算每个候选动作的 Q 值
-> 按 Q 值排序
-> 按约束贪心生成动态编组方案
```

这样可以复用现有射程、射高、弹药、部署域、保护区等硬约束逻辑，降低改造风险。

## 2. 状态 State 设计

DQN 的输入建议采用“候选动作级状态向量”，即每一个候选分配项都构造一条特征向量。

候选动作级状态由五类特征组成：

```text
state_action_vector =
[
  全局态势特征,
  当前目标特征,
  当前武器特征,
  当前弹种特征,
  武器-目标关系特征
]
```

### 2.1 全局态势特征

来自 `SimulationContext`：

| 特征 | 来源 | 含义 |
| --- | --- | --- |
| `step` | `SimulationContext.step` | 当前仿真步 |
| `enemyCount` | `SimulationContext.enemies` | 当前敌方目标数量 |
| `availableWeaponCount` | `SimulationContext.weapons` | 当前可用武器数量 |
| `totalAmmo` | `WeaponNode.ammoStates` | 当前总弹药量 |
| `zoneCount` | `SimulationContext.zones` | 保护区数量 |
| `paradigm` | `SimulationContext.paradigm` | 当前编组范式 |

### 2.2 目标特征

来自 `EnemyNode` 和 `EnemyType`：

| 特征 | 来源 | 含义 |
| --- | --- | --- |
| `longitude` | `EnemyNode.longitude` | 目标经度 |
| `latitude` | `EnemyNode.latitude` | 目标纬度 |
| `altitude` | `EnemyNode.altitude` | 目标高度 |
| `headingSin` / `headingCos` | `EnemyNode.heading` | 航向角编码 |
| `speed` | `EnemyNode.speed` | 当前速度 |
| `enemyValue` | `EnemyType.value` | 目标价值 |
| `damageCapability` | `EnemyType.damageCapability` | 目标毁伤能力 |
| `maxAttackRange` | `EnemyType.maxAttackRange` | 最大攻击半径 |
| `rcs` | `EnemyType.rcs` | 雷达散射截面 |
| `categoryOneHot` | `EnemyType.category` | 目标类别编码 |

### 2.3 武器特征

来自 `WeaponNode` 和 `WeaponType`：

| 特征 | 来源 | 含义 |
| --- | --- | --- |
| `weaponStatus` | `WeaponNode.status` | 武器状态 |
| `ammoCount` | `WeaponNode.ammoStates.currentCount` | 当前弹种余量 |
| `ammoRatio` | `WeaponNode.ammoStates` / `WeaponType.fireTypes` | 弹药余量比例 |
| `channelCount` | `WeaponType.channelCount` | 通道数 |
| `deployDomainOneHot` | `WeaponType.deployDomain` | 部署域编码 |
| `functionOneHot` | `WeaponType.function` | 装备功能编码 |

### 2.4 弹种特征

来自 `FireType`：

| 特征 | 来源 | 含义 |
| --- | --- | --- |
| `interception` | `FireType.interception` | 拦截概率 |
| `maxRange` | `FireType.maxRange` | 最大射程 |
| `minRange` | `FireType.minRange` | 最小射程 |
| `maxAlt` | `FireType.maxAlt` | 最大射高 |
| `minAlt` | `FireType.minAlt` | 最小射高 |
| `attCost` | `FireType.attCost` | 调度成本 |
| `fireCost` | `FireType.cost` | 火力成本 |

### 2.5 武器-目标关系特征

这部分是 DQN 最关键的输入，可复用当前动态编组服务中的计算逻辑。

| 特征 | 含义 |
| --- | --- |
| `distanceKm` | 武器所在保护区到目标距离 |
| `rangeMatched` | 是否满足射程约束 |
| `altitudeMatched` | 是否满足射高约束 |
| `targetInZone` | 目标是否进入保护区 |
| `nearestZoneValue` | 最近保护区价值 |
| `nearestZoneHealth` | 最近保护区健康度 |
| `timeToZone` | 预计到达保护区时间 |
| `headingToZoneScore` | 目标是否朝向保护区 |
| `dispatchCost` | 距离乘以弹种调度成本 |
| `threatScore` | 基于毁伤能力、攻击半径、入区状态计算的威胁分 |

## 3. 动作 Action 设计

动作定义为选择一个候选分配项：

```text
action = weaponId + fireType + enemyId
```

示例：

```text
HQ9-BJ-01 使用 HQ-9_Missile 拦截 E-001
```

同时保留一个特殊动作：

```text
NO_OP / STOP
```

用于当前没有合适分配，或者本轮编组提前结束。

由于每一帧的敌方目标、武器、弹药数量都可能变化，不建议将动作空间固定为全局巨大编号表。推荐采用动态候选动作空间：

```text
1. 先由规则层生成合法候选动作
2. DQN 对每个候选动作计算 Q 值
3. 按 Q 值排序
4. 由规则层执行最终约束过滤
```

最终约束仍由 Java 代码保证：

- 一个武器本轮最多分配一次
- 一个目标本轮最多接收一次主分配
- 弹药数量必须大于 0
- 必须满足射程和射高约束
- 必须满足部署域和编组范式约束
- 编组规模不超过 `maxGroupSize`

## 4. 奖励 Reward 设计

第一版奖励函数建议基于当前项目已有指标组合：

```text
reward =
  目标价值收益
+ 保护区防护收益
+ 拦截概率收益
+ 威胁处置收益
- 弹药消耗成本
- 调度成本
- 违规动作惩罚
- 高威胁目标漏防惩罚
```

推荐初始公式：

```text
reward =
  2.0 * enemyValueNorm * interception
+ 1.5 * inZoneBonus
+ 1.0 * threatScore
- 0.5 * ammoCostNorm
- 0.5 * dispatchCostNorm
- 2.0 * invalidActionPenalty
- 3.0 * highThreatUnassignedPenalty
```

其中：

| 变量 | 含义 |
| --- | --- |
| `enemyValueNorm` | 归一化后的目标价值 |
| `interception` | 当前弹种拦截概率 |
| `inZoneBonus` | 目标进入保护区时为 1，否则为 0 |
| `threatScore` | 目标威胁分 |
| `ammoCostNorm` | 弹药成本归一化值 |
| `dispatchCostNorm` | 调度成本归一化值 |
| `invalidActionPenalty` | 非法动作惩罚 |
| `highThreatUnassignedPenalty` | 高威胁目标未分配惩罚 |

后续如果完善 `CombatProcessor`，可以将奖励改为更接近真实战果：

```text
成功拦截高价值目标       +10
成功保护高价值保护区     +5
保护区健康度下降         -5
目标进入保护区但未拦截   -10
弹药浪费                 -2
超出射程或射高动作       -5
```

## 5. 训练数据设计

DQN 经验样本格式：

```text
(s, a, r, s', done)
```

对应到项目中：

| 字段 | 项目含义 |
| --- | --- |
| `s` | 当前仿真帧的态势特征 |
| `a` | 当前选择的 `weaponId + fireType + enemyId` |
| `r` | 动作执行后的奖励 |
| `s'` | 下一帧仿真态势 |
| `done` | 本轮仿真是否结束 |

训练数据来源建议分两阶段：

### 5.1 启发式预训练

当前动态编组已经有启发式评分逻辑，可以作为 DQN 的“专家老师”。

做法：

```text
1. 运行现有启发式动态编组
2. 记录每个候选动作的特征和启发式分数
3. 将最高分候选作为专家动作
4. 先训练 DQN 逼近启发式策略
```

这样可以减少 DQN 初期随机探索导致的无效动作。

### 5.2 仿真在线训练

接入 `SimulationEngineService` 后，每个 tick 都可以产生经验：

```text
MovementProcessor
-> DynamicFormationProcessor
-> DQN 选择动作
-> RewardCalculator 计算奖励
-> ReplayBuffer 存储经验
-> DQNTrainingService 定期训练
```

## 6. 推荐后端模块划分

当前已经新增以下模块：

```text
algorithm/dqn/DqnFeatureBuilder.java
algorithm/dqn/DqnActionGenerator.java
algorithm/dqn/DqnRewardCalculator.java
algorithm/dqn/DqnPolicyService.java
algorithm/dqn/DqnReplayBuffer.java
algorithm/dqn/DqnNeuralQModel.java
algorithm/dqn/DqnTrainingService.java
algorithm/dqn/model/DqnAction.java
algorithm/dqn/model/DqnExperience.java
algorithm/dqn/model/DqnFeatureVector.java
algorithm/dqn/model/DqnScoredAction.java
```

职责说明：

| 模块 | 职责 |
| --- | --- |
| `DqnFeatureBuilder` | 将敌方、武器、弹种、保护区转换为模型输入特征 |
| `DqnActionGenerator` | 复用现有匹配逻辑生成合法候选动作 |
| `DqnRewardCalculator` | 计算即时奖励 |
| `DqnPolicyService` | 输出候选动作 Q 值，当前融合启发式冷启动和神经网络 Q 模型 |
| `DqnReplayBuffer` | 保存经验回放数据 |
| `DqnNeuralQModel` | 当前神经网络 Q 估计器，使用隐藏层和反向传播更新 |
| `DqnTrainingService` | 负责写入经验、采样 batch、执行 TD 更新 |
| `DqnAction` | 描述 `weaponId + fireType + enemyId` |
| `DqnExperience` | 描述 `(s, a, r, s', done)` |
| `DqnFeatureVector` | 封装模型输入向量 |
| `DqnScoredAction` | 封装候选动作、特征、启发式分数和 Q 值 |

## 7. 接入点

### 7.1 动态方案生成接口

在 `DynamicFormationPlanningService` 中，当前启发式排序逻辑位于候选生成和方案组装之间。

现有思路：

```text
候选动作
-> 计算启发式 score
-> 按 score 排序
-> 贪心分配
```

DQN 接入后：

```text
候选动作
-> DqnFeatureBuilder 构造输入
-> DqnPolicyService 计算 Q 值
-> 按 Q 值排序
-> 贪心分配
```

### 7.2 仿真实时动态编组

在 `DynamicFormationProcessor` 中，当前 `generateAssignmentsEnhanced` 方法负责候选动作评分和选择。

改造后：

```text
matchWeaponFireEnemy()
-> DQN 评分
-> generateAssignmentsByQValue()
-> applyAssignments()
```

## 8. 约束处理策略

DQN 只负责学习“哪个候选动作更值得选”，硬约束仍由规则层处理。

这样做有三个好处：

- 避免模型输出非法动作
- 保持当前业务规则可解释
- 便于调试和回退到启发式策略

硬约束包括：

```text
射程约束
射高约束
弹药约束
部署域约束
编组范式约束
一武器一次分配约束
一目标一次主分配约束
maxGroupSize 编组规模约束
```

## 9. 推理流程

每个动态编组 tick 的推理流程：

```text
1. 加载 SimulationContext
2. 过滤可用武器和有效目标
3. 根据射程、射高、弹药生成合法候选动作
4. 为每个候选动作构造特征向量
5. DQN 输出每个候选动作的 Q 值
6. 按 Q 值从高到低排序
7. 按业务约束贪心选取最终分配
8. 更新武器状态并扣减弹药
9. 将结果写回 Redis 并推送前端
```

## 10. 训练流程

DQN 标准训练流程如下。当前版本已经实现 ReplayBuffer、reward 计算、batch 采样、神经网络 Q 估计和 TD 更新：

```text
初始化 Q 网络和 Target Q 网络
初始化 ReplayBuffer

for epoch in epochs:
    初始化仿真场景
    for step in stepsPerEpoch:
        生成候选动作
        使用 epsilon-greedy 选择动作
        执行动作并推进仿真
        计算 reward
        存储 (s, a, r, s', done)
        从 ReplayBuffer 采样 batch
        计算 target = r + gamma * max Q_target(s', a')
        更新 Q 网络
定期同步 Target Q 网络
```

当前已实现的在线更新流程：

```text
1. 生成候选动作 weapon-fire-enemy
2. 构造 DqnFeatureVector
3. DqnPolicyService 预测当前 Q 值
4. DqnRewardCalculator 计算即时 reward
5. DqnTrainingService 写入 DqnReplayBuffer
6. 从 ReplayBuffer 采样 batch
7. 计算 target
   - done=true 或 nextState=null 时：target = reward
   - 否则：target = reward + gamma * Q_target(nextState)
8. DqnNeuralQModel 按 TD error 反向传播更新网络权重
9. 每 targetUpdateFreq 步同步一次 Target Network
```

当前神经网络结构：

```text
input = DqnFeatureVector 固定特征顺序
hidden = ReLU(W1 * input + b1)
Q = sigmoid(W2 * hidden + b2)
loss = 0.5 * (target - Q)^2
```

当前 TD target：

```text
done=true 或 nextState=null：
target = reward

否则：
target = reward + gamma * Q_target(nextState)
```

训练时根据 `target - Q` 对输出层和隐藏层执行反向传播。

当前 Target Network 机制：

```text
online network: DqnNeuralQModel 当前训练网络
target network: online network 的参数快照

训练时：
Q(s, a) 使用 online network
Q_target(s') 使用 target network

同步时：
target network = online network
同步频率由 targetUpdateFreq 控制
```

当前模型持久化机制：

```text
保存文件：bianzu-backend/models/dqn-model.json

后端启动：
DqnModelPersistenceService 自动尝试加载模型文件

训练过程中：
每次 Target Network 同步时保存当前模型快照

保存内容：
online network 参数
target network 参数
featureNames
```

当前 episode 经验链：

```text
仿真 tick t：
选择当前排名最高候选动作 action_t

仿真 tick t+1：
选择新的排名最高候选动作 action_t+1
写入经验：
(state_t, action_t, reward_t, state_t+1, done=false)

其中：
state_t      = action_t 的 DqnFeatureVector
action_t     = action_t 的 DqnAction
reward_t     = 根据 action_t 特征计算的即时奖励
state_t+1    = action_t+1 的 DqnFeatureVector
```

当前 epsilon-greedy 探索机制：

```text
1. 对所有候选动作计算 Q 值
2. 生成随机数 p
3. 如果 p < epsilon：
   随机选择一个候选动作，并将其提升到本轮排序首位
4. 如果 p >= epsilon：
   按 Q 值从高到低排序
5. 后续仍由规则层执行一武器一次、一目标一次、弹药、射程、射高等约束
```

项目中已有配置类 `DynamicAlgorithmConfigDTO`，可以继续使用：

```text
learningRate
gamma
epsilon
targetUpdateFreq
replayBufferSize
epochs
stepsPerEpoch
batchSize
maxGroupSize
planCount
```

## 11. 第一版落地建议

第一版优先实现“DQN 候选动作评分器”：

```text
模型类型：DQN 候选动作评分模型
输入：global + enemy + weapon + fireType + pairFeature
输出：该候选动作的 Q 值
动作：选择一个 weapon-fire-enemy
约束：仍由 Java 规则层保证
奖励：目标威胁收益 + 保护区收益 + 拦截收益 - 成本
训练：先用启发式结果预训练，再接入仿真回放训练
推理：每个 tick 生成候选，DQN 排序，组装方案
```

推荐迭代顺序：

```text
[已完成] 1. 抽出候选动作对象 DqnAction
[已完成] 2. 抽出特征构造器 DqnFeatureBuilder
[已完成] 3. 将现有启发式分数改造成可记录的冷启动分数
[已完成] 4. 实现 DqnPolicyService，输出候选动作 Q 值
[已完成] 5. 增加 ReplayBuffer 和 RewardCalculator
[已完成] 6. 增加 DqnNeuralQModel，支持神经网络 Q 估计和在线反向传播
[已完成] 7. 接入动态方案生成和仿真 tick 流程
[已完成] 8. 增加 Target Network 和 epsilon-greedy 探索
[已完成] 9. 增加模型持久化
[已完成] 10. 接入跨 tick 的 s -> s' episode 经验链
[待完成] 11. 增加离线训练和评估报表
[待完成] 12. 对比启发式策略、神经网络 Q 策略和完整 DQN 策略的拦截率、成本、漏防率
```

## 12. 评估指标

建议从以下指标评估 DQN 是否优于当前启发式算法：

| 指标 | 含义 |
| --- | --- |
| `interceptionSuccessRate` | 拦截成功率 |
| `highValueTargetCoverage` | 高价值目标覆盖率 |
| `zoneProtectionRate` | 保护区防护率 |
| `averageDispatchCost` | 平均调度成本 |
| `ammoEfficiency` | 单位弹药收益 |
| `unassignedThreatScore` | 未分配目标总威胁 |
| `decisionLatencyMs` | 单帧决策耗时 |

上线前至少需要对比：

```text
当前启发式策略
启发式预训练 DQN
仿真训练后 DQN
```

## 13. 总结

本项目的已有数据已经可以支撑第一版 DQN 动态编组模型。

最合适的工程路径是：

```text
DQN 不直接替代全部编组逻辑
DQN 先替代候选动作评分逻辑
规则层继续负责合法性和业务约束
仿真流水线负责产生经验数据
启发式策略负责冷启动预训练
```

这种方案可以最大化复用当前代码，同时为后续真实强化学习训练留出清晰接口。

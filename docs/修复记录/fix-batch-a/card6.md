先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 6：怪物类型、宝箱掉落、世界Boss位置

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/props/MonsterType.java、data/excels/monster/、game/entity/gadget/GadgetChest.java、game/world/WorldBossSpawnHelper.java。

## 要修的条目

### R2-056 [能力]MONSTER_PARTNER 442 行不在服务端 MonsterType 枚举
- 影响：卡住流程；工作量：中；第三轮核实：仍存在
- 证据：MonsterExcelConfigData.json:178545 有该值；MonsterType.java:7-15 仅列 NONE/ORDINARY/BOSS/ENV_ANIMAL/LITTLE_MONSTER/FISH，MonsterData.java:34 用该枚举反序列化；EntityMonster.java:422 死亡时无空值保护地调用 getType().getValue()。
- 说明：推断：这类怪物进入服务端死亡链会在按怪物类型触发副本条件时抛异常，中断后续收尾；须以具体刷怪实例复核，不能断言 442 行全部实际刷出。

### R2-069 [能力]场景 chest_drop_id 有 18 处指向缺失原生根
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：44,508 个 Lua 文件逐行扫描，gadget/chest_drop_id 共 1,339 次，缺失 18 次/16 个 ID；例 resources/Scripts/Scene/3/scene3_group133307177.lua:28 的 1050269。GadgetChest.java:312-329 优先尝试原生根，之后可能走 WorldChestLootHelper。
- 说明：并非每处必然空奖励；需按箱型、场景与回退路径逐个验证，不能把 18 处全部判为玩家损失。

### R2-131 世界 Boss 103 缺出生位置
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：latest.log:269 等 15 次，均为 boss 103/group 133706025；WorldBossSpawnHelper.java:1843-1870 找不到位置即 continue，不注册该 Boss
- 说明：只有 1 个唯一 Boss 缺位置；15 次主要是重启重复。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card6.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
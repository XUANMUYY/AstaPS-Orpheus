先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-c/共用说明.md，遵守其中全部规则。

# 任务卡 2：Scene Lua 新式 StartChallenge / StopChallenge 写法

要点：支持条件数组参数的 StartChallenge、按挑战索引的 StopChallenge，接到通用挑战状态机；补 GetEffigyChallengeLimitTime。

## 要修的条目

### R2-035 [副本]Scene Lua 的新式启动与停止写法未接到通用挑战状态机
- 影响：卡住流程；工作量：大；核实：仍存在
- 证据：Scene Lua StartChallenge 21 处，如 resources/Scripts/Scene/3/scene3_group133001379.lua:197（条件数组参数）；StopChallenge 338 处，如 resources/Scripts/Scene/1/scene1_group111101015.lua:407。ScriptLib.java:224-297 只有六整数的 ActiveChallenge；ScriptLib.java:1667…
- 说明：本轮统计仅 Scripts/Scene，含注释行；函数缺失/半实现的全资源复核归卡 3。Scene 内未检出 ScriptLib.CreateChallenge/PauseChallenge/ResumeChallenge 调用，故不凭名字报缺口。

### R1-180 缺失 StartChallenge
- 影响：卡住流程；工作量：中；核实：仍存在
- 证据：resources/Scripts/Scene/3/scene3_group133001379.lua:79-80,182-208；RegionMembershipTick.java:59-69；ScriptLoader.java:118-120
- 说明：玩家进入组 133001379 的区域 379077，回调调用不存在的数组参数 StartChallenge 抛错；机关 379015 状态与 suite 2/3 都未启动。

### R1-181 缺失 GetEffigyChallengeLimitTime
- 影响：卡住流程；工作量：中；核实：仍存在
- 证据：resources/Scripts/Scene/40028/scene40028_group240028002.lua:35,164-173；EntityMonster.java:279-281；SceneScriptManager.java:1239-1243
- 说明：组 240028002 怪物出生事件先读取不存在的 GetEffigyChallengeLimitTime；Lua 报错，后续 ActiveChallenge 不执行，限时挑战不开。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果写到 out/card2.md（相对任务卡目录）：每个条目一行——状态（已修 / 已不存在 / 跳过及原因）、提交号、改动文件、新增测试、进游戏怎么验证（写具体场景组和操作）。
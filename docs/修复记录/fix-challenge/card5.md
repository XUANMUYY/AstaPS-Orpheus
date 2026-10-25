先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-c/共用说明.md，遵守其中全部规则。

# 任务卡 5：地脉花：其余脚本函数和进度事件

要点：R2-022 第三轮核实为「判断有误」（客户端通用事件可转交），先核实：确实不缺就不改，只在结果里说明。R1-139/R1-140/R1-162 是待实机或未核实项，与本卡同一处代码可以顺手修，结果里单独标出。

## 要修的条目

### R2-022 地脉进度 EVENT_BLOSSOM_PROGRESS_FINISH 无显式生产者
- 影响：卡住流程；工作量：大；核实：判断有误
- 证据：Lua 223 次/222 group；例 resources/Scripts/Common/BlossomGroupV2.lua:16；Java 除 EventType.java:45 无该常量引用。
- 说明：仅可能由通用客户端事件透传；地脉流程服务端主动触发缺口为静态推断。

### R1-147 未实现 RefreshBlossomGroup
- 影响：卡住流程；工作量：大；核实：仍存在
- 证据：resources/Scripts/Scene/1/scene1_group111101077.lua:141-148；ScriptLib.java:1143-1146；Scene.java:1435
- 说明：组 111101077 加载时要求刷新到 suite 2；RefreshBlossomGroup 仅回 0，没有刷新，工作台/循环玩法所需 suite 不出现，却被 Lua 当作成功。

### R1-151 未实现 AddBlossomScheduleProgressByGroupId
- 影响：卡住流程；工作量：中；核实：仍存在
- 证据：resources/Scripts/Scene/1/scene1_group111101077.lua:115-120；ScriptLib.java:300-303；EntityGadget.java:428
- 说明：组 111101077 机关死亡事件调用 AddBlossomScheduleProgressByGroupId；方法只警告并回 0，循环玩法进度未加却报告成功。

### R1-163 缺失 GetBlossomRefreshTypeByGroupId
- 影响：卡住流程；工作量：中；核实：仍存在
- 证据：resources/Scripts/Scene/3/scene3_group133001373.lua:204-223；SceneScriptManager.java:279,387-388,1239-1243
- 说明：组 133001373 刷新事件读取不存在的 GetBlossomRefreshTypeByGroupId；Lua 异常中断，后续操作台 gadget 创建和地脉花阶段设置均未执行。

### R1-139 未实现 RefreshBlossomDropRewardByGroupId
- 影响：卡住流程；工作量：大；核实：待实机
- 证据：Lua 625（去注释 625）/group 95；resources/Scripts/Scene/1/scene1_group111101088.lua:212；ScriptLib.java:1137；日志 U0/C0
- 说明：推断：地脉奖励刷新缺失，奖励流程不完整。

### R1-140 未实现 SetBlossomScheduleStateByGroupId
- 影响：卡住流程；工作量：大；核实：待实机
- 证据：Lua 605（去注释 605）/group 213；resources/Scripts/Common/BlossomGroupV2.lua:125；ScriptLib.java:1246；日志 U0/C0
- 说明：推断：地脉阶段写入缺失，后续阶段判定/奖励受阻。

### R1-162 未实现 CreateBlossomChestByGroupId
- 影响：卡住流程；工作量：中；核实：待实机
- 证据：resources/Scripts/Scene/1/scene1_group111101088.lua:61,240-255；ScriptLib.java:519-522；EventType.java:45
- 说明：CreateBlossomChestByGroupId 空实现；样例依赖 EVENT_BLOSSOM_PROGRESS_FINISH，而基线未找到该事件的服务端生产链，不能确认回调会执行并影响领奖。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果写到 out/card5.md（相对任务卡目录）：每个条目一行——状态（已修 / 已不存在 / 跳过及原因）、提交号、改动文件、新增测试、进游戏怎么验证（写具体场景组和操作）。
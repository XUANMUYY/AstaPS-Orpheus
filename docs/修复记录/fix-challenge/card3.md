先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-c/共用说明.md，遵守其中全部规则。

# 任务卡 3：父子挑战链

要点：CreateFatherChallenge / AttachChildChallenge / StartFatherChallenge / ModifyFatherChallengeProperty（若需要）以及 CHALLENGE_FATHER_SUCC_IN_TIME；R2-033 需要让挑战记住来源区域以填 EVENT_CHALLENGE_SUCCESS 的 source_eid。

## 要修的条目

### R2-034 [副本]父子挑战链未形成可执行工厂
- 影响：卡住流程；工作量：大；核实：仍存在
- 证据：DungeonChallengeConfigData.json:196 的 CHALLENGE_FATHER_SUCC_IN_TIME 116 行且无工厂；Scene Lua AttachChildChallenge 425 处、StartFatherChallenge 171 处，如 resources/Scripts/Scene/1/scene1_group111101058.lua:133-134；ScriptLib.java:376-377,1630-1631 均为…
- 说明：与第二轮卡 3 的脚本函数复核交叉；这里记录父挑战配置、子挑战装配到结算的断链。

### R1-143 未实现 AttachChildChallenge
- 影响：卡住流程；工作量：大；核实：仍存在
- 证据：resources/Scripts/Scene/1/scene1_group111101058.lua:38,115-135；ScriptLib.java:376-379；HandlerSelectWorktopOptionReq.java:48
- 说明：玩家在组 111101058 选装置 58001 的选项 7 后，Lua 装配子挑战；AttachChildChallenge 只警告并回 0，没有登记子挑战，随后 StartFatherChallenge 也空实现，挑战不会启动。

### R1-153 未实现 StartFatherChallenge
- 影响：卡住流程；工作量：大；核实：仍存在
- 证据：resources/Scripts/Scene/1/scene1_group111101058.lua:115-135；ScriptLib.java:1630-1633；HandlerSelectWorktopOptionReq.java:48
- 说明：组 111101058 玩家选装置选项 7 后执行 StartFatherChallenge；该方法不创建或启动挑战却回 0，脚本随即删除选项，玩家不能按此入口开始父挑战。

### R1-152 未实现 CreateFatherChallenge
- 影响：卡住流程；工作量：大；核实：待实机
- 证据：Lua 188（去注释 188）/group 120；resources/Scripts/Common/HideAndSeek.lua:154；ScriptLib.java:543；日志 U0/C0
- 说明：推断：脚本依赖该状态/动作，缺失可能中断场景推进。

### R2-033 EVENT_CHALLENGE_SUCCESS 的 source_eid 缺失
- 影响：功能缺失；工作量：中；核实：仍存在
- 证据：1,767 group 注册；6 处条件读取 evt.source_eid，如 Scene/9/scene9_group199001094.lua:800；WorldChallenge.java:147-150 只填 param2 与 source，source_eid 恒为 0。
- 说明：条件用它查区域实体时不能命中。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果写到 out/card3.md（相对任务卡目录）：每个条目一行——状态（已修 / 已不存在 / 跳过及原因）、提交号、改动文件、新增测试、进游戏怎么验证（写具体场景组和操作）。
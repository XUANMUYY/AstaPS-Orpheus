先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 2：ScriptLib 返回值与缺失函数

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：scripts/ScriptLib.java、scripts/SceneScriptManager.java、地脉花（blossom）相关管理器。

## 要修的条目

### R2-019 CreateGadget 无效 config 返回成功
- 影响：卡住流程；工作量：小；第三轮核实：仍存在
- 证据：Lua 调用 4,960 次，0 比较 4,097 次；ScriptLib.java:549-558 对 config_id==0 只告警并返回 0，未创建实体。
- 说明：当前静态引用不证明运行时会传 0；这是明确的失败码漏洞。

### R2-020 ActiveChallenge 被拒绝却返回成功
- 影响：卡住流程；工作量：中；第三轮核实：仍存在
- 证据：Lua 调用 1,597 次，0 比较 1,552 次；ScriptLib.java:229-246 在非飞行挑战已有运行中挑战或塔楼等待换队时直接返回 0，未启动请求的挑战。
- 说明：可能是幂等/特例保护；脚本按 0 认定启动成功，需明确契约。

### R1-136 半实现 SetPlatformPointArray
- 影响：卡住流程；工作量：中；第三轮核实：待实机
- 证据：Lua 1153（去注释 1124）/group 352；resources/Scripts/Scene/1/scene1_group111101017.lua:23；ScriptLib.java:1412；日志 U0/C0
- 说明：推断：成功路径仍返回 -1（Java 1444-1449），Lua 有 0 ~= 失败分支（Scene/1/scene1_group111101090.lua:123），会提前退出机关动作。

### R1-142 未实现 RefreshHuntingClueGroup
- 影响：卡住流程；工作量：中；第三轮核实：仍存在
- 证据：Lua 516（去注释 516）/group 516；resources/Scripts/Scene/3/scene3_group133001434.lua:86；ScriptLib.java:1178；日志 U0/C0
- 说明：推断：组加载即调用以填充线索实体（Scene/3/scene3_group133001434.lua:84-89）。

### R1-146 缺失 GetBlossomScheduleStateByGroupId
- 影响：卡住流程；工作量：大；第三轮核实：仍存在
- 证据：Lua 352（去注释 352）/group 88；resources/Scripts/Scene/3/scene3_group133001373.lua:138；ScriptLib.java 无同名 public 方法；日志 U0/C0
- 说明：推断：脚本用返回值决定操作台选项（Scene/3/scene3_group133001373.lua:138-144）。

### R1-154 缺失 AddExtraFlowSuite
- 影响：卡住流程；工作量：大；第三轮核实：仍存在
- 证据：Lua 157（去注释 157）/group 89；resources/Scripts/Scene/1/scene1_group111101014.lua:185；ScriptLib.java 无同名 public 方法；日志 U0/C0
- 说明：推断：脚本依赖该状态/动作，缺失可能中断场景推进。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card2.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
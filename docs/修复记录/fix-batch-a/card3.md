先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 3：场景脚本事件参数

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：scripts/SceneScriptManager.java、scripts/data/ScriptArgs.java、挑战类 WorldChallenge.java、相关 callEvent 调用点。

## 要修的条目

### R2-021 EVENT_TIME_AXIS_PASS 的 source_name 与 param1
- 影响：卡住流程；工作量：中；第三轮核实：仍存在
- 证据：Lua 注册 1,603 次/628 group；372 group 的条件读取 evt.source_name 共 781 次，evt.param1 共 912 次；如 Scene/1/scene1_group111101370.lua:265。SceneTimeAxis.java:41-43 仅填 source，ScriptArgs.java:7-15 无 source_name，param1 默认为 0。
- 说明：定时轴动作条件无法按脚本预期通过；是否所有 912 次均要求非 0 尚待逐条判定。

### R2-026 EVENT_TIMER_EVENT 条件读取不存在的 source_name
- 影响：卡住流程；工作量：小；第三轮核实：仍存在
- 证据：Lua 919 次/394 group；49 group 的条件读取 evt.source_name 共 50 次，如 Scene/1/scene1_group111101041.lua:118；SceneScriptManager.java:1606 仅 setEventSource，ScriptArgs.java:7-15 无此字段。
- 说明：触发器自己的 source 筛选可通过，但条件中再比较 evt.source_name 会失败。

### R2-027 EVENT_VARIABLE_CHANGE 条件读取不存在的 source_name
- 影响：卡住流程；工作量：小；第三轮核实：仍存在
- 证据：Lua 4,140 次/2,236 group；13 group 条件读取该字段 23 次，如 Scene/1/scene1_group111101036.lua:96；五处 setter/change 路径只 setEventSource(var)（ScriptLib.java:455-473,1372-1388）。
- 说明：变量名在 source 字段，Lua 期待 source_name。

### R2-031 EVENT_GROUP_LOAD/EVENT_GADGET_CREATE 的 param2 缺失
- 影响：功能缺失；工作量：小；第三轮核实：仍存在
- 证据：各有 6 处条件读取 evt.param2；如 resources/Scripts/Scene/3/scene3_group133303018.lua:101、resources/Scripts/Scene/3/scene3_group133102294.lua:108。生产者分别为 Scene.java:1434（只传 group ID 到 param1）和 EntityGadget.java:362-364（只传 config ID 到 param1），param2 恒…
- 说明：少数脚本要求非零状态/ID；可能是脚本事件类型写错，也可能是服务端填参不足，需逐组核对。

### R2-032 EVENT_CHALLENGE_FAIL 参数缺失
- 影响：功能缺失；工作量：小；第三轮核实：仍存在
- 证据：1,803 group 注册；6 处条件读取 evt.param1，如 Scene/43001/scene43001_group243001005.lua:477；WorldChallenge.java:180-183 创建 ScriptArgs 未传 param1，恒为 0。
- 说明：部分脚本按挑战 ID 判定，实际是否触发到这些脚本待验证。

### R2-033 EVENT_CHALLENGE_SUCCESS 的 source_eid 缺失
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：1,767 group 注册；6 处条件读取 evt.source_eid，如 Scene/9/scene9_group199001094.lua:800；WorldChallenge.java:147-150 只填 param2 与 source，source_eid 恒为 0。
- 说明：条件用它查区域实体时不能命中。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card3.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
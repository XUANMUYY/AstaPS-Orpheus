先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 7：活动校验与跑酷脚本报错

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/activity/、server/packet/recv/HandlerActivity*、server/packet/send/PacketActivityScheduleInfoNotify.java、scripts/ScriptLib.java。

## 要修的条目

### R2-082 [活动]观察器领奖缺完成与开放检查
- 影响：功能缺失；工作量：小；第三轮核实：仍存在
- 证据：HandlerActivityTakeWatcherRewardReq.java:17-25 直接调用 takeWatcherReward 并回包；PlayerActivityData.java:68-92 只检查 ID 和是否已领，没有调用 isFinished()，也没有活动开放检查；PacketActivityTakeWatcherRewardRsp.java:11-15 不设失败码
- 说明：静态可判定未完成 watcher 可请求领奖；未做实际发包验证。

### R2-083 [活动]试用角色领奖可重复且界面 detail 未填
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：TrialAvatarActivityHandler.java:34-37 注释掉 setTrialAvatarInfo；:114-140 发奖前未检查 passedDungeon/receivedReward；两个标志定义见 TrialAvatarPlayerData.java:63-84
- 说明：当前 9 项排期不含试用角色；如果启用 5002，这会影响展示及领奖正确性。

### R2-086 [活动]活动开放状态与奖励入口未统一校验
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：PacketActivityScheduleInfoNotify.java:18-25 对所有加载项恒置 isOpen=true；ActivityManager.java:149-176 虽有时间检查函数，HandlerGetActivityInfoReq.java:12-17 和 HandlerActivityTakeWatcherRewardReq.java:13-25 未调用
- 说明：默认 9 项在 2026-10-25 均处于配置时间窗；问题在未来排期和覆盖配置时更明显。

### R2-124 海灯节跑酷 Lua 触发器抛错
- 影响：卡住流程；工作量：中；第三轮核实：仍存在
- 证据：log.2026-10-24_21.log.gz:10244 等 4 次 action_gadget_create；堆栈指向 Common/V3_4/Activity_SeaLampParkour.lua 的 nil 索引；SceneScriptManager.java:1320-1328 返回 -1
- 说明：推断对应活动机关/挑战事件中断；日志来自旧构建，当前 main 未实测。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card7.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
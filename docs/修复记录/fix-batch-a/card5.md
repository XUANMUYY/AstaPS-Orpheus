先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 5：副本复活和入口条件

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/dungeons/、data/excels/dungeon/DungeonData.java、DungeonEntryData.java、server/packet/recv/HandlerDungeon*、HandlerGetDungeonEntryExploreConditionReq.java。

## 要修的条目

### R2-041 [副本]死亡复活没有按副本次数/选项执行
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：DungeonExcelConfigData.json 有 reviveMaxCount 的 1,524 行；DungeonData.java:31 读字段但 Java 全局无 getReviveMaxCount 调用。HandlerDungeonDieOptionReq.java:12-19 只处理立即退出，其他选项忽略；HandlerDungeonPlayerDieReq.java:15-21 总是调用 Scene.respawnPlayer；Scene.java:97…
- 说明：当前资源 0 行含 reviveType，该字段不能据此宣称 7.1 有具体分类。日志有复活点激活请求 13 次，但无死亡请求可验证。

### R2-045 [副本]入口条件表未进入校验，探索请求直接解锁
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：DungeonEntryExcelConfigData.json:23-75 的 NCPHMNJMPKB 包含等级/任务条件，151 行均有该字段，其中等级 38、任务 37 个入口；DungeonEntryData.java:20-27,45-65 却读 satisfiedCond（未见该混淆键映射）。HandlerGetDungeonEntryExploreConditionReq.java:14-23 注释掉条件应答，直接发送解锁；DomainDungeonHelpe…
- 说明：入口任务条件和副本自身等级门槛是两层数据；后者已校验，不能把整条进入链路说成无验证。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card5.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
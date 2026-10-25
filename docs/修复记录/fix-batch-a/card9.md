先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 9：图鉴、成就、聊天

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/player/PlayerCodex.java、server/packet/send/PacketCodexData*、game/achievement/、ChatSystem.java、PacketQueryCodexMonsterBeKilledNumRsp.java。

## 要修的条目

### R2-112 [登录数据/成就]图鉴书籍与景观条目只有空类型快照
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：资源 BooksCodexExcelConfigData.json 293、ViewCodexExcelConfigData.json 268；PlayerCodex.java:21,24,39,41 只有集合；PacketCodexDataFullNotify.java:27-34,109-118 创建书籍/景观空类型并原样下发；GameData.java:209-237 无相应数据表；全主源码无书籍/景观解锁集合写入
- 说明：561 条资源不等于 561 条用户必然可解锁；需补读取、事件、存档和已读协议。

### R2-113 [登录数据/成就]动物图鉴捕获/钓鱼分支不完整
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：AnimalCodexExcelConfigData.json 585 条：击杀 360、捕获 107、钓鱼 60、无类型 58；CodexAnimalData.java:23-26 仅枚举 KILL/CAPTURE；game/world/Scene.java:709,715 两处均以 KILL 调 PlayerCodex.checkAnimal；PlayerCodex.java:118-131 无其他入口
- 说明：捕获 107 条无调用；钓鱼 60 条类型不识别。击杀路径已有。

### R2-114 [登录数据/成就]成就组奖励只验证 ID/重复领取，未验证组完成
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：AchievementGoalExcelConfigData.json 73 行、72 个有 ID、71 个有奖励 ID；HandlerTakeAchievementGoalRewardReq.java:12-29 验帧后直调；Achievements.java:362-404 仅拒绝空/重复/已领，按存在的奖励 ID 发奖并存档，无同组完成条件检查；旧日志该请求 0 次
- 说明：服务端可在未达成时响应并发奖（代码路径推断）；无原生请求复现。混合合法/未知 ID 批次还会跳过未知项，却把全部请求 ID 放入成功响应。

### R2-127 PullRecentChatReq 在无服务器历史时不回包
- 影响：功能缺失；工作量：小；第三轮核实：仍存在
- 证据：请求 45 次、SEND PullRecentChatRsp 0 次；ChatSystem.java:70-72,267-281：serverHistory.isEmpty() 直接返回；PacketPullRecentChatRsp.java 可编码空列表
- 说明：当前代码条件路径确证；旧日志 0 回包不等于 45 次全因该条件，需 payload/配置核实。

### R2-130 QueryCodexMonsterBeKilledNumRsp 仅填 ID
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：请求/回复各 31 次；PacketQueryCodexMonsterBeKilledNumRsp.java:11-26 只 addCodexIdList，两个数量列表代码已注释；protocol.desc 同名消息另有两条 repeated uint32 字段 10/15
- 说明：两字段名混淆，杀/捕数量的对应顺序未确认；图鉴计数显示异常为推断。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card9.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
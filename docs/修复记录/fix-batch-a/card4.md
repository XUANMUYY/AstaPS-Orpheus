先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 4：每日委托奖励

注意：对话 Talk 由另一个会话负责，本卡不要改 HandlerNpcTalkReq.java 和 TalkManager.java。

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：server/packet/recv/HandlerNpcTalkReq.java、game/talk/TalkManager.java、game/dailytask/DailyTaskManager.java、server/packet/recv/HandlerTakeDailyTaskScoreRewardReq.java。

## 要修的条目

### R2-009 四委托奖励先自动发，领取请求不校验发奖结果
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：DailyTaskManager.java:856-861,1117-1153 第四项完成即尝试发 12 档奖励并标记领取；HandlerTakeDailyTaskScoreRewardReq.java:35-47 只查完成数，忽略 claimScoreReward() 返回值，照样回成功和预览物品；DailyTaskLevelExcelConfigData.json:1 共 12 级档、DailyTaskRewardExcelConfigData.json:1 共 3 组…
- 说明：正常自动发奖时客户端确认可用；推断：发奖失败时按钮仍可能显示成功，且与 NPC 手动领取时点不同。日志未见该请求，未原生验收。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card4.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
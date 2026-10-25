先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-c/共用说明.md，遵守其中全部规则。

# 任务卡 4：多阶段玩法的通关条件

要点：第三轮标「待实机」，主会话同意放进本批：确认失败路径后再修，确认不了就跳过并说明。

## 要修的条目

### R2-037 [副本]多阶段玩法通关条件无处理器
- 影响：卡住流程；工作量：大；核实：待实机
- 证据：resources/ExcelBinOutput/DungeonPassExcelConfigData.json:73 等 49 条 DUNGEON_COND_END_MULTISTAGE_PLAY，关联 49 个副本（48 条 RELEASE）；src/main/java/emu/grasscutter/game/dungeons/enums/DungeonPassConditionType.java:15 仅有枚举，game/dungeons/pass_conditio…
- 说明：涉及机关棋谭 15、棋盘 12、绘想游迹式棋盘 8、怪物对抗攻/守各 7。推断：靠该条件自然结算不可达；活动专用旁路未运行验证。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果写到 out/card4.md（相对任务卡目录）：每个条目一行——状态（已修 / 已不存在 / 跳过及原因）、提交号、改动文件、新增测试、进游戏怎么验证（写具体场景组和操作）。
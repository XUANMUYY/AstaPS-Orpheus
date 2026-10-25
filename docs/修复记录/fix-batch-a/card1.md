先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 1：任务内容条件枚举

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/quest/enums/QuestContent.java、game/quest/content/、game/quest/QuestManager.java；参考已有的 ContentQuestGlobalVar*.java 写法。

## 要修的条目

### R1-061 缺枚举 · QUEST_CONTENT_ITEM_NUM_EQUAL
- 影响：卡住流程；工作量：小；第三轮核实：仍存在
- 证据：16 主任务 / 25 处（finishCond 25）；resources/BinOutput/Quest/40128.json:626；QuestContent.java 无此名
- 说明：新发现（相对笔记）；完成条件被过滤，可能误完成；与其他条件共存时逻辑也改变

### R1-063 缺枚举 · QUEST_CONTENT_PARENT_QUEST_STATE_EQUAL
- 影响：卡住流程；工作量：小；第三轮核实：仍存在
- 证据：11 主任务 / 22 处（failCond 1 / finishCond 21）；resources/BinOutput/Quest/6017.json:1230；QuestContent.java 无此名
- 说明：笔记剩余排序已列；完成条件被过滤，可能误完成；与其他条件共存时逻辑也改变；失败条件被过滤，失败可能不触发

### R1-073 缺枚举 · QUEST_CONTENT_ITEM_NUM_GREATER
- 影响：卡住流程；工作量：小；第三轮核实：仍存在
- 证据：3 主任务 / 6 处（finishCond 6）；resources/BinOutput/Quest/40162.json:1618；QuestContent.java 无此名
- 说明：新发现（相对笔记）；完成条件被过滤，可能误完成；与其他条件共存时逻辑也改变

### R1-089 缺枚举 · QUEST_CONTENT_MISC_RENAME_COMPLETE
- 影响：卡住流程；工作量：中；第三轮核实：仍存在
- 证据：1 主任务 / 1 处（finishCond 1）；resources/BinOutput/Quest/75000.json:678；QuestContent.java 无此名
- 说明：新发现（相对笔记）；完成条件被过滤，可能误完成；与其他条件共存时逻辑也改变

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card1.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
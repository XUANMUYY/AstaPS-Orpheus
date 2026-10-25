先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-c/共用说明.md，遵守其中全部规则。

# 任务卡 1：挑战工厂：已枚举但没有工厂的挑战类型

要点：逐个补齐工厂处理器，按 DungeonChallengeConfigData 的参数语义实现计数、计时和成功失败判定；拿不准语义的类型保留并在结果里列出。

## 要修的条目

### R2-036 [副本]已枚举挑战类型大量没有工厂处理
- 影响：卡住流程；工作量：大；核实：仍存在
- 证据：DungeonChallengeConfigData.json:86 的 CHALLENGE_TRIGGER_COUNT 196 行，Scene Lua ActiveChallenge 数字 ID 引用 26 次；CHALLENGE_TRIGGER_IN_TIME_FLY 4 行/27 次，CHALLENGE_KILL_COUNT_FROZEN_LESS 4 行/4 次，CHALLENGE_GUARD_HP 15 行/1 次。ChallengeFactory.java:17…
- 说明：总共 24 种非空挑战类型，只有 8 种工厂接住；已枚举但无工厂 13 种。数字 ID Lua 引用数只计同一行可解析的第二实参。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果写到 out/card1.md（相对任务卡目录）：每个条目一行——状态（已修 / 已不存在 / 跳过及原因）、提交号、改动文件、新增测试、进游戏怎么验证（写具体场景组和操作）。
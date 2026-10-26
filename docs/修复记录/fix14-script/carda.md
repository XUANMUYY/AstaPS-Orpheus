先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/共用说明.md，遵守其中全部规则。

# 任务卡 A：挑战与平台事件

- 工作目录（本卡专用 worktree）：`F:/Fork Reversed-worktrees/Genshin/fix14-script-a`，分支 `fix14-script-a`。只在这里改和提交。
- 结果文件：`C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/out/carda.md`

## 要修的条目

### R5-076 [新接通周边]父挑战 fail_on_wipe 参数被丢弃，全队倒下没有对应失败事件
- 影响：卡住流程；工作量：小；第四轮编号 R4-247
- 证据：R/Scripts/Scene/1/scene1_group111101094.lua:175-182,214-219；J/scripts/ScriptLib.java:617-629；J/game/player/TeamManager.java:1003-1029
- 失败路径和建议：组 111101094 进入区域后创建 fail_on_wipe=true 父挑战；ScriptLib 只取 success/fail，TeamManager 全队倒下只发死亡通知/事件，父挑战不立即 fail，Lua 的 CHALLENGE_FAIL 复位动作不运行。建议：让 CreateFatherChallenge 保存 fail_on_wipe，并在全队死亡事件对匹配的父挑战调用 fail()。

### R5-144 [新接通周边]同场景已有挑战时，另一组的挑战启动失败会截断后续 Lua 机关动作
- 影响：卡住流程；工作量：中；第四轮编号 R4-246
- 证据：ScriptLib.java:244-250；scene3_group133001379.lua:75,195-214；邻近可用第一挑战 scene3_group133003521.lua:82,274-276。
- 失败路径和建议：同场景 3 的另一挑战运行时进入组 133001379 区域 379077 → StartChallenge 因场景已有挑战返回 1 → Lua 立即 return -1，379015 的 GearStart/StartPlatform 均未执行，平台不动。并发发生频率仍未知。

### R5-062 平台到达 EVENT_PLATFORM_ARRIVAL 无显式生产者
- 影响：卡住流程；工作量：中；第四轮编号 R4-201
- 证据：R/Scripts/Scene/20151/scene20151_group220151005.lua:54-76,179-205,315-374；J/game/entity/EntityGadget.java:553-559,591-597,648-654,678-684；J/scripts/constants/EventType.java:123
- 失败路径和建议：scene20151 电梯抵达后 Java 只发 EVENT_PLATFORM_REACH_POINT，Lua 六处 ARRIVAL 触发器均不运行，传送/关墙/激活脚本动作缺失。建议：在 EntityGadget 到达目标点的两处平台路径补 ARRIVAL，保持 Lua 的 param1/2/3 语义。

### R5-041 缺失 AutoPoolMonsterTide
- 影响：卡住流程；工作量：小；第四轮编号 R4-058
- 证据：R/Scripts/Scene/3/scene3_group133007088.lua:12,69-86,246-266；R/Scripts/Scene/3/scene3_monster.pool.lua:2348-2357,2375-2383；J/scripts/service/ScriptPoolMonsterTideService.java:80-105
- 失败路径和建议：组 133007088 第一波传池 18004、tag=2；池怪无 tag，prepare 读成 0 并精确过滤成空，AutoPoolMonsterTide 回 -1，怪潮与画廊进度停住；后续 4/8 同理。建议：在 ScriptPoolMonsterTideService.prepare 将未标记怪物视作可匹配点位 tag，保留显式 tag 规则。

## 本卡要点
- R5-076：`CreateFatherChallenge` 保存 fail_on_wipe（看 Lua 参数表的写法），全队倒下时让匹配的父挑战失败，走已有的挑战失败流程（会发 EVENT_CHALLENGE_FAIL）。
- R5-144：先读官方行为证据——同场景已有挑战时官方是并存还是拒绝？看 Lua 里调用方怎么用返回值、同场景有没有两个挑战同时存在的设计。目标是不让 133001379 的机关动作被截断：若官方允许不同组的挑战并存，就让挑战按组并存；若证据不足，采用最小改动（例如同场景已有挑战属于别的组时允许启动），并在注释写「推断」。不要破坏已有的同组重复启动保护。
- R5-062：平台到达目标点时，在发 EVENT_PLATFORM_REACH_POINT 的同一处补发 EVENT_PLATFORM_ARRIVAL，参数语义照 scene20151_group220151005.lua 的触发器条件（param1/2/3）。注意不要让同一次到达在路线终点重复触发。
- R5-041：`ScriptPoolMonsterTideService.prepare` 让没有 tag 的池怪能匹配点位 tag，显式 tag 的规则保留。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果按共用说明的格式写到结果文件。

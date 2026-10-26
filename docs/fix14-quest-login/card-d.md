# 任务卡 fix14-ql-d：执行项与开放状态：天气区、气候区、供奉等级开放状态、对话内嵌坐标传送

模型 gpt-6-sol，强度 high。

## 要修的条目（第五轮确认待修）

### R5-059 缺处理器 · QUEST_EXEC_MODIFY_WEATHER_AREA（第四轮 R4-192）
- 证据：R/BinOutput/Quest/7012.json:928；J/game/quest/enums/QuestExec.java:77；J/game/quest/QuestSystem.java:116-130；J/scripts/ScriptLib.java:1684-1700
- 说明：主任务 7012 对话完成的 finishExec 进入 QuestSystem 后找不到处理器，天气区域不改；Lua 的 SetWeatherAreaState 已实现且能发玩家天气变化，但不是此任务 exec 的旁路。建议：给该 QuestExec 增处理器，复用已核对的天气区域设置逻辑。

### R5-060 缺处理器 · QUEST_EXEC_MODIFY_CLIMATE_AREA（第四轮 R4-194）
- 证据：R/BinOutput/Quest/12042.json:488；J/game/quest/enums/QuestExec.java:69；J/game/quest/QuestSystem.java:116-130
- 说明：1204209 对话完成的 finishExec 找不到 QuestSystem 处理器，气候区域状态与客户端天气不会按该 exec 变化。建议：在 QuestSystem 增该 exec 处理器，并接场景气候状态及玩家同步。

### R5-061 OpenState：不自动开放（第四轮 R4-196）
- 证据：R/ExcelBinOutput/OpenStateConfigData.json:215-224；J/game/player/PlayerProgressManager.java:219-249；J/game/entity/gadget/OfferingHelper.java:408-469
- 说明：玩家通过 OfferingHelper 供奉到 2 并完成 31101 后，OpenState 1011 的 OPEN_STATE_OFFERING_LEVEL 分支仍固定 false，登录/任务完成重检也不开状态。供奉领奖系统已经存在，缺 OpenState 条件读取与回写；原“供奉系统未做”归因不对。建议：在 PlayerProgressManager 的该分支读取供奉等级，并在 OfferingHelper 升级后重检。

### R5-075 [新接通周边]对话内嵌坐标传送被当作 dummy point 名称，仍回成功（第四轮 R4-245）
- 证据：R/ExcelBinOutput/TalkExcelConfigData.json:2921-2929,2969-2983；J/game/talk/exec/ExecTransSceneDummyPoint.java:21-34；J/game/talk/TalkManager.java:57-73
- 说明：合法对话 1000312 的执行参数是 [3_13648:…] 坐标串；ExecTransSceneDummyPoint 只查该串加 .pos 的 dummy key，查不到就静默返回；TalkManager 仍发成功并投递完成事件，玩家未被传到剧情目标。建议：在 ExecTransSceneDummyPoint.execute 解析带方括号坐标，传送失败向 TalkManager 返回失败而不推进。

## 提示

R5-059/060：在 `game/quest/exec/` 加处理器，复用 ScriptLib.SetWeatherAreaState 已有的天气区域逻辑（抽成可共享方法时不要改变 ScriptLib 原行为）；气候区按资源字段和现有场景天气/气候状态同步。R5-061：`PlayerProgressManager` 里 OPEN_STATE_OFFERING_LEVEL 分支读取供奉等级（OfferingHelper 的存储），并在供奉升级后触发开放状态重检；不要改 `data/offering_levels.json`。R5-075：`game/talk/exec/ExecTransSceneDummyPoint.java` 解析带方括号的坐标串（格式先从 TalkExcelConfigData 里汇总所有该执行项的参数样式），传送失败时让 TalkManager 返回失败、不推进对话完成事件（看第十三批 TalkManager.triggerTalkAction 已有 retcode 返回的写法）。


# 通用规则（必须遵守）

- 项目：原神 7.1 私服 AstaPS-Orpheus（Grasscutter 下游 fork，Java 21，Gradle）。工作目录 `F:\Fork Reversed-worktrees\Genshin\fix14-ql-d`，是 git worktree，分支 `fix14-ql-d`，基线 main `88b9edb5`。只在这个目录里改文件。
- 证据里 `J/` = `src/main/java/emu/grasscutter/`，`R/` = `resources/`，`B/` = `resources/BinOutput/Quest/`。行号是第五轮盘点时的，可能略有偏移。
- `resources/` 和 `data/admin-catalog/` 是指向主仓的只读联接，**不要修改**。
- 不要碰：`data/investigation_elite_respawn.json` 等运行时状态文件、`data/offering_levels.json`、`data/_sakura_reward_5x.flag`、`TrialPrepareCommand.java`、`config.json`、`docs/规划.md`。不改玩家存档；需要存档迁移时写成 `PlayerDataMigrations` 里的版本化规则。
- 只修下面列出的条目。每条先在代码里确认问题仍然存在；已经不存在的写进汇报，不要改。
- 修普适的通用代码，按官方逻辑和资源表字段实现，不写一次性脚本，不针对单个 ID 打补丁，缺协议、缺数据的不编造。
- 其他会话同时在改 `ScriptLib.java`、`QuestManager`、`ShopSystem`、`DailyTaskManager`、`Player.java` 等共享文件：改动尽量局部，不做无关重排和格式化。
- 读资源大文件时用脚本汇总（`python -I`），不要整份打印。
- 代码注释用简体中文；风格跟周围代码一致。
- 第五轮报告（只读）：`F:\Fork Reversed-worktrees\Genshin\gap-audit\docs\客户端需求缺口盘点\第五轮.md`、`F:\Fork Reversed-worktrees\Genshin\gap-audit\docs\客户端需求缺口盘点\第五轮\`、`F:\Fork Reversed-worktrees\Genshin\gap-audit\docs\客户端需求缺口盘点\第四轮\`，按条目编号（R5-xxx，或 prev 的 R4-xxx）搜索证据和失败路径。
- 完成标准：
  1. `./gradlew.bat --no-daemon compileJava` 通过；
  2. 每条修复写针对性 JUnit 测试（`src/test/java/...` 对应包下，参考已有测试，不依赖数据库和服务器启动），只跑这些测试：`./gradlew.bat --no-daemon test --tests "<测试类>"`。不要跑全量测试。
  3. 本地提交到当前分支（`git add` 只加你改的文件；不要 push、不要切分支、不要动 main）。提交信息用简体中文，第一行概括，正文按条目编号列出改了什么；作者已在 git config 配好，不要改；**不要加 Co-Authored-By**。
- 大工作量项一次做不完时，先做能让失败路径断掉的最小可用实现，剩下的写进汇报。
- 最终汇报（会被保存成文件）控制在 30 行内：提交号；每个条目的结论（已修 / 基线已不存在 / 没修及原因 / 部分完成及剩余）；改了哪些文件（标出共享文件）；测试结果；每项进游戏怎么验证（一句话，地点写玩家找得到的名字或任务名）。

# 任务卡 fix14-ql-c：任务完成条件：改名完成、时间变量刷新策略、景物状态、离开范围加房间、QuestCheck

模型 gpt-6-sol，强度 high。

## 要修的条目（第五轮确认待修）

### R5-037 缺枚举 · QUEST_CONTENT_MISC_RENAME_COMPLETE（第四轮 R4-020）
- 证据：R/BinOutput/Quest/75000.json:672-678；J/game/quest/enums/QuestContent.java:75-107；J/data/excels/quest/QuestData.java:56-59
- 说明：75000 子任务的改名完成条件进入 QuestData.onLoad，枚举无该项而被过滤；改名后该条件不能使任务推进。建议：在 QuestContent 增枚举，并给任务改名完成事件接 @QuestValueContent。

### R5-054 缺枚举 · QUEST_CONTENT_TIME_VAR_PASS_REFRESH_POLICY（第四轮 R4-186）
- 证据：R/BinOutput/Quest/76046.json:205；J/game/quest/enums/QuestContent.java:76；J/game/quest/QuestManager.java:396-421；J/game/quest/QuestSystem.java:100-113
- 说明：7604603 唯一完成条件已可解析，但 checkTimeVars 只投递 GT_EQ 与 PASS_DAY；无该类型 content 处理器，时间满足仍不完成。建议：在 QuestManager.checkTimeVars 投递刷新策略事件，并加快照判定的 @QuestValueContent。

### R5-055 缺枚举 · QUEST_CONTENT_SCENERY_STATE_EQUAL（第四轮 R4-187）
- 证据：R/BinOutput/Quest/71139.json:230；J/game/quest/enums/QuestContent.java:77；J/game/quest/QuestSystem.java:100-113
- 说明：7113902 的失败条件可解析，景物状态变化却无此任务事件/快照处理器；目标状态出现时应失败的子任务仍保持原状态。建议：在景物状态更新入口向 QuestManager 投事件，并加 @QuestValueContent 读取当前状态。

### R5-056 缺枚举 · QUEST_CONTENT_LEAVE_SCENE_RANGE_AND_ROOM（第四轮 R4-188）
- 证据：R/BinOutput/Quest/3015.json:46；J/game/quest/QuestManager.java:373-383；J/game/quest/content/ContentLeaveSceneRange.java:22,83-102
- 说明：主任务 3015 子任务 301502 离开场景 3 指定范围及 room 230 时，onTick 只投递普通 LEAVE_SCENE_RANGE，且现有处理器不判 room；唯一完成条件不触发。建议：在 QuestManager 投递联合类型，扩展 ContentLeaveSceneRange 读取 room。

### R5-057 缺枚举 · QUEST_CONTENT_QUEST_CHECK_EQUAL（第四轮 R4-190）
- 证据：R/BinOutput/Quest/19182.json:1674-1717；J/game/quest/enums/QuestContent.java:85；J/game/quest/QuestSystem.java:100-113
- 说明：1918220 完成/失败条件读 check 1005=1/0，枚举存在却无处理器、状态重放或事件生产者；check 改变或先于任务激活时均不能据此推进。建议：在 QuestManager 保存/投递 QuestCheck 状态，并加 @QuestValueContent 快照判定。

## 提示

代码在 `game/quest/enums/QuestContent.java`、`game/quest/content/`、`game/quest/QuestManager.java`、`QuestSystem`。每个都要：枚举（若缺）、`@QuestValueContent` 处理器、事件生产者（在对应状态变化处 `queueEvent`），以及任务激活时已满足的快照判定。R5-037 改名完成在改名请求（如 SetPlayerNameReq / 改名相关 handler）成功后投递。R5-054 看 RefreshPolicyExcelConfigData（第十三批去重时保留在基线，可以用）和 checkTimeVars。R5-055 景物状态找现有景物（scenery）状态存储；没有就说明。R5-056 room 判定参考 ContentLeaveSceneRange 和场景 room 数据（找不到 room 数据就按资源能支持的程度做并说明）。R5-057 QuestCheck 需要持久化 check 值（存进 QuestManager 或 PlayerProgress），并在设置处投事件；先查清谁设置 check（执行项、Lua、请求）。


# 通用规则（必须遵守）

- 项目：原神 7.1 私服 AstaPS-Orpheus（Grasscutter 下游 fork，Java 21，Gradle）。工作目录 `F:\Fork Reversed-worktrees\Genshin\fix14-ql-c`，是 git worktree，分支 `fix14-ql-c`，基线 main `88b9edb5`。只在这个目录里改文件。
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

# 任务卡 fix14-ql-b：每日委托：重复领奖回包、questId 型委托生成、场景型委托挑战/机关结算

模型 gpt-6-sol，强度 high。

## 要修的条目（第五轮确认待修）

### R5-011 四委托奖励先自动发，领取请求不校验发奖结果（第四轮 R4-042）
- 证据：J/server/packet/recv/HandlerTakeDailyTaskScoreRewardReq.java:21-49；J/game/dailytask/DailyTaskManager.java:1111-1187,1191
- 说明：完成四委托并首次领取后再发 TakeDailyTaskScoreRewardReq，HandlerTakeDailyTaskScoreRewardReq:39-49 因已领而跳过发奖却仍回 RET_SUCC 与奖励预览；客户端见成功，实际不加物品。修：在已领分支明确回重复领取错误码且不带奖励预览，并保持首次发奖事务。

### R5-038 257 条 questId 型每日委托不能由现有生成器选入（第四轮 R4-040）
- 证据：J/game/dailytask/DailyTaskManager.java:304-334,424-431,538-549；资源流式统计 R/ExcelBinOutput/DailyTaskExcelConfigData.json = 257
- 说明：每日刷新 generateDailyTasks 先过 isSupportedTask，仅准场景杀怪型，257 条 questId 资源行不进候选；客户端不会接到对应剧情委托。建议：在 DailyTaskManager 的候选、任务激活及结算链增加 questId 型分支。

### R5-039 243 条场景型委托的挑战/机关结算未接入（第四轮 R4-041）
- 证据：J/game/dailytask/DailyTaskManager.java:424-431,538-549,824-875；资源流式统计 243 条，例 ID 10300 为 DAILY_FINISH_GADGET_ID_NUM
- 说明：同一过滤器拒绝非 DAILY_FINISH_MONSTER_NUM 场景委托，挑战或机关结束事件也没有进入当前每日任务完成路径；客户端抽不到这些委托。建议：扩展 DailyTaskManager 的类型准入，并接挑战/机关结算事件。

## 提示

代码在 `game/dailytask/DailyTaskManager.java`、`HandlerTakeDailyTaskScoreRewardReq`、相关 Packet。R5-011 小，先做。R5-038/R5-039 是大项：先盘点 DailyTaskExcelConfigData 里各 finishType 的数量与参数（脚本汇总），再扩展候选准入：questId 型要能激活对应任务、任务完成时结算委托（看 QUEST_EXEC_NOTIFY_DAILY_TASK 是否已在第十三批实现，复用它）；场景型要接挑战结束（挑战成功事件）和机关（DAILY_FINISH_GADGET_ID_NUM 等）事件计数结算。对确认无法闭环的类型继续过滤，避免玩家接到完成不了的委托。做不完的类型列在汇报里。委托变量已有 getDailyTaskVar/setDailyTaskVar，不要另建存储。不要改日切时间逻辑（另一张卡在改 Player 日切）。


# 通用规则（必须遵守）

- 项目：原神 7.1 私服 AstaPS-Orpheus（Grasscutter 下游 fork，Java 21，Gradle）。工作目录 `F:\Fork Reversed-worktrees\Genshin\fix14-ql-b`，是 git worktree，分支 `fix14-ql-b`，基线 main `88b9edb5`。只在这个目录里改文件。
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

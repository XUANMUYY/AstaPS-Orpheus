# 任务卡 fix14-ql-a：队伍锁：镜像队、角色锁、试用角色锁不被换队请求绕过

模型 gpt-6-sol，强度 high。

## 要修的条目（第五轮确认待修）

### R5-002 缺枚举 · QUEST_EXEC_LOCK_MIRROR_AVATAR_TEAM（第四轮 R4-007）
- 证据：B/15036.json:528-534；J/game/quest/exec/ExecLockMirrorAvatarTeam.java:13-19；J/game/player/TeamManager.java:586-617,836-859,1376-1399
- 说明：7.1 传说任务 1503607.finishExec 锁镜像队并下发试用阵容；随后 ChooseCurAvatarTeamReq/SetUpAvatarTeamReq 进入 setCurrentTeam/setupAvatarTeam，均未检查镜像锁，重建实体和回包仍可替换任务阵容。资源 3 参数中后 2 参数未读，其含义单独待实机。修：在 TeamManager.setCurrentTeam/setupAvatarTeam 校验镜像锁及允许阵容，拒绝时保持实体并回正确状态；再核实后 2 参数。

### R5-006 缺处理器 · QUEST_EXEC_LOCK_AVATAR_TEAM（第四轮 R4-034）
- 证据：B/11102.json:332-336；J/game/quest/exec/ExecLockAvatarTeam.java:21-27；J/game/player/TeamManager.java:61-68,586-617,836-859
- 说明：1110204.finishExec 写角色 10000026 的锁；玩家在锁期发 ChooseCurAvatarTeamReq 或 SetUpAvatarTeamReq，服务端直接换当前/编辑已存阵容，角色约束失效并下发更新。修：在 TeamManager.setCurrentTeam/setupAvatarTeam 共用锁校验，限制要求角色及阵容，拒绝时同步现状。

### R5-007 缺处理器 · QUEST_EXEC_GRANT_TRIAL_AVATAR_AND_LOCK_TEAM（第四轮 R4-035）
- 证据：B/12026.json:814-819；J/game/quest/exec/ExecGrantTrialAvatarAndLockTeam.java:21-29；J/game/player/TeamManager.java:70-88,473-525,836-859
- 说明：1202605.finishExec 发试用角色 32 并置锁；接着 ChooseCurAvatarTeamReq 重建普通队，setCurrentTeam 不查锁，客户端可离开试用阵容。修：同一队伍请求校验覆盖试用锁，切换时保留试用实体，解锁执行项再清理试用角色。

## 提示

三条共用一处：`TeamManager.setCurrentTeam` / `setupAvatarTeam`（以及 ChooseCurAvatarTeamReq、SetUpAvatarTeamReq、ChangeAvatarReq 等改阵容的入口）要统一做锁校验。先读 ExecLockMirrorAvatarTeam、ExecLockAvatarTeam、ExecGrantTrialAvatarAndLockTeam 以及对应解锁执行项，弄清锁存在哪里。被拒时不改实体、回客户端能接受的失败码并同步当前队伍状态（避免客户端显示和服务端不一致）。R5-002 后两个参数含义只能从资源和客户端用法推断，推断不了就保留不读并在汇报说明。


# 通用规则（必须遵守）

- 项目：原神 7.1 私服 AstaPS-Orpheus（Grasscutter 下游 fork，Java 21，Gradle）。工作目录 `F:\Fork Reversed-worktrees\Genshin\fix14-ql-a`，是 git worktree，分支 `fix14-ql-a`，基线 main `88b9edb5`。只在这个目录里改文件。
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

# 任务卡 fix14-ql-e：登录与日切：纪行加载等待、任务加载完再发列表、04:00 日切、纪行周点数和整周离线重置

模型 gpt-6-sol，强度 high。

## 要修的条目（第五轮确认待修）

### R5-170 [存档持久化][登录/纪行] 独立纪行加载未等待，慢查询可中断登录（第四轮 无）
- 证据：Player.java:1690-1709 只 awaitLoad 角色、背包、成就；Grasscutter.java:83-91 加载池有 6 个并行线程；Player.java:1700,1479-1482 将纪行加载另投线程；Player.java:1792,1822 与 PacketBattlePassAllDataNotify.java:14 直接解引用；HandlerPlayerLoginReq.java:66,74 登录响应在 onLogin 后
- 说明：失败路径：GetPlayerTokenReq 加载玩家 → 纪行查询尚未返回，但前三项先完成 → PlayerLoginReq 进入 onLogin → 首次跨日的 doDailyReset() 或纪行全量包解引用空 battlePassManager → 抛异常，PlayerLoginRsp 不会走到第 74 行。需要以延迟纪行读取的复现确认命中频率。

### R5-171 [存档持久化][登录/任务] 任务文档未加载完即可发空任务列表（第四轮 无）
- 证据：Player.java:1698,1707-1709,1794,1827；QuestManager.java:866-889 在异步任务中逐条写 mainQuests；QuestManager.java:337-359 登录扫描；PacketQuestListNotify.java:21-34 从当下内存构造全量
- 说明：失败路径：已有任务存档的玩家在任务 DB 读取慢于角色/背包/成就时登录 → onLogin() 扫描空 mainQuests 并发空 QuestListNotify → 读取线程稍后只把任务写入 map，没有重发该全量；客户端本次会话看不到已存任务，任务登录恢复也被跳过。与 R1-025 队伍全量及 R1-030 纪行包缺发不是同项。

### R5-173 [存档持久化][日切] 每日委托、纪行日任务及树脂购买计数在 0 点先重置（第四轮 无）
- 证据：Player.java:1575,1603-1632,1792 以 LocalDate 自然日判断并在在线 tick/登录执行；DailyTaskManager.java:225-253,1303-1309 委托另以系统时区自然日为配额键；ShopSystem.java:17-18,31-40 商店以上海 4 点计算刷新
- 说明：失败路径：服务器在上海时区，玩家 23:59 仍在线，跨到 00:00 后下一次 Player.onTick() → 委托换板、纪行日任务清进度、resinBuyCount 归零；按题定 04:00 日切，应保留到 04:00。若机器时区不是上海，偏差还会变化。树脂存量走按秒补充，不应被当成日切清零。

### R5-174 [存档持久化][纪行] 每周点数 cyclePoints 累加却从不归零（第四轮 无）
- 证据：BattlePassManager.java:25-26,69-81 按 BATTLE_PASS_POINT_PER_WEEK - cyclePoints 限额并累加；BattlePassManager.java:349-364 周任务只清状态/进度；全类 cyclePoints 引用仅声明、限额、累加；Player.java:1620-1622 调用周重置
- 说明：失败路径：本周领取纪行任务点数使 cyclePoints 达周上限 → 下周一触发 resetWeeklyMissions()，计数仍为上限 → 再领取周任务时 addPointsDirectly(..., true) 得到 0 并直接返回；客户端任务虽可刷新，纪行等级点数不再增长。

### R5-175 [存档持久化][纪行] 整周离线后周任务不重置（第四轮 无）
- 证据：Player.java:1607-1622 只在执行重置当天是周一时调 resetWeeklyMissions()；BattlePassManager.java:349-364 周任务恢复逻辑只有该调用
- 说明：失败路径：上周日完成/领取周任务后离线，直到下周二才登录 → 日期已变化，但不是周一 → 只重置日任务，周任务仍保留旧 POINT_TAKEN/进度，客户端无法开展新一周对应任务。该项与上一项的周点数上限是不同状态。

## 提示

代码在 `game/player/Player.java`（加载 awaitLoad、onLogin、onTick 日切）、`game/battlepass/BattlePassManager.java`、`game/quest/QuestManager.java` 的异步加载、`DailyTaskManager` 的日期键、`ShopSystem` 的刷新时间。R5-170/171：登录前等待纪行和任务加载完成（参考已有角色/背包/成就的 awaitLoad 写法），不要在 onLogin 用未加载的数据。R5-173：统一用「上海时区 04:00」作为日切边界（ShopSystem 已有类似计算，抽成公共工具或复用），只改日切判定，不改树脂按秒补充；DailyTaskManager 里只改日期键计算，改动尽量小（另一张卡在改委托生成）。R5-174：周重置时 cyclePoints 归零。R5-175：周重置按「是否跨过了周一 04:00」判断，而不是「今天是周一」。需要存档字段时注意兼容旧存档。


# 通用规则（必须遵守）

- 项目：原神 7.1 私服 AstaPS-Orpheus（Grasscutter 下游 fork，Java 21，Gradle）。工作目录 `F:\Fork Reversed-worktrees\Genshin\fix14-ql-e`，是 git worktree，分支 `fix14-ql-e`，基线 main `88b9edb5`。只在这个目录里改文件。
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

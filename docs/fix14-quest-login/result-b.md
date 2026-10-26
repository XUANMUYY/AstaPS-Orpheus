已在 `fix14-ql-b` 提交：`65e5d12c3ede3c8135d8d8e28aeeb3b3a5718b92`。工作区干净；尚未进游戏验证。

- **R5-011 已修**：重复领取返回“奖励已领取”，不附奖励预览；首次发奖失败仍可重试。进游戏验证：完成四个每日委托后连续领取两次，确认第二次报错且物品不增加。
- **R5-038 部分完成**：剧情型委托已能进入候选、激活任务，并通过现有完成回调结算。资源预筛中 238/257 条满足起始任务、重复执行和回调条件；其余 19 条继续过滤。玩家已有的剧情主任务也会跳过，重复开启该任务线仍待实现。进游戏验证：抽到蒙德委托「紧急修理」后，完成艾伦的任务，确认委托结算。
- **R5-039 部分完成**：已接入挑战成功和指定机关死亡的组内计数；缺少可用组资源的委托继续过滤。进游戏验证：分别完成「危险运输现象」和「攀高危险」，确认挑战成功及摧毁两座哨塔后各自结算。

修改文件：[DailyTaskManager.java（共享）](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/game/dailytask/DailyTaskManager.java>)、[DailyTask.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/game/dailytask/DailyTask.java>)、[DailyTaskData.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/data/excels/DailyTaskData.java>)、[MainQuestData.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/data/binout/MainQuestData.java>)。
事件与领奖文件：[WorldChallenge.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/game/dungeons/challenge/WorldChallenge.java>)、[EntityGadget.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/game/entity/EntityGadget.java>)、[HandlerTakeDailyTaskScoreRewardReq.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/main/java/emu/grasscutter/server/packet/recv/HandlerTakeDailyTaskScoreRewardReq.java>)。
定向测试：[DailyTaskQuestAndSceneCompletionTest.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/test/java/emu/grasscutter/game/dailytask/DailyTaskQuestAndSceneCompletionTest.java>)、[DailyTaskScoreRewardClaimTest.java](<F:/Fork Reversed-worktrees/Genshin/fix14-ql-b/src/test/java/emu/grasscutter/server/packet/recv/DailyTaskScoreRewardClaimTest.java>)。

`compileJava` 通过；仅运行上述两类定向测试，7 个用例通过。编译和单测不代表原生客户端结算已验收。


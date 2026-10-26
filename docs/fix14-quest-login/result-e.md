已在 `fix14-ql-e` 本地提交 **`2e52e84b`**，工作区干净；未推送、未部署。

- **R5-170 已修**：登录前等待纪行加载完成，加载失败会中止登录并报告原因。
- **R5-171 已修**：登录前等待任务存档加载完成，再扫描任务并发送列表。
- **R5-173 部分完成**：委托日期键、纪行日任务和树脂购买计数已按上海时间 **04:00** 日切；树脂按秒恢复未改。若旧版恰在切换当天 00:00–04:00 已生成委托，旧日期键仍可能导致该次委托提前换板；按任务卡要求未改另一张卡负责的委托生成逻辑。
- **R5-174 已修**：周重置清零 `cyclePoints` 并保存纪行状态。
- **R5-175 已修**：跨过周一 04:00 即重置周任务，周二登录也会执行。

修改文件：共享文件 [Player.java](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/main/java/emu/grasscutter/game/player/Player.java>)、[BattlePassManager.java](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/main/java/emu/grasscutter/game/battlepass/BattlePassManager.java>)、[DailyTaskManager.java](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/main/java/emu/grasscutter/game/dailytask/DailyTaskManager.java>)；新增 [GameDay.java](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/main/java/emu/grasscutter/utils/GameDay.java>) 及对应的 [登录测试](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/test/java/emu/grasscutter/game/player/PlayerLoginLoadsTest.java>)、[周重置测试](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/test/java/emu/grasscutter/game/battlepass/BattlePassWeeklyResetTest.java>)、[日期边界测试](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-e/src/test/java/emu/grasscutter/utils/GameDayTest.java>)。

验证：`compileJava` 通过；只运行上述 3 个测试类，**5 个测试通过**。尚未进游戏验证。

进游戏验收：

- R5-170：延迟纪行查询后登录已解锁纪行的角色，确认能进世界并打开「纪行」。
- R5-171：延迟任务查询后登录有已存任务的角色，确认「任务」列表显示原有任务。
- R5-173：跨 00:00 和 04:00 查看「冒险之证」每日委托、「纪行」日任务及树脂购买次数，确认只在 04:00 切换。
- R5-174：本周纪行点数达到上限后跨周，领取新周任务，确认纪行点数继续增长。
- R5-175：周日领取纪行周任务后离线至周二，登录确认周任务重新可做。


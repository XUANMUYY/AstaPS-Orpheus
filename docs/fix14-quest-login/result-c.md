已在 `fix14-ql-c` 本地提交 **`7cff2145477df8b45ebebf1c39973934b43d6e5b`**；工作区干净，未推送。五条中一条已修、一条部分完成、三条因缺少状态来源未修。

| 条目 | 结论 | 进游戏验证 |
| --- | --- | --- |
| R5-037 | **已修**：改名成功后投递事件，并用已保存的改名记录在任务激活时判定。 | 在任务追踪的角色改名步骤完成改名，检查 7500007 是否推进；重登后检查快照判定。 |
| R5-054 | **部分完成**：已接时间事件及间隔策略处理器；资源表缺少策略 `990210`，7604603 仍不能完成。 | 在 76046 的等待刷新步骤跨过刷新时间，确认该阻点仍在。 |
| R5-055 | **未修**：没有可用的景物状态存储或变更入口。 | 在 71139 的景物状态步骤切换目标景物，检查失败条件。 |
| R5-056 | **未修**：没有玩家当前 room 状态；仅按距离完成会误判。 | 在须弥城中心的 301502 步骤离开指定范围及房间，检查完成条件。 |
| R5-057 | **未修**：未找到 QuestCheck 结果的设置入口或持久状态。 | 在 1918220 的 QuestCheck 步骤产生检查结果，检查完成或失败条件。 |

改动文件：[QuestManager.java（共享）](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-c/src/main/java/emu/grasscutter/game/quest/QuestManager.java>)、[QuestContent.java](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-c/src/main/java/emu/grasscutter/game/quest/enums/QuestContent.java>)、[HandlerQuestRenameAvatarReq.java](</F:/Fork Reversed-worktrees/Genshin/fix14-ql-c/src/main/java/emu/grasscutter/server/packet/recv/HandlerQuestRenameAvatarReq.java>)、两个新 content 处理器及对应两个 JUnit 测试，共 7 个文件。

`compileJava` 通过；只运行了这两个指定测试类，均通过。**尚未进游戏验证。** R5-037 将资源中的类型 `2` 按改名请求的 `renameId` 匹配，仍需客户端实测确认。


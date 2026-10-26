已在 `fix14-ql-a` 提交 **`91566fe2`**；工作树干净。

- **R5-002 部分完成**：镜像锁期间，换队和编队请求会被拒绝，服务端保留试用实体并同步当前队伍。资源参数后两位的含义仍无法确认，保留不读。
- **R5-006 已修**：指定角色锁期间，目标阵容必须包含任务要求的角色。
- **R5-007 已修**：任务试用角色锁期间，换队请求无法重建普通队；解锁继续走现有试用角色清理路径。

改动文件：[TeamManager.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/main/java/emu/grasscutter/game/player/TeamManager.java)（共享核心）、[TeamSetupHelper.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/main/java/emu/grasscutter/game/player/TeamSetupHelper.java)、[ExecLockAvatarTeam.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/main/java/emu/grasscutter/game/quest/exec/ExecLockAvatarTeam.java)、[HandlerSetUpAvatarTeamReq.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/main/java/emu/grasscutter/server/packet/recv/HandlerSetUpAvatarTeamReq.java)、[PacketChooseCurAvatarTeamRsp.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/main/java/emu/grasscutter/server/packet/send/PacketChooseCurAvatarTeamRsp.java)、[PacketSetUpAvatarTeamRsp.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/main/java/emu/grasscutter/server/packet/send/PacketSetUpAvatarTeamRsp.java)、[TeamManagerQuestLockTest.java](/F:/Fork%20Reversed-worktrees/Genshin/fix14-ql-a/src/test/java/emu/grasscutter/game/player/TeamManagerQuestLockTest.java)。

验证：`compileJava` 通过；目标 JUnit 测试 **3/3 通过**。尚未进游戏实测客户端表现。

- R5-002：在丝柯克传说任务「吞星」触发镜像队后，尝试切换和编辑队伍，确认试用阵容保持。
- R5-006：在魈传说任务触发角色锁后，尝试切到不含魈的队伍或将魈移出阵容，确认请求被拒。
- R5-007：在荒泷一斗传说任务获得试用角色后尝试换队，确认试用角色仍在；完成解锁后确认其被清理。


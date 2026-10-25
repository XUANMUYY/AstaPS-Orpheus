先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 12：养成和队伍

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/avatar/Avatar.java、game/player/TeamManager.java、game/systems/InventorySystem.java、HandlerUnlockAvatarTalentReq.java、头像框相关。

## 要修的条目

### R3-155 [战斗]风主二命的 +16% 充能效率未进入最终属性
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：① resources/ExcelBinOutput/AvatarTalentExcelConfigData.json:14337-14349：talentId 72 的 addProps 是 FIGHT_PROP_CHARGE_EFFICIENCY +0.16；resources/BinOutput/Talent/AvatarTalents/ConfigTalent_Player_Wind.json:370 对应 openConfig 为空。② main:AvatarTa…
- 说明：失败路径：解锁风主二命 talentId 72 → Avatar.recalcStats 遍历命座却忽略其 addProps → 最终 FightProperty 少 0.16，AvatarFightPropNotify 下发的充能效率偏低；进入服务端非固定能量回复时也按偏低数值计算。当前 762 条 AvatarTalent 行中仅这一条有非零 addProps，范围已限定。

### R3-191 [养成队伍]删除较小编号的自定义队伍后编辑后续队伍抛空指针
- 影响：卡住流程；工作量：小；第三轮核实：待修
- 证据：main: TeamManager.java:160-165 以 1..teams.size() 当作连续键并直接调用 teams.get(i).equals；:1196-1221 允许 5、6 等队伍并删除任一键；HandlerSetUpAvatarTeamReq.java:15-19；旧日志同名 RECV 5 次 → TeamSetupHelper.java:42-44 → TeamManager.java:317-339
- 说明：失败路径：先新增队伍 5、6，删除 5，再向队伍 6 发送 SetUpAvatarTeamReq。TeamSetupHelper 已清空并写入队伍 6，addAvatarsToTeam 内的 getTeamId 在 i=5 对 null 调用 equals，SetUpAvatarTeamRsp 不会发出；编队界面可能一直等待，服务端内存状态也已改变。与 R1-025 的登录全量通知是不同缺口。

### R3-192 [养成队伍]7.1 新头像框 100015–100021 被当作头像写入
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：resources/ExcelBinOutput/ProfileFrameExcelConfigData.json 有 12 框，MaterialExcelConfigData.json 的 216005–216011 对应这 7 框；main: ProfileUnlocks.java:100-133 会列出已解锁框；HandlerMiao6969.java:21,78-80 只识别 100000、100011–100014；HandlerSetPlayerHeadImag…
- 说明：失败路径：玩家持有 216005 等解锁道具，打开头像框列表选用 100015（请求 6969）。无论两个同包号处理器谁胜出，都会写 headImage 而不写 profileFrameId；客户端个人资料的头像框保持旧值，头像可能变错。双处理器注册顺序还需运行时指纹确认，但不影响这 7 个 ID 的静态结论。

### R3-194 [养成队伍]冒险等级/世界等级跳过 PlayerLevelLock 的突破任务
- 影响：功能缺失；工作量：中；第三轮核实：待修
- 证据：resources/ExcelBinOutput/PlayerLevelLockExcelConfigData.json 有 10 档，世界等级 2、4、6、7、9 行分别含 unlockMainQuestId 25001、25005、25009、25011、25015 及等级上限；main: Player.java:856-872 earnExp 只查 PlayerLevel 的经验值并连续 setLevel；:874-892 用等级阈值直接改世界等级；全 main 数据…
- 说明：失败路径：冒险等级 25 且突破任务 25001 未完成时获得足够冒险经验，addExpDirectly 仍升到 26，updateWorldLevel 仍设为 2；本应受突破任务约束的等级/世界等级门槛被跳过。与 R2-042 的副本动态怪物等级无关。

### R3-195 [养成队伍]角色突破未校验所需冒险等级
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：resources/ExcelBinOutput/AvatarPromoteExcelConfigData.json 的 875 行中 744 行有 requiredPlayerLevel（15/25/30/35/40/50）；main: AvatarPromoteData.java:13,42-44 已读该字段；HandlerAvatarPromoteReq.java:12-29 → InventorySystem.java:594-651 只检查当前角色级别、表行、材料…
- 说明：失败路径：低冒险等级账号有足够材料，发送 AvatarPromoteReq；达到角色当前上限后，payItems 成功就突破并回 AvatarPromoteRsp，越过表内玩家等级门槛。新维度，区别于已列的 R2-090 武器突破同类问题。

### R3-196 [养成队伍]命座重复请求重复扣材料且回成功
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：main: HandlerUnlockAvatarTalentReq.java:13-23 直接传客户端 talentId；Avatar.java:1187-1207 在校验是否已解锁、是否属于当前技能库之前先 payItem(...,1)，随后 HashSet.add 的结果未检查，仍发 AvatarUnlockTalentNotify 和成功 UnlockAvatarTalentRsp；Avatar.java:461-475 表明有效命座本应按当前技能库过滤
- 说明：失败路径：同一已解锁 talentId 再发 UnlockAvatarTalentReq，材料再次扣除；Set 不增加命座，客户端却收到成功回包。传入其他角色的 talentId 也未被拒绝，但其客户端影响另需验证。7.1 AvatarTalent 表的 mainCostItemCount 全为 1，故不把「固定扣 1」误报为数量错误。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card12.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/共用说明.md，遵守其中全部规则。

# 任务卡 D：角色点阵移动、追踪线索组、世界 Boss 103

- 工作目录（本卡专用 worktree）：`F:/Fork Reversed-worktrees/Genshin/fix14-script-d`，分支 `fix14-script-d`。只在这里改和提交。
- 结果文件：`C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/out/cardd.md`

## 要修的条目

### R5-016 未实现 MoveAvatarByPointArray
- 影响：功能缺失；工作量：大；第四轮编号 R4-067
- 证据：S/Scene/1/scene1_block1101.lua:121、scene1_group111101124.lua:166-173；J/scripts/ScriptLib.java:1195-1224
- 失败路径和建议：场景 1 开放世界组 111101124 的传送机关 TeleportAction 使用点列与 speed=10；MoveAvatarByPointArray 只求终点并 softTeleportInScene，客户端直接瞬移，沿点阵动作/速度丢失。修：在 ScriptLib.MoveAvatarByPointArray 用 PointArrayAvatarMovement 输出整条路线，接入已核实的角色路线移动同步并保留速度/标记；协议未核实前先做原生抓包。

### R5-040 未实现 RefreshHuntingClueGroup
- 影响：卡住流程；工作量：大；第四轮编号 R4-046
- 证据：R/Scripts/Scene/3/scene3_group133001434.lua:12-36,65-86；J/scripts/ScriptLib.java:1331-1334
- 失败路径和建议：组 133001434 加载时 Lua 调函数；该组套件无实体，ScriptLib 只记录未实现并回成功，线索实体不能由此生成。建议：在 ScriptLib.RefreshHuntingClueGroup 按 HuntingData 与组 points 生成实体，并仅成功后回 0。

### R5-046 世界 Boss 103 缺出生位置
- 影响：功能缺失；工作量：中；第四轮编号 R4-130
- 证据：R/ExcelBinOutput/InvestigationMonsterConfigData.json:165-178；D/admin-catalog/scene_points.json:140462-140485；J/game/world/WorldBossSpawnHelper.java:1844-1869,1964-1977
- 失败路径和建议：Boss 索引读到调查 ID 103、组 133706025，但该行无 DJLCKJCAKDA 坐标、无组 Lua 或补丁坐标；ensureIndexBuilt 在位置为 null 时跳过登记，玩家追踪到区域不会遇到这只合成 Boss。trackingScenePointId=2262 对应的传送点坐标存在，不能等同精确出生点；原“完全缺数据”归因过宽。建议：在 WorldBossSpawnHelper.loadInvestigationPositions 接可信出生坐标来源，先校验点 2262 附近实体位置。

## 本卡要点
- R5-016：先查协议里有没有已核实的「角色按点阵移动」通知或已有的路线移动同步实现（搜 PointArray、AvatarFollowRoute、PlatformRoute 等）。有可靠协议才实现整条路线；没有的话本条跳过，写清缺什么，不要编造包。
- R5-040：`RefreshHuntingClueGroup` 按 HuntingData（看 resources/ExcelBinOutput 里 Hunting 相关表和场景组的 points）生成线索实体；只有成功生成才回 0，失败回非 0。数据对不上就做到「不再假装成功」为止，并写清缺什么。
- R5-046：先校验调查 103 / 组 133706025 是否真的拿不到坐标（组 Lua、scene_points、ScriptSceneData、补丁坐标都查一遍）。有可信坐标来源就接上；只有传送点 2262 的坐标的话，不要把传送点当出生点，本条跳过并写清。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果按共用说明的格式写到结果文件。

先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 11：地图世界

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：scripts/SceneScriptManager.java（刷新策略）、game/world/、PacketPlayerWorldSceneInfoListNotify.java、地图锚点传送处理器。

## 要修的条目

### R3-186 [地图世界]每日型野怪刷新被降为击杀后 24 小时
- 影响：功能缺失；工作量：中；第三轮核实：待修
- 证据：RefreshPolicyExcelConfigData.json 的 id 1002 是 REFRESH_DAILY/time=14400；resources/Scripts/Scene/3/scene3_block3001.lua:75 的 group 133001073 引用 1002，scene3_group133001073.lua:13-24 有怪物；main:RefreshPolicyExcelConfigData.java:43-50,78-81 把单元素时…
- 说明：失败路径：场景 3 该 group 的怪物死亡 → onMonsterKilled 保存 refreshId=1002 与击杀时刻 → 再进组/刷新时 isExpired 得到 86400 秒间隔 → 不按资源的每日定点策略复生。静态脚本统计有 1885 处 block 声明引用 1002，不能等同于 1885 个实际受影响怪物。

### R3-188 [地图世界]任务尚未解锁的多层地图默认全开
- 影响：功能缺失；工作量：中；第三轮核实：待修
- 证据：MapLayerExcelConfigData.json 有 431 层、MapLayerGroupExcelConfigData.json 有 190 组；resources/BinOutput/Quest/4011.json:529-533,660-664 在任务节点显式把层 3350401、3360101 设为 1；main:Player.java:318,625-627 初始状态表为空；main:PacketPlayerWorldSceneInfoListNotif…
- 说明：失败路径：新角色进入世界 → 初始 PlayerWorldSceneInfoListNotify 把任务后才设置为 1 的地图层提前列入 unlockMapLayerList → 后续任务「解锁」对这些层的列表没有变化。属于地图分层显示/进度提前暴露；尚未把这等同于传送权限，传送另有已解锁点校验。

### R3-190 [地图世界]地图锚点传送丢失资源指定朝向
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：resources/BinOutput/Scene/Point/scene3_point.json 的点 10 指定 tranRot.y=230.564...、rot.y=196.303...；逐文件统计 957 个 SceneTransPoint 中 892 个带 tranRot；main:PointData.java:22-25 已读取该字段；main:HandlerSceneTransToPointReq.java:20-56 仅传 tranPos；main:Worl…
- 说明：失败路径：从地图点选已解锁锚点 → 请求处理器用目标坐标调用传送重载，不传 tranRot → World 保留角色此前朝向 → 落地朝向与点位资源不符。跨场景和同场景 waypoint 共用此路径；坐标本身已使用 tranPos。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card11.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
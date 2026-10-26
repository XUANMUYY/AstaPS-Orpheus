# 任务卡 D 结果

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R5-016 | 跳过：未找到已核实的 7.1 角色路线通知字段或现成发送实现 | — | — | — | 场景 1 开放世界传送机关（约 2556, 212, -1341；组 111101124）：操作相连机关，观察角色仍直接到终点，未沿点阵以 speed=10 移动。 |
| R5-040 | 部分修：无实体生成时改回非 0；线索生成尚缺 HuntingData 运行态 | 14c07739 | `ScriptLib.java`、`ScriptLibHuntingClueTest.java` | `ScriptLibHuntingClueTest` | 场景 3、14 号传送锚点附近（约 1394, 234, -1548；狩猎区域 102，终点组 133001434）：触发组加载，确认失败分支不再被当作生成成功；线索实体仍不会出现。 |
| R5-046 | 跳过：无可靠 Boss 实体出生坐标 | — | — | — | 场景 3、2262 号传送锚点附近（约 7654, 208, 6276；调查 103、组 133706025）：追踪可抵达该区域，但当前不会生成 Boss；锚点坐标不能代替出生点。 |

## 核查依据与缺口

- R5-016：`MoveAvatarByPointArray` 当前只解析最后一个点并 `softTeleportInScene`；`AvatarFollowRouteNotify` 仅见 `PacketOpcodes` 包号，`protocol/7.1/files.txt`、`compat/`、现有发送代码均无可用的路线消息定义。需原生抓包核实字段、CmdId、speed 和 marker 语义后实现。
- R5-040：`HuntingGroupInfoExcelConfigData` 将组 133001434 标为区域 102 的 `HUNTING_CLUE_FINAL`，Lua 给出 5 个 points；当前服务端未找到玩家 HuntingData 运行态或该类线索的实体选择与生成流程，因此只修正虚假成功返回。
- R5-046：调查 103 的表项有组 133706025、怪物 25135202 和追踪点 2262，却无 `DJLCKJCAKDA`；场景 block、组 Lua、`ScriptSceneData`、现有补丁坐标均未给出该组出生点。2262 是传送锚点位置，不能作实体位置。

## 测试

- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.scripts.ScriptLibHuntingClueTest"`：通过，`BUILD SUCCESSFUL`（14m 43s）；仅定向单测，未部署或做原生客户端验证。

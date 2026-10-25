# 任务卡 5：副本复活和入口条件

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-041 | 已修（代码和定向测试；待 7.1 客户端验收） | `8ffd95d2` | `game/dungeons/DungeonManager.java`、`DungeonReviveCounter.java`；`game/player/TeamManager.java`；`server/packet/recv/HandlerDungeonDieOptionReq.java`、`HandlerDungeonPlayerDieReq.java` | `DungeonReviveCounterTest`、`HandlerDungeonDieOptionReqTest` | 进入 `reviveMaxCount=5` 或 `10` 的副本，激活复活点并全队阵亡；验证复活到复活点、次数逐次减少、用尽后不能再复活；另测重开重置次数、退出返回原场景、伪造不同副本 ID 的请求被拒。资源无 `reviveType`，不声称已验证类型差异。 |
| R2-045 | 已修（代码和定向测试；待 7.1 客户端验收） | `6992b42b` | `data/excels/dungeon/DungeonEntryData.java`；`game/dungeons/DomainDungeonHelper.java`、`enums/DungeonEntryCondCombType.java`；`server/packet/recv/HandlerGetDungeonEntryExploreConditionReq.java`；`server/packet/send/PacketGetDungeonEntryExploreConditionRsp.java` | `DungeonEntryDataTest` | 用入口配置 `10`（等级 10 **或**任务 35802）测试：两项都不满足时，探索请求返回阻挡条件且无法入场；满足任一项时可探索和入场。另确认副本自身 `limitLevel` 仍单独生效。 |

测试：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.data.excels.dungeon.DungeonEntryDataTest"`，通过；`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.dungeons.DungeonReviveCounterTest" --tests "emu.grasscutter.server.packet.recv.HandlerDungeonDieOptionReqTest"`，通过（修订后复跑）。仅完成编译和定向单元测试，未部署、未操作客户端。

推断边界：未配置 `reviveMaxCount` 按 0 次处理；`DIE_OPT_CANCEL` 按退出处理；死亡通知中的 `reviveCount` 按剩余次数填写；双条件均不满足时 `MULTIPLE` 原因的 `param1` 使用等级值。这些需以上述客户端操作核对。

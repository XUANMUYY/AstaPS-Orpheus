# 任务卡 11：地图世界

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
|---|---|---|---|---|---|
| R3-186 | 已修 | `d9b64947` | `RefreshPolicyExcelConfigData.java`、`OpenWorldSpawnHelper.java` | `RefreshPolicyExcelConfigDataTest.java`：03:59 击杀后 04:00 刷新、04:00 之后击杀等次日、间隔型不混用 | 在场景 3 击杀 group `133001073` 的怪物；国服时间 04:00 前重进仍无怪，过 04:00 后重进应复生，无需等击杀后 24 小时。 |
| R3-188 | 已修 | `ecdc3720` | `PacketPlayerWorldSceneInfoListNotify.java`、`ExecSetMapLayerUnlockState.java`、`Player.java` | `PacketPlayerWorldSceneInfoListNotifyTest.java`：任务门槛、显式状态覆盖、旧任务进度 | 新角色在完成 4011 对应任务前检查地图层 `3350401`、`3360101` 未提前开放；完成各解锁节点后再检查显示。已完成任务的旧角色应保留对应层。 |
| R3-190 | 已修 | `e569281c` | `HandlerSceneTransToPointReq.java` | `HandlerSceneTransToPointReqTest.java`：同场景、跨场景的 `tranRot`，以及缺失 `tranRot` 的回退 | 解锁并选场景 3 的锚点 10，从不同朝向传送；落地应按 `tranRot.y≈230.564°`，而非锚点实体的 `rot.y≈196.303°` 或传送前朝向。 |

定向编译与测试（均 `BUILD SUCCESSFUL`）：

```powershell
.\gradlew.bat test --no-daemon --tests "emu.grasscutter.server.packet.recv.HandlerSceneTransToPointReqTest"
.\gradlew.bat test --no-daemon --tests "emu.grasscutter.data.excels.RefreshPolicyExcelConfigDataTest"
.\gradlew.bat test --no-daemon --tests "emu.grasscutter.server.packet.send.PacketPlayerWorldSceneInfoListNotifyTest" --tests "emu.grasscutter.game.quest.exec.ExecSetMapLayerUnlockStateTest"
```

未部署、未启动原生客户端；进游戏验证步骤待用户执行。

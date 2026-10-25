# 任务卡 2 结果

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-035 | 已修 | `01e7e4e2` | `ScriptLib.java`、`ChallengeFactory.java`、`ChallengeFactoryHandler.java`、`TriggerCountChallengeFactoryHandler.java`、`Trigger2AvoidTrigger1ChallengeFactoryHandler.java`、`Trigger2AvoidTrigger1Trigger.java` | `ScriptLibChallengeTest`：Lua userdata 调用、六元素数组、按索引成功/失败停止、双触发条件、五元素保存进度 | 进入 scene 3 的组 `133001379`，踏入区域 `379077` 启动挑战；让平台 `379015` 到路线点 38 应失败，重试后击毁目标装置应成功。另在 scene 40028 组 `240028002` 让全队倒下，确认 `StopChallenge(1,0)` 结束当前挑战。 |
| R1-180 | 已修 | `01e7e4e2` | `ScriptLib.java`、`ChallengeFactory.java`、`ChallengeFactoryHandler.java`、`Trigger2AvoidTrigger1ChallengeFactoryHandler.java`、`Trigger2AvoidTrigger1Trigger.java` | `ScriptLibChallengeTest`：`StartChallenge(666,203,{26,888,8,2,999,1})` 经 Lua 绑定启动 | 在 scene 3 组 `133001379` 踏入区域 `379077`，确认挑战 HUD 出现、机关 `379015` 变为 `GearStart`、suite 2/3 内容出现且平台开始移动。完成后的 suite 切换还依赖 `ce3fa00c`。 |
| R1-181 | 已修（默认时限路径） | `01e7e4e2` | `ScriptLib.java` | `ScriptLibChallengeTest`：未配置 Effigy 限时条件时返回 `-1` | 进入 scene 40028、组 `240028002` 的副本，触发怪物 `2001` 出生；确认 Lua 不再因缺失函数报错，挑战 `215` 以默认 `420` 秒启动。当前服务端没有已选 Effigy 限时条件状态，条件扣减尚不能验证。 |

测试：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.entity.ScriptLibChallengeTest" --tests "emu.grasscutter.game.entity.ScriptLibReturnCodesTest" --tests "emu.grasscutter.game.dungeons.challenge.factory.ChallengeFactoryMissingTypesTest"`；11 个测试通过，编译通过。未部署、未进游戏验证。

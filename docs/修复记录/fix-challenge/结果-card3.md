# 任务卡 3：父子挑战链

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-034 | 已修 | `d6f8a364`、`98352057` | `FatherChallenge.java`、`FatherChallengeFactoryHandler.java`、`ChallengeFactory.java`、`WorldChallenge.java`、`PacketDungeonChallengeBeginNotify.java`、`ScriptLib.java`、`SceneScriptManager.java` | `ChallengeFactoryMissingTypesTest.fatherFactoryUsesLuaSuccessAndFailureThresholds`；`ScriptLibChallengeTest.fatherAndChildStartAndSettleFromGroupTrigger` | 场景 1 组 111101058，在装置 58001 选选项 7，观察 9001 父挑战和 9005 子挑战同时开始；完成子挑战后父挑战成功、选项恢复；等 300 秒应失败。 |
| R1-143 | 已修 | `d6f8a364` | `ScriptLib.java`、`FatherChallenge.java`、`PacketDungeonChallengeBeginNotify.java` | `ScriptLibChallengeTest.fatherAndChildStartAndSettleFromGroupTrigger`、`childCanBeAttachedAfterFirstStageCompletes` | 场景 1 组 111101094，进入区域 94041，破坏装置 94023、94024 使子挑战 1001 完成，再进入区域 94095；确认运行中追加的子挑战 1002 可完成并推动父挑战 100 结算。 |
| R1-153 | 已修 | `d6f8a364` | `ScriptLib.java`、`FatherChallenge.java`、`WorldChallenge.java` | `ScriptLibChallengeTest.fatherAndChildStartAndSettleFromGroupTrigger`、`childFailuresReachFatherThresholdAndRejectLaterChanges` | 场景 1 组 111101058，装置 58001 选 7 后确认倒计时和挑战目标出现；等待超时，确认父挑战失败事件和选项恢复，再次选择可重启。 |
| R1-152 | 已修 | `d6f8a364` | `ScriptLib.java`、`FatherChallenge.java`、`FatherChallengeFactoryHandler.java` | `ScriptLibChallengeTest.fatherAndChildStartAndSettleFromGroupTrigger`、`fatherPropertyAndChildStopUseTheirOwnIndexes` | 场景 1 组 111101094，进入区域 94041 后确认父挑战 100（配置 64）创建并随子挑战 1001 启动；在组 111101095 重走分支，核对 `CUR_SUCC` 属性能推进父挑战。 |
| R2-033 | 已修 | `a75c71f0`、`98352057` | `WorldChallenge.java`、`FatherChallenge.java`、`ScriptLib.java`、`SceneScriptManager.java` | `WorldChallengeEventArgsTest.successKeepsTheRegionThatStartedTheChallenge`、`fatherAndChildKeepTheLatestRegionReachedDuringTheChallenge` | 场景 9 组 199001094，乘船在装置 94001 选 175 启动赛船 22004，穿过终点区域 94159 并留在其中直到父挑战 2018 成功；确认 `EVENT_CHALLENGE_SUCCESS_94172` 的区域人数条件命中，装置 94149 被移除。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.dungeons.challenge.WorldChallengeEventArgsTest"`；`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.entity.ScriptLibChallengeTest" --tests "emu.grasscutter.game.dungeons.challenge.factory.ChallengeFactoryMissingTypesTest" --tests "emu.grasscutter.game.dungeons.challenge.WorldChallengeEventArgsTest"`。均 `BUILD SUCCESSFUL`；未跑全量测试，未进游戏、未部署。

范围说明：组 111101058 的后续 9004 子挑战类型 `CHALLENGE_KILL_COUNT_FROZEN_LESS` 仍无独立工厂，触发该分支时 `AttachChildChallenge` 会返回失败；HideAndSeek.lua 的按 UID 构造的超 32 位子挑战索引未作客户端协议验证。父挑战积分和进度参数映射依据 Lua 推断，需以上述实机路径复核。

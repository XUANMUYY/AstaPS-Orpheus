# 任务卡 12：养成和队伍

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
|---|---|---|---|---|---|
| R3-155 | 已修 | `f873a29f` | `Avatar.java` | `AvatarExtraTalentsTest.constellationAddPropsIncreaseFinalChargeEfficiency` | 风主解锁二命前后保持装备不变，角色面板及 `AvatarFightPropNotify` 中充能效率应增加 0.16。 |
| R3-191 | 已修 | `654a83d2` | `TeamManager.java` | `TeamManagerSparseTeamTest` | 创建自定义队伍 5、6，删除 5，再编辑 6；应收到 `SetUpAvatarTeamRsp`，重新打开编队仍为编辑后的队伍。 |
| R3-192 | 已修 | `38ab2e23` | `ProfileUnlocks.java`、`HandlerMiao6969.java`、`HandlerSetPlayerHeadImageReq.java`、`PacketSetPlayerHeadImageRsp.java` | `ProfileUnlocksTest.currentFrameTableClassifiesNewFramesAsFrames` | 持有 216005–216011 后分别选用头像框 100015–100021；框当场改变、头像不变，重登后仍保持。双处理器运行时注册顺序仍需指纹确认。 |
| R3-194 | 已修 | `a3d1a18a` | `GameData.java`、`PlayerLevelLockData.java`、`PlayerLevelLocks.java`、`Player.java`、`GameMainQuest.java` | `PlayerLevelLocksTest` | 冒险等级 25、突破任务 25001 未完成时继续获得冒险经验，应停在 25 且世界等级为 1；完成任务后结存经验升级并解锁世界等级 2。按表复核 25005、25009、25011、25015。 |
| R3-195 | 已修 | `0a2bfc6e` | `AvatarPromoteData.java`、`InventorySystem.java` | `AvatarPromoteDataLevelTest` | 角色达到当前上限且材料足够、冒险等级低于下一突破行的 `requiredPlayerLevel` 时请求突破，应不扣材料也不突破；达到门槛后可突破。 |
| R3-196 | 已修 | `f873a29f` | `Avatar.java` | `AvatarConstellationUnlockTest` | 首次解锁命座成功后重发同一 `UnlockAvatarTalentReq`，应回失败且命座材料不再减少；跨技能库或未满足前置命座的 ID 也应失败。 |

编译：`.\gradlew.bat compileJava compileTestJava --no-daemon`，通过。

定向测试：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.avatar.AvatarExtraTalentsTest" --tests "emu.grasscutter.game.avatar.AvatarConstellationUnlockTest" --tests "emu.grasscutter.game.player.TeamManagerSparseTeamTest" --tests "emu.grasscutter.game.player.ProfileUnlocksTest" --tests "emu.grasscutter.game.player.PlayerLevelLocksTest" --tests "emu.grasscutter.data.excels.avatar.AvatarPromoteDataLevelTest"`，15 个用例通过。之后增加前置命座断言，单独重跑 `AvatarConstellationUnlockTest` 通过。

未部署，未运行游戏客户端；上表最后一列是待执行的进游戏验收步骤。

# 任务卡 A：挑战与平台事件

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R5-076 | 已修；原生待验 | `c8160310` | `FatherChallenge.java`、`ScriptLib.java`、`TeamManager.java`、`Scene.java` | `ScriptLibChallengeTest.fatherChallengeFailsOnTeamWipeOnlyWhenRequested` | 场景 1 组 111101094，附近 `/tp 2154 217 -1402 1`：进入区域 94041 启动父挑战 100，让全队倒下；确认立即发挑战失败、组刷新、`hasStarted` 归零，能再次启动。再核对未写 `fail_on_wipe` 的父挑战不会被团灭直接判负。 |
| R5-144 | 已修；不同组并存属「推断」，原生待验 | `c8160310` | `Scene.java`、`ScriptLib.java`、`SceneScriptManager.java`、`EntityMonster.java`、`EntityGadget.java`、`EntityBaseGadget.java`、`TeamManager.java` | `ScriptLibChallengeTest.challengesInDifferentSceneGroupsCanRunAndStopIndependently` | 大世界场景 3，先在组 133003521 的移动平台附近（约 `/tp 2650 231 -1134 3`）保持挑战运行；另一玩家在组 133001379 附近 `/tp 1941 197 -1587 3` 进入区域 379077。确认 379015 变 `GearStart`、平台移动，两组挑战各自结算；同组重复进入仍拒绝。 |
| R5-062 | 已修；原生待验 | `35050448` | `EntityGadget.java`、`ConfigRouteDeadlineTest.java` | `ConfigRouteDeadlineTest.normalPollPhasesNeverFireEarly`、`retainedCallbackFiresOnceUnderConcurrentInvocations` | 须弥赤王陵场景 20151／副本 1156，组 220151005 的电梯操作台 5002／5004：乘 5001 电梯经过路线 6 的点 1、2、3、5、8，确认激活、提示、关墙与终点传送各触发一次；起点不提前触发。 |
| R5-041 | 已修；原生待验 | `e2dd78c4` | `ScriptPoolMonsterTideService.java`、`ScriptPoolMonsterTideServiceTest.java` | `ScriptPoolMonsterTideServiceTest.untaggedPoolMonsterUsesTaggedPointWithoutRelaxingExplicitTags` | 大世界场景 3，普尔希娜光钉附近 `/tp 2856 207 -27 3`，组 133007088：启动画廊 12002，确认池 18004 的首波怪在 tag 2 点位生成；推进到 tag 4／8 波，确认显式 tag 不串位。 |

定向测试：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.entity.ScriptLibChallengeTest" --tests "emu.grasscutter.game.entity.ConfigRouteDeadlineTest" --tests "emu.grasscutter.scripts.service.ScriptPoolMonsterTideServiceTest"`，第二次运行 `BUILD SUCCESSFUL`，29 项通过。首次运行 29 项中 2 项失败，均为测试夹具参数或组上下文设置错误，修正后复测通过。

验证边界：仅源码编译与定向单元测试；未部署、未连接运行服务或 MongoDB、未进原生客户端。R5-144 的两组 Lua 都使用挑战索引 666，客户端同时显示和结算仍需实机确认。worktree `fix14-script-a` 干净；未推送或合并。
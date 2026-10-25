# 任务卡 3：场景脚本事件参数

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-021 | 已修；时间点累计秒数的解释标为推断 | b4b4ebad | `SceneTimeAxis.java`、`ScriptLib.java`、`ScriptArgs.java` | `SceneScriptLifecycleTest.timeAxisEmitsEveryTimePointWithOneBasedIndex` | 在 scene 1 的 111101370 组触发 `init_killRoad` 和 `init_reset` 时间轴，确认第一个时间点对应的道路销毁、重置动作执行；多点时间轴逐点检查顺序。 |
| R2-026 | 已修 | b4b4ebad | `ScriptArgs.java` | `SceneScriptLifecycleTest.groupTimerFiresOnceAfterItsSeconds` | 在 scene 1 的 111101041 组启动四个机关之一的计时器，计时结束后确认对应机关回到 `Default`。 |
| R2-027 | 已修 | b4b4ebad | `ScriptArgs.java` | `SceneScriptLifecycleTest.variableEventExposesVariableNameToLua` | 在 scene 1 的 111101036 组改变 `moveState`，确认按变量名筛选的后续动作执行。 |
| R2-031 | 部分已修：`GADGET_CREATE` 补充 gadget ID 和 config 来源；`GROUP_LOAD` 及两处以状态/config ID 判断创建事件的脚本跳过，因为与当前事件语义冲突，不能从 group load 推出机关状态 | 32e885a7 | `EntityGadget.java` | `EntityGadgetCreateEventArgsTest` | 在 scene 3 的 133108200 组创建 gadget ID `70800060` 的机关，确认对应创建触发器动作；133303018 等 `GROUP_LOAD` 条件仍不会命中。 |
| R2-032 | 已修事件参数；所举 43001 父挑战脚本的创建接口目前尚未实现 | 1af48de5 | `WorldChallenge.java` | `WorldChallengeEventArgsTest` | 在已支持的挑战中主动失败，确认失败事件 `param1` 为挑战 ID；43001 的父挑战 228 要等 `CreateFatherChallenge` 实现后才能做游戏内端到端验证。 |
| R2-033 | 跳过：六处 Lua 均将 `source_eid` 当区域实体 ID，但 `WorldChallenge` 未保存挑战的来源区域；猜填任一区域会误判人数 | — | — | — | 待挑战创建路径保留确切区域实体 ID 后，在 scene 9 的 199001094 等组完成挑战并确认区域内角色数条件命中。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.scripts.SceneScriptLifecycleTest.timeAxisEmitsEveryTimePointWithOneBasedIndex" --tests "emu.grasscutter.scripts.SceneScriptLifecycleTest.groupTimerFiresOnceAfterItsSeconds" --tests "emu.grasscutter.scripts.SceneScriptLifecycleTest.variableEventExposesVariableNameToLua" --tests "emu.grasscutter.game.entity.EntityGadgetCreateEventArgsTest" --tests "emu.grasscutter.game.dungeons.challenge.WorldChallengeEventArgsTest"`。

结果：`BUILD SUCCESSFUL`；5 个定向测试通过。未进游戏、未部署。

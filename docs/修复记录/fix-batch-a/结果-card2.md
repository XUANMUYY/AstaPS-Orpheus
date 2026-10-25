# 任务卡 2 结果

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-019 | 已修；`config_id=0` 和实体创建返回 `null` 均报失败 | `227d7806` | `ScriptLib.java` | `ScriptLibReturnCodesTest.createGadgetRejectsZeroConfigInsteadOfReportingCreation` | 在隔离测试场景让 Lua 以 `config_id=0` 调用 `CreateGadget`，检查返回非 0、没有新实体；正常配置仍可创建。 |
| R2-020 | 已修；已有挑战和塔楼等待换队均报失败 | `227d7806` | `ScriptLib.java` | `ScriptLibReturnCodesTest.activeChallengeReportsFailureWhenAnotherChallengeIsRunning` | 已有挑战运行时重复触发挑战，以及塔楼下半场等待换队时触发，检查 Lua 收到非 0，旧挑战及计时不被替换；实机待验。 |
| R1-136 | 已修；切换成功和重复设置均返回 0 | `dd4c77e4` | `ScriptLib.java`、`PlatformPointArrayTest.java` | 更新 `PlatformPointArrayTest.changedArrayNotifiesOnceAndRetiresTheConfigRun` | 到场景 1 的 `111101090` 机关，触发平台点阵切换，确认后续 Lua 动作执行、平台移动；重复触发时不重复发换路通知；实机待验。 |
| R1-142 | 跳过：缺少狩猎任务运行态、线索点实体生成链及完整 7.1 狩猎请求 CmdId；仅改 Lua stub 无法依据 `HuntingGroupInfo` 正确选择线索，且会假报成功 | — | — | — | 实现狩猎控制器后接取委托并进入 `133001434` 等线索组，检查组加载生成与玩家当前委托匹配的线索实体；未实机。 |
| R1-146 | 已修（推断）；getter 读取同组状态，setter 写入已有组变量缓存，`0` 指当前组 | `c474d00f` | `ScriptLib.java` | `ScriptLibBlossomScheduleTest.zeroGroupIdReadsCurrentGroupAndExplicitIdKeepsStatesSeparate` | 在 `133001373` 地脉花组检查初态操作台选项 187；选择后状态 2 不再显示，奖励状态 3 保持；重登后检查状态恢复，实机待验。 |
| R1-154 | 跳过：当前 `SceneGroup` 不解析 `suite_disk`，`SceneSuite` 不表示带状态的 Flow Suite，`FlowSuiteOperatePolicy`、`SetFlowSuite`、`RemoveExtraFlowSuite` 等也缺失；单加入口不能执行原行为 | — | — | — | 补齐 Flow Suite 加载、策略与生命周期后，在 `111101014` 触发挑战，检查 suite 2 的实体出现及移除、suite 3 的状态切换；未实机。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "*ScriptLibReturnCodesTest" --tests "*PlatformPointArrayTest"`；`BUILD SUCCESSFUL`（3 项测试）。

测试命令：`.\gradlew.bat test --no-daemon --tests "*ScriptLibBlossomScheduleTest"`；`BUILD SUCCESSFUL`（1 项测试）。未部署，未进游戏。

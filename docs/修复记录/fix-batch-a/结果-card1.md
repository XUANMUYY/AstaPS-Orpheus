# 任务卡 1：任务内容条件枚举

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
|---|---|---|---|---|---|
| R1-061 | 已修 | `959db2c1` | `QuestContent.java`、`Inventory.java`、`QuestManager.java`、`ContentItemNumCompare.java`、`ContentItemNumEqual.java` | `ContentItemNumCompareTest` | 让子任务 4012810 处于进行中；物品 120596 数量分别为 2、3、4 时触发背包变化，核对只有恰好 3 个才满足该条件，且仍需同时满足 Lua 通知。 |
| R1-063 | 已修 | `7dfa0794` | `QuestContent.java`、`GameMainQuest.java`、`QuestManager.java`、`ContentParentQuestStateEqual.java` | `ContentParentQuestStateEqualTest` | 进行子任务 601714：父任务 6031 完成前不应满足状态条件，完成后应满足；另核对 60214 在父任务 70102 完成时触发失败条件。 |
| R1-073 | 已修 | `959db2c1` | `QuestContent.java`、`Inventory.java`、`QuestManager.java`、`ContentItemNumCompare.java`、`ContentItemNumGreater.java` | `ContentItemNumCompareTest` | 进行子任务 4016230：物品 121007 为 0 个时不满足，取得 1 个后满足；移除到 0 个后新接取的同类条件仍不满足。 |
| R1-089 | 跳过：缺少 `QuestRenameMiscReq` 的协议结构和接收处理器；只有 opcode，无法可靠识别 `param[0]=2` 的成功改名事件，须由协议/请求处理区域接入 | — | 无 | 无 | 补齐 7.1 改名请求处理后，在 7500007 完成对应 `misc` 改名，核对只在成功响应后推进。当前未进游戏验证。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.quest.content.ContentItemNumCompareTest" --tests "emu.grasscutter.game.quest.content.ContentParentQuestStateEqualTest"`。结果：`BUILD SUCCESSFUL`，4 个针对性用例通过；未部署、未做原生客户端验证。

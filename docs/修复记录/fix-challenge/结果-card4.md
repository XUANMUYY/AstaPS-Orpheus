# 任务卡 4：多阶段玩法通关条件

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-037 | 跳过：待实机；静态断链存在，但没有实机失败路径，活动旁路和结算语义未确认，单补条件处理器无法使流程可达 | 无 | 无 | 无 | 在允许的独立实机环境，通过活动入口分别进入机关棋谭 7001、棋盘 7400、绘想游迹式棋盘 7500、怪物对抗进攻 3820 和防守 3830；每组完成全部阶段，记录多阶段结算通知、副本通关界面及奖励/退出结果；再各做一次失败或主动结束，确认不会误通关。若活动入口不可进入，记录阻断点和对应副本 ID，不把静态结果算作实机验证。 |

静态依据：`DungeonPassExcelConfigData.json` 有 49 条该条件，映射 49 个副本（48 个 `DUNGEON_STATE_RELEASE`、1 个 `DUNGEON_STATE_TEST`）。当前仅 `DungeonPassConditionType` 枚举声明了 `DUNGEON_COND_END_MULTISTAGE_PLAY`；`pass_condition/` 无处理器，Java 中无该条件的 `triggerEvent` 调用；`ScriptLib`/`SceneScriptManager` 无 `InitSceneMultistagePlay`、`EndSceneMultiStagePlay` 实现，且无多阶段结算通知发送实现。`resources/Scripts/Common/V3_2/CharAmuse_Main.lua` 表明阶段结束还涉及脚本回调。资源的参数 `[玩法 ID, 999, 0/1]` 不能单靠静态表解释其成功/失败语义。未触碰运行服务、MongoDB、客户端和只读资源。

测试命令及结果：未运行 Gradle 测试；本条没有代码修改或新增测试。完成了只读源码搜索、49 条配置与副本映射核对。工作树保持干净；无本地提交。

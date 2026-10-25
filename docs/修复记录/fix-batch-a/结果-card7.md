| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
|---|---|---|---|---|---|
| R2-082 | 已修 | c4d57734 | PlayerActivityData.java、HandlerActivityTakeWatcherRewardReq.java、PacketActivityTakeWatcherRewardRsp.java；与 R2-086 共用开放判断 | ActivityRewardValidationTest：未完成、已领、无效 ID 与失败码 | 活动开放时用未完成观察器尝试领奖应无物品且回失败；完成后仅首次成功；关闭期间应失败。 |
| R2-083 | 已修 | 6a6bc224 | TrialAvatarActivityHandler.java、TrialAvatarPlayerData.java、HandlerReceivedTrialAvatarActivityRewardReq.java | TrialAvatarRewardTest：通关/已领判定与 detail 字段 | 在测试环境启用试用角色活动 5002：详情显示通关/领取标志；未通关不能领，通关后首次可领，再次请求不得增加物品。当前默认排期未启用 5002。 |
| R2-086 | 已修 | c4d57734 | ActivityConfigItem.java、ActivityManager.java、PacketActivityScheduleInfoNotify.java、PacketGetActivityInfoRsp.java；领奖入口同 R2-082 | ActivityRewardValidationTest：活动时间与领奖窗口、排期 isOpen | 用未来开始、开放中、已关闭三种测试排期登录：入口与详情只在开放中显示，关闭期间观察器不增加进度且领奖失败。 |
| R2-124 | 已修 | b3f00bcb | ScriptLoader.java | ScriptLoaderTest：实际加载 scene3_group133001080.lua 并执行 action_gadget_create 的局部 defs 访问 | 在海灯节跑酷场景触发机关 80092：出现选项 175，启动挑战并完成/重置；检查服务端不再出现该 action_gadget_create 的 nil 索引错误。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.activity.ActivityRewardValidationTest" --tests "emu.grasscutter.game.activity.trialavatar.TrialAvatarRewardTest" --tests "emu.grasscutter.scripts.ScriptLoaderTest"`
结果：编译通过，定向测试 10/10 通过；未部署、未做原生客户端验证。

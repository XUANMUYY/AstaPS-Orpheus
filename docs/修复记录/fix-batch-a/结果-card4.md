# 任务卡 4：每日委托奖励

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-009 | 已修 | `dda660b1` | `DailyTaskManager.java`、`HandlerTakeDailyTaskScoreRewardReq.java` | `DailyTaskScoreRewardClaimTest.java`：完成后未自动领取、请求发奖、重复请求、未完成拒绝、缺奖励配置或物品定义拒绝、库存拒收后重试 | 更新并部署后用今日未完成的角色完成四项委托：第四项结束时仅获得该项委托奖励，四委托奖励仍显示未领取；在客户端领取入口领取后，奖励只入账一次且界面变为已领取；重复操作或重登不得再次入账。此处尚未原生验收，NPC 对话入口由另一任务卡负责。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.server.packet.recv.DailyTaskScoreRewardClaimTest"`

结果：`BUILD SUCCESSFUL`；5 个测试通过。仅本地编译与单元测试，未部署或进游戏验证。

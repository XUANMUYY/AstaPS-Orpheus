# 任务卡 9：图鉴、成就、聊天

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
|---|---|---|---|---|---|
| R2-112 | 跳过：书籍已有 `ItemUseUnlockCodex` 解锁及持久化，景观仍无解锁入口；景观触发预计需改别卡负责的 `GadgetObject.onInteract`，且 7.1 缺 `ViewCodexReq` 消息定义，已读协议无法确认 | — | — | — | 使用书籍后重登检查书籍条目；景观交互和已读状态待协议及交互归属确认 |
| R2-113 | 跳过：资源确有 107 捕获、60 钓鱼条目，但服务端未定位到可信的捕获/钓鱼成功事件；不能把击杀或普通物品入包伪装成捕获 | — | — | — | 待捕获、钓鱼事件实现后分别成功获取一次并重登核对图鉴计数 |
| R2-114 | 已修：验证同组所有在用成就完成；混合未知/未完成 ID 整批拒绝，不写领取记录或发奖 | `3d625958` | `Achievements.java` | `AchievementGoalCompletionTest.java`（组完成判定、未完成拒领） | 用未完成成就组请求奖励应拒绝且道具不增加；完成全组后领取一次成功，再领拒绝；混合合法/未知 ID 不发奖 |
| R2-127 | 已修：无服务器聊天历史时回空 `PullRecentChatRsp` | `576cf8f8` | `ChatSystem.java` | `ChatSystemRecentChatTest.java`（空历史响应） | 关闭欢迎消息、清空会话历史后打开聊天页或重登，请求最近聊天应收到空响应而非一直等待 |
| R2-130 | 跳过：7.1 响应两条计数字段为混淆名 `GOIJKJBPKAF`(15) / `BMCNAMPBGAF`(10)，击杀/捕获顺序无可信映射；现存 `unlockedAnimal` 只存一类合并计数，填错会误报 | — | — | — | 待取得含已知击杀/捕获样本的原生响应，确认字段顺序后核对两种计数及重登持久化 |

测试：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.achievement.AchievementGoalCompletionTest" --tests "emu.grasscutter.game.chat.ChatSystemRecentChatTest"`，BUILD SUCCESSFUL（3 个针对性用例）；仅完成编译和合成测试，未部署、未做原生客户端验证。
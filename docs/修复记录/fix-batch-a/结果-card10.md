# 任务卡 10：尘歌壶

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R3-171 | 已修 | `26fc1fd5` | `GameHome.java`、`FurnitureManager.java` | `FurnitureMakeSlotTest` | 新家园不先查询制作状态就启动家具制作；信任 1 级队列可容纳 10 件、第 11 件拒绝，信任 10 级可容纳 15 件；多件的槽位索引应不同。 |
| R3-177 | 已修 | `bdcc8e58` | `GameHome.java` | `HomeComfortResourcesTest` | 在室内增加摆设，使室内外合计跨过产速档位；查看舒适度通知及下一小时洞天币、好感经验产速，切换洞天后应按最高洞天合计值计算。 |
| R3-178 | 已修 | `e86c0b43` | `FurnitureManager.java` | `FurnitureMakeClaimTest` | 开始制作后立即领取应收到未完成错误；使用仙速瓶可立即领，等待完成后领取不消耗仙速瓶。 |
| R3-179 | 已修 | `e86c0b43` | `GameHome.java`、`FurnitureManager.java` | `HomeFurnitureTrustTest` | 同一家具配方制作并领取两次，信任经验只在首次领取增加；另一种家具首次领取仍增加。 |
| R3-182 | 已修 | `6c0bc7f8` | `HandlerHomeChangeModuleReq.java` | `HomeChangeModuleValidationTest` | 对未解锁的模块发送 `HomeChangeModuleReq`，应返回 `RET_HOME_MODULE_NOT_UNLOCKED`，洞天不切换、不传送；已解锁模块仍可切换。 |
| R3-184 | 已修 | `1ccb23d3` | `HandlerGetHomeLevelUpRewardReq.java`、`PacketGetHomeLevelUpRewardRsp.java` | `HomeLevelRewardValidationTest` | 低信任等级请求高等级奖励或未知等级，应返回错误且背包、已领奖集合不变；达到对应等级后可正常领取一次。 |

测试命令（在 worktree 根目录，PowerShell）：

```powershell
.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.managers.FurnitureMakeSlotTest" --tests "emu.grasscutter.game.home.HomeComfortResourcesTest" --tests "emu.grasscutter.game.managers.FurnitureMakeClaimTest" --tests "emu.grasscutter.game.home.HomeFurnitureTrustTest" --tests "emu.grasscutter.server.packet.recv.HomeChangeModuleValidationTest" --tests "emu.grasscutter.server.packet.recv.HomeLevelRewardValidationTest"
```

结果：六项定向测试全部通过，`BUILD SUCCESSFUL`。未部署、未做原生客户端验证。

边界：7.1 等级表不含 `furnitureMakeSlotCount`；容量依[官方 4.6 公告](https://www.hoyolab.com/article/27598031)扩至 10、[Luna VI 公告](https://genshin.hoyoverse.com/en/news/detail/163454)中信任 10 级扩至 15 的说明推断，仍需用 7.1 客户端核对低等级队列展示。旧家园存档没有历史首次制作记录，新增记录只约束修复后领取的家具；无法可靠回填已制作但当前未持有的家具。

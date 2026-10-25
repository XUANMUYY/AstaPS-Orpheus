# 任务卡 8：商店与道具

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
|---|---|---|---|---|---|
| R2-090 | 已修 | `60598742` | `WeaponPromoteData.java`、`InventorySystem.java` | `WeaponPromoteDataTest` | 准备达到当前突破前武器等级的武器及足够材料；冒险等阶低于下一阶配方 `requiredPlayerLevel` 时突破应被拒且材料不扣，达到要求后应成功。 |
| R2-095 | 已修 | `94428cbd` | `ItemUseUnlockCodex.java`、`PlayerCodex.java`、`PacketCodexDataFullNotify.java` | `ItemUseUnlockCodexTest` | 使用物品 `100178`，书籍图鉴应解锁 `50001001`；重登后仍可见，重复使用不能再次消耗。 |
| R2-096 | 跳过：112 种箱子的默认 `ShopChest.v2.json` 仅 34 种有非空奖励、11 种为空、67 种无键；`resources/Server` 掉落表也只覆盖其中部分，无法为剩余箱子还原官方奖励，不能编造映射。需补齐客户端/官方服务端奖励来源。 | — | 无 | 无 | 补表后用物品 `115041` 等缺表箱子验证奖励、数量及消耗；本次未改行为。 |
| R2-100 | 已修 | `8e3f3c14` | `GameData.java`、`ReliquaryDecomposeConfigData.java`、`CombineManger.java` | `ReliquaryDecomposeConfigDataTest` | 用 3 件五星圣遗物分解应成功；混入低星、重复 GUID、少于 3 件或超过 39 件应拒绝且不消耗。 |
| R2-101 | 已修 | `c4e2b94f` | `HandlerGetShopBatchReq.java`、`HandlerGetShopmallDataReq.java`、`PacketGetShopBatchRsp.java`、`PacketGetShopmallDataRsp.java` | `PacketGetShopBatchRspTest` | 打开商城并触发批量查询；请求的每种店铺应收到与单店查询一致的商品列表，商城店铺类型列表应来自当前加载的商店数据。 |
| R2-102 | 已修 | `37b7067f` | `ForgeData.java`、`ForgingManager.java` | `ForgeRandomOutputTest` | 锻造配方 `12001` 后领取单份/全部队列产物；按服务端掉落 `208001500` 应得到 `104013×6`、`102×50`、`105×10`，不是只有展示物。 |
| R2-103 | 跳过：六条合成配方的 `dropId` 为 `22005000`/`22005100`，当前 `resources/Server/DropTableExcelConfigData.json` 和 `DropSubTableExcelConfigData.json` 均无这两个根；`randomItems` 也为空。无法确定奖励品种与权重，不能用 0 号物品或猜测替代。 | — | 无 | 无 | 获得对应掉落表后测试 `51001`–`51005`、`51011` 的材料扣除、随机奖励和回包；本次未改行为。 |

定向测试命令与结果（均在本工作树运行，均 `BUILD SUCCESSFUL`）：

- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.data.excels.weapon.WeaponPromoteDataTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.props.ItemUseAction.ItemUseUnlockCodexTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.data.excels.ReliquaryDecomposeConfigDataTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.server.packet.send.PacketGetShopBatchRspTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.managers.forging.ForgeRandomOutputTest"`

只完成源码构建与定向单测；未部署，未做国服 7.1 客户端实机验收。

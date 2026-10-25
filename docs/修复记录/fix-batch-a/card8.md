先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 8：商店与道具

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/systems/InventorySystem.java、物品使用动作目录、game/combine/、锻造管理器、server/packet/recv/HandlerGetShop*。

## 要修的条目

### R2-090 [商店/道具]武器突破忽略配方要求的玩家等级
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：WeaponPromoteExcelConfigData.json 1814 行中 1552 行有非零 requiredPlayerLevel；WeaponPromoteData.java:7-18,47-49 读取该字段，InventorySystem.java:544-591 的 promoteWeapon 只查武器等级、材料和摩拉，不查玩家等级。
- 说明：WeaponPromoteReq、WeaponUpgradeReq 和圣遗物升级请求均有非零包号处理器；武器普通强化 InventorySystem.java:340-397 从 WeaponPromoteData 及经验物品动作读取数据。

### R2-095 [商店/道具]图鉴解锁道具虽有处理器却固定失败
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：UNLOCK_CODEX 277 件（MaterialExcelConfigData.json；ItemUseAction.java:41-42）；ItemUseUnlockCodex.java:13-17 的 useItem 直接 return false。
- 说明：有枚举和动作类仍不等于可用。基础食物 ADD_SERVER_BUFF 995、复活 129、生命 29，经验书 ADD_EXP 3、武器经验 3，普通角色卡 131，随机箱 112、自选箱 14、选奖 13，元素能量 28+8 均有动作分派；这里只确认静态分派，不代表实机效果验收。

### R2-096 [商店/道具]随机礼包/箱子多数缺奖励映射
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：MaterialExcelConfigData.json 的 ITEM_USE_OPEN_RANDOM_CHEST 112 件有动作；其 useParam[0] 对照 src/main/resources/defaults/data/ShopChest.v2.json，仅 34 件映射到非空奖励，11 件命中空值，67 件无键。样本物品 115041 见 MaterialExcelConfigData.json:151546。ShopSystem.java:86-106 忽…
- 说明：工作树 data/ShopChest.v2.json 不存在，源码可复制默认文件（DataLoader.java:134-158）；实际部署可能另有自定义表，未读取部署态。

### R2-100 [商店/道具]圣遗物分解未执行 Excel 的等级/数量约束
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：ReliquaryDecomposeExcelConfigData.json 40 行含 needReliquaryRankLevel、needReliquaryCount、maxReliquaryNum（:3-7）；data/ReliquaryDecompose.json 46 行覆盖全部 40 个 Excel id。CombineManger.java:28-39,133-170 读取自定义掉落表，只检查 input.size()==count3 和 GUID 存在，不…
- 说明：HandlerReliquaryDecomposeReq.java:7-14 已收包；本项是规则不一致，不是“分解请求无处理器”。

### R2-101 [商店/道具]商店批量查询与商城数据回空
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：HandlerGetShopBatchReq.java:6-12 只发空 GetShopBatchRsp，日志 latest.log:403157 起 19 次有处理器；HandlerGetShopmallDataReq.java:7-13 标 TODO 并发默认应答，日志 latest.log:2584915 起 8 次。
- 说明：GetShopReq 在日志 43 次有处理器；这些次数只能证明请求到达，不证明界面结果。不能将批量查询等同于已实现整店刷新。

### R2-102 [商店/道具]锻造一条随机产出配方退化成固定展示物
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：ForgeExcelConfigData.json 61 行的 materialItems 均有有效材料；id 12001 有 mainRandomDropId=208001500 而无 resultItemId；ForgeRandomExcelConfigData.json 11 行无服务端资源类。ForgingManager.java:182-184,244-246 对缺产物 ID 改用 showItemId，不抽随机掉落。
- 说明：其余普通锻造配方 ForgeData.java:13-27 字段对应；不能说锻造全空。随机锻造最终奖励与资源表不一致。

### R2-103 [商店/道具]合成掉落型六条配方会走 0 号产物
- 影响：功能缺失；工作量：中；第三轮核实：仍存在
- 证据：CombineExcelConfigData.json 593 行的 materialItems 均有有效材料；其中 combineId 51001–51005、51011 六行用 dropId 而没有 resultItemId。CombineData.java:12-23 虽存 randomItems，CombineManger.java:99-113 只用 resultItemId 和 resultItemCount 发物品，未取 dropId。
- 说明：普通 587 行的字段名与读取类对应，不能概括为“合成配方全空”。六条掉落型配方的随机奖励未实现；具体客户端是否开放这些配方待测。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card8.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
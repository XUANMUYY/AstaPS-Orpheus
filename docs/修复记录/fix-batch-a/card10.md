先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix-a/共用说明.md，遵守其中全部规则。

# 任务卡 10：尘歌壶

建议先看（路径相对 src/main/java/emu/grasscutter/，以实际代码为准）：game/home/、FurnitureManager.java、server/packet/recv/HandlerHome*、HandlerGetHomeLevelUpRewardReq.java。

## 要修的条目

### R3-171 [家园]家具制作启动恒判槽位满
- 影响：卡住流程；工作量：中；第三轮核实：待修
- 证据：4 处；FurnitureManager.java:53-68,103-108；HomeWorldLevelData.java:10-21；7.1 HomeworldLevelExcelConfigData.json 10 级均无 furnitureMakeSlotCount 字段，Java 值恒为 0；PacketOpcodes.java:1929,1945 请求包号非零
- 说明：失败路径：先发 FurnitureMakeReq(2299) 初始化空制作列表，再发 FurnitureMakeStartReq(22221)；0 <= 0 成立，返回 RET_FURNITURE_MAKE_SLOT_FULL，玩家无法制作家具。若跳过查询直接启动，列表初始为 null，还可能在 .size() 抛异常。未编译或实机复现，静态分支确定。

### R3-177 [家园]室内舒适度未计入洞天币与好感经验产速
- 影响：功能缺失；工作量：中；第三轮核实：待修
- 证据：3 处；PacketHomeComfortInfoNotify.java:22-33 同时下发室外分区及室内舒适度；GameHome.java:503-539 却仅用室外 homeScene.calComfort() 选择每小时 4～30 洞天币、2～5 好感经验；HomeWorldComfortLevelExcelConfigData.json 10 档与代码阈值对应
- 说明：失败路径：室内增加摆设后通知里的 roomSceneComfortValue 增加，但登录/小时更新走 getComfortResources，未读取 getMainHouseItem(...).calComfort()；若合计舒适度跨档，资源仍按较低室外档累积。具体官方合计规则尚需实机对照，但服务端通知与计产值不一致是确定的。

### R3-178 [家园]家具制作完成时间未校验，可立即领取
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：3 处；FurnitureManager.java:111-155，beginTime + durTime 的校验在 :146-151 整段被注释，随后直接发家具；FurnitureMakeSlotItem.java:10-25 存时间
- 说明：失败路径：已有制作槽时立即发 TakeFurnitureMakeReq(4563) 且 isFastFinish=false → 直接领家具，无等待或加速道具。当前新建账号先受“槽位恒满”阻断；已有槽位存档或修复前项后可触达。

### R3-179 [家园]重复制作同配方重复发信任经验
- 影响：功能缺失；工作量：中；第三轮核实：待修
- 证据：3 处；FurnitureManager.java:154-159 每次领取均 Inventory.addItem(121, makeData.getExp())；Inventory.java:627-629 将 121 转入 GameHome.addExp；未见首次制作判定，虽有已解锁公式集合
- 说明：失败路径：同一配方多次制作并领取 → 每次获得完整家园经验、家园等级提前增长；同受当前槽位问题阻断。官方“首次制作”语义需实机/资源核对，代码中的注释明确承认首次判定未接。

### R3-182 [家园]切换洞天不校验是否已解锁
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：3 处；HandlerHomeChangeModuleReq.java:19-49 仅检查访客与场景存在，未查 Player.realmList；HomeWorld.java:80-90 依场景 ID 获取；7.1 模块和场景表存在第 4～8 洞天
- 说明：失败路径：发送 HomeChangeModuleReq 指向未在 realmList 的模块 → 服务端更新 currentRealmId、回成功并传送，玩家可进入未解锁洞天。原生 UI 一般不提供该入口，属于服务端请求校验问题。

### R3-184 [家园]家园等级奖励不核当前等级
- 影响：功能缺失；工作量：小；第三轮核实：待修
- 证据：2 处；HandlerGetHomeLevelUpRewardReq.java:18-35 只查重，直接查任意表等级的奖励并发物品；GameHome.java:52,337-348 有当前等级
- 说明：失败路径：低等级账号发送 GetHomeLevelUpRewardReq 指向存在的更高等级 → 奖励照发并写已领奖集合；服务端没有 req.level <= home.level 检查。原生 UI 未必会主动发送，属请求篡改可见问题；未知等级还可能空指针。

## 输出
- 每个条目一个本地提交（同一文件里紧密相关的可合并一个），提交信息写明条目编号。
- 结果写到 out/card10.md（相对任务卡目录）：每个条目一行——状态（已修 / main 上已不存在 / 跳过及原因）、改了哪些文件、加了哪些测试、进游戏怎么验证。
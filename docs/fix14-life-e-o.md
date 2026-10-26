# fix14-life-e 汇报

代码提交：`ff49891f`（`fix14-life-e`）。

- R5-150：已修。`isForbidGuest` 为真且交互者不是场景世界房主时，回 `RET_GADGET_INTERACT_COND_NOT_MEET`；不发物品、不记交互任务。房主和未禁访客的采集照常成功。
- R5-158：已修。按 `CookRecipeData.maxProficiency` 判定质量 0 的自动烹饪资格；未满回 `RET_RECIPE_NOT_AUTO_QTE`，扣材料前结束。依据是该配方字段与 7.1 专用 retcode。
- R5-044：未修。`PacketOpcodes` 虽有 `WidgetCaptureAnimalReq`、`FishBattleEndReq` 包号，但 7.1 生成协议没有对应消息定义；当前也无捕获或钓鱼成功结算处理器。不能凭包号编造请求、成功判据或奖励。
- R5-045：未修。`QueryCodexMonsterBeKilledNumRsp` 的两个计数数组只有混淆名 `BMCNAMPBGAF`（字段 10）与 `GOIJKJBPKAF`（字段 15），没有可靠的击杀/捕获映射；现存 `unlockedAnimal` 也只有一份计数。保留只填 ID 并在代码中标明原因。

验证：`./gradlew.bat --no-daemon compileJava` 成功；定向运行 `GatherGuestGuardTest`、`CookingManagerAutoQteTest`，共 4 例通过。未跑全量测试、服务端或原生客户端。

改过的已有文件：`GadgetGatherObject.java`、`GadgetGatherPoint.java`、`CookingManager.java`、`PacketGadgetInteractRsp.java`、`PacketQueryCodexMonsterBeKilledNumRsp.java`。

进游戏复测：
- R5-150：双人联机去蒙德摘星崖采「塞西莉亚花」，访客交互应失败且背包不增加，房主可采；另选未标记禁访客的普通采集物确认访客仍可采。
- R5-158：在蒙德城烹饪锅用「甜甜花酿鸡」测未满熟练度的手动烹饪；测试请求质量 0 应返回 620 且材料不减；满熟练度后自动烹饪应正常出菜。
- R5-044：待补齐 7.1 协议与结算后，在野外用「四方八方之网」捕获动物、在钓鱼点钓「花鳉」，分别核对图鉴次数；当前不能作为通过项。
- R5-045：待确认协议字段映射后，击杀「丘丘人」并打开「图鉴·生物志」核对击杀次数，捕获动物再核对捕获次数；当前不能作为通过项。

# 任务卡 B：地脉花领奖持久化与缺掉落根的宝箱

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R5-014 | 部分修：未领脚本花的 config ID 写入现有 `SceneGroupInstance`，组重载且 `GROUP_LOAD` 完成后重建并注册，领完清记录；组 111101088 的奖励来源仍缺，创建可能先在 `registerScriptedChest` 失败 | `64ef0c8b` | `SceneGroupInstance.java`、`ScriptLib.java`、`BlossomManager.java`、`Scene.java` | `BlossomScriptedChestTest.pendingFlowerOutsideSuitesIsRebuiltAndCanBeCleared` | 须弥地脉花图标附近、组 133001373（1694,264,-1319）：完成战斗，领奖前退出重登，确认 373006 重现且可正常领取，再重登确认不重复；蒙德雪山活动营地、组 111101088（2677,194,-1745）核对 88010 创建是否先失败。 |
| R5-023 | 已修保护路径：场景明确给出 `chest_drop_id` 而原生根缺失时，回 `RET_SVR_ERROR_VALUE`，不标已领、不改开启状态；1050269 的实际奖励仍缺 | `d67e2b04` | `DropSystem.java`、`GadgetChest.java` | `GadgetChestNativeDropTest.authoredDropWithoutRootMustBeRefusedBeforeClaim` | 须弥沙漠区域 32 的符文装置群、组 133307177（-1311,-3,5423），尝试开启 177007/70330386：应收到失败响应且箱子保持可重试；补齐可信根后再验实际奖励。 |

定向测试：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.managers.blossom.BlossomScriptedChestTest" --tests "emu.grasscutter.game.entity.gadget.GadgetChestNativeDropTest"`，两次均 `BUILD SUCCESSFUL`；最终代码测试通过，`git diff --check` 通过，工作树干净。测试夹具未初始化 Lua globals，打印脚本错误日志，但相关断言通过；未连接 MongoDB，未部署或实机验证。

静态边界：`resources/ExcelBinOutput/BlossomGroupsExcelConfigData.json` 含 133001373、不含 111101088；后者 Lua 的 88010 是脚本奖励花，组内没有原生金/蓝地脉源机关，`data/Spawns.json` 也没有该组，不能凭空确定树脂奖励类型。`data/ChestDrop.json` 未找到 1050269；本 worktree 的只读 `resources/ExcelBinOutput/` 未提供 `DropTableExcelConfigData.json` / `DropSubTableExcelConfigData.json` 的可信 1050269 根。其余 17 处未逐一证明奖励来源相同。未修改只读资源或玩家存档。

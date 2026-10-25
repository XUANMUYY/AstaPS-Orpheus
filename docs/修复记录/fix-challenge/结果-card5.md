# 任务卡 5：地脉花脚本函数与进度事件

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-022 | 已不存在：当前 `HandlerClientScriptEventNotify` 把客户端 `eventType` 原样交给 `SceneScriptManager.callEvent`，可转交 39 号地脉完成事件；无须补显式生产者。尚未确认客户端在各场景实际发送该事件。 | — | — | — | 场景 1 组 111101088：完成地脉战斗，确认 `EVENT_BLOSSOM_PROGRESS_FINISH` 后 88010 宝箱出现；同时抓客户端事件类型 39。 |
| R1-147 | 已修：`RefreshBlossomGroup` 解析当前组、suite、`exclude_prev`，执行组刷新并检查结果；`is_delay_unload` 暂沿即时替换，待实机校准。 | `1e341a4c` | `ScriptLib.java` | `ScriptLibRefreshBlossomGroupTest` | 场景 1 组 111101077：靠近触发 GROUP_LOAD，确认刷新到 suite 2；完成后再加载该组，确认目标套件和触发器。 |
| R1-151 | 已修：按 `BlossomGroupsExcelConfigData.finish_progress` 累加，达到阈值后只发一次完成事件；未列入该表的旧组按一次完成（推断）。 | `1b42033c` | `BlossomCampData.java`、`ScriptLib.java` | `ScriptLibBlossomProgressTest` | 场景 3 组 133001373：启动战斗，击杀达到原表阈值 7，确认完成事件只触发一次；场景 1 组 111101077 摧毁机关后确认进度结束。 |
| R1-163 | 已修：从当前选中的原生地脉营地机关返回刷新类型，摩拉为 1、经验为 3；无活动营地返回 0。 | `0a3dff1d` | `BlossomManager.java`、`ScriptLib.java` | `BlossomRefreshTypeTest` | 场景 3 组 133001373：刷新该组，确认 GROUP_REFRESH 创建对应摩拉/经验操作台 373004/373005，且阶段设置继续执行。 |
| R1-139 | 已修：刷新时重新发布当前地脉奖励预览；奖励仍由现有 `BlossomManager.claimReward` 在领取时按类型、世界等级和树脂消耗掷取，不提前固定全队掉落。实机奖励正确性待验。 | `4a0f9cb2` | `ScriptLib.java` | `ScriptLibBlossomRewardRefreshTest` | 场景 1 组 111101088：刷新组、启动地脉，确认奖励预览更新；领取后核对树脂扣除与摩拉/经验奖励。 |
| R1-140 | 已不存在：基线 `c474d00f`（R1-146）已有 `SetBlossomScheduleStateByGroupId` 与对应读取，使用组变量保存 0–3 阶段；本卡 R1-151 仅增加阶段 1 的进度重置。 | `c474d00f`（基线） | — | 既有 `ScriptLibBlossomScheduleTest`；本卡新增 `ScriptLibBlossomProgressTest` 覆盖重置 | 场景 3 组 133001373：刷新、启动、完成战斗，核对阶段依次 1→2→3；离开并重新进入检查阶段。 |
| R1-162 | 已修：脚本宝箱按 Lua 配置创建，绑定现有地脉树脂领奖入口，领取结束转交 `EVENT_BLOSSOM_CHEST_DIE`；重复创建不再重复注册。实机及重登后宝箱恢复待验。 | `4a0f9cb2` | `ScriptLib.java`、`SceneGadget.java`、`BlossomManager.java`、`BlossomActivity.java`、`GadgetChest.java` | `BlossomScriptedChestTest` | 场景 1 组 111101088：完成挑战，确认 88010 宝箱出现，使用树脂领取且只发一次奖励；场景 3 组 133001373 同法核对 373006 和领取后下一营地刷新。 |

定向测试命令（均 `BUILD SUCCESSFUL`）：

- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.scripts.ScriptLibRefreshBlossomGroupTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.managers.blossom.BlossomRefreshTypeTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.scripts.ScriptLibBlossomProgressTest" --tests "emu.grasscutter.game.entity.ScriptLibBlossomScheduleTest"`
- `.\gradlew.bat test --no-daemon --tests "emu.grasscutter.scripts.ScriptLibBlossomRewardRefreshTest" --tests "emu.grasscutter.game.managers.blossom.BlossomScriptedChestTest"`

仅编译和定向单元测试通过；未部署、未进实机。`BlossomScriptedChestTest` 的轻量场景夹具会打印未初始化 Lua globals 的日志，但测试本身通过且不执行真实 Lua；需按上表完成原生客户端验收。未修改只读资源或运行中服务。

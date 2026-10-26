# 任务卡 C：画廊和镜头通知

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R5-015 | 部分修 | `f5a66c62` | `ScriptLib.java`、`GalleryScriptState.java`、`ScriptLibGalleryTest.java` | 画廊目标坐标写入、停止清理与异常场景断言 | 稻妻清籁岛浅濑神社附近接每日委托「猫之迹」，与寝子对话完成 2204101 后跟随找猫；场景 3 组 133217135。当前只能用服务端测试核验状态；距离提示仍不会弹出。 |
| R5-042 | 跳过 | — | 无 | 无（没有可发送的 7.1 消息结构） | 蒙德场景 1 的「解谜高级蒙德」机关群（约 2669,265,-1362），组 111101180；脚本未给出关联任务或附近锚点。触发后镜头仍不会按 Lua 注目。 |

## 处理与阻塞

- R5-015：基线 `StartGallery` 只记录开始时间，`StopGallery` 和 `SetHandballGalleryBallPosAndRot` 未实现。现已保存 Lua 从六个目标 gadget 取得的位置，停止时清除开始时间、分数和目标。`GalleryExcelConfigData` 的 9001 行只有类型、场景和文本等信息，没有可直接使用的结束时间或目标位置。
- `protocol/7.1/protocol.desc`、`compat/` 和重新生成的 Java 均无具名 `GalleryStartNotify`、`GalleryStopNotify` 及其字段号；`PacketOpcodes.java` 虽有包号，不能据此猜测消息体。因此两种通知未发，客户端距离提示缺口仍在。需恢复 7.1 消息字段、目标位置和时间语义后续接入。
- R5-042：Lua 的实际参数包括 `look_pos`、`duration`、`is_force`、`is_broadcast` 等；`PacketBeginCameraSceneLookNotify` 当前构包代码全部注释。7.1 描述集、`compat/` 和生成类均无具名 `BeginCameraSceneLookNotify`，无法核实字段号或现有发送类映射，故没有发送。`EndCameraSceneLookNotify` 的 CmdId 标为 unknown，也未发送。现有入口仍回 0，以免截断后续 Lua 动作。

## 验证

- 命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.scripts.ScriptLibGalleryTest"`
- 结果：`BUILD SUCCESSFUL`；2 个用例、0 失败、0 错误。仅完成编译与定向单测；未部署、未进游戏验证。
- `git diff --check` 通过；提交后工作树干净。未推送、未合并。

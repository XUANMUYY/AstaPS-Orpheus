先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/共用说明.md，遵守其中全部规则。

# 任务卡 C：画廊和镜头通知

- 工作目录（本卡专用 worktree）：`F:/Fork Reversed-worktrees/Genshin/fix14-script-c`，分支 `fix14-script-c`。只在这里改和提交。
- 结果文件：`C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/out/cardc.md`

## 要修的条目

### R5-015 未实现 StartGallery
- 影响：只是提示不弹；工作量：大；第四轮编号 R4-064
- 证据：B/22041.json:1-12,122；S/Scene/3/scene3_group133217135.lua:422-436；J/scripts/ScriptLib.java:1794-1803；J/net/packet/PacketOpcodes.java:1976
- 失败路径和建议：每日委托 22041 完成 2204101 时，场景 3 组 133217135 的 EVENT_QUEST_FINISH 六处调用 StartGallery(9001)；ScriptLib 只存 GalleryScriptState，没有发送 GalleryStartNotify，客户端缺目标距离画廊提示。修：补齐 7.1 Gallery 通知结构与 StartGallery/StopGallery 发包，并按任务目标填目标位置。

### R5-042 未实现 BeginCameraSceneLook
- 影响：功能缺失；工作量：大；第四轮编号 R4-077
- 证据：R/Scripts/Scene/1/scene1_group111101180.lua:255；J/scripts/ScriptLib.java:452-457；J/server/packet/send/PacketBeginCameraSceneLookNotify.java:8-16
- 失败路径和建议：剧情组 Lua 调镜头函数；ScriptLib 明确只记日志回 0，现有发送类没有被此入口调用，镜头通知不发，客户端镜头不按脚本转向。建议：取得 7.1 通知结构后，在 ScriptLib.BeginCameraSceneLook 构包并向场景玩家发送。

## 本卡要点
- R5-015：StartGallery/StopGallery 要发 GalleryStartNotify/GalleryStopNotify。先在 protocol/7.1 里核实这两个消息的字段号；能核实就发包（填 gallery_id、开始时间、结束时间、目标位置等 7.1 有的字段，数据来自 GalleryExcelConfigData 和场景）；核实不了就只做能做的部分并写清缺什么。
- R5-042：BeginCameraSceneLook 用现有的 PacketBeginCameraSceneLookNotify 发给场景内玩家，按 Lua 传入的参数表（look_pos、duration、is_force、is_broadcast 等，以 Lua 实际写法为准）填字段；先核实 7.1 字段号和现有发送类是否一致。EndCameraSceneLookNotify 的包号标着「unknown」，不要发它。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果按共用说明的格式写到结果文件。

先读 C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/共用说明.md，遵守其中全部规则。

# 任务卡 B：地脉花领奖持久化与缺掉落根的宝箱

- 工作目录（本卡专用 worktree）：`F:/Fork Reversed-worktrees/Genshin/fix14-script-b`，分支 `fix14-script-b`。只在这里改和提交。
- 结果文件：`C:/Users/23988/AppData/Local/Temp/claude/F--Fork-Reversed-Punishing-Gray-Raven/0ec28137-7b28-4f8d-b272-e75b80710c71/scratchpad/fix14/out/cardb.md`

## 要修的条目

### R5-014 未实现 CreateBlossomChestByGroupId
- 影响：卡住流程；工作量：大；第四轮编号 R4-060
- 证据：S/Scene/1/scene1_group111101088.lua:105-121,240-270；J/scripts/ScriptLib.java:580-596；J/game/managers/blossom/BlossomManager.java:535-573
- 失败路径和建议：世界地脉组 111101088 的 BLOSSOM_PROGRESS_FINISH 动态造 88010 并注册领奖；玩家未领即退出重登，宝箱既不在 suite 1/2，也无持久化记录，GROUP_LOAD 仅刷新 suite 2，客户端重入看不到领奖花。修：在 BlossomManager 持久化未领脚本宝箱/阶段；组加载时按记录调用 registerScriptedChest 重建，再处理已领清理。

### R5-023 [能力]场景 chest_drop_id 有 18 处指向缺失原生根
- 影响：功能缺失；工作量：中；第四轮编号 R4-100
- 证据：S/Scene/3/scene3_block3307.lua:179、scene3_group133307177.lua:28；E/GadgetExcelConfigData.json:139755-139763；J/game/entity/gadget/GadgetChest.java:311-345,419-439
- 失败路径和建议：场景 3/区域 32 的组 133307177 静态宝箱 177007/70330386 标 chest_drop_id=1050269；根在两张服务端掉落表均不存在。玩家解锁区域后开箱，GadgetChest 的无奖励分支仍标已领、更新开箱状态并回成功，客户端失去一次性奖励。只证实此例，未把其余 17 处等同处理。修：补 1050269 的可信掉落根或逐箱来源映射；缺根时不得消耗箱子，须回可重试错误。

## 本卡要点
- R5-014：未领的脚本地脉宝箱在退出重登、组重新加载后要能重建。持久化放在现有的组/场景持久化里（例如 SceneGroupInstance 或 BlossomManager 已有的存档字段），不要新建数据库集合；领过后清掉记录。看 fix-challenge 的 card4/card5 结果了解当时的设计。
- R5-023：chest_drop_id 指向的掉落根在掉落表里不存在时，`GadgetChest` 不能把箱子标成已领、不能改开箱状态，要回一个可重试的错误码（看现有 retcode 用法）。不要编造 1050269 的掉落内容；如果在 `resources/` 或 `data/` 里能找到可信来源（例如同名掉落在别的表、或 ChestDrop.json），在结果里写明来源，但仍不改 resources。

## 输出
- 每个条目一个本地提交（紧密相关的可合并），提交信息写明条目编号。
- 结果按共用说明的格式写到结果文件。

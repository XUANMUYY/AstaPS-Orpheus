# 任务调整 NOTIFY_GROUP_LUA 审计

审计日期：2026-10-25。范围是指定的 **14 个子任务、16 条通知、12 个目标 group**；只读真实资源与当前源码，未执行 Lua、未部署、未验证原生画面。

## 调用路径与判断口径

`QuestSystem.triggerExec → ReplayContext.dispatch → ExecNotifyGroupLua.execute`：参数为 scene、group，捕获派发时任务状态，FINISHED 对应 `EVENT_QUEST_FINISH`（param2=1），其余对应 `EVENT_QUEST_START`。先等脚本初始化、玩家进入目标场景和场景加载完成，再注册目标 group 并 `scriptManager.callEvent`。事件 source 为子任务编号。

`SceneScriptManager.callEvent → realCallEvent → handleEventForTrigger`：运行在独立的四线程 Lua 事件池中；按事件类型、目标 group 和 source 筛选当前 suite 的 trigger，再计算 condition 并运行 action。不能只看 action 是否调用 RefreshGroup：suite 中的 monsters、区域、GADGET_CREATE / VARIABLE_CHANGE、定时器和后续回调都要看。`QuestManager.eventExecutor` 的屏障本身不能证明这些 Lua 队列和定时器已结束；C4因此增加了对独立Lua池和本次已通知组定时器的等待，超时隔离上下文。

本组变量、机关状态、无战斗的 suite/机关创建可以放行。脚本会在玩家后续交互后刷怪的，区分本次回调是否立即或通过新激活区域开启战斗。跨 group 修改、直接刷怪/战斗及未实现 API 保留阻断。所有路径的任务完成仍受本次新增的重放边界约束，不能靠 Lua 通知完成计划外子任务。

## 逐项结论

路径统一为 `resources/Scripts/Scene/3/scene3_group<group>.lua`。39004 有三条通知，合为一行核对。

| 子任务 / 阶段 | group | 回调及可达副作用 | 结论 |
|---|---|---|---|
| 308/30803 finish | 133001247 | `QUEST_FINISH_247006` 将本组 `allow_spawn=1`；没有 VARIABLE_CHANGE 刷怪回调。玩家之后进入区域247001才会加suite2生成4只史莱姆，本次通知本身不刷怪 | 放行；注明后续区域交互 |
| 354/35404 begin | 133003439 | `QUEST_START_539` 创建本组1秒 `born` 定时器，`TIMER_EVENT_542` 创建机关3834；机关被玩家打碎才通知133003079并创建宝箱4115。无怪物、挑战、跨组写入 | 放行；异步机关创建不能视为即时完成证据 |
| 380/38001 finish | 133003151 | `QUEST_FINISH_124` RefreshGroup到suite2，其中明确包含怪物620（遗迹守卫24010101） | **阻断：刷怪战斗** |
| 380/38003 finish | 133003151 | `QUEST_FINISH_464` RefreshGroup到suite1，该suite仍包含怪物620，会重新生成遗迹守卫 | **阻断：重刷怪物** |
| 388/38803 finish | 133007001 | `QUEST_FINISH_27` KillEntityByConfigId永久关闭本组风墙机关191 | 放行 |
| 389/38905 begin | 133007227 | `QUEST_START_308` 加本组suite2（机关974）、Quest_Flag=1；GADGET_CREATE同步模型状态，可能创建本组光柱。机关达到Action03才通知任务、开本组特效定时器，无战斗 | 放行 |
| 390/39003 begin | 133007227 | `QUEST_START_227001` 同上，加本组suite2和Quest_Flag=1，无怪物、挑战或跨组修改 | 放行 |
| 390/39004 finish | 133007228、133007229、133007230 | `QUEST_FINISH_310/312/315` 各加本组suite2与Quest_Flag=1；前两组是本组光柱/机关；第三组suite2激活区域230001，首次进入会加suite3的怪物422/423/424并StartSealBattle（含kill_time限时） | **整对子任务阻断：第三组启用战斗区域；不能只放行前两条** |
| 390/39005 finish | 133007052 | `QUEST_FINISH_28` 将本组dvalin_progress加1；VARIABLE_CHANGE安排本组3秒/7秒定时器，仅更新提示机关状态、创建封印光效 | 放行 |
| 390/39006 finish | 133007052 | `QUEST_FINISH_29` 同上；无刷怪、挑战或跨组写入 | 放行 |
| 390/39007 finish | 133007052 | `QUEST_FINISH_30` 同上；进度3时安排7秒光效定时器 | 放行 |
| 393/39301 finish | 133007029 | `QUEST_FINISH_22` 调用UnlockScenePoint(context,38)；当前ScriptLib及脚本绑定源码中无此API，不能把返回/派发成功当作场景点已解锁 | **阻断：未实现API，存档效果缺失** |
| 394/39403 finish | 133007183 | `QUEST_FINISH_185` 永久删除133007004/7、133007076/300、133007078/304、133007079/306、133007001/1；KillEntityByConfigId支持显式目标group并记录销毁 | **阻断：修改其他5个group** |
| 398/39812 begin | 133003910 | `QUEST_START_910037` AddQuestProgress("133003910")；已实现，向本场景玩家增加该键并排队LUA_NOTIFY。没有传送、刷怪、挑战、跨group API；任务完成由重放边界约束 | 放行；这是任务通知，不是纯画面 |

共 **放行9个子任务（9条通知），阻断5个子任务（7条通知）**。本次直接通知链未发现传送玩家；危险战斗来自38001/38003的含怪suite及39004的封印战区域。39301的点38解锁不做替代补丁，保持阻断等待通用API实现。

空白新号试算额外遇到两处，进一步只读审计后也保留阻断（测试号2已真实完成它们，不代表新号可以跳过）：

| 子任务 / 阶段 | group | 回调及副作用 | 结论 |
|---|---|---|---|
| 353/35303 begin | 133003448 | QUEST_START_544延迟2秒CreateMonster(440/441)；ANY_MONSTER_LIVE_533向另一组133003002创建delay2定时器 | **阻断：刷怪战斗、跨group定时器** |
| 353/35304 begin | 133003449 | QUEST_START_545延迟2秒CreateMonster(442/443/444/445)；ANY_MONSTER_LIVE_535向另一组133003002创建delay3定时器 | **阻断：刷怪战斗、跨group定时器** |

连同补充两处，已审计总数为16个子任务，放行9个、阻断7个；不混用这个数字与原任务卡14处的9/5结论。

## 配置与执行边界

`exec-policy.json` 保持原 `types` 格式；NOTIFY_GROUP_LUA 的类型策略改为 handled，新增可选 `exceptions`（type、subId、action、note）覆盖类型策略，规划与实际dispatch共用检查。审计阻断例外不能被按类型的临时override绕过。

额外的可选 `auditedSubIds` 限制已审计范围。QuestExcel单表统计有2338个带begin/finish通知的子任务，按引擎规则补入BinOutput字段后共2340个；已审计上述16个（原14个加新号补充2个），其他子任务仍以“尚未审计”阻断，不能据此放行全部Lua。旧策略不含这两个字段时照常加载。

本报告属于静态副作用审计。handler返回true只代表安排派发；新增等待覆盖独立事件池及已通知组的1/3/7秒定时器，但Lua条件不匹配、组尚未加载或Lua异常仍不能据此视为世界效果成功。C4不把dry-run零阻断说成世界状态已重放完成；后续部署前仍需隔离环境核对journal、group持久化和异步收尾。资源或API实现变化后需要重新审计。

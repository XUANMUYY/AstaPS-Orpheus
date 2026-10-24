# C5b：选择性目标、后台流程与离线验证

本批只修改任务调整包、相关测试、任务调整数据和分析工具、console.html 以及 WebConsoleHandler 的 quest 接口。不部署、不启停服务、不连接数据库、不调用 apply/undo。交付的类别与示例试算全部使用 new / UID 0 占位，不绑定任何账号。个人号仍先不跳。

## 目标格式 v2（兼容 v1）

| 字段 | 含义 |
|---|---|
| version | 1 或 2；旧字段仍可用，建议新文件用 2 |
| source | 核对说明，不作为执行授权 |
| completeChapters / completeParents / completeSubs / archonThrough | 保持 v1 语义；archonThrough 按资源 tag 取魔神章节前缀 |
| include | 选择器数组，各选择器取并集；与旧字段选中内容合并 |
| exclude | 同格式选择器数组，选中主任务从目标中排除；若前置闭包仍需要完成它们，则 excluded_dependency 阻断，不静默越界 |
| stopBefore | 只能包含 chapter、parent、sub 之一；章节/主任务支持 ID 或精确中文名，子任务仅正整数 ID |

选择器支持 `regions`（地区中文名）、`categories`（类别中文名）、`chapters`（章节引用）、`parents`（主任务引用）、`expansions`（扩展区域中文名），每个字段都是非空数组。同一维度取并集，各维度取交集。章节引用支持数字、中文名、`{"id":1101}`、`{"chapterTitleZh":"浮世浮生千岩间"}`；主任务支持数字、中文名、`{"id":375}`、`{"titleZh":"温迪的新计划"}`。同名不猜，返回候选；未知维度、类别、地区和空目标报错。

每日委托以 `DailyTaskExcelConfigData.questId` 映射 `QuestExcelConfigData.mainId`，当前命中 257 个主任务（256 个 IQ、1 个 WQ）；不能把所有 IQ 都标成每日委托。直接主任务、子任务、章节选择、类别选择和排除项出现每日委托均拒绝；委托如果只作为前置闭包出现，也会阻断。

类别配置见 classification-rules.json，完整目录和证据见 classification.json。魔神由 AQ 章节样式识别，传说由 LQ 且排除 COOP 章节识别，世界由 WQ 识别，活动由 EQ 或显式 activityId 识别，邀约由 COOP 章节样式识别。类别允许交叠，例如活动关联的世界任务。

限时/下线传说要求 LQ 非邀约且有章节时间或明确活动关联。当前资源没有符合该规则的条目；保留类别接口，但零目标按 selection_unresolved 阻断，不能宣称已覆盖全部历史限时传说，也不根据当前日期臆断官服下线状态。

扩展区域按 CityConfigData.areaIdVec、WorldAreaConfigData 的场景/区域层级、任务 guide、场景组 area、ShareConfig 场景/坐标和 scene3_worldArea 多边形推导；详细区域/场景编号均在配置中，不在 Java 中排列任务 ID。涉及扩展区域的跨区主任务也纳入对应扩展区域。须弥 area 32 的正式中文区域名尚待核实，单列而不猜名。排除扩展区域时，无足够区域证据的世界任务保守省略，并在 omittedParents 列出。

classification.json 记录 8,486 个输入文件 SHA256。加载时校验资源、admin-catalog 和规则文件，并使用传入的资源根目录；生成脚本、补充名称文档仅作溯源，部署不要求保留源码目录。资源/目录/规则改变后需重新生成分类。分类不会把行政地区或角色国籍当成任务所在地。

## 两个示例

`goals/example-before-liyue-act1.json`：

```json
{"version":2,"stopBefore":{"chapter":{"id":1101}}}
```

魔神章节按 tag 完成此前章节，停止点使用 ChapterExcelConfigData.beginQuestId；本章和后续魔神章节不作为完成目标。停止点本身不执行 start/finish。主任务和子任务停止点仅求自身接取前置，不凭编号推断剧情顺序。

`goals/example-mondstadt-liyue-world-main.json`：

```json
{
  "version":2,
  "include":[{"regions":["蒙德","璃月"],"categories":["世界任务"]}],
  "exclude":[{"categories":["扩展区域世界任务"]}]
}
```

`goals/example-sumeru-complete-candidate.json` 已改为维度选择；用当前资源离线比对，选中主任务集合仍与旧文件的 881 项完全一致。仅是未采用的通用候选，没有为其生成账号计划。

## 停止点的执行限制（尚未解决）

plan.stopPreview 返回 subId、beforeState、projectedState、status（可接/不可接）、reasons 和 handoffAvailable。状态来自假定前置步骤成功后的静态投影；计划有阻断时，这些不是实际结果。

如果原有进度已开始/完成停止点，返回 stop_unavailable，不自动倒退。若前置完成产生的状态事件或正常交接会开始停止点，返回 stop_auto_accept，禁止执行。当前正常接取路径没有可供 adjust 使用的“保持入口未开始”钩子，本批范围内无法兑现这类严格停止点的在线执行。后续建议在正常接取入口增加仅重放上下文生效的边界保护；本批没有修改 QuestManager 或 GameQuest，也没有以重置状态/跳过 beginExec 冒充实现。

璃月示例空白试算中，100002 条件上可接，但 projectedState=2，因而保留 stop_auto_accept。停止点不能被解释为“已验证可保持未开始”。

## [0,3] 调查与修正规划

全 QuestExcel acceptCond 中有 4,942 个不同子任务使用 QUEST_COND_STATE_EQUAL [0,3]，没有其他 [0,…] 取值。其中 4,285 个是各主任务最低 order 的入口，657 个不是入口。相关主任务中，657 个有唯一建议交接来源、3,571 个没有来源、57 个来源不唯一。详见 build/quest-adjust/zero-state-audit.json，可用 audit_zero_state.py 重算。

不能把 `[0,3]` 改成全局恒真。上游 Grasscutter ConditionStateEqual 同样在任务不存在时仅当目标状态为 0 才返回真：
https://raw.githubusercontent.com/Grasscutters/Grasscutter/development/src/main/java/emu/grasscutter/game/quest/conditions/ConditionStateEqual.java

上游 GameMainQuest 的建议交接代码还是注释；本地 QuestManager.opensUnlinked/startMainQuestIfUnlinked、出生入口以及 GameMainQuest.tryStartFollowingQuests 已针对这种断开入口提供显式启动和登录补交接。对原版官方服务端语义没有源码证据；“丢失引用/需外部交接的入口占位”是基于本地资源与调用路径的推断，不是官方语义定论。

本批仅在 adjust 中修正规划与预览：普通条件求值仍为 false；单条件入口有唯一可证明的建议前置时，依赖其唯一正常结算点，并模拟交接的接取及 beginExec 审计。无来源/多来源/非入口 [0,3] 分别保留 entry_handoff_missing、entry_handoff_ambiguous、zero_reference_unresolved 阻断。

4004/400401 没有任何 suggestTrackMainQuestList 来源；因此不能从“已完成须弥”直接证明它可自然接取。正常账号也可能遇到此缺口，已有的断开入口补丁不能覆盖它。建议先从可靠资源恢复真实前置或章节解锁入口，再在数据驱动的正常接取路径修复，补充出生/登录补交接验证；不要修改 ConditionStateEqual 让任务 0 永远视为完成。本批未改服务端条件处理器，未做原生接取验收。

## 后台操作

1. 登录后台，刷新在线账号并选择账号；离线撤销时可直接填写 UID。
2. 加载选择目录，按地区/类别/扩展区域多选，搜索中文章节或主任务名；或者上传/粘贴目标 JSON。需多个 include 分组时使用 JSON 模式。
3. 生成只读计划。显示步数、阻断按类型、邮件数、1.3 秒/步粗估、停止点状态、顺带接取候选与省略项；展开完整 JSON 核对逐步操作、条件、奖励来源和阻断原因。
4. 无阻断且有步骤、执行能力开启时才可点击确认。目标、账号、JSON、排除或停止点变化后，旧计划立即失效；请求途中变更也不会接受旧响应。
5. 确认后才调用执行；每两秒查询 job。显示进度和完整报告，终态停止轮询。执行期间账号须在线且在自己的场景 3 大世界、保持不动；结束后强制下线并重新登录。
6. 查看备份，填写 job/backup ID。账号离线后再次确认才能撤销；冲突字段和不可回退项目保留并报告。

新增 GET /console/api/quest/options，其余沿用现有 plan/apply/job/backups/undo。所有 quest 路由仍先调用 authorized，沿用 X-Console-Token；页面没有跳过未知阻断的勾选项。服务端还会拒绝过期/错账号计划、变更后的存档/资源/策略、执行中互斥冲突、非大世界/未加载环境及在线撤销。页面完成源码接线和 JavaScript 语法检查；未启动服务或做浏览器/原生联调，不能声称端到端执行/撤销验收通过。

## 离线结果与重跑

类别总表：build/quest-adjust/category-dryrun-summary.md，结构化统计同名 .json；明细在 category-plans-delivery/。每行都是独立空白状态，步数/邮件只覆盖可解析可排序部分，有阻断均不能跳。无需且不应将这些离线 planId 交给 apply。

```powershell
python tools/quest-analysis/category_dryrun.py
python tools/quest-analysis/audit_zero_state.py
& tools/quest-analysis/run_c5b_locked.ps1 -Task Test
& tools/quest-analysis/run_c5b_locked.ps1 -Task Categories -OutputDir build/quest-adjust/category-plans-next
python tools/quest-analysis/category_dryrun.py --summarize --plans build/quest-adjust/category-plans-next
```

脚本用 CreateNew 获取父目录 build.lock，所有 Gradle 任务 --no-daemon，finally 仅删除自己获得的锁。输出 CREATE_NEW，重跑需使用新的输出目录，不覆盖已有明细。测试实际命令为 `./gradlew.bat test --tests "*quest.adjust*" -PskipHandbook=1 --no-daemon`。测试 52 项通过，0 失败/错误/跳过；覆盖维度、并集/交集/排除、歧义、每日委托、stopBefore、[0,3] 以及原有重放/邮件/撤销单测。旧单测写测试号报告的行为已移除，夹具改为 UID 0 且不输出账号计划文件。

仍未解决：严格 stopBefore 的自动接取保护；枫丹等断开入口真实前置；限时/下线传说的历史分类证据；area 32 的正式名称；各类 Lua/持久效果/邀约进度/结算分支阻断；浏览器、真实服务端和原生世界验收。这些未以强制完成标记替代。

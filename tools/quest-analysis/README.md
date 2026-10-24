# 任务牵连静态分析

纯 Python 3 标准库，建议 Python 3.10+。脚本只读取任务目录、奖励表、Java 枚举和注解 handler，只在本目录 `out/` 写文件，不访问数据库或服务端。

## 用法

在项目根目录运行（也可使用脚本绝对路径，工作目录不限）：

```powershell
python tools/quest-analysis/quest_deps.py
python tools/quest-analysis/quest_deps.py --main 351 --depth 2
python tools/quest-analysis/quest_deps.py --sub 500006 --depth 3
python tools/quest-analysis/test_quest_deps.py
```

全量模式覆盖生成四个输出；查询模式实时读取输入，只打印下游树，不覆盖报告。`--main` 从父任务和全部子任务同时展开，`--depth 0` 只列根节点。重复节点只展开一次，边仍显示；循环不会无限递归。未知 ID 和负深度报错。

## 输出与结构

- `out/deps.json`：`nodes` 用 `main:ID` / `sub:ID` 分命名空间，避免 ID 碰撞；每个节点 `out` 包含八类出边及副作用。`edges` 为全部有目标的边（含目录外引用）；`resourceReaders` 是全局变量、open state、点位、区域读取索引；`unresolved` 保存无法确认的引用。
- `out/stats.json`：cond/content/exec 出现次数、阶段分布、状态、handler 路径、输入 SHA256、旧目录统计与 2017/42 基线核对。
- `out/stats.md`：每种出现类型的中文统计表。
- `out/validation.json`：本次五项回归、五组 CLI 检查及重跑输出哈希一致性的验证记录；独立于全量生成，不自动刷新。
- `out/forced_complete_checklist.md`：按执行阶段排列的通用清单；蒙德神像、纳塔神像、完整 chapterId=1001 与 351 前置的具体参数、奖励、缺口、前后边界引用。

`quest_state` 出边从被读取任务指向读取它的任务；全局变量等出边从 exec 写入者指向 cond/content 读取者。边证据保存原记录、字段、数组位置、比较参数、逻辑组合和状态。`finishParent` 单独连接子任务到父任务，再由父任务连向引用其状态的任务。父任务奖励仅在父节点展开，完成父任务的子节点仅引用它，不能逐子任务重复发奖。

`handled` = 当前枚举存在并在对应目录找到 `@QuestValueCond/Content/Exec`；`enum_only` = 枚举存在但无上述注解 handler；`missing` = 枚举没有。去掉 Java 注释后扫描，包含多行及限定类名注解，不把 import 当 handler。即便其他特判代码补偿某行为，也不改变此状态定义。统计仅计算有效字段，不重复计 sourceVariants；cond/content 按类型前缀分组。

## 识别规则与局限

- 状态条件的参数 0 是子任务 ID；PARENT/MAIN_QUEST_STATE 类型使用父任务命名空间。零或不存在的 ID 保留为未解析引用，不偷偷丢掉。ITEM_GIVING_FINISHED 参数 0 作为任务关联事件保留，不当成已完成前提。COMPLETE_ANY/ALL_QUEST 列表按候选 ID 解析，当前输入无此类型，若未来新增格式须核对定义。QUEST_CHECK 参数语义未确认，列入 unresolved，避免按数值碰撞错连任务。
- `may_affect` 是可能影响，不能推出“现在可接取”。SET 值可能不满足比较，INC/DEC 依赖原值，NOT_EQUAL/失败条件可能阻塞或使任务失败；ANY_TRANS_POINT 只按场景粗匹配，额外筛选条件仍需人工核验。全局变量证据可区分写入者和读取者，循环反馈不当作拓扑顺序。
- 拓扑仅用 accept/finish 中等于已完成（子任务状态 3）的正向状态边和 finishParent；不把负条件、fail、变量反馈当先决顺序。OR 分支会被保守地一起列入依赖覆盖；不意味着需要同时执行所有分支。环及被环阻塞节点单独列出；order/ID 只用于稳定排序。
- open state、点位/区域按参数位置连接；批量 open state 从首参数提取候选 ID，未知版本语义须人工确认。场景 group、suite、gadget、tag、ROLLBACK 保留原 exec；REFRESH_GROUP_SUITE 同时展开场景及 group/suite 对。未硬猜所有 scene/group 变量读取布局；本地 QUEST_VAR、进度事件等保留在 other，未构造跨任务变量边。
- 没有出现 ADD_ITEM 类 exec 时不会伪造物品；若未来出现则分类为 item_reward 并保留参数，未知批量格式不猜数量。gainItems 保留原 ID/数量；RewardExcelConfigData 展开 itemId/itemCount，同时保留奖励原记录。奖励表缺失/奖励 ID 缺失会标记 unresolved，不能推断无奖励。
- Lua、动态条件、group 变量、NPC/talk 配置、事件注册、客户端逻辑、未执行的源码分支不在输入范围。handler 存在不保证已编译、已注册、语义完整或事件能触发。
- 主任务 351 无 chapterId；序章范围使用 352 的 1001，并额外加入 351。同章旧测试任务保留，不能把它们误称为实际必做主线。两条神像均在 scene 3，不代表同地区。
- 输入一次性载入（约 75MB），输出为可读 JSON，约 120MB；运行时内存高于输入大小。记录读取内容 SHA256，导出前重新检查输入未变化；各输出原子替换，但四个文件并非一个事务。其他会话修改输入后应重跑，不覆盖其文件。

## 已验证边界

测试覆盖正/负状态依赖、父任务完成传递、全局变量反向关联、点位关联、奖励不重复、注解状态与全量计数闭合；CLI 测试及重跑哈希另在交付时执行。这里只证明静态分析输出，不代表强制完成、服务端部署或游戏内接取已验证。

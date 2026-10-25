# 任务卡 6 结果

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-056 | 已修；`MONSTER_PARTNER` 可反序列化，数值 6 是按现有 0–5 顺序的推断，代码已标注 | `b1685788` | `src/main/java/emu/grasscutter/game/props/MonsterType.java` | `src/test/java/emu/grasscutter/game/props/MonsterTypePartnerTest.java` | 在实际会刷出 `MONSTER_PARTNER` 的流程中击败对应实体，检查副本击杀类型条件与后续死亡收尾，确认日志无空指针异常；静态表的 442 行不等于 442 个实际刷怪点。 |
| R2-069 | 跳过：18 处引用的 16 个 ID 均不在 `resources/Server/DropTableExcelConfigData.json`；普通宝箱已有配置回退，但 `SearchPoint`、遗迹机关等没有可核对的奖励源，不能凭 ID 编造掉落 | — | 无 | 无；缺失原生掉落数据，无法验证正确奖励 | 取得对应原生根后分别开 `133307415/415001` 普通宝箱、`133307177/177007` 遗迹机关、`133315008/8015` 调查点，核对奖励、开箱状态及重复交互。 |
| R2-131 | 跳过：调查表记录 Boss 103/group `133706025`/monster `25135202`，但该 group Lua、`scene3_block3706.lua` 的 group 坐标及 `scene3_point.json` 的追踪点 `2262` 都缺失；相邻 group 坐标不能代替 Boss 出生点 | — | 无 | 无；缺少可信出生坐标 | 取得客户端场景坐标后进入对应区域，确认 Boss 出生、击败后奖励花及再次刷新；检查日志不再报 `missing position for boss id=103`。 |

测试命令：`.\gradlew.bat test --no-daemon --tests "emu.grasscutter.game.props.MonsterTypePartnerTest"`。结果：`BUILD SUCCESSFUL`（定向测试通过；未部署、未进游戏）。

# 任务卡 1 结果

| 条目编号 | 状态 | 提交号 | 改动文件 | 新增测试 | 进游戏怎么验证 |
| --- | --- | --- | --- | --- | --- |
| R2-036 | 已修 3 种；跳过 10 种（见下） | `0a71e578918d83a945b4bd36992c403bdcc3bf98` | `game/dungeons/challenge/factory/{ChallengeFactory,GuardHpChallengeFactoryHandler,TriggerCountChallengeFactoryHandler,TriggerInTimeChallengeFactoryHandler,TriggerInTimeFlyChallengeFactoryHandler}.java`；`trigger/{GuardForTimeTrigger,GuardTrigger,KillMonsterTimeIncTrigger,TriggerGroupTriggerTrigger}.java` | `ChallengeFactoryMissingTypesTest`、`ChallengeTriggerParameterTest`（6 项） | 场景 20123／组 220123010：启动灯台机关挑战，复位并激活灯台，观察进度从 Lua 初值 1/4 到 4/4；场景 1／组 111101097：操作 97030 开始飞行限时挑战，击杀同组魔物看剩余时间增加，进入区域 97023 看成功，另一次等待超时看失败；场景 1／组 111101015：触发 15001 出生、出现守护物 15019，保护 30 秒看成功，另一次让 15019 被击毁看失败。 |

已接通类型：`CHALLENGE_TRIGGER_COUNT`（事件类型、tag、目标数、初始进度）；`CHALLENGE_TRIGGER_IN_TIME_FLY`（到达目标一次成功、魔物击杀补时、超时失败）；`CHALLENGE_GUARD_HP`（守护时长、血量显示、被击毁失败）。守护类型最后一个参数 `90` 的用途只有 Lua 与文本模板旁证；代码已标注「推断」，目前仅作为静态显示参数，血量归零判失败。

保留的 10 种：`CHALLENGE_KILL_COUNT_FROZEN_LESS`（受冻次数事件未接入）；`CHALLENGE_TRIGGER2_AVOID_TRIGGER1`（两种 tag 的先后与失败条件缺证）；`CHALLENGE_FATHER_SUCC_IN_TIME`（父挑战成功状态缺证）；`CHALLENGE_MONSTER_DAMAGE_COUNT`（伤害累计事件缺证）；`CHALLENGE_ELEMENT_REACTION_COUNT`、`CHALLENGE_CRYSTAL_ELEMENT_REACTION_COUNT`、`CHALLENGE_SWIRL_ELEMENT_REACTION_COUNT`（反应事件与归类缺证）；`CHALLENGE_FREEZE_ENEMY_IN_TIME`（冻结状态与时长事件缺证）；`CHALLENGE_SHEILD_ABSORB_DAMAGE_COUNT`（护盾吸收事件缺证）；`CHALLENGE_DIE_LESS_IN_TIME`（角色死亡计数事件缺证）。这些类型仍未注册工厂，避免错误判定成功。

场景 1065／组 201065001 的 `TRIGGER_COUNT` 流程调用 `StopChallenge`；该函数当前仍未实现，属于任务卡 2 的 R2-035，须完成后才能验证这一组的完整多波流程。本卡未启动游戏，也未部署；上述为进游戏验证步骤，不是验收结论。

测试命令：`.\gradlew.bat test --no-daemon --tests 'emu.grasscutter.game.dungeons.challenge.factory.ChallengeFactoryMissingTypesTest' --tests 'emu.grasscutter.game.dungeons.challenge.trigger.ChallengeTriggerParameterTest'`。
结果：`BUILD SUCCESSFUL`，6 项定向测试通过；未运行全量测试或数据库测试。

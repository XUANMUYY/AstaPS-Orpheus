# fix14-life-c 结果

代码提交：`002a5e58`（分支 `fix14-life-c`）。仅静态编译与定向单测通过，未部署、未进行原生客户端验收。

| 条目 | 结果与依据 | 进游戏怎么测 |
| --- | --- | --- |
| R5-074 | 已修会话缺失、曲目或 UGC ID 不匹配、重复结算可推进 watcher 的路径；失败有专用 retcode。`MusicGameBasicConfigData` 没有曲目时长或明确满分字段，因此未加时长下限和分数上限；开始后立即自报高分仍是剩余风险。 | 打开「荒泷极上盛世豪鼓大祭典」，分别测试原生曲目与自制谱面：正常演奏结算成功；未开始、换曲目或重放结算不得推进任务。 |
| R5-143 | 已修：先比较旧最高分，再写入新成绩与返回 `isNewRecord`。 | 在「荒泷极上盛世豪鼓大祭典」同一曲目先得低分再刷新高分，核对结算的新纪录提示；同分不再提示。 |
| R5-141 | 部分修：5346 的 `NOT_TAKE_ALL_WATCHER` 已按 watcher 领奖状态判断；5344 的 `NOT_TAKE_ALL_SCORE_REWARD` 未修，现有 7.1 协议缺 `ActivityTakeScoreRewardReq`，服务端也无活动积分领取状态，不能编造已领取判据。 | 「往冥府的安魂歌」开放至第 28 天后领尽三个任务奖励，再打开活动页并核对 `meetCondList` 不含 5346001；「无忧无虑大雪仗」积分奖励分支待协议与状态补齐后再测。 |
| R5-142 | 已修：`DAYS_LESS` 按活动开始后的第 1 天起算，条件为开放天数 `< N`。 | 「无忧无虑大雪仗」完成前置任务后，在第 4 天核对 5344004 不满足、第 6 天核对 5344005 不满足；页面表现仍待客户端核对。 |

编译：`.\gradlew.bat --no-daemon compileJava` 通过（原有弃用警告 5 条）。
测试：`MusicGamePlayTrackerTest` 3 例、`MusicGameRecordTest` 2 例、`MiniGameActivityConditionTest` 3 例；定向 `test --tests` 共 8 例通过。
改过的已有文件：`ActivityConditions.java`、`DayLess.java`、`MusicGameActivityHandler.java`、`MusicGamePlayerData.java`、`GameSession.java`、`HandlerMusicGameStartReq.java`、`HandlerMusicGameSettleReq.java`。

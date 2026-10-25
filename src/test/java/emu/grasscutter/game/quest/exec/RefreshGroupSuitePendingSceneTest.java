package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 玩家不在目标场景时，刷新要先落成任务套件记录，进场景时才会按记录加载。 */
final class RefreshGroupSuitePendingSceneTest {
    @Test
    void unloadedSceneRefreshIsRememberedOnTheOwningMainQuest() throws Exception {
        var main = new GameMainQuest();
        var field = GameMainQuest.class.getDeclaredField("questGroupSuites");
        field.setAccessible(true);
        field.set(main, new ArrayList<QuestGroupSuite>());
        var quest = new GameQuest();
        quest.setMainQuest(main);

        // 35802：骑士团总部室内 1004，玩家站在大世界
        assertTrue(ExecRefreshGroupSuite.rememberPending(quest, "201004902,2", 1004));
        assertEquals(1, main.getQuestGroupSuites().size());
        var saved = main.getQuestGroupSuites().get(0);
        assertEquals(List.of(1004, 201004902, 2), List.of(saved.getScene(), saved.getGroup(), saved.getSuite()));

        assertFalse(ExecRefreshGroupSuite.rememberPending(quest, "201004902,2", 1004), "重复刷新不算变化");
        assertTrue(ExecRefreshGroupSuite.rememberPending(quest, "bad;201004902,0", 1004), "格式错误的项跳过，套件 0 清除记录");
        assertTrue(main.getQuestGroupSuites().isEmpty());
    }
}

package emu.grasscutter.game.quest.content;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.enums.QuestContent;
import org.junit.jupiter.api.Test;

/** 46504 等：param = [1018, 0]，玩家身处场景 1018 即满足。 */
final class ContentPresentAtSpecificSceneTest {
    @Test
    void comparesAgainstCurrentScene() {
        assertTrue(ContentPresentAtSpecificScene.isPresent(new int[] {1018, 0}, 1018));
        assertFalse(ContentPresentAtSpecificScene.isPresent(new int[] {1018, 0}, 3));
        assertFalse(ContentPresentAtSpecificScene.isPresent(new int[] {0, 0}, 0));
        assertFalse(ContentPresentAtSpecificScene.isPresent(new int[] {}, 1018));
    }

    @Test
    void questDataNameNoLongerDropped() {
        // 枚举里没有的名字会被 Gson 解析成 null，QuestData.onLoad 随即把整条条件过滤掉。
        assertEquals(QuestContent.QUEST_CONTENT_PRESENT_AT_SPECIFIC_SCENE,
                QuestContent.getContentTriggerByName("QUEST_CONTENT_PRESENT_AT_SPECIFIC_SCENE"));
    }
}

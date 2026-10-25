"""任务生成机关的静态场景来源，使用最小 Lua 配置验证保守边界。"""
import unittest
from instanced_scenes import quest_gadget_sources


class QuestGadgetSourcesTest(unittest.TestCase):
    def test_multiple_tasks_and_mixed_scenes_are_merged(self):
        sources, unknown = quest_gadget_sources('''
        quest_data["1"]={gadgets={{id=77,scene_id=20023,pos="a"},{id=88,scene_id=3}}}
        quest_data["2"]={gadgets={{id=77,scene_id=3,pos={x=1,y=2}}}}
        ''')
        self.assertEqual({3, 20023}, sources['77'])
        self.assertEqual({3}, sources['88'])
        self.assertFalse(unknown)

    def test_missing_or_dynamic_scene_cannot_be_inferred_from_other_actors(self):
        sources, unknown = quest_gadget_sources('''
        -- gadgets={{id=99,scene_id=3}}
        npcs={{id=88,scene_id=20023}}
        gadgets={{id=77,scene_id=20023},{id=77,pos="unknown"},{id=88,scene_id=scene},{id=66,scene_id=20023+1}}
        ''')
        self.assertEqual({'77': {20023}}, dict(sources))
        self.assertEqual({'77', '88', '66'}, unknown)


if __name__ == '__main__':
    unittest.main()

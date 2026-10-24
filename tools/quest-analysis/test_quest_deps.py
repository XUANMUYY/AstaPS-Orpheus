"""对真实目录和关键语义执行回归检查；不写输入文件。"""
import unittest
from quest_deps import Analysis, ROOT

class DependencyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.a = Analysis(ROOT)

    def test_counts_close(self):
        a=self.a
        for kind, table in a.counts.items():
            expected=sum(len(q.get(f,[]) or []) for q in list(a.mains.values())+list(a.subs.values())
                         for f in (('beginExec','finishExec','failExec') if kind=='exec' else ('acceptCond','finishCond','failCond'))
                         if kind=='exec') if kind=='exec' else None
            if expected is not None:self.assertEqual(expected,sum(sum(c.values()) for c in table.values()))
        stats=a.stats()
        expected=sum(len(q.get(f,[]) or []) for q in list(a.mains.values())+list(a.subs.values()) for f in ('acceptCond','finishCond','failCond'))
        self.assertEqual(expected,stats['summary']['cond']['total']+stats['summary']['content']['total'])

    def test_statue_direction_and_parameters(self):
        a=self.a
        self.assertTrue(any(e['target']=='sub:35301' for e in a.adj['sub:35205']))
        self.assertTrue(any(e['target']=='main:352' for e in a.adj['sub:35205']))
        self.assertTrue(any(e['source']=='sub:500016' for e in a.incoming['sub:500006']))
        e=next(e for e in a.adj['sub:500006'] if e['category']=='global_var')
        self.assertEqual(e['resource'],'7509701')
        self.assertEqual(e['target'],'sub:500016')
        self.assertEqual(a.subs[500006]['finishCond'][0]['param'],[3,1166])
        self.assertTrue(any(e['category']=='trans_point' for e in a.incoming['sub:500006']))

    def test_reward_single_owner(self):
        a=self.a
        self.assertTrue(a.nodes['main:352']['out']['item_reward'])
        self.assertFalse(any(e.get('source')=='rewardIdList' for e in a.nodes['sub:35205']['out']['item_reward']))
        self.assertEqual(a.classify('QUEST_EXEC_ADD_QUEST_PROGRESS'),'other')
        self.assertEqual(a.classify('QUEST_EXEC_ADD_ITEM'),'item_reward')

    def test_negative_and_cycle_topology(self):
        a=Analysis.__new__(Analysis)
        a.nodes={n:{'order':0,'id':i} for i,n in enumerate(['a','b','c'])}
        def edge(x,y,t='QUEST_COND_STATE_EQUAL'):
            return {'source':x,'target':y,'evidence':{'type':t,'field':'acceptCond','param':[1,3]}}
        a.edges=[edge('a','b'),edge('b','a','QUEST_COND_STATE_NOT_EQUAL')]
        order,cycles=a.topo(['a','b']);self.assertEqual(order,['a','b']);self.assertFalse(cycles)
        a.edges.append(edge('b','a'));order,cycles=a.topo(['a','b','c'])
        self.assertEqual(order,['c']);self.assertEqual(set(cycles),{'a','b'})

    def test_handler_states(self):
        self.assertEqual(self.a.status('QUEST_EXEC_UNLOCK_POINT'),'handled')
        self.assertEqual(self.a.status('QUEST_EXEC_NOTIFY_DAILY_TASK'),'enum_only')
        self.assertEqual(self.a.status('QUEST_EXEC_UNLOCK_MIRROR_AVATAR_TEAM'),'missing')

if __name__=='__main__':unittest.main()

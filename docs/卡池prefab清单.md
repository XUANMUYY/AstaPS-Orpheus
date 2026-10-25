# 卡池 prefab 清单

来源与方法：
- 名称与卡池对应：`data/GachaBannerPrefab.txt`（社区整理，A### → 卡池/UP 名称；该表只到 A300）。
- 客户端资源证据：只读扫描 `ForkClient/YuanShen_Data/{StreamingAssets,Persistent}/AssetBundles/blocks/*.blk` 里的明文资源名（blk 大部分内容加密/压缩，只有头部明文可见，所以**没扫到不等于不存在**）。三类证据：P=`GachaShowPanel_A###`（界面 prefab），T=`UI_Tab_GachaShowPanel_A###`（页签预览图），U=`UI_Gacha_A###_*`（UP 素材）。
- 客户端未启动、未修改。

**数量**：社区表 243 个编号（A007–A300 范围内的卡池界面）；客户端资源里明文可见的编号 184 个（P 3、T 79、U 171）；两者并集 269 个。最大编号：安装包 A310（U）/A307（T），热更新 A311（T）。另有非 A 系列界面：N、N2、N3、N7（新手/集录一类，推断）、S（推断为常驻）。

## 7.1 各池对应的 prefab（修订，以此为准）

**规律**：社区表里每个版本半期的卡池界面按「角色池、角色池2、武器池」三个连号一组递增；复刻也会拿新编号（例：奈芙尔 A259 与 A287、哥伦比娅 A271 与 A298、杜林 A265 与 A290）。所以 A280–A282（丝柯克、爱可菲、武器）是 6.x 复刻时的界面，**不是** 7.1。已知分组：A298–A300=哥伦比娅/雷电将军/武器，A301–A303=7.0（A303 对应武器 11520，上游映射表可证），A304–A306、A307–A309、A310–A312 依次往后（推断）。

客户端资源证据（只读扫描，见下表）：
- 同一个资源块 `01720630.blk` 里同时有 `UI_Tab_GachaShowPanel_A305`、`UI_Tab_GachaShowPanel_A307` 和 `UI_Gacha_AvatarIcon_Vesna`；`UI_Gacha_A307_Up2` 另在 `06977816.blk`。薇斯纳的卡池界面因此落在这一组里（A307，推断，A305 也有可能）。
- 比 A307 更新的只有 A310（`UI_Gacha_A310_Up2`）、A311（`UI_Gacha_A311_Up2D`；`UI_Tab_GachaShowPanel_A311` 在 10-22 的热更新里）。安装包里没有 A313 及以后，说明安装包内容到 7.1 为止，**A310–A312 就是 7.1 下半**（推断）。

| 卡池 | 时间（北京时间） | prefab | 依据 |
|---|---|---|---|
| 煦风欢舞时（薇斯纳） | 09-23 06:00 – 10-13 17:59 | A307 | 同块出现薇斯纳图标，推断 |
| 涌浪叙歌（沃雅妮莎） | 同上 | A308 | 组内第二个，无明文痕迹，推断 |
| 神铸赋形（蝶变/漩流颂歌） | 同上 | A309 | 组内武器，武器界面全部无明文痕迹，推断 |
| 虚星临渡（丝柯克） | 10-13 18:00 – 11-03 14:59 | A310 | `UI_Gacha_A310_Up2`，推断 |
| 莓色香颂（爱可菲） | 同上 | A311 | `UI_Tab_GachaShowPanel_A311`、`UI_Gacha_A311_Up2D`，推断 |
| 神铸赋形（苍耀/香韵奏者） | 同上 | A312 | 组内武器，推断 |
| 奔行世间（常驻） | 全程 | A022 | 历代常驻 A008/A017/A022，A022 最新；有 `UI_Gacha_A022_Up1` |

服务端把「有明文证据」的 A307、A310、A311 以及实测可显示的 A280、A281、A022 放进 `GachaPrefabAvailable.json`；其余（A308、A309、A312）暂借备用界面（A307 或 A280），以免任何一个缺失导致整个祈愿界面「资源过期」。验证一个就加一个进名单。

## 全部编号

| 编号 | 社区表中的卡池/UP | 客户端证据 | 来源 blk（示例） |
|---|---|---|---|
| A001 | （表中无） | P | 安装包(Streaming):AssetBundles\blocks\00\03214986.blk；安装包(Streaming):AssetBundles\blocks\00\04550477.blk |
| A007 | 初行者推荐祈愿 | — |  |
| A008 | 奔行世间 | — |  |
| A009 | 杯装之诗 | — |  |
| A010 | 黎明巡礼 | — |  |
| A011 | 南风之眷 | — |  |
| A012 | 靖妖傩舞 | — |  |
| A013 | 神铸赋形 | — |  |
| A015 | 闪焰的驻足 | — |  |
| A016 | 初行者推荐祈愿 | T | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk |
| A017 | 奔行世间 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A018 | 闪焰的驻足 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A019 | 杯装之诗 | — |  |
| A020 | 神铸赋形 | — |  |
| A021 | 神铸赋形 | — |  |
| A022 | 奔行世间 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A023 | 暂别冬都 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A024 | 陵薮市朝 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A027 | 深秘之息 | T | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk |
| A028 | 浮生孰来 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A031 | 烟火之邀 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A032 | 鱼龙灯昼 | — |  |
| A033 | 赤团开时 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A036 | 杯装之诗 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A037 | 暂别冬都 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A040 | 陵薮市朝 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A041 | 浪涌之瞬 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A044 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A045 | 叶落风随 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A048 | 白鹭之庭 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A049 | 焰色天河 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A052 | 影寂天下人 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A053 | 浮岳虹珠 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A056 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A057 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A060 | （表中无） | T | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk |
| A061 | 鬼门斗宴 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A062 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A065 | 出尘入世 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A066 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A068 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A069 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A071 | 华紫樱绯 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A073 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A074 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A076 | 苍流踏花 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A077 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A079 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk；安装包(Streaming):AssetBundles\blocks\00\14219615.blk |
| A081 | 素霓伣天 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A082 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A084 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A086 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A087 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A089 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A091 | 巡御蘙荟-提纳里 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A092 | 陵薮市朝-钟离 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A093 | 神铸赋形-猎人贯虹 | — |  |
| A094 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A095 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A097 | 雳裁冥昭-赛诺 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A098 | 杯装之诗-温迪 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A099 | 神铸赋形-赤沙终末 | — |  |
| A100 | 翩舞歈莲-妮露 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A101 | 深秘之息-阿贝多 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A102 | 神铸赋形-圣显绿箭 | — |  |
| A103 | 月草的赐慧-纳西妲 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A104 | 焰色天河-宵宫 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A105 | 神铸赋形-千夜飞雷 | — |  |
| A106 | 华紫樱绯-八重神子 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A107 | 暂别冬都-达达利亚 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A108 | 神铸赋形-神乐冬极 | — |  |
| A109 | 余火变相-流浪者 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A110 | 鬼门斗宴-荒泷一斗 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A111 | 神铸赋形-图莱杜拉赤角 | — |  |
| A112 | 影寂天下人-雷电将军 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A113 | 苍流踏花-神里绫人 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A114 | 神铸赋形-薙草波乱 | — |  |
| A115 | 敕诫枢谋-艾尔海森 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A116 | 烟火之邀-魈 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A117 | 神铸赋形-裁叶萃光和璞鸢 | — |  |
| A118 | 赤团开时-胡桃 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A119 | 素霓伣天-夜兰 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A120 | 神铸赋形-护摩若水 | — |  |
| A121 | 迪希雅 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A122 | 赛诺 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A123 | 苇海信标/赤沙之杖 | — |  |
| A124 | 申鹤 | U | 安装包(Streaming):AssetBundles\blocks\00\13951068.blk |
| A125 | 神里绫华 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A126 | 雾切之回光/息灾 | — |  |
| A127 | 纳西妲 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A128 | 妮露 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A129 | 千夜浮梦/圣显之钥 | — |  |
| A130 | 白术 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A131 | 甘雨 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A132 | 碧落之珑/阿莫斯之弓 | — |  |
| A133 | 宵宫 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A134 | 八重神子 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A135 | 飞雷之弦振/神乐之真意 | — |  |
| A136 | 艾尔海森 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A137 | 枫原万叶 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A138 | 苍古自由之誓/裁叶萃光 | — |  |
| A139 | 优菈 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A140 | 可莉 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A141 | 松籁响起之时/四风原典 | — |  |
| A142 | 珊瑚宫心海 | — |  |
| A143 | 流浪者 | U | 安装包(Streaming):AssetBundles\blocks\00\14219615.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A144 | 不灭月华/图莱杜拉的回忆 | — |  |
| A145 | 林尼 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A146 | 夜兰 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A147 | 最初的大魔术/若水 | — |  |
| A148 | 钟离 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A149 | 公子 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A150 | 贯虹之槊/冬极白星 | — |  |
| A151 | 那维莱特 | T | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk |
| A152 | 胡桃 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A153 | 万世流涌大典/护摩之杖 | — |  |
| A154 | 莱欧斯利 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A155 | 温迪 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A156 | 金流监督/终未嗟叹之诗 | — |  |
| A157 | 芙宁娜 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A158 | 白术 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A159 | 静水流涌之辉/碧落之珑 | — |  |
| A160 | 赛诺 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A161 | 神里绫人 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A162 | 波乱月白经津/赤沙之杖 | — |  |
| A163 | 娜维娅 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\14219615.blk |
| A164 | 神里绫华 | U | 安装包(Streaming):AssetBundles\blocks\00\14219615.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A165 | 裁断/雾切之回光 | — |  |
| A166 | 雷电将军 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A167 | 宵宫 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A168 | 薙草之稻光/飞雷之弦振 | — |  |
| A169 | 闲云 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A170 | 纳西妲 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A171 | 鹤鸣余音/千夜浮梦 | — |  |
| A172 | 魈 | — |  |
| A173 | 八重神子 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A174 | 和璞鸢/神乐之真意 | — |  |
| A175 | 千织 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A176 | 荒泷一斗 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A177 | 有乐御簾切/赤角石溃杵 | — |  |
| A178 | 那维莱特 | TU | 安装包(Streaming):AssetBundles\blocks\00\08311857.blk；安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A179 | 枫原万叶 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A180 | 万世流涌大典/苍古自由之誓 | — |  |
| A181 | 阿蕾奇诺 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A182 | 林尼 | U | 安装包(Streaming):AssetBundles\blocks\00\15224338.blk |
| A183 | 赤月之形/最初的大魔术 | — |  |
| A184 | 流浪者 | U | 安装包(Streaming):AssetBundles\blocks\00\03331993.blk |
| A185 | 白术 | U | 安装包(Streaming):AssetBundles\blocks\00\03331993.blk |
| A186 | 图莱杜拉的回忆/碧落之珑 | — |  |
| A187 | 克洛琳德 | — |  |
| A188 | 艾尔海森 | — |  |
| A189 | 赦罪 / 裁叶萃光 | — |  |
| A190 | 希格雯 | U | 安装包(Streaming):AssetBundles\blocks\00\04000414.blk；安装包(Streaming):AssetBundles\blocks\00\14219615.blk |
| A191 | 芙宁娜 | TU | 安装包(Streaming):AssetBundles\blocks\00\04000414.blk；安装包(Streaming):AssetBundles\blocks\00\14085494.blk |
| A192 | 白雨心弦 / 静水流涌之辉 | — |  |
| A193 | 娜维娅 | TU | 安装包(Streaming):AssetBundles\blocks\00\04000414.blk；安装包(Streaming):AssetBundles\blocks\00\14085494.blk |
| A194 | 妮露 | U | 安装包(Streaming):AssetBundles\blocks\00\04000414.blk |
| A195 | 裁断 / 圣显之钥 | — |  |
| A196 | 艾梅莉埃 | U | 安装包(Streaming):AssetBundles\blocks\00\14051391.blk |
| A197 | 夜兰 | TU | 安装包(Streaming):AssetBundles\blocks\00\01407834.blk；安装包(Streaming):AssetBundles\blocks\00\14051391.blk |
| A198 | 柔灯挽歌 / 若水 | — |  |
| A199 | 玛拉妮 | U | 安装包(Streaming):AssetBundles\blocks\00\14051391.blk |
| A200 | 枫原万叶 | U | 安装包(Streaming):AssetBundles\blocks\00\14051391.blk |
| A201 | 冲浪时光 / 苍古自由之誓 | — |  |
| A202 | 基尼奇 | U | 安装包(Streaming):AssetBundles\blocks\00\09602454.blk |
| A203 | 雷电将军 | U | 安装包(Streaming):AssetBundles\blocks\00\09602454.blk |
| A204 | 山王长牙 / 薙草之稻光 | — |  |
| A205 | 希诺宁 | U | 安装包(Streaming):AssetBundles\blocks\00\09602454.blk |
| A206 | 千织 | U | 安装包(Streaming):AssetBundles\blocks\00\09602454.blk |
| A207 | 岩峰巡歌 / 有乐御簾切 | — |  |
| A208 | 纳西妲 | U | 安装包(Streaming):AssetBundles\blocks\00\06479199.blk |
| A209 | 胡桃 | U | 安装包(Streaming):AssetBundles\blocks\00\06479199.blk |
| A210 | 千夜浮梦 / 护摩之杖 | — |  |
| A211 | 恰斯卡 | T | 安装包(Streaming):AssetBundles\blocks\00\07711961.blk |
| A212 | 林尼 | — |  |
| A213 | 星鹫赤羽 / 最初的大魔术 | — |  |
| A214 | 钟离 | TU | 安装包(Streaming):AssetBundles\blocks\00\02186838.blk；安装包(Streaming):AssetBundles\blocks\00\06254239.blk |
| A215 | 那维莱特 | U | 安装包(Streaming):AssetBundles\blocks\00\06254239.blk |
| A216 | 贯虹之槊 / 万世流涌大典 | — |  |
| A217 | 玛薇卡 | U | 安装包(Streaming):AssetBundles\blocks\00\06254239.blk |
| A218 | 茜特菈莉 | T | 安装包(Streaming):AssetBundles\blocks\00\02186838.blk |
| A219 | 焚曜千阳 / 祭星者之望 | — |  |
| A220 | 阿蕾奇诺 | U | 安装包(Streaming):AssetBundles\blocks\00\06254239.blk；安装包(Streaming):AssetBundles\blocks\00\14219615.blk |
| A221 | 克洛琳德 | U | 安装包(Streaming):AssetBundles\blocks\00\06254239.blk；安装包(Streaming):AssetBundles\blocks\00\14219615.blk |
| A222 | 赤月之形 / 赦罪 | — |  |
| A223 | 梦见月瑞希 | TU | 安装包(Streaming):AssetBundles\blocks\00\07025029.blk；安装包(Streaming):AssetBundles\blocks\00\12247975.blk |
| A224 | 希格雯 | U | 安装包(Streaming):AssetBundles\blocks\00\07025029.blk |
| A225 | 寝正月初晴/白雨心弦 | — |  |
| A226 | 芙宁娜 | TU | 安装包(Streaming):AssetBundles\blocks\00\06286806.blk；安装包(Streaming):AssetBundles\blocks\00\12304558.blk |
| A227 | 莱欧斯利 | TU | 安装包(Streaming):AssetBundles\blocks\00\06286806.blk；安装包(Streaming):AssetBundles\blocks\00\12304558.blk |
| A228 | 静水流涌之辉/金流监督 | — |  |
| A229 | 瓦雷莎 | TU | 安装包(Streaming):AssetBundles\blocks\00\06286806.blk；安装包(Streaming):AssetBundles\blocks\00\12304558.blk |
| A230 | 闲云 | TU | 安装包(Streaming):AssetBundles\blocks\00\06286806.blk；安装包(Streaming):AssetBundles\blocks\00\12304558.blk |
| A231 | 溢彩心念 / 鹤鸣余音 | — |  |
| A232 | 希诺宁 | U | 安装包(Streaming):AssetBundles\blocks\00\06961658.blk |
| A233 | 温迪 | U | 安装包(Streaming):AssetBundles\blocks\00\06961658.blk |
| A234 | 岩峰巡歌 / 终末嗟叹之诗 | P | 安装包(Streaming):AssetBundles\blocks\00\09732188.blk |
| A235 | 爱可菲 | U | 安装包(Streaming):AssetBundles\blocks\00\06961658.blk |
| A236 | 娜维娅 | TU | 安装包(Streaming):AssetBundles\blocks\00\06961658.blk；安装包(Streaming):AssetBundles\blocks\00\11752448.blk |
| A237 | 香韵奏者 / 裁断 | — |  |
| A238 | 基尼奇 | U | 安装包(Streaming):AssetBundles\blocks\00\10079534.blk |
| A239 | 雷电将军 | TU | 安装包(Streaming):AssetBundles\blocks\00\10079534.blk；安装包(Streaming):AssetBundles\blocks\00\13847790.blk |
| A240 | 山王长牙 / 薙草之稻光 | — |  |
| A241 | 丝柯克 | U | 安装包(Streaming):AssetBundles\blocks\00\10079534.blk |
| A242 | 申鹤 | U | 安装包(Streaming):AssetBundles\blocks\00\10079534.blk |
| A243 | 苍耀/息灾 | — |  |
| A244 | 玛薇卡 | U | 安装包(Streaming):AssetBundles\blocks\00\04973743.blk |
| A245 | 艾梅莉埃 | U | 安装包(Streaming):AssetBundles\blocks\00\04973743.blk |
| A246 | 焚曜千阳 / 柔灯挽歌 | — |  |
| A247 | 伊涅芙 | T | 安装包(Streaming):AssetBundles\blocks\00\00272924.blk |
| A248 | 茜特菈莉 | T | 安装包(Streaming):AssetBundles\blocks\00\00272924.blk |
| A249 | 支离轮光 / 祭星者之望 | — |  |
| A250 | 玛拉妮 | U | 安装包(Streaming):AssetBundles\blocks\00\08609329.blk |
| A251 | 恰斯卡 | U | 安装包(Streaming):AssetBundles\blocks\00\08609329.blk |
| A252 | 冲浪时光 / 星鹫赤羽 | — |  |
| A253 | 菈乌玛 | U | 安装包(Streaming):AssetBundles\blocks\00\08609329.blk |
| A254 | 纳西妲 | U | 安装包(Streaming):AssetBundles\blocks\00\08609329.blk |
| A255 | 纺夜天镜/千叶浮梦 | — |  |
| A256 | 菲林斯 | TU | 安装包(Streaming):AssetBundles\blocks\00\02691757.blk；安装包(Streaming):AssetBundles\blocks\00\03214986.blk |
| A257 | 夜兰 | TU | 安装包(Streaming):AssetBundles\blocks\00\02691757.blk；安装包(Streaming):AssetBundles\blocks\00\03214986.blk |
| A258 | 血染荒城 / 若水 | — |  |
| A259 | 奈芙尔 | TU | 安装包(Streaming):AssetBundles\blocks\00\02691757.blk；安装包(Streaming):AssetBundles\blocks\00\03214986.blk |
| A260 | 芙宁娜 | TU | 安装包(Streaming):AssetBundles\blocks\00\02691757.blk；安装包(Streaming):AssetBundles\blocks\00\03214986.blk |
| A261 | 真语秘匣 / 静水流涌之辉 | — |  |
| A262 | 钟离 | U | 安装包(Streaming):AssetBundles\blocks\00\01744125.blk |
| A263 | 阿蕾奇诺 | U | 安装包(Streaming):AssetBundles\blocks\00\01744125.blk；安装包(Streaming):AssetBundles\blocks\00\06004998.blk |
| A264 | 贯虹之槊 / 赤月之形 | — |  |
| A265 | 杜林 | T | 安装包(Streaming):AssetBundles\blocks\00\10518886.blk |
| A266 | 温迪 | PU | 安装包(Streaming):AssetBundles\blocks\00\00265660.blk；安装包(Streaming):AssetBundles\blocks\00\06004998.blk |
| A267 | 黑蚀/黎明破晓之矢 | — |  |
| A268 | 瓦雷莎 | T | 安装包(Streaming):AssetBundles\blocks\00\03104075.blk |
| A269 | 希诺宁 | U | 安装包(Streaming):AssetBundles\blocks\00\07347193.blk |
| A270 | 溢彩心念 / 岩峰巡歌 | — |  |
| A271 | 哥伦比娅 | U | 安装包(Streaming):AssetBundles\blocks\00\07347193.blk |
| A272 | 伊涅芙 | TU | 安装包(Streaming):AssetBundles\blocks\00\03104075.blk；安装包(Streaming):AssetBundles\blocks\00\07347193.blk |
| A273 | 帷间夜曲/支离轮光 | — |  |
| A274 | 兹白 | U | 安装包(Streaming):AssetBundles\blocks\00\07347193.blk |
| A275 | 那维莱特 | TU | 安装包(Streaming):AssetBundles\blocks\00\03104075.blk；安装包(Streaming):AssetBundles\blocks\00\07347193.blk |
| A276 | 朏魄含光 / 万世流涌大典 | — |  |
| A277 | 法尔伽 | TU | 安装包(Streaming):AssetBundles\blocks\00\02997937.blk；安装包(Streaming):AssetBundles\blocks\00\14221955.blk |
| A278 | 菲林斯 | U | 安装包(Streaming):AssetBundles\blocks\00\14221955.blk |
| A279 | 狼的武功歌/血染荒城 | — |  |
| A280 | 丝柯克 | U | 安装包(Streaming):AssetBundles\blocks\00\01248862.blk |
| A281 | 爱可菲 | TU | 安装包(Streaming):AssetBundles\blocks\00\01248862.blk；安装包(Streaming):AssetBundles\blocks\00\06350426.blk |
| A282 | 苍耀/香韵奏者 | — |  |
| A283 | 莉奈娅 | U | 安装包(Streaming):AssetBundles\blocks\00\01248862.blk |
| A284 | 恰斯卡 | U | 安装包(Streaming):AssetBundles\blocks\00\01248862.blk |
| A285 | 霜结的誓金枝/星鹫赤羽 | — |  |
| A286 | 菈乌玛 | U | 安装包(Streaming):AssetBundles\blocks\00\11206375.blk |
| A287 | 奈芙尔 | TU | 安装包(Streaming):AssetBundles\blocks\00\05275568.blk；安装包(Streaming):AssetBundles\blocks\00\11206375.blk |
| A288 | 纺夜天镜 / 真语秘匣 | — |  |
| A289 | 尼可 | TU | 安装包(Streaming):AssetBundles\blocks\00\05275568.blk；安装包(Streaming):AssetBundles\blocks\00\11206375.blk |
| A290 | 杜林 | T | 安装包(Streaming):AssetBundles\blocks\00\05275568.blk |
| A291 | 尘光七谕 / 黑蚀 | — |  |
| A292 | 洛恩 | U | 安装包(Streaming):AssetBundles\blocks\00\12287307.blk |
| A293 | 玛薇卡 | U | 安装包(Streaming):AssetBundles\blocks\00\12287307.blk |
| A294 | 焚曜千阳/灾悔 | — |  |
| A295 | 桑多涅 | U | 安装包(Streaming):AssetBundles\blocks\00\12287307.blk |
| A296 | 茜特菈莉 | TU | 安装包(Streaming):AssetBundles\blocks\00\06276706.blk；安装包(Streaming):AssetBundles\blocks\00\12287307.blk |
| A297 | 超越之匙/祭星者之望 | — |  |
| A298 | 哥伦比娅 | U | 安装包(Streaming):AssetBundles\blocks\00\11331095.blk |
| A299 | 雷电将军 | TU | 安装包(Streaming):AssetBundles\blocks\00\08993177.blk；安装包(Streaming):AssetBundles\blocks\00\11331095.blk |
| A300 | 帷间夜曲 / 薙草之稻光 | — |  |
| A301 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\08993177.blk；安装包(Streaming):AssetBundles\blocks\00\11331095.blk |
| A304 | （表中无） | U | 安装包(Streaming):AssetBundles\blocks\00\06977816.blk |
| A305 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\01720630.blk；安装包(Streaming):AssetBundles\blocks\00\06977816.blk |
| A307 | （表中无） | TU | 安装包(Streaming):AssetBundles\blocks\00\01720630.blk；安装包(Streaming):AssetBundles\blocks\00\06977816.blk |
| A310 | （表中无） | U | 热更新(Persistent):AssetBundles\blocks\00\13156026.blk |
| A311 | （表中无） | TU | 热更新(Persistent):AssetBundles\blocks\00\13156026.blk；热更新(Persistent):AssetBundles\blocks\00\16225192.blk |

## 非 A 系列

| 资源名 | 来源 |
|---|---|
| GachaShowPanel_N | 安装包(Streaming):AssetBundles\blocks\00\06961658.blk |
| GachaShowPanel_N | 安装包(Streaming):AssetBundles\blocks\00\07356235.blk |
| UI_Tab_UI_Tab_GachaShowPanel_N2 | 安装包(Streaming):AssetBundles\blocks\00\05275568.blk |
| UI_Tab_UI_Tab_GachaShowPanel_N3 | 安装包(Streaming):AssetBundles\blocks\00\14922153.blk |
| UI_Tab_UI_Tab_GachaShowPanel_N7 | 安装包(Streaming):AssetBundles\blocks\00\02186838.blk |
| GachaShowPanel_S | 安装包(Streaming):AssetBundles\blocks\00\12287307.blk |
| GachaShowPanel_S | 安装包(Streaming):AssetBundles\blocks\00\16483293.blk |

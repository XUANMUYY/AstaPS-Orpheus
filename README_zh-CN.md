# AstaPS-Orpheus

[English](README.md) · 简体中文 · [繁體中文](README_zh-TW.md)

一个基于 Grasscutter 的《原神》**7.1.0** 私人服务器。

> 这是一个研究与保存性质的项目，与 HoYoverse / miHoYo 没有任何从属、背书或关联，亦不作商业用途。

如果你能修复问题，请帮帮我。

## AstaPS-Orpheus 开发计划

AstaPS-Orpheus 基于 [MeChen618/AstaPS](https://github.com/MeChen618/AstaPS) 进行二次开发，保留并感谢上游项目及其贡献者的工作。

计划从零开始，用全新存档重新体验主线，记录并修复途中遇到的 Bug，逐步让这一版本更贴近原版的游戏体验。优先关注开局流程、任务引导、对话与过场触发，以及主线任务之间的衔接。这是后续开发目标，不代表目前已经完整实现全部主线。

后续会定期合并上游更新，根据具体冲突逐项整合自己的修改与上游修复，并在编译和游戏内验证后再部署。

### 体验与维护环境

维护者目前使用**国服 PC 客户端**体验和维护本项目，硬件配置如下：

- 显卡：NVIDIA GeForce RTX 5090 D，32 GB 显存
- 处理器：AMD Ryzen 9 9950X3D
- 内存：192 GB

这套配置较高，可能掩盖性能问题，因此部分性能缺陷可能在体验过程中被忽略。本机运行流畅，不代表中低配置设备上具有相同的性能或资源占用表现。欢迎其他配置的使用者反馈问题，并附上硬件配置和复现步骤。

### 当前本地改动

- 离线管理操作没有客户端会话时，跳过网络发包，避免空指针异常。
- 根据配置选择当前分区，不再硬编码 `os_usa`。
- 记录 HTTP 方法、路径和响应状态，便于排查请求；不记录请求体或查询参数。
- 为关闭任务系统时创建的存档提供显式的一次性开局补启动标记。实际剧情表现尚未验证，不能视为通用任务修复方案。

本仓库不包含本机账号测试配置、玩家存档、密码、客户端和资源包。下方原有编译与运行要求仍然适用。

## 需求

| | |
|---|---|
| Java | 编译需要 **JDK 21**，运行需要 **Java 21**。AstaPS 使用了虚拟线程等 Java 21 API。 |
| MongoDB | Community Server，必须在服务器启动前先跑起来。 |
| 游戏客户端 | 原神 7.1.0。官方客户端会校验 region 签名，要连私服需要另打客户端补丁，例如 [hk4e-patch-universal](https://github.com/MeChen618/patch)。AstaPS 本身不附带补丁。 |
| 资源文件 | 7.1.0 的资源包，解压到服务器目录下的 `resources/`。如果没有资源文件，可以通过[该链接](https://github.com/MeChen618/AstaPS-Resource)下载。 |

## 编译

编译前先确认两个 Java 命令都指向 21：

```
java -version
javac -version
```

然后执行：

```
./gradlew jar -PskipHandbook=1
```

`grasscutter-7.1.0.jar` 会生成在项目根目录。去掉 `-PskipHandbook=1` 可一并编译游戏内的手册；该步骤需要 NodeJS，没有就会失败。

Windows 上用 `.\gradlew.bat`。

## 运行

1. 启动 MongoDB。
2. 把 7.1.0 资源包放进 `resources/`。
3. 先运行一次 jar。它会生成 `config.json`，若缺少关键内容则退出。
4. 再启动一次。默认情况下，调度服务器监听 `8088`，游戏服务器监听 `22101`。
5. 把客户端指向调度服务器：[GrasscutterTools](https://github.com/jie65535/GrasscutterCommandGenerator) 自带代理，也可以用 Fiddler、mitmproxy 或客户端补丁。

## 命令

游戏内命令以 `/` 开头，服务器控制台不需要。输入 `help` 查看命令列表，`help <命令> [子命令...]` 查看语法和别名。控制台支持 Tab 补全，参数错误时会显示对应语法。

玩家选择器支持 `@UID`、`username@` 和 `username@UID`。账号管理仅限服务器控制台。

```text
help teleport pos
give 202 --amount 3 @10001
```

详细说明见 [CLI 迁移文档](docs/cli-picocli-migration.md)；插件开发参阅 [命令 API v5](docs/plugin-command-api-v5.md)。

## TPS 射击（7.1）

至冬的第三人称射击玩法可用。命令、TPS 秘境和尚未确定的部分见 [docs/tps/README.md](docs/tps/README.md)。

## 许可

以 **GNU General Public License v3.0** 发布。见 [`LICENSE`](LICENSE)。

`LICENSE-ClassGraph.txt` 不是本项目的许可。ClassGraph 是一个 MIT 许可的依赖，其编译后的类随 `grasscutter-7.1.0.jar` 一同分发，MIT 只要求其声明随附其中。

## 致谢

本服务器基于 **Grasscutter**。参考项目：**LunaGC**、**HunkyMeow**。

需求中链接的客户端补丁 [hk4e-patch-universal](https://github.com/MeChen618/patch) 是 [capyb2222/animegamepatch](https://github.com/capyb2222/animegamepatch) 的副本，由 **capyb2222** 维护，基于 [xeondev](https://git.xeondev.com/reversedrooms/hk4e-patch) 最初的 hk4e-patch 以及 [oureveryday](https://github.com/oureveryday/) 最初的 hk4e-patch-universal。它是独立项目，遵循其自身的 GPL-3.0 许可。

本仓库根目录的导入提交，按姓名列出了其承载作品的作者。

## 协议来源

协议定义来源于 [genshin-protocol](https://gitlab.com/kitkat-multiverse/genshin-protocol)。

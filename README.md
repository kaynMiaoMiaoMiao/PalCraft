# PalCraft

PalCraft 是一个面向 Minecraft 1.20.1 Fabric 的原创伙伴捕捉、养成、战斗协助、伙伴管理和据点自动化 Mod。

1.11.0 是第一个封档的基础玩法版本。当前功能包括捕捉球、多种伙伴、玩家伙伴仓库、召唤与收回、战斗协助、成长系统、元素技能、据点核心、据点伙伴仓库、据点部署、据点工作分配和可见的据点工作动作。

## AI 辅助开发说明

本项目大部分代码由 AI 辅助编写。项目所有者负责玩法方向、实现审查、游戏内测试和后续开发决策。

AI 生成代码在发布前需要继续审查，重点关注玩法正确性、性能、多人同步、授权合规以及 Minecraft/Fabric 更新兼容性。

## 项目方向

- 原创 Minecraft 伙伴捕捉玩法。
- 面向 Minecraft 1.20.1 的 Fabric Mod。
- 核心玩法逻辑以服务端为准。
- 按文档里程碑逐步开发。
- 使用原创资产、名称、伙伴、UI 和机制表达。

## 环境要求

- Java 17 或更高版本。
- 使用项目自带 Gradle Wrapper：`./gradlew`。

## 常用命令

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

构建后的 Mod jar 会生成在 `build/libs/`。

## 文档

开发计划和版本规划记录在：

[doc/palcraft-development-plan.md](doc/palcraft-development-plan.md)

[doc/palcraft-1.11-release-notes.md](doc/palcraft-1.11-release-notes.md)

[doc/palcraft-2.0-plan.md](doc/palcraft-2.0-plan.md)

## 许可证

PalCraft 使用 GNU General Public License v3.0 only。详见 [LICENSE](LICENSE)。

# PalCraft 模型转换排查记录

## 适用场景

这份记录用于后续维护 `tools/convert_bbmodel_to_geckolib.js`，尤其是把 ModelEngine / MCPets / MythicMobs 来源的 `.bbmodel` 转成 GeckoLib `geo.json`、`animation.json` 和贴图资源时。

如果游戏内模型看起来缺块、散架、四七八歪，但动画本身还能播放，优先按本文流程检查转换结果。

## 2026-07-07 修复记录

### 现象

游戏内动作基本能播放，但实体模型本体明显缺件和错乱。Blockbench 打开原始 `.bbmodel` 时模型正常，打开转换后的 `geo.json` 时细节明显缺失。

抽样对比发现旧转换结果大量丢 cube：

| 模型 | 源 `.bbmodel` 元素数 | 旧 `geo.json` cube 数 | 修复后 `geo.json` cube 数 |
| --- | ---: | ---: | ---: |
| dodo | 55 | 27 | 54 |
| ram | 98 | 43 | 97 |
| leopard | 66 | 47 | 65 |
| lizard | 74 | 46 | 73 |
| moth | 44 | 12 | 43 |

修复后少的 1 个通常是 `hitbox`，转换器会故意跳过它，不属于模型缺失。

### 根因

源资源的 `meta.model_format` 是 `free`，不是 Bedrock Entity 原生格式。这套 ModelEngine 资产大量使用 0 厚度但有面积的平面元素表示羽毛、毛发、耳朵、翅膀等细节。

旧转换器在 `convertCube` 中使用了类似下面的规则：

```js
if (size.some((entry) => Math.abs(entry) < 0.001)) {
  return null;
}
```

这个判断会把任意一个维度为 0 的元素全部丢掉。对普通实体来说，这不是退化元素，而是合法的平面装饰，所以转换后的模型缺了大量关键部件。

### 修复规则

`convertCube` 现在只跳过少于 2 个有效维度的元素：

```js
const nonZeroDimensions = size.filter((entry) => Math.abs(entry) >= 0.001).length;
if (nonZeroDimensions < 2) {
  return null;
}
```

含义：

- 3 个有效维度：正常 cube，保留。
- 2 个有效维度：0 厚度平面，有面积，保留。
- 0 或 1 个有效维度：线或点，没有可渲染面积，跳过。

后续修改转换器时，不要恢复成“任一维度为 0 就跳过”的逻辑。

## 推荐排查流程

### 1. 用 Blockbench 看原始模型

先打开源 `.bbmodel`，确认资产本身是否正常。示例源目录：

```text
E:\Desktop\MC\坐骑包\ModelEngine\blueprints
```

如果原始 `.bbmodel` 在 Blockbench 中正常，而游戏内异常，优先怀疑转换器或 GeckoLib 渲染资源。

### 2. 对比源元素数和导出 cube 数

可以用 Node 统计源元素数和 `geo.json` 中实际 cube 数。重点看是否出现大比例丢失。

示例检查目标：

```text
src/main/resources/assets/palcraft/geo/entity
```

经验判断：

- `geo.json` cube 数接近源元素数，通常是正常的。
- 差 1 个一般是跳过 `hitbox`。
- 少几十个时，通常是转换器过滤逻辑出错。

### 3. 用 Blockbench 打开转换后的 `geo.json`

把转换后的 `geo.json` 作为 Bedrock Entity 几何导入 Blockbench。注意不要叠加到已经打开的原始模型上，必须清空场景或新建项目后再导入，否则截图会出现重影，不能作为判断依据。

导入后检查：

- cube 数是否接近源元素数。
- 羽毛、耳朵、毛发、翅膀等平面细节是否存在。
- 模型整体姿态是否和原始 `.bbmodel` 接近。

### 4. 重新生成资源

转换器修复后，从源 `.bbmodel` 重新生成所有相关资源：

```powershell
node tools\convert_bbmodel_to_geckolib.js <input.bbmodel> palcraft <entity_name> src\main\resources
```

本次修复重新生成了成年体、幼体和蛋的 `geo/entity/*.geo.json`、`animations/entity/*.animation.json` 和 `textures/entity/*.png`。

### 5. 构建验证

至少运行：

```powershell
gradle build
```

本次修复验证结果：

```text
BUILD SUCCESSFUL
```

### 6. 游戏内验证

最终仍需进游戏看实体。建议检查：

- Dodo / Ram / Leopard / Lizard / Moth 成年体。
- 对应幼体和蛋模型。
- idle / walk / fly / attack 等动画是否仍然正常。
- 平面细节在不同角度下是否可见。

如果模型已经完整但游戏内仍然姿态异常，再继续检查坐标轴、骨骼 pivot、父子层级和动画 pose offset，不要先改过滤 0 厚度平面的逻辑。

## 注意事项

- `hitbox` 组不应作为可见骨骼导出。
- 0 厚度平面不是错误数据，ModelEngine 资产经常依赖它做视觉细节。
- 只靠游戏内截图很难定位是动画问题还是几何丢失，必须同时对比原始 `.bbmodel` 和导出的 `geo.json`。
- 批量重新转换会重写 JSON 文件，review 时要重点确认 cube 数、骨骼名、动画映射和贴图路径，而不是只看 diff 行数。

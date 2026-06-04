# `Canvas.drawLine` 支持任意 `Brush` 的技术方案（UIView 渲染后端）

> 作用范围：仅限 `RenderBackend = UIView` 的 V3 渲染管线
> （对应文件夹 `PictureRecorderV3`，相关入口 `TMMCanvasViewProxyV3.mm` / `TMMCanvasLayerDrawerV3.m`）。
> Skia 后端走独立通路，本方案不涉及。

---

## 1. 背景与现状

### 1.1 问题现象

在 V3 后端下，对 `drawLine` 使用 `Brush.sweepGradient`、`Brush.radialGradient`、`Brush.imageShader` 等
**非 LinearGradient** 的 `Brush` 时，会直接崩溃：

```
*** -[TMMNativeSweepGradientShader from]: unrecognized selector sent to instance ...
```

调用栈起点位于：

- `AdaptiveCanvas.drawLine` (Kotlin) →
- `TMMCanvasViewProxyV3 -drawLine:...:paint:` →
- `TMMCALayerDrawLineV3(...)` →
- `[(TMMNativeLineGradientLayer *)layerForDrawing drawWithPointX1:...:shader:(TMMNativeLinearGradientShader *)shader ...]`

### 1.2 复现方法

#### 1.2.1 环境

| 项 | 取值 |
|---|---|
| Sample 工程 | `compose-multiplatform-sample/composeApp` |
| 启动 target | iOS app（真机或模拟器均可，iOS 12+） |
| RenderBackend | **必须是 `UIView`**（`Skia` 不复现；详见 [TMMComposeUIViewControllerConfiguration] 中 `renderBackend = .uiView`） |
| 入口页面 | `BrushDemo()`（`composeApp/src/commonMain/kotlin/dynamic/samples/canvas/BrushDemo.kt`） |

#### 1.2.2 操作步骤

1. 打开 sample 工程，确认 `ComposeUIViewController` 的配置走 `RenderBackend.UIView`
   （sample 默认就是 UIView，不需要改；如果之前手动切到 Skia，要切回来）。
2. 编译并启动到设备/模拟器。
3. 在 sample 首页路由到 `BrushDemo`（菜单里"Canvas / Brush"）。
4. 滚动到 **"SolidColor + drawLine(Brush)"** 这一段，让最右侧 tile **"drawLine(sweep)"** 进入屏幕参与首次绘制。
5. App 立即崩溃，控制台堆栈与 §1.1 一致：
   `-[TMMNativeSweepGradientShader from]: unrecognized selector ...`

#### 1.2.3 最小复现代码（已存在于 BrushDemo.kt:208 起）

```kotlin
BrushTile(label = "drawLine(sweep)") {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676),
            ),
            center = Offset(60f, 60f),
        )
        drawLine(
            brush = brush,
            start = Offset(15f, 15f),
            end = Offset(115f, 115f),
            strokeWidth = 16f,
        )
    }
}
```

> 进一步缩小到独立单元的复现：把上面的 Canvas 直接放进任意 `setContent { ... }`
> 入口的根 Composable 即可，无需依赖 sample 的 LazyColumn 结构。

#### 1.2.4 变体（用于覆盖修复后的回归面）

把上述代码里的 `brush =` 换成下面任一项，预期**修复前**全部 crash、**修复后**全部正常显示：

```kotlin
// radial — 当前同样 crash（selector: from / to / radius 视类做法略有不同）
val brush = Brush.radialGradient(
    colors = listOf(Color.White, Color(0xFF1565C0)),
    center = Offset(60f, 60f),
    radius = 70f,
)

// imageShader — 当前同样 crash
val brush = ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
```

以及对每种 brush 叠加：

- `start/end` 为水平、垂直、斜线三种朝向
- `strokeCap = StrokeCap.Butt / Round / Square`
- 极端：`start == end`（零长度，预期"什么都不画"，不能 crash）

#### 1.2.5 反向验证

把同一份代码切到 `RenderBackend.Skia`，sweep/radial/image 三种 brush 的 drawLine 都应该**正常渲染**，
这能反向证明 bug 仅在 V3 UIView 通路上（确认问题边界，避免误改 Kotlin 公共代码）。

### 1.3 根因分析（结构性问题，非 V3 引入）

通过翻阅历史，本问题在三代实现中的演进如下：

| 版本 | 入口实现 | 对 `[paint shader]` 的处理 |
|---|---|---|
| **V1** （`TMMComposeAdaptivedCanvasLayer+AdaptivedCanvas.m`，已被 commit `6d8b6414e2f` 删除） | `-drawLine:...:paint:` 内部只读 `lineColor / strokeWidth / strokeCap`，完全不读 `[paint shader]` | sweep / radial / image 会被静默降级为实色，**不崩但渲染错误** |
| **V2 第一版** （`TMMNativeLineLayer.m` 内部） | 首次引入 `drawWithPointX1:...:shader:` 入口，**每次绘制都 `[CAGradientLayer layer]` 并 `addSublayer:`**，签名硬转 `TMMNativeLinearGradientShader *` | sweep/radial/image **crash**，且每帧泄漏 sublayer |
| **V2 修复版** （commit `4c823c4768b` 2025-03-17 helenyan, *"fix(): Fix drawLine with shader."*） | 把 line+shader 的逻辑从 `TMMNativeLineLayer` 抽成独立的 `TMMNativeLineGradientLayer : CAGradientLayer`，并把它纳入复用池 `TMMNativeDrawingTypeShaderLine` | 修掉了 sublayer 泄漏，但 **shader 类型分发的问题原样保留**——`(TMMNativeLinearGradientShader *)shader` 这个硬转、以及"只接受 axial gradient"的 layer 实现都被照搬过来了 |
| **V3** （`PictureRecorderV3/TMMCanvasLayerDrawerV3.m`，当前活跃版本） | 完全照搬 V2 修复版的实现 | bug 延续，未修复 |

#### 为什么 helenyan 没顺手改对？——commit `4c823c4768b` 考古

这次 fix 的真正动机是**修 sublayer 泄漏**，而不是补齐 shader 多类型支持，证据：

1. commit message 是 `Fix drawLine with shader.`，没提"扩展更多 brush"，从工作量上也只动了 line 这一条线。
2. diff 中 `TMMNativeLineLayer` 旧实现里赫然写着：
   ```objc
   CAGradientLayer *gradientLayer = [CAGradientLayer layer];   // 每次 drawLine 都新建
   ...
   [self addSublayer:gradientLayer];                            // 每次 drawLine 都 addSublayer
   gradientLayer.mask = shapeLayer;
   ```
   而 `TMMNativeLineLayer` 自身被复用池缓存，于是每帧每条 shader line 都会让它多挂一层 `CAGradientLayer`，**线性内存/层级泄漏**。helenyan 把这段逻辑搬出去单独包成 `TMMNativeLineGradientLayer`（自身就是 `CAGradientLayer`），并在 `TMMNativeDrawingLayerClass.m` 的 `TMMDrawingLayerClassFromType` 里把 `TMMNativeDrawingTypeShaderLine → TMMNativeLineGradientLayer` 注册进复用池——这就根治了泄漏。
3. 但她**完全没动 shader 的类型适配**：`TMMNativeLineLayer` 旧代码里的 `(TMMNativeLinearGradientShader *)` 硬转、以及那段只处理 horizontal/vertical/linear 三种"轴向 case"的逻辑，被一字不改地搬到了新 layer 里。也就是说她**继承了原作者关于"line shader == LinearGradient"的隐含假设**，没意识到这个假设本身是错的。
4. 当时 `TMMNativeComposeGradientLayer.applyShader:` 已经存在 7 个月（2024-08-27 引入，commit `13c011c676b`），**完全可以直接复用**——但 helenyan 选择了"最小改动止血"路线：先把对外的 layer class 类型从 `TMMNativeLineLayer` 改名成 `TMMNativeLineGradientLayer`，方便用复用池接住，至于"颜色/纹理来源还能不能扩展"被推到了未来。

> 一句话总结：**`(TMMNativeLinearGradientShader *)` 这个硬转是 V2 第一版作者写的，helenyan 在修泄漏 bug 时没意识到这是 bug，所以一路平移到了 V3。** 不是"明知有 `TMMNativeComposeGradientLayer` 还要重复造轮子"——是修 bug 时只盯着泄漏路径，没回头审视类型分发。

也就是说：

- 真正的 bug 在 **V2 第一版引入"line + shader"能力时**就已经埋下，并非 V3 / 删除 V2 / helenyan 修复时引入。
- 之所以一直未暴露，是因为业务里对 `Canvas.drawLine` 几乎只使用 `Brush.linearGradient` 或单色；
  本次在 `BrushDemo` 中专门为 `drawLine` 接入 `sweepGradient` 才把它打了出来。
- helenyan 的 fix 是**正确但不完整的修补**：止住了内存泄漏，但 shader 类型分发的债务延续至今。本方案就是来还这部分债务的。

### 1.4 复用池绑定的硬伤

`OVComposePictureRecorder.mm` 中 `TMMDrawingLayerClassFromTypeV2` 把 drawingType 与 layer class 写死：

```objc
case TMMNativeDrawingTypeLine:        return [TMMNativeLineLayer class];
case TMMNativeDrawingTypeShaderLine:  return [TMMNativeLineGradientLayer class];
```

而 `TMMNativeLineGradientLayer : CAGradientLayer` 的 `drawWithPointX1:...:shader:` 只接受
`TMMNativeLinearGradientShader *`，且内部仅按 `axial` 渐变实现，**不具备承载 sweep/radial/image 的能力**。

因此问题的本质是：**"Line + Shader" 这一路只为 LinearGradient 设计，复用池映射、入口函数签名、Layer 实现三处共同锁死了能力**。

### 1.5 对照：drawRect / drawCircle / drawPath 是怎么解决的

```objc
case TMMNativeDrawingTypeShaderRect:
case TMMNativeDrawingTypeShaderCircle:
case TMMNativeDrawingTypeShaderPath:
    return [TMMNativeComposeGradientLayer class];   // 通用层
```

`TMMNativeComposeGradientLayer.applyShader:` 内部已经用 `isKindOfClass:` 完整分发了 4 种 shader：

```objc
- (void)applyShader:(TMMNativeBasicShader *)shader {
    if ([shader isKindOfClass:[TMMNativeLinearGradientShader class]]) { ... self.type = kCAGradientLayerAxial;  ... }
    else if ([shader isKindOfClass:[TMMNativeRadialGradientShader class]]) { ... self.type = kCAGradientLayerRadial; ... }
    else if ([shader isKindOfClass:[TMMNativeSweepGradientShader  class]]) { ... self.type = kCAGradientLayerConic;  ... }
    else if ([shader isKindOfClass:[TMMNativeImageShader          class]]) { ... self.contents = ... }
}
```

这就是 line 改造后要对齐的"标准答案"。

---

## 2. 设计目标

1. **功能完整性**：`Canvas.drawLine` 在 V3 后端下支持四类 `Brush`：
   - `Brush.linearGradient` / `Brush.horizontalGradient` / `Brush.verticalGradient`
   - `Brush.radialGradient`
   - `Brush.sweepGradient`
   - `ShaderBrush(ImageShader(...))`（按 tile 重复）
2. **视觉对齐**：与 Skia 后端 / Android 上的 `Canvas.drawLine(brush)` 保持一致——
   非线性渐变的语义是"先按 brush 在 line 的 bounding box 内填充，再用 line 的形状做 mask"。
3. **零回归**：纯色 line、LinearGradient line 的现有效果 / 性能 / 复用池命中率不下降。
4. **健壮性**：未知 shader 类型不再 crash，应有兜底（降级为实色或忽略 shader）。
5. **不污染 Kotlin 侧**：所有改动局限在 OC 端的 V3 渲染目录与对应 layer。

---

## 3. 可行性论证

线段在 CALayer 体系下，可以抽象为：

```
带渐变的"形状层" = (gradient layer，承载颜色/纹理) + (mask = 沿线段的 stroke 路径)
```

这一思路在 `TMMNativeLineGradientLayer` 当前实现里就已经体现：
它本身是 `CAGradientLayer`，内部挂了一个 `CAShapeLayer` 作为 mask 来"切出线段的形状"。

只要把"颜色/纹理来源"从 LinearGradient 扩展为通用 `applyShader:`，
而把"stroke 路径 + lineCap + bounds 计算"作为一段公共形状逻辑保留下来，就能完整覆盖四类 shader：

| Shader | 颜色源（gradient layer 配置） | mask | 是否可行 |
|---|---|---|---|
| Linear | `type = axial`，`startPoint/endPoint` 走 `[from, to]` 归一化 | 线段 stroke path | ✅ 已有 |
| Radial | `type = radial`，`startPoint = center`，`endPoint = center + radius` | 线段 stroke path | ✅ |
| Sweep  | `type = conic`（iOS 12+），`startPoint = center` | 线段 stroke path | ✅ |
| Image  | `self.contents = tiled image`，`colors/locations = nil` | 线段 stroke path | ✅ |

> Sweep 在 iOS < 12 不可用，与现有 `TMMNativeComposeGradientLayer` 保持一致地走 `@available(iOS 12.0, *)` 守护，旧系统降级为实色或不显示，与现有 rect/circle 行为对齐。

---

## 4. 总体方案

整体思路：**把 line 的 shader 通路统一到通用渐变层 `TMMNativeComposeGradientLayer`**。

不再依赖只支持 Linear 的 `TMMNativeLineGradientLayer`；
通过 `applyShader:` 复用 rect/circle/path 已经验证好的多 shader 分发逻辑；
"线段形状"通过给 layer 设置一个 `CAShapeLayer mask` 来表达。

### 4.1 复用 vs 新增 layer

两条候选：

- **A. 复用 `TMMNativeComposeGradientLayer`**（推荐）
  优点：完全跟 rect/circle/path 同构，shader 适配代码 0 增量；
  唯一需要补的是"如何用 mask 把它裁成 line 形状"，可以在 drawer 函数里就地完成。
- B. 在 `TMMNativeLineGradientLayer` 上扩展，使其也能切换 `type`（axial/radial/conic）+ contents
  优点：保留 line 专用 layer；缺点：与 `TMMNativeComposeGradientLayer` 出现两份等价代码，维护成本高，且复用池 `TMMNativeDrawingTypeShaderLine` 仍只能映射一个类，逻辑不统一。

**结论：采用方案 A**。

### 4.2 数据流

```
AdaptiveCanvas.drawLine(brush)
        │
        ▼
TMMCanvasViewProxyV3.drawLine:...:paint:
        │  drawingType = shader ? ShaderLine : Line
        ▼
PictureRecorder 分配 / 复用 layer
        │  ShaderLine -> [TMMNativeComposeGradientLayer class]   ← 修改点 ①
        ▼
TMMCALayerDrawLineV3
        │  ── shader == nil  → 走 TMMNativeLineLayer 老路（不变）
        │  ── shader != nil  → ② 在 layer 上配置 frame / mask / applyShader:
        ▼
渲染
```

---

## 5. 详细改动点

### 5.1 改动 ①：复用池映射改写

文件：`PictureRecorderV3/OVComposePictureRecorder.mm`
函数：`TMMDrawingLayerClassFromTypeV2`

```diff
case TMMNativeDrawingTypeShaderRect:
case TMMNativeDrawingTypeShaderCircle:
case TMMNativeDrawingTypeShaderPath:
+case TMMNativeDrawingTypeShaderLine:
    return [TMMNativeComposeGradientLayer class];
case TMMNativeDrawingTypeLine:
    return [TMMNativeLineLayer class];
-case TMMNativeDrawingTypeShaderLine:
-    return [TMMNativeLineGradientLayer class];
```

> V2 旧版的 `PictureRecorderV2/TMMUIKitPictureRecorder.mm` 中相同位置已经随 V2 通路在 `e70308503ed` 中下线；
> 如未来还需保留 V2 路径，应做同步修改，但当前主线只需改 V3 的这处。

### 5.2 改动 ②：`TMMCALayerDrawLineV3` 重写 shader 分支

文件：`PictureRecorderV3/TMMCanvasLayerDrawerV3.m`
函数：`TMMCALayerDrawLineV3`

伪代码：

```objc
if (!shader) {
    // 走 TMMNativeLineLayer 老路（保持不变）
    ...
} else {
    TMMNativeComposeGradientLayer *gradientLayer = (TMMNativeComposeGradientLayer *)layerForDrawing;

    // 1. 计算线段的 bounding box（含 stroke 半径外扩，保持与原 LineGradientLayer 一致）
    const CGFloat density   = ...;
    const CGFloat strokeW   = [paint strokeWidth] / density;
    const CGFloat r         = strokeW / 2.0;
    const CGFloat lineLen   = hypot(pointX2 - pointX1, pointY2 - pointY1);
    if (lineLen < FLT_EPSILON) {
        gradientLayer.mask     = nil;
        gradientLayer.colors   = nil;
        gradientLayer.contents = nil;
        return;
    }
    const CGFloat cosA = fabs(pointX2 - pointX1) / lineLen;
    const CGFloat sinA = fabs(pointY2 - pointY1) / lineLen;
    const CGFloat offsetX = r * sinA;
    const CGFloat offsetY = r * cosA;

    // 2. 构造 layerFrame（在原 layerFrame 基础上做外扩，保证 cap 不被裁掉）
    CGRect expandedFrame = CGRectInset(layerFrame, -offsetX, -offsetY);
    CALayerApplyCoordinatesInfo(gradientLayer, saveState, density, expandedFrame);

    // 3. 配置 shader（复用通用分发）
    [gradientLayer applyShader:shader];

    // 4. 用一个 CAShapeLayer 作为 mask，沿线段画 stroke 路径
    CAShapeLayer *maskLayer = (CAShapeLayer *)gradientLayer.mask;
    if (![maskLayer isKindOfClass:[CAShapeLayer class]]) {
        maskLayer = [CAShapeLayer layer];
        gradientLayer.mask = maskLayer;
    }
    UIBezierPath *path = [UIBezierPath bezierPath];
    [path moveToPoint:CGPointMake((pointX1 - MIN(pointX1, pointX2)) / density + offsetX,
                                  (pointY1 - MIN(pointY1, pointY2)) / density + offsetY)];
    [path addLineToPoint:CGPointMake((pointX2 - MIN(pointX1, pointX2)) / density + offsetX,
                                     (pointY2 - MIN(pointY1, pointY2)) / density + offsetY)];
    maskLayer.path        = path.CGPath;
    maskLayer.lineWidth   = strokeW;
    maskLayer.fillColor   = UIColor.clearColor.CGColor;
    maskLayer.strokeColor = UIColor.blackColor.CGColor;        // mask 只看 alpha
    maskLayer.lineCap     = TMMConvertStrokeCap([paint strokeCap]);
    maskLayer.frame       = gradientLayer.bounds;
}
```

要点：

- 不再调用 `TMMNativeLineGradientLayer drawWithPointX1:...:shader:`；该旧入口最终可考虑删除。
- `mask` 用 `CAShapeLayer`，`stroke` 路径决定线段的视觉形状，颜色/纹理交由 `applyShader:`。
- `applyShader:` 内部会按 shader 子类正确切换 `self.type` 与 `colors / startPoint / endPoint / contents`，**自动支持** Linear/Radial/Sweep/Image。

### 5.3 改动 ③：`applyShader:` 在"非 Linear 时"的语义对齐

`TMMNativeComposeGradientLayer.applyShader:` 当前实现里：

- Linear / Radial / Sweep 的 `colors` / `startPoint` / `endPoint` 都是按 layer 自身的 `frame.size` 与 `density` 归一化来计算的；
- 改造后 line 用例下，layer 的 `frame.size` 是"line 的 bounding box（含外扩）"，与 Skia 在 `drawLine(brush)` 时使用的"line 的局部坐标系"语义等价。

需要补充验证的边界：

- 线段为水平/垂直时，外扩方向只在一个轴向，sweep/radial 的中心点（`shader.center`）落在 box 内的归一化坐标是否仍然正确——预期与 rect 用例一致，已被 `applyShader:` 内部公式覆盖。
- 当 `shader.from == shader.to` 等退化情况，复用现有 rect 分支已有的兜底（`MIN(.../layerSize.x, 1)`）。

> 若发现某些极端 brush 在 line bbox 上视觉位置与 Skia 不一致，可以在 `TMMCALayerDrawLineV3` shader 分支里
> 根据 line 长度方向给 `applyShader:` 包一层"先变换坐标"的辅助函数，但不在第一版纳入；
> 第一版目标是"不崩、能正确覆盖 4 种 shader"。

### 5.4 改动 ④：`TMMNativeLineGradientLayer` 的去留

- **第一版保留**：因为外部可能还有 V2 旧通路或测试代码引用（已确认只在 V2 已删通路里用），但实际 V3 不再使用。
- **后续清理**：在 V3 通路稳定后，可独立提一个 PR 删除：
  - `TMMNativeLineGradientLayer.h/.m`
  - 工程文件 `project.pbxproj` 中的引用
  - 头文件 import `TMMNativeLineGradientLayer.h` 处

---

## 6. 复用池与 hash 的影响

`TMMCanvasViewProxyV3.drawLine:` 中使用：

```objc
const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderLine : TMMNativeDrawingTypeLine;
```

并参与 `hashMerge(TMMNativeDataHashFromPaint(paint), drawingType)` 的 hash。
本方案 **保留** `TMMNativeDrawingTypeShaderLine` 这一类型，不新增枚举：

- 优点：drawingType 的二进制 ABI 不变；KMM Kotlin 侧 / 业务侧无任何影响。
- 复用池中 ShaderLine 类型对应 layer 类从 `TMMNativeLineGradientLayer` 改为 `TMMNativeComposeGradientLayer`，
  **首次发布时需要清空一次 layer pool**——`PictureRecorderProps::prepareForReuse` 在每帧重置时会将 `layerPool` 清空，
  发布期就完成了自然过渡，不需要额外迁移代码。

**潜在风险点**：
若 `TMMNativeDataHashFromPaint` 没有把 shader 的"具体子类型"卷入哈希，则不同种类 shader 的同位置 line
可能命中同一个复用 cell。这本身**不是 bug**（每帧都会重新 `applyShader:`），但需要确认；
若发现命中后 `applyShader:` 内部 `if (self.shader != shader)` 早返回造成残留，需要在 line 分支强制重置。
当前 `applyShader:` 的提前返回逻辑是基于 **shader 对象指针** 比较，跨帧时 shader 实例会变化，安全可控。

---

## 7. 兼容性 / 降级 / 回退

| 维度 | 处理 |
|---|---|
| iOS 11 及以下（无 `kCAGradientLayerConic`） | 复用 `TMMNativeComposeGradientLayer.applySweepGradientShader:` 已有 `@available(iOS 12.0, *)` 守护，行为与 rect 一致：在旧系统上 sweep gradient 不显示，**不会 crash**。 |
| Image shader 非 Repeat tile mode | 复用 `applyRepeatImage:shader:` 现有判断，不支持时静默不绘（与 rect 一致）。 |
| 未来出现新的 `TMMNativeBasicShader` 子类 | `applyShader:` 已经天然兜底（不命中任何分支即不绘 shader），不会 crash。 |
| 配置开关 / 实验位 | 可考虑在 `OVComposeExperimentalConfig` 上加一个 `lineShaderRouteVersion`（默认新版），
                    异常时让外部回滚到老路径（"shader=nil 同款实色"）；非必需，可延迟到出问题再加。 |

---

## 8. 工作量拆解

| # | 修改 | 文件 | 估时 |
|---|---|---|---|
| 1 | 复用池映射调整（ShaderLine → ComposeGradientLayer） | `OVComposePictureRecorder.mm` | 5 min |
| 2 | `TMMCALayerDrawLineV3` shader 分支重写 | `TMMCanvasLayerDrawerV3.m` | 1 h |
| 3 | 提取 line stroke path / cap 转换工具方法 | 同上（小重构） | 30 min |
| 4 | 自测 4 种 brush + 横/竖/斜线 + StrokeCap (Butt/Round/Square) | demo `BrushDemo.kt` | 1 h |
| 5 | 与 Skia 后端做截图对比 | sample 工程 | 30 min |
| 6 | （可选）下线 `TMMNativeLineGradientLayer` | 文件 + pbxproj | 30 min |

总计：约 **半天**。

---

## 9. 验证清单

测试用例（建议直接放进 `composeApp/.../canvas/BrushDemo.kt`）：

1. `drawLine(brush = Brush.linearGradient(...))` —— 横线 / 竖线 / 斜线
2. `drawLine(brush = Brush.horizontalGradient(...))`
3. `drawLine(brush = Brush.verticalGradient(...))`
4. `drawLine(brush = Brush.radialGradient(...))`
5. `drawLine(brush = Brush.sweepGradient(...))` ← **本次崩溃用例**
6. `drawLine(brush = ShaderBrush(ImageShader(bitmap, TileMode.Repeated, ...)))`
7. 上述每种 brush 配 `StrokeCap.Butt / Round / Square`
8. 极端：`p1 == p2`（零长度）/ 透明色 brush / `strokeWidth = 0`

每一项验证：
- 不 crash；
- 与 Skia 后端 / Android 截图视觉一致（允许 1px 抗锯齿差异）。

---

## 10. 与本次 crash 的关系

- **快速止血**：可以先用最小改动在 `TMMCALayerDrawLineV3` 的 shader 分支前面加
  `if (![shader isKindOfClass:[TMMNativeLinearGradientShader class]]) { /* fallback to LineLayer with paint color */ }`
  立即消除 crash；这是**临时方案**，不在本文档范围内，由独立 hotfix 提交。
- **本方案**是真正"补齐能力"的方案，目的是让 V3 后端的 `drawLine` 与 Android/Skia 在 brush 维度上对齐。

---

## 11. 参考实现指引（节选）

- 通用渐变层多 shader 分发：[TMMNativeComposeGradientLayer.m](../../Layers/TMMNativeComposeGradientLayer.m) `-applyShader:`
- 旧版 line gradient layer（仅供阅读，本方案后续可下线）：[TMMNativeLineGradientLayer.m](../../Layers/TMMNativeLineGradientLayer.m)
- drawRect shader 分支：[TMMCanvasLayerDrawerV3.m](TMMCanvasLayerDrawerV3.m) 中 `TMMCALayerDrawRectV3`
- drawCircle shader 分支：同上 `TMMCALayerDrawCircleV3`
- 复用池映射：[OVComposePictureRecorder.mm](OVComposePictureRecorder.mm) 中 `TMMDrawingLayerClassFromTypeV2`

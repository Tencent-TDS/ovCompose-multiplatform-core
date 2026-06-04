import { RenderNode, FrameNode, NodeController, DrawContext, ShapeClip, Matrix4, Rect, Position,
  FrameCallback, RoundRect,
  BuilderNode,
  NodeContent,
  Size} from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { displaySync } from '@kit.ArkGraphics2D';
import { JsNodeDraw } from 'libentry.so';
import { InternalArkUIViewController } from 'libcompose_arkui_utils.so';
import { ArkUINativeView, ArkUIViewArkUIBacked as ArkUIView } from './ArkUIViewArkUIBacked';

interface ComposeRenderNode {
  dispose(): void

  reSize(size: SizeResult): void
}

class RenderFrameCallback extends FrameCallback {
  renderNodeUIView :InternalArkUIViewController | null = null

  onFrame(frameTimeNanos: number) {
    this.renderNodeUIView?.onFrame(frameTimeNanos)
  }

  onIdle(timeLeftInNano: number): void {
    this.renderNodeUIView?.onIdle(timeLeftInNano)
  }
}

export class RenderFrameManager {
  private frameCallback = new RenderFrameCallback()
  private context: UIContext
  private sync: displaySync.DisplaySync
  private nodeIdSet = new Set<number>()
  private isSyncStart = false
  private isDisposed = false

  constructor(context: UIContext, sync: displaySync.DisplaySync) {
    this.context = context
    this.sync = sync
    this.sync.setExpectedFrameRateRange({ expected: 120, min: 0, max: 120 })
  }

  setCallbackController(UIView :InternalArkUIViewController) {
    this.frameCallback.renderNodeUIView = UIView
  }

  postFrameCallback() {
    if (this.isDisposed) {
      return
    }
    this.context.postFrameCallback(this.frameCallback)
  }

  enableFrameCallback(id: number) {
    if (this.isDisposed) {
      return
    }
    this.nodeIdSet.add(id)
    if (!this.isSyncStart) {
      this.sync.start()
      this.isSyncStart = true
    }
  }

  disableFrameCallback(id: number) {
    if (this.isDisposed) {
      return
    }
    this.nodeIdSet.delete(id)
    if (this.isSyncStart && this.nodeIdSet.size == 0) {
      this.sync.stop()
      this.isSyncStart = false
    }
  }

  buildView(name: string, parameter: Object): ArkUIView | null {
    if (!this.context) {
      return null
    }
    let arkUIView = new ArkUIView(this.context, name, parameter)
    return arkUIView
  }

  buildNativeView(): ArkUINativeView {
    return new ArkUINativeView(this.context)
  }

  onDispose() {
    if (this.isDisposed) {
      return
    }
    this.isDisposed = true
    this.frameCallback.renderNodeUIView = null
    if (this.isSyncStart) {
      this.sync.stop()
      this.isSyncStart = false
    }
    this.nodeIdSet.clear()
  }

}

@Builder
export function ContentBuilder(rootSlot: NodeContent) {
  Stack() {
    ContentSlot(rootSlot)
  }
  .height('auto')
  .width('auto')
}

export class ComposeNodeController extends NodeController {
  private controller: InternalArkUIViewController | null = null
  private rootNode: FrameNode | null = null;
  private composeNode: ComposeRenderNode | null = null;
  private enableCApi: boolean = false
  private rootContent: NodeContent | null = null
  private nodeBuilder: BuilderNode<[NodeContent]> | null = null

  constructor(enableCApi: boolean, rootContent: NodeContent | null) {
    super();
    this.enableCApi = enableCApi
    this.rootContent = rootContent
  }

  makeNode(uiContext: UIContext): FrameNode | null {
    if (this.enableCApi && this.rootContent !== null) {
      this.nodeBuilder = new BuilderNode(uiContext)
      this.nodeBuilder.build(wrapBuilder(ContentBuilder), this.rootContent)
      this.composeNode = new ComposeCRenderNode(this.controller as InternalArkUIViewController)
      return this.nodeBuilder.getFrameNode()
    }
    this.rootNode = new FrameNode(uiContext)
    const rootRenderNode = this.rootNode.getRenderNode();
    if (rootRenderNode !== null) {
      this.composeNode = new ComposeJsRenderNode(this.controller as InternalArkUIViewController)
      rootRenderNode.appendChild(this.composeNode as ComposeJsRenderNode);
    }
    return this.rootNode
  }

  setController(controller: InternalArkUIViewController) {
    this.controller = controller
  }

  aboutToResize(size: Size): void {
    this.composeNode?.reSize({
      width: vp2px(size.width),
      height: vp2px(size.height)
    })
  }

  aboutToDisappear() {

  }

  onDestroy() {
    if (this.composeNode) {
      if (this.composeNode instanceof  ComposeJsRenderNode) {
        this.composeNode.clearChildren()
        this.rootNode?.getRenderNode()?.clearChildren()
        this.rootNode?.clearChildren()
      }
      this.composeNode.dispose()
      this.composeNode = null
    }
  }
}

export class ComposeCRenderNode implements ComposeRenderNode {
  private renderNodeUIView: InternalArkUIViewController | null
  constructor(renderNodeUIView: InternalArkUIViewController) {
    this.renderNodeUIView = renderNodeUIView
  }
  dispose(): void {
    if (this.renderNodeUIView) {
      this.renderNodeUIView = null
    }
  }

  reSize(size: SizeResult): void {
    this.renderNodeUIView?.onSurfaceChanged(size.width, size.height)
  }

}

export class ComposeJsRenderNode extends RenderNode implements ComposeRenderNode {
  private renderNodeUIView: InternalArkUIViewController | null
  private child: ComposeChildRenderNode

  constructor(renderNodeUIView: InternalArkUIViewController) {
    super()
    this.renderNodeUIView = renderNodeUIView
    this.child = this.renderNodeUIView?.getJsNode() as ComposeChildRenderNode
    this.child.renderNodeUIView = this.renderNodeUIView
    this.appendChild(this.child)
  }

  reSize(size: SizeResult) {
    if (this.renderNodeUIView == null) {
      return
    }
    this.renderNodeUIView?.onSurfaceChanged(size.width, size.height)
    this.resetSize(size)
  }

  resetSize(size: SizeResult) {
    this.size = size
    this.child.size = size
  }

  dispose() {
    super.dispose()
    if (this.renderNodeUIView) {
      this.child.renderNodeUIView = null
      this.renderNodeUIView = null
    }
  }
}

export class ComposeChildRenderNode extends RenderNode {
  renderNodeUIView: InternalArkUIViewController | null  = null

  draw(context: DrawContext) {
    if (this.renderNodeUIView != null) {
      this.renderNodeUIView.draw(context)
    } else {
      JsNodeDraw(context, this)
    }
  }
}

export class NodeStatusModify {
  private opArray: Int32Array | null = null;
  private opIndex: number = 0;
  private numberArray: Float32Array | null = null;
  private numberIndex: number = 0;
  private svgStrArray: string[] | null = null;
  private svgStrIndex: number = 0;
  private nodeArray: ComposeChildRenderNode[] | null[] = [];
  private nodeIndex: number = 0;
  private shape = new ShapeClip();

  getNextSize(): Size {
    if (this.numberArray == null) {
      return {width: 0, height: 0};
    } else {
      return {
        width: this.numberArray[this.numberIndex++],
        height: this.numberArray[this.numberIndex++]
      }
    }
  }

  getNextPosition(): Position {
    if (this.numberArray == null) {
      return {x: 0, y: 0};
    } else {
      return {
        x: this.numberArray[this.numberIndex++],
        y: this.numberArray[this.numberIndex++]
      }
    }
  }

  getNextRect(): Rect {
    if (this.numberArray == null) {
      return {left: 0, top: 0, right:0, bottom: 0};
    } else {
      return {
        left: this.numberArray[this.numberIndex++],
        top: this.numberArray[this.numberIndex++],
        right: this.numberArray[this.numberIndex++],
        bottom: this.numberArray[this.numberIndex++]
      }
    }
  }

  getNextRoundRect(): RoundRect {
    if (this.numberArray == null) {
      return {
        rect: { left: 0, top: 0, right: 0, bottom: 0 },
        corners: {
          topLeft: { x: 0, y: 0 },
          topRight: { x: 0, y: 0 },
          bottomLeft: { x: 0, y: 0 },
          bottomRight: { x: 0, y: 0 }
        }
      };
    } else {
      return {
        rect: {
          left: this.numberArray[this.numberIndex++],
          top: this.numberArray[this.numberIndex++],
          right: this.numberArray[this.numberIndex++],
          bottom: this.numberArray[this.numberIndex++]
        },
        corners: {
          topLeft: { x: this.numberArray[this.numberIndex++], y: this.numberArray[this.numberIndex++] },
          topRight: { x: this.numberArray[this.numberIndex++], y: this.numberArray[this.numberIndex++] },
          bottomLeft: { x: this.numberArray[this.numberIndex++], y: this.numberArray[this.numberIndex++] },
          bottomRight: { x: this.numberArray[this.numberIndex++], y: this.numberArray[this.numberIndex++] }
        }
      }
    }
  }

  getNextMatrix(): Matrix4 {
    if (this.numberArray == null) {
      return [
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
      ]
    }
    return [
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++],
      this.numberArray[this.numberIndex++]
    ]
  }

  getNextCommandPath(): string {
    if (this.svgStrArray == null) {
      return "";
    }
    return this.svgStrArray[this.svgStrIndex++];
  }

  doModify() {
    this.opIndex = 0;
    this.numberIndex = 0;
    this.nodeIndex = 0;
    this.svgStrIndex = 0;
    if (this.opArray != null) {

      for(this.opIndex = 0; this.opIndex < this.opArray.length; this.opIndex++) {
        const op = this.opArray[this.opIndex];
        const node = this.nodeArray[this.nodeIndex++];
        if (node == null) {
          hilog.info(0x0000, 'CanvasPage', `invaild: null node with op: ${op}`);
          continue;
        }
        switch (op) {
          case 0:
            node.invalidate();
            break;
          case 1:
            node.removeChild(this.nodeArray[this.nodeIndex++]);
            break;
          case 2:
            node.insertChildAfter(this.nodeArray[this.nodeIndex++], this.nodeArray[this.nodeIndex++]);
            break;
          case 3:
            node.clearChildren();
            break;
          case 10:
            node.appendChild(this.nodeArray[this.nodeIndex++]);
            break;
          case 4:
            node.size = this.getNextSize();
            break;
          case 5:
            node.position = this.getNextPosition();
            break;
          case 6:
            node.transform = this.getNextMatrix();
            break;
          case 7:
            this.shape.setRectShape(this.getNextRect());
            node.shapeClip = this.shape;
            break;
          case 8:
            this.shape.setRoundRectShape(this.getNextRoundRect())
            node.shapeClip = this.shape;
            break;
          case 9:
            this.shape.setCommandPath({ commands: this.getNextCommandPath() });
            node.shapeClip = this.shape;
            break;
          default:
            hilog.info(0x0000, 'CanvasPage', `invaild op: ${op}`);
            break;
        }
      }
    }
    this.nodeArray = []
  }
}

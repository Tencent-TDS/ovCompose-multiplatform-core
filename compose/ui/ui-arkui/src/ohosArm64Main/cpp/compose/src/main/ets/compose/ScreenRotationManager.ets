import { display } from '@kit.ArkUI';
import { hiTraceMeter } from '@kit.PerformanceAnalysisKit';

/**
 * 屏幕旋转管理控制器（单例模式）
 *
 * 核心功能：
 * 1. 监听系统屏幕旋转事件
 * 2. 管理旋转状态
 * 3. 提供旋转状态回调通知
 */
export class ScreenRotationManager {
  private static instance: ScreenRotationManager;
  private readonly TAG: string = "OhosSnapshot_ScreenRotationManager";
  /** 屏幕旋转动画默认时长（ms） */
  private readonly ROTATE_ANIM_TIME: number = 600;
  private timeoutId?: number;
  /** 当前是否处于旋转状态 */
  private changing: boolean = false;
  /** 系统回调是否已注册 */
  private sysCallbackRegistered: boolean = false;
  /** 当前屏幕旋转角度 */
  private rotation: number;
  /** 屏幕旋转回调注册表 */
  private callbacks: Set<ScreenChangeCallback> = new Set();
  /**
   * 系统屏幕旋转事件回调
   * @param displayId 触发事件的显示设备ID
   */
  private sysCallback = (displayId: number) => {
    const newRotation = this.getDisplayRotation();

    // 过滤无效旋转事件
    if (newRotation === this.rotation) {
      return;
    }
    console.log(this.TAG, `系统屏幕旋转事件触发 displayId=${displayId} 旋转角度 ${this.rotation} -> ${newRotation}`);

    // 更新旋转状态
    let oldRotation = this.rotation
    this.rotation = newRotation;
    this.notifyScreenChangeEnd();
    this.changing = true;

    // 通知所有注册的回调
    this.callbacks.forEach(callback => {
      callback.onScreenChangeStart(oldRotation, newRotation);
    });

    // 设置旋转结束计时器
    this.timeoutId = setTimeout(() => {
      this.changing = false;
      this.notifyScreenChangeEnd();
    }, this.ROTATE_ANIM_TIME);
  };

  private constructor() {
    this.rotation = this.getDisplayRotation();
  }

  public static getInstance(): ScreenRotationManager {
    if (!ScreenRotationManager.instance) {
      ScreenRotationManager.instance = new ScreenRotationManager();
    }
    return ScreenRotationManager.instance;
  }

  /**
   * 获取当前旋转状态
   * @returns 是否正在执行旋转动画
   */
  public isChanging(): boolean {
    return this.changing;
  }

  /**
   * 注册屏幕旋转回调
   * @param callback 包含开始/结束回调的对象
   */
  public registerScreenChange(callback: ScreenChangeCallback): void {
    this.callbacks.add(callback)

    // 如果还没有注册系统回调，则注册
    if (!this.sysCallbackRegistered) {
      display.on('change', this.sysCallback);
      this.sysCallbackRegistered = true;
      console.log(this.TAG, "System callback registered");
    }
  }

  /**
   * 注销屏幕旋转回调
   */
  public unregisterScreenChange(callback: ScreenChangeCallback): void {
    this.callbacks.delete(callback)
    if (this.callbacks.size === 0) {
      this.cleanupRotationListener();
    }
  }

  /**
   * 获取当前屏幕旋转角度
   * @returns 屏幕旋转角度（0/90/180/270）
   */
  public getRotation(): number {
    return this.rotation;
  }

  private cleanupRotationListener(): void {
    display.off('change', this.sysCallback);
    this.sysCallbackRegistered = false;
    console.log(this.TAG, "系统旋转监听已注销");
  }

  private notifyScreenChangeEnd(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = undefined;
      this.callbacks.forEach(callback => {
        callback.onScreenChangeEnd();
      });
      console.log(this.TAG, "屏幕旋转动画结束通知完成");
    }
  }

  private getDisplayRotation(): number {
    hiTraceMeter.startTrace("getDisplayRotation", 0);
    const rotation = display.getDefaultDisplaySync().rotation;
    hiTraceMeter.finishTrace("getDisplayRotation", 0);
    return rotation;
  }
}

/**
 * 屏幕旋转事件回调接口
 */
export interface ScreenChangeCallback {
  /**
   * 屏幕旋转开始回调
   */
  onScreenChangeStart: (oldRotation: number, newRotation: number) => void;

  /**
   * 屏幕旋转结束回调
   */
  onScreenChangeEnd: () => void;
}
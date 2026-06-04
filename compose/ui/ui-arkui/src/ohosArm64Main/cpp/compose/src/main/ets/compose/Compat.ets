import { deviceInfo } from "@kit.BasicServicesKit";

/**
 * Use onKeyEventDispatch since 20, else onKeyEvent.
 */
export class OnKeyEventCompatModifier<T extends CommonMethod<T>> implements AttributeModifier<T> {
  callback: Callback<KeyEvent, boolean>

  constructor(callback: Callback<KeyEvent, boolean>) {
    this.callback = callback
  }

  applyNormalAttribute(instance: T): void {
    instance.onKeyEvent(this.callback);
  }
}

/**
 * Use onKeyEventDispatch since 17, else nothing.
 */
export class OnAxisEventCompatModifier<T extends CommonMethod<T>> implements AttributeModifier<T> {
  callback: Callback<AxisEvent>

  constructor(callback: Callback<AxisEvent>) {
    this.callback = callback
  }

  applyNormalAttribute(instance: T): void {
    if (deviceInfo.sdkApiVersion >= 17) {
      instance.onAxisEvent(this.callback)
    }
  }
}
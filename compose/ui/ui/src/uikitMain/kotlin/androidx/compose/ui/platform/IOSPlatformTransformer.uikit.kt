package androidx.compose.ui.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.uikit.utils.TMMNativeItalicType
import androidx.compose.ui.uikit.utils.TMMNativeTextDecorator
import androidx.compose.ui.unit.TextUnit
import platform.UIKit.NSTextAlignment
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.NSTextAlignmentLeft
import platform.UIKit.NSTextAlignmentNatural
import platform.UIKit.NSTextAlignmentRight
import platform.UIKit.UIColor
import platform.UIKit.UIFontWeight
import platform.UIKit.UIFontWeightBlack
import platform.UIKit.UIFontWeightBold
import platform.UIKit.UIFontWeightHeavy
import platform.UIKit.UIFontWeightLight
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UIFontWeightRegular
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIFontWeightThin
import platform.UIKit.UIFontWeightUltraLight

/**
 * 将[Color]转换成iOS平台的[UIColor]
 */
internal inline fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble()
    )
}

/**
 * 将[FontWeight]转换成iOS平台的[UIFontWeight]
 */
internal inline fun FontWeight.toUIFontWeight(): UIFontWeight = when (this) {
    FontWeight.Thin -> UIFontWeightUltraLight
    FontWeight.ExtraLight -> UIFontWeightThin
    FontWeight.Light -> UIFontWeightLight
    FontWeight.Normal -> UIFontWeightRegular
    FontWeight.Medium -> UIFontWeightMedium
    FontWeight.SemiBold -> UIFontWeightSemibold
    FontWeight.Bold -> UIFontWeightBold
    FontWeight.ExtraBold -> UIFontWeightHeavy
    FontWeight.Black -> UIFontWeightBlack
    else -> UIFontWeightRegular
}

internal inline fun TextAlign.toNSTextAlignment(): NSTextAlignment = when (this) {
    TextAlign.Left -> NSTextAlignmentLeft
    TextAlign.Right -> NSTextAlignmentRight
    TextAlign.Center -> NSTextAlignmentCenter
    TextAlign.Unspecified -> NSTextAlignmentNatural
    else -> NSTextAlignmentNatural
}

internal inline fun TextDecoration.toTMMNativeDecorator(): TMMNativeTextDecorator = when (this) {
    TextDecoration.None -> TMMNativeTextDecorator.TMMNativeTextDecoratorNone
    TextDecoration.Underline -> TMMNativeTextDecorator.TMMNativeTextDecoratorUnderLine
    TextDecoration.LineThrough -> TMMNativeTextDecorator.TMMNativeTextDecoratorLineThrough
    else -> TMMNativeTextDecorator.TMMNativeTextDecoratorNone
}

internal inline fun FontStyle.toTMMNativeItalicType(): TMMNativeItalicType = when (this) {
    FontStyle.Italic -> TMMNativeItalicType.TMMNativeItalicSpecific
    FontStyle.Normal -> TMMNativeItalicType.TMMNativeItalicNormal
    else -> TMMNativeItalicType.TMMNativeItalicNone
}

/**
 * 根据FontSize计算Sp
 */
internal fun TextUnit.toSp(fontSize: Int): Float {
    if (isEm) {
        // 如果是em则需要根据fontSize计算宽度
        return value * fontSize
    }
    return value
}
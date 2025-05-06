package androidx.compose.ui.text

/**
 * 由于[Paragraph]是sealed类，外部无法访问，这个类只用于公开Paragraph的接口，让外部可以访问，而不用修改原本Paragraph接口
 */
interface PublicParagraph : Paragraph
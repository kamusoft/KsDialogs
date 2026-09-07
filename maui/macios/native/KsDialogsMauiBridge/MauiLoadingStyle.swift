import Foundation
import KsDialogs
import UIKit

/// MAUI 側が設定した進捗テキストの組み立て方。
///
/// 進捗が未報告のときは第2引数が nil になる。ObjC 互換面では Swift の関数型をそのまま表せないため、
/// 数値は `NSNumber` で運ぶ。
public typealias MauiLoadingProgressFormat = (String?, NSNumber?) -> String

/// MAUI 側で設定された既定ローディングの見た目を運ぶ入れ物。
///
/// この面は見えを作らず、受け取った値をそのまま Native ライブラリの設定へ渡す (core/ADR-0001)。
/// 色は ObjC 境界を越えられる形として ARGB 32bit 整数で受け取る。
/// 既定値は書き写さず Native ライブラリの既定値から引く。何も設定しないまま渡せば
/// スタイルを設定しない場合と同じ見え方になり、既定値がずれる余地も残らない。
@objc(KSDMauiLoadingStyle)
public final class MauiLoadingStyle: NSObject, @unchecked Sendable {
    /// Native ライブラリが定める既定値。各項目の初期値はここから引く。
    private static let contractDefaults = LoadingStyle()

    /// 回転インジケータの色 (ARGB 32bit)。
    @objc public var indicatorColorArgb: Int32 = contractDefaults.indicatorColor.argbValue

    /// メッセージの文字の大きさ (pt)。
    @objc public var messageFontSize: Double = contractDefaults.messageFontSize

    /// メッセージの文字色 (ARGB 32bit)。
    @objc public var messageColorArgb: Int32 = contractDefaults.messageColor.argbValue

    /// メッセージを省略して表示したときに使う文言。nil ならメッセージなしで表示する。
    @objc public var defaultMessage: String? = contractDefaults.defaultMessage

    /// 表示テキストの組み立て方。nil なら Native ライブラリの既定の組み立て方が使われる。
    @objc public var progressFormat: MauiLoadingProgressFormat?
}

extension LoadingStyle {
    /// ObjC 互換面のスタイルを Native ライブラリの型へ写す。
    init(_ style: MauiLoadingStyle) {
        var format = LoadingStyle.defaultProgressFormat
        if let suppliedFormat = style.progressFormat {
            // MAUI 側の組み立て方は UI スレッド上で呼ばれるが、契約上は任意の文脈から呼べる形なので
            // 並行性検査を跨いで運ぶ (呼び出しの安全は呼び出し元の責務)
            let boxedFormat = MauiUncheckedSendableBox(suppliedFormat)
            format = { message, progress in
                boxedFormat.value(message, progress.map { NSNumber(value: $0) })
            }
        }
        self.init(
            indicatorColor: UIColor(argb: style.indicatorColorArgb),
            messageFontSize: style.messageFontSize,
            messageColor: UIColor(argb: style.messageColorArgb),
            defaultMessage: style.defaultMessage,
            progressFormat: format
        )
    }
}

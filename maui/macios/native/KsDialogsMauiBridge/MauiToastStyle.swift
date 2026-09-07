import Foundation
import KsDialogs
import UIKit

/// MAUI 側で設定された Toast の一括設定を運ぶ入れ物。
///
/// この面は見えを作らず、受け取った値をそのまま Native ライブラリの設定へ渡す (core/ADR-0001)。
/// 色は ObjC 境界を越えられる形として ARGB 32bit 整数で受け取る。
/// 既定値は書き写さず Native ライブラリの既定値から引く。何も設定しないまま渡せば
/// スタイルを設定しない場合と同じ見え方になり、既定値がずれる余地も残らない。
@objc(KSDMauiToastStyle)
public final class MauiToastStyle: NSObject, @unchecked Sendable {
    /// Native ライブラリが定める既定値。各項目の初期値はここから引く。
    private static let contractDefaults = ToastStyle()

    /// デフォルト View のピルの地色 (ARGB 32bit)。
    @objc public var backgroundColorArgb: Int32 = contractDefaults.backgroundColor.argbValue

    /// デフォルト View のメッセージの文字色 (ARGB 32bit)。
    @objc public var textColorArgb: Int32 = contractDefaults.textColor.argbValue

    /// デフォルト View のメッセージの文字の大きさ (pt)。
    @objc public var fontSize: Double = contractDefaults.fontSize

    /// デフォルト View のピルの角丸半径 (pt)。
    @objc public var cornerRadius: Double = contractDefaults.cornerRadius

    /// duration を省略した表示に使うミリ秒。
    @objc public var defaultDuration: Int32 = Int32(contractDefaults.defaultDuration)

    /// アプリ全体の既定配置。nil なら Toast の契約既定値が使われる。
    @objc public var defaultPlacement: MauiDialogPlacement?
}

extension ToastStyle {
    /// ObjC 互換面の一括設定を Native ライブラリの型へ写す。
    init(_ style: MauiToastStyle) {
        self.init(
            backgroundColor: UIColor(argb: style.backgroundColorArgb),
            textColor: UIColor(argb: style.textColorArgb),
            fontSize: style.fontSize,
            cornerRadius: style.cornerRadius,
            defaultDuration: Int(style.defaultDuration),
            defaultPlacement: style.defaultPlacement.map { DialogPlacement($0) }
        )
    }
}

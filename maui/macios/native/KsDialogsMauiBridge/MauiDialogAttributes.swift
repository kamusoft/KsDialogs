import Foundation
import KsDialogs
import UIKit

/// ダイアログを基準領域のどこへ寄せるか (ObjC 互換面の表現)。
@objc(KSDMauiDialogAlignment)
public enum MauiDialogAlignment: Int, Sendable {
    case start = 0
    case center = 1
    case end = 2
    case fill = 3
}

/// サイズと位置の計算を行う基準領域 (ObjC 互換面の表現)。
@objc(KSDMauiDialogLayoutArea)
public enum MauiDialogLayoutArea: Int, Sendable {
    case window = 0
    case visibleArea = 1
}

/// MAUI 側で中身に添付された静的メタ属性を運ぶ入れ物。
///
/// この面はレイアウト計算を行わず、受け取った値をそのまま中身の View への添付として
/// Native ライブラリへ渡す (core/ADR-0001)。無効値の丸めは Native ライブラリの責務なので、
/// ここでは値を検査しない。
/// 色は ObjC 境界を越えられる形として ARGB 32bit 整数で受け取る。
/// 既定値は書き写さず Native ライブラリの既定値から引く。何も設定しないまま渡せば
/// 属性を添付しない中身と同じ見え方になり、既定値がずれる余地も残らない。
@objc(KSDMauiDialogOptions)
public final class MauiDialogOptions: NSObject, @unchecked Sendable {
    /// Native ライブラリが定める既定値。各項目の初期値はここから引く。
    private static let contractDefaults = DialogOptions()

    /// サイズと位置の計算の基準になる領域。
    @objc public var layoutArea = MauiDialogLayoutArea(contractDefaults.layoutArea)

    /// 基準 rect の上辺から控除する余白。
    @objc public var marginTop: Double = contractDefaults.dialogMargin.top

    /// 基準 rect の左辺から控除する余白。
    @objc public var marginLeft: Double = contractDefaults.dialogMargin.left

    /// 基準 rect の下辺から控除する余白。
    @objc public var marginBottom: Double = contractDefaults.dialogMargin.bottom

    /// 基準 rect の右辺から控除する余白。
    @objc public var marginRight: Double = contractDefaults.dialogMargin.right

    /// 基準 rect の幅に対する比率。0 以下は未指定。
    @objc public var proportionalWidth: Double = contractDefaults.proportionalWidth

    /// 基準 rect の高さに対する比率。0 以下は未指定。
    @objc public var proportionalHeight: Double = contractDefaults.proportionalHeight

    /// ダイアログの背後を覆う色 (ARGB 32bit)。
    @objc public var overlayColorArgb: Int32 = contractDefaults.overlayColor.argbValue

    /// 外側タップをキャンセルと同じ経路で閉じる操作として扱うか。
    @objc public var isCanceledOnTouchOutside: Bool = contractDefaults.isCanceledOnTouchOutside
}

/// MAUI 側で決まった置き場所を運ぶ入れ物。
///
/// MAUI 側で「show 引数 > 中身への添付 > 契約既定値」の優先順に合成し終えた実効値が入る
/// (core/ADR-0015)。既定値は options と同じく Native ライブラリの既定値から引く。
@objc(KSDMauiDialogPlacement)
public final class MauiDialogPlacement: NSObject, @unchecked Sendable {
    /// Native ライブラリが定める既定値。各項目の初期値はここから引く。
    private static let contractDefaults = DialogPlacement()

    /// 水平方向の配置。
    @objc public var horizontalAlignment = MauiDialogAlignment(contractDefaults.horizontalAlignment)

    /// 垂直方向の配置。
    @objc public var verticalAlignment = MauiDialogAlignment(contractDefaults.verticalAlignment)

    /// 配置を決めた後に加える水平方向の移動量。正の値で右へ動く。
    @objc public var offsetX: Double = contractDefaults.offsetX

    /// 配置を決めた後に加える垂直方向の移動量。正の値で下へ動く。
    @objc public var offsetY: Double = contractDefaults.offsetY
}

extension DialogAlignment {
    /// ObjC 互換面の配置を Native ライブラリの配置へ写す。
    init(_ alignment: MauiDialogAlignment) {
        switch alignment {
        case .start: self = .start
        case .center: self = .center
        case .end: self = .end
        case .fill: self = .fill
        }
    }
}

extension DialogLayoutArea {
    /// ObjC 互換面の基準領域を Native ライブラリの基準領域へ写す。
    init(_ area: MauiDialogLayoutArea) {
        switch area {
        case .window: self = .window
        case .visibleArea: self = .visibleArea
        }
    }
}

extension DialogOptions {
    /// ObjC 互換面の静的メタ属性を Native ライブラリの型へ写す。
    init(_ options: MauiDialogOptions) {
        self.init(
            layoutArea: DialogLayoutArea(options.layoutArea),
            dialogMargin: DialogEdgeInsets(
                top: options.marginTop,
                left: options.marginLeft,
                bottom: options.marginBottom,
                right: options.marginRight
            ),
            proportionalWidth: options.proportionalWidth,
            proportionalHeight: options.proportionalHeight,
            overlayColor: UIColor(argb: options.overlayColorArgb),
            isCanceledOnTouchOutside: options.isCanceledOnTouchOutside
        )
    }
}

extension DialogPlacement {
    /// ObjC 互換面の置き場所を Native ライブラリの型へ写す。
    init(_ placement: MauiDialogPlacement) {
        self.init(
            horizontalAlignment: DialogAlignment(placement.horizontalAlignment),
            verticalAlignment: DialogAlignment(placement.verticalAlignment),
            offsetX: placement.offsetX,
            offsetY: placement.offsetY
        )
    }
}

extension MauiDialogAlignment {
    /// Native ライブラリの配置を ObjC 互換面の配置へ写す (既定値を引くために使う)。
    /// 判別できない値は、この互換面が運べる意味に最も近い中央として扱う。
    init(_ alignment: DialogAlignment) {
        switch alignment {
        case .start: self = .start
        case .center: self = .center
        case .end: self = .end
        case .fill: self = .fill
        @unknown default: self = .center
        }
    }
}

extension MauiDialogLayoutArea {
    /// Native ライブラリの基準領域を ObjC 互換面の基準領域へ写す (既定値を引くために使う)。
    /// 判別できない値は、契約の既定である可視領域として扱う。
    init(_ area: DialogLayoutArea) {
        switch area {
        case .window: self = .window
        case .visibleArea: self = .visibleArea
        @unknown default: self = .visibleArea
        }
    }
}

extension UIColor {
    /// 色を ARGB 32bit 整数にする。
    /// 成分を読めない色空間の色でも運べるよう、読めない場合はグレースケールとして読む。
    var argbValue: Int32 {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        if !getRed(&red, green: &green, blue: &blue, alpha: &alpha) {
            var white: CGFloat = 0
            getWhite(&white, alpha: &alpha)
            red = white
            green = white
            blue = white
        }
        func component(_ value: CGFloat) -> UInt32 {
            UInt32((min(max(value, 0), 1) * 255).rounded())
        }
        let packed = component(alpha) << 24 | component(red) << 16 | component(green) << 8 | component(blue)
        return Int32(bitPattern: packed)
    }

    /// ARGB 32bit 整数から色を作る。
    convenience init(argb: Int32) {
        let value = UInt32(bitPattern: argb)
        self.init(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: CGFloat((value >> 24) & 0xFF) / 255
        )
    }
}

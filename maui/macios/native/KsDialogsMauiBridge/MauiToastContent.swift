import Foundation

/// カスタム Toast の中身と、それに効くメタ属性を新規に供給する関数。
///
/// 供給元 (MAUI 側) は自分の失敗をこの境界の外へ漏らさず、中身を作れなかったときは nil を返す。
/// managed / native の呼び出し境界を例外が越えると未処理の障害になるため、失敗は値で返す取り決めにしてある。
/// nil を受けた表示は、受理後の失敗としてその1枚だけが破棄される。
public typealias MauiToastContentProvider = () -> MauiDialogContent?

/// 表示1回分の中身の指定。
///
/// デフォルト View (ライブラリ同梱の内蔵コンテンツ) では中身の供給がなく、メッセージと duration・
/// 置き場所だけを運ぶ。カスタム Toast では MAUI 側が中身とメタ属性を供給する。
@objc(KSDMauiToastContent)
public final class MauiToastContent: NSObject {
    /// カスタム Toast の中身の供給元。デフォルト View では nil。
    let contentProvider: MauiToastContentProvider?

    /// デフォルト View に表示するメッセージ。カスタム Toast では nil。
    @objc public let message: String?

    /// 表示するミリ秒。未指定なら nil (一括設定の既定 duration が使われる)。
    @objc public let duration: NSNumber?

    /// 表示 API の引数で渡された置き場所。未指定なら nil。
    @objc public let placement: MauiDialogPlacement?

    /// デフォルト View の表示を指定する。
    /// - Parameters:
    ///   - message: 表示する文言
    ///   - duration: 表示するミリ秒。nil なら一括設定の既定 duration
    ///   - placement: 置き場所。nil なら一括設定のアプリ既定配置、それも無ければ契約の既定値
    @objc(initWithMessage:duration:placement:)
    public init(message: String, duration: NSNumber?, placement: MauiDialogPlacement?) {
        contentProvider = nil
        self.message = message
        self.duration = duration
        self.placement = placement
        super.init()
    }

    /// カスタム Toast の表示を指定する。
    /// - Parameters:
    ///   - contentProvider: 中身とメタ属性の供給元
    ///   - duration: 表示するミリ秒。nil なら一括設定の既定 duration
    ///   - placement: 置き場所。nil なら中身への添付が使われる
    @objc(initWithContentProvider:duration:placement:)
    public init(
        contentProvider: @escaping MauiToastContentProvider,
        duration: NSNumber?,
        placement: MauiDialogPlacement?
    ) {
        self.contentProvider = contentProvider
        message = nil
        self.duration = duration
        self.placement = placement
        super.init()
    }
}

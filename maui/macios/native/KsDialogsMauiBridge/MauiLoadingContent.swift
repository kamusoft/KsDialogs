import Foundation

/// カスタム Loading の中身と、それに効くメタ属性を新規に供給する関数。
///
/// 供給元 (MAUI 側) は自分の失敗をこの境界の外へ漏らさず、中身を作れなかったときは nil を返す。
/// managed / native の呼び出し境界を例外が越えると未処理の障害になるため、失敗は値で返す取り決めにしてある。
/// nil を受けた開始は成立せず、その失敗が完了の通知として MAUI 側へ届く。
public typealias MauiLoadingContentProvider = () -> MauiDialogContent?

/// MAUI 側の ViewModel へ進捗を届ける口。UI スレッドから呼ばれる。
public typealias MauiLoadingProgressReceiver = (Double) -> Void

/// 表示1回分の中身の指定。
///
/// 既定ローディング (ライブラリ同梱の内蔵コンテンツ) では中身の供給がなく、メッセージと置き場所だけを
/// 運ぶ。カスタム Loading では MAUI 側が中身とメタ属性を供給し、進捗の転送先も一緒に渡す。
@objc(KSDMauiLoadingContent)
public final class MauiLoadingContent: NSObject {
    /// カスタム Loading の中身の供給元。既定ローディングでは nil。
    let contentProvider: MauiLoadingContentProvider?

    /// 既定ローディングに表示するメッセージ。未指定なら nil。
    @objc public let message: String?

    /// 表示 API の引数で渡された置き場所。未指定なら nil。
    @objc public let placement: MauiDialogPlacement?

    /// 進捗の転送先。転送しないなら nil。
    let progressReceiver: MauiLoadingProgressReceiver?

    /// 既定ローディングの表示を指定する。
    /// - Parameters:
    ///   - message: 表示するメッセージ。nil ならスタイルの既定メッセージ
    ///   - placement: 置き場所。nil なら契約の既定値
    @objc(initWithMessage:placement:)
    public init(message: String?, placement: MauiDialogPlacement?) {
        contentProvider = nil
        self.message = message
        self.placement = placement
        progressReceiver = nil
        super.init()
    }

    /// カスタム Loading の表示を指定する。
    /// - Parameters:
    ///   - contentProvider: 中身とメタ属性の供給元
    ///   - placement: 置き場所。nil なら中身への添付が使われる
    ///   - progressReceiver: 進捗の転送先。転送しないなら nil
    @objc(initWithContentProvider:placement:progressReceiver:)
    public init(
        contentProvider: @escaping MauiLoadingContentProvider,
        placement: MauiDialogPlacement?,
        progressReceiver: MauiLoadingProgressReceiver?
    ) {
        self.contentProvider = contentProvider
        message = nil
        self.placement = placement
        self.progressReceiver = progressReceiver
        super.init()
    }
}

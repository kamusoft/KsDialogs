# ios concepts 目次

iOS Native (Swift) の公開面 — 公開名・署名・コード例・framework 固有の注意。挙動の契約は [core](../core/index.md) が正 ([rules.md の「配置判断」](../rules.md)・cross/ADR-0014)。

## api/

- [dialog-surface.md](api/dialog-surface.md) — Dialog の公開面: 既定エントリと登録の入口・結果型の省略形・UIKit / SwiftUI での中身の書き分け・SwiftUI の添付 modifier・インライン show・結果報告口の取得・構成ミスのエラー種別・型指定 show
- [layout-surface.md](api/layout-surface.md) — レイアウトの公開面: 属性の型とプロパティ・UIKit の添付プロパティと SwiftUI の modifier・show 引数での置き場所指定・論理単位と色型
- [transition-surface.md](api/transition-surface.md) — トランジションの公開面: 演出の型とフックの型・UIKit の添付プロパティと SwiftUI の modifier・プリセット factory と辺の綴り・duration と easing の型・MainActor の注意
- [loading-surface.md](api/loading-surface.md) — Loading の公開面: 契約と既定エントリ・show / hide / メッセージ更新 / スコープ形の署名・進捗報告口と進捗受け口・カスタム View の登録 (UIKit / SwiftUI)・型指定 show / start と VM factory 登録・styling と器メタ属性・MainActor と throws の注意
- [toast-surface.md](api/toast-surface.md) — Toast の公開面: 契約と既定エントリ・4 経路の show の署名・カスタム View の登録 (UIKit / SwiftUI)・型指定 show と VM factory 登録・styling のプロパティ・factory 閉包が throws である理由と持たない操作

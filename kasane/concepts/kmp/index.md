# kmp concepts 目次

Kotlin Multiplatform 固有の公開面と Native host との統合知識。挙動の契約は [core](../core/index.md) が正 ([rules.md の「配置判断」](../rules.md)・cross/ADR-0014)。共有コード (commonMain) に添付の面が無いレイアウト・トランジションは、各 OS 側の公開面 ([ios](../ios/index.md) / [android](../android/index.md)) を参照する。

## api/

- [dialog-surface.md](api/dialog-surface.md) — Dialog の公開面 (共有コード): 既定エントリと show・結果型を省略できないこと・型指定 show と VM factory の登録 (共有 Kotlin コード専用、呼び出し元の文脈で生成・configure、Swift から見えない)・View factory の登録は各 OS 側で行うこと・共有コードに添付の面が無いこと・Android / iOS ホスト側の見え方・失敗とキャンセルの届き方と Swift 境界の `@Throws`
- [loading-surface.md](api/loading-surface.md) — Loading の公開面 (共有コード): 既定エントリと 4 操作・型指定 show / start と VM factory の登録 (共有 Kotlin コード専用)・進捗報告口と進捗受け口・styling と器メタ属性がこの面に無いこと・カスタム View の登録は各 OS 側であること・Swift 境界の `@Throws`
- [toast-surface.md](api/toast-surface.md) — Toast の公開面 (共有コード): 既定エントリと 3 経路の show・duration 引数・型指定 show と VM factory の登録 (共有 Kotlin コード専用。VM factory / configure の例外も同期に伝播)・一括設定がこの面に無いこと・カスタム View の登録は各 OS 側であること・Swift 境界の `@Throws`
- [ios-host-integration.md](api/ios-host-integration.md) — KMP 利用者の iOS ホスト統合: KMP 共有モジュールから KsDialogs を使う iOS アプリの依存経路と初回統合、発行 metadata の Swift 参照が version で決まること (SNAPSHOT は local・リリース版は配信リポジトリの exact) と Kotlin サポート範囲、Swift 側で登録するもの (型付き登録入口・結果型の指定・View factory のオーバーロード。Swift 向け面に型指定 show は無い)、Sample の合成 Swift package 再生成手順

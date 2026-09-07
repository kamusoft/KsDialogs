# maui concepts 目次

.NET MAUI (C#) 固有の知識 — binding 構成と公開面 (公開名・署名・コード例・framework 固有の注意)。挙動の契約は [core](../core/index.md) が正 ([rules.md の「配置判断」](../rules.md)・cross/ADR-0014)。

## api/

- [dialog-surface.md](api/dialog-surface.md) — Dialog の公開面: 既定エントリと登録の入口・真偽値の顔・中身は MAUI の View だけであること・インライン show・結果報告口の取得・構成ミスの例外型・型指定 show と非同期 configure・移植元の API 名との対応
- [layout-surface.md](api/layout-surface.md) — レイアウトの公開面: 項目ごとの添付プロパティと Get / Set・束ねた値オブジェクト・show 引数での置き場所指定・XAML から書ける範囲・移植元の属性名との対応
- [transition-surface.md](api/transition-surface.md) — トランジションの公開面: 演出の型とフックのデリゲート型・code-behind からの添付プロパティ・プリセット factory と辺の綴り・duration と easing の型・ミリ秒表現に収まらない時間の扱い
- [loading-surface.md](api/loading-surface.md) — Loading の公開面: 契約と既定エントリ・表示 / 非表示 / メッセージ更新 / スコープ形の署名・進捗の報告口と受け口・カスタム View の登録と DI 糖衣・型指定 show / start と VM factory 登録 (オーバーロード束縛の注意)・styling と器メタ属性・中身は MAUI の View だけであること
- [toast-surface.md](api/toast-surface.md) — Toast の公開面: 契約と既定エントリ・4 経路の表示呼び出しと duration 引数・カスタム View の登録と DI 糖衣・型指定 Show と VM factory 登録・styling のプロパティ・戻り値を持たないことと器メタ属性の受け口が無いこと
- [di-registration.md](api/di-registration.md) — MAUI の DI 連携と登録糖衣: 1行登録 `RegisterForDialog` / `RegisterForLoading` / `RegisterForToast` の配線 (View / VM factory の自動登録・BindingContext の同一性保証)・`AddKsDialogs` と fallback resolver の解決順序 (明示 → fallback → 失敗。Dialog 限定)・fallback 設定の合成と持続・構成ミスの失敗の種類 (maui/ADR-0005・core/ADR-0035 由来。MAUI 限定糖衣)

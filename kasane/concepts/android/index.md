# android concepts 目次

Android Native (Kotlin) の公開面 — 公開名・署名・コード例・framework 固有の注意。挙動の契約は [core](../core/index.md) が正 ([rules.md の「配置判断」](../rules.md)・cross/ADR-0014)。

## api/

- [dialog-surface.md](api/dialog-surface.md) — Dialog の公開面: 既定エントリと登録の入口・真偽値の別名・従来 View 系と Compose の別名での呼び分けと配布モジュール・Compose の属性宣言・インライン show・結果報告口の取得・構成ミスの例外型・型指定 show
- [layout-surface.md](api/layout-surface.md) — レイアウトの公開面: 属性の型とプロパティ・従来 View 系の拡張プロパティと Compose の宣言・show 引数での置き場所指定・論理単位と色の表現
- [transition-surface.md](api/transition-surface.md) — トランジションの公開面: 演出の型とフックの型・従来 View 系の拡張プロパティと Compose の宣言・プリセット factory と辺の綴り・duration と easing の型・ミリ秒に落ちる微小値の扱い
- [loading-surface.md](api/loading-surface.md) — Loading の公開面: 契約と既定エントリ・show / hide / メッセージ更新 / スコープ形の署名・進捗報告口と進捗受け口・従来 View 系と Compose の登録と表示・型指定 show / start と VM factory 登録・styling と器メタ属性・配布モジュールと Context レシーバの注意
- [toast-surface.md](api/toast-surface.md) — Toast の公開面: 契約と既定エントリ・4 経路の show と duration 引数・従来 View 系と Compose の登録と表示・型指定 show と VM factory 登録・styling のプロパティ・持たない操作と OS の Toast API との違い

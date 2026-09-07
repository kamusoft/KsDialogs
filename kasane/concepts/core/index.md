# core concepts 目次

全 platform が共有するダイアログ契約 (この層の文書が正 — 実装はここに合わせる)。

api/ は**挙動の契約**だけを持ち、公開名・署名・コード例は各 platform の公開面 concept にある ([rules.md の「配置判断」](../rules.md)・cross/ADR-0014) → [ios](../ios/index.md) / [android](../android/index.md) / [maui](../maui/index.md) / [kmp](../kmp/index.md)。

## api/

- [result-notification-semantics.md](api/result-notification-semantics.md) — 結果通知のルール: show は completed(結果) / cancelled をちょうど1回返す。どの操作がキャンセルになるか・結果が確定する時点と呼び出し元へ渡る時点 (ラッチと配送)・移植元の型安全性の弱点の解消 (core/ADR-0003 由来)
- [multi-display-semantics.md](api/multi-display-semantics.md) — 多段表示のルール: 重なり管理は OS の提示機構に任せ、保証する挙動4点だけを定める。下から閉じる等の保証しない挙動も明記 (core/ADR-0006 由来)
- [registration-show-semantics.md](api/registration-show-semantics.md) — 登録と表示の呼び出し面のルール: 真偽値結果は結果型の記述を省ける (形態別の省略形と KMP 共有コードの例外)・中身は従来 View 系と宣言的 UI 系 (SwiftUI / Compose) のどちらでも書けて観察できる挙動は同一・宣言的 UI での属性の添付・登録せずにその場で表示するインライン show はレジストリを変えない (core/ADR-0010〜0013・kmp/ADR-0003・0004 由来)
- [model-binding-semantics.md](api/model-binding-semantics.md) — ViewModel 主導の呼び出しのルール: show 中の ViewModel から結果報告口を取得できる仕組みと全終端経路での除去・ViewModel の型だけを渡す型指定 show (VM factory 解決・configure の順序保証・スナップショット解決。Dialog / Loading / Toast の 3 機能で同型)・ViewModel 契約の参照型 (class) 限定と形態別の強制手段・「非破壊の追加」が指す範囲・KMP の共有コードでの見え方 (VM factory は共有コード側、生成・configure は呼び出し元の文脈、Toast も同期伝播) (core/ADR-0018〜0021・0035・kmp/ADR-0006 由来)
- [layout-semantics.md](api/layout-semantics.md) — レイアウトのルール: 器のメタ属性と既定値・添付と show 引数による供給と優先順位・軸ごとの rect 決定手順・座標系・基準領域・外側タップキャンセル (core/ADR-0007・0008・0009・0014・0015 由来)
- [transition-semantics.md](api/transition-semantics.md) — トランジション (出入りの演出) のルール: 演出は第3の添付スロット `DialogTransition` で供給し、フックは全形態でホスト View を受け取る統一形・既定はライブラリのクロスフェード (覆いは別レイヤで常時フェード)・プリセットと成立しない duration の扱い・結果のラッチと配送 (配送は退出完了後)・フックを実行する閉鎖経路と失敗/未完了の扱い (core/ADR-0016・0017 由来)
- [loading-semantics.md](api/loading-semantics.md) — Loading (処理中の操作ブロックとインジケータ表示) のルール: 呼び出し面の構成 (インスタンス渡し・インライン・型指定の 3 形) と多重利用の合流と世代・器の性質 (ダイアログより手前・ユーザー操作では閉じない)・既定ローディングの styling・カスタム View 版の専用レジストリ (View factory と VM factory の 2 スロット) と進捗受け口・持たない機能 (core/ADR-0022〜0027・0035 由来)
- [toast-semantics.md](api/toast-semantics.md) — Toast (fire-and-forget の非対話通知表示) のルール: 呼び出し面の構成 (メッセージ入口・登録・インライン・型指定の 4 経路) と duration の時間モデル・失敗モデル3段階・完全非対話と非モーダル (タッチ素通し)・多重表示と機能間の前後関係 (Loading が常に前面)・配置の優先順とアプリ既定配置・デフォルト View (角丸ピル) と支援技術への通知・持たない機能 (core/ADR-0028〜0033・0035 由来)

## architecture/

- [layout-case-table.md](architecture/layout-case-table.md) — レイアウト共通ケース表と OS 差の統制: 期待値を固定する `core/layout-spec/cases.json` の形と読み方・ケース ID の運用・全量検証を課す範囲・OS 差を承認つきで記録する仕組み。利用者が使う契約ではなく Native 2 実装の検証機構の記述 (core/ADR-0009 由来。利用者向け Skill の源泉からは除外)

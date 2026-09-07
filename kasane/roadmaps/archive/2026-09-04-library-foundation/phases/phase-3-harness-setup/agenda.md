# ハーネス・規約整備

phase-1 の決定と phase-2 の実物を使って、Kasane ハーネスと規約類をプロジェクトの実態に合わせる research フェーズ (KsAppKMP phase-3 の写像)。「実物のない規約は書かない」を守り、実物不足の規約は先送りする。

## 論点

- ~~reference-repositories.md の設置~~: **2026-08-13 に ksn-concept で先行導入済み** (cross/conventions/。AiForms.Maui.Dialogs / KsSettingsView / KsAppKMP の3件を登録済み)。本フェーズではパスの実在確認のみ → **2026-08-14 実在確認済み** (3件とも実在。KsSettingsView / KsAppKMP は kasane/ 構造も確認)
- ~~comment-policy + 機械検査の移植~~: **2026-08-13 に ksn-concept で先行導入済み** (cross/conventions/ + scripts/ + PreToolUse hook。初期段階からラチェットを効かせるためロードマップ着手前に導入)。本フェーズでは実態との突き合わせのみ → **2026-08-14 突き合わせ済み** (参照3ファイル実在・PreToolUse hook 登録済み・lint selftest 全件 OK)

## 決定事項

- **ADR 昇格は cross/0001・0003 の2件のみ (2026-08-14)**: 運用実績で検証済みの方針決定2件 (rebrand-policy・knowledge-intake-hybrid) を accepted に昇格。core/0002〜0007 の6件は実装で検証される決定のため proposed のまま維持し、phase-4 縦串の実装検証を経て蒸留で昇格する
- **core concepts の書き下ろし範囲は契約意味論2件 (2026-08-14)**: phase-3 で書くのは結果通知の意味論 (core/ADR-0003) と多段表示の意味論 (core/ADR-0006) の2件 (core/api/ へ)。レイアウト規則 (core/ADR-0007) は縦串に必要な最小を phase-4 (共通仕様テストの器)、全量を phase-5 で実物と共に段階化し、phase-3 では書かない
- **sample-parity は追加成果物なしで閉じる (2026-08-14)**: 方針の持ち込み先である phase-4 agenda の論点に翻案元 (KsSettingsView cross)・再定義ポイント (「デモ画面」の定義)・concepts 化タイミング (最小 Sample という実物を根拠に) まで記載済みであることを確認。先行の方針メモは二重管理になるため作らない
- **test-execution を実測ベースで翻案設置 (2026-08-14)**: 原則 (実行件数の確認までが検証・完了判定は全件実行) は KsSettingsView 版を翻案、コマンド・件数の得方・落とし穴は4ビルドルートで実測し直して記載。実測の発見 — kmp は `test` が曖昧エラーで `allTests` が正、ios は Swift Testing / XCTest で件数行が2系統 (`Executed 0 tests` に騙されない)、android は testDebugUnitTest のみ、android/ と kmp/ に local.properties (sdk.dir) が必要。未実測の先例知見 (Robolectric 描画限界等) は参照ノートに留めた。cross/conventions/test-execution.md として設置済み
- **runtime-behavior-verification を移植設置 (2026-08-14)**: KsSettingsView 版の骨格そのまま + 適応3点 — (1) 例示をダイアログ向け (表示/dismiss アニメーション・多段表示のタイミング) に差し替え (2) 4ビルドルートでの「実環境」定義 (症状が報告された形態で再現) を追加 (3) 出典事案の帰属を KsSettingsView と明記して保持。cross/conventions/runtime-behavior-verification.md として設置済み
- **aiforms-origin-reference を翻案設置 (2026-08-14)**: KsSettingsView 版の構造を踏襲しつつ3点適応 — (1) ローカルパスは reference-repositories.md 参照 (2) 仕様の正の序列 = README (711行) 一次 + コード補完を明記 (3) 適用対象は Dialog / Loading、Toast は新実装のため対象外。時限の終期は phase-7 完了想定。cross/conventions/aiforms-origin-reference.md として設置済み
- **config.yaml は最小反映で確定 (2026-08-14)**: domains は変更なし。core の domain-skills は「契約層でコードを持たないため割り当てなし」で確定 (コメント更新済み)。ui.screenshot は phase-4 で Sample の実物を実測してから記載 (phase-4 agenda に申し送り済み)。共通 skills は実物が出るまで空のまま
- **ドメイン導出規則はビルドルート基準で確定 (2026-08-14)**: concepts/rules.md の導出規則を phase-2 実物の4ビルドルート (cross/ADR-0004) に紐づけて明文化。kmp/ の androidMain actual は kmp、maui/ の platform handler は maui、ビルドルート横断の実物 (バージョンカタログ共有・scripts/・CI) は cross。既存 ADR 10件の配置変更なし。rules.md 自体が規約の正のため ADR は起票しない

## 調査結果 (2026-08-14 完了)

「実物のない規約は書かない」を守り、KsSettingsView / KsAppKMP の規約をプロジェクト実態に合わせて取り込んだ。

- **concepts 新規5件**: cross/conventions に aiforms-origin-reference (時限規約)・runtime-behavior-verification・test-execution (4ビルドルート実測ベース)、core/api に結果通知のルール・多段表示のルール (ksn-scout の移植元調査で裏取り、初見レビュー15件反映済み)
- **確定**: concepts/rules.md のドメイン導出規則 (ビルドルート基準)、config.yaml (core は契約層で domain-skills なし)
- **ADR**: cross/0001 (rebrand-policy)・cross/0003 (knowledge-intake-hybrid) を運用実績により accepted 昇格。core/0002〜0007 は phase-4 の実装検証を経て蒸留で昇格する方針
- **実測の副産物**: kmp は `./gradlew test` 不可で `allTests` が正 / ios は Swift Testing と XCTest で件数行が2系統 / android・kmp に local.properties (sdk.dir) が必要 (作成済み)
- **後続フェーズへの影響** (各 agenda に反映済み): phase-4 へ ui.screenshot 実測・共通仕様テストの検証項目候補4件を申し送り。レイアウト規則の全量仕様化は phase-5、Loading / Toast の結果の扱いは phase-7 / phase-8 へ先送り
- **lessons**: 「長命層の文書は提示前に初見レビュー」を process scope で捕捉 (count 1)

## TODO

- [x] 論点の解消
- [x] core/api/ に結果通知の意味論 (core/ADR-0003 由来) を書き下ろす
- [x] core/api/ に多段表示の意味論 (core/ADR-0006 由来) を書き下ろす
- [x] 調査結果のまとめ (history.md のフェーズまとめ参照)
- [x] ksn-roadmap で research 完了をマーク (2026-08-14)

## artifacts

- [scout-origin-notification-multidisplay.md](artifacts/scout-origin-notification-multidisplay.md) — 移植元の結果通知・多段表示挙動の ksn-scout 調査記録 (意味論2件の裏取り。README とコードの食い違い4件を含む)

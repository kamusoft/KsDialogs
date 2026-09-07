# Proposal: localize-dialog-error-messages

## Why

KsDialogs の 4 形態 (Native iOS / Native Android / .NET MAUI / KMP) が実行時に外へ出す診断文言 — 失敗型 (`DialogError` / `DialogException`) のメッセージ、throw 箇所で直接渡す例外文言、受理後失敗や演出失敗の警告ログ、その部品となる定数 — は全 61 件が日本語のハードコードで、ローカライズ機構は使っていない。読み手はライブラリを組み込む開発者であり、一般公開ライブラリとして英語圏の利用者がログと例外で日本語を読む状態になっている。利用者向け Agent Skills の診断表は en 版でも実装の日本語リテラルを引用しており (handbook `cross/user-skill-writing-style.md` の規約)、同じ問題が文書側にも映っている。

言語方針は探索で決定済み: **診断文言は英語固定、ローカライズしない** (cross/ADR-0015、accepted 2026-09-07)。

## What Changes

- ライブラリ本体 (samples / テストを除く) の日本語の診断文言 61 件を英語文言に置き換える。公開契約 (case 名・例外型・引数・throw される条件) は変えない
  - iOS Native 26 件: `DialogError` 7、`KsDialogsKmpError` 2、storyboard 非対応 init の `fatalError` 6、警告ログ 11
  - Android Native 12 件: `DialogException` 5、警告ログ 7
  - KMP 共有 3 件: iOS gateway の失敗メッセージ定数 3 (Android / iOS gateway が素通しする Native 文言は上の変更で英語になる)
  - MAUI 20 件: `DialogException` 6、gateway の例外文言 6、警告ログとその部品 5、iOS ブリッジの内部 Error 2、Android ブリッジの `error()` 1
- iOS と Android で対になる警告ログ (フック失敗・フック未完了・duration 不正・Toast の中身生成失敗) は**本文テンプレート** (動的な値の前の文) を同じ英語にする。動的なエラー説明の渡し方は各 OS のログ慣行に従い揃えない (iOS は本文に `{error}` を埋め込み、Android は `Log.w` の throwable 引数で渡す)。対応物が片側にしか無いもの (iOS だけの添付値未達、Android の共通 layout host が出す機能名を持たない収束警告) は同一化の対象外。文言の対応表はデルタスペックが持つ
- 文言に依存する KMP のテスト 4 assertion (`androidHostTest` 完全一致 1、`iosTest` 部分一致 3) を英語文言へ追随させる
- 利用者向け Skills の診断表 (en / ja × iOS / Android / MAUI / KMP × dialogs / loading / toast = 24 ファイル、各言語 12) のメッセージ列を英語文言に置き換え、型名の埋め込みは en / ja とも `{TypeName}` のプレースホルダで書く (メッセージ列を両言語で byte 一致させる)。診断表に「メッセージは現在の実装値で、安定 API ではない」の 1 文を添える。列構成・節の並びは変えない

影響する能力: ios-native / android-native / kmp-facade / maui-binding / user-skills

## Non-Goals

- **ソースコメント・doc comment の英語化** — プロジェクトのコメント規約は日本語 (`kasane/handbook/cross/comment-policy.md`、cross/ADR-0015 の対象外)
- **samples / テストコード内の日本語文言** (samples の `assertionFailure` 文言、テストが自分で投げる例外の文言など) — ライブラリが外へ出す文言ではない。samples を触ると sample-parity 規約の対象になり別の作業になる
- **エンドユーザー向け文字列のローカライズ基盤** — ライブラリはそのような文字列を持たない。持つようになったときに別途決める (cross/ADR-0015 Revisit When)
- **MAUI の 1 行登録の View 生成失敗を `DialogException` に合流させる件** — 例外型の設計判断を含み、ロードマップ package-distribution の MAUI パッケージングフェーズが扱う
- **handbook `cross/user-skill-writing-style.md` の「実装が日本語リテラルを持つ限り en でも日本語のまま引用する」規約の改訂** — 実装が英語になれば規約の条件が外れるので、本 change の Skills 更新は規約違反にならない。規約本文の書き換えは蒸留 (ksn-distill) で行う
- **`skills/.manifest.json` の更新と docs-refresh の実行** — 本 change は Skills を直接改稿するが、manifest の concepts スナップショットは蒸留完了後の docs-refresh が書く (`kasane/handbook/cross/docs-refresh-timing.md`)

## Impact

- 破壊的変更なし (呼ぶ側のソース互換の意味で)。case 名・例外型・引数・throw 条件は不変。実証手段はテストの全件実行 (iOS / Android / KMP / MAUI とその互換面) と Skills の parity 検査。カバーしない面: 利用者側のコードが例外メッセージの文字列一致に依存している場合は挙動が変わる
- **メッセージ文字列は互換契約ではない**: 契約は case 名・例外型・throw 条件であり、文言は現在の実装値。デルタスペックの完全一致 Scenario は今回の置き換えが対応表どおりであることを固定する受け入れ基準で、文言を安定 API に昇格させるものではない。将来の文言改訂は非破壊の修正として扱う (Skills の診断表にもその旨を 1 文添える)
- 影響範囲: 4 形態 × 約 25 ソースファイル、KMP テスト 2 ファイル、Skills 24 ファイル (各言語 12)、`scripts/scenario-id-coverage.py` の除外表 (静的 grep で受け入れる Scenario ID の登録)
- リスク: 訳語のブレ (形態間で同じ状況に別の英語が付く)。デルタスペックの対応表を唯一の文言源にし、レビュー・verify がそれと突き合わせることで抑える

## 級: M

公開 API は変えず可逆だが、4 形態横断で Skills まで追随が要り局所的ではない。文言の対応表をデルタスペックに持ってレビュー・verify・Skills 追随を機械的に突き合わせるため M (2026-09-07 オーナー確定)。

domain: cross

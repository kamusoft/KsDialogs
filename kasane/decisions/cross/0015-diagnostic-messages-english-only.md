---
id: 0015
title: ライブラリが外へ出す診断文言 (例外メッセージ・警告ログ) は英語固定とし、ローカライズしない
status: accepted
date: 2026-09-07
---

## Context

KsDialogs の 4 形態 (Native iOS / Native Android / .NET MAUI / KMP) が実行時に外へ出す文字列は、失敗型のメッセージ (iOS `DialogError` の `errorDescription`、Android / MAUI の `DialogException` の message)、throw 箇所で直接渡す例外文言、受理後失敗や演出失敗の警告ログのすべてが日本語のハードコードで、2026-09-07 の棚卸しで 61 件を数えた。OS のローカライズ機構 (`.strings` / strings.xml / `.resx`) は 1 か所も使っておらず、利用者 (エンドユーザー) に見える UI 文字列 (ボタン文言・アクセシビリティラベル等) はライブラリ本体に存在しない。つまりこれらの文字列はすべて、ライブラリを組み込む**開発者**がログや例外で読む診断文言である。

KMP 共有コードの `DialogException` は自前の文言を持たず、Native 側のメッセージを素通しする (gateway 固有の日本語定数を除く)。利用者向け Agent Skills (cross/ADR-0011) は 4 形態 × dialogs / loading / toast の診断表にメッセージ列を持ち、en 版でも実装の日本語リテラルをそのまま引用している (`kasane/handbook/cross/user-skill-writing-style.md`「実装が日本語リテラルを持つ限り en でも日本語のまま引用する」)。一般公開ライブラリとして、英語圏の利用者が en Skill とログの両方で日本語の診断文を読む状態になっている。

前提: これらの文言の読み手はライブラリを組み込む開発者であり、エンドユーザーに表示する文字列をライブラリが持たないこと。KsDialogs が一般公開ライブラリであること。

## Decision

- ライブラリ本体 (samples / テストを除く) が実行時に外へ出す診断文言 — 失敗型のメッセージ、throw 箇所で直接渡す例外文言、警告ログ、それらの部品となる定数 — は**英語固定**で書く。ローカライズ機構は持たない
- 対象は失敗型に限らず診断文言全般 (到達不能な init の `fatalError` 文言も含む) とし、「例外は英語・ログは日本語」のような 2 段の規約は置かない
- 4 形態で同じ方針を取る。iOS と Android で対になる警告ログは同じ英語文言で揃える
- ソースコメント・doc comment は本決定の対象外 (日本語のまま。`kasane/handbook/cross/comment-policy.md`)
- 利用者向け Skills の診断表は英語文言を引用する形へ追随し、handbook の「日本語リテラルを引用する」規約は撤回する

## Alternatives Considered

- **日本語固定のまま、en Skill に「メッセージは日本語で出る」と注記する**: 実装を変えずに済むが、公開ライブラリとして英語圏の利用者がログで原因を読めない状態が残る。却下
- **多言語化 (en + ja、OS のローカライズ機構で切り替え)**: 端末やアプリの言語で文言が切り替わるが、開発者向け診断文のために 4 形態へリソース基盤を新設し、case を足すたびに 2 言語 × 4 形態の保守が要る。端末言語で変わる文言を Skills の診断表に固定できず、「実物と食い違わない」保証が崩れる。却下
- **文言を持たず case / 例外型だけを公開し、文言は利用者に委ねる**: 保守は最も軽いが、ログに出る情報が減り、利用者が case 名から原因を推測することになる。却下
- **スコープを失敗型 (20 件) または例外に届く文言 (42 件) に絞る**: 「失敗型は英語、ログは日本語」の 2 段になり、次に case やログを足す人が毎回迷う。件数が手で追い切れる規模 (61 件) で、文言に依存するテストも KMP の 4 assertion だけと分かっているため、全件を対象にした。却下

## Consequences

- 正: どの言語環境でもログと例外に英語の診断文が出て、en Skill の診断表が実物と一致する
- 正: 「外へ出す診断文言は英語」の 1 行で規約が済み、handbook の日本語引用の例外規定が不要になる
- 負: 3 形態の失敗型 + gateway 定数 + 警告ログの文言を書き換え、KMP のテスト 4 assertion と Skills 12 ファイルの診断表を同じ change で追随させる作業が要る
- 負: 日本語圏の利用者も英語の診断文を読むことになる

## Revisit When

- ライブラリがエンドユーザーに表示する文字列 (既定ボタン文言・アクセシビリティラベル等) を持つようになったとき (その文字列は本決定の対象外で、ローカライズの要否を別途決める)
- 前提 (Context) が崩れたとき

---
出典: kasane/changes/archive/2026-09-07-localize-dialog-error-messages/exploration.md (検討した選択肢・決定事項) / 2026-09-07 の探索での議論
現行照合: 2026-09-07 確認 (実装完了時)。61 件をデルタスペックの対応表 (ja → en) どおりに置き換え (iOS 26 / Android 12 / KMP 3 / MAUI 20)、iOS と Android で対になる警告 4 種は本文テンプレートを同一化 (動的なエラー説明は iOS は本文へ埋め込み、Android は throwable 引数)。Context の「文言に依存するテストは KMP の 4 assertion だけ」は実測とずれており、MAUI 側にも 3 箇所あって追随した (archive の deviation.md)。文言は互換契約ではない (契約は case 名・例外型・throw 条件。文言の改訂は非破壊の修正) と proposal で確定し、規範は kasane/handbook/cross/diagnostic-message-language.md に置いた。handbook/cross/user-skill-writing-style.md の日本語引用の規定は撤回済み。合流した add-kmp-typed-show の共有コード由来の 2 文言も同日に英語化。判定: 維持

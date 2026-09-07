---
id: 0007
title: Sample は4ルートでパリティを保ち、一致単位は「デモ項目」とする
status: accepted
date: 2026-08-14
---

## Context

Sample は最初の実物 (add-vertical-slice の最小 Sample) 以降常に並走し、機能変更の完了条件に「パリティ準拠の Sample 通し」が含まれる。先例 KsSettingsView の sample-parity 規約 (同 cross/ADR-0016 + concepts の sample-parity.md) は「Sample はプラットフォーム間パリティの検証装置であり、Sample 自体がばらつくと画面差が本体の仕様差 (=バグ) か Sample の書き方の差か判別できない」を根拠に、全 platform 一字一句同一の文言・画面構成を要求する。ただし先例の一致単位「デモ画面」(画面タイトルで対応を取る) は、ページを持たないダイアログライブラリにはそのまま適用できない。

## Decision

KsSettingsView の parity 規約を4ルート (samples/ios / android / maui / kmp) 対応で翻案し、採用する:

- **一致単位は「デモ項目」**: ルートメニューの起動項目 (文言) + 表示されるダイアログの内容 (タイトル・本文・ボタン文言・デモデータ) + 閉じた後の結果表示、をワンセットで一致させる。対応はメニュー項目文言で取る
- 4ルート全部で同一メニュー構成・同一文言。KMP Sample が登録する Native View は samples/ios・android の View と重複実装になるが、共有せず各自書く (samples 間のコード共有は consumer 境界を濁す)
- 色は共通 SampleTheme の同一 RGBA を参照し、platform 固有 semantic color は禁止
- 許容差異・例外枠 (platform 固有 API のデモは固有区分、技術検証画面は「検証」区分)・禁止事項 (追跡なし片側放置・片側だけの改善・製品契約との混同) は先例の規約を踏襲する
- 規約 (一致単位・4ルート同一の文言・SampleTheme のトークン値) の**正は handbook が持つ** (`handbook/cross/sample-parity.md`)。`samples/README.md` はその利用者向け抜粋 (写像) であり、食い違ったら handbook が勝つ

## Alternatives Considered

- **緩い parity (画面構成のみ一致、文言は自由)**: 却下。文言差と仕様差が混ざり、検証装置として機能しない
- **規約なし (platform ごとに idiomatic なサンプル)**: 却下。先例で却下済みの型 — 見本価値は同一サンプルでも果たせる

## Consequences

- 正: Sample 間の差分を「本体の仕様差 (=バグ)」と判定でき、全機能フェーズの完了条件が機械的に検査可能になる
- 正: 先例と同じ規約型のため、KsSettingsView と横断した運用一貫性が保たれる
- 負: デモ項目の追加・変更は常に4ルート一斉が原則になり、維持コストがかかる (追跡付きの一時的片側先行で緩和)
- 負: KMP Sample の Native View 重複実装は、同一文言の多重管理を4ルート分に増やす
- 負: Sample がパリティの検証装置である以上、挙動観察のための一時的な改変 (重ね表示・入力欄つき検証 View 等) は原本へ戻すまでが観察作業に含まれる — 戻し漏れはパリティの根拠そのものを崩す

出典: kasane/roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md (2026-08-14: sample-parity 規約の確定) / 同 artifacts/scout-kssettingsview-precedents.md

現行照合: 2026-08-15 確認。samples/ios/KsDialogsSample/SampleText.swift ほか4ルートの `SampleText` が同一文言を持ち、規約の正は kasane/handbook/cross/sample-parity.md (確認時の在り処は kasane/concepts/cross/conventions/sample-parity.md)、samples/README.md はその利用者向け抜粋になっている。判定: 維持

現行照合: 2026-08-30 確認。規約本文を kasane/handbook/cross/sample-parity.md へ移送し、本 ADR の Decision 中の置き場もオーナー許可のもと handbook へ書き換えた (concepts → handbook の層移動であり、決定内容 — 一致単位はデモ項目・4ルート同一の文言・SampleTheme のトークン値 — は不変)。samples/README.md が利用者向け抜粋である関係も変わらない。判定: 維持

現行照合: 2026-09-05 確認。本 change で `samples/README.md` を廃止したため、Decision と過去の現行照合にある写像関係は履歴となった。現行の規約は kasane/handbook/cross/sample-parity.md だけが持ち、別の利用者向け抜粋は持たない。決定内容 (一致単位・4ルート同一の文言・SampleTheme のトークン値) は不変。判定: 維持

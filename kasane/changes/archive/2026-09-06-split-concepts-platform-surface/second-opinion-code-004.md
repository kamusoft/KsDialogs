# セカンドオピニオン: split-concepts-platform-surface (code-004)
**相方**: codex / **label**: so-code-split-concepts-platform-surface (同一セッションの 4 ターン目) / **日付**: 2026-09-05 / **対象**: 修正サイクル 3 (オーケストレーター直接修正) の確認 — kasane/changes/split-concepts-platform-surface/verification/ の kmp 禁止集合への 6 件復帰 (forbidden-tokens.json / baseline.md / forbidden-tokens-concepts.txt)
---
# 再レビュー結果

**最終判定: APPROVED**

前回のMajorは解消しています。Critical 0、Major 0、Minor 1です。

## 1. 前回Major

**解消**です。

以下6件がKMP禁止集合へ復帰し、142件になっていることを確認しました。

- `Dialog.shared`
- `Loading.shared`
- `Toast.shared`
- `viewModelFactoryNotRegistered`
- `viewModelAlreadyShowing`
- `Loading.shared.options`

`baseline.md:209-244` では、完全一致単位、handbook確定行の優先、Skill側検査の抽出範囲、`rules.md`との定義差が明確に記録されています。修正サイクル2の記録も履歴として残しつつ、サイクル3が現行判断であることを判別できます。

## 2. 追加2件への異論

異論はありません。

- `viewModelAlreadyShowing`
  - 実装上はKMP Swift入口から到達可能ですが、`kasane/handbook/cross/user-skill-api-listing.md:73` で「内部層・interop層」としてオーナー確定済みです。
  - fixtureは実装上の到達可能名の完全な一覧ではなく、利用者向けSkillへの掲載可否を検査するものなので、handbookの層判断を優先する扱いが妥当です。

- `Loading.shared.options`
  - KMP入口のdoc commentには設定先として現れますが、`kasane/handbook/cross/user-skill-api-listing.md:120` で対象Skill外と確定しています。
  - KMPの源泉conceptにも現れないため、禁止へ戻しても現行の正当な生成内容は落ちません。

## 残る指摘

### 🟡 Minor: 再実行結果のMAUI件数が誤記されている

**該当箇所**: `verification/forbidden-tokens-concepts.txt:35`

**問題点**: `maui: 禁止 151 件` とありますが、JSONの実数は154件です。独立に再計算しても次の結果でした。

- iOS: 200件、一致0
- Android: 178件、一致0
- MAUI: 154件、一致0
- KMP: 142件、一致0
- AiForms migration: 151件、一致0

**推奨修正**: 35行目の151を154へ訂正してください。判定結果の「一致0」自体には影響しません。

## 機械確認

- KMP禁止集合: 142件
- 復帰対象6件: すべて存在
- 全5範囲のconcept側突き合わせ: 一致0件
- JSON構文: 正常
- `git diff --check`: 成功
- ファイル書き込み、ビルド、テスト: 未実施


## 突き合わせ結果 (2026-09-05)

- 前回 Major: **確定 (解消)**。追加で戻した 2 件にも異論なし
- Minor (forbidden-tokens-concepts.txt の maui 件数表記): **採用** — 出力行が concept-exempt 3 件を除いた後の数 (151) を書いていた。「禁止 154 件 (concept-exempt 3 件を除いた 151 件で突き合わせ)」に表記を直した (判定「一致 0」は不変)
- 中間レビュー (tasks グループ 1〜4) はホスト側 review-003 APPROVED / 相方 APPROVED で収束。グループ 5〜7 へ進む

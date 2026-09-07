---
type: concept
title: 多段表示のルール (ダイアログの重ね出し)
description: ダイアログ表示中にさらにダイアログを出したときの core 契約 — ライブラリはスタックを管理せず OS の提示機構に任せる。保証する挙動4点と、保証しない挙動を定める
tags: [dialog, multi-display, contract]
timestamp: 2026-08-22
---

# 多段表示のルール (ダイアログの重ね出し)

この文書は、全形態 (iOS Native / Android Native / MAUI / KMP) 共通の「ダイアログ表示中に、さらにダイアログを出す (重ね出しする)」ときのルールを定める。読むと、重ね出しで何が保証され、何が OS 任せで保証されないかが分かる。

本文中の**移植元**は AiForms.Maui.Dialogs (本ライブラリの移植元) を指す。参照ルールは [移植元 AiForms.Maui.Dialogs の参照](../../../handbook/cross/aiforms-origin-reference.md)、リポジトリの在り処は [参考リポジトリの在り処](../../cross/reference/reference-repositories.md) が定める。

**この文書が正であり、実装はここに合わせる**。根拠決定は [core/ADR-0006](../../../decisions/core/0006-multi-display-os-delegation.md) (accepted 済み。ADR が持つのは「なぜそう決めたか」で、「何が成り立つか」の正はこの文書側にある)。OS ごとの挙動差が実測で判明したら、この文書に追記して吸収する。

- core は「全形態が共有する契約」の層 (層の区分は [concepts 配置ルール](../../rules.md))。公開名・署名・コード例は各形態の公開面が持つ (末尾の「形態別の公開面」)
- ここに書いた観察可能な挙動には Scenario ID `PB-MD-01`〜`PB-MD-05` が振られており、両 Native 実装の同名テストで固定されている ([core/ADR-0016](../../../decisions/core/0016-behavior-spec-scenario-tests.md))

## 基本ルール (保証する挙動4点)

ライブラリは「今何枚出ているか」を自分で管理しない。重なりの管理は **OS の提示機構** — iOS なら ViewController の present の連なり、Android なら Dialog ウィンドウ — にそのまま任せる。移植元も同じ作りである。保証するのは次の4点だけ:

1. **重ね出しできる**: ダイアログ表示中でも show を呼べる
2. **結果は1枚ずつ独立**: 重なっていても、各 show はそれぞれ独立に結果を返す ([結果通知のルール](result-notification-semantics.md) がそのまま成り立つ)。結果を報告する部品 (**結果報告口** `DialogNotifier`) も show 1回ごとに新しいものが渡る
3. **後から出したものが手前**: 手前の1枚だけがユーザー操作を受け、下の画面はタッチが効かない
4. **枚数は数えない・公開しない**: ライブラリは重なりの枚数や一覧を持たず、API としても公開しない

実測 (2026-08-15 / iPhone 17 Simulator・Android Emulator API 35) では、**上から順に閉じる場合と、重ね出し中に外側をタップした場合のどちらも iOS / Android で同じ観察結果**になった — 手前の1枚だけが自分の結果を受け取り、下のダイアログは表示されたまま結果未確定で残る。上の保証4点の 2・3 のとおりで、この範囲に OS 差はない (`PB-MD-01`・`PB-MD-02`・`PB-MD-03`)。

報告を経ずに**器** (ダイアログを載せている表示コンテナ) が画面から外れたときに show が cancelled でちょうど1回確定すること ([結果通知のルール](result-notification-semantics.md) のルール4) も、重ね出しの有無によらず成り立つ (`PB-MD-05`)。

## 保証しないこと

**保証しないのは「どう見えるか」であって、「結果が返るかどうか」ではない。** 下記のどの状況でも、各 show が completed か cancelled をちょうど1回返すことは崩れない ([結果通知のルール](result-notification-semantics.md) のルール3・4)。

### 閉じる順序と、下を先に閉じたときの見え方 (`PB-MD-04`)

上から順に閉じる (後入れ先出し) 以外の閉じ方の見え方は保証しない。細部は OS の提示機構に従い、**実測 (2026-08-15) で OS 差が確認された**。下の1枚に対して先に結果を報告したときの観察結果は次のとおり:

| OS | 見え方 | 上の show の結果 |
|---|---|---|
| iOS | 上下とも画面から消える (提示の連なりごと外れる) | **cancelled で確定して返る** |
| Android | 下だけが閉じ、上は表示されたまま残る | そのまま操作でき、後から通常どおり completed で返る |

この差はどちらかに揃えず、**OS 差として明文化して吸収する**。揃えようとすると OS の提示機構と二重管理になり、委譲の前提 (基本ルール) が崩れるためである。結果値をどの形で受け取るか (完了とキャンセルの綴りと形) は形態ごとに違うので、各形態の公開面の「失敗とキャンセルの形」を参照する ([iOS](../../ios/api/dialog-surface.md) / [Android](../../android/api/dialog-surface.md) / [MAUI](../../maui/api/dialog-surface.md) / [KMP](../../kmp/api/dialog-surface.md))。

なお、この状況は**アプリコードが下の結果報告口 (`DialogNotifier`) を保持して先に報告した場合にのみ起きる** — 中身へ渡された報告口をアプリ側が握っておき、下のダイアログの外から完了・キャンセルを報告した場合である。ユーザー操作 (完了 / キャンセル / 外側タップ / 戻るボタン) は常に手前の1枚にしか届かないため、ユーザー操作だけで下を先に閉じることはできない。

iOS で上の1枚が消えるのは**提示の連なりが外れた結果 (OS 発の器の消失)** であり、ライブラリが閉鎖を実行したわけではない。したがって上の1枚に退出の演出を添付 (中身の定義に演出を結びつけて供給すること) していても実行されず、即座に cancelled が配送される ([トランジションのルール](transition-semantics.md))。

### 閉じるアニメーション中のタイミング

閉じ始めてから結果が呼び出し元へ渡るまでのラグは、**ライブラリの既定の退出処理 (クロスフェード 250 ミリ秒)** の分だけかかる。演出を添付していればその時間になる。この待ち時間そのものは契約として固定しないが、「結果が渡るのは器が撤去された後」という順序は保証する ([トランジションのルール](transition-semantics.md))。移植元の Android は閉じ終わるまで約 0.5 秒かかっていた。

### OS が戻る操作を先に消費する場合

Android でソフトキーボードが出ているときの1回目の戻るボタンは、OS の既定処理でキーボードだけを閉じ、ダイアログには届かない (2回目で cancelled になる)。ライブラリはこれを保証せず OS の既定に委ねる — 実測と方針は [結果通知のルール](result-notification-semantics.md) の「どの操作がキャンセルになるか」を参照。

### Toast / Loading とダイアログの前後関係

移植元は platform 間で実装方式が揃っておらず (iOS の Loading はウィンドウへ直接ビューを貼る方式、Android はダイアログ方式)、重なり順が一致しない。ここのルール化は Loading / Toast 各機能の実装時に行う。

## 形態別の公開面

重ね出しに専用の API はなく、使うのは通常の show である。各形態での show の綴り・戻りの形・結果値の受け取り方は次の公開面が定める:

- [iOS の Dialog 公開面](../../ios/api/dialog-surface.md)
- [Android の Dialog 公開面](../../android/api/dialog-surface.md)
- [MAUI の Dialog 公開面](../../maui/api/dialog-surface.md)
- [KMP の Dialog 公開面](../../kmp/api/dialog-surface.md)

## 出典

- [core/ADR-0006](../../../decisions/core/0006-multi-display-os-delegation.md) — 決定 (OS の提示機構への委譲)。OS 差の明文化に至った経緯は同 ADR の「実装結果」節
- [core/ADR-0016](../../../decisions/core/0016-behavior-spec-scenario-tests.md) — 決定 (挙動系の共通仕様は同名 Scenario テストで検証し、OS 間差はこの文書の差分表を正とする)
- 縦串スライス (Dialog 1本の4形態貫通) での実測 (2026-08-15): iPhone 17 Simulator (iOS 26.5) と Android Emulator API 35 (1080x2340) での実機観測。上から順に閉じる / 重ね出し中の外側タップ / 下を先に閉じる / キーボード表示中の戻るボタン の4ケース
- 移植元の挙動調査 (2026-08-14): 下記の調査記録を参照

移植元の調査対象は AiForms.Maui.Dialogs リポジトリの iOS / Android の提示ヘルパ (`Native/iOS/DialogHelpers.cs`・`Native/Android/DialogHelpers.cs`) と再利用ダイアログの実装 (`Dialog/ReusableDialog.iOS.cs` / `.Android.cs`)。調査記録は [scout-origin-notification-multidisplay.md](../../../roadmaps/archive/2026-09-04-library-foundation/phases/phase-3-harness-setup/artifacts/scout-origin-notification-multidisplay.md)。

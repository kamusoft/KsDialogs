---
type: concept
title: 多段表示のルール (ダイアログの重ね出し)
description: ダイアログ表示中にさらにダイアログを出したときの core 契約 — ライブラリはスタックを管理せず OS の提示機構に任せる。保証する挙動4点と、保証しない挙動を定める。出す先の画面を待っている Dialog が複数あるときの出る順番 (呼んだ順・後から呼んだものが手前・待っている間の show は後ろに並ぶ) も定める
tags: [dialog, multi-display, contract]
timestamp: 2026-09-29
---

# 多段表示のルール (ダイアログの重ね出し)

この文書は、全形態 (iOS Native / Android Native / MAUI / KMP) 共通の「ダイアログ表示中に、さらにダイアログを出す (重ね出しする)」ときのルールを定める。読むと、重ね出しで何が保証され、何が OS 任せで保証されないかが分かる。

本文中の**移植元**は AiForms.Maui.Dialogs (本ライブラリの移植元) を指す。参照ルールは [移植元 AiForms.Maui.Dialogs の参照](../../../handbook/cross/aiforms-origin-reference.md)。ローカルでは `../AiForms.Maui.Dialogs` で参照する。

この文書は、4 形態の実装とテストが満たしている挙動を記述する (一次情報はコードとテスト)。根拠決定は [core/ADR-0006](../../../decisions/core/0006-multi-display-os-delegation.md) (accepted 済み。ADR が持つのは「なぜそう決めたか」)。OS ごとの挙動差が実測で判明したら、この文書に追記して吸収する。

- core は「全形態が共有する契約」の層 (層の区分は [concepts 配置ルール](../../rules.md))。公開名・署名・コード例は各形態の公開面が持つ (末尾の「形態別の公開面」)
- 重ね出しの基本の挙動には Scenario ID `PB-MD-01`〜`PB-MD-05` が振られており、両 Native 実装の同名テストで固定されている ([core/ADR-0016](../../../decisions/core/0016-behavior-spec-scenario-tests.md))
- 提示先を待つ挙動 (下記「提示先を待っている Dialog の出る順番」を含む) の Scenario ID は `PB-HW-*` で、[結果通知のルール](result-notification-semantics.md) の「出す先の画面が無いとき」が案内する

## 基本ルール (保証する挙動4点)

ライブラリは「今何枚出ているか」を自分で管理しない。重なりの管理は **OS の提示機構** — iOS なら ViewController の present の連なり、Android なら Dialog ウィンドウ — にそのまま任せる。移植元も同じ作りである。保証するのは次の4点だけ:

1. **重ね出しできる**: ダイアログ表示中でも show を呼べる
2. **結果は1枚ずつ独立**: 重なっていても、各 show はそれぞれ独立に結果を返す ([結果通知のルール](result-notification-semantics.md) がそのまま成り立つ)。結果を報告する部品 (**結果報告口** `DialogNotifier`) も show 1回ごとに新しいものが渡る
3. **後から出したものが手前**: 手前の1枚だけがユーザー操作を受け、下の画面はタッチが効かない
4. **枚数は数えない・公開しない**: ライブラリは重なりの枚数や一覧を持たず、API としても公開しない

実測 (2026-08-15 / iPhone 17 Simulator・Android Emulator API 35) では、**上から順に閉じる場合と、重ね出し中に外側をタップした場合のどちらも iOS / Android で同じ観察結果**になった — 手前の1枚だけが自分の結果を受け取り、下のダイアログは表示されたまま結果未確定で残る。上の保証4点の 2・3 のとおりで、この範囲に OS 差はない (`PB-MD-01`・`PB-MD-02`・`PB-MD-03`)。

報告を経ずに**器** (ダイアログを載せている表示コンテナ) が画面から外れたときに show が cancelled でちょうど1回確定すること ([結果通知のルール](result-notification-semantics.md) のルール4) も、重ね出しの有無によらず成り立つ (`PB-MD-05`)。

## 提示先を待っている Dialog の出る順番

出す先の画面 (**提示先**) が無いときに呼ばれた show は、失敗せずに提示先の出現を待つ ([結果通知のルール](result-notification-semantics.md) の「出す先の画面が無いとき」)。待っている Dialog が複数あるとき、ライブラリは OS に渡す前の待ちを呼んだ順の列で持ち、次の順で出す。

show はすべてこの列を通るが、列が空で、列から出た 1 枚が画面に載る途中でもなく、提示先があれば、待たずにそのまま OS に渡す。1 枚ずつ出すのは列で待っていた Dialog だけで、提示先があるときに続けて呼んだ show は直列にならない (これまでどおり OS の提示機構に任せる)。

- 提示先が現れたら、列で待っていた Dialog を呼んだ順に 1 枚ずつ載せる。前の 1 枚が画面に載り終えてから (閉じるのを待つのではない) 次の 1 枚を載せるので、後から呼んだものが手前に重なる (保証3と同じ見え方。`PB-HW-06`)。「載り終えた」は、iOS では present の完了通知、Android では器のウィンドウを追加した時点である
- 待っている Dialog があるうちに呼ばれた show は、提示先があってもその後ろに並び、先に待っている Dialog を追い越さない (`PB-HW-08`)
- 打ち切りや結果の確定で待ちを終えた 1 枚は、順番を待たずにその場で列から外れる
- 列から出た 1 枚を OS が載せられなかった (cancelled で返る — [結果通知のルール](result-notification-semantics.md) のルール4) 場合も、列で次に待っている Dialog はそのまま表示に進む
- 列はアプリ全体で 1 つで、既定のエントリ・DI で得たインスタンス・KMP・MAUI のどの入口から呼んでも同じ列に並ぶ

この列は OS に渡す前の待ちであり、渡したあとの重なりは今までどおり OS の提示機構に任せる。列の長さも公開しないので、保証4 (枚数は数えない・公開しない) とも両立する。

### 「呼んだ順」が指す範囲

| 呼び方 | 列に並ぶ順 |
|---|---|
| 同じ UI スレッドから続けて呼んだ show | 呼んだ順 |
| UI スレッド以外から呼んだ show (同じスレッドから続けて呼んだ場合も、別々のスレッドどうしも) | 保証しない。UI スレッドに着いた順に並ぶ |
| configure が中断する (非同期の configure が await で待つ) 型指定 show | 保証しない。configure を終えて準備ができた順に並ぶ |

型指定 show の扱いは、提示先がある場合の見え方に揃えたものである。提示先があるときも、configure が中断している間に後から呼んだ Dialog が先に出る。呼んだ時点で順番を取る形にすると、configure の遅い Dialog が後ろをすべて待たせ、提示先の有無で見え方が変わってしまう。

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

Loading と Toast の器は、ここで述べた OS の提示の連なりに**参加しない**専用の器である (core/ADR-0026・0030)。そのためダイアログとの前後関係は、多段表示の仕組みではなく各機能の決定で決まる:

| 組み合わせ | 前後関係 | 根拠 |
|---|---|---|
| Loading とダイアログ | 視覚・入力の双方で常に Loading が手前 | core/ADR-0022 ([Loading のルール](loading-semantics.md) の「器の性質」) |
| Toast とダイアログ | 契約で保証しない (多段表示と同じく、重なり順を数えも公開もしない) | core/ADR-0006 の線を core/ADR-0030 が維持 ([Toast のルール](toast-semantics.md) の「機能間の前後関係」) |
| Loading と Toast | 起動順によらず常に Loading が前面 | core/ADR-0030 |

移植元は platform 間で実装方式が揃っておらず (iOS の Loading はウィンドウへ直接ビューを貼る方式、Android はダイアログ方式)、重なり順が一致しなかった。上の表はこの不揃いを本ライブラリの決定で揃えたものである。

## 形態別の公開面

重ね出しに専用の API はなく、使うのは通常の show である。各形態での show の綴り・戻りの形・結果値の受け取り方は次の公開面が定める:

- [iOS の Dialog 公開面](../../ios/api/dialog-surface.md)
- [Android の Dialog 公開面](../../android/api/dialog-surface.md)
- [MAUI の Dialog 公開面](../../maui/api/dialog-surface.md)
- [KMP の Dialog 公開面](../../kmp/api/dialog-surface.md)

## 出典

- [core/ADR-0006](../../../decisions/core/0006-multi-display-os-delegation.md) — 決定 (OS の提示機構への委譲)。下の段を先に閉じたときの OS 差は同 ADR の Consequences と現行照合 footer にある
- [core/ADR-0016](../../../decisions/core/0016-behavior-spec-scenario-tests.md) — 決定 (挙動系の共通仕様は同名 Scenario テストで検証し、OS 間差はこの文書の差分表を正とする)
- [core/ADR-0039](../../../decisions/core/0039-wait-for-host-appearance.md) — 決定 (出す先の画面が無いときは失敗せずに待つ)
- 待っている Dialog の列と「呼んだ順」の範囲の経緯 (2026-09-27〜28): [wait-for-host-appearance の実装乖離メモ](../../../changes/archive/2026-09-29-wait-for-host-appearance/deviation.md) (過去の変更の作業記録)
- 縦串スライス (Dialog 1本の4形態貫通) での実測 (2026-08-15): iPhone 17 Simulator (iOS 26.5) と Android Emulator API 35 (1080x2340) での実機観測。上から順に閉じる / 重ね出し中の外側タップ / 下を先に閉じる / キーボード表示中の戻るボタン の4ケース
- 移植元の挙動調査 (2026-08-14): 下記の調査記録を参照

移植元の調査対象は AiForms.Maui.Dialogs リポジトリの iOS / Android の提示ヘルパ (`Native/iOS/DialogHelpers.cs`・`Native/Android/DialogHelpers.cs`) と再利用ダイアログの実装 (`Dialog/ReusableDialog.iOS.cs` / `.Android.cs`)。調査記録は [scout-origin-notification-multidisplay.md](../../../roadmaps/archive/2026-09-04-library-foundation/phases/phase-3-harness-setup/artifacts/scout-origin-notification-multidisplay.md)。

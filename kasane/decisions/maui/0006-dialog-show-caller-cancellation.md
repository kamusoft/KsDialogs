---
id: 0006
title: MAUI の Dialog の show に呼び出し元の打ち切りを足し、待っている Dialog と表示中の Dialog をコードから止められるようにする
status: accepted
date: 2026-09-27
---

## Context

MAUI の Dialog の show (`ShowAsync`) は、呼び出し元の打ち切りの手段を受け取らなかった (2026-09-27 コード確認)。ADR で決めた振る舞いではなく、そうなっている状態だった。

ほかの形態は、呼び出し元の打ち切りを受け付ける。iOS は Task のキャンセル、Android・KMP はコルーチンのキャンセルで表示中の Dialog を閉じ、結果が未確定なら cancelled で確定する (core の結果通知の契約「呼び出し元の打ち切り」)。同じ契約の形態ごとの見え方の表で、MAUI は「経路なし」と記述されていた。

core/ADR-0039 で、提示先が無いまま呼ばれた Dialog は、失敗せず、上限なしで提示先の出現を待つことになった。待ちを終わらせるのは結果の確定か呼び出し元の打ち切りだけで、打ち切りを持たない MAUI では、待ち始めた Dialog をコードから止める手段が無い。

前提: MAUI の Dialog の表示は、両 OS の Native にブリッジ経由で委譲する。打ち切りも、Native の打ち切りへ中継できる。

## Decision

MAUI の Dialog の show に、呼び出し元の打ち切りを受け付ける手段を足す。全入口で揃える。打ち切られたときの振る舞いは、ほかの形態と同じ契約 (結果通知の「呼び出し元の打ち切り」) に従う。提示先の出現を待っている間なら、Dialog は一度も表示されずに終わる。表示中なら、Dialog を閉じる。

範囲に含めないもの:

- 打ち切られたことが呼び出し元にどう見えるか (結果で返すか、例外で伝えるか) と、引数の形。C# の慣習と既存の契約の表に照らして設計が決める
- Loading・Toast の入口。どちらも自分の寿命 (処理の終了・表示時間) で終わるので、打ち切りが無くても待ちは止まる

理由: core/ADR-0039 で Dialog の待ちには上限が無くなり、止める手段が呼び出し元の打ち切りだけになった。MAUI だけ止められない差を残すと、画面が現れない場所から呼んだ Dialog が MAUI でだけ永久に返らない。打ち切りを足せば、待ちの止め方も表示中の Dialog の閉じ方も、ほかの形態と揃う。

## Alternatives Considered

- **MAUI は打ち切れないままにし、形態の差として契約に残す** — 却下。Dialog の待ちに上限が無い以上、MAUI でだけ、待ち始めた Dialog を止める手段が無くなる

## Consequences

- 正: MAUI でも、待っている Dialog と表示中の Dialog をコードから止められる
- 正: 結果通知の契約の形態別の表から MAUI の「経路なし」が消え、呼び出し元の打ち切りが全形態で揃う
- 負: MAUI の Dialog の show の全入口に、引数が増える
- 負: 打ち切りを、MAUI から両 OS の Native へ中継する仕組みが要る。ブリッジは Native の show を打ち切れる形で持つ必要がある

## Revisit When

前提 (Context) が崩れたとき

出典:
- `kasane/changes/archive/2026-09-29-wait-for-host-appearance/exploration.md` (Dialog の現状・決定事項。2026-09-27 の探索でオーナーが「この change で MAUI に打ち切りを足す」案を採用)
- `kasane/changes/archive/2026-09-29-wait-for-host-appearance/proposal.md` (Non-Goals の Loading・Toast の入口) / `design.md` (Decision 6: 打ち切りの見え方と引数の形、ブリッジでの中継)
- `kasane/concepts/core/api/result-notification-semantics.md` (呼び出し元の打ち切りのルールと形態別の見え方) / `kasane/concepts/maui/api/dialog-surface.md` (MAUI の Dialog の公開面)

関連: core/ADR-0039 (提示先が無いまま呼ばれた Dialog は上限なしで提示先の出現を待つ。本決定はその待ちを MAUI でも止められるようにする)

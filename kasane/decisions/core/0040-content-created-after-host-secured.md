---
id: 0040
title: Toast・Loading の中身は全形態で提示先を確保してから作り、提示先が無いまま始まった Loading の生成失敗は表示だけを諦める
status: accepted
date: 2026-09-27
amends: 0033
---

## Context

core/ADR-0033 は、利用者の View factory の失敗を言語境界の内側で捕捉し、各機能の既存の失敗契約へ合流させると決めた。合流先は、Toast はその表示 1 枚の破棄、Loading は開始そのものの失敗、Dialog は閉鎖通知の失敗である。Loading の合流先は、中身を開始の時点で作ることを前提にしている。

core/ADR-0039 で、提示先が無いまま呼ばれた Loading・Toast は、失敗せず提示先の出現を待つことになった。一方、中身をいつ作るかは形態で違っていた (2026-09-27 コード確認)。

| 形態 | Toast の中身 | Loading の中身 |
|---|---|---|
| Android | 提示先を確保してから作る | 開始時点で提示先があれば開始時点、無ければ提示先が現れた時点で作る。後者の生成に失敗したら、警告を残して表示だけを諦める |
| iOS | 受理の時点で作る | 開始の時点で作る。失敗は開始の失敗 |

Android は中身 (View) を提示先の Activity を文脈にして作るので、提示先が無い開始時点では中身を作れず、Loading の生成失敗を開始の失敗にできない。Android の振る舞いは ADR-0033 の Loading の合流先と食い違っていたが、記録は無かった。core の Toast の契約は、中身の生成の時点の違い (Android は提示先の確保後、iOS は受理時点) を承認済みの差として記述していた。Toast の型指定経路では、VM factory と configure が中身の生成と一続きで走る (core/ADR-0035 の現行照合の観測)。

MAUI の中身の供給は、MAUI の画面の文脈を必要とする。iOS が受理・開始の時点で中身を作ると、MAUI の画面ができる前の呼び出しで供給が失敗し、Loading は開始が失敗し、Toast は捨てられる。どちらも core/ADR-0039 の「失敗せず待つ」に反する。

前提: Android の View は、提示先の Activity を文脈にしないと作れない。MAUI の中身は、MAUI の画面の文脈が無いと作れない。

## Decision

Toast・Loading の中身は、全形態で、提示先を確保してから作る。Toast は提示先に取り付ける時点で作る。Loading は、開始時点で提示先があれば開始時点で作り、無ければ提示先が現れた時点で作る。

core/ADR-0033 の決定のうち、Loading の失敗の合流先 (開始そのものの失敗) を、提示先が無いまま始まった Loading に限って本決定で置き換える。この場合の中身の生成の失敗は、警告ログを残して表示だけを諦め、合流状態と処理は続け、呼び出し元へは返さない。ADR-0033 のほかの決定 (境界の内側での捕捉、MAUI の境界で例外を値に変えて渡すこと、開始時点で中身を作る場合の開始の失敗、Dialog と Toast の合流先) は維持する。

範囲に含めないもの: View factory の登録の解決。未登録の失敗は今までどおり受理・開始の時点で起き、構成ミスは呼び出し元へ返る。

理由: 提示先の無い時点で中身を作れない形態 (Android) と、作ると失敗しうる形態 (MAUI) がある。中身を作る時点を提示先の確保後に揃えれば、全形態で同じ規則になり、core/ADR-0039 の「失敗せず待つ」も成り立つ。その場合、提示先が無いまま始まった Loading では中身を作るのが処理の開始より後になり、開始の失敗として返す機会はもう無い。処理を止めずに表示だけを諦めるのは、Android がすでに取っていた扱いである。

## Alternatives Considered

- **ADR-0033 に従う (iOS は中身を開始・受理の時点で作ったままにし、取り付けだけを遅らせる)** — 却下
  - Android は開始時点で中身を作れないので、ADR-0033 との食い違いが Android に残る
  - 提示先が無いまま始まった Loading の生成失敗の扱いが、OS で分かれる (iOS は開始の失敗、Android は表示の諦め)
  - MAUI の iOS で、MAUI の画面ができる前に始めた Loading の開始が失敗する
- **MAUI の側で画面の文脈が取れるまで待ってから Native を呼ぶ** — 却下。Native の待ちと MAUI の待ちが二重になる。Toast の計時や Loading の処理の開始が MAUI の待ちに引きずられ、Native の契約と食い違う

## Consequences

- 正: Toast・Loading の中身を作る時点が全形態で揃い、core の Toast の契約から中身の生成の時点の承認済みの差が消える
- 正: MAUI の中身の供給が、Native の提示先を確保した後に呼ばれる
- 正: Android の振る舞いと決定が一致し、記録の無い食い違いが消える
- 負: 提示先が無いまま始まった Loading で factory が失敗すると、呼び出し元には返らず、警告ログでしか気づけない
- 負: 提示先が現れないまま満了した Toast では、型指定経路の VM factory と configure が一度も呼ばれない。iOS では、受理の時点で呼ばれていたものが呼ばれなくなる
- 負: iOS の Toast・Loading に、中身の指定を控えておき、提示先を確保してから作る仕組みが要る

## Revisit When

前提 (Context) が崩れたとき

出典:
- `kasane/changes/archive/2026-09-29-wait-for-host-appearance/design.md` (Decision 2 とその代替案)
- `kasane/changes/archive/2026-09-29-wait-for-host-appearance/second-opinion-spec-001.md` (Major 1 と突き合わせ結果。提案レビューで ADR-0033 との衝突が指摘され、2026-09-27 にオーナーが ADR-0033 の改訂を選択)
- `kasane/decisions/core/0033-user-factory-failure-boundary.md` (Decision の Loading の合流先)
- `kasane/decisions/core/0035-loading-toast-typed-show-vm-factory.md` (現行照合の、Android の Toast の中身の生成時点の観測)

関連: core/ADR-0039 (提示先が無いまま呼ばれた Dialog・Loading・Toast は、失敗せず提示先の出現を待つ。本決定はそのための中身の生成の時点を定める)
関連: core/ADR-0035 (Loading・Toast の型指定 show。Toast の型指定経路の VM factory・configure は中身の生成と一続きで走るので、本決定により全形態で取り付けの時点に走る)

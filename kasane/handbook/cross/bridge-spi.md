---
kind: rule
applies-when:
  always: false
  paths: ["ios/Sources/**", "maui/macios/native/**"]
  tasks: [MAUI の iOS 互換面が iOS Native の公開 API に無い口を必要とするときの実装とレビュー]
title: ブリッジ専用の口 (@_spi)
description: MAUI の iOS 互換面 (別モジュール) だけが使う口を iOS Native に置くときは、公開面を増やさずに `@_spi(KsDialogsBridge)` で出す。置き方・使う側の import・利用者向けの公開面と Skill に載せないこと・同じモジュールの呼び出し口では使わないこと
timestamp: 2026-09-29
---

# ブリッジ専用の口 (@_spi)

この文書は、MAUI の iOS 互換面 (`maui/macios/native/KsDialogsMauiBridge/`) だけが使う口を iOS Native (`ios/Sources/KsDialogs/`) に置くときの書き方を定める。読むと、利用者向けの公開面を増やさずに別モジュールのブリッジへ口を渡す方法と、その口をどこに書いてはいけないかが分かる。

## いつ当たるか

MAUI の iOS 互換面は iOS Native とは別のモジュールで、Native の `public` な API しか呼べない。互換面が、Native の内部の機能 (`internal` の型や関数) を使わないと契約を満たせないと分かったときに当たる。最初の例は、Task を作って UI スレッドへ移る前に表示の順番を同期で取る口 (`Dialog.reserveShow()` と `show(_:placement:reservation:)`、型 `DialogShowReservation`) である。

同じモジュールにある呼び出し口 (KMP の互換面 `ios/Sources/KsDialogs/Interop/`) は `internal` をそのまま使えるので、この規約の対象ではない。SPI にせず、`internal` のまま呼ぶ。

## 書き方

| 側 | 書くこと |
|---|---|
| iOS Native (出す側) | 口の宣言に `public` と `@_spi(KsDialogsBridge)` を付ける。doc コメントに、使う側 (どのブリッジが何のために使うか) と「利用者向けの公開面には出さない」ことを書く |
| MAUI の iOS 互換面 (使う側) | `@_spi(KsDialogsBridge) import KsDialogs` で読み込む |
| iOS Native のテスト | その口を直接確かめるテストだけ、`@_spi(KsDialogsBridge) @testable import KsDialogs` で読み込む |

SPI の名前は `KsDialogsBridge` を使う。

## 載せない場所

SPI の口は利用者が使う API ではない。次の場所には名前を書かない。

- concepts の各形態の公開面 (`kasane/concepts/<platform>/api/`) — 利用者向け Skill の API 名網羅検査の源泉になるため、書くと Skill の掲載漏れとして報告される
- 利用者向け Skill (`skills/**`) と README — 網羅検査が拾った場合は、[利用者向け Skill の API 掲載基準](user-skill-api-listing.md) の「内部層・interop 層」に当たる

口が保証する振る舞い (例: 互換面から続けて呼んだ show も呼んだ順に並ぶ) は、名前を出さずに core の契約として concepts に書く。

到達状態: concepts の公開面・`skills/**`・README に SPI の口の名前が現れず、口を足しても利用者向けの公開面が増えていない。

## なぜ

公開 API を足すと、利用者向けの面と Skill が増え、0.x の間も破壊的変更の対象になる。`internal` のままでは別モジュールの互換面から呼べない。`@_spi` は、名前を知って明示的に import した側だけが使える `public` で、通常の `import KsDialogs` からは見えない。互換面が必要とする口を、利用者の面を広げずに渡せる。

最初の例では、Task の開始順に頼って表示の順番を決める形は、実測では安定していたが言語の保証が確認できず、iOS 17 の実行環境でも確かめられなかった。そこで順番を同期で取る口を互換面に渡す必要があり、公開面を増やさない方法としてオーナーが `@_spi` を選んだ (2026-09-28。経緯は `kasane/changes/archive/2026-09-29-wait-for-host-appearance/deviation.md`)。

## 関連

- [利用者向け Skill の API 掲載基準](user-skill-api-listing.md) — 網羅検査の仕分けと「内部層・interop 層」の除外
- [ソースコメント規約](comment-policy.md) — doc コメントに作業文書のパスや変更識別子を書かない

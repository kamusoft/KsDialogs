---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-26
last-seen: 2026-09-26
evidence:
  - add-page-layout-area (dialog-contract が相方スペックレビュー #5 の採用で「候補は提示先と同じ window (Android は同じ Activity のウィンドウ) に属するもの」を全形態共通の不変条件に加えたが、maui-binding の Scenario「モーダルが優先される」はモーダルページの PlatformView を基準に求めていた。MAUI の Android 版はモーダルページを Activity とは別のダイアログウィンドウに載せるため、Android では両記述が両立せず、MAUI 実装ワーカーが 4.4 の実配置テスト (モーダル + NavigationPage で FAIL) で停止した)
---

## ルール文

提示先・所属ウィンドウ・座標系のような「どの画面の器に属するか」を全形態共通の不変条件として spec に足すとき、その条件が掛かる各形態の Scenario (特に上位フレームワーク — MAUI・Compose・SwiftUI — が自前でウィンドウや器を作る構成: モーダル・ダイアログ・ポップアップ・別 Activity) を列挙し、各構成で候補がどのウィンドウに載るかを形態ごとに 1 行で書いた対応表を proposal か spec の補足に含める。対応表で不変条件に反する構成があれば、(a) 不変条件の定義を広げる、(b) その構成を既知の制約として Scenario に書く、のどちらかに倒してから spec を確定する。

## 経緯

- 2026-09-26 add-page-layout-area: 同一ウィンドウ条件はマルチウィンドウでの取り違え防止として後から足されたが、既存 Scenario (MAUI のモーダル) の載り先との照合が行われず、Native Android の実装が字面どおり「Activity のメインウィンドウだけ」と解釈した時点で矛盾が確定した

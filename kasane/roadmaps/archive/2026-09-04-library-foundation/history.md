# library-foundation 構造変更の経緯

## 2026-08-15: パッケージングの非ゴール→ゴール昇格とフェーズ2件の追加

**操作**: ゴール / 非ゴールの改訂 + フェーズ追加 (phase-10-packaging-model [research]、phase-11-packaging [change])。

**理由**: phase-4 vertical-slice (add-vertical-slice) の実測で、配布モデルが core の API 表面設計を規定することが判明した — (1) KMP framework は static で Swift 実体を同梱せず、アプリが KMP と Swift パッケージの両方をリンクする構造 (dynamic は未解決シンボルでリンク失敗を実測)。登録 API の置き場所 (verify-001 ❌2・❌3 で顕在化した Swift 向け KMP 面の欠落) はこのリンク構造と不可分 (2) 縦串で動いた Kotlin SwiftPM 連携はローカルパッケージ参照前提で、配布物での成立が未検証 (3) 3パッケージ + MAUI NuGet のバージョン整合は公開 API の互換ポリシーと直結。パッケージ観点なしで phase-5 (API 表面確定) を進めると手戻りリスクが高いため、オーナー判断で配布モデル設計 (phase-10) を phase-5 の前提に置き、成果物生成・発行検証 (phase-11) を機能フェーズ後・docs 前に追加した。配布の継続運用 (リリース CI・署名・レジストリ運用) は引き続き非ゴール。

**実行順の変更**: phase-4 → **phase-10** → phase-5 (API 表面設計の入力)。phase-6/7/8 → **phase-11** → phase-9 (docs はインストール手順を配布物基準で書くため)。

## 2026-08-17: phase-5-dialog-completion を分割 (論点13超の膨張)

**操作**: phase-5 を3サブフェーズに分割 — phase-5-1-layout-spec (レイアウト仕様と共通テスト、主たるサブフェーズ・in-progress)、phase-5-2-api-surface (API 表面の完成)、phase-5-3-presentation-behavior (提示挙動の完成)。

**理由**: ksn-agenda 着手時点で本論点7 + phase-4 申し送り6 (Sample UI 未決4件の内訳含む) が積まれ、分割トリガー (8論点) を超過。テーマが「レイアウト仕様」「API 表面」「提示挙動」に自然に分かれ、それぞれ単独で change として完了可能と判断した。元論点「Sample 通し」は各サブフェーズの完了条件に分散。依存は 5-1 → 5-3 (共通仕様テストの器が先行)、5-2 は並行可。phase-6 は3サブフェーズ完了を待つ。

## 2026-09-04: phase-11-packaging / phase-9-docs を package-distribution へ昇格

姉妹ライブラリ KsSettingsView が配信 (パッケージング・検証 CI・消費者検証・release workflow・利用者向け Skills・public 化) を初回リリースまで完了したため、KsDialogs 側もその実績を踏襲する独立ロードマップ [package-distribution](../../package-distribution/roadmap.md) を起案し、残り 2 フェーズを昇格した (ksn-explore 2026-09-04、経緯は同ロードマップの exploration.md)。本ロードマップの非ゴール「配布の継続運用」は新ロードマップのゴールになる。SwiftPM の配り方は cross/ADR-0008 のルート Package.swift から配信リポジトリ方式 (cross/ADR-0008 (2026-09-04 改訂)) へ改め、phase-10 の TODO「cross/0004 の Consequences へルート Package.swift 例外を追記する」は不要になった。素材の移送先: phase-11 の論点・合流・申し送り → package-distribution の phase-5 / 6 / 7 / 9、phase-9 の論点 → phase-2。両フェーズの history.md は見出しのみで移送対象なし。これで全フェーズが completed / promoted になり、同日アーカイブした。

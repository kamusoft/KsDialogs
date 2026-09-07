# アーキテクチャ調査

実装ゼロの状態でアーキテクチャの土台を決め、ADR として一気に起こす research フェーズ (KsAppKMP phase-1 の写像)。

## 論点


## 決定事項

- **KMP の位置づけ**: 各 Native (Swift / Kotlin) 独立実装を土台に、MAUI は binding、KMP は commonMain の呼び出し契約 + 薄い expect/actual ファサードで包む (View の中身は Native View / Compose を直接利用)。core 契約は仕様 (concepts + 共通仕様テスト) として共有 — core/ADR-0001 (proposed)、経緯は [history](history.md) 2026-08-13
- **リブランド方針**: Native 主・互換 shim なし・独立ブランド (仕様と実装パターンのみ継承) の3原則を明文化 — cross/ADR-0001 (proposed)、KsSettingsView cross/ADR-0017 の翻案。経緯は [history](history.md) 2026-08-13
- **技術セット**: 最新安定セットで開始 (KsAppKMP cross/ADR-0001 の判断型踏襲) — Kotlin 2.4.10 / Gradle 9.7.0 / AGP 9.3.0 / Xcode 26.6 (Swift 6.3) / net10.0 (LTS)。KMP→Swift は `@objc` 互換面 (Swift Export は Alpha につき見送り)。最低対象 OS は iOS 17 / Android minSdk 24 (新規判断) で、年1回相場を調査して見直す (一般公開予定のため消費者プロジェクトに非拘束)。swift-tools-version は phase-2 で実物確定 — cross/ADR-0002 (proposed)、経緯は [history](history.md) 2026-08-13
- **既存資産知識の取り込み方針**: ハイブリッド型 — 原則系 (意思決定を縛る方針) は使った時点で KsDialogs 自身の ADR / concepts へ即時翻案、機構系 (作り方の知識) は使うフェーズまで取り込まず reference-repositories.md 経由のオンデマンド参照とし、吸収時に新 ADR へ出典リンクを残す。一括移植はしない — cross/ADR-0003 (proposed)、KsAppKMP core/ADR-0006 の判断型踏襲。経緯は [history](history.md) 2026-08-13
- **公開 API 形状**: 両対応 — 契約を interface / protocol で定義し、既定 singleton エントリ (原典 Instance 踏襲) と DI 登録の両方から使える。命名は原典踏襲 (対応概念は原典命名、非対称箇所は対称性優先、契約に無い機能は互換提供せず再設計 = KsSettingsView maui/ADR-0008 の翻案)。singleton と DI インスタンスの同一性担保は論点「DI 差し込み方式」で具体化 — core/ADR-0002 (proposed)、経緯は [history](history.md) 2026-08-13
- **結果通知方式**: async 単発 + 型付き結果 — show は「completed(結果) / cancelled」を1回返す非同期操作とし、結果型は show の型パラメータで固定・通知役も同型に束縛 (原典の object キャストと cancel=default の粗を解消)。Swift は async + enum、Kotlin / KMP は suspend + sealed、MAUI は Task。コールバックは ObjC 互換面の最下層にのみ残し公開 API の一級市民にしない — core/ADR-0003 (proposed)、経緯は [history](history.md) 2026-08-13
- **DI 差し込み方式**: VM 型キー → View factory の明示レジストリを core 契約に (リフレクション不要で Swift 適合、KMP は KClass キー + 各 platform 側で factory 登録 = 共有 VM と Native View の紐付けの実体)。既定 singleton と DI 登録インスタンスは同じレジストリを共有 (論点4の残課題解消)。MAUI には原典互換糖衣 (SetIocConfig 相当、null 上書きの粗は修正)。コンテナ adapter は将来のオプション — core/ADR-0004 (proposed)、書き味は [artifacts/api-sketch-registry.md](artifacts/api-sketch-registry.md)、経緯は [history](history.md) 2026-08-13
- **View 再利用機構**: 契約から削除 — show は毎回 factory で View を生成する使い捨てモデルに一本化 (IReusableDialog / Create 系 / OnceInitializeAction は持ち込まない)。原典の動機「MAUI View 実体化の重さ」は Native 実装で構造的に消滅し、作者実体験でも利用実績なし。将来必要になれば非破壊の追加機能として Native 起点で再設計 — core/ADR-0005 (proposed)、経緯は [history](history.md) 2026-08-13
- **多段表示の core 契約表現**: OS の提示機構への委譲を踏襲し、契約は観察可能な意味論のみ規定 — 表示中でも show 可 (多段可) / 各 show は独立に結果を返す / 後発が手前に重なり閉じる順序は OS 提示機構に従う / ライブラリは段数の状態を持たない・公開しない。OS 差は縦串 + 共通仕様テストで記録して吸収 — core/ADR-0006 (proposed)、経緯は [history](history.md) 2026-08-13
- **レイアウト計算の共通仕様化**: 属性セット (原典踏襲命名) とサイズ・配置の決定規則 (Proportional / Fill / 未指定クランプの優先順位) を core 仕様 (concepts) として1本化し、実装は各 OS のレイアウト機構で自由 (手動 Measure の同型移植を要求しない)。一貫性は共通仕様テストで担保。属性の取捨は phase-4/5 の仕様化で確定 — core/ADR-0007 (proposed)、経緯は [history](history.md) 2026-08-13

## 素材

- [scout 調査: 技術セット裏取り + 参考 ADR 内容 (2026-08-13)](artifacts/scout-tech-stack-2026-08.md) — 論点「技術セット」「リブランド方針」「既存資産知識の取り込み方針」用
- [scout 調査: 移植元の公開 API 表面 + KsAppKMP ADR-0006 (2026-08-13)](artifacts/scout-origin-api-surface.md) — 論点「公開 API 形状」「結果通知」「DI 差し込み」「View 再利用」「レイアウト計算」「既存資産取り込み」用
- [API スケッチ: VM 型キー → View factory レジストリ (2026-08-13)](artifacts/api-sketch-registry.md) — core/ADR-0004 の合意時に提示した書き味 (命名は仮)

## 調査結果 (2026-08-13 完了)

全10論点を解消し、ADR 10件 (core 7 + cross 3) を proposed で起票した。決定一覧と後続フェーズへの申し送りは [artifacts/research-summary.md](artifacts/research-summary.md)、経緯は [history.md](history.md) を参照。後続フェーズ (2〜7, 9) の agenda への決定反映も実施済み。proposed ADR の accepted 昇格レビューは phase-3 で実施候補。

## TODO

- [x] 論点の解消 (全10論点、2026-08-13)
- [x] 調査結果のまとめ ([artifacts/research-summary.md](artifacts/research-summary.md))
- [x] ksn-roadmap で research 完了をマーク (2026-08-13)

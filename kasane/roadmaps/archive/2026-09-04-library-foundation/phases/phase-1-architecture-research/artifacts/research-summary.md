# phase-1-architecture-research 調査結果まとめ (2026-08-13)

実装ゼロの状態でアーキテクチャの土台を確定し、ADR 10件 (core 7 + cross 3) を proposed で起こした。全10論点を1日で解消し、決定は後続フェーズの agenda に反映済み。詳細な経緯は [history.md](../history.md)、個別の決定は各 ADR を参照。

## 決定一覧

| 論点 | 決定 | ADR |
|---|---|---|
| KMP の位置づけ | Native 2実装を土台に MAUI は binding、KMP は薄い expect/actual ファサード。core 契約は仕様として共有 | core/ADR-0001 |
| 公開 API 形状 | 契約 interface + 既定 singleton の両対応。命名は原典踏襲 (maui/ADR-0008 翻案) | core/ADR-0002 |
| 結果通知方式 | async 単発 + 型付き結果 (completed / cancelled を型で区別)。原典の object キャスト・cancel=default の粗を解消 | core/ADR-0003 |
| DI 差し込み方式 | VM 型キー → View factory の明示レジストリ。MAUI に原典互換糖衣。singleton / DI はレジストリ共有 | core/ADR-0004 |
| View 再利用機構 | 契約から削除。show は毎回生成の使い捨てモデル (動機消滅 + 実利用ゼロ + 非破壊追加可能) | core/ADR-0005 |
| 多段表示 | OS 提示機構への委譲を踏襲。契約は観察可能な意味論のみ | core/ADR-0006 |
| レイアウト計算 | 観察可能な規則を core 仕様に1本化。実装は各 OS のレイアウト機構で自由。共通仕様テストで担保 | core/ADR-0007 |
| リブランド方針 | Native 主・互換 shim なし・独立ブランド (KsSettingsView cross/ADR-0017 翻案) | cross/ADR-0001 |
| 技術セット | 2026-08 最新安定セット (Kotlin 2.4.10 / Gradle 9.7.0 / Xcode 26.6 / net10.0)。最低 OS は iOS 17 / minSdk 24、年1回相場調査で見直し | cross/ADR-0002 |
| 既存資産取り込み | ハイブリッド型 — 原則系は即時翻案、機構系はオンデマンド参照 (KsAppKMP core/ADR-0006 踏襲) | cross/ADR-0003 |

## 重要な前提 (議論中に確定)

- **KsDialogs は一般公開予定のライブラリ** — 設計判断を消費者プロジェクト (KsAppKMP 等) に拘束しない。整合は副次的利点
- KMP 形態の主役は「共有層 Presenter からの呼び出し」と「共有 VM と Native View の紐付け」。MAUI の View 実体化に相当する重い層は KMP に不要

## 後続フェーズへの申し送り

- **phase-2 (monorepo-scaffold)**: swift-tools-version を実物で確定する (cross/ADR-0002 の残課題)
- **phase-4 (vertical-slice) の最優先疎通確認**:
  - commonMain の VM クラスが ObjC クラスとして Swift から見え、レジストリのキー同一性が成立すること (core/ADR-0004 の前提)
  - Swift async → `@objc` completion handler → Kotlin suspend の変換経路 (core/ADR-0003 × cross/ADR-0002)
  - 共通仕様テストの器 (レイアウト規則・多段表示の意味論を3形態で検証する仕組み) の初版
- **phase-4/5 (仕様化作業)**: レイアウト属性セットの取捨確定 (core/ADR-0007)、結果通知の通知役 API の型設計 (core/ADR-0003)
- **Toast (phase-8)**: 原典で `[Obsolete]` を実測確認済み。新実装での復活はロードマップ前提どおり

## 素材

- [scout-tech-stack-2026-08.md](scout-tech-stack-2026-08.md) — 技術セット相場と参考 ADR 実物の要約
- [scout-origin-api-surface.md](scout-origin-api-surface.md) — 移植元の公開 API 表面の実測 (粗の記録含む)
- [api-sketch-registry.md](api-sketch-registry.md) — レジストリ API の書き味スケッチ (phase-4 の設計素材)

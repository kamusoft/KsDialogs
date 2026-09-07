# セカンドオピニオン: add-layout-spec (spec-001)
**相方**: codex / **日付**: 2026-08-17 / **対象**: 提案一式 (proposal / design / specs 6本 / tasks / ui-brief)
---
# レビュー結果: add-layout-spec

**判定**: NEEDS_DISCUSSION  
**件数**: Critical 0 / Major 7 / Minor 1 / Suggestion 0  
**実施範囲**: 静的レビューのみ。ビルド・テスト・ファイル書き込みは未実施。

## サマリー

ADR 0007〜0009 の方向性は反映されていますが、期待 rect の正となるケース表が未作成のまま実装タスクへ送られており、現状では実装者が仕様とテスト期待値を同時に決められます。また、公開 API、座標計算、Bind 完了、実環境検証、Sample UI に実装前に確定すべき未決事項があります。

## 指摘事項

### [🟠 Major] 共通ケース表が仕様ではなく実装成果物になっている

**該当箇所**: `kasane/changes/add-layout-spec/design.md:14`、`kasane/changes/add-layout-spec/design.md:24`、`kasane/changes/add-layout-spec/tasks.md:6`、`kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:56`

**問題点**:  
Native 実装の正となる `cases.json` がレビュー対象に存在せず、作成が実装タスク 1.2 に置かれています。「全ケースに適合」のケース集合と期待値を実装者自身が決められるため、受け入れ基準が循環しています。

さらに提示された schema には、次の入力・判定情報がありません。

- 内容サイズ
- visibleArea の原点・各辺 inset
- Border 検証用の内容領域
- dp/pt/px の座標単位
- Android の整数丸めと許容誤差
- 矩形の座標系

このままでは内容サイズ、visibleArea、Border の Scenario を `{screen, attributes, expected rect}` だけで表現できません。

**推奨修正**:  
実装開始前にケース表を仕様アーティファクトとして作成・レビューし、凍結してください。少なくとも `layoutAreaRect`、`contentSize`、属性、期待外接 rect、必要なら期待内容 rect、単位、丸め・許容誤差を定義し、Requirement/Scenario とケース ID の対応表を持たせてください。

---

### [🟠 Major] 任意の `osDiff` が ADR のプラットフォーム間一貫性を無効化する

**該当箇所**: `kasane/changes/add-layout-spec/design.md:24`、`kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:58`、`kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:60`

**問題点**:  
`osDiff` を任意ケースに追加できる設計では、実装差が出たときに期待値を OS 別へ分岐するだけでテストを通せます。これは「同じ属性入力から同じ規則を導く」という ADR 0007・0008 の狙いを弱めます。許容される OS 差の種類、承認条件、既知差の一覧もありません。

**推奨修正**:  
一般的な `osDiff` 逃げ道は削除してください。真に避けられない差がある場合は、実装前に差分ごとの理由・対象ケース・期待値を Scenario または明示的な許容差一覧として確定してください。safe-area inset など環境差は、期待値分岐ではなく入力 rect の差として表現するのが適切です。

---

### [🟠 Major] 公開 API の形・型・既定値が決まっておらず「破壊的変更なし」を検証できない

**該当箇所**: `kasane/changes/add-layout-spec/proposal.md:24`、`kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:7`、`kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:9`

**問題点**:  
次が未定義です。

- 明示サイズのプロパティ名
- 属性を VM、View、別の layout object のどこに持たせるか
- 各言語の数値型と単位
- DialogMargin の型と各辺の既定値
- Color の公開表現・interop 表現
- Offset、CornerRadius、BorderWidth の既定値
- NaN、Infinity、負値、比率範囲外の扱い
- MAUI/KMP の「無変換」が表現変換まで禁止するのか、意味保存を指すのか

既存の `DialogViewModel` は空の marker 契約です（`ios/Sources/KsDialogs/Contract/DialogViewModel.swift:7`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewModel.kt:12`、`maui/KsDialogs.Maui/Contract/DialogViewModel.cs:12`）。ここへ必須メンバーを追加するなら既存準拠型の source compatibility に影響します。

また、現行既定挙動には 24 の content margin と黒 40% の scrim があります（`ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:9`、同 `:30`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:118`、同 `:132`）。DialogMargin と OverlayColor の既定値が未定義なので、現状維持の主張も判定できません。

**推奨修正**:  
Swift/Kotlin/C#/KMP ごとの公開 API 写像表を追加し、所有場所、型、単位、既定値、無効値の処理、interop の値保存規則を確定してください。既存 VM が変更なしでコンパイルできる仕組みも Scenario または互換性要件として明記してください。

---

### [🟠 Major] サイズ優先順位だけでは最終 rect を一意に導けない

**該当箇所**: `kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:47`、同 `:51`、同 `:32`、同 `:42`

**問題点**:  
規則から次の結果が導けません。

- `ProportionalWidth + Fill` で幅が半分になった場合の x 位置
- 非対称 DialogMargin のとき Center がどの領域の中心になるか
- 比率・明示サイズも Margin 内へクランプするか
- Start/End が物理方向か、RTL 対応の論理方向か
- visibleArea が safe area、systemBars inset、display cutout、home indicator のどこまでを含むか
- Offset で領域外へ出た場合に許容、クランプ、クリップのどれになるか

実装者が自然だと思う規則を各 OS で選ぶと、ケース表作成前から差が発生します。

**推奨修正**:  
軸ごとに「基準 rect の確定 → Margin 適用 → サイズ選択 → サイズクランプ → anchor 決定 → Offset 適用 → 最終境界処理」の順序を規範化してください。Fill と比率の競合、非対称 Margin、RTL、visibleArea inset、領域外 Offset の Scenario を追加してください。

---

### [🟠 Major] 「Bind 完了」の観測点が現行アーキテクチャに存在しない

**該当箇所**: `kasane/changes/add-layout-spec/design.md:38`、`kasane/changes/add-layout-spec/specs/dialog-contract/spec.md:65`、`kasane/changes/add-layout-spec/tasks.md:11`

**問題点**:  
現在は同期 factory が VM を受け取って View を生成する構造であり、共通の Bind lifecycle や完了通知はありません（`ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift:21`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt:29`）。MAUI、UIKit、SwiftUI、Android View、Compose で「Bind 完了」の意味も異なります。

このままでは、factory 戻り時、BindingContext 設定時、最初の layout pass、非同期 VM 初期化完了のどれを待つか実装ごとに変わります。新しい lifecycle API を追加する場合は Non-Goal の API 拡張とも衝突し得ます。

**推奨修正**:  
Bind の開始主体・完了条件・失敗時の挙動を定義し、各 UI 技術への写像を設計に追加してください。公開 lifecycle を増やさないなら、「factory が返した内容へ初期状態を適用し、初回 native layout を完了させた後の intrinsic size」のように、既存境界だけで判定できる契約へ言い換えてください。

---

### [🟠 Major] Native 実レイアウトと実行時挙動を検証する経路が確定していない

**該当箇所**: `kasane/changes/add-layout-spec/design.md:54`、`kasane/changes/add-layout-spec/tasks.md:19`、`kasane/changes/add-layout-spec/tasks.md:24`、`kasane/changes/add-layout-spec/tasks.md:38`

**問題点**:  
Android の検証方式を実装フェーズへ先送りしています。この状態では純粋な rect 計算関数だけをケース表で検証し、実際の LayoutParams/constraints に反映されていなくても合格できます。

また、以下は通常の単体テストでは判定できません。

- MAUI iOS の描画中心 hit-test
- Android の透明 Overlay 表示中の system bar
- native layout 後の実 frame

現行 MAUI の `net10.0` gateway テストは platform view 化・hit-test へ到達しない設計です。Sample 手動確認にも中心座標タップと透明 Overlay の確認手順がありません。

**推奨修正**:  
実装前に検証面を確定してください。

- iOS: Simulator 上で layout 後の実 frame を取得
- Android: instrumented test または忠実度を実証した Robolectric で実 View の rect を取得
- MAUI hit-test: MAUI iOS 実環境で描画中心をタップし、結果遷移まで確認
- Overlay: 表示前後の system bar を同一条件で比較する手順と証跡を指定

計算器テストと実 UI 反映テストを別の受け入れ条件にしてください。

---

### [🟠 Major] Sample の要求と承認 UI が異なる操作モデルを示している

**該当箇所**: `kasane/changes/add-layout-spec/specs/samples/spec.md:5`、`kasane/changes/add-layout-spec/specs/samples/spec.md:7`、`kasane/changes/add-layout-spec/ui/brief.md:5`、`kasane/changes/add-layout-spec/ui/brief.md:7`

**問題点**:  
デルタスペックは「属性の効果を切り替えて確認できる」デモを要求しますが、brief と承認モックは固定の「End 配置 + DialogMargin」1例だけです。切替操作、状態、選択肢が定義されていません。

また、4ルートで一字一句一致させるための新規文言、完了結果値、キャンセル後の結果が spec に列挙されておらず、パリティを客観的に検証できません。

**推奨修正**:  
固定デモか切替式デモかを決定してください。固定なら「切り替えて」を削除し、切替式なら操作要素・選択肢・状態遷移を brief/mock/Scenario に追加してください。新規デモの表示文言と結果値も表として確定し、SampleText とパリティ規約更新を tasks に含めてください。

---

### [🟡 Minor] ケース表ローダーの対象形態が文書間で一致していない

**該当箇所**: `kasane/changes/add-layout-spec/proposal.md:13`、`kasane/changes/add-layout-spec/design.md:24`、`kasane/changes/add-layout-spec/design.md:25`、`kasane/changes/add-layout-spec/tasks.md:12`、`kasane/changes/add-layout-spec/tasks.md:28`

**問題点**:  
proposal/design は各形態にローダーを置く、またはローダー4本と説明していますが、tasks は iOS/Android にしかローダーを設けず、MAUI/KMP はパススルーテストだけです。

**推奨修正**:  
ADR 0009 の責務分担どおり、Native 2実装だけがケース表をロードし、MAUI/KMP は属性輸送の値保存を検証する、と全文書で統一してください。

## アクションプラン

1. 公開 API 写像表、既定値、単位、無効値規則を確定する。
2. 最終 rect を一意に導ける軸別アルゴリズムと境界 Scenario を追加する。
3. 共通ケース表と期待値を実装前に作成し、レビュー済み仕様として凍結する。
4. `osDiff` を除去するか、許容差を個別に仕様化する。
5. Bind 完了条件と実レイアウト／実機検証経路を確定する。
6. Sample の固定／切替モデルを決め、brief・spec・パリティ文言を一致させる。

## 突き合わせ結果 (2026-08-17 ホスト側判定)

ホスト側自己レビュー (2周・チェックリスト型) はいずれも検出していなかった。採否は根拠で判定:

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | ケース表が実装成果物 (受け入れ基準の循環) + schema 不足 | **採用** | 実害シナリオ明確 (実装者が仕様と期待値を同時に決められる)。ケース表を提案フェーズで作成・凍結し、schema に contentSize / 基準 rect / 単位 / 許容誤差を追加する |
| 2 | osDiff の一般的な逃げ道 | **採用 (修正形)** | ADR-0006/0007 の「OS 差の記録」意図は保持しつつ、osDiff エントリは理由 + オーナー承認を必須とし、環境差は入力 rect の差で表現する |
| 3 | 公開 API 写像表・既定値・無効値規則の欠落 (既定 margin 24 / scrim 黒40% の現状と「現状維持」主張の矛盾を含む) | **採用** | 該当コード行まで特定した強い根拠。design に写像表と既定値・無効値規則を追加 |
| 4 | 優先順位だけでは最終 rect が一意に導けない (Fill+比率の位置・非対称 Margin・RTL・visibleArea 範囲・領域外 Offset) | **採用** | 軸別アルゴリズム (基準 rect → Margin → サイズ → クランプ → anchor → Offset → 境界処理) を規範化し境界 Scenario を追加 |
| 5 | 「Bind 完了」の観測点が現行アーキテクチャに無い | **採用** | 契約を既存境界で判定できる形 (「factory が返した内容に初期状態を適用し初回レイアウトを完了した後のサイズで提示」) へ言い換える |
| 6 | 実レイアウト・実行時挙動の検証経路が未確定 | **採用** | 計算器テストと実 UI 反映テストを受け入れ条件として分離し、検証面 (iOS 実 frame / Android instrumented or 忠実度実証済み Robolectric / MAUI 実機 hit-test / Overlay 前後比較) を tasks に確定 |
| 7 | Sample の切替式要求と固定モックの不一致 + パリティ文言未列挙 | **採用 (解決済み)** | オーナー判断 (2026-08-17): 原典サンプル踏襲の属性調整パネル方式に確定。spec・brief・mock を改訂し文言表を brief に設置 |
| 8 | ローダー対象形態の文書間不一致 (Minor) | **採用** | ADR-0009 どおり「ローダーは Native 2形態のみ」に全文書を統一 |

降格: なし / 未解決: なし (#7 の A/B 選択のみオーナー判断待ち)

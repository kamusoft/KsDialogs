# Exploration: revisit-dialog-margin-default

## 課題 / 動機

ダイアログの余白 `dialogMargin` の既定値 (全辺 24) を 0 に見直したい (オーナー要望、2026-09-26)。

- 既定値が 0 なら、配置 (Start / End) と基準領域 (window / visibleArea / currentPage) の結果がそのまま基準の端に接して見え、利用者が挙動を理解しやすい
- 発見の文脈: add-page-layout-area の Sample 撮影証跡を見たオーナーが、カードが基準領域の端から 24 離れていて「ぱっと見検証しにくい」と指摘。同 change では Layout Dialog パネルのダイアログだけ dialogMargin を全辺 0 に指定した (`kasane/changes/archive/2026-09-27-add-page-layout-area/deviation.md` の Sample の余白の項)
- 決定の記録: core/ADR-0008 Decision 6「既定値の乖離3件」で、dialogMargin = 全辺 24 (原典 AiForms は 0) を「属性を何も指定しなければ今までどおりに出る」ためのオーナー判断として維持している。concepts `core/api/layout-semantics.md` の既定値表と「原典と既定値が違う3つ」節も同じ内容

### 現状 (2026-09-27 の調査)

| 対象 | 余白の既定値の持ち方 |
|---|---|
| 契約 (直書き) | iOS `ios/Sources/KsDialogs/Contract/DialogOptions.swift:37` / Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt:28` (KDoc :17) / MAUI `maui/KsDialogs.Maui/Contract/DialogOptions.cs:30` |
| NaN のフォールバック | iOS は `DialogOptions()` から引く (追従)。Android は契約とは別の private 定数 `DEFAULT_MARGIN` で二重定義 (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayout.kt:49`) |
| MAUI bridge | Android bridge が 24 を直書き (`maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogAttributes.kt:51-60`)。実効値になる経路は無く (C# の `ToBridgeOptions` が常に上書き)、`MauiDialogOptions().toDialogOptions() == DialogOptions()` のテストでだけ効く。iOS bridge は `DialogOptions()` から引く (追従) |
| KMP | commonMain に dialogMargin が無く、既定値の定義なし |
| Loading | `DialogOptions` を共有。既定ローディングは公開の一括設定 (`Loading` の `options`) で器メタ属性を渡せる |
| Toast | 中身に添付が無ければ `DialogOptions()` を使う (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastLayoutSnapshot.kt:26` / `ios/Sources/KsDialogs/Presentation/ToastContainerViewController.swift:235`)。既定 Toast (メッセージ入口のピル) には余白を渡す口が無く、ToastStyle にも項目が無い。現在の下端は「可視領域の下端 − 余白 24 − オフセット 80」 |
| 公開状況 | 0.1.0-beta.2 まで公開済み |

## 検討した選択肢 (却下案と理由を含む)

余白の既定値そのもの:

- **全辺 24 を維持 (core/ADR-0008 に従う)** — 却下。利用者は配置と基準領域の結果を「端から 24 引いて」読むことになり、Sample も検証用に全辺 0 を明示し続ける。維持の理由 (契約導入前の実装の見た目を保つ) は、beta 公開後は守る相手が変わっている
- **全辺 0 に改訂** — 採用 (オーナー要望)

Toast の扱い:

| 案 | 既定 Toast の縦位置 | 長いメッセージの左右 | 規則 | 判定 |
|---|---|---|---|---|
| A: Toast も一律 0 | 24 下がり、Material のボトムナビ (80dp) の上端にちょうど接する | 画面端まで広がり、逃げ道なし | 余白の既定が 1 つ | 却下 |
| B: Toast 全体 (既定・カスタム) の契約既定を 24 | 不変 | 不変 | 余白の契約既定が Dialog・Loading の 0 と Toast の 24 の 2 つ | 却下 (カスタム Toast は添付で余白を付けられるので分ける必要が無い) |
| C: Toast も 0、既定オフセットを 80→104 | 不変 | 画面端まで広がり、逃げ道なし | ADR-0032 の 80 の根拠を書き直す | 却下 |
| **採用: 既定 Toast のピルだけが余白 24 を持ち、カスタム Toast は契約どおり 0** | 不変 | 不変 | 契約既定は 0 の 1 つ。24 は同梱ピルの性質 | オーナー決定 |

## 決定事項

1. **余白の契約既定値を全辺 0 にする** (core/ADR-0008 Decision 6 のうち余白だけを改訂。overlayColor・layoutArea の既定の乖離は維持)。Dialog・Loading (既定ローディングを含む)・カスタム Toast に効く
2. **既定 Toast (メッセージ入口の同梱ピル) は、ライブラリがピル自身に余白 全辺 24 を持たせ、見た目を変えない**。契約既定値の例外ではなく、同梱コンテンツの性質として扱う (利用者のカスタム View が添付で余白を付けるのと同じ位置づけ)。既定ローディングは一括設定で余白を渡せるため 0 にそろえる
3. **非有限値 (NaN / ±Infinity) の規則は一般規則「その値の既定値」のまま**。結果として NaN の辺は 0 になり、負の辺と同じになる。layout-semantics の「非対称に見えるが…」の説明を書き直す。Android の NaN フォールバック用の二重定義 (`DEFAULT_MARGIN`) は iOS と同じく契約の既定値から引く形にそろえる
4. **公開済み beta との互換**: beta の中での見た目の変更として扱い、リリース pull request の `## Changes` に「余白の既定が 0 に。既定 Toast は変わらない」を書く。アプリ全体の余白の既定を戻す切り替え口は作らない (24 が欲しい利用者は中身ごとに添付する)
5. **Sample**: Layout Dialog パネルの全辺 0 の明示は 4 ルート (5 ファイル) とも外し、handbook `kasane/handbook/cross/sample-parity.md` の「パネルが出すダイアログの余白」行を既定値のままに書き換える (ksn-propose で改訂: パネルに `Margin` の行を足し、パネルの値を添付する形にした — オーナー提案 2026-09-27。形は ui/ のモック承認で決める)。Custom Toast メニューの 2 枚 (オフセット -80 / -160) は 24 ずつ下がるが間隔は変わらないので直さない。Toast Stack / Toast Placement は既定 Toast なので不変
6. **変更級は M** (オーナー確定 2026-09-27)

## ADR 候補 (作成済み: core/ADR-0039 / 未起票: なし)

- [core/ADR-0039](../../decisions/core/0039-dialog-margin-default-zero.md) (proposed) — 余白の契約既定値を原典と同じ 0 に戻し、既定 Toast のピルだけは従来の余白を自分で持つ (0008 を amends)。accepted への昇格と 0008 への `amended-by`・index の「一部改訂: 0039」の追記は ksn-distill が行う
- ADR-0039 のうちエージェントが推し量って書いた箇所 (オーナーの修正待ち): 却下案 B の却下理由「カスタム Toast は添付で余白を付けられるので契約既定値を分ける必要が無い」、前提「公開は beta の段階にある」

## 未決の論点

提案化 (ksn-propose) で詰める設計・確認事項:

- **既定 Toast に余白 24 を持たせる経路**: 同梱ピルの中身に器メタ属性を添付するのか、既定 Toast の経路だけ別の既定値を渡すのか。show の配置引数・ToastStyle のアプリ既定配置と組み合わせたときにも 24 が効くこと。MAUI・KMP の既定 Toast が Native の同梱ピルを通るか (薄いラッパーの原則 core/ADR-0001 どおりか) の確認
- **共通ケース表** `core/layout-spec/cases.json`: defaults の説明文 (:5)、C23 (Toast 既定配置 — 既定 Toast の扱いなら期待値は不変のはず。ケースが既定 Toast とカスタム Toast のどちらを表すかの確認が要る)、C24・C26 (currentPage の End/End。調査時の推算で x 96→120、y 506→530 / 566→590、未検算)、C19・C24 の note の「margin24」
- **テストの追随** (13〜15 ファイル): Android unit `DialogAttributeDefaultsTests`、Android instrumented `DialogCurrentPageTests` / `DialogPresentedWindowTests` / Compose `ComposeCurrentPageTests`、iOS 7 ファイル (Defaults / AttributeSupply / CurrentPageStage / LoadingAttribute / LoadingTypedShow / ToastAttribute / ToastTypedShow)、MAUI `DialogLayoutPassthroughTests`、PlacementHost の `PlacementMeasurement` 2 ファイル。Android の Toast / Loading instrumented テストが暗黙に 24 に依存していないかは未精査
- **ドキュメントの追随**: concepts `core/api/layout-semantics.md` (:48 / :60 / :64-66 / :226-241 の例)・`core/architecture/layout-case-table.md` (:45 / :65)・`core/api/toast-semantics.md` (既定 Toast の余白の記述を足すか) は蒸留で。skills (ios / android / maui / kmp の layout.md、aiforms-migration の SKILL.md と api-mapping.md の「変わった既定値」3→2 件、en / ja) は docs-refresh 経由
- 「原典と既定値が違う3つ」が 2 つになるため、concepts の節見出しと ADR-0008 を指す説明の書き直し

## UI 素材 (ui/references/ の一覧と注釈)

なし (見た目の変更は既定値の差し替えによるもので、新しい UI は作らない。Sample の Layout Dialog パネルは明示の削除だけ)

## 変更級の推奨: M (オーナー確定)

- 3 形態の公開 API の既定値の変更 (公開 API の小変更) と accepted ADR の改訂を伴うため S には収まらない
- 変わるのはレイアウトの既定値と既定 Toast の同梱ピルだけで、新しい設計判断は既定 Toast の余白の持たせ方に限られ、design.md を要しない
- config の `second-opinion.spec-review` により M 級も相方のスペックレビューの対象

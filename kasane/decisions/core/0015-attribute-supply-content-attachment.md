---
id: 0015
title: メタ属性は静動2分割の値オブジェクトとし、コンテンツ定義への添付 + show 引数 (placement のみ) で供給する
status: accepted
date: 2026-08-18
---

## Context

初版設計「メタ属性は ViewModel 契約のオプションメンバとして供給する」は実装フェーズでオーナーが差し戻した: VM に UI 要素が入り責務が曖昧になる・原典から乖離する・VM を使わない呼び出し (インライン factory show、core/ADR-0013) に運び手がない・KMP では言語制約と iOS interop 配管不在で実装不能 (二重ブロッカー)。core/ADR-0014 で属性は「器にしか実現できないメタ要素」10個に絞られ、静的メタ6 (layoutArea / dialogMargin / proportionalWidth / proportionalHeight / overlayColor / isCanceledOnTouchOutside) と動的メタ4 (horizontalAlignment / verticalAlignment / offsetX / offsetY) に分類された。またオーナー要求として「登録 (register) を重くしない」「Native / MAUI / KMP を無理に同型にしない (KMP の特別処理は許容)」がある。

## Decision

1. **運び手は静動2分割の不変値オブジェクト**とする: `DialogOptions` (静的メタ6) と `DialogPlacement` (動的メタ4)。全フィールド既定値付き。VM には一切属性を持たせない
2. **供給はコンテンツ (View) 定義への添付**を主経路とする。登録 API・show API に options 引数は作らない (レジスターは factory 1行のまま):
   - UIKit: `UIView` extension プロパティ (associated object) — View の init で添付
   - Android View: `View` extension プロパティ (setTag) — View の init で添付
   - SwiftUI: body ルートの view modifier (`.ksDialogOptions(...)` / `.ksDialogPlacement(...)`) — View 型の定義内に置く。同一属性の重畳は外側勝ち (SwiftUI modifier 慣習)
   - Compose: composable 関数冒頭の属性宣言 composable (`KsDialogAttributes(options, placement)`)。`ksdialogs-compose` モジュールに置き本体は Compose 非依存を維持
   - MAUI: 添付プロパティ (`ksd:Dialog.*`) — MAUI 層が束ねて Native の同型オブジェクトへ写す
3. **placement のみ show 引数で上書き可能**とする。優先順位は show 引数 > コンテンツ添付 > 契約既定値。options は View の性質であり show からは変更できない (誤用を型と供給点で構造的に防ぐ)
4. **採用時点のスナップショット契約**: 器が採用する実効値は、器を画面 (window) に載せたあとの**初回ネイティブレイアウトパス完了時点**で添付されている値のスナップショットとする。実装がいつ読み取るかは自由だが、採用値はこの時点の値で一意に決まる (提示前サイズ確定と同じ契約点で、内容サイズも同じパスの中で確定する)。この時点より後の添付変更は表示に反映しない
5. **KMP**: commonMain に公開するのは `DialogPlacement` のみ (具象 data class)。`DialogOptions` は KMP に供給経路が存在しないため公開しない — options は各 OS の View 定義側で完結し Kotlin/Native 境界を渡らない。境界を渡るのは show 引数の placement だけで、iOS interop 面に ObjC 表現可能な placement DTO の配管を1本追加する (MAUI の `KSDMauiDialogLayoutAttributes` と同型の実証済みパターン)

## Alternatives Considered

- **VM 契約のオプションメンバ (差し戻された初版設計)** — 却下。VM に UI 要素が混入し責務が曖昧、インライン show に運び手がなく、KMP では expect interface の既定実装不可 (言語制約) と iOS interop 配管不在で実装不能
- **単一 `DialogLayout` 型 (10属性1本)** — 却下。静的属性と動的属性が型の中で再び混ざり、show ごとに overlayColor を変えるような誤用が型上可能になる
- **登録 / show の引数で options を渡す** — 却下。レジスターが重くなる (オーナー要求違反)。インライン show の引数も肥大する
- **View 側属性のみで show 上書きなし** — 却下。View を持たない KMP 共有コードから配置を指定できず、属性調整パネル (承認済み Sample モック) のような呼び出し時可変ニーズも満たせない
- **`DialogOptions` も KMP の commonMain に公開する案** — 却下。KMP には options の供給経路が存在せず、供給経路のない公開型は API 形状の誤り検出を妨げる

## Consequences

- 正: VM が純粋なまま・登録と show が軽いまま・原典 ExtraView の「View が自分の性質を持つ」思想へ回帰しつつ、共有コードの動的配置も成立する
- 正: KMP の実装ブロッカー2件が構造的に解消し、特別処理が「interop への placement DTO 1本」に縮小する
- 負: 形態ごとに添付イディオムの実装が必要。特に SwiftUI (modifier 値の運搬) と Compose (CompositionLocal 経由の伝達) は「初回レイアウトパスまでに器へ届く」ことの実現可能性プローブを提案フェーズで行う (core/ADR-0014 と同じ教訓の適用)
- 負: 初版設計で実装済みだった VM 経路 (各形態の `DialogLayoutProviding` 系) は撤去・置換の手戻りが発生する

出典: kasane/changes/add-layout-spec/exploration-redesign.md (初版設計差し戻し後の再設計議論、2026-08-18) / kasane/changes/add-layout-spec/design.md (Decision 4・6)。core/ADR-0013 (インライン show)・core/ADR-0014 (属性の取捨) と接続

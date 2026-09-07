# Design: add-layout-spec

改訂履歴: 2026-08-18 全面改訂 — Decision 6 (属性は VM 契約) のオーナー差し戻しを受け core/ADR-0014・0015 を反映 (経緯は exploration-redesign.md)。2026-08-17 相方スペックレビュー採用指摘の反映 (初版)。

## Context

core/ADR-0007 (規則の仕様化)・0008 (意図的乖離)・0009 (共通ケース表)・**0014 (属性の取捨: 器にしか実現できないメタ属性のみ)**・**0015 (供給機構: コンテンツ添付 + show 引数)** を実装に落とす。レイアウト計算の実体は Native 2実装のみ (core/ADR-0001)、MAUI はマッピング、KMP は placement の interop 配管1本を特別処理として持つ。初版で実装済みの資産 (計算エンジン・検証基盤・透明オーバーレイ対応・hit-test 解明) は温存し、VM 経路 (`DialogLayoutProviding` 系) を撤去・置換する。

## Goals / Non-Goals

- Goals: メタ属性10個の全形態導入 (供給 = 添付 + show placement) / 規則とケース表の単一ソース化 / 提示前サイズ確定 / isCanceledOnTouchOutside の導入 / hit-test の構造的捕捉
- Non-Goals: proposal の Non-Goals に同じ (廃止5属性・SwiftUI/Compose DSL 実装・クランプ時の中身の見え方・immersive 引き継ぎ 等)

## Decisions

### Decision 1: 共通ケース表はリポジトリルート `core/layout-spec/cases.json` に置く

**採用案:** リポジトリルートの `core/layout-spec/cases.json` を単一の正とする。ローダーは Native 2形態のみ。MAUI / KMP はケース表を読まず、輸送の値保存のみ検証する (ADR-0009 の責務分担)。
**理由:** ケース表は「両 Native が参照する契約データ」であり、どのビルドルートにも属さない。
**代替案:**
- **A: kasane/concepts/ 配下** — 却下。ビルド・テストがハーネス層を参照する結合が生まれる
- **B: 各ビルドルートへコピー配置** — 却下。単一の正が崩れる

### Decision 2: ケース表は提案フェーズで作成・凍結し、schema に判定情報を全部持たせる

**採用案:** 改訂版ケース表 ([specs/dialog-contract/layout-cases.json](specs/dialog-contract/layout-cases.json)) を**本改訂で作成し直して再凍結**する。初版19ケースから廃止属性のケース (C03/C15 = 明示サイズ、C18 = border) を除去し、visibleArea の水平軸適用を固定する C20/C21 (非対称 left/right inset) と空有効領域を固定する C22 を追加した19ケース。ID は参照安定のため欠番方式で維持し、残存ケースの期待値は不変 (廃止属性を使用していないため)。schema は初版どおり (unit / tolerance / insets 入力 / approvedDiff 承認制)。`attributes` は**供給合成後の実効値** (添付と show 引数のマージ結果) として解釈する — ケース表は供給経路によらずレイアウト規則だけを検証する。
**理由:** 受け入れ基準を実装者が同時に決められる循環を断つ (初版から継承)。実効値解釈により、供給機構の変更がケース表に波及しない。
**代替案:**
- **A: 実装タスクでケース表を作る** — 却下 (初版から継承。期待値の循環)
- **B: 供給経路別にケースを分ける** — 却下。優先順位の検証は Scenario (少数) で足り、直積でケース数が爆発する

### Decision 3: 基準領域属性は enum `LayoutArea { window, visibleArea }`、既定は visibleArea

**採用案:** (初版から変更なし) visibleArea = システムバーを除いた可視領域。既定 visibleArea は現行の承認済み挙動の維持 (原典既定 window からの意図的乖離)。
**理由・代替案:** 初版のとおり (bool 改名維持案・既定 window 案は却下済み)。

### Decision 4: 提示前サイズ確定と添付読み取りは「初期状態適用 + 初回レイアウトパス完了後」を共通の契約点とする

**採用案:** 「factory が返した内容に VM の初期状態を適用し、初回のネイティブレイアウトパスを完了した後のサイズで提示する」(初版)。**添付属性の採用時点も同じ契約点に統合する** — 器が採用する実効値は**初回レイアウトパス完了時点で添付されている値のスナップショット**とし (読み取りの実装タイミングは自由だが、採用値はこの時点の値で一意)、以降の添付変更は表示に反映しない (ADR-0015)。
**理由:** 既存構造の境界だけで判定でき、新しい lifecycle API を追加しない (初版)。読み取り境界を同じ点に置くことで「提示時点で options/placement が確定している」を1つの観測点で保証できる。
**代替案:**
- **A: Bind 完了通知 API 新設** — 却下 (初版から継承)
- **B: 添付の継続監視 (提示後の動的追従)** — 却下。静的メタの定義 (ADR-0014) と矛盾し、監視機構のコストに見合う要求もない

### Decision 5: 軸別レイアウトアルゴリズムの規範化 (明示サイズ廃止で簡素化)

**採用案:** 最終 rect は軸ごとに次の順で一意に導く:

1. **基準 rect R**: `LayoutArea` に従い window または visibleArea (= window − insets)
2. **有効領域 A**: 軸ごとに `A.origin = R.min + 先頭側 Margin`、`A.length = max(0, R.length − 先頭側 Margin − 末尾側 Margin)` (非対称 Margin 可)。長さ 0 (空) でも origin は定義され、以降の手順は `A.min = origin` / `A.max = origin + length` で成立する (空のときは s = 0・位置は A.min に一意確定。中身の見え方は未規定域のまま)
3. **サイズ s** の選択: 比率指定 (`s = 比率 × R の軸長`) > Fill 配置 (`s = A の軸長`) > **内容サイズ** (View 自身のサイズ宣言は内容サイズとして扱われる — 明示サイズ属性は廃止、core/ADR-0014)
4. **クランプ**: `s = min(s, A の軸長)`
5. **anchor 位置 p**: Start = `A.min` / Center = `A.min + (A長 − s) / 2` / End = `A.max − s`。サイズ側で Fill が選ばれなかった軸の Fill 配置は Center として扱う
6. **Offset 適用**: `p += Offset` (+X 右 / +Y 下)。クランプしない (領域外を許容)
7. Start / End は物理方向 (RTL の論理方向対応は将来の拡張)

**理由:** 優先順位だけでは競合時の挙動が一意に導けない (初版から継承)。明示サイズの除去は ADR-0014 の帰結で、段数が1つ減り「View が自分のサイズを言い、器がメタ規則で配置する」という責務分離が規則文にも一致する。
**代替案:**
- **A: Offset を領域内へクランプ** — 却下 (初版から継承。意図的な画面外配置を塞がない)
- **B: 明示サイズを維持** — 却下。core/ADR-0014 (View で表現可能な属性は契約から外す) に反する

### Decision 6: 属性の運び手は `DialogOptions` / `DialogPlacement`、供給はコンテンツ添付 + show 引数 (placement のみ)

**採用案:** core/ADR-0015 の実装形。

**写像表** (すべて論理単位 pt / dp、数値は double 系):

| 型 | 属性 | フィールド型 | 既定値 | 備考 |
|---|---|---|---|---|
| `DialogOptions` (静的メタ) | layoutArea | enum LayoutArea (window/visibleArea) | **visibleArea** | Decision 3 |
| | dialogMargin | 各辺 Double の Thickness 相当 | **全辺 24** | 現行維持 (原典 0 からの意図的乖離) |
| | proportionalWidth / proportionalHeight | Double | -1 (未指定) | 0 < 値 ≤ 1 で比率指定が成立。**0 以下はすべて (既定の -1 を含む) 未指定**として扱い、1 を超える値は 1 へ丸める (2026-08-18 オーナー決定: 0 は未指定側) |
| | overlayColor | 各形態のネイティブ色型 | **黒 40% (0x66000000)** | 現行 scrim 維持 (原典 Transparent からの意図的乖離)。interop 境界は ARGB 32bit 整数 |
| | isCanceledOnTouchOutside | Bool | **true** | 原典既定 true の踏襲 (原典 DialogView.cs 実測)。規則は Decision 8 |
| `DialogPlacement` (動的メタ) | horizontalAlignment / verticalAlignment | enum DialogAlignment (start/center/end/fill) | center | |
| | offsetX / offsetY | Double | 0 | +X 右 / +Y 下。NaN は 0 |

**供給機構** (形態別):

| 形態 | 添付機構 | 本 change のスコープ |
|---|---|---|
| UIKit | `UIView` extension プロパティ (associated object)。View の init 等で `ksDialogOptions` / `ksDialogPlacement` を設定 | 実装する |
| Android View | `View` extension プロパティ (setTag + リソース id キー) | 実装する |
| MAUI | 添付プロパティ (`ksd:Dialog.*`)。MAUI 層が束ねて Native の同型オブジェクトへ写像 | 実装する |
| SwiftUI | body ルートの `.ksDialogOptions(...)` / `.ksDialogPlacement(...)` modifier | **expand-api-surface へ申し送り** (登録経路がそちらのスコープ)。プローブ済み: PreferenceKey 方式が表示中 window + `layoutIfNeeded()` 中に同期到達。同期発火は公式保証がないため、未着の場合は初回提示前に到達を待って再レイアウトし、スナップショット契約 (Decision 4) に収束させる (提示後の再適用は契約違反のため行わない — 到達を保証できない事態が判明したら契約の再検討事項)。値型は `Equatable & Sendable` |
| Compose | composable 冒頭の `KsDialogAttributes(options, placement)` | **同上申し送り**。プローブ済み: `SideEffect` 書き込み + `doOnPreDraw` 読み。attach または初回 measure で composition 同期実行の構造保証 (実機2台)。Lazy スコープ内は初回に実行されない — DSL 契約に明記 |

- **優先順位: show 引数 (placement のみ) > コンテンツ添付 > 契約既定値**。show API に options 引数は存在しない (誤用を供給点で構造的に防ぐ)
- **置換意味論**: show 引数の placement は添付 placement を**オブジェクト単位で置換**する (フィールド単位のマージはしない — 全フィールド既定値付きの値オブジェクトでは「省略」と「明示的に既定値を指定」を区別できないため)。添付 options には影響しない
- **数値の正規化**: 非有限値 (NaN / ±Infinity) は当該フィールドの既定値として扱う。dialogMargin の負の辺は 0 に丸める。proportional は 0 以下を未指定、1 超を 1 へ丸める (写像表の個別規則はこの一般規則の具体化)
- **KMP**: commonMain に公開するのは **`DialogPlacement` のみ** (具象 data class: `DialogAlignment` enum ×2 + Double ×2 — 色や Insets を含まないため型写像の難所がない)。`DialogOptions` は KMP に供給経路が存在しないため公開しない (各 OS の View 定義側で完結し Kotlin/Native 境界を渡らない)。iOS interop 面に placement DTO (enum 2 + double 2、ObjC 表現可能) の配管を1本追加する — MAUI の `KSDMauiDialogLayoutAttributes` で実証済みのパターン

**公開シグネチャ表** (名前は清書時の最終確定を許すが、形状はこの表が正):

| 形態 | 型 | フィールド表現 | 供給面 |
|---|---|---|---|
| UIKit (Swift) | `DialogOptions` / `DialogPlacement` struct | UIColor / DialogEdgeInsets / Double / enum | `UIView` extension var `ksDialogOptions` / `ksDialogPlacement` + `show(_:placement:)` |
| Android (Kotlin) | 同名 data class | `@ColorInt` Int / DialogEdgeInsets / Double / enum | `View` 拡張プロパティ (setTag 実装) + `show(vm, placement = ...)` |
| MAUI (C#) | 同名 class | `Maui.Color` / `Thickness` / double / enum | 添付プロパティは**スカラー10個** (`ksd:Dialog.OverlayColor` 等、原典スタイル。内部で2オブジェクトに束ねて Native へ写像) + `Show(vm, placement)` |
| KMP (commonMain) | `DialogPlacement` data class のみ | `DialogAlignment` enum / Double | `show(vm, placement = ...)`。iOS actual は interop DTO へ写像 |

- 各形態に**公開 API 形状のコンパイル検査** (既存互換 + 形状の負のコンパイル検証) をタスクへ含める
- VM 契約には一切属性を持たせない。初版実装の `DialogLayoutProviding` 系 (iOS protocol / Android interface / MAUI `IDialogLayoutProviding`) は撤去する

**理由:** core/ADR-0014・0015 の Alternatives に記載 (VM 契約案・単一型案・登録/show 引数案・View 側属性のみ案の却下理由)。
**代替案:** 同 ADR 参照 (再掲しない)。

### Decision 7: 検証面を計算器テストと実 UI 反映テストに分離して確定する

**採用案:** (初版から変更なし) ①ケース表テスト = iOS はシミュレータ実 frame / Android は instrumented 実 View。②実環境の個別確認 = MAUI hit-test (解消済み・証跡あり)・Android 透明オーバーレイ (撮影比較)。本改訂で③として**供給・優先順位の Scenario テスト** (添付のみ / show 上書き / 既定値) を各形態に追加する。
**理由・代替案:** 初版のとおり。

### Decision 8: isCanceledOnTouchOutside の規則

**採用案:** `isCanceledOnTouchOutside = true` (既定) のとき、ダイアログ外形 rect の外側 (オーバーレイ領域を含む) へのタップで、キャンセル操作と同一の経路 (`cancelled` 結果) でダイアログを閉じる。false のとき外側タップは何も起こさない (イベントはオーバーレイが吸収し背後へ透過しない — 現行のモーダル挙動維持)。ダイアログ内側のタップはこの機構に関与しない。overlayColor の値 (透明含む) と独立に機能する。
**理由:** 原典の観察可能な挙動 (既定 true、外側タップ = キャンセル) の踏襲。結果経路を cancel と共有することで、結果通知の型契約 (core/ADR-0003) に新しい結果種別を増やさない。
**代替案:**
- **A: 外側タップで dismissed など第3の結果種別を導入** — 却下。結果契約の拡張は本 change の範囲外で、原典も cancel と同経路
- **B: 既定 false (安全側)** — 却下。原典既定 true からの乖離に「現行挙動の維持」以外の理由がない (現行実装には外側タップ機構自体が無く、要件漏れの新規導入のため原典踏襲が基準)

## Risks / Trade-offs

- SwiftUI の preference 同期発火は公式保証のない実装挙動 — フォールバック方針 (Decision 6) を expand-api-surface の設計に引き継ぐ
- 添付の読み取り境界 (Decision 4) の器実装が形態間でずれると優先順位が実装差になる — 供給 Scenario とケース表 (実効値) で強制解消する
- Android instrumented test の CI コストは初版のとおりローカル実行 + 証跡記録で受け入れる

## Migration Plan

新規属性はすべて既定値付き追加のため利用者移行なし。既定値時挙動 = 現行実装と一致 (margin 24・scrim 黒40%・visibleArea 基準・外側タップキャンセルのみ新規追加で原典既定に一致)。実装済み VM 経路の撤去は「`DialogLayoutProviding` 系の削除 → 添付機構への置換 → テスト Support の属性宣言を添付へ書き換え」の順で、ケース表検証がリグレッションゲートになる。

## Open Questions

なし (クランプ時に rect が内容サイズより小さい場合の中身の見え方のみ、proposal の Non-Goals どおり意図的未規定として維持。proportional = 0 は「0 以下は未指定」で確定済み — Decision 6)

## ADR 候補

- Decision 1 + 2 (ケース表の配置と再凍結・approvedDiff): core/ADR-0009 の増分として蒸留時に反映
- Decision 3 (LayoutArea): 起票済み核 (core/ADR-0008 Decision 4 系) — 初版から変更なし
- Decision 5 (優先順位の簡素化) + Decision 6 (運び手と供給): **core/ADR-0014・0015 起票済み (proposed)** — 蒸留時に accepted 昇格を諮る
- Decision 8 (isCanceledOnTouchOutside 規則・原典踏襲既定 true): core/ADR-0008 の増分として蒸留へ

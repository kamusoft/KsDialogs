# 蒸留への申し送り: expand-api-surface

tasks 7.3 の記録。実装・検証を終えた時点で、長命層 (decisions/ concepts/) に反映すべきこと・
棚卸しが要ることを列挙する。反映の実施は ksn-distill の責務であり、本ファイルでは何も書き換えていない。

作成日: 2026-08-19

---

## 1. cross/ADR-0008 への `ksdialogs-compose` の追記

**根拠**: design.md Decision 7 の「影響」欄 (`配布物が1つ増えるため、cross/ADR-0008 (標準3チャネル) への追記を蒸留時の申し送りとする`)。

**内容**: Android の Compose 系 API を別モジュール `ksdialogs-compose` (Maven 座標 `jp.kamusoft:ksdialogs-compose`、`jp.kamusoft:ksdialogs` に依存) として新設した。
配布物が1つ増えるため、[cross/ADR-0008 (標準チャネルによる配布)](../../decisions/cross/0008-distribution-model-standard-channels.md) の配布物一覧へ追記が要る。
バージョンは lockstep ([cross/ADR-0009](../../decisions/cross/0009-lockstep-single-version.md)) に含める。

**併せて確認すること**: 本体 `ksdialogs` が Compose に依存しないことは Gradle の検査タスク
`:ksdialogs:verifyNoDeclarativeUiDependency` で固定してある (下の 4 も参照)。

## 2. cross/sample-parity 規約への反映 (2件)

対象: [`concepts/cross/conventions/sample-parity.md`](../../concepts/cross/conventions/sample-parity.md)

### 2-1. 文言表の反映

**根拠**: specs/samples の Requirement「API 表面の新デモ項目」(`文言 … は ui/brief.md の文言表を正とし、実装完了後の蒸留で cross の sample-parity 規約へ反映すること`)。

**内容**: `ui/brief.md` の「文言表 (パリティの正 — 4ルート一字一句一致)」を規約側へ移す。実装で確定した文言は次のとおり (Sample 通しで実物と一致を確認済み — `verification/sample-walkthrough/`)。

| 場所 | 文言 |
|---|---|
| メニュー項目3〜5 | `Declarative Dialog` / `Text Input Dialog` / `Inline Dialog` |
| Declarative Dialog 本文 | `こんにちは、KsDialogs!` |
| Text Input Dialog 本文 | `メッセージを入力してください` / 入力欄プレースホルダ `ここに入力` / 初期入力値なし (空) |
| Inline Dialog 本文 | `インライン表示です` |
| ボタン (全デモ共通) | `キャンセル` / `OK` |
| 結果表示 | 真偽値 `結果: completed(true)` / 文字列 `結果: completed("<入力値>")` / キャンセル `結果: cancelled` |

なお `ui/brief.md` の末尾はこの反映を「tasks 7.4」と書いているが、tasks.md の採番は **7.3** である (足場は凍結のため未修正。蒸留時に混乱しないよう注記)。

### 2-2. パネル操作部の読み上げ規約の反映

**根拠**: specs/samples の Requirement「属性調整パネル操作部の読み上げ対応」(phase-5-2 agenda 決定 2026-08-19)。

**内容**: 既存の「戻る記号の読み上げ」規約と同型で、属性調整パネルの操作部にも読み上げ用の名前・役割・状態を4ルート一致で与えることを規約化する。確定した規則:

- 読み上げ名は画面の文言と同一を基本とする
- 同名の選択肢 (Horizontal / Vertical それぞれの Start / Center / End) だけは「所属行の文言 + 選択肢の文言」の複合名 (例 `Horizontal Start`) とする
- 状態 (選択肢の選択状態・トグルの ON/OFF・移動量欄の現在値) は OS 標準の役割・状態機構に載せる
- 検証は accessibility tree の検査 (自動検査または実機スクリーンリーダー) で行い、4ルートの証跡を残す

証跡と、ルート別の取得方法・実測された役割/状態の対応表は `verification/panel-accessibility/README.md` にある。

## 3. sample-parity.md の古い記述の削除・更新

対象: [`concepts/cross/conventions/sample-parity.md`](../../concepts/cross/conventions/sample-parity.md) の「利用者と同じ側から使う」節 (105行目付近)

**現状の記述**:

> 暫定の例外: `samples/kmp` の iosApp は、KMP の共有コードで定義した ViewModel と Swift 側の View を結び付けるための公開された登録の窓口がまだ設計されていないため、ObjC 互換の内部インターフェースを直接呼んでいる ([cross/ADR-0006](...) の帰結)

**申し送り**: この暫定例外は本変更の tasks 6.2 (specs/kmp-facade の Requirement「KMP iOS Sample の公開 API 化」) で解消済み。KMP iOS Sample の登録は Swift 向け型付き公開 API のみを使い、`KsDialogsInteropBridge` への直接参照はない。よって**この箇条書きは削除**する (または「解消済み」への書き換え)。

## 4. cross/conventions/test-execution.md の棚卸し — **解決済み (蒸留での作業は不要)**

対象: [`concepts/cross/conventions/test-execution.md`](../../concepts/cross/conventions/test-execution.md)

review-001 の指摘 (A)(B) を受け、**本変更の修正サイクルで規約本体へ反映済み**である。
蒸留側で改めて行う作業はない。反映内容:

| ビルドルート | 反映前の記載 | 反映後 (2026-08-19 実測。すべて 0 failures) |
|---|---|---|
| ios/ | 59 tests / 14 suites | **90 tests / 19 suites** (Swift Testing。XCTest 側は 0) |
| android/ | 40 tests | **50 tests** |
| android/ (instrumented) | 62 tests | **94 tests** (`:ksdialogs` 62 + `:ksdialogs-compose` 32) |
| kmp/ | 48 tests (iosSimulatorArm64 26 + androidHostTest 22) | 48 tests (変化なし・未再実測) |
| maui/ | 44 tests | 44 tests (実測更新済み・変化なし) |
| maui/android/native/ | 9 tests | 9 tests (変化なし・未再実測) |
| 負のコンパイル検査 | 17 本 | 17 本 (android の 4 本を再実行し、規約の期待診断と一致することを確認) |

併せて規約へ書き足したこと:

- 「本体の Compose 非依存の検証」節を新設し、`:ksdialogs:verifyNoDeclarativeUiDependency` の実行方法・
  検査対象の classpath・**成功が期待結果**であることを明記した。同時にビルド定義側で `check` に加えて
  `test` にも結線したため、規約表の `./gradlew test --rerun-tasks` にこの検査が乗るようになっている
  (`--dry-run` のタスクグラフと、依存を一時的に足して失敗することの両方で実証)
- instrumented の件数確認先が `:ksdialogs` と `:ksdialogs-compose` の2モジュールに分かれた点

**残る棚卸し (未対応。concepts の索引・履歴の整備は蒸留の本務のため意図的に回す)**:

1. [`concepts/cross/index.md`](../../concepts/cross/index.md) の test-execution.md の1行説明が
   `公開 API 形状の検査 (正/負16本)` のままで、負の検査の本数 **17** と食い違っている。
   あわせて新設した「本体の Compose 非依存の検証」節が index の1行説明から見えないため、
   索引の粒度をどうするか (節を列挙するか、説明を「形状・依存の検査」へ広げるか) を判断されたい
2. [`concepts/log.md`](../../concepts/log.md) に **test-execution.md 更新のエントリが無い**。
   同じ本変更でもタスク 1.1 分 (`registration-show-semantics.md` 新設ほか) は記載済みで、
   修正サイクルで入れた test-execution.md の更新分だけが漏れている (review-002 Minor)

## 5. phase-5-3 以降への申し送り: Kotlin `IosDialogGateway` のキャンセル追随

**内容**: Kotlin 側の `IosDialogGateway` (KMP 共有コードから suspend で show する経路) は
「呼び出し元キャンセルでも表示が残る」既存挙動のままである。本変更のデルタスペックは
KMP の **Swift 公開面**に限定されている (specs/kmp-facade の Requirement「呼び出し元キャンセルでの閉鎖 (KMP Swift 面)」) ため、
Kotlin 経路はスコープ外として扱った。

ただし core の結果通知契約 (`concepts/core/api/result-notification-semantics.md`) は
呼び出し元キャンセルでの閉鎖と cancelled 確定を要求しており、Kotlin 経路はこの契約に未適合のまま残る。
**追随の候補**: 本変更で機械面に追加した show 単位のキャンセルハンドル (design Decision 9) を
`invokeOnCancellation` で引いて閉じる。phase-5-3 以降の変更として起票を検討されたい。

Swift 面の確認結果と、Sample での手動確認ができない理由は `verification/kmp-task-cancellation/README.md` にある。

## 6. verify-001 由来の申し送り (2件)

`verify-001.md` の「所見」から、長命層・コードの整合に関わるものを引き取る。判定 (VALID) には算入されていない。

### 6-1. design 宣言表の `KmpDialogNotifier<R>` は実装に存在しない

design.md Decision 6 の KMP Swift 宣言表は notifier の型を `KmpDialogNotifier<R>` と書いているが、
実装は iOS Native と同じ `DialogNotifier<R>` に一本化されている (型消去の往復が不要になったため)。

当該ブロックは「Swift パッケージの新公開型 (**名前は仮**)」と明記されているため verify は乖離として扱わなかったが、
Decision 6 の前文は「名前の微修正は deviation として記録」とも書いており、両者は緊張関係にある。
**蒸留で ADR / concepts へ写す際は実装形 (`DialogNotifier<R>`) を正とすること**。
同種の差 (`Dialog.shared.kmp` / `KsDialogsKmp` という配置) も同じ根拠で乖離なしと判定されている (review-001)。

### 6-2. `IosDialogGateway` の doc コメントが機械面の現状と食い違う

`kmp/.../IosDialogGateway.kt:41-42` の doc コメントが「取り消しの操作を持たない」と書いたままだが、
本変更で機械面 (`KsDialogsInteropBridge.show`) は show 単位のキャンセルハンドルを返すようになった (design Decision 9)。
Kotlin 経路がそのハンドルをまだ引いていないこと自体は上の 5 で申し送り済みだが、
**コメントの記述は現状と異なる**ため、5 の追随を行うかどうかに関わらず訂正が要る。

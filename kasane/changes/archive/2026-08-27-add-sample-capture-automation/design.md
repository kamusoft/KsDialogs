# Design: add-sample-capture-automation

## Context

方式レベルの決定 (デモ駆動モード = 起動引数 + 安定デモ ID + 全構成有効 / 撮影スクリプト = 単一入口 + 宣言的操作列) は ADR cross/0010 で確定済み。**2026-08-27 改訂: 後半の撮影スクリプトは取り下げ (経緯は proposal 改訂記録・ADR の改訂申し送りは deviation.md 参照)。**本 design は、その方式を6アプリに実装するために必要な下位の設計判断 — 引数の外部表現・起動ライフサイクル・デモ別ディスパッチ・成果物契約・検証割り付け — を固定する (spec-review のセカンドオピニオン second-opinion-spec-001 で採用した指摘への応答)。

## Goals / Non-Goals

**Goals:** 6アプリが同じ解釈をする引数契約 / 二重自動再生のないライフサイクル / 現行コードの実態に合ったディスパッチ責務 / 部分成功を成功扱いしない撮影契約 (2026-08-27 改訂で取り下げ) / CA Scenario の検証割り付け (同改訂で CA-SA 系のみに縮小)

**Non-Goals:** scenario-id-coverage の Python 対応拡張 (lint 基盤の変更は別 change)、ビルド・デプロイの自動化 (proposal の Non-Goals どおり)。2026-08-27 改訂で撮影の入口ツール化そのものも Non-Goals へ移動 (proposal 改訂記録参照)

## Decisions

### Decision 1: 起動引数の外部表現は「iOS = `--キー 値` トークンペア / Android = 同名キーの string extra」

**採用案:**

- iOS 系 (ios / maui-ios / kmp-ios): launch arguments のトークン列を `--demo <id> --loading-step-interval-ms <n>` の隣接ペアとして自前で走査する (`ProcessInfo.processInfo.arguments` / MAUI は `NSProcessInfo`)

  ```
  xcrun simctl launch <UDID> <bundle-id> --demo basic-dialog --loading-step-interval-ms 2000
  ```

- Android 系 (android / maui-android / kmp-android): 同名キーの **string extra** (`getStringExtra`)。数値も string で渡し、解析は4ルート同型の「文字列 → 検証」に揃える

  ```
  adb shell am start -n <package>/<activity> --es demo basic-dialog --es loading-step-interval-ms 2000
  ```

- 異常系は両 OS 共通: キーの直後に値がない・空文字・検証に通らない値 → **当該キーを無視して既定動作**。同じキーが複数回現れたら**最初の1組を採用**

**理由:** simctl / adb の素の起動コマンドにそのまま同居し、キー・値の意味論が両 OS で同型になる。string 統一により数値の失敗系 (非数値・範囲外) も4ルート同じ検証コードの形になる。

**代替案:**
- **A: iOS を `-demo value` (NSUserDefaults 引数ドメイン連携) にする** — 読み出し機構がルートごとに変わり (MAUI / KMP からは素の走査の方が同型)、UserDefaults 経由の暗黙挙動に依存する。却下
- **B: `demo=<id>` 形式の独自構文** — 両 OS の既存慣行 (`--flag value` / `--es key value`) のどちらとも違う第3の構文を発明することになる。却下
- **C: Android を int extra (`--ei`) にする** — 数値解析の失敗系が OS 間で非対称になる (iOS は文字列検証、Android は adb 側で丸め)。却下

### Decision 2: スクリプトは毎回コールド起動し、Sample は自動再生を「プロセス起動につき1回」だけ消費する (スクリプト部分は 2026-08-27 改訂で取り下げ — 「撮影は毎回コールド起動」はエージェント手順の前提として存続)

**採用案:** 入口スクリプトは起動前に対象アプリを必ず終了してから起動する (`xcrun simctl terminate` / `adb shell am force-stop`。未起動でもエラーにしない)。Sample 側は引数の解析結果をプロセス単位に保持し、自動再生はメニュー画面の初回表示時に1度だけ発火する。画面・Activity の再生成 (回転等) では再発火しない (Android は再生成検知に savedInstanceState を使い、iOS / MAUI / KMP はプロセス内の消費フラグ)。起動中アプリへの温かい再配達 (SingleTop の onNewIntent、scene 再 activation) は正式対応しない。

**理由:** MAUI Android は SingleTop (`Platforms/Android/MainActivity.cs`) で、温かい起動では extra の到達経路が変わる。「毎回コールド起動」を撮影側の契約にすることで6アプリのライフサイクル差を平準化し、one-shot 消費で再生成時の二重自動再生を仕組みで塞ぐ。

**代替案:**
- **A: onNewIntent / 再 activation の正式対応** — 6アプリにライフサイクル分岐が増える。撮影用途はコールド起動で足り、利得がない。却下
- **B: one-shot なし (設定が生きている限り毎回自動再生)** — 画面再生成でダイアログが二重に出る。却下

### Decision 3: 自動再生のディスパッチは「メニュー項目のタップハンドラと同じ入口」を UI 層から呼ぶ

9デモの起動直後の期待状態と担当層:

| 安定デモ ID | 起動直後の期待状態 | 担当層 |
|---|---|---|
| `basic-dialog` / `declarative-dialog` / `model-dialog` / `text-input-dialog` | ダイアログが表示される | ios: SampleMenuScreen→Model / android: MainActivity / maui: SampleMenuPage 内部メソッド / kmp: 共有 Presenter |
| `inline-dialog` | ダイアログが表示される | 同上。ただし kmp は OS 側 (Inline は共有 Presenter 外) |
| `layout-dialog` / `transition-dialog` | **属性パネル画面が開く** (メニュータップと同一) | 全ルート OS UI 層 (パネルは画面状態であり Presenter 外) |
| `default-loading` / `custom-loading` | ローディングが開始される | ios: Model / android: MainActivity / maui: Page / kmp: 共有 Presenter |

**採用案:** 自動再生はメニュー画面の初回表示タイミングで、該当メニュー項目の**タップハンドラと同じコードパス**を呼ぶ。必要ならハンドラを内部メソッドに切り出して共用する (MAUI のイベントハンドラ等)。

**理由:** 「メニュー項目タップと同じ」を文字通り同じコードで実現し、手動起動と自動再生の初期状態乖離を構造的に防ぐ。Layout / Transition がパネルを開く現行実態 (kmp iosApp SampleMenuScreen の画面状態変更 / kmp androidApp MainActivity の openLayoutPanel・openTransitionPanel) にもそのまま合う。

**代替案:**
- **A: KMP は共有 Presenter に自動再生を一元化** — Layout / Transition のパネルは Presenter 外の画面状態であり、Presenter だけでは「メニュータップと同じ」初期状態を作れない (現物確認済み)。却下
- **B: 自動再生専用のディスパッチ表を別に作る** — タップハンドラと二重管理になり、片方だけ直す事故の型。却下

### Decision 4: 撮影成果物は「操作列データが宣言する状態マニフェスト」を単位に契約する — 取り下げ (2026-08-27 改訂)

**改訂注記:** capture-tooling (入口スクリプト) の取り下げに伴い本 Decision は失効。iOS シミュレータへの座標タップをスクリプトから注入する CLI 手段が環境に無く (simctl 単体に入力注入なし・「シミュレータ操作ツール」はエージェント専用 MCP)、外部ツール導入・XCUITest ランナー同梱はオーナー判断で不採用。撮影はエージェント手順で行う。以下は経緯保存のため残す。

**採用案:** 操作列データはデモごとに「撮影する状態の列 (例: `shown` → `result`。パネル系は `panel` → `shown` → `result`)」「状態ごとの表示待ち時間」「状態間のタップ列 (座標)」を宣言する。スクリーンショットは `<app>-<demo>-<NN>-<state>.png` (NN は状態の順序) で、**引数で指定された保存先** (必須引数) に保存する。iOS の simctl 制約 (プロジェクト配下へ直接書けない) は一時領域経由で吸収する。1状態でも起動・操作・撮影に失敗したら**非0で終了**し、部分成果物は残すが標準エラーで失敗を明示する (成功扱いしない)。既存ファイルは上書きする (再撮影が主用途)。

**理由:** 「スクリーンショット一式」の定義をデータ側に持たせ、成功判定 (マニフェストの全状態が撮れたか) を機械化する。

**代替案:**
- **A: 全デモ固定2枚 (表示+結果)** — パネル系の3状態・Loading の途中経過に合わない。却下
- **B: 保存先を証跡ディレクトリに固定** — 撮影は検証証跡以外 (README 素材等) にも使うため、置き場の判断は呼び出し元 (ワーカー/オーナー) に残す。却下

### Decision 5: CA Scenario の検証は「Python 単体テスト + 理由付き allow-missing」に割り付ける — 2026-08-27 改訂で CA-SA 系のみに縮小

**改訂注記:** CA-CT 系 Scenario は capture-tooling の取り下げとともに削除。残る割り付けは「CA-SA 系 (Sample 挙動) を4ルート通しの verification 証跡で検証し、理由付きで allow-missing に登録 (LD-SA 系と同じ扱い)」のみ。以下は経緯保存のため残す。

**採用案:** scenario-id-coverage は Python を走査しない (TEST_EXT に .py がなく、scripts/ はテスト置き場の対象外) ため:

- CA-SA 系 (Sample 挙動): 4ルート通し (入口スクリプト使用) の verification 証跡で検証し、allow-missing に登録 (理由: Sample 専用・手動通し。add-loading の LD-SA 系と同じ扱い)
- CA-CT 系のうち selftest まわり (CA-CT-03 / CA-CT-06): `scripts/capture/tests/` の Python 単体テスト (テスト名に Scenario ID を埋める) で自動化し、実行証跡を残す。coverage lint 上は allow-missing に登録するが、検証の実体はテストが担う
- CA-CT 系のうちデバイス依存・文書検査 (CA-CT-01 / 02 / 04 / 05): 実地検証 + 証跡で確認し、allow-missing に登録 (理由明記)

**理由:** lint 基盤 (scenario-id-coverage) の Python 対応は別能力の変更であり本 change に持ち込まない。一方で「単純な除外」は避け、自動化可能なものは Python テストとして実体を固定する。

**代替案:**
- **A: scenario-id-coverage を .py 対応に拡張** — lint 基盤の変更は影響が全 change に及ぶ。別 change の規模。却下
- **B: CA-CT を全部 allow-missing だけで済ませる** — selftest の負経路 (欠落検出) は自動テスト可能であり、除外だけでは「不整合なら非0」を固定できない。却下

## Risks / Trade-offs

- 「タップハンドラと同じ入口」方式は、メニュー画面の初回表示タイミングがルートごとに違う (SwiftUI の task / Activity の onCreate 後 / MAUI の Appearing) ため、発火タイミングの実装差は残る。契約は「起動直後に期待状態になる」の観察で吸収する
- string extra 統一により、Android で `--ei` を使った誤用は無視される (拾わない)。docstring のコマンド例で正しい形を示す

## Migration Plan

追加のみで既存挙動の変更なし (引数なし起動の不変は CA-SA-01 で契約)。ロールバックはデモ駆動モードの受け口の削除で完結する (scripts/capture/ は 2026-08-27 改訂により未作成のまま取り下げ)。

## Open Questions

なし

## ADR 候補

なし — 方式レベルは ADR cross/0010 で起票済み。本 design の Decision 1〜5 はその詳細化であり、単独では選別3基準 (覆すコスト高 / 境界を越える / 将来を制約) に該当しない。

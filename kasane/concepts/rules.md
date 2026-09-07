---
type: policy
title: concepts 配置ルール
description: ドメイン導出規則と、この concepts/ のカテゴリ定義・配置基準・handbook との振り分け
timestamp: 2026-09-05
---

# concepts 配置ルール

この文書を読むと、KsDialogs の知識 (concepts / decisions) をどのドメイン・カテゴリに置くべきかが分かる。

## ドメイン導出規則

ドメイン一覧の正は `kasane/config.yaml` の `domains` (core / ios / android / maui / kmp。`cross` は予約ドメインとして常に存在する)。判定の主軸はリポジトリの4ビルドルート ios/ / android/ / kmp/ / maui/ (cross/ADR-0004) であり、知識・決定の行き先は次の順で判定する:

1. 単一ビルドルートに閉じるコードの知識・決定 → そのビルドルートのドメイン
   - kmp/ 内の androidMain actual (Android Native への委譲層) も `kmp`
   - maui/ 内の platform handler (iOS / Android 実装) も `maui`
2. ビルドルートを持たない全形態共有の契約・意味論 (ダイアログモデル・表示制御の意味論・styling 規則・レイアウト規則・共通 architecture) → `core`
3. ビルドルート横断の実物 (バージョンカタログのファイル共有・scripts/・CI) と、リポジトリ構成・命名規約・ハーネス運用などのメタ事項 → `cross`
4. 新しいビルドルート・パッケージ系統が増えた場合: 既存ビルドルートに属するなら該当ドメインへ。属さないならユーザー合意の上で `config.yaml` の `domains` に追加する
5. 変更 (proposal) が複数ドメインに触る場合の `domain:` 欄は `cross`。蒸留時の ADR / concepts の行き先は内容ごとに本規則で判定する

## カテゴリ定義

### core/

| カテゴリ | 対象 | 主な type |
|---|---|---|
| architecture/ | レイヤ構造・責務境界・プラットフォーム間の共通原則 | concept |
| api/ | 全 platform が共有する観察可能な挙動と保証 (ダイアログ契約)。公開名・署名・コード例は持たない | concept, reference, glossary |

### ios/ / android/ / maui/ / kmp/

| カテゴリ | 対象 | 主な type |
|---|---|---|
| api/ | その platform の公開面 (公開名・署名・コード例・framework 固有の注意)・binding 境界 | concept, reference |

### cross/

| カテゴリ | 対象 | 主な type |
|---|---|---|
| architecture/ | リポジトリ・ビルド構成の責務境界 | concept |
| reference/ | 外部資産の在り処・対応表 (実体は外部にあり、concepts はポインタだけを持つ) | reference |

## handbook との振り分け

カテゴリを選ぶ前に、その文書が**記述**か**規範**かを決める。判定は「この文書とコードが食い違ったとき、直すのはどちらか」— コードを直すなら規範で、置き場は concepts ではなく `kasane/handbook/<domain>/` (命名規則・コメント規約・テスト実行手順・Sample パリティなど)。文書を直す余地があるなら記述で、concepts に置く。

例外の目印: 移植元・外部システムの仕様要約のように**外部の実物と照合して真偽が決まる**文書は記述のため concepts に置く。その外部資産を「どう参照するか」を定めた文書は規範のため handbook に置く。

## 配置判断

まずドメイン導出規則でドメインを決め、次にドメイン内のカテゴリを選ぶ。複数プラットフォームに共通する責務境界や設計原則は core/architecture/ へ、特定プラットフォームに閉じる内容は `<platform>/api/` へ配置する。複数カテゴリにまたがる場合は、最も中心となる契約を持つカテゴリを選び、他の概念からリンクする。

### 契約と公開面の振り分け (cross/ADR-0014)

同じ機能の知識でも、**挙動の契約**と**公開面**は別の文書に置く ([cross/ADR-0014](../decisions/cross/0014-concepts-core-contract-platform-surface.md))。

| 書こうとしている内容 | 置き場 |
|---|---|
| 観察可能な挙動と保証 (何が起きるか・何を保証するか)、承認済みの platform 差分の挙動 | `core/api/` |
| 公開名・署名・コード例・framework 固有の注意 | `<platform>/api/` |
| 利用者が使う契約ではない検証機構の記述 | `core/architecture/` |

core/api の本文でバッククォート表記してよい識別子は、4 つの Skill 範囲 (iOS / Android / MAUI / KMP) すべての公開面に同綴りで存在することを実装コードで確認した**共通概念名**とそのメンバー名に限る (MAUI の interface 接頭辞 `I` は同綴り扱い、大文字小文字の違いは別綴り)。ここでいう KMP の公開面は、commonMain の公開宣言に加えて、KMP Skill の源泉に入る Android ホスト側 (`androidMain` の typealias が指す Android Native の型) と Swift 向け公開面 (`kmp/api/ios-host-integration.md` が扱う面) を合わせた 3 側を指す ([cross/ADR-0014](../decisions/cross/0014-concepts-core-contract-platform-surface.md))。条件を満たさない用語 (既定シングルトン・形態別の別名・添付の面・例外の詳細型・framework 型) は core ではバッククォートを付けずに散文で書き、綴りは platform 側に置く。承認済みの platform 差分の挙動は利用者が観察する契約なので core に残すが、識別子を含まない散文か表で書き、具体形を持つ platform concept へリンクする。

`<platform>/api/` の公開面 concept は機能単位で割り、4 platform で同じファイル名 (`dialog-surface` / `layout-surface` / `transition-surface` / `loading-surface` / `toast-surface`) を使う。各 core concept は末尾の「形態別の公開面」節から、各公開面 concept は冒頭から、互いにリンクする。

本プロジェクトはダイアログ UI ライブラリであるため、公開 API の契約と利用コード例は製品知識として concepts に記載できる。一方、内部実装フローや単なるファイル一覧は記載しない。

実装側の README と `kasane/` 配下の文書 (concepts / handbook) に同じ知識が載る場合、**正は `kasane/` 側、README は利用者向け抜粋 (写像)** とする。README 側に正がある旨の参照を1行置き、食い違ったら `kasane/` 側が勝つ (規範なら handbook、記述なら concepts)。

概念間リンクは実配置基準の相対パス。他ドメインへは `../../<domain>/<category>/<concept>.md` の形になる。他ドメインの ADR を参照するときは `<domain>/ADR-NNNN` の正式形で表記する。

## 新カテゴリの条件

既存カテゴリに適切に収まらない概念が3つ以上蓄積した場合に、ユーザー合意の上で新設する。

# Scenario ID の網羅検査 — 実行の記録

tasks 1.5 の確認記録 (2026-08-21)。
デルタスペックの Scenario ID (`PB-<領域>-<NN>`) が、すべてどこかのテスト名に現れているかを
機械的に突合する検査 `scripts/scenario-id-coverage.py` を追加し、現在の作業ツリーで実行した。

design Decision 7 の「メタ Scenario は spec に置かず、ID 網羅性はテスト外の検査スクリプトで見る」に対応する。
そのため本検査自体は spec の Scenario を持たない。

## スクリプトの位置づけ

- 置き場: `scripts/scenario-id-coverage.py` (Python 3 標準ライブラリのみ。`scripts/comment-policy-lint.py` と同じ流儀)
- 実行契機: **ローカル実行**。本リポジトリに CI ワークフローは無いため、実装の区切り (Scenario テストを足した後) と
  変更のレビュー前に手で回す
- 終了コード: 未網羅があれば 1、無ければ 0

```
python3 scripts/scenario-id-coverage.py                  # 既定の仕様置き場とテスト置き場で突合
python3 scripts/scenario-id-coverage.py --selftest       # 正規化と判定が壊れていないことの確認
```

## 何をどう突合しているか

| 面 | 既定の探索対象 | 抽出するもの |
|---|---|---|
| 仕様 | `kasane/changes/*/specs/*/spec.md` と `kasane/changes/archive/*/specs/*/spec.md` | `#### Scenario: [PB-xx-NN] …` の見出しの ID |
| テスト | `ios/Tests` / `android/*/src/test` / `android/*/src/androidTest` / `maui/*Tests` / `maui/android/native/*/src/test` / `kmp/*/src/*Test` | **テストの宣言**に現れる ID (表記ゆれを正規化) |

アーカイブ済みの仕様も既定で見るのは、仕様が完了後にアーカイブへ移る足場であり、
移った後も ID とテストの対応は保たれるべきものだから (`--specs` で対象を絞れる)。

テスト名の表記は言語ごとに割れるため、区切り文字の差を吸収して `PB-MD-01` の形へ正規化する
(deviation.md の「テスト名の表記差」に対応):

| 形 | 例 | 出どころ |
|---|---|---|
| `[PB-MD-01] …` | `@Test("[PB-MD-01] 結果確定で自分のダイアログだけが閉じる")` | Swift の表示名 (属性) |
| `PB_MD_01_…` | `func PB_MD_01_…` / `fun PB_MD_01_…` / `public void PB_MA_01_…` | Swift / Kotlin / C# の関数・メソッド名 |
| `PB-KC-01 …` | ``fun `PB-KC-01 表示中のコルーチンを…`()`` | Kotlin のバッククォート名 |
| `[PB-MA-01] …` | `[Description("[PB-MA-01] 添付")]` | C# の属性 |

### 数える場所はテストの宣言に限る

テスト側で ID を数えるのは、**関数・メソッドの宣言行**と、**その宣言に続いている属性・注釈**
(多行にわたる `@Test(…)` を含む) だけである。コメント・説明用の文字列・証跡ファイル名
(`capture("PB-SB-01-all-bars-hidden")` のような本文中の文字列) にしか ID が無いものは網羅と見なさない。

ソース全文から拾うと、テストを 1 本も書かずにコメントへ ID を書くだけで「テストあり」と読めてしまい、
`--require-mirror` も同じ理由で通ってしまう。宣言に限ることで、網羅の判定が実在のテストに結び付く。
宣言を見分けられない拡張子のファイルは ID を数えない (数え方が定義されていないものを黙って通さないため)。

判定は 3 つ:

- **仕様にあってテストに無い ID** → 失敗 (終了コード 1)。除外指定した ID は失敗にしない
- **テストにあって仕様に無い ID** → 警告 (テスト名の打ち間違いの検出)。終了コードは変えない
- **領域別の集計** を表示する

形態別の存在確認 (同じ ID が iOS と Android の両方にあるか) は既定では見ない。
両 Native に同じ ID を置く領域 (PB-TR / PB-MD / PB-WN) についてだけ `--require-mirror` で任意に検査できる。

### 除外 (PB-SM-01〜03)

Samples の PB-SM-01〜03 は Sample アプリのデモ項目・デモ画面・調整面を対象とする Scenario で、
自動テストではなく Sample 通し (tasks 8.4) の実機証跡で受け入れる。
スクリプト内の既定の除外リストに理由つきで置いてあり、出力にも除外として明示される。
`--allow-missing` で追加でき、`--no-default-allow` で既定の除外を外せる。

## 実行結果

```
$ python3 scripts/scenario-id-coverage.py
[Scenario ID 網羅検査]
仕様: 6 ファイル / Scenario ID 61 件 (ID を持たない仕様 19 ファイルは対象外)
テスト: 147 ファイル / 検出 ID 58 件

領域別の網羅:
  OK  PB-AA  3/3
  OK  PB-IA  3/3
  OK  PB-KC  3/3
  OK  PB-MA  5/5
  OK  PB-MD  5/5
  OK  PB-SB  7/7
  OK  PB-SM  0/3 (除外 3 件)
  OK  PB-TR  29/29
  OK  PB-WN  3/3
  合計: 58/61 (除外 3 件)

除外 (自動テストの対象外):
  PB-SM-01 — Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる
  PB-SM-02 — Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる
  PB-SM-03 — Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる

結果: 未網羅なし
```

**61 Scenario のうち 58 件がテストの宣言に存在し、残る 3 件は既定の除外 (PB-SM-01〜03) である。**
未網羅の ID は無い (終了コード 0)。

`--require-mirror` も通る — PB-TR / PB-MD / PB-WN の全 ID が iOS と Android の双方にある。
ID ごとの検出箇所は `coverage-locations.log` にある。

「ID を持たない仕様 19 ファイル」は ID 体系より前に書かれたアーカイブ済みの仕様で、
母数から外している (ID を使っている仕様の中に ID の無い Scenario 見出しが混じっている場合だけ警告する)。

## 検査が空振りしていないことの確認

- `--no-default-allow` で既定の除外を外すと、PB-SM-01〜03 が未網羅として並び終了コード 1 になる
  (`negative-check.log`)。「未網羅を検出できる」ことの確認
- `--selftest` は ID の抽出と正規化 (4 言語の表記 + 拾ってはいけない形)・**テスト宣言に限った抽出**
  (コメントだけ・本文の証跡ファイル名だけの ID を数えないこと・多行の属性を拾えること)・仕様の走査・
  網羅の判定・終了コードの決定・ミラー判定を検証する。全件 OK (`selftest.log`)
- テストの置き場の指定がどれか 1 つでも 1 件も拾えないときは警告を出す。
  置き場が変わって黙って空振りするのを防ぐため (現在の実行では 6 指定すべてがファイルを拾っている)

## ファイル

| ファイル | 内容 |
|---|---|
| `coverage.log` | 既定の実行 (終了コード 0) |
| `coverage-locations.log` | `--require-mirror --show-locations` の実行 (ID ごとの検出箇所つき) |
| `selftest.log` | `--selftest` の実行 |
| `negative-check.log` | `--no-default-allow` で未網羅が検出されることの確認 (終了コード 1) |

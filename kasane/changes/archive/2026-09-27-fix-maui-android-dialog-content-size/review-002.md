# レビュー結果: fix-maui-android-dialog-content-size (002 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

review-001 の Minor 2 件はどちらも解消した。比率指定と fill の軸での見え方は、新しいシナリオの A/B として証跡に残り、ルートの Width / Height は合否に入った。これで修正前の Android では大きさのシナリオ 8 件すべてが FAIL し、修正後は両 OS とも全件 PASS する。新しいシナリオ `size-dialog-contentview-proportional-fill` の外形の期待値は、layout-semantics の rect 決定手順 (比率は R、fill は A) に沿っている。証跡の数値もコードの判定式と一致する。見送り 2 件はどちらも妥当と判断した。ただし End / End 判定を見送った理由は訂正が要る (解消表を参照)。ライブラリ本体は前回から変わっていない。

## 照合した規約

- comment-policy.md (always) — 追加コメントの参照形式・記述類型。`scripts/comment-policy-lint.py --summary` は禁止 0 件
- test-execution.md (テストを実行するとき・完了を判定するとき)
- runtime-behavior-verification.md (実行時挙動が絡む不具合の完了判定) — 修正前の再現 → 同一手順での解消 → `evidence/` の 3 点

## 実行したビルドとテスト

| 実行 | 結果 |
|---|---|
| `dotnet build maui/KsDialogs.Maui.PlacementHost/...csproj -f net10.0-android` | 成功 (警告 0) |
| `dotnet build maui/KsDialogs.Maui.PlacementHost/...csproj -f net10.0-ios` | 成功 (警告 0) |
| `dotnet test maui/KsDialogs.Maui.Tests` | 197 件合格 / 失敗 0 / skip 0 |
| `scripts/local-path-lint.py` / `scripts/identity-lint.py` | どちらも exit 0 |
| Android 互換面 | 再実行せず (前回 38 件合格。互換面にも Native にも差分なし) |
| iOS 互換面 (`xcodebuild test`) | 未実行 (前回と同じ。完了判定で要るならオーケストレーター側で別に boot したデバイスで回す) |

実配置テストホストの実機再実行はしていない (証跡とコードで判定)。

## ライブラリ本体が変わっていないことの確認

`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogContent.cs` の更新時刻は review-001 より後だった。証跡には「修正前のビルドのために HEAD の版へ一時的に戻し、あとで md5 の一致を確かめた」とあり、この時刻の変化はその操作と整合する。内容では次を確かめた。HEAD との diff は前回と同じ 92 行追加 / 34 行削除。`Create` の差し替え、`ObserveFirstLayoutPass` の削除、`DialogContentView` (`OnAttachedToWindow` / `OnMeasure` / `OnLayout` / `ApplyAttributes`) は、前回レビューした内容と一字一句同じである。`git diff --stat -- maui/KsDialogs.Maui/` もこのファイル 1 件だけ。

## 指摘の解消表

| review-001 の指摘 | 扱い | 確認結果 |
|---|---|---|
| 🟡 Minor 1: 比率指定と fill の軸での見え方の変化に証跡が無い | 対応 | **解消**。`size-dialog-contentview-proportional-fill` を追加した (`ContentSizeScenario.cs`: 幅は比率 0.8、高さは Fill、ルートは 160x100 の ContentView)。外形 (`OuterRect` = ルートの platform view の親) が契約どおりの大きさになっていることと、ルートが宣言サイズのまま中央に置かれていることを合否に入れている (`PlacementRunner.cs` の `CheckOuter`)。修正前の Android ではルートが外形いっぱい (288x544 dp) に広がり、修正後は両 OS とも 160x100 で中央に置かれる。この A/B と観測の一文が `evidence/placement-host-runs.txt` の冒頭にあり、蒸留で拾える |
| 🟡 Minor 2: 大きさのシナリオが外形とルートの Width / Height を見ていない | 対応 (`rootBounds` を合否へ)。End / End 判定は見送り | **解消**。`boundsMatch` (ルートの `Width` / `Height` を宣言サイズと比べる。許容差は OS の許容差を MAUI の単位へ換算したもの) が `shownMatches` と AND で合否に入った。修正前の Android では Grid の 3 件と最小値の 1 件も `rootBounds=-1x-1` で FAIL に変わり、併発症状 (ルートに配置が届かない) を区別できるようになった。見送りの妥当性は下の注記を参照 |
| 🔵 Suggestion: Toast が一度も表示されなかったときの閉じ待ち | 見送り | **妥当**。テストホスト内の一過性の重なりで、証跡でも起きていない。起きても測定結果の不一致として表に出る (黙って PASS にはならない) |

### 注記: End / End 判定を見送った理由の訂正 (判定には影響しない)

オーケストレーターの見送り理由は「rootBounds で退行を検出できるため」だが、review-001 で挙げた退行はこれでは検出できない。挙げた退行は「包みが測定で大きい値 (AT_MOST の上限そのまま等) を返し、arrange ではルートを宣言サイズで中央に置く」形である。このときルートの `Width` / `Height` は宣言サイズのまま (160x100) なので、`rootBounds` も `shown` も PASS する。新設の `CheckOuter` も、外形を器が決める EXACTLY の軸でしか走らないので、AT_MOST の経路には効かない。

それでも見送りは妥当と判断する。理由は、この形の退行は既存の位置シナリオで検出できるからである。位置シナリオのプローブは `WidthRequest=120` / `HeightRequest=80` の ContentView で、End / End に寄せて出す。外形がルートより大きくなると、ルートは外形の中央に寄り、右端・下端が「領域の端から余白分内側」からずれて FAIL する。包みは Dialog / Loading / Toast に共通の部品なので、Dialog で検出できれば足りる。検出を担っているのが rootBounds ではなく位置シナリオであることを、蒸留時の記録ではこの形で残すこと (機構が無いと何が変わるか: 包みが外形を大きく返すと位置シナリオ 9 件が FAIL し、大きさのシナリオは PASS のまま)。

## 確認した観点

- **新シナリオの判定と契約の照合** (layout-semantics の rect 決定手順)
  - 幅: 比率が選ばれ、基準は R (可視領域。dialogMargin を控除する前) → `ratio × (v.Right - v.Left)`
  - 高さ: fill が選ばれ、基準は A (可視領域から上下の余白 24 を控除) → `(v.Bottom - v.Top) - 2 × margin`
  - どちらも手順 3 の優先順「比率指定 > fill 配置 > 内容サイズ」と、「比率の基準は R、fill とクランプの基準は A」に一致する。基準領域は既定の可視領域で、ホストの `VisibleArea` (Android はシステムバーの inset を控除、iOS は safe area) と対応する
  - 期待値の式は A へのクランプ (`min(比率 × R, A)`) を入れていない。ただし比率 0.8 では、R の軸長が約 240 単位を下回らない限りクランプは効かない。実在の端末では式が契約と食い違う条件に入らないので、指摘にはしない
- **修正前の症状を検出できるか** (lessons/code-review.md L-001)
  - 修正前 (API 35): ContentView ルートの 3 件は `shown=26.5x19` で縮み、Grid の 3 件と最小値の 1 件は `rootBounds=-1x-1` で、どちらも FAIL する。proportional-fill は `shown=288x544` と `rootBounds=-1x-1` で FAIL する。計 8 件が FAIL (SUMMARY: passed=9 / failed=8)
  - 修正後: Android 17/17、iOS 16/16 がすべて PASS
  - 修正前の proportional-fill は `outerMatches=True` / `rootCentered=True` (ルート = 外形なので中央判定は自明に真) だが、`shownMatches` と `boundsMatch` で FAIL しており、区別に必要な述語はそろっている
- **証跡の数値とコードの一致** (手計算で確認)
  - Android API 35 (密度 2): 可視領域は 720x1184。期待する外形は 576x1088 で、実測 outer (72,96)-(648,1184) も 576x1088、水平位置 72 = (720 - 576) / 2 と一致。ルート (200,540)-(520,740) は 320x200 px = 160x100 dp。左右の隙間は 128 / 128、上下は 444 / 444
  - iOS: 可視領域は 402x778。期待する外形は 321.6x730 で、実測 outer 321.4x730 (差 0.2 は許容差 1 以内)。ルートは 160x100、左右の隙間 80.7 / 80.7、上下 315 / 315
  - 位置シナリオの外形は、修正前 74x38 px (約 37x19 dp) が修正後 240x160 px (= 120x80 dp) に戻り、全件 PASS のまま。iOS の件数が 1 少ないのは `#if ANDROID` のシナリオ 1 件の分
  - 参考として 4 節に旧判定・API 31 での実行を残しており、前回の証跡と同一の行であることも確認した
- **修正前のビルドの取り方**: `PlatformDialogContent.cs` だけを HEAD の版に戻し、md5 で元に戻ったことを確かめたと記録されている。上の本体無変更の確認と整合する
- **外形の測り方**: `OuterRect` はルートの platform view の親を外形とする。修正後の Android では包み (`DialogContentView`)、修正前は Native の `DialogContentHolder`、iOS は包みの `DialogContentView` になる。いずれも器が中身として受け取る View で、holder はその大きさに合わせるので、契約の rect と同じ矩形を測っている
- **コード品質**: 新しいコメントは現在形で自己完結しており、禁止参照は無い。`ExpectedOuterRatio` の「0 の軸は fill」という符号化はテストホスト内に閉じていて、doc コメントで明示されている。公開 API・Native・互換面に差分は無い

## 指摘事項

なし。

## アクションプラン

1. 蒸留時の記録では、「外形がルートより大きくなる退行」の検出を位置シナリオが担っていることを残す (上の注記)
2. 完了判定で必要なら、iOS 互換面のテストを別に boot したデバイスで回す (handbook の test-execution: 3 実行)

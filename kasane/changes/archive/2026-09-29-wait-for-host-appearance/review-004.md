# レビュー結果: wait-for-host-appearance (004 回目)

**日付**: 2026-09-28
**判定**: APPROVED

## サマリー

verify-001.md §5 の所見 1 と所見 2 への対応だけを見た。Sample の catch 節のコメント 19 か所 (iOS Sample 9・KMP の iOS Sample 10) は、今の実装で起こりうる失敗 (未登録・型の食い違い・同じ ViewModel の重ね表示) だけを挙げる記述になった。コードはコメント行以外に 1 行も変わっていない。`evidence/test-run-summary.md` の Android の出現表は、unit (`src/test`) と instrumented (`src/androidTest`) の分類がテストファイルの実際の置き場所と一致した。Critical・Major・Minor は無く、今回のスコープ外の Suggestion が 1 件だけある。

## 照合した規約

- comment-policy.md (always): 許容する外部参照・禁止する参照・禁止する記述類型・公開メンバーの doc コメント・書き換え時の判断基準の各節と照合した
- test-execution.md (テスト結果を報告するとき): evidence の要約の分類 (android unit と instrumented の区別、件数の得方) と照合した
- sample-parity.md (`samples/` を触るとき): 今回の変更はコメントだけで、デモ項目・文言・色・OS 操作への反応・起動引数に触れないため、一致の対象外であることを確かめた

## 確認した内容

### 所見 1 (Sample の catch 節のコメント)

- **コードが変わっていないこと**: 対象 6 ファイルの `git diff e2d4ac9 -U0` で、追加・削除行はすべて `//` の行コメントだった。catch 節の有無・`assertionFailure` の呼び出し・戻り値は変わっていない
- **新しい記述が事実と合っていること**:
  - iOS の `DialogError` (`ios/Sources/KsDialogs/Contract/DialogError.swift`) は `presentationHostUnavailable` が削除され、残る 6 case は未登録 2 種・型の食い違い 3 種・重ね表示 1 種。どれも構成エラーで、「未登録などの構成エラー」の言い方と合う
  - KMP の `DialogException` (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogException.kt`) の説明も未登録と型の食い違いだけを挙げ、提示先の不在は含まない。`KsDialogsKmpError.publicError` のフォールバックも `DialogError.presentationHostUnavailable` から理由の欠けを表す専用の失敗に置き換わっている
  - Inline Dialog の 2 か所 (`samples/ios/KsDialogsSample/SampleMenuModel.swift:172`、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuModel.swift:194`) は、登録を使わない経路なので「未登録」ではなく「同じ ViewModel の重ね表示など」と書き分けている。この経路で起こりうる構成エラーは `viewModelAlreadyShowing` が主なので、書き分けは妥当
  - Loading・Toast の catch 節 (`samples/ios/KsDialogsSample/SampleMenuModel.swift:216`・`:253` ほか) も、提示先の不在を失敗の理由に挙げなくなった。Loading・Toast は提示先が無ければ待つ実装なので、事実と合う
- **4 ルートに同じ種類の古い記述が残っていないこと**: `samples/` 全体を「提示先不在・提示先の不在・提示先が無い・HostUnavailable・PresentationHost」などで検索した。catch 節の説明として残るものは 0 件。Android Sample・KMP の Android Sample・MAUI Sample には、ライブラリの失敗を catch してコメントを付ける箇所自体が無い (Kotlin・C# の Sample に `catch`・`runCatching` は見当たらない)。KMP の共有コード `samples/kmp/shared/.../SamplePresenter.kt` の KDoc も未登録だけを挙げている
- **comment-policy**: 新しいコメントは作業文書の参照・通番・履歴記述・デルタスペックの構文キーワードを含まず、そのファイルだけで意味が通る。`python3 scripts/comment-policy-lint.py --summary` は禁止 0 件。対象ファイルの advisory 5 件 (Sample 内の doc コメントの ADR 参照) は今回触れていない既存行で、Sample アプリの型はライブラリの公開面でもない

### 所見 2 (evidence の Android の出現表)

`evidence/test-run-summary.md` (evidence/ は未追跡のため `git diff e2d4ac9` には出ない。全文を読んで照合した) の分類を、テストファイルの関数名の出現数と突き合わせた。

| 置き場所 | 実際の出現 | 表の記載 |
|---|---|---|
| `android/ksdialogs-core/src/test` | PB-HW-01〜08 各 1、PB-HA-01〜03 各 1、DM-AN-01 1 | unit: PB-HW 8・PB-HA 3・DM-AN-01 1 |
| `android/ksdialogs-core/src/androidTest` | PB_HW_06 1、LD_HW_01〜07 各 1、TS_HW_01〜03 各 1 | instrumented: PB-HW-06 1・LD-HW 7・TS-HW 3 |
| `android/ksdialogs/src/androidTest` | LD_HA_02 1 | instrumented: LD-HA-02 1 |
| `android/ksdialogs/src/test` | 無し | 記載なし |

すべて一致する。「PB-HW-06 は unit と instrumented に 1 本ずつ、合わせて 2 本」の注記も実態どおりで、verify-001 の数えた PB-HW の 9 本 (8 + 1) とも矛盾しない。上の件数表・成否は変わっていない。ローカルの絶対パスや端末の個体識別子は含まれていない。

## 指摘事項

### 🔵 Suggestion Android・MAUI の Sample の自動再生のコメントが、待つ理由を Sample 側の都合として説明している (今回のスコープ外)

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:90`、`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:93`、`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:92`
**問題点**: 「ダイアログの提示先はメニューが画面に載ってから決まるため、再生もその時点まで待つ」とある。記述そのものは誤りではないが、この change でライブラリが提示先の出現を待つようになったため、Sample 側で待つ必要があるかのように読める。iOS 系の Sample (`SampleMenuScreen.swift` の 2 つ) は「提示先が現れるまで待つのはライブラリの役目」に改めており、4 ルートで説明の立場が揃っていない。catch 節の説明とは種類が違い、proposal は Sample の変更を iOS 系の自動再生に限っている (Android・MAUI の自動再生の形はこの change の対象外) ので、この change で直す必要は無い
**推奨修正**: この change では対応しない。蒸留または drift のときに、Android・MAUI の Sample の自動再生を post / Dispatch で遅らせる理由をどう書くか (または遅らせる処理を外すか) を検討する候補として記録する

## アクションプラン

1. (対応不要) 所見 1・所見 2 への対応は完了している。次の工程へ進んでよい
2. (任意・後続) Suggestion の Android・MAUI の Sample の自動再生のコメントを、蒸留または drift の検討候補として控える

## 備考

- コンテキストパッケージの指定により静的レビューとして行った。ビルドは実施済みで成功と報告されている (iOS Sample・KMP の iOS Sample)。今回の変更はコメントと evidence の文書だけで、テストの対象となるコードの変更は無い

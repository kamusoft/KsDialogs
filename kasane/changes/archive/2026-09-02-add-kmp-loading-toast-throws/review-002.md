# レビュー結果: add-kmp-loading-toast-throws (002 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

review-001 の Minor 3 件はいずれも解消されている。`KsLoading` の 2 本は生成 ObjC ヘッダの `@note` 行で裏付けられ、誤っていた「ヘッダ差分としては観測できない」の断り書きは撤去された。A/B とヘッダ差分の役割分担も明記され、`SampleMenuModel.swift` の catch コメントからは Toast 経路に存在しない「提示先不在」が落ちている。証跡に貼られたヘッダ抜粋は、本レビューで自分で framework をリンクし直して**全 7 宣言 (KsLoading 5・KsToast 2) を 1 行ずつ照合し、完全に一致**した。

コード本体は前サイクルから無変更 (この周で変わったのは `SampleMenuModel.swift` のコメント 1 行と `evidence/` のみ) で、退行は無い。ビルド・テスト・lint はすべて再実行して通っている。残るのは前回からの持ち越し Suggestion (回帰テストの要否はオーナー判断) と、証跡の読みやすさに関する非阻害の 🔵 1 件だけである。

### 実行した検証 (すべて本レビューで再実行)

| 対象 | コマンド | 結果 |
|---|---|---|
| KMP ライブラリ | `kmp` で `./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL。iosSimulatorArm64Test 49 + testAndroidHostTest 47 = **96 tests / 0 failures / 0 skipped** (review-001 と同値) |
| 生成 ObjC ヘッダの独立照合 | `samples/kmp` で `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --rerun-tasks` | BUILD SUCCESSFUL。`SampleShared.framework/Headers/SampleShared.h` を自分で読み、証跡の抜粋と突き合わせた (下記) |
| Sample 共有コード (Android) | `samples/kmp` で `./gradlew :androidApp:compileDebugKotlin` | BUILD SUCCESSFUL |
| KMP iOS Sample アプリ | `samples/kmp/iosApp` で `xcodebuild build -scheme KsDialogsSampleKmp` (停止中の iPhone 16e を id 指定。起動中の 2 台は触っていない) | ** BUILD SUCCEEDED **。新たな Simulator の boot はしていない |
| 一時検証コードの残留 | 作業ツリーを `KSD-VERIFY` / `showUnregisteredToast` / `Unregistered` で走査 | evidence の記述以外に該当なし (既存テストの `Unregistered*ViewModel` のみ) |
| 足場の書き換え | `exploration.md` の更新時刻 (16:16) が review-001 (16:43) より前で、この修正周では未変更 | 書き換えなし |
| lint | `comment-policy-lint.py` / `identity-lint.py` / `local-path-lint.py` / `doc-structure-lint.py` / `log-sanitize.py` | comment-policy 0 件 (907 ファイル)、identity 0 件、doc-structure は本 change の指摘 0 件。`evidence/` の 4 ファイルは未追跡で `git grep` ベースの lint が拾わないため個別に sanitize と手検査を通し、**置換対象なし / 絶対パス・個体値なし**を確認。local-path の 5 件は `scripts/local-path-lint.py` 自身の selftest fixture で本 change と無関係 (review-001 と同じ既存事象) |

### ヘッダ抜粋の独立照合 (Minor 1 の裏取り)

`evidence/kmp-generated-objc-header.txt` の 7 宣言を、自分でリンクしたヘッダと 1 行ずつ照合した。**全件一致**:

- `KsLoading`: VM 経路 2 本 (`show(viewModel:)` / `start(viewModel:)`) だけが `@note This method converts instances of DialogException, CancellationException to errors.`、message 経路 2 本と `hide` / `setMessage` は `CancellationException` のみ
- `KsToast`: VM 経路は `@note ... DialogException ...` + 戻り値 `BOOL` + `error:` 引数、message 経路は `@note` 行なしの `void`
- 抜粋が省いているのは各 `@note` に続く定型行 `Other uncaught Kotlin exceptions are fatal.` だけで、判定に影響しない

あわせて `evidence/kmp-toast-throws-ab.md` の「suspend では ObjC **シグネチャ**は `@Throws` の有無で変わらない」も、同じヘッダ内で裏が取れることを確認した — `KsLoading` の VM 経路 (`@Throws` あり) と message 経路 (なし) の completionHandler はどちらも `void (^)(NSError * _Nullable)` で同形である。VM 経路の `completionHandler_` の末尾アンダースコアは `KsDialogs.show(viewModel:placement:completionHandler:)` (ブロック引数が 2 個) との ObjC セレクタ衝突による名前修飾であって、`@Throws` の有無とは無関係 (両者の衝突は本 change 以前から在る形)。証跡の主張は正しい。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — この周で変わった `SampleMenuModel.swift:137` のコメントはファイル単独で読め、change / フェーズの裸参照も無い。lint 0 件
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む修正の完了判定) — 1〜3 の充足状況が前回から変わった。非 suspend の Toast は A/B 実測 (1〜3 充足)、suspend の `KsLoading` 2 本は「ユニットテストで症状を再現できない」対象だが、修正の効き目が生成ヘッダという静的成果物に現れる性質のため、`@note` 差分が 3 (証跡) の要件を満たす。証跡側も A/B が示す範囲 (機構) と、ヘッダが示す範囲 (公開面が機構に載ったこと) を分けて書いており、過剰主張が無い
- `kasane/handbook/cross/test-execution.md` — kmp ルートを `--rerun-tasks` 付きで全件実行し件数 (96) まで確認。負のコンパイル検査は本 change が触る形状 (Kotlin 側の呼び出し形) を対象にしないため未実行
- `kasane/handbook/cross/sample-parity.md` (`samples/**` を触る) — デモ項目・文言・色トークンは無変更。観測用の一時改変の戻し切りを全文走査で再確認
- ksn-core `references/evidence.md` — `evidence/` に置かれているのは抜粋ログ 2 本 + テキスト証跡 2 本のみ (動画・全文ログ・環境 dump なし)。sanitize は全ファイル「置換対象なし」
- ksn-core `references/paths.md` — 証跡内の参照は change 相対 / リポジトリ相対で、ローカル絶対パスは無い (省略形については 🔵 参照)
- `kasane/decisions/kmp/0001-swift-interop-plain-suspend.md` — 素の suspend 直接公開を維持。`@Throws` の追加はその徹底で範囲内
- `kasane/concepts/core/api/toast-semantics.md` / `loading-semantics.md` の失敗モデル、iOS Native protocol (`ios/Sources/KsDialogs/Presentation/KsToast.swift` の VM 経路 `throws` / message 経路 非 throwing) と整合
- `kasane/lessons/` に `code-review.md` は無いため、昇格済みの重点観点・除外観点はなし

## 前回指摘への対応状況

| review-001 の指摘 | 状態 | 確認内容 |
|---|---|---|
| Minor 1: `KsLoading` 2 本の証跡欠落と誤った断り書き | **解消** | `evidence/kmp-toast-throws-ab.md` の該当節が「`KsLoading` の 2 本 (suspend) の裏付け」に差し替わり、誤りの「ヘッダ差分としては観測できない」は消えている。`@note` 対比の抜粋と、全 7 宣言を収めた `evidence/kmp-generated-objc-header.txt` が追加され、内容は自分のビルドと一致 |
| Minor 2: A/B とヘッダ差分の役割分担が未記載 | **解消** | 同ファイルの「判定と、A/B とヘッダ差分の役割分担」節で、A/B が示すのは機構、公開面の裏付けはヘッダ、と明示。A ビルドで 3 箇所同時に外していた事実と、最外周が一時関数である点も書かれている |
| Minor 3: catch コメントの「提示先不在」 | **解消** | `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuModel.swift:137` が「未登録の ViewModel 型は Sample の組み立ての誤りなので、開発中に気づけるよう止める」に修正済み。Dialog を経由する `runToastOverlap` 側 (同ファイル 161 行) は従来どおりで正しい |
| Suggestion: `@Throws` の回帰テスト | **未対応 (想定どおり)** | 本 change では対応不要と結論済み。下記に持ち越す |

## 指摘事項

### [🔵 Suggestion] 証跡テキストの 2 箇所の書き方 (阻害しない)

**該当箇所**: `evidence/kmp-generated-objc-header.txt:4`、`evidence/kmp-toast-throws-ab.md:3,16`

**問題点**: いずれも判定には影響しない読みやすさの話。(a) ヘッダ証跡の前書き「`@Throws` が無い関数にはこの行が出ない (suspend は CancellationException のみ)」は、前半と括弧内が逆のことを言っているように読める — 実際の規則は「非 suspend で `@Throws` 無しなら `@note` 行そのものが出ず、suspend なら `@Throws` 無しでも `CancellationException` だけの行が出る」。抜粋ファイルは単独で読まれる前提なので、1 文に詰めず 2 文に割ったほうが誤読が減る。(b) パス表記が `handbook/cross/runtime-behavior-verification.md` (先頭の `kasane/` 欠落) と `samples/kmp/shared/.../SamplePresenter.kt` (中間を `...` で省略) の形になっており、ksn-core `references/paths.md` の「リポジトリルートからの相対パス」から外れている。grep で辿れる完全形が望ましい。

**推奨修正**: 本 change での対応は任意。蒸留時にまとめて直してもよい。

### [🔵 Suggestion] `@Throws` の欠落を機械的に固定する手段 (review-001 からの持ち越し)

**該当箇所**: `kmp/ksdialogs-kmp/src/androidHostTest/` (現状、`@Throws` を固定するテストはリポジトリに 1 本も無い)

**問題点**: この欠陥は「宣言を消しても Kotlin 側は何も壊れず、テストも全部通り、Swift 直呼びのときだけプロセスが落ちる」形で退行する。今回の証跡で「生成ヘッダの `@note` 行が宣言の有無を機械的に映す」ことが確定したため、review-001 で挙げた JVM リフレクション案に加えて**リンク済みヘッダを grep する検査**という選択肢も現実的になった (`:shared:linkDebugFrameworkIosSimulatorArm64` の成果物が要るためコストは高い)。いずれも `KsDialogs.show` を含む既存宣言全件の扱いを決める設計判断を含むため、本 change の要件にはしない。

**推奨対応**: 本 change では対応不要。必要と判断するなら別 change として起票する。

## アクションプラン

1. **なし (マージ可)** — 前回の Minor 3 件は解消済みで、Critical / Major・優先度の高い Minor は無い
2. 🔵 2 件はいずれもオーナー判断。証跡テキストの表現は蒸留時のついでで足り、回帰テストは別 change の候補として `kmp/ADR-0001` の現行照合とあわせて検討するのがよい

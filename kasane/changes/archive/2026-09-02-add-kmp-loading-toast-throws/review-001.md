# レビュー結果: add-kmp-loading-toast-throws (001 回目)

**日付**: 2026-09-02
**判定**: CHANGES_REQUESTED

## サマリー

コードは合意済みスコープ (決定事項 3 件) と過不足なく一致しており、既存先例 (`KsDialogs.show` / `GatewayKsDialogs.show`) と同じ「公開 interface + それを実装する具象クラスの両方に宣言する」形も守られている。Sample の 4 本の切り分けも正しい — 登録経路 (VM 経路) を通るのは `showCustomToast` の 1 本だけで、残り 3 本は message 経路のみ。ビルド・テスト・lint はすべて通り、検証用の一時コードの残留もない。生成 ObjC ヘッダを自分で作り直して、`KsToast.show(viewModel:)` が `error:` 付き (Swift で `throws`) に変わり `show(message:)` は非 throwing のままであることも独立に確認した。

一方で、証跡が覆っているのは Toast (非 suspend) の 1 本だけで、**`KsLoading` の 2 本には証跡が無い**。しかも evidence がその理由として書いている「suspend はヘッダ差分としては観測できない」は事実と異なる — 生成ヘッダの `@note` 行が「どのクラスを NSError に変換するか」を関数ごとに記録しており、VM 経路と message 経路で実際に差が出る (下記 Minor 1 に実測を添付)。本 change は「Swift から呼ぶ場合、この例外は NSError として届く」を**ライブラリの公開 KDoc に新しく書き足している**ため、その主張が検証できない形のままアーカイブされるのは避けたい。修正はコードではなく evidence の 1 節だけで済む。

### 実行した検証

| 対象 | コマンド | 結果 |
|---|---|---|
| KMP ライブラリ | `kmp` で `./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL。iosSimulatorArm64Test 49 + testAndroidHostTest 47 = **96 tests / 0 failures / 0 skipped** (handbook の実測値と一致) |
| Sample 共有コード (iOS) | `samples/kmp` で `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | BUILD SUCCESSFUL (生成ヘッダの確認に使用) |
| Sample 共有コード (Android) | `samples/kmp` で `./gradlew :androidApp:compileDebugKotlin` | BUILD SUCCESSFUL |
| KMP iOS Sample アプリ | `samples/kmp/iosApp` で `xcodebuild build -scheme KsDialogsSampleKmp` (Simulator は起動中の端末を使わず、停止中の iPhone 17 を id 指定) | ** BUILD SUCCEEDED ** |
| 一時検証コードの残留 | 作業ツリー全体を `KSD-VERIFY` / `showUnregisteredToast` / `Unregistered` で走査 | 該当なし (既存テストの `Unregistered*ViewModel` のみ)。差分自体も 8 行の追加に収まっており、戻し漏れは無い |
| lint | `comment-policy-lint.py` / `identity-lint.py` / `local-path-lint.py` | comment-policy 0 件 (907 ファイル)、identity 0 件。local-path は `scripts/local-path-lint.py` 自身の selftest 用 fixture 5 行のみ (本 change の変更前から在る既存の指摘で、本 change とは無関係) |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加した KDoc・コメントは change / フェーズ・レビュー通番の裸参照を含まず、ファイル単独で読める。既存の `(kmp/ADR-0002・0003)` 形式の参照も維持。lint 0 件
- `kasane/handbook/cross/test-execution.md` (テスト実行・完了判定) — kmp ルートを `--rerun-tasks` 付きの全件実行で回し、件数 (96) まで確認。負のコンパイル検査 55 本は本 change が触る形状 (Kotlin 側の呼び出し形) を対象にしていないため未実行
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む修正の完了判定) — Toast (非 suspend) は 1〜3 を満たす (修正前の abort 再現 → 修正後の catch 到達 → `evidence/` に抜粋ログ)。Loading の 2 本は 2・3 が未達 (Minor 1)
- `kasane/handbook/cross/sample-parity.md` (`samples/**` を触る) — デモ項目・文言・色トークンは無変更でパリティ要件の枠外。禁止事項の「観測用の一時改変を戻さないまま終える」は差分と全文走査の両方で戻し切りを確認
- `kasane/decisions/kmp/0001-swift-interop-plain-suspend.md` — 素の suspend 直接公開は維持。第三者依存の追加なし。`@Throws` の追加は ADR の Consequences「ObjC 境界で何が失われるかが公開 API 面にそのまま現れる」の徹底であり、範囲内
- `kasane/concepts/core/api/toast-semantics.md` の失敗モデル (「解決の失敗は show の呼び出し時点で同期に失敗。message 入口とインライン経路にこの失敗はない」) / `loading-semantics.md` / `result-notification-semantics.md` の KMP→Swift 境界の補足 — 実装はいずれとも整合。iOS Native protocol (`ios/Sources/KsDialogs/Presentation/KsToast.swift` の VM 経路 `throws` / message 経路 非 throwing) とも同形
- `kasane/lessons/` に `code-review.md` は無いため、昇格済みの重点観点・除外観点はなし (`spec-review.md` のみ)

### 合意済みスコープとの一致 (自分で確認した範囲)

- 決定 1 (`KsToast.show(viewModel)` の throws 化): `KsToast.kt:51` と `IosToastGateway.kt:30` / `AndroidToastGateway.kt:18`。生成ヘッダで `error:` 付きに変わることを再現確認
- 決定 2 (`KsLoading` は VM 経路 2 本だけ): `KsLoading.kt:43,88` と両 gateway。**message 経路に足していないことは正しい** — `IosLoadingGateway.beginBuiltin` は `KSDInteropLoadingBridge.beginBuiltin` → `LoadingCoordinator.beginUse(.builtin,...)` → `makeContent(for: .builtin)` と辿り、`.builtin` は registry を引かず throw する枝が無い (`ios/Sources/KsDialogs/Presentation/LoadingCoordinator.swift:256-266`)。`hide` / `setMessage` も同様に失敗を返す口が無い
- 決定 3 (Sample の非 suspend Toast は投げうるものだけ): `SamplePresenter.kt` の非 suspend 4 本のうち `toast.show(viewModel = ...)` を呼ぶのは `showCustomToast` だけで、他 3 本 (`showDefaultToast` / `showToastStack` / `showToastPlacement`) は message 経路のみ。`@Throws` は `showCustomToast` の 1 本だけに付いており、切り分けは正しい。Swift 側 (`SampleMenuModel.swift:122`) の `try` 追随も対応済み
- `DialogException` は `RuntimeException` 派生のため、`@Throws` を JVM 側 (`AndroidLoadingGateway` / `AndroidToastGateway`) に足しても Kotlin / Java 消費者に checked 例外の負担は生じない。決定事項の「Android 側は挙動に影響しない」と一致

## 指摘事項

### [🟡 Minor / 優先度高] `KsLoading` の 2 本に証跡が無く、evidence の「ヘッダ差分としては観測できない」が事実と異なる

**該当箇所**: `evidence/kmp-toast-throws-ab.md` の「適用範囲の限界」節

**問題点**: 同節は「suspend のため生成 ObjC ヘッダの形は `@Throws` の有無で変わらない。宣言の効果は実行時挙動だけで、ヘッダ差分としては観測できない」と書いているが、後半は誤り。ObjC シグネチャ (completionHandler の `NSError`) が変わらないのは正しい一方、**生成ヘッダの doc comment に出る `@note` 行が、その関数がどのクラスを NSError へ変換するかを列挙している**。本レビューで `:shared:linkDebugFrameworkIosSimulatorArm64` を回して確認した現在のヘッダ (`SampleShared.framework/Headers/SampleShared.h`) では、同じ `KsLoading` の中で VM 経路と message 経路にはっきり差が出ている:

```
/// show(viewModel:placement:) の直前
 * @note This method converts instances of DialogException, CancellationException to errors.

/// show(message:placement:) の直前
 * @note This method converts instances of CancellationException to errors.
```

つまり Loading 側にも 1 行で取れる証跡が実在し、それが「宣言が効いていること」と「message 経路には効かせていないこと (決定 2 の意図)」を同時に示す。本 change は `KsLoading` の KDoc に「Swift から呼ぶ場合、この例外は NSError として届く」を新規に書き足しており、5 本中 2 本というライブラリ側の過半がこの主張の対象なので、検証できない旨の誤った断り書きを残したままアーカイブするのは避けたい。

**推奨修正**: コード変更は不要。`evidence/kmp-toast-throws-ab.md` の「適用範囲の限界」を、(a) suspend では ObjC **シグネチャ**は変わらない、(b) ただし `@note` 行は変わるので Loading の 2 本はその差分で確認した、の 2 点に書き直し、上記 2 行の抜粋 (VM 経路と message 経路の対比) を貼る。取得手順は `samples/kmp` で `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` を回してヘッダを見るだけで、Simulator も実行も要らない。

### [🟡 Minor] A/B が 3 箇所を同時に切り替えているため、ライブラリ側の宣言単独の寄与が runtime では分離されていない

**該当箇所**: `evidence/kmp-toast-throws-ab.md` の「A: 修正前」節

**問題点**: A ビルドでは `KsToast.show(viewModel)` / `IosToastGateway.show(viewModel)` / 一時関数 `showUnregisteredToast` の 3 つから同時に `@Throws` を外している。Swift が実際に跨いだ境界は `SamplePresenter.showUnregisteredToast` であり (before ログのスタックでも `objc2kotlin_kfun:...SamplePresenter#showUnregisteredToast` が最外周)、Kotlin→Kotlin の内側呼び出しには `@Throws` は要らない。したがって「abort → catch」の A/B 差分は、厳密には**一時関数の宣言**で説明できてしまい、ライブラリ側の 2 箇所が単独で効いたことの証明にはなっていない。実際にはヘッダ差分 (`error:` 引数の出現) がライブラリ側の証拠として十分な識別力を持っているが、evidence 側にその役割分担が書かれていないため、読み手が A/B だけでライブラリ面を証明したと誤読しうる。

**推奨修正**: 追加実測は不要。「非 suspend の abort → NSError という**機構**は A/B が示し、**ライブラリの公開面がその機構に載ったこと**はヘッダの `error:` 引数が示す」と役割を 1〜2 文で明記する (Minor 1 の書き直しとまとめて 1 回で済む)。

### [🟡 Minor] Sample の catch コメントが Toast 経路に存在しない失敗を挙げている

**該当箇所**: `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuModel.swift:137`

**問題点**: `// 未登録・提示先不在は Sample の組み立ての誤りなので、開発中に気づけるよう止める` に変更されたが、この `do` ブロックが囲むのは `presenter.showCustomToast()` (登録経路 → 未登録のみ) と `Toast.shared.show(_:duration:placement:factory:)` (インライン経路 → factory の失敗) の 2 つで、**Toast には提示先不在の失敗が無い**。`DialogError.presentationHostUnavailable` を投げるのは `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:39` だけで、`ToastCoordinator` が投げるのは `viewFactoryTypeMismatch` / `viewFactoryNotRegistered` の 2 つのみ (`ToastCoordinator.swift:286,296`)。これは本 change の探索が確定させた事実 (「提示先不在を投げるのは Dialog 経路だけ」) とも食い違う。なお同ファイル 161 行 (`runToastOverlap`) の同文コメントは Dialog を経由するため成立しており、そちらに揃えたことが原因と見える。comment-policy の「現在の仕様を現在形で書く」に照らすと、到達しない失敗を挙げるコメントは誤読のもとになる。

**推奨修正**: `showCustomToast` 側は「未登録」だけにする (例: `// 未登録の ViewModel 型は Sample の組み立ての誤りなので、開発中に気づけるよう止める`)。`runToastOverlap` 側は現状のままでよい。

### [🔵 Suggestion] `@Throws` の欠落を機械的に固定する手段の検討

**該当箇所**: `kmp/ksdialogs-kmp/src/androidHostTest/` (現状、`@Throws` を固定するテストはリポジトリに 1 本も無い)

**問題点**: この穴は「宣言を消しても Kotlin 側は何も壊れず、テストも全部通り、Swift 直呼びのときだけプロセスが落ちる」という形で退行する。同じ穴が短期間に 2 回 (Sample → ライブラリ) 出ていることを踏まえると、回帰の受け皿が無いのは弱い。`androidHostTest` は JVM 実行なので、`KsToast::class.java.getMethod("show", ...).exceptionTypes` に `DialogException` が含まれることをリフレクションで固定でき、iOS ターゲットを起動せずに宣言の消失を検出できる (`@Throws` は JVM バイトコードの throws 節として残るため)。ただし `KsDialogs.show` を含む既存の宣言にも先例が無く、「JVM の副産物で Swift 境界の契約を守る」形の是非は設計判断を含むため、本 change での必須要件とはしない。

**推奨対応**: 本 change では対応不要。必要と判断するなら別 change として起票し、`KsDialogs.show` を含む宣言全件を一度に覆う形で検討する。

## アクションプラン

1. **Minor 1 + Minor 2 (evidence の書き直し、1 回でまとめて)** — `evidence/kmp-toast-throws-ab.md` の「適用範囲の限界」を訂正し、Loading の 2 本を生成ヘッダの `@note` 差分で裏付ける (再ビルド 1 回、Simulator 不要)。あわせて A/B とヘッダ差分の役割分担を明記する
2. **Minor 3 (コメント修正)** — `SampleMenuModel.swift:137` から「提示先不在」を落とす
3. **Suggestion (任意)** — `@Throws` の回帰テストの要否はオーナー判断。必要なら別 change へ

コード本体 (`kmp/ksdialogs-kmp/**` の 6 箇所と `SamplePresenter.kt` の 1 箇所) は修正不要である。

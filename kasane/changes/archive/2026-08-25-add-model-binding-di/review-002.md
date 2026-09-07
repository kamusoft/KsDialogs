# レビュー結果: add-model-binding-di (002 回目)

**日付**: 2026-08-25
**判定**: APPROVED

## サマリー

review-001 と second-opinion-code-001 の突き合わせで確定した Major 3件・Minor 3件は、いずれも意味論のレベルで解消されている。fallback はスロット単位の合成に変わり (`MergeFallbacks`)、value class の拒否は Android の全 show 経路が通る共通入口へ移り、KMP/iOS の結果型検証は show 時スナップショット (`Binding.declaredResultType`) を持つようになった。3件とも「直したことを実際に観察する」退行テストが付いており、名前だけ合わせた空テストは無い。全ルートを自分で回して green を確認した (ios 158 / android 66 / kmp 57 / maui 86)。

修正が新たな契約違反・退行を持ち込んだ形跡は見つからなかった。指摘は Minor 1件 (記録の欠落) と Suggestion 2件のみで、いずれも APPROVED を妨げない。

## 確定指摘の解消判定

| 確定指摘 | 判定 | 根拠 |
|---|---|---|
| [Major] MAUI `AddKsDialogs` 再呼び出しの fallback 後勝ち・null 上書き | **解消** | `MergeFallbacks` がスロット単位の合成 (`maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:168-182`)。退行テスト2件が実際に show を通して fallback の生存を観察 |
| [Major] Android インライン show の value class 検査迂回 | **解消** | 共通入口 `presentWithFactory` の先頭で検査 (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:57-64`)。提示先確認・factory 解決・紐付けのいずれよりも前 |
| [Major] KMP/iOS notifier アクセサの結果型検証がレジストリ参照 | **解消** | 紐付けに show 時の宣言結果型を保持 (`ios/Sources/KsDialogs/Contract/DialogNotifierBindings.swift:23`)、表示中は binding 側で判定 (`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:158-176`)。表示中の再登録を含む退行テストあり |
| [Minor] MAUI doc comment の絞り込み | **解消** | `TView` の登録が表示に効かないことを明記 (`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:55-63`・`:124-130`)。実装挙動は不変 |
| [Minor] MB-TS-02 のタイムアウト待ち | **解消** | configure 開始 gate 方式に変更 (`android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogTypedShowTests.kt:55-83`・`ios/Tests/KsDialogsTests/DialogTypedShowTests.swift:48-74`)。約10秒の待機が消え、観察する順序関係 (configure 完了前は生成 0) は保たれている |
| [Minor] deviation.md 末尾の連結 | **解消** | `kasane/changes/add-model-binding-di/deviation.md:19-20` が独立した2項目に分離。連結していた後半は iOS 分の独立エントリとして残り、内容の欠落はない |

### 修正内容の individual な確認

**MAUI fallback の合成** — `_fallbacks with { View = fallbacks.View ?? _fallbacks.View, ... }` で、渡された組の null スロットだけ既存を保持する。明示登録側 (`UpdateEntry`) の「スロット単位の後勝ち」と同じ意味論に揃っており、`DialogViewRegistry` クラスの `<remarks>` と `AddKsDialogs` の `<remarks>` の双方に「引数なしの呼び出しは null で上書きしない」と書かれている。maui/ADR-0005 の「null 上書き・後勝ちの粗を構造ごと消す」に沿う。退行テストは `maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:228` (設定付き → 引数なし) と `:253` (View / VM を別々の呼び出しで設定) の2件で、どちらも「設定が残っている」ことを表明値ではなく実際の show の観察 (BindingContext の同一性 / VM 型) で確かめている。相方が挙げた3つの選択肢のうち「非 null スロットの合成」を採っており、推奨の範囲内。

**Android の共通入口** — `present(viewModel, registry, ...)` と `present(viewModel, factory, ...)` の両方が `presentWithFactory` に集約され、`viewModel::class.requireReferenceTypeViewModel()` が factory 解決 (`resolveFactory()`) よりも前に走る。型指定 show 側は `Dialog.kt:46` の事前検査が残っており、VM factory を呼ぶ前に弾く順序も保たれている。`requireReferenceTypeViewModel` は kotlin-reflect に依存しない `box-impl` 判定のままで、追加の依存は入っていない。テストは MB-AN-03 の同名で3本 (登録+型指定 / インライン factory / インスタンス渡し) あり、インライン版は「中身の生成へ進まない」ことを `creationCount == 0` で、提示に至らないことを `presentedContainers.isEmpty()` で確認している。インライン show がレジストリを読み書きしない性質 (core/ADR-0013) も壊れていない。

**KMP/iOS の結果型スナップショット** — `bind` は常に `factory.declaredResultType` を控え、`notifier(for:result:)` は「表示中は binding、非表示時のみレジストリ」の順で判定する。「型判定は表示中かどうかより先」という既存契約 (show 前でも不一致を報告する) は if の順序で保たれている。Native 経路の factory は `declaredResultType` が nil で、Native の `vm.notifier` は `resultChannel(for:)` しか使わないため、この変更で Native 側の観察は変わらない (nil の binding で判定が素通りする経路は、修正前のレジストリ参照でも同じく nil を引いていたので退行ではない)。退行テスト `ios/Tests/KsDialogsTests/KsDialogsKmpModelBindingTests.swift:81` は、表示したまま同じ VM 型を別結果型で登録し直したうえで「出ている方は元の型で引ける」「新しい型を指定すると不一致で失敗する」「未表示のインスタンスは現在の登録で判定される」の3点を同時に押さえており、修正前の実装では通らない形になっている。

## 指摘事項

### [🟡 Minor] value class 拒否の入口を spec の列挙より広げた点が deviation.md に無い

**該当箇所**: `kasane/changes/add-model-binding-di/deviation.md` (該当エントリなし) / `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:63`

**問題点**: android-native spec の Requirement は「value class の VM は**登録および型指定 show の時点で**構成ミスとして拒否する」と拒否点を2つに限って書いている。修正後の実装は、これに加えてインスタンス渡し show とインライン show でも拒否する (Scenario MB-AN-03 の THEN 「構成ミスとして失敗する」は満たすが、Requirement の列挙より広い)。

これに伴う観察可能な変化が2つある。

1. レジストリ経由のインスタンス渡し show に value class を渡した場合、従来は `ViewFactoryNotRegistered` (登録できない型なので必ず未登録) だったのが `ValueClassViewModel` に変わった。どちらも構成ミス失敗だが、公開例外型が変わっている
2. 拒否の入口が spec の記述より多いこと自体

足場 (spec) は凍結で書き換えられないため、差分の受け皿は deviation.md しかない。deviation.md は archive 後も残る一次記録で、現状は「spec は2箇所、実装は4経路」の食い違いがどこにも説明されていない。

**推奨修正**: deviation.md に「android-native spec (value class の拒否点): spec は登録・型指定 show の2点 → 実装は全 show 経路の共通入口で拒否。理由: dialog-contract / core/ADR-0018 の参照型限定は全 show 経路に及び、インライン show が唯一の抜け穴として残るため。付随して、レジストリ経由のインスタンス渡し show の失敗は `ViewFactoryNotRegistered` から `ValueClassViewModel` へ変わる」旨を1エントリ追記する。コードの修正は不要。

---

### [🔵 Suggestion] MAUI では値型 VM が fallback 経由のインスタンス渡し show で提示まで到達しうる

**該当箇所**: `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs:39-41`・`maui/KsDialogs.Maui/Internals/DialogPresenter.cs:31-40`

**問題点**: Android で塞いだ穴の姉妹面。MAUI のインスタンス渡し show は `ShowAsync<TResult>(IDialogViewModel<TResult> viewModel, ...)` と interface を直接受けるため、`class` 制約が書けず struct の VM を渡せる。登録側 (`Register` / `RegisterForDialog`) は `class` 制約で弾かれるので通常は `ViewFactoryNotRegistered` で失敗するが、View fallback resolver を設定した構成 (規約ベースの一括解決 — まさに maui/ADR-0005 が狙う使い方) では、fallback が View を返した時点で提示まで到達する。

到達した場合、`DialogNotifierBindings` の鍵は show に渡された box になる。中身が `(MyStruct)BindingContext` のように値型へ取り出してから `Notifier` を読むと boxing で別インスタンスになり、`Notifier` は常に null を返す。結果を報告できないままダイアログが残る (呼び出し元がキャンセルするまで閉じない) — Android で「声を上げない入口」として問題視したのと同じ形。

ただし **maui-binding spec は値型 VM の拒否を明示的にコンパイル時 (class 制約) と定めており**、interface 引数のインスタンス渡し show は C# ではコンパイル時に拒否できない。つまり spec の方針が入口を1つ数え落としている可能性があり、実装だけの誤りとは言い切れない。deviation #14 も「値型 VM の拒否は登録と型指定 show の class 制約で成立し」と現在の位置を合意済みとして記録している。

**推奨修正**: 本変更での実装修正は求めない。次のいずれかをオーナー判断で選ぶ — (a) `DialogPresenter.PresentAsync` の入口で `viewModel.GetType().IsValueType` を実行時に弾く (MAUI には `ValueClassViewModel` 相当の例外が無いため、例外型の新設を伴う公開面変更になる)、(b) 到達条件 (値型 VM + View fallback) と観察される挙動を deviation.md か concepts に明記して現状を追認する。Android との非対称が本変更で新たに生じた点なので、蒸留フェーズで少なくとも記録には残すことを勧める。

---

### [🔵 Suggestion] 合成に変えたことで、公開面から fallback を解除する手段が無くなった

**該当箇所**: `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:150-182`・`maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:24`

**問題点**: 修正前は「引数なしの `AddKsDialogs()` が両スロットを null で潰す」ことが (意図せず) 解除手段になっていた。合成に変えた結果、`DialogViewRegistry.Shared` が process 全体で1つである以上、一度設定した fallback を公開 API から外す方法が無くなった。ライブラリ自身のテストは internal の `UseFallbacks(null)` を TearDown で使って隔離しているが、利用者のテストコードにはこの入口が無い。

実アプリでは host が1つなので実害はない。影響が出るのは「1つのテストプロセスで複数の host を組み立てる利用者のテスト」で、先の検証で設定した fallback が次の検証へ漏れる。修正前後どちらが良いかは自明でなく、合成は相方の推奨3案の1つなので選択自体は妥当。

**推奨修正**: 対応は任意。取るなら「テスト向けの解除手段を公開面に足す」よりも、`AddKsDialogs` の `<remarks>` に「設定は共有レジストリに載り、解除する入口は無い (process 内で持続する)」の1文を足すのが安い。

## 確認した観点 (実測)

**ビルドとテスト** — 全ルートを自分で実行し green を確認した:

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | 158 tests / 28 suites passed |
| android/ | `./gradlew test` | 66 tests / 0 failures |
| kmp/ | `./gradlew allTests` | 57 tests / 0 failures |
| maui/ | `dotnet test` | 86 / 0 failures |

**横断検査** — `python3 scripts/scenario-id-coverage.py` は「未網羅なし」(91/97、除外 6 = MB-SM / PB-SM の Sample 通し)。`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` はいずれも 0 件 (comment-policy は 599 ファイル検査)。

**足場の凍結** — `git diff HEAD -- kasane/changes/add-model-binding-di/` の差分は `tasks.md` (チェックのみ) と `ui/brief.md` (ksn-ui の照合記録) の2ファイルだけで、proposal / design / specs / deviation の本文に修正サイクルによる書き換えは無い (deviation.md は未コミットの新規ファイルのため diff には出ないが、review-001 が指摘した連結の解消以外の改変は内容から確認できる)。tasks.md にはこの修正サイクルで新規タスクが足されておらず、既存チェックの虚偽も見つからなかった。

**修正が触れた範囲の退行確認** — 3つの Major 修正はいずれも既存の契約を弱めていない: (1) terminal-path 契約 (紐付け後の全終端経路での除去) は Android の `finally` / iOS の `defer` の位置が変わっておらず、value class 検査は紐付けより前に throw するので除去対象を作らない。(2) 並行 show 検出 (`ViewModelAlreadyShowing`) の位置と意味は不変。(3) iOS の紐付け表は `declaredResultType` フィールドの追加のみで、キー方式 (`ObjectIdentifier` + 弱参照の生存・同一性確認) とアドレス再利用の掃除は不変。(4) MAUI のスナップショット解決 (`Entry` のコピー) と解決順序 (明示 → fallback → 失敗) は不変。

**Scenario との対応** — MB-AN-03 は spec の THEN (構成ミスとして失敗・結果に化けない) を4経路すべてで満たす。MB-KM-04 は spec の「typed error で、show 外の『空』と区別できる」を満たしたうえで、表示中の再登録に対する show 時スナップショット (design Decision 3 の「呼び出し時点のスナップショット」) にも整合するようになった。MB-TS-02 は gate 化後も「configure 完了まで中身の生成が始まらない」という順序関係を観察している。

## 未解決 (引き継ぎ — 本レビューでは扱わない)

review-001 で挙げた低優先度の項目のうち、今回の修正対象外だったものは未対応のまま残っている。いずれも APPROVED を妨げない:

- `ActivatorUtilities` の生成失敗が `DialogException` に包まれない (review-001 Suggestion)
- `verification/` が ksn-core のディレクトリ規約に無い置き場 (蒸留フェーズの判断)
- concepts の追随2件 — `kasane/concepts/cross/conventions/sample-parity.md` のデモ項目表、`kasane/concepts/cross/conventions/test-execution.md` の件数・負の compile 検査本数 (件数は本サイクルでさらに動いた: ios 158 / android 66 / kmp 57 / maui 86)
- `ui/brief.md`「未解決」節の遷移記号 `›` の扱い (オーナー判断待ち・deviation #8)

## アクションプラン

優先度順。いずれも APPROVED を妨げるものではない。

1. deviation.md に value class 拒否の入口拡張 (+ 失敗例外型の変化) を1エントリ追記する — archive 前に直すのが安い
2. MAUI の値型 VM 到達経路について、実装で塞ぐか deviation で追認するかをオーナーが決める (Android との非対称が本サイクルで生じたため、記録だけでも残す)
3. `AddKsDialogs` の `<remarks>` に fallback が process 内で持続する旨の1文 (任意)
4. 蒸留フェーズへの引き継ぎ: concepts の追随 (件数は本レビューの実測値へ)、`verification/` の置き場の扱い

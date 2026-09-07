# レビュー結果: add-model-binding-di (001 回目)

**日付**: 2026-08-25
**判定**: APPROVED

## サマリー

4形態 + KMP interop 面へのデルタスペック (6能力・97 Scenario 中 MB 系 33 件) の実装は、全 Requirement / Scenario を実際に検証するテストを伴って満たしている。全ビルドルートを自分で回して green を確認し (ios 156 / android 64 / android instrumented 139 / kmp 57 / maui 84 + bridge 15)、Scenario ID 網羅検査・両 Native ミラー検査・新規の負の compile 検査4本も期待どおりの診断で失敗することを実測した。特に「紐付け後の全終端経路で除去する」という terminal-path 契約は3形態とも `defer` / `finally` で構造的に担保されており、テストも「factory 実行中に引けること」「失敗直後に外れていること」を実際に観察する形で書かれている (名前だけ合わせた空のテストは見つからなかった)。

Critical / Major はなし。指摘は MAUI DI 糖衣まわりの低優先度 Minor 3件と、Suggestion 4件 (うち2件は蒸留フェーズへの引き継ぎ事項)。

## 指摘事項

### [🟡 Minor] `AddKsDialogs` の再呼び出しが設定済み fallback resolver を黙って消す

**該当箇所**: `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:37-38`

**問題点**: `AddKsDialogs` は毎回無条件に

```csharp
DialogViewRegistry.Shared.UseFallbacks(
    new DialogFallbackResolvers(options.ViewFallback, options.ViewModelFallback));
```

を実行する。`configure` を省略した呼び出しは両スロットが `null` の組を書き込むため、`AddKsDialogs(o => o.UseViewFallback(...))` のあとに素の `AddKsDialogs()` が来ると、設定済みの一括解決が失われる。初期化サービスの登録は `TryAddEnumerable` で冪等なのに、fallback の設定だけが「後勝ちで全消し」という非対称になっている。`maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:228-240` (`TheInitializerRegistrationIsIdempotent`) はまさに `AddKsDialogs().RegisterForDialog<...>().AddKsDialogs()` の並びを書いており、この並びが現実に起こりうる形であることを示しているが、fallback が消える点は検証も文書化もされていない。

spec (maui-binding「fallback resolver」Requirement) は再呼び出しの意味論を規定していないため仕様違反ではないが、DI チェーンは複数箇所から組み立てられるのが普通で、消えたことが実行時まで分からない (未登録 VM が `ViewFactoryNotRegistered` になるだけ) 分だけ発覚が遅い。

**推奨修正**: `configure` が実際に設定したスロットだけを更新する (未設定スロットは保持する) か、それが意図に反するなら `KsDialogsOptions` の doc comment と `AddKsDialogs` の `<remarks>` に「呼び出しごとに置き換わる」ことを明記し、置き換えを固定するテストを1件足す。

---

### [🟡 Minor] `TryAddTransient<TView>()` は中身の View の生成経路に一切効かない

**該当箇所**: `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:100`・`:124-133` (`CreateView`)

**問題点**: `RegisterPair` は `TView` / `TViewModel` を `TryAddTransient` でサービス登録し、doc comment には「利用者が既に登録していればそれを尊重する」とある。ところが View の生成は `ActivatorUtilities.CreateInstance(provider, viewType, ...)` で行われ、`ActivatorUtilities` は **その型自身のサービス登録を参照しない** (コンストラクタ引数だけを provider から解決する)。したがって利用者が `services.AddTransient<MyView>(sp => 独自の組み立て)` を先に書いていても、ダイアログの中身としてはその factory は使われず、コンストラクタ直呼びになる。

一方 `TViewModel` 側は `DialogServiceProvider.Require().GetRequiredService<TViewModel>()` で解決するため、利用者の登録が本当に尊重される。同じ doc の一文が2つの型引数で違う意味になっている。

なお spec (maui-binding「1行登録」Requirement) は「TView の生成は現在の VM インスタンスを明示引数に渡して行う」と定めており、実装はそちらに従っている。仕様違反ではなく、**doc comment の書きぶりが実挙動より広く読める**のが問題。

**推奨修正**: doc comment を「サービス登録は上書きしない (TryAdd)」という意味に限定して書き直し、View の生成が常にコンストラクタ経由であることを明記する。あわせて `TView` を DI 登録する目的 (コンストラクタ依存の解決以外に何があるか) が無いなら、登録自体の要否を再検討する。

---

### [🟡 Minor] deviation.md の最終エントリが2件分の記述の連結になっている

**該当箇所**: `kasane/changes/add-model-binding-di/deviation.md:19`

**問題点**: `scripts/scenario-id-coverage.py` の `DEFAULT_TEST_GLOBS` 追加についての付随修正の文末 (`…必要な2行追加 (2026-08-25)`) に、続けて DialogRegistryTests の書き換えについての記述 (`「同一 VM インスタンスの再 show は独立した重ね出しになる」テストは MB-NI-04 … と矛盾するため、別インスタンスでの重ね出し検証 … へ書き換え`) が改行なしで連結している。この後半は既に項目5 (Android) と項目16 (MAUI) に記録済みで、重複でもある。

deviation.md は archive 後も残る「合意済み差分」の一次記録であり、後から読む人が「スクリプトの glob 追加とテスト書き換えが1件の乖離」と誤読しうる。

**推奨修正**: 連結した後半を削除する (項目5・16 が既に記録済み)。または独立した行に分ける。

---

### [🔵 Suggestion] Android のインライン show では value class の VM が拒否されない

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:29-38`

**問題点**: `requireReferenceTypeViewModel()` は `DialogViewRegistry.register` / `registerViewModel` (`DialogViewRegistry.kt:60`・`:76`) と型指定 show (`Dialog.kt:45`) で呼ばれるが、インライン show (`show(viewModel, placement, factory)`) では呼ばれない。インライン show はレジストリを経由しないため、value class の VM がどの検査も通らずに提示まで到達する。

実害は限定的で、インライン show は2引数 factory しか公開しておらず報告口は引数で必ず渡るため、壊れるのは中身の中で `viewModel.notifier` を引いた場合だけ (boxing で同一性が失われ常に `null` になり、結果を報告できないまま黙って残る)。ただし dialog-contract の「VM 契約は参照型に限定する」という保証に、声を上げない入口が1つ残っている形にはなる。

android-native spec は拒否点を「登録および型指定 show」とだけ列挙しているため、**仕様側が入口を数え落としている可能性**があり、実装だけの誤りとは言い切れない。

**推奨修正**: インライン show の入口にも `requireReferenceTypeViewModel()` を1行足す (局所的で既存テストに影響しない)。仕様の意図として意図的に外していたのなら、その旨を deviation.md に残す。

---

### [🔵 Suggestion] `RegisterForDialog` した View の生成失敗が `DialogException` にならない

**該当箇所**: `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:124-133`

**問題点**: `TakesViewModel` が false を返す形 (VM を受けないコンストラクタしか無い) で、かつコンストラクタ依存が DI で解決できない場合、`ActivatorUtilities.CreateInstance` が素の `InvalidOperationException` を投げ、それが提示経路をそのまま抜けて呼び出し元に届く。design Decision 6 は「構成ミスは各形態の既存の構成ミス失敗経路に合流させる」としており、`ServiceProviderUnavailable` は `DialogException` として足されているのに、この経路だけ型が揃わない。

**推奨修正**: 生成失敗を `DialogException.ViewFactoryNotRegistered` 相当 (または新設の構成ミス例外) に包み直す。優先度は低い — 利用者から見て「構成ミスで即失敗」という観察可能な挙動自体は spec どおり。

---

### [🔵 Suggestion] `verification/` は ksn-core のディレクトリ規約に無い置き場

**該当箇所**: `kasane/changes/add-model-binding-di/verification/kmp-model-binding/README.md`

**問題点**: design の Risks 節と tasks 4.3 が「verification/ に証跡を残す」と指示しており、実装はそれに忠実に従っている (足場の指示どおりなので違反ではない)。一方 ksn-core のディレクトリ規約が `changes/<id>/` 配下に定める置き場は `evidence/` と `ui/` だけで、`verification/` は archive 時の媒体削除 (`distill.archive-media`) の対象にも入らない。中身は媒体ではなくテキストの索引で、実体の画像・ログは正しく `evidence/` に置かれている。

**推奨修正**: 蒸留時に、この README の内容を `evidence/` 側の索引か verify / deviation の本文へ寄せるか、`verification/` を規約に足すかを決める。実装側の作業ではない。

---

### [🔵 Suggestion] concepts の追随が2件残っている (蒸留フェーズの作業)

**該当箇所**: `kasane/concepts/cross/conventions/sample-parity.md:42-54`・`kasane/concepts/cross/conventions/test-execution.md:17-26`・`:132-184`

**問題点**: コード側は正しいが、長命層が実態から離れた状態が残る。

- sample-parity 規約の「文言の正」表は現在のデモ項目を6件としており、`Model Dialog` と `ViewModel から表示しています` が入っていない。写像である `samples/README.md` は7件に更新済みなので、**規約 (正) より projection が先行している**状態になっている (規約自身が「食い違ったら本文書が勝つ」と書いているため、放置すると Model Dialog が規約違反に見える)。
- test-execution 規約の実測値が古い: 負の compile 検査は 27 本 → 実際は 31 本 (新フラグ `KsDialogsNegativeCheckTypedShowContract` / `KsDialogsNegativeCheckNotifierResultType` / `KsDialogsNegativeCheckValueTypeViewModel`、ios の `KSDIALOGS_NEGATIVE_CHECK_VALUE_TYPE_VIEW_MODEL` が未記載)。件数表も ios 137→156 / android 50→64 / instrumented 138→139 / kmp 51→57 / maui 62→84 に変わっている。

**推奨修正**: ksn-distill での追随項目として引き継ぐ。実装フェーズでの concepts 更新は求めない。

## 確認した観点 (実測)

**ビルドとテスト** — 全ルート自分で実行し green を確認した:

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17'` | 156 tests / 28 suites passed |
| android/ | `./gradlew test --rerun-tasks` | 64 tests / 0 failures (`:api-surface-check:compileDebugKotlin` と `verifyNoDeclarativeUiDependency` を含む) |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` (実機1台) | `:ksdialogs` 105 (skipped 1) + `:ksdialogs-compose` 34 = 139 / 0 failures |
| kmp/ | `./gradlew allTests --rerun-tasks` | 57 tests / 0 failures (`:api-surface-check:compileKotlinIosSimulatorArm64` を含む) |
| maui/ | `dotnet test` | 84 / 0 failures |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 15 / 0 failures |

**負の compile 検査 (本変更の新規4本)** — 1本ずつ回し、いずれも期待どおりの診断で失敗することを確認:

- `KsDialogsNegativeCheckTypedShowContract` → CS0311 (`string` → `IDialogViewModel`)
- `KsDialogsNegativeCheckNotifierResultType` → CS0029 (`DialogNotifier<bool>` → `DialogNotifier<string>`)
- `KsDialogsNegativeCheckValueTypeViewModel` → CS0452 が2件 (登録・型指定 show の両方)
- ios `KSDIALOGS_NEGATIVE_CHECK_VALUE_TYPE_VIEW_MODEL` → `non-class type 'ConsumerValueDialogViewModel' cannot conform to class protocol 'DialogViewModel'`

**横断検査** — `python3 scripts/scenario-id-coverage.py` は「未網羅なし」(91/97、除外 6 = MB-SM/PB-SM の Sample 通し)。`--selftest` 全件 OK、`--require-mirror` も「対象領域の ID はすべて iOS / Android の双方にあります」。`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件。

**仕様充足** — deviation.md 記録済みの19件 (乖離13件・付随修正6件) は合意済みとして違反に数えていない。付随修正は同梱条件 (本務と同じ能力内 / 公開 API・ADR に触れない / 局所的 / テストで担保) に収まっていることを確認した — README 群と kmp テストダミーはビルド追随の必然、`scenario-id-coverage.py` の3件は本変更の横断検証 (task 6.1) の成立に必要な数行で、`--selftest` が壊れていないことを実測した。tasks.md の全チェックについて実物 (実装・テスト・証跡) の存在を確認し、虚偽のチェックは見つからなかった。足場アーティファクト (proposal / design / specs) に書き換えは無い (`git diff` で確認)。`ui/brief.md` の追記は ksn-ui が書く照合結果の記録であり、逆流修正ではない。

**堅牢性** — terminal-path 契約 (紐付け後の全終端経路での除去) を3形態のコードで追跡した。iOS は `defer` (`ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:52`)、Android は `try/finally` (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:84-88`)、MAUI は `try/finally` (`maui/KsDialogs.Maui/Internals/DialogPresenter.cs:70-75`) で、いずれも「並行 show 検出による失敗」は紐付け成立前に throw するため先行 show の紐付けを壊さない。除去は結果型復元より前に起きるため MB-NI-03 の観察順序も満たす。同一性キーの実装 (`ObjectIdentifier` + 弱参照 / `WeakReference` + identity hash + `ReferenceQueue` / `ConditionalWeakTable`) はいずれも equals 非依存で、解放済みインスタンスのアドレス再利用と GC 後の掃除も手当てされている。

**テストの質** — MB-NI-01 は「factory 実行中に `viewModel.notifier` が引けること」を factory 本体の中で観察しており、生成後に紐付ける実装では通らない形になっている。MB-TS-02 は門で非同期 configure を止めて「その間 View factory が呼ばれない」ことを数で確認している。MB-NI-07 は初回だけ失敗する factory で異常終了経路を作っている。言い訳コメントによる実質スキップは見つからなかった。自動化が浅い箇所 (MB-KM-02 の配送到達・MB-KM-03 の Native 報告) は deviation.md #7 と `verification/kmp-model-binding/README.md` に理由と補完先が明示されている。

**設計品質** — レジストリの2スロット化はスナップショット解決 (呼び出し時点のエントリのコピー) で統一され、MB-TS-06 のスロット独立性を3形態で満たす。テスト容易性は `Dialog(registry, gateway)` / `DialogTestHarness` / `TestMauiApp` (実 `IMauiInitializeService` を回す) で確保されている。命名は形態間で意図的に非対称な箇所 (Kotlin/C# の `registerViewModel` / `RegisterViewModel` vs Swift の `register(_:viewModel:)`) が deviation #4・#12 に理由付きで記録済み。

## 未解決 (オーナー判断待ち — レビューでは扱わない)

`ui/brief.md`「未解決」節の、Model Dialog のメニュー行の遷移記号 `›` の扱い。deviation #8 に経緯が記録済みで、既存6行すべてに及ぶ判断のため本変更のレビュー対象外とした。

## アクションプラン

優先度順。いずれも APPROVED を妨げるものではない。

1. deviation.md:19 の連結エントリを分割 / 重複削除 (記録の正確さ。archive 前に直すのが安い)
2. `AddKsDialogs` 再呼び出し時の fallback の扱いを決める (マージするか、置き換えを明記してテストで固定するか)
3. `RegisterForDialog` の doc comment を実挙動 (View は常にコンストラクタ生成) に合わせて書き直す
4. Android のインライン show への value class 検査の追加 (または見送りの deviation 記録)
5. `ActivatorUtilities` の生成失敗の `DialogException` への包み直し
6. 蒸留フェーズへの引き継ぎ: sample-parity / test-execution の追随、`verification/` の置き場の扱い

## 備考

`kasane/config.yaml` の `second-opinion.code-review` は `[m, l]`。本変更は L 級のため、ksn-orchestrator 側で ksn-second-opinion (code-review モード) の並走がまだなら実施対象になる。

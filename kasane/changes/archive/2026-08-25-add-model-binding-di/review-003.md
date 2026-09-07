# レビュー結果: add-model-binding-di (003 回目)

**日付**: 2026-08-25
**判定**: APPROVED

## サマリー

review-002 の Suggestion「MAUI では値型 VM が fallback 経由のインスタンス渡し show で提示まで到達しうる」に対し、オーナー裁定で採られた (a) 案 — 共通提示入口での実行時拒否 — が入った。検査は `PresentCoreAsync` の先頭、fallback を含む factory 解決・提示委譲・紐付けのいずれよりも前にあり、MAUI の全 show 経路 (インスタンス渡し / インライン / 型指定) がこの1点を通ることをコード上で確認した。退行テスト2件は「fallback が呼ばれない」「View が作られない」まで観察しており、名前だけ合わせた空テストではない。

Android 側 (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:70`) との観察可能な挙動は対称になった。例外名の非対称 (`ValueClassViewModel` / `ValueTypeViewModel`) と、レジストリ経由インスタンス渡し show の失敗種別が `ViewFactoryNotRegistered` から変わった点は deviation.md の末尾エントリに記録済みで、記述と実装は一致している。既存の解決順序 (明示 → fallback → 失敗) と terminal-path (bind/unbind の try/finally) は弱まっていない。

指摘は Minor 1件 (公開 doc comment の記述が修正前のまま) と Suggestion 1件のみで、いずれも APPROVED を妨げない。

## 今回の修正の個別確認

**全 show 経路が検査を通るか** — `IKsDialogs` の実装は `maui/KsDialogs.Maui/Presentation/Dialog.cs` の `Dialog` だけで、そこから提示に入る口は 3 つ (インスタンス渡し `:52`、インライン factory `:69`、型指定 `:156`)。3 つとも `DialogPresenter.PresentAsync` の 2 オーバーロードを経由し、両オーバーロードは `PresentCoreAsync` に集約されている (`maui/KsDialogs.Maui/Internals/DialogPresenter.cs:33`・`:63`)。`DialogPresenter` を呼ぶ箇所は他に `OnUiThreadAsync` の利用 1 箇所だけで、提示に入る別経路は無い。したがって検査 (`:83-89`) は全経路を押さえている。

**検査の位置** — `viewModelType.IsValueType` は `resolveFactory()` (`:91`) より前にあるため、View fallback resolver は呼ばれない。`DialogNotifierBindings.Bind` (`:93`) より前でもあるので、この失敗では紐付けが作られず、`finally` の除去対象も生まれない。core/ADR-0018 の「紐付けはインスタンス同一性」という前提を、fallback 構成でも破らせない位置になっている。

**コンパイル時拒否が残っているか** — `DialogViewRegistry` の公開 `Register` / `RegisterViewModel` 6 本と型指定 `ShowAsync` はすべて `where TViewModel : class` を保っている (`maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs:44`〜`:129`)。負の compile 検査 `KsDialogsNegativeCheckValueTypeViewModel` を実際に回し、期待どおり CS0452 が 2 件 (登録・型指定 show) で失敗することを確認した。実行時検査の追加でコンパイル時拒否が緩んでいない = spec MB-MA-02 の THEN は引き続き満たされる。

**Android との対称性** — 値型 / value class の VM が到達しうる入口を形態別に並べると次のとおりで、「どの show 経路でも提示に至らず、専用の構成ミス例外で失敗する」点が一致する。

| 入口 | Android | MAUI |
|---|---|---|
| 登録 | 実行時拒否 (`ValueClassViewModel`) | コンパイル拒否 (CS0452) |
| 型指定 show | VM factory 実行前に拒否 (`Dialog.kt:46`) | コンパイル拒否 (CS0452) |
| インライン factory show | 共通入口で拒否 | コンパイル拒否 (`where TViewModel : class`) |
| インスタンス渡し show | 共通入口で拒否 | **共通入口で拒否 (本修正)** |

検査の相対順序も、Android は提示先確認・factory 解決・紐付けより前、MAUI は factory 解決・紐付け・委譲より前で揃っている。

**退行確認** — 解決順序 (`DialogResolution.ResolveViewFactory` のスナップショット解決と 明示 → fallback → 失敗) は不変。`MergeFallbacks` のスロット単位合成 (review-002 で確定) も触られていない。terminal-path は `try`/`finally` の位置・`Unbind` の引数ともに不変で、値型の失敗は `Bind` より前に投げるため除去漏れの経路を作らない。`ViewModelAlreadyShowing` / `ViewFactoryNotRegistered` / `PresentationHostUnavailable` の意味と位置も変わっていない。

**範囲外の混入** — 今サイクルで書き換わったファイルは `maui/KsDialogs.Maui/Internals/DialogPresenter.cs`・`maui/KsDialogs.Maui/Contract/DialogException.cs`・`maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs`・`maui/KsDialogs.Maui.Tests/Support/ModelBindingTestTypes.cs`・`deviation.md` の 5 つ (+ review-002 直後の `maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs` の doc 追記)。他ドメイン (ios / android / kmp / samples) と足場の spec / proposal / design には手が入っていない。`DialogException.cs` の追加型はいずれも本変更のスコープ (型指定 show・並行 show・値型拒否・DI 配線) に属し、無関係な公開面の追加は無い。

**doc 追記 (review-002 Suggestion 3)** — `AddKsDialogs` の `<remarks>` に「設定済みの一括解決を公開 API から解除する手段は提供しない」の 1 文が入っている (`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:28-30`)。同じ段落の冒頭が「共有レジストリに載る」と述べているため、review-002 が求めた内容 (共有・解除不能) は満たされている。挙動は不変。

## 指摘事項

### [🟡 Minor] 公開 doc comment 2 箇所が「値型はコンパイル時にだけ弾かれる」という修正前の説明のまま

**該当箇所**: `maui/KsDialogs.Maui/Contract/DialogViewModelExtensions.cs:19-20`・`maui/KsDialogs.Maui/Presentation/IKsDialogs.cs:25-26`

**問題点**: 本修正で「値型 VM をインスタンス渡し show に渡すと `ValueTypeViewModel` で失敗する」という観察可能な挙動が公開面に増えたが、それを説明すべき 2 つの doc comment が修正前の記述のまま残っている。

1. `Notifier` 拡張プロパティの `<remarks>` は「値型は…登録と型指定 show の `class` 制約で弾かれる」と書いており、拒否点を 2 つに限定している。この文だけを読むと「インスタンス渡し show なら値型でも通り、`Notifier` が常に null になる」と読める — まさに本修正が塞いだ挙動である
2. `ShowAsync<TResult>(IDialogViewModel<TResult>, …)` の `<remarks>` は構成エラーを「未登録の ViewModel 型・提示先不在」と列挙している。新しい失敗が起きるのはちょうどこのオーバーロードなのに、列挙に加わっていない

`DialogPresenter.cs` と `DialogException.cs` の doc は今回きちんと更新されているが、この 2 つは internal 型 / 例外型側の説明であり、利用者が最初に読む VM 契約面と show 契約面が取り残された形になっている。Android では対応する記述 (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewModel.kt:14-18`) が「登録の時点と、インライン factory を含むすべての show の提示時に `DialogException.ValueClassViewModel` として拒否する」と更新済みで、公開契約の説明という観点では対称化がまだ片側に届いていない。一般公開予定のライブラリで、公開 API の doc は契約そのものとして読まれる。

**推奨修正**: 挙動の変更は不要。(1) `Notifier` の `<remarks>` を「登録と型指定 show は `class` 制約で、インスタンス渡し show は提示の入口の実行時検査で拒否する」旨へ、(2) `ShowAsync` の構成エラーの列挙に値型 VM (`DialogException.ValueTypeViewModel`) を加える。MAUI の `IDialogViewModel` 自体に Android の KDoc と同じ「参照型限定とその強制手段」の説明を置くかは任意。

**追記 (2026-08-25・修正後確認)**: **解消済み**。両方とも入っているのを確認した。

1. `maui/KsDialogs.Maui/Contract/DialogViewModelExtensions.cs:19-20` — 「登録と型指定 show の `class` 制約で拒否され、残る show 経路でも提示前の実行時検査で弾かれる」に更新されている。拒否点を 2 つに限定していた記述は消え、「インスタンス渡し show なら値型でも通る」とは読めなくなった。推奨した文言より一般化して「残る show 経路」としているが、実行時検査は `PresentCoreAsync` の先頭にあり全 show 経路が通るため記述は正しい (インライン factory show は加えて `where TViewModel : class` でもコンパイル拒否されるので、実行時検査に到達するのは実際にはインスタンス渡し経路のみ — 記述はその上位集合として真)
2. `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs:25` — 構成エラーの列挙が「未登録の ViewModel 型・値型の ViewModel・提示先不在」になり、値型 VM が加わった。直後の行が `<see cref="DialogException"/>` で失敗の運び方を述べているため、型名を明示しない列挙でも契約としては読める

残るスコープ (`IDialogViewModel` への参照型限定の説明) は当初から任意扱いで、未対応でも指摘は解消と判断する。値型に関する他の公開 doc (`DialogException.ValueTypeViewModel` の `<remarks>`・負の compile 検査の doc) を grep で洗ったが、「コンパイル時にだけ弾かれる」と読める記述の残りは無い。挙動への影響なし (doc comment のみ)。

---

### [🔵 Suggestion] 同じメソッド内で `viewModel.GetType()` を 2 度引いている

**該当箇所**: `maui/KsDialogs.Maui/Internals/DialogPresenter.cs:83`・`:96`

**問題点**: 本修正で `PresentCoreAsync` の先頭に `Type viewModelType = viewModel.GetType();` が入った結果、`ViewModelAlreadyShowing` を投げる側 (`:96`) の `DialogResolution.TypeName(viewModel.GetType())` が同じ値を取り直す形になった。実害は無い (失敗経路の 1 回だけ) が、直上で束縛したローカルを使わない書き方が同一メソッド内に並んでいる。

**推奨修正**: `:96` を `DialogResolution.TypeName(viewModelType)` に揃える。任意。

**追記 (2026-08-25・修正後確認)**: **解消済み**。`maui/KsDialogs.Maui/Internals/DialogPresenter.cs:95-96` は `DialogResolution.TypeName(viewModelType)` になり、`:83` で束縛したローカルを再利用する形に揃った。`viewModel` は `PresentCoreAsync` 内で再代入されないため `viewModelType` と `viewModel.GetType()` は同値で、例外メッセージ・型名ともに変化なし (`ViewModelAlreadyShowing` の生成箇所以外に手は入っていない)。同メソッド内に `viewModel.GetType()` の呼び出しは残っていない (残る 1 箇所は `PresentAsync` のレジストリ解決側 `:36` で、別メソッドのローカル)。挙動への影響なし。

## 確認した観点 (実測)

**ビルドとテスト** — 差分が触れたルートと、対称性の相手として参照した Android を自分で回した:

| ルート | コマンド | 結果 |
|---|---|---|
| maui/ | `dotnet test` | 88 tests / 0 failures |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 15 tests / 0 failures |
| android/ | `./gradlew test --rerun-tasks` | 66 tests / 0 failures |

ios / kmp は今サイクルで 1 ファイルも変更されていないため再実行していない (review-002 実測の ios 158 / kmp 57 が有効)。

**負の compile 検査** — `dotnet build KsDialogs.Maui.ApiSurfaceCheck -p:KsDialogsNegativeCheckValueTypeViewModel=true` が期待どおり CS0452 × 2 で失敗することを確認 (成功したら検査失敗、という向きの検査である点を含めて確認済み)。

**横断検査** — `python3 scripts/scenario-id-coverage.py` は「未網羅なし」(91/97、除外 6 = MB-SM / PB-SM)。`local-path-lint.py` / `identity-lint.py` は違反 0 件、`comment-policy-lint.py` は 599 ファイル検査で禁止 0 件。

**足場の凍結** — `git diff HEAD -- kasane/changes/add-model-binding-di/` の差分は `tasks.md` (チェックのみ) と `ui/brief.md` (照合結果と、オーナー判断で解決した `›` の記録) の 2 ファイルのみ。proposal / design / specs は無改変。`deviation.md` は未追跡のため diff に出ないが、末尾 2 エントリ (MAUI 値型拒否点・iOS 既存テストの書き換え) 以外に今サイクルの改変は内容から確認できる。tasks.md にこの修正サイクルで新規タスクは足されておらず、虚偽チェックも見つからなかった。

**新規テストの実質** — 追加された 2 件は Scenario ID を持たない (spec に無い、オーナー裁定由来の追加挙動のため正しい扱い)。`TheValueTypeViewModelIsRejectedEvenWhenTheViewFallbackCanResolveIt` は fallback の呼び出し回数 0 と `CreatedViews` の空を同時に見ており、「解決に進まない」「提示に至らない」を分けて観察している。`TheValueTypeViewModelFailsTheSameWayWithoutAnyFallback` は fallback 無し構成での対照で、修正前は `ViewFactoryNotRegistered` になっていた経路が `ValueTypeViewModel` に変わったことを固定する。テスト用の値型 VM (`maui/KsDialogs.Maui.Tests/Support/ModelBindingTestTypes.cs:121`) は `readonly struct` で、doc に「インスタンス渡し show からのみ渡せる」理由が書かれている。

**deviation の記述と実装の一致** — 末尾エントリの主張 (共通提示入口での実行時検査・全 show 経路への拡大・`DialogException.ValueTypeViewModel` の公開面追加・fallback 未設定時の失敗種別の変化・例外名の言語別語彙による非対称) はいずれもコードで裏が取れた。dialog-contract spec が「強制手段は形態別 (…C#: class 制約…、各形態 spec 参照)」と maui-binding spec へ委譲している構造なので、差分の受け皿を maui-binding spec に対する 1 エントリに集約した扱いは妥当と判断した。

## 未解決 (引き継ぎ — 本レビューでは扱わない)

review-002 から持ち越しの項目は変わらず残っている。いずれも APPROVED を妨げない:

- `ActivatorUtilities` の生成失敗が `DialogException` に包まれない (review-001 Suggestion)
- `verification/` が ksn-core のディレクトリ規約に無い置き場 (蒸留フェーズの判断)
- concepts の追随 — `kasane/concepts/cross/conventions/sample-parity.md` のデモ項目表、`kasane/concepts/cross/conventions/test-execution.md` の件数 (本レビュー実測: maui 88 / bridge 15 / android 66) と「負の検査 27 本」の本数 (本変更で増えている)。加えて、値型 / value class VM の拒否点が形態別にどう強制されるかは長命の公開契約なので、`kasane/concepts/core/api/` への 1 行追随を蒸留時に検討する価値がある

## アクションプラン

優先度順。いずれも APPROVED を妨げるものではない。

1. `Notifier` と `ShowAsync` (インスタンス渡し) の doc comment を、実行時拒否が入った後の実態に合わせる — archive 前に直すのが安い (挙動不変)
2. `DialogPresenter.cs:96` のローカル再利用 (任意)
3. 蒸留フェーズへの引き継ぎ: concepts の件数・本数の追随と、参照型限定の強制手段の形態別表の記録

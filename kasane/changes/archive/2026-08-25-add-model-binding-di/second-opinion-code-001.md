# セカンドオピニオン: add-model-binding-di (code-001)
**相方**: codex / **label**: so-code-add-model-binding-di / **日付**: 2026-08-25 / **対象**: コミット 5a2c0c3 に対する作業ツリーの全変更 (4形態 + samples + scripts)

---

## 指摘

Critical はありません。

### Major 1 — KMP/iOS の notifier 型検証が表示時スナップショットに従わない

- 該当箇所: [ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:156](ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:156)
- 問題点: 表示中 VM の結果型を、表示開始時に解決した factory ではなく「現在の registry」から取得しています。表示中に同じ VM 型を別の結果型で再登録すると、既存ダイアログの正しい notifier 取得が失敗するか、新しい型の notifier が型消去チャネルへ接続されます。後者では誤った型の値を報告でき、ダイアログ終了後の復元時まで不一致が検出されません。show 時のスナップショット契約と型安全性に反します。
- 推奨修正: `DialogNotifierBindings.Binding` に表示時 factory の `declaredResultType` も保持し、表示中は binding 側の型で検証してください。非表示時だけ registry を参照すれば、現在の「show 前でも型不一致を報告する」契約を維持できます。表示中の再登録を含む回帰テストも追加してください。

### Major 2 — MAUI の既存 TView サービス登録が実際の表示では無視される

- 該当箇所: [maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:94](maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:94)、[同:122](maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:122)
- 問題点: `TryAddTransient<TView>()` は既存 descriptor を上書きしませんが、表示時には `GetRequiredService<TView>()` を使わず、常に `ActivatorUtilities.CreateInstance` で新規生成しています。そのため、利用者が登録した TView の factory、singleton/scoped lifetime、装飾済みインスタンスは一切使われません。仕様・design の「既存登録を尊重する」と、116行目の「通常の DI 解決で作る」という説明にも反します。
- 推奨修正: 現在 VM の明示注入と既存 TView 登録の双方が成立する dialog-specific factory を設けるか、既存 descriptor がある場合の解決規則を定義して実際に provider 経由で解決してください。既存 singleton/factory を事前登録し、表示された View の同一性と呼出回数を確認するテストが必要です。

### Major 3 — 複数の AddKsDialogs が fallback を後勝ち・null 上書きする

- 該当箇所: [maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:33](maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:33)
- 問題点: 呼び出すたびに新しい空の `KsDialogsOptions` を作り、`DialogViewRegistry.Shared` を即座に置換しています。したがって、設定付き `AddKsDialogs` の後に引数なしの `AddKsDialogs()` を呼ぶだけで fallback が消えます。View fallback と VM fallback を別々の呼び出しで設定した場合も後の呼び出しが他方を null に戻します。ADR-0005 が構造的に排除するとした「後勝ち・null 上書き」を再導入しています。既存テストは initializer 数しか検証しておらず、この消失を検出しません。
- 推奨修正: options を `IServiceCollection` に保持して初期化時に一度だけ適用し、再呼び出しを no-op、非 null スロットの合成、または競合エラーのいずれかにしてください。少なくとも「設定付き → 引数なし」と「View/VM を別々に設定」のテストを追加してください。

### Major 4 — Android の inline show で value class VM の拒否を迂回できる

- 該当箇所: [android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:29](android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt:29)、[DialogPresenter.kt:49](android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:49)
- 問題点: 登録と型指定 show では `requireReferenceTypeViewModel()` を呼びますが、inline factory の show は検査せず、そのまま同一性サイドテーブルへ bind します。`value class : DialogViewModel<R>` はこの公開 API をコンパイルできるため、「全 show 経路で参照型限定」という dialog-contract/ADR-0018 を迂回できます。boxing によって `vm.notifier` の同一性保証も成立しません。
- 推奨修正: factory 解決方式に依存しない共通の提示入口で value class を検査し、提示先確認や bind より前に `ValueClassViewModel` を投げてください。MB-AN-03 に inline show 経路を追加してください。

### Minor 1 — MB-TS-02 の正常系が毎回タイムアウトを待つ

- 該当箇所: [android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogTypedShowTests.kt:70](android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogTypedShowTests.kt:70)、[ios/Tests/KsDialogsTests/DialogTypedShowTests.swift:62](ios/Tests/KsDialogsTests/DialogTypedShowTests.swift:62)
- 問題点: 発生してはいけない `creationCount > 0` を既定5秒で待ち、戻り値を捨てています。正しい実装ほど両テストで合計約10秒の待機が発生します。
- 推奨修正: configure 開始を通知する別の gate を用意し、開始を待ってから `creationCount == 0` を即時確認してください。

### Minor 2 — deviation.md の末尾が別項目と連結されている

- 該当箇所: [kasane/changes/add-model-binding-di/deviation.md:19](kasane/changes/add-model-binding-di/deviation.md:19)
- 問題点: scenario coverage の付随修正の末尾に、既存テスト書き換えの説明が区切りなく連結されています。合意内容そのものではなく、監査時にどの乖離へ属する記録か判別しにくい状態です。
- 推奨修正: 重複部分を削除するか、独立した箇条書きへ分離してください。

## 総評・判定

サイドテーブルの終端 cleanup、2スロット構成、UI スレッド実行、サンプル4ルートの対応は概ね一貫しています。一方、結果型スナップショットと DI 構成の意味論に利用者影響のある問題が残っています。MAUI DI は、`TryAdd` の有無だけでなく、表示時に既存登録と lifetime が実際に利用されるかまで含めて評価しました。

制約に従いビルド・テストは実行せず、提示された全ルートグリーンの結果を前提としました。ファイルへの書き込みも行っていません。

**判定: CHANGES_REQUESTED**

---

## 突き合わせ結果 (ksn-orchestrator, 2026-08-25)

ホスト側 review-001.md (APPROVED / Minor 3・Suggestion 4) との突き合わせ。

| 相方の指摘 | 対応するホスト側指摘 | 採否 | 確定重要度 | 根拠 |
|---|---|---|---|---|
| Major 3 (AddKsDialogs 再呼び出しの fallback 後勝ち・null 上書き) | Minor 1 (同一事象) | **確定** | **Major** (高い方) | maui/ADR-0005 が構造的に排除するとした粗の再導入。実害シナリオ具体的 |
| Major 4 (Android インライン show の value class 検査迂回) | Suggestion (同一事象) | **確定** | **Major** (高い方) | dialog-contract / core/ADR-0018 の参照型限定は全 show 経路に及ぶ。サイドテーブルの同一性保証が boxing で崩れる |
| Major 1 (KMP notifier アクセサの型検証が現在レジストリ参照) | なし (相方のみ) | **採用** | **Major** | design Decision 3「show 時の解決は呼び出し時点のスナップショット」に反する。表示中の再登録で誤型 notifier が成立する具体的シナリオあり |
| Major 2 (TryAdd した TView 登録が表示時に使われない) | Minor 2 (同一事象・仕様違反ではないと実測) | **降格** (Minor で確定) | Minor | spec (maui-binding「1行登録」) は「TView の生成は現在の VM インスタンスを明示引数に渡して行う」と明記しており、ActivatorUtilities 生成は仕様どおり。相方の「既存登録を尊重に反する」は TryAdd (サービス登録) の節の誤読。doc comment が実挙動より広く読める点のみ修正対象 |
| Minor 1 (MB-TS-02 が毎回タイムアウト待ち) | なし (相方のみ) | **採用** | Minor | 該当箇所特定・実害 (テスト毎回 約10秒の無駄待ち) 具体的 |
| Minor 2 (deviation.md 末尾の連結) | Minor 3 (同一事象) | **確定** | Minor | 記録の監査性。オーケストレーターが直接修正済み |

**統合判定: CHANGES_REQUESTED** — Major 3件 (fallback 上書き / value class 迂回 / KMP 型検証スナップショット) + Minor 3件 (doc comment / MB-TS-02 待ち方 / deviation 整形済み) を修正サイクルへ。

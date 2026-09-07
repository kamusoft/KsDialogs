# kmp-facade デルタ (localize-dialog-error-messages)

KMP 共有コードの `DialogException` は自前の文言を持たず Native の失敗の説明を素通しする (この構造は変えない)。本デルタは、iOS ホスト側 gateway が自前で持つ失敗メッセージ定数 3 本を英語固定にし (cross/ADR-0015)、Native の文言変更 (ios-native / android-native デルタ) が素通しで届くことを固定する。実現経路: `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt` の `MISSING_RESULT_MESSAGE` / `UNKNOWN_FAILURE_MESSAGE`、`IosLoadingGateway.kt` の `UNKNOWN_FAILURE_MESSAGE` の置き換え。文言に依存する既存テスト (`androidHostTest/.../AndroidDialogGatewayContractTests.kt` の完全一致 1 件、`iosTest/.../InteropBridgeContractTests.kt` の部分一致 3 件 — うち 2 件は互換面の結果を直接検査、1 件は共有コードの `DialogException` を検査) は英語文言へ追随する。文言そのものは互換契約ではない (完全一致・部分一致の Scenario は今回の置き換えの受け入れ基準)。

## ADDED Requirements

### Requirement: KMP iOS ホストの gateway 固有メッセージは英語固定

iOS ホスト側 gateway が Native の説明を得られない場合に `DialogException` の `message` として使う定数は、次の英語文言とする (SHALL)。

| 定数 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `IosDialogGateway.MISSING_RESULT_MESSAGE` | `ダイアログの結果が届きませんでした。` | `No Dialog result was delivered.` |
| `IosDialogGateway.UNKNOWN_FAILURE_MESSAGE` | `ダイアログの表示に失敗しました。` | `Failed to show the Dialog.` |
| `IosLoadingGateway.UNKNOWN_FAILURE_MESSAGE` | `ローディングの表示に失敗しました。` | `Failed to show the Loading.` |

#### Scenario: [DM-KM-01] iOS 互換面の結果に Native の英語文言が載る
- **GIVEN** iOS ホストで View factory 未登録の ViewModel と、提示先の無い状態で登録済みの ViewModel
- **WHEN** それぞれ互換面 (bridge) 経由で show を呼び、失敗の結果を受け取る
- **THEN** 結果の `error.localizedDescription` が、ios-native デルタの英語文言 (`No View factory is registered for ViewModel type` / `No screen is available to present the Dialog.`) をそれぞれ含む

#### Scenario: [DM-KM-03] iOS ホストで共有コードの DialogException に Native の英語文言が素通しで届く
- **GIVEN** iOS ホストで View factory 未登録の ViewModel と、提示先の無い状態で登録済みの ViewModel
- **WHEN** それぞれ `Dialog.instance.show` を呼ぶ
- **THEN** 投げられた共有コードの `DialogException` の `message` が、上と同じ英語文言をそれぞれ含む (未登録・提示先なしの両経路で message を検証する)

#### Scenario: [DM-KM-04] KMP 共有コードのメイン側に日本語の文字列リテラルが残らない
- **GIVEN** `kmp/ksdialogs-kmp/src/{commonMain,androidMain,iosMain}/` 配下の Kotlin ソース (コメント行を除く)
- **WHEN** 日本語を含む文字列リテラルを検索する
- **THEN** 該当行は 0 件で、上表の英語文言が 3 定数のリテラルと一致している (静的 grep とレビューで受け入れる Scenario。`scripts/scenario-id-coverage.py` の除外表に理由付きで登録する。Native の説明が得られない経路は既存テストに器が無いため自動テストの対象にしない)

### Requirement: KMP Android ホストは Native の英語文言を素通しする

Android ホスト側 gateway は Native の `DialogException` の `message` を共有コードの `DialogException` の `message` にそのまま転記する (SHALL、現行どおり)。

#### Scenario: [DM-KM-02] Android ホストで Native の英語文言が DialogException に届く
- **GIVEN** Android ホストで提示先の無い状態で登録済みの ViewModel
- **WHEN** `Dialog.instance.show` を呼ぶ
- **THEN** `DialogException` の `message` が `No screen is available to present the Dialog.` と完全一致し、`cause` が Native の `DialogException.PresentationHostUnavailable` である

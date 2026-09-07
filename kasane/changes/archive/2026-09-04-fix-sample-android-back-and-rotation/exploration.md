# Exploration: fix-sample-android-back-and-rotation

2026-09-02 に簡易起票 2 件 (`fix-sample-android-gesture-back-lint` / `keep-sample-dialog-across-rotation`) を統合。どちらも Android Sample の `MainActivity` (と KMP androidApp の同名ファイル) に閉じ、「ダイアログ表示中に OS 操作 (戻る / 回転) をしたときの挙動」を実機で確認する場面が同じため、検証を 1 回にまとめる目的で 1 change に束ねた。

## 課題 / 動機

### 1. 戻るジェスチャーの lint 失敗 (旧 fix-sample-android-gesture-back-lint)

Android サンプル (`samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt` の `onBackPressed` オーバーライド付近) が Android lint の `GestureBackNavigation` 指摘で `:app:lintDebug` を error 失敗させる。add-loading の Custom Loading デモ実装 (6.2) 中に発見した既存事項。予測型戻りジェスチャー (predictive back) への API 移行指摘であり、`OnBackPressedCallback` 等への置き換えという挙動変更を伴うため、サンプルの戻る挙動 (ダイアログ表示中の戻るの扱い) を確認しながら移行する必要がある。

kmp-android (`samples/kmp/androidApp/.../MainActivity.kt`) の `onBackPressed` も同じ deprecation 警告を出しており、対応対象に含める (2026-08-27 add-sample-capture-automation の実装中に確認、オーナー指示で追記)。

### 2. 回転時にダイアログが閉じ、iOS Sample と見え方が揃わない (旧 keep-sample-dialog-across-rotation)

Android Sample (`samples/android`) の `MainActivity` に `android:configChanges` が無いため、**ダイアログ表示中に端末を回転させると Activity が再生成され、ダイアログは器消失 (cancelled) で閉じる**。ライブラリの契約 (OS 発の器消失 → cancelled、core/api/result-notification-semantics.md / multi-display-semantics.md) どおりの挙動で不具合ではないが、iOS Sample では回転後もダイアログが残って新しい寸法で再配置される (PB-WN-01 の実 Sample 裏取り) ため、**4ルートの Sample で「回転したときの見え方」が揃っていない**。

発見の文脈: add-presentation-behavior の tasks 1.4 (PB-WN-01〜03 の実機裏取り) で、Android の回転後スクリーンショットを撮ろうとしたところダイアログが閉じていた (`kasane/changes/archive/2026-08-22-add-presentation-behavior/verification/window-change-android/` の `03-landscape-after-rotation.png` と README。裏取りは回転後に出し直して実施)。MAUI Android / KMP androidApp の Sample も同じ構成かは未確認。

## 探索で確認した現状 (2026-09-04、コードから)

- `samples/android` と `samples/kmp/androidApp` の `MainActivity` はどちらも素の `android.app.Activity` を継承し、AndroidX の activity ライブラリに依存していない (依存は ksdialogs / ksdialogs-compose / Compose foundation / coroutines のみ)。targetSdk は 36 (`android/gradle/libs.versions.toml` の `android-compileSdk`)
- **Android 16 (API 36) を targetSdk にすると予測型戻りが既定で有効になり、`onBackPressed` が呼ばれなくなる**。したがって API 36 端末 (Pixel 6a) では「パネルを開いている間の戻る → メニューへ」の導線がすでに効いていない可能性が高い (コード上の推定。実装時に実機で確認する)。lint 指摘は実害の予告として扱う
- ダイアログ表示中の戻るは本体の器 (`android/ksdialogs/.../DialogContainer.kt` の `setCancelable(true)`) が受けて cancelled にするため、Sample の `onBackPressed` は関与しない。4ルートの「ダイアログ表示中の戻る」パリティには影響しない
- 回転: iOS Sample と MAUI Android Sample (`samples/maui/.../Platforms/Android/MainActivity.cs` の MAUI テンプレート既定 `ConfigurationChanges = ScreenSize | Orientation | UiMode | ScreenLayout | SmallestScreenSize | Density`) は回転後もダイアログが残り、Kotlin の 2 ルートだけ閉じる。**4ルートが 2 対 2 で割れている**
- lint baseline ファイルは両ルートとも存在しない

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: 戻るの移行方針

| 案 | Android 16 でパネルの戻るが効く | 依存の追加 | コードの形 | lint |
|---|---|---|---|---|
| A. OS 標準の戻り受け口 (`OnBackInvokedDispatcher`、API 33+) + 旧経路 (`onBackPressed`) の併存 | 効く | なし | API 分岐が 2 経路残り、旧経路も残る | 旧経路が残るため指摘が残る可能性 |
| **B. AndroidX の戻り受け口 (`ComponentActivity` + `OnBackPressedCallback`)** | 効く | `androidx.activity` を 2 ルートに追加 | 1 経路。基底クラスが変わる | 解消 |
| C. lint baseline で抑制 | 効かないまま | なし | 変更なし | 抑制のみ |

- 却下 A: API 33 以上と 24〜32 の 2 経路を Sample が自前で持つことになり、旧経路が残る分 lint 指摘も残り得る
- 却下 C: Android 16 での実害 (パネルの戻る導線が効かない) を放置することになる

### 論点 2: 回転時の見え方を揃えるか

| 案 | 4ルートの見え方 | 付随作業 |
|---|---|---|
| **A. Kotlin 2 ルートの Manifest に MAUI と同じ `configChanges` の集合を付け、「回転してもダイアログが残る」に揃える** | 4ルート一致 | sample-parity に「OS 操作 (回転) への反応」を一文足す (蒸留で handbook 更新)。PB-WN-01 の Android 実 Sample 証跡を撮り直せる |
| B. 揃えない (課題 1 だけに縮める) | 2 対 2 のまま | sample-parity にルート差として明記が必要 |
| C. 「閉じる」側に揃える | 成立しない | iOS は Activity 再生成に相当する仕組みがなく閉じられない |

- 却下 B: Sample は「4ルートを並べて差を見る装置」であり、OS 操作への反応が割れていると装置として弱い。ルート差の明記も別途要る
- 却下 C: iOS 側で実現できない
- 補足: MAUI 利用者は既定でこの構成のため、揃えても利用者の典型から外れない

## 決定事項

- **論点 1 = 案 B** (オーナー承認 2026-09-04): `samples/android` / `samples/kmp/androidApp` の `MainActivity` を `ComponentActivity` 継承に変え、パネルを開いている間の戻るを `OnBackPressedCallback` で受ける。`androidx.activity` を両ルートの依存に追加。`onBackPressed` オーバーライドは撤去
- **論点 2 = 案 A** (オーナー承認 2026-09-04): 両ルートの `AndroidManifest.xml` の `MainActivity` に MAUI Android Sample と同じ集合 (`orientation|screenSize|screenLayout|smallestScreenSize|uiMode|density`) の `android:configChanges` を付け、4ルートとも「回転してもダイアログが残り、新しい寸法で再配置される」に揃える
- 両決定とも Sample に閉じた可逆な選択のため ADR にはしない (探索メモのみ)
- **依存の置き場** (オーナー承認 2026-09-04、実装後): Sample 専用の version catalog が無く両 Sample の settings が `android/gradle/libs.versions.toml` を共有する構成のため、`androidx-activity` の版はこの共有カタログに Sample 専用と注記して置く。本体の依存グラフには入らない (review-001 で解決グラフを実測)。Sample 側に専用カタログを切る案は見送り
- 実機確認: 「ダイアログを出したまま戻る / 回転」と「パネルを開いたまま戻る」を android / kmp-android の 2 ルートで 1 セッションにまとめる。API 36 端末と API 33 端末の両方で戻るを確認する (予測型と旧経路の両方を踏む)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 実装時に確認: `ComponentActivity` へ変えたことで本体の Activity 追跡 (`KsDialogsInstaller` の lifecycle callbacks) や Compose 登録経路に影響が出ないこと (基底クラス非依存のはずだが実機で踏む)
- 実装時に確認: `configChanges` 付与後、Sample の View 側 (メニュー・パネル) が回転で再レイアウトされること。崩れるなら `onConfigurationChanged` の最小対応が要るか
- 蒸留時: sample-parity (`kasane/handbook/cross/sample-parity.md`) の一致対象に「OS 操作 (戻る・回転) への反応」を足す文言。`samples/README.md` の写像追随も

### 起票時の疑問点 (探索で解消済み、記録として残す)

課題 1 (戻る):
- 4ルートのサンプルパリティ (戻る操作の挙動一致) に影響するか
- lint baseline で一時抑制するか、API 移行まで一気に行うか

課題 2 (回転):
- Sample として「回転してもダイアログが残る」を見せたいのか (利用者アプリの典型は `configChanges` なしで Activity 再生成 → ダイアログが閉じるのも一つの現実)。handbook/cross/sample-parity.md の「一致の単位はデモ項目」に回転時の挙動は含まれるか
- `android:configChanges="orientation|screenSize|screenLayout"` を Sample の Activity に付けるだけで済むか。付けた場合、Compose / View の再レイアウトでダイアログの再配置 (毎レイアウトパス再計算) が実 Sample で観察できるか (PB-WN-01 の Android 実 Sample 証跡を取り直せる)
- MAUI Android (`samples/maui`、MAUI の `MainActivity` は既定で `ConfigurationChanges` を持つ) と KMP androidApp (`samples/kmp/androidApp`) の現状確認
- 本体側の論点ではない (Activity 再生成でダイアログが閉じるのは契約どおり) — Sample だけの変更に収まるか

統合に伴う論点:
- 課題 2 が「揃えない (やらない)」判定になっても課題 1 は単独で成立する。その場合はこの change を課題 1 だけに縮める
- 実機確認は「ダイアログを出した状態で戻る / 回転」を 1 セッションで済ませる (android / kmp-android の 2 ルート)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (確定候補)

触るのは 2 ルートの `MainActivity.kt` / `AndroidManifest.xml` / `build.gradle.kts` (依存追加) だけで、本体・公開 API には触れない。可逆で UI の見た目変更もない。独立レビューは必須。sample-parity への追記は蒸留で扱う。実機確認 (2 端末 × 2 ルート) を伴うため、正解が実物を見ないと分からない調整型ではなく、直接実装 (Plan モード + 実機確認) で進める。

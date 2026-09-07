# Exploration: fix-kmp-ios-unhandled-exception-crash

## 課題 / 動機

KMP iOS Sample (`jp.kamusoft.ksdialogs.samples.kmp.ios`) が散発的にクラッシュする。オーナーも度々観測しており、2026-08-27 の add-sample-capture-automation 検証中だけで3件発生 (13:30 / 13:51 / 15:06)。15:06 の1件は `--demo basic-dialog` 付きコールド起動の直後に発生し、同条件の直後の再起動では再現しなかった (散発性)。

3件ともクラッシュ署名は**完全に同一**:

- `Abort trap: 6` (SIGABRT)、faulting thread = 0 (main)
- `kotlin::ProcessUnhandledException` → `terminateWithUnhandledException` — **Kotlin/Native の未処理例外**による abort
- 直前のフレームが `Kotlin_ObjCExport_ExceptionAsNSError` → `Kotlin_ObjCExport_runCompletionFailure` → `createContinuationArgumentFromCallback` / `BaseContinuationImpl.invokeSuspend` — **ObjC export された suspend 関数の completion (Swift 側から呼ばれる) に失敗結果を渡す経路**で、Kotlin 例外が NSError 変換の途中に未処理のまま runtime に到達している

サニタイズ済みのクラッシュログ3件 (uuid をプレースホルダ化) を `evidence/` に添付。

## 調査結果 (2026-09-02 探索)

### 直接原因 (確度: 高)

スタックの再開連鎖は `IosDialogGateway.present` → `GatewayKsDialogs.show` → **`SamplePresenter` の suspend 関数** → ObjC export の completion。abort しているのは `Kotlin_ObjCExport_ExceptionAsNSError` で、これは「投げられた例外の型が、export された関数の `@Throws` に列挙されていない」ときに Kotlin/Native が未処理例外として終了させる経路。

- ライブラリ側の `KsDialogs.show` (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt:28`) には `@Throws(DialogException::class, CancellationException::class)` があり、ライブラリだけを Swift から直接呼ぶ経路なら NSError で届く
- **Sample 共有コードの `SamplePresenter` (`samples/kmp/shared/.../SamplePresenter.kt`) の suspend 関数 10 本には `@Throws` が無い**。Swift 側 (`SampleMenuModel.swift`) は `try await presenter.showBasicDialog()` を `catch` で受ける前提だが、`@Throws` 無しの suspend 関数では CancellationException 以外の例外は Swift に届く前に runtime が abort する
- gateway 内で作られる失敗はすべて `DialogException` (`InteropDialogShowSurface.outcomeOf` / nil 結果) で、ライブラリの `@Throws` には含まれる。つまり **ライブラリの契約 (kmp/ADR-0001・result-notification-semantics 基本ルール 5「KMP→Swift 境界では NSError で届く」) はライブラリの公開面では守られており、Sample が自分の export 面で同じ宣言を欠いていた**

### 根底の失敗 (確度: 中 — 例外メッセージが abort で失われているため未確定)

Kotlin 側に届いた失敗は Swift Native の `DialogPresenter.present` が投げた `DialogError` のいずれか。7 種のうち起動直後に出やすいのは `presentationHostUnavailable` (`UIKitDialogPresentationSurface.canPresent` = `topmostViewController() != nil` が false)。

- 3 件とも add-sample-capture-automation の検証中 = `--demo` 付きコールド起動の直後で、15:06 の件は起動直後と明記されている
- Sample の `SampleMenuScreen.autoPlay()` は「初回表示と同じターンで提示を取りこぼした」対策として `await Task.yield()` を 1 回挟んでいるが、window が key になる前に `show` へ到達する余地が残る (散発性と整合)
- 代替候補は `viewModelAlreadyShowing` (自動再生とタップの重複) だが、消費済みガード (`consumeDemo`) があるため可能性は低い

### 再現の試行 (2026-09-02、iPhone 17 Pro Simulator / iOS 26.5、main 当日ビルド)

`--demo basic-dialog` のコールド起動を 25 回試したが、いずれも落ちず自動再生の Basic Dialog は正常に表示された。

| 条件 | 回数 | 結果 |
|---|---|---|
| terminate → launch (通常) | 10 | 全件正常 |
| 同上 + CPU 負荷 (`yes` 6 プロセス並走) | 10 | 全件正常 |
| Simulator shutdown → boot → 即 launch | 5 | 全件正常 |

- 元の 3 件と同じ条件 (撮影自動化の並走・当時のホスト負荷) は再現できておらず、自然再現は未達
- インストール済みだった旧ビルド (8/21) は `--demo` 非対応で、再現条件を満たしていなかったため main から再ビルドして実施
- 機構の確認 (「`@Throws` 無しの suspend 関数から `DialogException` が抜けると同じスタックで abort する」) は、提示先不在を一時的に強制する方法で決定的に行える (未実施)

### 機構の確認 (2026-09-02、強制失敗による決定的確認)

iOS ライブラリの提示判定に起動引数で提示先不在を強制する一時変更 (コミットせず revert 済み) を入れ、`--demo basic-dialog` で 2 段階を確認した。

| 段階 | Sample の状態 | 結果 |
|---|---|---|
| 1 | `@Throws` 無し (元のコード) | SIGABRT。スタックは evidence の 3 件と **35/35 フレーム一致**。stderr に Kotlin/Native の「Exception doesn't match @Throws-specified class list ... Program will be terminated.」と `Uncaught Kotlin exception: DialogException: ダイアログを提示できる画面がありません。` |
| 2 | `SamplePresenter` の suspend 全件に `@Throws(DialogException::class, CancellationException::class)` | abort せず Swift の catch に NSError (`Domain=KotlinException`, 同メッセージ) が届き、Sample 設計どおり Debug の `assertionFailure` で停止 |

- 直接原因 = Sample の export 面の `@Throws` 欠落: **確定**
- 根底の失敗 = 提示先不在 (`presentationHostUnavailable`) であること: 元の 3 件については自然再現できていないため状況証拠のまま。ただし起動直後の自動再生で唯一起こりうる `DialogError` であることは変わらない
- 段階 1 のクラッシュログ (サニタイズ済み) を evidence/ に保存: `KsDialogsSampleKmp-2026-09-02-143530.forced-host-unavailable.sanitized.ips`

### 提示先待ちの実測 (2026-09-02、review-001 Minor 2 への対応)

Sample の待ちループに一時ログ (待った回数。コミットせず除去済み) を入れ、`--demo basic-dialog` のコールド起動で「`.task` 開始時点で提示先 (前面でアクティブなシーンの key window) があったか」を実測した。

| 条件 | 回数 | `.task` 開始時点で提示先あり | 50ms 後に提示先あり |
|---|---|---|---|
| terminate → launch (通常) | 10 | 0 | 10 |
| 同上 + CPU 負荷 | 10 | 0 | 10 |
| Simulator shutdown → boot → 即 launch | 3 | 0 | 3 |

- 23/23 で `.task` 開始時点の提示先は不在。ライブラリの提示可否と同じ述語なので、この瞬間に show へ到達すれば `presentationHostUnavailable` になる
- 従来の `Task.yield()` 1 回は、この窓を MainActor の 1 ターンで跨げていただけで、保証ではない (ホスト負荷でターンの進みが遅れれば失敗する — 元の 3 件と整合する説明)
- 待ちを入れた状態で 23/23 正常。待ちを外した A/B (自然再現) は元々成立していないため、証跡は「待ちが覆う窓が実在すること」の実測で代える

### 切り分けの結論

- **ライブラリ本体の欠陥ではない** (契約どおり fail-fast し、`@Throws` も宣言済み)。Sample (samples/kmp) 側の 2 点の問題
  1. `SamplePresenter` の export 面に `@Throws` が無く、構成エラーが NSError にならず abort する
  2. 起動直後の自動再生が提示先の準備 (key window) を待たずに show へ到達しうる
- ただし 1 は **KMP 消費者が同じ罠を踏む**典型例 (自分の共有コードで `KsDialogs.show` を包んだ suspend 関数を Swift に出すとき)。ライブラリの利用者向け文書に注意書きを置く価値がある (phase-9-docs 候補)

## 検討した選択肢 (却下案と理由を含む)

- **Sample 修正 (推奨)**: `SamplePresenter` の suspend 関数全件に `@Throws(DialogException::class, CancellationException::class)` を付ける + 自動再生の開始を提示先の準備完了 (scene active / key window) に同期させる。Sample に閉じ、契約変更なし
- **ライブラリ側で host 準備を待つ**: 却下候補。基本ルール 5 (提示先不在は失敗で返す) と衝突し、「登録漏れが画面上で観察できないまま素通りする」を防ぐ意図を崩す
- **`SampleMenuModel` の `assertionFailure` を結果表示に変える**: 補助案。Debug では例外が届いても止まる設計なので、メッセージを結果エリアへ出すほうが撮影自動化と相性がよい

## 決定事項

- 2026-09-02: **ライブラリの公開面は据え置き、Sample 側を直す** (オーナー確定)。`SamplePresenter` の Swift へ出る suspend 関数全件に `@Throws(DialogException::class, CancellationException::class)` を付け、起動直後の自動再生を提示先の準備完了に同期させる
- 2026-09-02: 「失敗を値で返す」方向への公開面変更は本 change に含めない。必要なら別論点として立てる (契約・kmp/ADR-0001 に触れるため)
- 2026-09-02: 利用者向けの注意書き (共有コードの suspend 関数を Swift に出すときの `@Throws`) は phase-9-docs の agenda 論点として登録 (本 change では扱わない)
- ADR は起票しない: kmp/ADR-0001 (素の suspend 直接公開) の範囲内で、公開契約の変更なし
- 2026-09-02: 自動再生の提示先待ちは **ライブラリの提示可否と同じ条件** (前面シーンの key window に rootViewController がある) を Sample 側でポーリングして待つ (50ms × 最大 40 回 = 2 秒)。上限超過は待たずに進み、失敗は従来どおり結果側に任せる。`scenePhase == .active` への同期は却下 — key window の有無を直接見ないため、ライブラリの判定とずれる余地がある
- 2026-09-02: `SampleMenuModel` の catch の Debug `assertionFailure` は **現状維持**。「未登録・提示先不在は Sample の組み立ての誤りなので開発中に気づけるよう止める」という既存の意図を崩さない。提示先待ちを入れたことで、起動直後の正常な起動では発火しなくなる
- 2026-09-02: iOS Native Sample (`samples/ios`) の自動再生も同じ `Task.yield()` 1 回の構造だが、本 change は KMP に閉じる。撮影支援機構はパリティ要件の枠外 (handbook/cross/sample-parity.md) のため揃える義務はなく、揃えるなら別途扱う

## ADR 候補 (作成済み: なし / 未起票: なし)

公開契約の変更なし。ADR 級の判断は含まない見込み (Sample 修正 + 文書の注意書き)

## 未決の論点

- 元の 3 件の根底が提示先不在だったことは自然再現できていないため状況証拠のまま (強制失敗では提示先不在の `DialogException` を確認済み)。提示先待ちの導入後に撮影自動化で再発しなければ実質確定とみなす

## レビュー対応 (review-001 → 修正サイクル 1、2026-09-02)

| 指摘 | 対応 |
|---|---|
| Major 1: 待ちの述語がライブラリより弱い (`foregroundActive` で絞っていない) | 修正。`activationState == .foregroundActive` のシーンに限定し、`ApplicationKeyWindowProvider.selectKeyWindow` と同じ述語にした |
| Major 2: ライブラリの `KsLoading` / `KsToast` にも `@Throws` が無い | スコープ外としてオーナー承認のうえ簡易起票 `add-kmp-loading-toast-throws` へ |
| Minor 1: 待ちループが `try?` でキャンセルを握り潰す | 修正。`Task.sleep` の失敗 (キャンセル) で待ちを打ち切って return する |
| Minor 2: 待ちの効果に識別力のある証跡がない | 「提示先待ちの実測」節を追加。`.task` 開始時点で提示先不在が 23/23 であることを一時ログで実測 |
| Suggestion: `@MainActor` の付け方が不揃い | 修正。両方から外し、`View` 準拠による型の推論に委ねた |
| Suggestion: 決定事項と実装が同一コミット | 記録の書き換えはせず、次回以降の運用として受け止める |

review-002 (APPROVED、Minor 2 / Suggestion 3):

| 指摘 | 対応 |
|---|---|
| Minor: 非 suspend の `showCustomToast()` にも `@Throws` が無い (発火条件は Sample の組み立て誤りのみ) | オーナー判断待ち。推奨は `add-kmp-loading-toast-throws` (Toast の非 suspend `@Throws` 方針を決める change) に寄せる |
| Minor: 実測の生ログが evidence に無い / 修正前コードの `Task.yield()` 直後は未計測 | 生ログを evidence に保存。`Task.yield()` 直後の計測は未実施 (待ちの導入で経路が変わるため、追加証跡としては任意) |
| Suggestion 3 件 (述語の照合先コメント・キャンセル後の進行・対応表の書き分け) | 未実施。蒸留時の判断材料として残す |

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (Sample に閉じる・契約変更なし・公開 API 変更なし。独立レビューは必須)

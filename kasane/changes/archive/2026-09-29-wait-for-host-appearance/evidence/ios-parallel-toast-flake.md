# iOS の既定の並列実行で Toast スイートの先頭のテストが落ちる件の切り分け

deviation.md の [付随修正]「iOS の既定の並列実行で Toast スイートの先頭のテストが間欠的に落ちる」の実測と結論。

## 結論

**原因はテスト側の前提 — 期限の短い Toast (150〜400 ms) の受理が、並列実行の立ち上がりで MainActor に積まれた仕事の後ろに並び、期限より後に走ること。** ライブラリは受理が MainActor に届いた時点で期限を越えていれば中身も器も作らずに捨てる (満了した表示は表示しない。`ToastCoordinator.beginDisplay` の期限の確認、変更前の HEAD にも同じ確認がある) ので、表示は一度も取り付かず、`waitUntilPresenting` が 5 秒の待ちの上限で落ちる。ライブラリの不具合ではない。

- 待ち不足ではない: 表示は捨てられているので、待ちを延ばしても通らない
- テスト間の共有状態ではない: 落ちた回の受理はすべて「受理から MainActor で走るまでの遅れ > duration」で、遅れが duration 未満の回は落ちていない (下表)
- Simulator の複製の性質ではない: 並列はプロセス内の Swift Testing のスイート並列 (`-parallel-testing-enabled NO` で止まる側) で、多数のスイートの最初のテストが一斉に MainActor へ仕事を積む
- 「スイートの先頭のテスト」に限られるのは、立ち上がりの混み合いに当たるのが各スイートの最初のテストだから

## 実行条件

- ルート: `ios/`、`xcodebuild test -scheme KsDialogs -destination 'id=<作業専用 Simulator>'` (iPhone 17 / iOS 26.5 を `xcrun simctl create` で新規作成、作業後に削除)
- 実行機: 12 論理 CPU の手元 Mac
- 既定の並列実行 (フラグなし)。直列は `-parallel-testing-enabled NO`

## 実測 1: 修正前の再現

| 回 | 結果 | 落ちたテスト |
|---|---|---|
| 1 | 335 件中 2 件失敗 | TS-CO-01・TS-IO-02 |
| 2 | 335 件全件成功 | — |
| 3 | 335 件中 2 件失敗 | TS-IO-02・TS-TR-01 |
| 4 | 335 件全件成功 | — |

落ちる箇所はいずれも `try #require(await harness.waitUntilPresenting())`。

## 実測 2: 受理の遅れの計測 (一時的な計測コードで測った。計測コードは除去済み)

`ToastCoordinator.accept` で受理時刻を控え、受理の待ち行列から `beginDisplay` が走った時点との差 (受理の遅れ) と、その時点で期限を越えていたかを標準出力へ出した。

| 回 | 結果 | 期限切れで捨てられた受理 (遅れ / duration) |
|---|---|---|
| 計測 1〜3 | 全件成功 | なし (遅れの最大 0.24 s / 0.53 s / 0.18 s。うち duration 400 ms の受理の最大は 0.26 s) |
| 計測 4 | 1 件失敗 (TS-TR-01) | 1 件 (0.44 s / 400 ms) |
| 計測 5 | 3 件失敗 (TS-IO-02・TS-CO-01・TS-TR-01) | 3 件 (1.32 s・1.70 s・1.08 s / いずれも 400 ms) |
| 計測 6 | 全件成功 | なし |
| 計測 7 | 1 件失敗 (TS-CO-01) | 1 件 (0.41 s / 400 ms) |
| 計測 8 | 3 件失敗 (TS-TR-01・TS-CO-01・TS-IO-02) | 3 件 (1.01 s・1.12 s・1.11 s / いずれも 400 ms) |

失敗の件数と「期限切れで捨てられた受理」の件数は全回で一致する。

## 修正

テスト側で、期限の短い表示の要求を出す前に MainActor の混み合いが引くのを待つ。

- `ios/Tests/KsDialogsTests/Support/DialogTestWaiting.swift` に `awaitMainActorResponsive` を足した。MainActor の末尾に積んだ仕事が走って戻るまでの 1 往復を測り、20 ms 未満が 3 回続いたら戻る (上限 30 秒。時間切れでも失敗にはしない)
- `ios/Tests/KsDialogsTests/Support/MainActorResponsiveTrait.swift` に、各テストの本体の直前でこれを呼ぶ trait (`.awaitsMainActorResponsive`) を置いた
- Toast を実時間の期限つきで表示するスイート 10 本 (`ToastContractTests`・`ToastTransitionTests`・`ToastSwiftUIContentTests`・`ToastMultiDisplayTests`・`ToastTypedShowTests`・`ToastAttributeTests`・`ToastAccessibilityTests`・`ToastNonModalTests`・`KsToastKmpTests`・`DialogHostAppearanceTests`) の `@Suite` に付けた

テストの本体 (duration・待ちの上限・観測点) は変えていない。

## 実測 3: 修正後の計測 (一時的な計測コードつき。計測コードは除去済み)

5 回とも全件成功。期限切れで捨てられた受理は 0 件、受理の遅れの最大は 0.019〜0.049 s (duration の最小 150 ms を十分下回る)。

trait の待ちは 1 回の実行で約 50 回走り、待ちが 0.1 s を超えたのは各回 10〜15 回 (各スイートの最初のテスト、つまり立ち上がりに当たる回)。中央値は 2〜4 ms。立ち上がりでの待ちの最大は 1.1〜7.8 s、その中の 1 往復の最大は 0.47〜5.8 s — 立ち上がりでは MainActor の 1 往復が秒単位になる。

## 実測 4: 修正後の最終確認 (計測コードなし)

| 実行 | 結果 |
|---|---|
| 既定の並列 × 6 回 | 6 回とも `Test run with 335 tests in 57 suites passed` |
| 直列 (`-parallel-testing-enabled NO`) × 1 回 | `Test run with 335 tests in 57 suites passed` |

件数行はどの回も Swift Testing が `335 tests`、XCTest が `Executed 0 tests, with 0 failures`。

## MAUI の iOS ブリッジ (`maui/macios/native`)

Toast の期限切れと同じ性質の失敗は無い — ブリッジの Toast のテストは duration 60 秒で、全スイートが直列の親スイートの下にあるため立ち上がりの一斉開始も起きない。

ただし別の失敗を 1 回観測した (既定 7 回・直列 4 回の計 11 回中 1 回、既定の実行で): `MauiBridgeHostWaitTests` の「待っていた 2 枚は、提示先が現れると呼んだ順に提示され、後の 1 枚が手前に重なる」で、`viewA.isDescendant(of: first.view)` と `viewB.isDescendant(of: second.view)` の 2 つが同時に偽になった。2 枚とも提示はされている (`root.presentedViewController?.presentedViewController != nil` は成立)。提示の順が入れ替わったか、観測した時点の並びが別物だったかは切り分けていない (今回の担当の外)。

## handbook に反映すべき内容 (蒸留で扱う)

- `kasane/handbook/cross/verification-ci.md` の「手元 (12 論理 CPU 級) では既定の並列で全件が安定する」は、この修正の前は事実と食い違っていた (4 回中 2 回、計測込みで 12 回中 6 回落ちた)。修正後は並列 6 回連続で全件成功
- 同文書の見分け表は「待ち不足」と「並列スイートの飢餓」の 2 型だけを持つが、今回の落ち方は第 3 の型である: 失敗までの時間は待ちの上限のすぐ後 (待ち不足に見える) でも、待ちを延ばしても直らない — 実時間の期限を持つ表示が、受理の前に満了して捨てられている
- `kasane/handbook/cross/ci-flaky-test-policy.md` の観測の規律に並ぶ項目として「実時間の期限 (Toast の duration) を持つ表示を観察するテストは、要求の前に MainActor の混み合いが引くのを待つ (`.awaitsMainActorResponsive`)。期限は受理の時点から進むので、MainActor の遅れが期限より長いと表示が取り付かない」

# 一致検証結果: add-vertical-slice (002 回目 — 再検証)

**日付**: 2026-08-15
**判定**: **VALID**

verify-001 で ❌ とした 3 件の対処を再確認し、併せて他セルの退行を抜き取りで確認した。
全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」となり、虚偽チェックなし・逆流なし・テスト全件 green。

## verify-001 の ❌ 3 件の再判定

| # | 対象 | verify-001 | verify-002 | 根拠 |
|---|---|---|---|---|
| ❌1 | kmp-facade「復元失敗は Kotlin 例外 → NSError で届く SHALL」 | ❌ 未実装 | **✅ 実装で充足** | 互換面での宣言結果型検査を新設。iOS +3 件 / KMP +1 件のテストで実証 |
| ❌2 | kmp-facade Scenario「Swift から await した結果の型と値が正しい」 | ❌ 実証欠落 | **⚠️ deviation 記録済み** | `deviation.md:5` — phase-5 の Swift 向け KMP 面設計へ送ることを合意 |
| ❌3 | samples「Sample から本体の内部実装を直接参照しない SHALL」 | ❌ 乖離 | **⚠️ deviation 記録済み** | `deviation.md:6` — KMP iOS 消費者向け公開登録経路が未設計 (構造的ギャップ)。正式設計は phase-5 |

deviation.md には上記 2 件に加え、**MD-b の契約方針**「OS 差として明文化する」(`deviation.md:7`) が記録されている。
MD-b はデルタスペックの Scenario ではない**調査ケース**のため対応表の判定対象外だが、記録として整合している
(dialog-contract「多段表示の基本保証」の「各 show は独立に結果を返し」は、iOS でも上の show が cancelled で
ちょうど1回返るため充足したままである)。

---

## ❌1 の充足確認 (詳細)

### 実装

| 段 | 実装 | 内容 |
|---|---|---|
| 登録 | `ios/Sources/KsDialogs/Interop/KsDialogsInteropResultType.swift:11-33` (新規) | `@objc(KSDInteropDialogResultType)` — 宣言結果型を **名前 + 判定手続き (`acceptsValue:`) の組**で ObjC 面に出す |
| 〃 | `KsDialogsInteropBridge.swift:37-51` | `registerViewFactoryForViewModelClass:resultType:factory:` で登録時に宣言結果型を受け取り、show ごとの notifier へ引き渡す |
| 検査 | `KsDialogsInteropNotifier.swift:26-39` | `complete` の時点で `resultType.accepts(value)`。不一致は `KsDialogsInteropResultTypeMismatch` の印で `settle(.completed(...))` — **cancelled に化けさせない** |
| 輸送 | `KsDialogsInteropResultTypeMismatch.swift:8-18` (新規) / `KsDialogsInteropResult.swift:27-32` | 印を検出して `kind = .error` + `DialogError.resultTypeMismatch(expected:actual:)` へ振り替え |
| 変換 | `kmp/.../iosMain/IosDialogGateway.kt:50-52` | `KSDInteropDialogResultKindError` → `DialogException` |
| Swift へ | `kmp/.../commonMain/KsDialogs.kt:24-25` | `@Throws(DialogException::class, CancellationException::class)` により Swift 側へは NSError で届く |

spec 本文は「**iosMain actual** は…復元を担い、復元失敗…は Kotlin 例外 → NSError 変換で Swift 側に届く SHALL」。
実装は**検査の位置を互換面 (iOS 側) に置き、例外化を iosMain が担う**構成になっている。
ジェネリクスを ObjC 境界へ出せないという design Decision 12 の制約下で
「復元失敗が Kotlin 例外 → NSError で Swift に届く」という**要求される観察可能な結末は満たしている**ため ✅ とする
(担い手の配置の記述差は下の「参考所見」に記す)。

### テスト (新規 4 件、いずれも green)

| テスト | 場所 | 主張 |
|---|---|---|
| 宣言結果型と合わない結果値の報告は失敗として返る | `ios/Tests/KsDialogsTests/KsDialogsInteropBridgeTests.swift:78` | `complete("文字列")` → `kind == .error` / `value == nil` / `error == .resultTypeMismatch(expected: "Bool", actual: "String")`。**器は閉じ (`waitForPresentedContainers(count: 0)`)、cancelled には化けない** |
| nil を包んだ結果値の報告も失敗として返る | 同 `:99` | `Optional<Bool>.none as Any` → `.resultTypeMismatch(expected: "Bool", actual: "Optional<Bool>")` |
| 型が合わない報告で確定した後の再報告は無効 | 同 `:119` | 不一致で確定後に `complete(true)` / `cancel()` を続けても `resultCount == 1` かつ `kind == .error` — **exactly-once が崩れない** |
| 登録時に渡した宣言結果型が合う値と合わない値を境界越しに判別する | `kmp/.../iosTest/InteropBridgeContractTests.kt:68` | cinterop 越しに `acceptsValue` を叩き、`true`/`false` を受理し `"文字列"`/`1` を拒否 — **判定手続きが ObjC 境界を跨いで機能する**ことの実測 |

**見せかけの緑ではない**: 不一致の待ち合わせは `waitUntil { resultCount == 1 }` で結果到達を確認した後、
さらに 300ms 待って `resultCount > 1` にならないことを主張しており、待たずに数を見るだけの形にはなっていない。

### 呼び出し側の追随

`registerViewFactoryForViewModelClass:` はシグネチャが変わったが、**全呼び出し箇所が追随済み**
(`samples/kmp/iosApp/.../SampleDialogRegistration.swift:14-17` / ios テスト 3 箇所 / kmp iosTest 1 箇所)。
旧シグネチャの残存はグレップで 0 件、かつ全ルートがビルド・テスト成功している。

---

## 対応表の更新差分

verify-001 の対応表のうち、**判定が変わったのは下記 3 行のみ**。他の全行は変更なし (退行確認は次節)。

### kmp-facade

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **Swift async からの直接呼び出し** — 型付き結果が正しい型・値で届く | `commonMain/KsDialogs.kt:24-25`。Swift 側の直接呼び出し元はリポジトリ内に不在 | 4.5 の実測記録 (verification-matrix:112) | **⚠️ deviation 記録済み** (`deviation.md:5`) |
| Scenario: Swift から await した結果の型と値が正しい | 同上 | 同上 | **⚠️ deviation 記録済み** (`deviation.md:5`) |
| (本文) **復元失敗**と構成エラーは Kotlin 例外 → NSError で届く | `KsDialogsInteropResultType.swift` / `KsDialogsInteropNotifier.swift:26-39` / `KsDialogsInteropResult.swift:27-32` / `IosDialogGateway.kt:50-52` | ios 3 件 + kmp 1 件 (上表) | **✅** |

### samples

| Requirement / Scenario | 実装 | 状態 |
|---|---|---|
| (本文) Sample から本体の内部実装を直接参照しない | `samples/kmp/iosApp/.../SampleDialogRegistration.swift:14` が `KsDialogsInteropBridge` を使用 | **⚠️ deviation 記録済み** (`deviation.md:6`) |

---

## 退行の抜き取り確認

verify-001 で ✅ とした 59 セルのうち、今回の変更 (互換面の登録シグネチャ変更 + 検査追加 + samples 3 行) が
影響し得る範囲を重点に抜き取った。**退行なし**。

| 確認対象 | 結果 |
|---|---|
| ios-native「Swift 公開 API での貫通」— 型付き公開 API が影響を受けていないか | ✅ `Registry/DialogViewRegistry.swift:21-30` の型付き `register` は**未変更**。`DialogNotifier<Result>` は静的に型付いているため resultType を要さない構成のままで、`samples/ios/.../SampleDialogRegistration.swift:10` も従来どおり `Dialog.shared.registry.register` を使う |
| ios-native「互換面経由でも結果は1回だけ届く」 | ✅ `KsDialogsInteropBridgeTests.swift:46` が green。不一致経路でも exactly-once が保たれることを新規テストが追加で主張 |
| dialog-contract「結果はちょうど1回だけ確定する」(4形態) | ✅ `DialogResultChannel` (ios / android) と `TaskCompletionSource` (maui) は未変更。ios 38 / android 34 / maui 19 / bridge 5 すべて green |
| dialog-contract「型付き結果の show」/「VM 型キーによる View 解決と毎回生成」 | ✅ 各形態の同名テストが全件 green (件数の内訳は下表) |
| kmp「commonMain からの show 貫通」/「Swift 側登録とのキー同一性」 | ✅ `InteropBridgeContractTests` 5→6 件 (既存 5 件は据え置きで全通過)。`AndroidDialogGatewayContractTests` 5 件も据え置き全通過 |
| maui-binding 全 Requirement | ✅ MAUI 側は今回の変更対象外。19 件 + bridge 5 件が green |
| samples パリティ (4ルートの文言一致) | ✅ `samples/*/SampleText.*` は未変更。KMP iOS の追加は登録呼び出しの 3 行のみで、表示文言に影響しない |
| Android 画面破棄経路の手当て | ✅ `ActivityDialogPresentationSurfaceTests` 4 件 + `結果確定とダイアログの閉鎖` 4 件が green (android 総数 34 で verify-001 から変化なし)。実機再観測の記録は `verification-matrix.md:136-137` |

---

## 追加検査

### テスト実行 (絞り込みなしの全件)

`kasane/concepts/cross/conventions/test-execution.md` のコマンドで実行し、件数まで確認した。**全件 green**。

| ルート | コマンド | 結果 | verify-001 比 |
|---|---|---|---|
| ios | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **38 tests / 9 suites passed** | 35 → 38 (+3 互換面の型検査) |
| android | `./gradlew test --rerun-tasks` | **34 tests / 0 failures** | 34 (変化なし) |
| kmp | `./gradlew allTests --rerun-tasks` | **33 tests / 0 failures** (iosSimulatorArm64 **18** + androidHostTest **15**) | 32 → 33 (+1 境界越しの判別) |
| maui | `dotnet test` | **19 tests / 0 failures** (`合格: 19、失敗: 0`) | 19 (変化なし) |
| maui bridge | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | **5 tests / 0 failures** | 5 (変化なし) |

いずれもコンテキストパッケージの期待値 (ios 38 / android 34 / kmp 33 / maui 19 + bridge 5) と一致。

### 逆流検査

- `proposal.md` / `design.md` / `specs/` 6 件 — **未変更**。`git diff HEAD` が空、`git log` 上の最終更新は提案作成コミット `36f75f2` のみ
- working tree で変更されているのは `tasks.md` (実績欄) と `ui/brief.md` (照合記録) のみ。**❌ の対処にあたって spec を書き換えて辻褄を合わせた形跡はない** — ❌2 / ❌3 は spec を残したまま deviation.md へ記録する、規約どおりの処理になっている

### tasks.md

**全 34 項目 `[x]`、未チェック 0 件、虚偽なし。**
verify-001 では「全 26 項目」と書いたが、これは当方の集計誤り (正しくは 1.x=2 / 2.x=5 / 3.x=4 / 4.x=7 / 5.x=5 / 6.x=6 / 7.x=5 の計 34)。
verify-001 で確認した実体の対応関係そのものに変更はなく、判定にも影響しない。

verify-001 で指摘した「verification-matrix の KI セル `A4.5` の根拠の書き過ぎ」は、
当該 Scenario が deviation 化されたことで**根拠を要求する前提自体が外れた**。
併せて `verification-matrix.md:114` に復元失敗検出の実績メモが追加され、検査の位置と担い手が記録されている。

### 未記録乖離

**なし。** `deviation.md` が新規作成され 3 件が記録されている (❌2 / ❌3 / MD-b の契約方針)。
verify-001 時点で「保留の合意自体が未記録」だった ❌1 は、保留ではなく**実装で解決**されたため記録不要となった。

### UI 変更の確認

- 承認モックの記録あり (`ui/brief.md:34` — mock-b / `approved.png` / 2026-08-14 オーナー承認)
- 合意済み妥協の記録あり (`ui/brief.md:69-78`、4 件)
- 今回の変更は UI に影響しない (登録呼び出しの引数追加のみ) ため、モックとの再照合は不要

---

## 参考所見 (判定に影響しない)

- **`ui/brief.md:66` の MD-b 行が手当て前の事実のまま**: iOS 欄が「上の show は**未完了のまま**」と書かれているが、
  `deviation.md:7` は「iOS = 上下とも画面から消え、**上の show は cancelled で確定**」を合意済みの契約方針として記録しており、
  `common-spec-scenarios.md:96` と `verification-matrix.md:135` も再観測後の事実に追随済み。
  brief.md だけが 3 箇所と食い違っている。MD-b はデルタスペックの Scenario ではないため判定には影響しないが、
  **deviation.md という合意記録と正面から矛盾する記述**になったため、verify-001 時点より優先度は上がっている。蒸留前に揃えるのが望ましい
- **spec 本文の担い手の記述と実装配置の差**: `specs/kmp-facade` は「**iosMain actual** は…復元を担い」とするが、
  実装は検査を iOS 互換面に、例外化を iosMain に置いている (方式 b)。観察可能な結末は SHALL どおりのため ✅ としたが、
  **spec 文言と実装配置の差は蒸留で concepts / ADR へ落とすときに解消しておくのが望ましい**
  (「どこで検査するか」は phase-5 の Swift 向け KMP 面設計とも接続する)
- **不一致の印が Swift 型付き入口へ漏れる経路** (review-002 Suggestion と同じ所見): 互換面で登録した VM を
  型付き `Dialog.show` から show すると、`Dialog.swift:35-40` の `value as? ViewModel.Result` が印の型で失敗し、
  `actual` に内部の印の型名が出る。失敗にはなるので宙吊りや誤成功にはならず、縦串の Sample はこの組み合わせを踏まないため
  spec 違反ではない。phase-5 の API 表面の突き合わせで拾えば足りる

---

## 判定

**VALID**

- 全 20 Requirement / 31 Scenario (4 形態展開で 62 セル) が「✅ 一致」または「⚠️ deviation 記録済み」
- ❌1 は**実装で充足** — 検査・輸送・例外化の 3 段が揃い、境界テスト 4 件が実質的に主張している
- ❌2 / ❌3 は **deviation.md に理由つきで記録済み**であり、規約上「合意済みの差分」として違反にあたらない
- 虚偽チェックなし / 逆流なし / 未記録乖離なし / 全ルートのテストが全件 green

**アーカイブ可能な状態**と判定する。蒸留に入る前に、上記「参考所見」の 1 点目 (`ui/brief.md:66` と deviation.md の矛盾) を
直しておくことを推す — 長命層へ流れ込む記述であり、deviation.md が蒸留時の入力になるため。

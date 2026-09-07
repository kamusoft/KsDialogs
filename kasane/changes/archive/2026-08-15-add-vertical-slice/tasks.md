# Tasks: add-vertical-slice

実装順は依存の少ない順 (Native 2実装 → KMP / MAUI → Samples → 仕上げ)。共通仕様シナリオ表は先頭で作り、各 platform のテストがシナリオ ID を参照する。

## 1. 共通仕様テストの器 (初版)

- [x] 1.1 共通仕様シナリオ表を core の仕様文書として作成する — **受け入れ Scenario** (MD-a / MD-c: 期待値あり、テスト合否で判定) と**調査ケース** (MD-b / MD-d: 期待値なし、「各 OS の実挙動の記録が存在すること」が終了条件) を区分して分離する。シナリオ ID + 前提 + 操作 + 期待結果 + OS 差の記録欄 (→ Requirement: 多段表示の基本保証 / design Decision 9)
- [x] 1.2 Requirement × 形態 (6セル) の検証対応表を作成する — 各セルの判定手段 (自動テスト / 手動確認 / Native 継承 + adapter 契約テスト / コンパイル検証) を明記し、tasks 完了時に全セルが埋まっていることをフェーズ完了の判定に使う (→ 全 Requirement)

## 2. iOS Native (ios/)

- [x] 2.1 契約実装: `DialogViewModel<R>` 準拠 VM + `DialogNotifier` (exactly-once 保証持ち)・show (async throws + enum 結果)・レジストリ (メタタイプキー、interface + singleton 両入口で共有)・毎回生成 (→ Requirement: Swift 公開 API での貫通 / 型付き結果の show / 契約 interface と既定 singleton の両対応とレジストリ共有 / 結果はちょうど1回だけ確定する / VM 型キーによる View 解決と毎回生成)
- [x] 2.2 最小カスタム View ダイアログの表示実装 — 最前面 ViewController の自動解決・UI スレッドマーシャリング・host 不在 throw・外タップ cancelled 既定・結果確定で自ダイアログのみ閉鎖 (→ Requirement: 呼び出しコンテキストの契約 / 結果確定とダイアログの閉鎖 / 多段表示の基本保証)
- [x] 2.3 `@objc` 互換面の公開 (KMP 委譲面) — 非ジェネリック型消去輸送 + 公開 API との分離 (design Decision 12) (→ Requirement: KMP 委譲向け互換面の提供)
- [x] 2.4 ユニットテスト: 結果経路 (completed / cancelled の型・値)・二重報告 no-op・未登録 throw・host 不在 throw・毎回生成・同一 VM 再 show・両入口レジストリ共有・型不一致のコンパイル検証 (→ 各 Scenario)
- [x] 2.5 多段表示テスト: MD-a / MD-c の同名テスト、MD-b の実挙動記録 (→ Scenario: MD-a / MD-c)

## 3. Android Native (android/)

- [x] 3.1 契約実装: `DialogViewModel<R>` 準拠 VM + `DialogNotifier`・show (suspend + sealed、例外チャネル)・レジストリ (クラス参照キー、両入口で共有)・毎回生成 (→ Requirement: Kotlin 公開 API での貫通 / 型付き結果の show / 契約 interface と既定 singleton の両対応とレジストリ共有 / 結果はちょうど1回だけ確定する / VM 型キーによる View 解決と毎回生成)
- [x] 3.2 最小カスタム View ダイアログの表示実装 — resumed Activity の自動追跡 (ActivityLifecycleCallbacks)・UI スレッドマーシャリング・host 不在例外・外タップ cancelled 既定・戻るボタン cancelled (キーボード非表示時)・結果確定で自ダイアログのみ閉鎖 (→ Requirement: 呼び出しコンテキストの契約 / 結果確定とダイアログの閉鎖 / 戻るボタンによるキャンセル (通常時))
- [x] 3.3 ユニットテスト: 結果経路・二重報告 no-op・未登録例外・host 不在例外・毎回生成・同一 VM 再 show・両入口レジストリ共有・戻るボタン (通常時)・型不一致のコンパイル検証 (→ 各 Scenario)
- [x] 3.4 多段表示テスト: MD-a / MD-c の同名テスト、MD-b / MD-d の実挙動記録 (→ Scenario: MD-a / MD-c)

## 4. KMP facade (kmp/)

- [x] 4.1 commonMain 契約: `interface KsDialogs` + `sealed DialogResult` + レジストリ契約 + 既定 singleton エントリの expect (→ Requirement: commonMain からの show 貫通)
- [x] 4.2 androidMain actual: Android Native lib への委譲 (→ Scenario: 共有コードからの show が Android で動作する)
- [x] 4.3 iosMain actual: iOS Native lib の `@objc` 互換面への cinterop 委譲 (→ Scenario: 共有コードからの show が iOS で動作する)
- [x] 4.4 **最優先疎通確認**: commonMain VM の ObjC 可視性とレジストリキー同一性の実測 — 崩れたら実装を止めて設計に戻る (→ Requirement: Swift 側登録とのキー同一性)
- [x] 4.5 疎通確認: Swift async → suspend 変換の粗の実測 (nullable 化・sealed の見え方・エラーチャネル)。受け入れ条件を割る場合は kmp/ADR-0001 のフォールバック発動 (→ Requirement: Swift async からの直接呼び出し)
- [x] 4.6 commonTest: fake 実装による Presenter 差し替えテスト + 結果経路テスト (両ターゲット) + **adapter 契約テスト** (actual の委譲で引数・結果・show 対応が保たれることの検証。iosMain は型消去→復元の往復を含む) (→ Requirement: テスト差し替え / Swift async からの直接呼び出し / 検証対応表の「Native 継承」セルの根拠)
- [x] 4.7 iOS deployment target: KMP の生成物が iOS 17 (cross/ADR-0002) を宣言していることを受け入れ条件として確認する。非保証フラグ (-Xoverride-konan-properties) の正統な代替手段を再確認し、満たせない場合は完了不可またはオーナー合意の deviation として扱う (TODO 送りにしない)

## 5. MAUI binding (maui/)

- [x] 5.1 ビルド連携: iOS を標準 `XcodeProject` アイテムで接続 (→ design Decision 7)
- [x] 5.2 ビルド連携: Android 標準 `AndroidGradleProject` アイテムを実測し、成立可否を記録。失敗時は gradlew Exec + `AndroidLibrary` へフォールバック。結果込みで maui ドメインの ADR を起票 (→ design Decision 7)
- [x] 5.3 使い捨て Bridge 実装: show / dismiss 操作 1:1 + show ごとの completion (→ Requirement: MAUI 公開 API での貫通)
- [x] 5.4 C# facade: MAUI レジストリ (VM 型 → MAUI View factory)・MAUI View の platform view 実体化・`Task` 型付き結果・internal gateway seam (→ Requirement: MAUI 公開 API での貫通 / 結果経路の platform 非依存検証)
- [x] 5.5 ユニットテスト (素の net10.0 + fake gateway): 結果経路・未登録 faulted Task・二重報告 no-op・同一 VM 再 show・両入口レジストリ共有 + **adapter 契約テスト** (gateway へ渡る引数・返る結果・show 呼び出しとの1:1 対応の検証) (→ Scenario: fake 実装で結果経路をユニットテストできる / 検証対応表の「Native 継承」セルの根拠)

## 6. Samples (samples/)

- [x] 6.1 samples/ios: Local Swift Package 参照 + Basic Dialog デモ (→ Requirement: 4ルートの Basic Dialog デモ項目 / Sample の consumer 境界)
- [x] 6.2 samples/android: composite build (`includeBuild` + `dependencySubstitution` 明示) + Basic Dialog デモ (→ 同上)
- [x] 6.3 samples/maui: facade への ProjectReference 1本 + Basic Dialog デモ (→ 同上) — **提示経路の修正後に再確認し、iOS / Android とも 起動 → 表示 → OK / キャンセル → 結果表示 まで成立**
- [x] 6.4 samples/kmp: shared + androidApp + iosApp、共有 Presenter からの show + 各 OS の View 登録 (→ Requirement: 4ルートの Basic Dialog デモ項目 / Sample の consumer 境界 / kmp: Swift 側登録とのキー同一性) — **iosApp で実 framework 越しの ObjC export 名でのレジストリキー同一性を実測確認 (グループ4の残存リスク解消)**
- [x] 6.5 mock との視覚照合: 承認モックと各 Sample のダイアログ・メニュー・結果表示を突き合わせる (→ Requirement: 4ルートのパリティ) — **6セル全部で構造・トークン・意図の一致を確認 (証跡と結果は `ui/verification/`)。外側タップ・戻るボタン・多段表示の見え方は 7.2 送り**
- [x] 6.6 パリティ確認: 4ルートの文言・メニュー構成・SampleTheme RGBA の一致を突き合わせる (→ Scenario: 4ルートのデモ項目が一致する) — **コード上の一致 (文言・メニュー構成・RGBA) と画面上の突き合わせの両方を6セルで確認**

## 7. 仕上げ (受け入れ条件の消化)

- [x] 7.1 BuildProbe (4ルート) を削除し、全ルートのビルド + 全件テストが通ることを確認する (テスト実行規約のコマンド・件数確認に従う)
- [x] 7.2 **6セル** (Native iOS / Native Android / MAUI iOS / MAUI Android / KMP iOS / KMP Android) の手動確認 (show → 表示 → OK / キャンセル / 外タップ) を行い、最終照合画像を `ui/verification/` に保存して brief.md に照合日・結果・妥協点を記録する。roadmaps の phase-4 artifacts/ からはリンクする (→ Requirement: 4ルートの Basic Dialog デモ項目 / 4ルートのパリティ)
- [x] 7.3 スクリーンショット取得手順を実測し `kasane/config.yaml` の `ui.screenshot` に記載する (phase-3 申し送り)
- [x] 7.4 MD-b / MD-d の OS 実挙動記録をシナリオ表の調査ケース欄に追記し、multi-display-semantics.md / result-notification-semantics.md への追記候補をまとめる (蒸留への申し送り)
- [x] 7.5 検証対応表 (1.2) の全セルを実績で埋める — 縦串の多段表示の自動検証は Native 2実装の同名テスト (2.5 / 3.4)、MAUI / KMP は Native 委譲による継承 + adapter 契約テスト (5.5 / 4.6) を根拠とする。委譲経由で継承が崩れる挙動を見つけたら OS 差の記録欄に記録する (→ Requirement: 多段表示の基本保証)

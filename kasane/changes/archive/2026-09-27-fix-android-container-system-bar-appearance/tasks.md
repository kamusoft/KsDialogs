# Tasks: fix-android-container-system-bar-appearance

実装前に handbook cross の `comment-policy.md` (常時)・`test-execution.md`・`ci-flaky-test-policy.md` を読む。コードの該当箇所には `ADR-0039` のコメントを残す (grep で辿れるようにする)。

## 1. Android: 引き継ぎの読み取りの補強 (`DialogWindowSystemBars`)

- [x] 1.1 明暗の読み取りで、OS の返す値に加えて、提示先の旧来のフラグ (systemUiVisibility の明るいステータスバー / ナビゲーションバーのフラグ) も合わせて読む (→ Requirement: システムバー表示状態の引き継ぎ / PB-SB-09)
- [x] 1.2 再表示の作法で、提示先が指定していない (OS が 0 を返す) ときは器の作法を OS の既定のままにする (→ Requirement: システムバー表示状態の引き継ぎ / PB-SB-10)
- [x] 1.3 実装前の確認: 旧来のフラグだけで明暗を指定した提示先について、API 35 以降でも OS が明暗を返さないことを実測する。返す場合は 1.1 の読み取りが API 35 以降で重複するだけなので害は無いが、結果を報告に書く
- [x] 1.4 実装前の確認: 作法のずれ (提示先が作法を指定していないのに 0 が写る) が起きる API の範囲を、API 31 (`ksn_api31`) と API 35 (`Small_Phone`) の実測と OS の定数の定義から確かめ、PB-SB-10 のテストの API の絞り方 (`assumeTrue`) を決める

## 2. Android: Loading / Toast の器の引き継ぎ

- [x] 2.1 `LoadingContainer` の decorView が画面に載った時点で、Dialog の器と同じ引き継ぎ (`DialogWindowSystemBars.inheritSystemBarState`) を 1 回行う (→ Requirement: システムバーの指定の非干渉 (Loading) / LD-SB-01・LD-SB-02)
- [x] 2.2 `ToastContainer` にも同じ引き継ぎを入れる。フォーカスを取らない Toast で可視状態を写しても、提示先のバーの状態が変わらないことをテストで確かめる (→ Requirement: システムバーの指定の非干渉 (Toast) / TS-SB-01・TS-SB-02)
- [x] 2.3 画面の作り直しで器を載せ替える経路 (`detachForReattach` のあとの新しい器) でも、載せ替え先の画面から写すことを確かめる。作り直しの前後で提示先に**異なる**指定を与え (例: 前は明暗の指定なし、後は明るい地向け)、載せ替え後の器が作り直し後の指定を採用したことを見る (既存の再取り付けのテストが通るだけでは、読み直したことの証明にならない)

## 3. Android: テスト (instrumented)

- [x] 3.1 テスト用の提示先を用意する: ステータスバーのアイコンを明るい地向けに明示する画面。バーを隠す・旧来のフラグだけで明暗を指定する・作法を指定しない、の各状態を作れること。各テストは比べる前に「提示先の指定が狙いどおりになっていること」を前提として確かめる (白地に白アイコン同士の比較のように、空振りで通らないため)
- [x] 3.2 比べ方をそろえる: 器の指定は、**テストが提示先に与えた指定から組み立てた期待値**と比べる。提示先のウィンドウから OS が返す値とは比べない (旧来のフラグだけ・作法の未指定では OS が 0 を返し、正しく引き継いだ器と食い違う)。作法を指定していない場合の期待値は、作法を指定していないウィンドウの OS の既定値 (android-native デルタ「テストの比べ方」)
- [x] 3.3 `PB_SB_08_…`: Dialog の器のウィンドウの明暗の値が、期待値 (明るい地向け) と同じ。提示先がナビゲーションバーの明暗も指定しているときは、それも同じであることを追加で確かめる (→ PB-SB-08)
- [x] 3.4 `LD_SB_01_…` / `LD_SB_02_…`: Loading の器の明暗 (ナビゲーションバーを含む)・可視状態・作法が期待値と同じ。バーを隠した提示先では、器の要求に加えて、画面上のバーが実際に隠れたままであることも見る (→ LD-SB-01・LD-SB-02)
- [x] 3.5 `TS_SB_01_…` / `TS_SB_02_…`: Toast の器について同じ (→ TS-SB-01・TS-SB-02)
- [x] 3.6 `PB_SB_09_…` / `PB_SB_10_…`: 旧来のフラグだけの提示先 (ステータスバーとナビゲーションバーの両方の明暗)・作法を指定しない提示先 (API の絞り方は 1.4 の結果に従う) (→ PB-SB-09・PB-SB-10)
- [x] 3.7 `DialogTransparentOverlayTests` の「覆いが透明ならステータスバーの明るさは表示前後で変わらない」を `PB_SB_11_…` として直す (→ PB-SB-11)
  - 提示先に明るい地向けの明暗を明示する
  - 「表示前」を測る前に、明るさとは独立の終端条件 — 提示先のウィンドウがフォーカスを持つ・提示先自身の描画が画面に出た・起動時の表示 (スプラッシュ) のウィンドウが無くなった — の成立を待つ。成り立たなければ測らずに失敗させる。待ちは handbook ci-flaky-test-policy.md の「終端状態の合意」に従い、共通プリミティブ (`InstrumentedStateSettling`) に合意の条件として渡す
  - 同じクラスの「既定の覆いではステータスバー領域も覆いの色で暗くなる」も同じ前提にそろえる
- [x] 3.8 既存の PB-SB-01〜07 が引き続き通ることを確かめる (PB-SB-04 は API 29 でだけ判定できるので、6.1 のとおり未実行になる)

## 4. iOS: テスト (コードは変えない)

- [x] 4.1 `[PB-SB-08]`: 明暗を指定した提示元の上に Dialog の器を出し、器がステータスバーの見えの制御を奪わない (`modalPresentationCapturesStatusBarAppearance` が false、`childForStatusBarStyle` が nil) ことを確かめる。既存の `DialogStatusBarAppearanceTests` (`PB-IA-03`) と同じ形 (→ PB-SB-08)
- [x] 4.2 `[LD-SB-01]` / `[LD-SB-02]`: 明暗を指定した提示元・ステータスバーを隠した提示元の上に、`KeyWindowLoadingPresentationSurface` (key window を差し込む) で Loading の器を出し、画面 (view controller) の提示も親子関係への組み込みも起きず、key window の root が提示元のままであることを確かめる (→ LD-SB-01・LD-SB-02)
- [x] 4.3 `[TS-SB-01]` / `[TS-SB-02]`: `KeyWindowToastPresentationSurface` で Toast の器について同じ (→ TS-SB-01・TS-SB-02)

## 5. 網羅検査

- [x] 5.1 `scripts/scenario-id-coverage.py` の `MIRROR_AREAS` に `("LD", "SB")` と `("TS", "SB")` を足す (PB-SB は Android 固有の Scenario を持つので足さない)
- [x] 5.2 `scenario-id-coverage.py` / `--require-mirror` / `--selftest` がすべて通る

## 6. 完了確認

- [x] 6.1 handbook cross/test-execution.md に従って全ルートを実行する。Android の instrumented は専用 AVD の `Small_Phone` (API 35) と `ksn_api31` (API 31) の両方で回す (共用の端末は使わない)。API 29 の端末は回さない (29 以下は実測しないオーナー判断 2026-09-26。handbook が前提にする `ksn_api29` もこの環境に無い)。完了報告では「API 31 と API 35 で全件実行、API 29 は対象外」と呼び、API 29 でだけ判定できる既存 Scenario (`PB_SB_04_旧経路でも非表示状態が維持される`) を未実行として明記する
- [x] 6.2 Sample Android (API 35) で、通常メニュー / Dialog / Loading / Toast を撮り直し、Loading / Toast の表示中もステータスバーのアイコンが暗い色のままであることを証跡に残す (`evidence/`。起動方法は config の `ui.screenshot`)
- [x] 6.3 lint (`comment-policy-lint.py`・`identity-lint.py`・`local-path-lint.py`・`scenario-id-coverage.py`) が通る

## 7. 蒸留への申し送り (実装タスクではない)

- concepts の追随: core/api/layout-semantics.md (Dialog の既存の 1 文を ADR-0039 の約束にまとめ直す)・loading-semantics.md・toast-semantics.md (約束を足す)。android/api の公開面は変わらない
- core/ADR-0039 を accepted に昇格する
- handbook の候補: test-execution.md の「API レベルで走る / 走らない Scenario」に PB-SB-10 の API の絞り方を足すか。local-development-setup.md に専用 AVD (`ksn_api31`) の作り方を足すか (探索で `avdmanager create avd -n ksn_api31 -k "system-images;android-31;google_apis;arm64-v8a" -d small_phone` で作成済み。handbook が前提にする `ksn_api29` は無い)

# Exploration: add-loading-toast-typed-show

## 課題 / 動機

Dialog には ViewModel の**型だけ**を渡して表示する型指定 show (core/ADR-0019〜0021) が iOS Native / Android Native / MAUI にあるのに、Loading / Toast にはどの形態にも無い (2026-09-06 コード確認):

- iOS: `ios/Sources/KsDialogs/Presentation/KsLoading.swift:35` / `KsToast.swift:35` はインスタンス渡しとインライン factory 版のみ
- Android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:52` / `KsToast.kt:51` も同様
- MAUI: `maui/KsDialogs.Maui/Presentation/IKsLoading.cs:63` / `IKsToast.cs:61`。`Show<TViewModel>` は存在するが第1引数が VM インスタンスのインライン factory 版で、型引数だけで呼べる経路ではない
- Loading / Toast のレジストリ (`LoadingViewRegistry` / `ToastViewRegistry`) は **View factory スロットしか持たない** (core/ADR-0025・0029)

利用者 (オーナー) の要望: Loading / Toast も Dialog と同様に、VM の実体を渡さない型指定のオーバーロードで呼べるようにしたい。KMP 共有コード側の型指定 show は別 change [add-kmp-typed-show](../add-kmp-typed-show/exploration.md) で扱う (両者は独立)。

## 検討した選択肢 (却下案と理由を含む)

論点 1: Loading / Toast の型指定 show のモデル

- **A. Dialog と完全に同型 (VM factory のレジストリ登録が正・未登録は構成ミスとして失敗・configure クロージャあり) — 採用**。core/ADR-0021 の「全形態・同型の登録が正」モデルに載り、DI 登録忘れの VM が黙って生まれる事故を Dialog だけ防ぐ非対称を作らない
- **B. 型だけで生成 (既定コンストラクタ規約 + configure)** — 却下。DI 未注入の VM が黙って生まれる。Swift はリフレクション不足で同型に表現できず形態間で割れる (core/ADR-0021 の却下理由と同じ)。ADR-0021 の方針と矛盾する

## 決定事項

- Loading / Toast のレジストリに **VM factory スロット**を追加する (Dialog レジストリと同型: View factory と VM factory の 2 スロット・再登録はスロット単位の後勝ち・show 時はスナップショット解決)
- 型指定 show を iOS Native / Android Native / MAUI の Loading / Toast に追加する。動詞は show 1 本のまま、引数の形で経路を表す (core/ADR-0020)
- 未登録の型指定 show は構成ミスとして失敗する (暗黙の既定コンストラクタ生成は採らない)
- 順序保証は Dialog と同じ: VM factory で生成 → configure 完了 → (Loading は進捗受け口の紐付け) → View factory → 提示。VM factory / configure の例外は提示に進まず呼び出し元へ伝播する
- configure の同期性は機能の性質に合わせる: **Loading は非同期 configure 可** (show が suspend / async)、**Toast は同期 configure のみ** (show が fire-and-forget の同期呼び出し — core/ADR-0031)
- 型指定 show でも置き場所 (`DialogPlacement`) と Toast の duration は引数で渡せる
- MAUI の 1 行登録糖衣 (`RegisterForDialog` 相当) を Loading / Toast にも設けるかは propose で決める (DI 配線の自動化は maui/api/di-registration.md の範囲)

## ADR 候補

- 作成済み: core/ADR-0035 (accepted 2026-09-06) — Loading / Toast のレジストリに VM factory スロットを追加し、型指定 show を Dialog と同型で提供する

## 未決の論点

- MAUI の Loading / Toast に DI 自動配線の 1 行登録糖衣を足すか (Dialog の `RegisterForDialog<TView, TViewModel>()` 相当)
- Loading のスコープ形 (start) にも型指定版を設けるか (show だけか start も対か)
- Android Compose 面 (`ksdialogs-compose`) の型指定 show 拡張の要否 (Compose 面は現状インライン factory 拡張のみ)
- 概念文書の追随範囲: core/api/loading-semantics.md・toast-semantics.md (公開面の構成・カスタム View 版) と model-binding-semantics.md (型指定 show の対象を Dialog 限定から 3 機能へ)、各形態の loading-surface / toast-surface

## UI 素材

なし (UI 変更なし)

## 変更級の推奨: M

3 形態 × 2 機能の公開 API 追加 (非破壊) とレジストリ内部表現の変更、core/ADR-0025・0029 に関わる概念文書の追随。複数能力にまたがるため S ではない。UI なし・非破壊のため L には届かない (オーナー確定 2026-09-06)。

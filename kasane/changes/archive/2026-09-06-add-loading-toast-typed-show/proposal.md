# Proposal: add-loading-toast-typed-show

## Why

Dialog には ViewModel の型だけを渡して表示する型指定 show (core/ADR-0019〜0021) が iOS Native / Android Native / MAUI にあるが、Loading / Toast にはどの形態にも無い。Loading / Toast の専用レジストリ (core/ADR-0025・0029) は View factory スロットしか持たず、VM の生成を利用者側に強いている。利用者 (オーナー) から「Loading / Toast も Dialog と同様に VM の実体を渡さない型指定のオーバーロードで呼びたい」という要望が出た。

決定は探索で確定済み: Dialog と完全に同型 (VM factory 登録が正・未登録は失敗・configure クロージャあり) にする — [core/ADR-0035](../../decisions/core/0035-loading-toast-typed-show-vm-factory.md) (accepted 2026-09-06)。経緯は [exploration.md](exploration.md)。

## What Changes

- **Loading / Toast のレジストリに VM factory スロットを追加する** (3 形態)。Dialog レジストリと同型の 2 スロット構成 (View factory + VM factory、再登録はスロット単位の後勝ち、show 時は呼び出し時点のスナップショット解決)。登録キーは既存の VM 型キーをそのまま使う
- **Loading に型指定 show と型指定 start を追加する** (3 形態)。start (処理ブロックを渡すスコープ形) は Loading の主経路の 1 つで、VM 生成と処理実行の対を型指定でも成立させる。configure は非同期可 (Dialog と同じ)
- **Toast に型指定 show を追加する** (3 形態)。configure は同期のみ (show が fire-and-forget の同期呼び出し — core/ADR-0031)。duration と配置は型指定 show でも引数で渡せる
- **失敗契約**: VM factory 未登録は Dialog と同じ「VM factory 未登録」の構成ミスとして失敗する (既存の `ViewModelFactoryNotRegistered` を Loading / Toast でも使う。View factory 未登録との区別は Dialog と同じ)。Loading では VM factory / configure の例外 (キャンセル含む) は提示に進まず呼び出し元へ伝播する。Toast では VM factory 未登録だけが呼び出し時点の同期失敗で、VM factory / configure の例外は受理後の失敗 (警告 + 1 枚破棄) に分類する
- **順序保証**: VM factory で生成 → configure 完了 → (Loading は進捗受け口の紐付け) → View factory → 提示。VM factory と configure は View factory と同じ UI スレッド保証で実行する
- **MAUI の 1 行登録糖衣** `RegisterForLoading` / `RegisterForToast` は、`RegisterForDialog` と同じく View factory に加えて **VM factory (DI コンテナから引く) を自動配線する**。既に `TViewModel` をサービス登録しているため、配線の追加だけで型指定 show まで有効になる
- **概念文書の追随** (蒸留時): core/api/model-binding-semantics.md (型指定 show の対象を Dialog 限定から 3 機能へ)・loading-semantics.md・toast-semantics.md (公開面の構成・カスタム View 版)、ios / android / maui の loading-surface・toast-surface、maui/api/di-registration.md (1 行登録の配線)

影響する能力: loading-contract (core 契約)・toast-contract (core 契約)・ios-native・android-native・maui-binding・samples (デモ項目の追加は sample-parity に従う — 下記 Impact)

## Non-Goals

- **KMP 共有コードの型指定 show** — 別 change [add-kmp-typed-show](../add-kmp-typed-show/exploration.md) (kmp/ADR-0006)。本 change の Native 型指定経路を使わないため独立
- **KMP の Swift 面 (`Loading.shared.kmp` / `Toast.shared.kmp`) の型指定 show** — 探索で見送り (需要が出たら非破壊追加)
- **MAUI の fallback resolver (maui/ADR-0005) の Loading / Toast への拡張** — fallback は Dialog レジストリの機構として設計されており (`DialogViewRegistry.Fallbacks`)、Loading / Toast へ広げるかは別の設計判断 (規約ベース一括解決の需要が Loading / Toast にあるか未確認)。本 change では明示登録 (と 1 行登録糖衣) のみ
- **Android Compose 面 (`ksdialogs-compose`) への型指定 show 拡張** — 不要と判断。Compose 面の `showCompose` はインライン factory 専用の拡張で、レジストリ経路は `registerCompose` で登録した View factory を Native の型指定 show が解決する。Compose 登録 + 型指定 show の組み合わせはテストで確かめる
- **Loading の既定ローディング (メッセージ入口) への型指定** — 対象外。VM を持たない経路であり型指定の意味が無い

## Impact

- **破壊的変更なし**: 公開 API の追加のみ。既存のインスタンス渡し show / インライン factory 版はそのまま。レジストリの内部表現が View factory 単体からエントリ (2 スロット) に変わるが、公開面の登録 API の署名は変えない
- **波及範囲**: iOS (`KsLoading` / `KsToast` protocol と既定 extension、`LoadingViewRegistry` / `ToastViewRegistry`、Loading / Toast の coordinator の解決経路)、Android (同名の interface / registry / 実装)、MAUI (`IKsLoading` / `IKsToast`、registry、gateway、`KsDialogsServiceCollectionExtensions`)、3 形態のテスト、samples (デモ項目の追加は sample-parity の規約 — 4 ルートの同期と安定デモ ID の運用 — に従う。既存の Custom Loading / Custom Toast デモを型指定経路に切り替えるか新設するかは tasks で確定)
- **リスク**: C# のオーバーロード解決 — Loading の既存 `StartAsync<T>(Func<IProgress<double>, Task<T>> …)` と新設の型指定 `StartAsync<TViewModel>(…)`、Toast の既存 `Show<TViewModel>(TViewModel, Func<…>)` と新設の `Show<TViewModel>(Action<TViewModel>? …)` が型引数 1 個で並ぶ。成立性はコンパイル検査で早期に確認し、不成立の形が見つかれば core/ADR-0020 (動詞 show 1 本) の扱いをユーザーに諮る (勝手に別動詞にしない)
- **Toast の失敗分類**: VM factory 未登録は呼び出し時点の同期失敗、VM factory / configure の例外は「受理後の失敗」(警告 + 1 枚破棄) — show が同期・任意スレッド呼び出しで VM factory / configure は UI スレッド実行のため (提案レビューで確定、core/ADR-0035 に追記)
- **Loading の合流**: 型指定経路は VM 生成・configure 後にインスタンス渡しと同じ合流判定に入る。合流側の VM は表示に使われない (提案レビューで確定)

## 級: M

3 形態 × 2 機能の公開 API 追加 (非破壊) とレジストリ内部表現の変更。複数能力にまたがるため S ではなく、UI なし・非破壊・設計判断は ADR で確定済みのため L に届かない (オーナー確定 2026-09-06)。

domain: cross

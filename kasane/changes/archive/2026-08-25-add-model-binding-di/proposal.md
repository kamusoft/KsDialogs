# Proposal: add-model-binding-di

## Why

共有層からの疎結合呼び出し — 「View を知らないコードから VM を投げたらダイアログが出て結果が返る」— は原典コンセプトの中核だが、現状の実装は factory 登録 + インスタンス渡し show まで。VM を主役にした呼び出し面 (notifier の VM 注入・型指定呼び出し・DI 連携) が未実装で、MAUI は原典水準の1行登録がなく書き味が原典比で後退している (C# の部分的型引数推論の欠如による)。

phase-6 の議論で設計判断は core/ADR-0018〜0021・maui/ADR-0005 として確定済み。本変更はそれらを4形態 + KMP interop 面に実装する。

## What Changes

- **notifier の VM 注入** (core/ADR-0018): show 時にサイドテーブル (弱参照・インスタンス同一性キー) で notifier を VM に紐付け、`vm.notifier` 拡張プロパティ相当で参照可能にする。factory の `(vm) → View` 形を非破壊追加。VM 契約を参照型 (class) 限定に狭める。iOS の KMP 面には notifier アクセサを追加
- **型指定呼び出し** (core/ADR-0019〜0021): `show(VM 型) { vm → ... }` (configure クロージャ、async 可) を全形態に追加。VM 解決はレジストリ登録の VM factory (未登録は構成ミスとして失敗)
- **MAUI DI 糖衣** (maui/ADR-0005): 1行登録 `.RegisterForDialog<TView, TViewModel>()` (VM factory の DI 自動配線込み) と `AddKsDialogs(options)` の fallback resolver (View / VM 分離、解決順序 = 明示 → fallback → 失敗)
- **共有層からの呼び出し検証**: KMP commonMain から UI 層参照なしで show を呼び、Native 側 View が VM 注入経由で報告した型付き結果を共有層が受け取れることの実証 (commonMain の呼び出し面自体は変更なし)
- **Sample 通し**: VM 紐づけ呼び出しのデモとコンテナ連携例 (MAUI IServiceCollection / Koin / 手動登録) をパリティ準拠で追加

影響する能力: dialog-contract / ios-native / android-native / maui-binding / kmp-facade / samples

## Non-Goals

- **共有 VM 自身の結果報告 (commonMain への expect notifier)** — 結果報告は View の責務に限定する決定 (agenda ②-2)。需要未実証で、必要になれば非破壊追加できる
- **Native の型のみ1行登録糖衣** — 不採用決定 (agenda ③-3)。Swift はクロージャ登録が既に1行、Kotlin のコンストラクタ参照は既存 API で成立するためドキュメント紹介のみ
- **notifier スロット付き VM 基底クラス糖衣** — サイドテーブル方式と排他でない任意オプション (core/ADR-0018)。需要が出たら別変更で
- **破棄フックの opt-in 提供** — 非採用決定 (core/ADR-0019)。await 後の呼び出し側後始末で代替
- **DI コンテナ別 adapter (Koin 用拡張等)** — core/ADR-0004 が将来オプションと位置づけ。コンテナ連携は VM factory の中身と Sample の例示で足りる
- **KMP commonMain への型指定呼び出しの公開** — 共有層の呼び出し面は現状維持 (合意済みの到達イメージ api-sketch-final-form.md どおり「共有コードからの呼び出しは変更なし」)。VM factory の登録モデル自体は core/ADR-0021 の同型のまま、公開面の拡張だけ見送る。需要が出たら別変更で

## Impact

- **破壊的変更あり**: VM 契約の参照型 (class) 限定化 — Swift は protocol の AnyObject 制約化。現状の利用実態では実害ゼロ (テスト・サンプル含め全 VM が final class であることを確認済み)。一般公開前のため互換 shim は作らない
- 4形態 + KMP interop 面 (ObjC ブリッジ) に波及。レジストリの内部表現が View factory + VM factory の2種になる
- リスク: C# のオーバーロード解決の成立性 (不成立の形が見つかれば core/ADR-0020 の supersede が必要)・Kotlin サイドテーブルの同一性キー実装 (equals ベースの WeakHashMap 不可)・KMP iOS 経路の notifier アクセサは実 framework 越しでしか完全検証できない (core/ADR-0004 の既知制約と同種)

## 級: L

4形態の公開 API に触れ、契約の破壊的変更 (class 限定) を含み、複数能力 (登録・表示・結果通知) にまたがるため。

domain: cross
roadmap: library-foundation/phase-6-model-binding-di

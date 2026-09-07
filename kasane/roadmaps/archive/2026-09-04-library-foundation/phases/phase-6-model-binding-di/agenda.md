# Model 紐づけ + DI 連携

ViewModel-first 呼び出しと DI コンテナ連携を実装する change フェーズ。共有層からの疎結合呼び出しという原典コンセプトの中核。

## 論点

(議論レベルの論点は全決着 — 2026-08-24。phase-5-2 申し送りの必須要件「MAUI 1行登録 `.RegisterForDialog<TView, TViewModel>()` を主経路、素の Register は低水準として存置」は ③-1 の決定と core/ADR-0021 に取り込み済み。以下は change の実装タスクとして残るスコープ)

- 共有層 (KMP commonMain / .NET 共有プロジェクト) からの呼び出し検証 (④): UI 層参照なしでダイアログを呼べることの実証
- Sample 通し (⑤): VM 紐づけ呼び出しのデモをパリティ準拠で追加 (完了条件)。コンテナ連携の例示 (MAUI IServiceCollection / Koin / 手動登録) もここに含める (③ から統合)。既定 singleton と DI 登録インスタンスのレジストリ共有 (core/ADR-0002) は実装済み確認済み (DialogViewRegistry.shared 共有)

## 素材

- [api-sketch-final-form.md](artifacts/api-sketch-final-form.md) — phase-6 完了時点の最終形 API スケッチ (C# / Swift / Kotlin / KMP。phase-5-2 の議論から、2026-08-17)
- [api-sketch-maui-fallback-resolver.md](artifacts/api-sketch-maui-fallback-resolver.md) — MAUI fallback resolver (SetIocConfig 相当の後継) の完成系イメージ (③-2 の決定時スケッチ、2026-08-24)

## 決定事項

- **Native の型のみ1行登録糖衣は採用しない (③-3, 2026-08-24)**: Swift / Kotlin はクロージャの型推論で factory 登録が既に1行のため、MAUI 1行登録の存在理由 (C# の型引数推論欠如による書き味後退の回復) が Native にはない。Swift 版は View 側に init 規約 protocol 準拠を強いて短縮分を相殺する。Kotlin はコンストラクタ参照 (`register(VM::class, ::View)`) が既存 API でそのまま通るためドキュメントで紹介のみ (API 追加ゼロ)。必要になれば糖衣として非破壊追加可能。可逆・局所のため ADR 非昇格
- **MAUI 一括解決糖衣は fallback resolver として再設計 (③-2, 2026-08-24)**: 原典 SetIocConfig 相当 (存置は ADR-0004 で決定済み) の形を、static 関数ペアではなくレジストリの fallback resolver (View fallback / VM fallback の明示分離) として再設計。解決順序は「明示レジストリ → fallback → 構成ミスとして失敗」を仕様として規定。設定は `AddKsDialogs(options => ...)` の DI チェーン一箇所で、null 上書きの粗は構造ごと消滅。MAUI 限定糖衣。スケッチ: [api-sketch-maui-fallback-resolver.md](artifacts/api-sketch-maui-fallback-resolver.md)。→ maui/ADR-0005 (accepted)
- **型指定呼び出しの VM 解決元 (③-1×①, 2026-08-24)**: レジストリに VM 型キー → VM factory を登録できるようにし (View factory と対)、型指定呼び出しはこれで解決する。MAUI の1行登録 `.RegisterForDialog<TView, TViewModel>()` は「DI コンテナから VM を引く factory」を自動配線するため利用者の追加手数ゼロ。未登録の型指定呼び出しは構成ミスとして失敗 (ADR-0004 の登録漏れと同じ扱い)。原典の「暗黙の既定コンストラクタ生成」は採らない。コンテナ連携 (Koin 等) は factory の中身として表現。これで論点①の議論レベルの未決は解消 (シグネチャ細部は spec 化)。→ core/ADR-0021 (accepted)
- **show 系 API の命名統一 (⑥, 2026-08-24 オーナー起案)**: 表示 API の動詞は show 1本 (C# は言語慣習で ShowAsync) に統一し、呼び出し経路 (インスタンス渡し / インライン factory / 型指定 + configure) は引数の形で表現する。原典の経路接尾辞 (FromModel)・結果型分裂 (ShowResult — ADR-0012 で既に消滅) は持ち込まない。C# のオーバーロード解決の成立は spec 化で検証。→ core/ADR-0020 (accepted)
- **notifier の VM 注入方式 (②-1, 2026-08-24)**: show 時にライブラリが notifier を VM に紐付け、`vm.notifier` (拡張プロパティ相当) で参照する方式を採用。紐付け表はライブラリ管理 (弱参照・インスタンス同一性キー)、VM 契約は参照型 (class) 限定に狭める。factory の `(vm) → View` 形は非破壊追加。基底クラス糖衣は任意の後日オプション。同一インスタンス並行 show は構成ミスとして失敗させる。→ core/ADR-0018 (accepted)。KMP commonMain 表現は②-2 で継続
- **ライフサイクルフック非採用 + 型呼び出しの configure クロージャ (②-3×①, 2026-08-24)**: VM 契約にライフサイクルフック (原典 DialogInitializeAsync / Destroy 相当) を持ち込まない。初期化は「呼び出し側 new のコンストラクタ」または「型指定呼び出しの configure クロージャ (async 可、VM 型にコンパイル時型付け)」、後始末は show の await 後に呼び出し側 (キャンセル時も finally / defer で拾える)。原典の粗 (パラメータ型不一致でサイレントスキップ) は構造的に消える。configure はパラメータ渡しに限らない汎用セットアップ地点 (非同期プリロード・VM 参照捕獲・コールバック配線)。→ core/ADR-0019 (accepted)
- **KMP 共有層での notifier の見せ方 (②-2, 2026-08-24)**: 結果報告は View の責務に限定し、commonMain に notifier 型は出さない。Android は Native typealias により `vm.notifier` 拡張が無修正で効く。iOS は KMP 面にアクセサを追加 (具体形は spec 化で確定)。共有層は show の戻り値 (core/ADR-0003 の async 単発) で結果を受ける。共有 VM 自身の報告 (commonMain expect notifier) は需要が出たら非破壊追加。可逆・局所のため ADR 非昇格

## TODO

- [x] 論点の解消 (2026-08-24 議論レベルは全決着。④⑤は change の実装タスクへ)
- [x] ksn-propose で変更提案を起こす (→ add-model-binding-di、実装完了)

## 実装結果 (2026-08-25 反映)

change [add-model-binding-di](../../../../changes/archive/2026-08-25-add-model-binding-di/proposal.md) として L 級で実装完了 (verify-001 VALID・review-003 APPROVED・全ビルドルート green)。決定事項はすべて設計どおり実装され、主な確定差分 (deviation 22項目、詳細は同 change の deviation.md):

- 型指定 show の Swift configure は `async throws` (例外伝播の Requirement を表現するため)
- VM factory 登録 API は Kotlin `registerViewModel` / C# `RegisterViewModel` (オーバーロード解決の曖昧化回避。iOS のみ `register(_:viewModel:)`)
- 値型 / value class VM の拒否は spec の入口列挙 (登録・型指定 show) を超えて**全 show 経路の共通提示入口**へ拡大 (相方レビュー指摘 + オーナー裁定。Android `ValueClassViewModel` / MAUI `ValueTypeViewModel` で対称化)
- design Open Questions は両方決着: C# 拡張プロパティ `vm.Notifier` は成立、2型引数形のオーバーロード解決も成立 (core/ADR-0020 の supersede 不要)

申し送りのルーティング:

- **MAUI iOS 面の Sample 通し未実施** (.NET for iOS が Xcode 26.1 を要求、現行環境は 26.5 でビルド不可 — deviation 9) → [phase-11 packaging の agenda](../phase-11-packaging/agenda.md) の論点に追記済み (発行検証で MAUI iOS ビルドが必須になるため)
- **`RegisterForDialog` した View の生成失敗が素の `InvalidOperationException` のまま** (review-001 Suggestion) → 簡易起票 [wrap-maui-view-creation-failure](../phase-11-packaging/agenda.md) へ (2026-09-02 に phase-11-packaging の agenda 論点へ合流)
- **MB-KM-02 の完全自動化 (KMP モジュールへの instrumented テスト源セット追加)** → 見送り (2026-08-25 蒸留時のオーナー判断)。「報告結果が show 呼び出し元へ届く」は Native 側の同名契約テストと Sample 通し (4ルート証跡取得済み) で担保されており、源セット追加のコストに見合う検出力の増分がない
- proposal の Non-Goals (notifier スロット付き VM 基底クラス糖衣・commonMain への型指定呼び出し公開・破棄フック・DI コンテナ別 adapter) は提案時の合意どおり「需要が出たら別変更で非破壊追加」の見送りのまま

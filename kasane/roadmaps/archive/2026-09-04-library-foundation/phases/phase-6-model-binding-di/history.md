# phase-6-model-binding-di 議論履歴

## 2026-08-24: 議論の順番

論点の依存関係から ② VM ライフサイクル + notifier 注入 → ① ShowFromModel 系 → ③ DI 差し込み → ④ 共有層検証 → ⑤ Sample 通し の順で進めることに合意。②が factory の形 (`(vm) → View` 化) を決め、①③の前提になるため。

前提調査 (ksn-scout): 4形態とも「VM 型キーで factory を引く → factory に (VM, DialogNotifier) を渡す → DialogResultChannel が1回だけ settle → show の戻り値に型復元」の骨格が実装済み。notifier は型消去 factory を呼ぶ瞬間に生成され、レジストリにも show にも渡らない。VM 契約は4形態とも空のマーカー (iOS のみ `associatedtype Result = Bool`)。ライフサイクルフック・DI 連携コードは現状ゼロ。MAUI は C# 側に独立 registry / presenter を持ち提示だけブリッジ委譲 (maui/ADR-0001)。

## 2026-08-24: notifier の VM 注入方式 (論点②-1)

**決定: B「ライブラリ管理の紐付け表 (サイドテーブル) + 拡張プロパティ」を採用。**

show 時にライブラリが notifier を VM に紐付け、View / VM は `vm.notifier` (拡張プロパティ相当) で参照する。紐付け表は弱参照・インスタンス同一性キーで、VM 契約は参照型 (class) 限定に狭める。これにより factory の `(vm) → View` 形を非破壊追加でき、1行登録 (phase-5-2 申し送りの必須要件) の前提が整う。

検討した選択肢:
- **A. 契約に settable プロパティ追加** — 却下。全 VM にプロパティ実装1行が必須になり、api-sketch の「中身ゼロの1行 VM 定義」が崩れる (コンパイル時型安全は最良だったが書き味を優先)
- **B. サイドテーブル + 拡張プロパティ** — 採用。空定義維持・他基底 (androidx ViewModel 等) と両立。型安全は「面は型付き・内部キャスト」で既存の型消去設計と同水準
- **C. ライブラリ基底クラス** — 主経路としては却下 (単一継承を消費し androidx ViewModel 等と衝突)。ただし B と排他ではなく、任意の糖衣として後日追加可能

付随の確定事項:
- VM 契約の参照型 (class) 限定への変更は、現状の利用実態で実害ゼロ (iOS の VM 準拠型はテスト・サンプル含め全て final class、struct はゼロ。grep で確認)
- 同一 VM インスタンスの並行 show は構成ミスとして失敗させる (ADR-0004 の登録漏れと同じ扱い。ADR-0005 の毎回生成モデルにより正常系では起こらない)
- KMP commonMain での notifier 表現 (共有 VM から `vm.notifier` をどう見せるか) は残課題として論点②-2 へ

→ core/ADR-0018 として proposed 起票。同日、オーナー確認 (規約違反の修正指示: 本文から phase 番号・論点番号を除去) を経て accepted に昇格。

## 2026-08-24: KMP 共有層での notifier の見せ方 (論点②-2)

**決定: A「結果報告は View の責務に限定し、commonMain に notifier 型は出さない」を採用。**

- 結果は View 上のユーザー操作から生まれるのが本筋で、共有層は show の戻り値 (core/ADR-0003 の async 単発) で受けるのが既定の契約という整理
- Android: 共有 VM は Native DialogViewModel の typealias のため、②-1 のサイドテーブルと Native の `vm.notifier` 拡張が無修正で効く
- iOS: 共有 VM は ObjC エクスポートクラスで Swift の VM protocol に準拠しないため、KMP 面にアクセサを追加する (具体形は spec 化で確定)
- 検討した選択肢: **B. commonMain に expect notifier を追加** (共有 VM 自身も報告可能) — 却下。expect/actual + interop bridge の逆方向経路と実装表面が大きく、共有層から閉じる需要が未実証。A から B へは非破壊で拡張できるため、需要が出たら追加する
- ADR 非昇格の判断: 覆すコストが低く (B の非破壊追加余地)、局所的なため。agenda 決定事項と本記録のみ

## 2026-08-24: ライフサイクルフックと型呼び出しのパラメータ渡し (論点②-3×①)

**決定: ライフサイクルフックは契約に持ち込まず、型指定呼び出しのパラメータ渡しは configure クロージャとする (B')。**

原典の実物確認 (AiForms.Maui.Dialogs) で判明した前提:
- `DialogInitializeAsync(T parameter)` は汎用初期化フックではなく、ShowFromModel 系 (ライブラリが DI から VM を解決する経路) 専用のパラメータ配達。呼び出し側が VM を渡す経路では呼ばれない
- `Destroy()` は View 再利用機構 (ReusableDialog) の dispose 時後始末
- 2つとも KsDialogs が却下済みの機構に存在理由が紐づく (呼び出し側 VM 生成 = コンストラクタで渡せる / core/ADR-0005 使い捨てモデル + ADR-0003 async 単発 = await 後に呼び出し側が後始末できる)
- 原典の粗: `if (vm is IDialogViewModel<TParameter>)` により、パラメータ型を間違えると初期化がサイレントスキップされる (コンパイルは通る)

議論の経緯: 当初「A. フックなし」を単独推奨したが、オーナー指摘「型から呼び出す場合はコンストラクタで渡せない」により、論点① (ShowFromModel 系) と結合して再整理。型指定呼び出し自体はロードマップのゴール (原典機能の移植完了) のため提供が前提。

検討した選択肢:
- **A'. 型呼び出しを提供しない** — 却下。原典機能の移植完了ゴールに反する
- **B'. 型呼び出し + configure クロージャ** — 採用。`ShowAsync<TVm>(vm => ...)` 形 (async クロージャ可)。契約フックなしで型安全 (クロージャが VM 型に直接効く)、原典のサイレントスキップの粗が構造的に消える。パラメータ渡し以外の汎用セットアップ (非同期プリロード・VM 参照捕獲・コールバック配線) にも使える
- **C'. 型呼び出し + 原典 init フック** — 却下。フック interface が契約に入り、型不一致の粗も残る (直しても実行時エラー止まり)
- **B. 破棄フックのみ opt-in** (②-3 当初案) — 却下。async 単発 + 使い捨てモデルで代替可、需要未実証、必要なら非破壊追加できる

VM の解決元 (DI コンテナ連携のどこに載せるか)・各形態シグネチャは論点③と一体で継続。→ core/ADR-0019 として起票し、同日オーナー承認で accepted に昇格。

## 2026-08-24: 論点⑥の追加 (オーナー起案)

show 系 API の命名統一を論点⑥として追加。原典は後付けの積み重ねで命名の統一性が崩れている (ShowAsync / ShowResultAsync / ShowFromModelAsync / ShowResultFromModelAsync) ため、KsDialogs では呼び出し経路の命名を揃えたい。ADR-0002 命名ポリシーの「原典命名が非対称な箇所は対称性を優先して改める」が方針の根拠。

## 2026-08-24: show 系 API の命名統一 (論点⑥)

**決定: A「動詞は show 1本に統一し、経路の違いは引数の形だけで表現する」を採用。**

前提の整理:
- 原典の命名崩れは2次元 — 結果型の有無 (Show / ShowResult) と経路接尾辞 (FromModel)。前者は core/ADR-0012 (結果型は VM が宣言) で KsDialogs では既に起こりようがない
- 現状実装は既にインスタンス渡し・インライン factory (core/ADR-0013) とも `show` / `ShowAsync` のオーバーロードに集約済み (ios/Sources/KsDialogs/Presentation/Dialog.swift ほかで確認)。未決だったのは型指定呼び出し (configure クロージャ = core/ADR-0019) の命名のみ

検討した選択肢:
- **A. show 1本 (引数形で経路を表現)** — 採用。型指定も `show(OKViewModel.self) { vm in ... }` / `ShowAsync<TVm>(vm => ...)` の形。対称性 (起案動機)・現状実装との整合・補完への集約が理由。C# は Async 接尾辞を言語慣習として維持
- **B. 経路ごとに動詞を分ける (showFromModel 等の原典踏襲)** — 却下。原典の崩れを持ち込み、既存2経路と非対称になる

留意点: 原典 ShowFromModelAsync 利用者への対応表をドキュメントで案内する。C# のオーバーロード解決の成立性 (引数なし `ShowAsync<TVm>()` とインスタンス版の分離) は spec 化で検証する。→ core/ADR-0020 として起票し、同日オーナー承認で accepted に昇格。

## 2026-08-24: 型指定呼び出しの VM 解決元 (論点③-1×①)

**決定: C「レジストリに VM factory も登録できるようにする (View factory と対)」を採用。**

- ADR-0004 の「リフレクション不要の明示レジストリ」思想を VM 解決に延長し、3形態 + KMP で同型の「登録が正」モデルにする
- MAUI の1行登録 `.RegisterForDialog<TView, TViewModel>()` は IServiceCollection チェーン上にあるため、View factory と併せて「VM を DI コンテナから引く factory」を自動配線する — 利用者は1行で型指定呼び出しまで有効になる
- 未登録の型指定呼び出しは構成ミスとして失敗させる (ADR-0004 の登録漏れと同じ扱い)

検討した選択肢:
- **A. DI 解決のみ** — 却下。Native に「DI」の標準がなく、形態ごとに解決フックの定義が必要になる
- **B. 原典同等の2段 (DI フック → 既定コンストラクタ)** — 却下。Swift はリフレクション不足で「既定コンストラクタ」段を同型表現できず形態間で挙動が割れる。暗黙 new は「DI 登録し忘れた依存なし VM が黙って生まれる」事故の温床で、構成ミスを失敗させる方針とも不整合
- **C. VM factory のレジストリ登録** — 採用。コンテナ連携は factory の中身 (`{ get() }` 等) で表現でき、core 契約にコンテナ知識が入らない

これで論点① (ViewModel-first 呼び出し) の議論レベルの未決は解消 (各形態シグネチャの細部は spec 化の領分)。→ core/ADR-0021 として起票し、同日オーナー承認で accepted に昇格。

## 2026-08-24: MAUI 一括解決糖衣 (SetIocConfig 相当) の形 (論点③-2)

**決定: C「レジストリの fallback resolver として再設計」を採用。**

前提: SetIocConfig 相当の存置自体は core/ADR-0004 で決定済み。ここで決めたのはその形。1行登録 (③-1 の VM factory 自動配線) の成立で個別登録の代替としての役目は消え、価値の本体は「per-type 登録なしの規約ベース一括解決 (Prism 風の VM 名 → View 名規約など)」に絞られた。

- 「登録済みなら明示レジストリが勝ち、未登録の VM 型が来たら fallback resolver に聞き、それでも解決できなければ構成ミスとして失敗」の一段構え。解決順序を仕様として規定
- 原典で暗黙に兼務されていた View 解決と VM 解決の fallback を明示分離 (View fallback / VM fallback。後者は「IServiceProvider から引く」既定実装を用意)
- 設定は `AddKsDialogs(options => ...)` の DI チェーン一箇所。static 差し込み口が存在しないため、原典の null 上書きの粗は構造ごと消滅
- MAUI 限定糖衣 (リフレクション文化圏)。Native / KMP には持ち込まない (ADR-0004 のまま)

検討した選択肢:
- **A. 原典形踏襲 (関数ペアの static 一括設定 + null 上書きだけ修正)** — 却下。レジストリの外に第2の解決経路が立ち、優先順位の規定が別途必要。static 設定は DI 登録インスタンスとの一貫性 (ADR-0002 のレジストリ共有) にも乗りにくい
- **C. fallback resolver** — 採用。解決順序がレジストリの中で閉じ、粗の温床ごと消える

オーナーの要望で完成系イメージを提示し、確認を経て採用。スケッチは artifacts/api-sketch-maui-fallback-resolver.md に保存。→ maui/ADR-0005 として起票し、同日オーナー承認で accepted に昇格。

## 2026-08-24: Native の型のみ1行登録糖衣の採否 (論点③-3)

**決定: 採用しない。Kotlin のコンストラクタ参照は既存 API で成立するためドキュメント紹介のみ (API 追加ゼロ)。**

- MAUI 1行登録の存在理由は「C# の部分的型引数推論の欠如による書き味後退の回復」(phase-5-2 実験) であり、クロージャ型推論が効く Swift / Kotlin には回復すべき後退がない — factory 登録が既に実質1行
- Swift の型のみ登録 (`register(View.self, for: VM.self)`) は View 側に init 規約 protocol への準拠宣言 (+1行) を強い、短縮効果 (数文字) を相殺する。オーバーロードも UIKit / SwiftUI × 通常 / KMP 面で増える
- Kotlin の `register(VM::class, ::View)` はコンストラクタ参照が factory 型に一致すれば既存 API でそのまま通る — 新 API 不要
- 対称性のためだけに足す価値はなく、必要になれば糖衣として非破壊追加できる。可逆・局所のため ADR 非昇格

あわせて、③ 最後の残項目「コンテナ連携例 (Koin / 手動登録) の見せ方」は議論の論点ではなく sample / ドキュメントの領分と整理し、⑤ (Sample 通し) に統合。**これで議論レベルの論点は全決着** — 残る④ (共有層検証)・⑤ (Sample 通し) は change の実装タスク。

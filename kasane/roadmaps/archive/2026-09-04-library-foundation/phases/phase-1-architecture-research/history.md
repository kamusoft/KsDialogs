# phase-1-architecture-research 議論履歴

## 2026-08-13: KMP の位置づけ (共有 core vs 並列ターゲット)

- 選択肢: A. 共有 core 方式 (KMP commonMain に core 契約を置き全形態の正とする) / B. 各 Native 独立 + KMP は薄い並列ファサード
- 採用: **B**。Swift / Kotlin の2つの Native 実装を土台に、MAUI は binding で、KMP は commonMain の呼び出し契約 + 薄い expect/actual ファサードで包む対称形。core 契約はコードではなく仕様 (concepts + 共通仕様テスト) として共有し、乖離は phase-4 の縦串スライスで早期検出する
- 理由: ライブラリの実体は各 OS の提示機構に乗る UI で、commonMain に置いて嬉しい共有ロジックが薄い / MAUI (C#) は commonMain を消費できないため共有 core にしても「全形態の正」になれない / 共有 core 方式は純 iOS Native 利用者に Kotlin ランタイム入り framework を背負わせる / リブランド方針「Native 主・実装は各形態で書き直す」と整合
- ユーザー補足 (KMP 利用イメージ): KsAppKMP の共有層 Presenter / ViewModel から呼べる呼び出しインターフェースが主役で、共有 ViewModel と Native View の紐付け機構が核心。View の中身は Native View を直接利用 (CMP なら Compose)。MAUI の「MAUI View 実体化」に相当する重い層は不要で KMP 層は薄くできる
- 派生する制約: KMP iOS 側は Kotlin から Swift 実装を呼ぶため ObjC 互換面 (または Swift Export) が必要 → 論点「技術セット」で interop の現状を調査する
- ADR: core/ADR-0001 として proposed 起票

## 2026-08-13: リブランド方針 ADR の明文化

- 内容: ロードマップ非ゴールで合意済みの3原則をそのまま ADR 化 — (1) Native 主 (Swift / Kotlin の Native 実装が主役、各言語のイディオム優先)、(2) 互換 shim なし (AiForms.Maui.Dialogs との API 互換レイヤーは提供しない)、(3) 独立ブランド (仕様と実装パターンのみ継承)
- 対抗案: 互換 shim を提供する案 — 却下。shim は旧 API の形 (MAUI 依存の設計) を永久に背負い込み、Native 主での設計仕切り直しと矛盾する。概念・命名は継承するため移行コストはもともと小さい
- 裏取り: scout が KsSettingsView cross/ADR-0017 の実物を確認 (Native 主・MAUI 副・shim なし・独立ブランド) — 3原則と矛盾なし。翻案元として ADR に参照を記載
- 補足: ユーザーから shim の意味の確認あり → 「旧 API 呼び出しを新実装に橋渡しする互換レイヤー」の説明で合意
- ADR: cross/ADR-0001 として proposed 起票

## 2026-08-13: 技術セット (バージョンと最低対象 OS)

- 判断型: KsAppKMP cross/ADR-0001 の「グリーンフィールドの今が移行コストゼロで新世代に乗れる唯一のタイミング」を踏襲し、2026-08 時点の最新安定セットで固める
- 採用: Kotlin 2.4.10 (KsAppKMP と同版) / Gradle 9.7.0 / AGP 9.3.0 / Xcode 26.6 (Swift 6.3) / .NET は net10.0 (LTS) 土台で .NET 11 (STS, GA 2026-11) は GA 後に TFM 追加 / KMP→Swift interop は `@objc` 互換面 + Swift Export 見送り (Alpha・production 非推奨。KsAppKMP core/ADR-0003 と同判断) / swift-tools-version は 6.2 相当と推測されるが一次情報未確認のため phase-2 scaffold 時に実物で確定
- 最低対象 OS (KsAppKMP にも決定記録がない新規判断): 選択肢 a. iOS 17 / minSdk 24 (KsAppKMP 実測整合) / b. iOS 16 / minSdk 31 (2026 相場) / c. iOS 16 / minSdk 24 (広め)
- 採用: **a (iOS 17 / Android minSdk 24 = Android 7.0)**。b は minSdk 31 が KsAppKMP (minSdk 24) に載らず最初の消費者を切るため除外。a と c の差は iOS 16 対応のみで、iOS の新版普及は速く (iOS 18 が12ヶ月で84%)、ダイアログ提示系で iOS 17 API を素直に使える利点が勝つ
- 根拠素材: [artifacts/scout-tech-stack-2026-08.md](artifacts/scout-tech-stack-2026-08.md)
- ADR: cross/ADR-0002 として proposed 起票

## 2026-08-13: 技術セット・補正 (最低 OS 更新ルールと判断根拠の修正)

- ユーザー提起: ADR-0002 に最低 OS の更新ルール (例: 1年ごとに +1) も入れたい
- ユーザー指摘 (重要な前提の明確化): **KsDialogs は一般に公開予定のライブラリ**であり、消費者プロジェクト (KsAppKMP) を意識した下限設定は不要。更新ルールも +1 固定ではなく「都度相場を調査して更新する」緩い取り決めでよい
- 反映: cross/ADR-0002 に年次見直しルール (年1回・8月目安に相場調査、引き上げ幅は都度判断、引き上げ時は supersede) を追記。minSdk 31 案の却下理由を「消費者に載らない」から「一般公開ライブラリとして広い間口を優先」に修正。「毎年 +1 の機械ルール」案は却下として記録 (相場の動きは年により異なる)
- 決定済みの iOS 17 / minSdk 24 自体は公開ライブラリ前提でも妥当として維持

## 2026-08-13: 既存資産知識の取り込み方針

- 選択肢: A. 全部いま一括取り込み / B. 全部オンデマンド / C. ハイブリッド (原則系は即時翻案、機構系はオンデマンド参照)
- 採用: **C**。原則系 (意思決定を縛る方針) は決定に使った時点で KsDialogs 自身の ADR / concepts へ翻案 (実例: リブランド3原則 = cross/ADR-0001 ← KsSettingsView cross/ADR-0017)。機構系 (モノレポ構成・Swift interop 構成・ビルド設定等の作り方の知識) は使うフェーズ (phase-2 scaffold / phase-4 縦串) まで取り込まず、reference-repositories.md 経由で「リポジトリ名 + ADR 番号」参照に留め、吸収時に新文脈で設計し直して新 ADR に出典リンクを残す
- 却下理由: A は phase-2〜4 まで使わない機構知識が先に腐る + 翻訳コスト + 移植した瞬間から乖離開始。B は原則が明文化されず phase-1 の議論がぶれる
- 判断型の出典: KsAppKMP core/ADR-0006 (scout が実物確認。同 ADR も「原則系即時宣言 + 機構系オンデマンド再決定・一括移植なし」)
- ADR: cross/ADR-0003 として proposed 起票

## 2026-08-13: 公開 API 形状 (静的 Instance vs DI サービス vs 両対応)

- 選択肢: A. 静的 Instance のみ (原典方式) / B. DI サービスのみ / C. 両対応 (契約 interface + 既定 singleton エントリ + DI 登録可)
- 採用: **C**。原典の `Dialog.Instance` は既に `IDialog` インターフェースを返す static であり (契約と入口の分離)、C 案の下地がある。これを3形態に一般化 — 契約は各形態のイディオムで interface / protocol 定義、既定エントリは原典踏襲の singleton (Swift `.shared` / Kotlin object / MAUI `Dialog.Instance`)、DI 利用者は同じ interface をコンテナ登録して注入 (KMP 共有層 Presenter の単体テストで fake 差し替え可能に)
- 却下理由: A は static 直呼びで fake 差し替え不可 (Presenter テスタビリティ欠落)。B は「いつでも呼び出せる」というライブラリのウリを失い、Swift で DI 前提は非イディオム
- 命名ポリシー: KsSettingsView maui/ADR-0008 を実物照合の上で翻案 — 対応概念は原典命名を踏襲、原典命名が非対称な箇所は対称性を優先して改め、契約に無い機能は互換提供せず再設計
- 残課題 (論点「DI 差し込み方式」へ): 既定 singleton と DI 登録インスタンスの同一性をどう保つか
- ADR: core/ADR-0002 として proposed 起票

## 2026-08-13: 結果通知方式 (コールバック / Flow / async)

- 原典の実物: DialogView 自動生成の DialogNotifier → Complete/Cancel → 内部イベント → TaskCompletionSource で await 解決 (async 対応済み)。粗が2つ — 結果が object キャストで型安全でない (`ShowResultAsync<T>` 中の引数なし `Complete()` でキャスト例外の可能性) / キャンセルが `default(TResult)` と区別不能
- 選択肢: A. コールバック / B. Flow・Observable / C. async 単発 + 型付き結果
- 採用: **C**。「1表示 = 1結果」の意味論に単発非同期が一致。core 契約は「show は completed(結果) | cancelled を1回返す非同期操作。結果型は show の型パラメータで固定し通知役も同型に束縛」— 原典の粗2つをここで解消。Swift は async + enum、Kotlin / KMP commonMain は suspend + sealed class、MAUI は Task (原典踏襲・戻りは型付き結果に洗練)
- 却下理由: B はストリームで過剰。A は全形態 async 標準の時代に公開 API の一級市民とする理由がない — ただし ObjC 互換面 (KMP→Swift 経路) の最下層には completion handler として残る (cross/ADR-0002 の `@objc` 互換面の具体例、Swift async から機械変換)
- ADR: core/ADR-0003 として proposed 起票

## 2026-08-13: DI 差し込み方式 (SetIocConfig の洗練)

- 原典の実物: `SetIocConfig(Func<Type,Type> viewTypeGetter, Func<Type,object> viewResolver = null)` — コンテナ非依存の関数2本 static 差し込み。null チェック漏れの粗あり
- 選択肢: A. 原典踏襲 (関数2本) / B. 明示レジストリ (VM 型キー → View factory 登録) / C. コンテナ adapter を形態別提供
- 採用: **B**。原典の Type 変換関数は C# リフレクション文化前提で、Swift (リッチなリフレクションなし) と KMP commonMain (platform View 型を知らない) に持っていけない。「VM キー → View factory」の登録なら Swift はメタタイプ、KMP は KClass がキーになり、factory 登録は各 platform 側で行う — 「共有 VM と Native View の紐付け」の実体。既定 singleton と DI 登録インスタンスは同じレジストリを共有し、論点「公開 API 形状」の残課題 (同一性担保) を解消。MAUI には原典互換糖衣 (SetIocConfig 相当の一括委譲、粗は修正) を残す
- 却下理由: A は Swift / KMP 不適合。C はコンテナごとの保守コストで、B の上に将来 adapter を足せるため対立案ではない
- ユーザー要望で書き味の具体例 (KMP 共有 Presenter / 純 Swift / MAUI 糖衣 / テスト fake) を提示して合意 → [artifacts/api-sketch-registry.md](artifacts/api-sketch-registry.md) に保存。iOS 側は「commonMain の VM クラスが ObjC クラスとして Swift から見える」ことがキー同一性の前提 — phase-4 縦串の最優先疎通確認ポイント
- ADR: core/ADR-0004 として proposed 起票

## 2026-08-13: View 再利用機構の抽象

- 選択肢: A. 原典機構ごと契約化 (ハンドル + 遅延初期化) / B. 再利用ハンドルのみ契約化・遅延初期化は実装詳細 / C. 再利用機構を契約から削除 (使い捨て一本化)
- 経緯: AI の当初推奨は B。ユーザーの実体験証言「原典で Reusable は全然使わなかった。使い捨てでもパフォーマンスで困らなかった」を受けて公平に再評価 → C に推奨変更
- 採用: **C**。(1) 原典の動機「MAUI View 実体化の重さ」は Native 実装 (core/ADR-0001) で構造的に消滅 (2) 作者本人が使わなかった機能は一般利用者はもっと使わない。公開ライブラリの契約は永久保守対象 (3) 削ると IDisposable 契約・Dispose 後 Show のエラー意味論・KMP ハンドルブリッジ・OnceInitializeAction (iOS/Android でシグネチャ割れの実装詳細) が連鎖的に消え、KMP 層を薄く保てる (4) 後から足すのは非破壊、後から消すのは破壊的 — 「今は載せない」が可逆側
- 残す側の論拠 (却下): 激重カスタム View の高頻度出し直しで生成コストが効く可能性 → 起きたら実測の上で非破壊追加すればよい / 原典 Create 利用者の移行書き換え → リブランド方針 (cross/ADR-0001) が互換非提供を宣言済み
- ADR: core/ADR-0005 として proposed 起票

## 2026-08-13: 多段表示の core 契約表現

- 選択肢: A. OS 提示機構への委譲を踏襲し契約は観察可能な意味論のみ / B. 明示スタック管理を契約化
- 採用: **A**。原典に明示スタック管理コードは存在せず (iOS = ViewController presentation、Android = Dialog ウィンドウへの委譲)、roadmap で挙動継承が前提合意済み。Native 2実装土台では各 OS の提示機構が本業として多段管理を担うため二重管理の理由がない。契約に書く意味論: (1) 表示中でも show 可 (2) 各 show は独立に結果を返す非同期操作 (core/ADR-0003 が多段でも成立) (3) 後発が手前・閉じる順序は OS 提示機構に従う (4) ライブラリは段数状態を持たない・公開しない
- 却下理由: B は OS の提示機構との二重管理・喧嘩のリスク。「dismiss all」等の拡張は原典になく、必要時に非破壊追加できる (View 再利用の削除と同じ非対称性)
- 留意: OS 差が挙動に漏れうる → phase-4 縦串 + 共通仕様テストで挙動差を記録して吸収
- ADR: core/ADR-0006 として proposed 起票

## 2026-08-13: レイアウト計算の共通仕様化

- 原典の実物: 属性は ExtraView 共通基底に集約。Measure アルゴリズムは iOS / Android でほぼ行対応のコピー (日本語コメントまで同一、本質差はデバイスサイズ取得のみ)。計算が共通層・iOS・Android の3箇所に散在 — 移植時の主要な負債
- 選択肢: A. 観察可能な規則を仕様化 + 実装自由 / B. 計算ロジックを共有コード化 / C. 原典アルゴリズムの同型移植を規範化
- 採用: **A**。共通化すべきは計算コードではなく観察可能な規則 — 属性セット (原典踏襲命名) と、サイズ決定の優先順位・クランプ規則 (Proportional = 比率 / Fill = 画面幅マージン控除 / 未指定 = 内容サイズを画面内クランプ) を core 仕様 (concepts) に1本化。実装は各 OS のレイアウト機構 (AutoLayout / LayoutParams / Compose modifier) で規則を満たせばよく、原典の手動 Measure は MAUI 仮想 View 都合として移植しない。一貫性は共通仕様テスト (同じ属性入力 → 期待サイズ・位置) で担保 — Sample パリティと同じ発想
- 却下理由: B は共有 core の再導入で core/ADR-0001 と矛盾。C はコピペ構図の負債ごと継承
- 留意: 共通仕様テストの整備が A の成立条件。属性の取捨は phase-4/5 の仕様化で確定
- ADR: core/ADR-0007 として proposed 起票

## 2026-08-13: 後続フェーズ agenda への決定反映

全論点解消後、ユーザー依頼で phase-1 の決定を後続フェーズの agenda に反映した:

- **phase-2**: ビルドルート論点の前提を core/ADR-0001 (KMP 薄いファサード) に更新。最小疎通に cross/ADR-0002 の技術セット参照を追記。swift-tools-version 実物確定を論点として追加 (cross/ADR-0002 の残課題)
- **phase-3**: core concepts 書き下ろし候補を ADR 番号で具体化 (レイアウト規則・多段表示・結果通知の意味論)。proposed ADR 10件の accepted 昇格レビューを実施候補として追記
- **phase-4**: 結果通知契約の決定済み注記。KMP 公開 API 論点に最優先疎通確認2点 (VM クラスの ObjC 可視性 = core/ADR-0004 前提、async 変換経路 = core/ADR-0003) を明記。共通仕様テストの器を論点として追加
- **phase-5**: **reusable 機構の論点を削除** (core/ADR-0005 の直撃。冒頭に反映注記)。Measure 一本化論点をレイアウト共通仕様テスト整備に置き換え (core/ADR-0007)。多段表示・結果通知の論点を「決定済み方式の実装」に更新
- **phase-6**: DI 差し込み論点を ViewTypeGetter / Resolver 方式からレジストリ方式 (core/ADR-0004) に置き換え。singleton / DI のレジストリ共有実装を明記
- **phase-7**: カスタム View 版論点から Create* 再利用を除外し、LoadingConfig の IsReusable 相当の再検討に置き換え (core/ADR-0005)
- **phase-8**: 影響なし (命名ポリシー等は全フェーズ共通の ADR 参照で足りる)
- **phase-9**: README 論点に両入口 (singleton / DI) の使い分け説明を追記 (core/ADR-0002)

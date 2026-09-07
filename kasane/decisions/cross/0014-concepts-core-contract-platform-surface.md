---
id: 0014
title: concepts は core に platform 非依存の契約だけを残し、公開名・署名・コード例は <platform>/api/ へ分離する
status: accepted
date: 2026-09-05
---

## Context

KsDialogs の concepts は `core/api/` の 8 本 (約 1,300 行) が 4 形態 (Native iOS / Native Android / .NET MAUI / KMP) の公開名・署名・コード例・framework 固有の注意 (Compose の遅延評価スコープ等) を 1 本文に同居させており、`ios/` と `android/` ドメインには concept が 1 本も無い (`maui/api/` 1 本・`kmp/api/` 1 本のみ)。`kasane/concepts/rules.md` のカテゴリ定義は `<platform>/api/` (platform 固有の公開 API・利用例) を既に持っているが、中身の配置が規則に追いついていない状態にある。

利用者向け Agent Skills (cross/ADR-0011) は platform ごとに 1 Skill で、docs-refresh の API 名網羅検査は各 Skill の源泉 concepts に現れる識別子がその Skill に載っているかを突き合わせる。全 Skill が `core/api/` 7 本を共有する構造のため、検査候補の約 6 割が「他 platform の公開名」になり、初回生成後にオーナーが仕分けた除外リスト (`kasane/handbook/cross/user-skill-api-listing.md`) は約 100 行に達した。うち実際に掲載可否の判断が要る名前 (内部 coordinator・低頻度 API) は 1 割程度で、残りは検査器の限界をオーナー判断の形で抱え込んだものである。姉妹ライブラリ KsSettingsView は同一の検査スクリプトで除外リストが 7 行に収まっており、差は検査器ではなく concepts の構造 (core = 契約、`<platform>/api/` = 公開面) にある。

前提: 利用者向けドキュメントが platform 単位の Skill であり、その源泉が concepts であること (cross/ADR-0011)。

## Decision

- `core/api/` には**観察可能な挙動と保証** (platform 固有の識別子を持たない契約) だけを残す。「show は completed / cancelled をちょうど 1 回返す」のように文で書き、特定 platform の識別子 (`ShowAsync` / `Loading.shared` 等) を本文に並べない。承認済みの platform 差分の挙動 (多段表示の下段先閉じの扱い・呼び出し元キャンセルの観察のされ方など) は利用者が観察する契約なので core に残し、識別子を含まない散文か表で書く
- core の本文でバッククォート表記してよい識別子は、4 つの Skill 範囲 (iOS / Android / MAUI / KMP) すべての公開面に同綴り (MAUI の interface 接頭辞 `I` は同綴り扱い、大文字小文字の違いは別綴り) で存在することを実装コードで確認した**共通概念名** (`DialogPlacement` / `DialogTransition` / `DialogViewModel` / `DialogResult` / `KsDialogs` / `KsLoading` / `KsToast` など。確定一覧は実装コードで確認して定める — 本 change の `kasane/changes/archive/2026-09-06-split-concepts-platform-surface/verification/core-contract-check.md` が初版で、`DialogException` は iOS が `DialogError` のため、`LayoutArea` はどの形態にも存在しない綴りのため条件を満たさなかった) とそのメンバー名に限る。ここでいう KMP の公開面は、commonMain の公開宣言に加えて、KMP Skill の源泉に入る Android ホスト側 (`androidMain` の typealias が指す Android Native の型) と Swift 向け公開面 (`kmp/api/ios-host-integration.md` が扱う面) を合わせた **3 側**を指す (この判定規則は `kasane/concepts/rules.md`「契約と公開面の振り分け」に規則として置く)。条件を満たさない用語は core ではバッククォートを付けずに散文で書く。既定シングルトン・形態別の別名・添付の面・例外の詳細型・framework 型・コード例は platform 側に置く。各 core concept は末尾の「形態別の公開面」節から 4 platform の同名 concept へリンクする
- **公開名・署名・コード例・framework 固有の注意**は `<platform>/api/` の concept へ出す。分割単位は「platform × 機能」で、利用者向け Skill の references と 1:1 に対応させる (`dialog-surface` / `layout-surface` / `transition-surface` / `loading-surface` / `toast-surface`)。`ios/api/` と `android/api/` を新設し、`maui/api/`・`kmp/api/` を拡充する。KMP は Native へ委譲する形態のため、KMP 側の concept は共有コード側の面 (dialog / loading / toast) だけを持つ薄いものでよく、KMP の Swift 向け公開面 (`Dialog.shared.kmp` 等) は KMP 利用者向けの既存 concept `kmp/api/ios-host-integration.md` に集約する。各公開面 concept は冒頭で対応する core 契約へリンクし、「扱うのは公開面だけで、挙動の契約は core が正」と宣言する (core 側の「形態別の公開面」節と対でリンクし合う)
- 利用者が使う契約ではない検証機構の記述 (レイアウトの共通ケース表と OS 差の統制) は `core/architecture/` (`core/architecture/layout-case-table.md`) に置き、既定どおり Skill の源泉から外す (manifest の `excluded` に理由つきで載せる)
- 移行 Skill の源泉は cross/ADR-0011 の基準「新 API 側の concepts のうち対応表が触れるもの」を維持する。ADR-0011 の Decision にある具体列挙 (`maui/api/di-registration.md`) は起票時点で maui/api がその 1 本だけだった事実の反映であり、分割後は対応表が触れる `maui/api/` の公開面 concept を含む
- Skill の源泉 (manifest の `targets`) は「core の契約 + 自 platform の api」の組に組み替える。docs-refresh の API 名網羅検査スクリプトは変更しない (KsSettingsView と同一を保つ)
- 分割の順序は、進行中の利用者向け文書の初回展開 (rollout-user-docs) の完了後とし、Skill の再生成が二重にならないようにする

## Alternatives Considered

- **検査スクリプトに除外パターン (ケース ID・ファイル名・framework 型のブラックリスト) を足す**: 安いが、候補の最多を占める他 platform の公開名には効かず、framework 型は列挙が追いつかない。却下
- **対象 platform のソースから public 宣言名を集め、その集合にある名前だけを候補にする**: 他 platform 名・framework 型・非 API token を 1 つの判定で落とせるが、Swift / Kotlin / C# の 3 言語で宣言抽出を持つ中規模の実装になり、抽出漏れがそのまま公開 API の見逃しになる。また concept 側の構造的なノイズ源が残り、concept が増えるたびに再生産される。検査器の改修より concept の構造を正す方が根本的であるとして却下
- **concept 側の識別子に platform 印を付ける**: 記述であるべき concept に検査都合の印を入れ、長命層に負担を移す。却下
- **検査器は変えず除外リストを基準ごとに圧縮する**: handbook の可読性は上がるが、報告のノイズは毎回出続ける。却下
- **platform ごとに完全な写しを持ち core を薄い索引にする**: 同じ挙動を 4 か所に書くことになり、挙動の改訂が 4 か所へ波及する。却下
- **core はそのままにして `<platform>/api/` へ名前の対応表だけ足す**: 安いが core に他 platform 名が残るので根本を変えない。却下

## Consequences

- 正: API 名網羅検査が検査器を変えずに「自 platform の名前」だけを候補にし、除外リストは掲載可否の実判断だけに縮む。concept 追加時も同じ構造で書けばノイズが再生産されない
- 正: concepts の配置が `rules.md` の既存カテゴリ定義 (`<platform>/api/`) と一致し、ios / android ドメインに concept の受け皿ができる
- 負: 8 本・約 1,300 行の再構成と concept の新設が要る大きな手入れで、manifest の `targets` 組み替えと Skill の再生成を伴う
- 負: 契約 (core) と公開面 (`<platform>/api/`) の 2 文書に分かれるため、1 つの挙動を読むときに 2 か所を辿る。core から platform 側へのリンクで補う
- 負: 現行の除外リストは分割後に再仕分けが要り、分割前の確定内容の多くは無効になる
- 正: 分割の実施で除外リストは約 95 行から 20 行に縮み、「対象 Skill 外・機械検査由来」の行は 0 件になった。core の本文から識別子を外す書き直しは、契約と実装の乖離 (型名・引数名・列挙子の綴りが形態間で揃っていない箇所 5 件) を顕在化させる副産物も生んだ (出典: 実装結果)
- 負: core の layout / transition の契約は識別子を外しても散文 10,000 字の警告閾値を超えたままで、構造の再検討は棚卸しに持ち越した (出典: 実装結果)

## Revisit When

- Skill の分割軸が platform 単位でなくなったとき (cross/ADR-0011 の見直し)
- 前提 (Context) が崩れたとき

---
出典: kasane/changes/archive/2026-09-06-split-concepts-platform-surface/exploration.md (検討した選択肢・決定事項) / kasane/changes/archive/2026-09-06-split-concepts-platform-surface/design.md (Decision 1〜3) / kasane/changes/archive/2026-09-05-rollout-user-docs/second-opinion-code-001.md (Suggestion 8) / ../KsSettingsView/kasane/concepts/index.md (翻案元の構造)

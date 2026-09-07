# レビュー結果: rollout-user-docs (014 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

修正後の `ksdialogs-maui` を、過去の MAUI review と実装者報告を参照せず、change の正本、対応する 9 concept、現行の C# 公開面・両 OS bridge・テスト・Sample から独立に照合した。構造、frontmatter、英日ロックステップ、planned manifest の source map、task 3e の確定除外は合格し、限定実行した .NET テストも 133 件すべて成功した。一方で、確定済み掲載基準に対する公開 API 名の欠落と、冒頭で案内する契約 interface の DI 注入を Skill 内だけでは構成できない問題があるため、判定は `CHANGES_REQUESTED` とする。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — `skills/**` の API 掲載範囲と task 3e の仕分け
- `kasane/handbook/cross/test-execution.md` — 限定ビルド・テストと合意済み全ビルド免除
- `kasane/handbook/cross/runtime-behavior-verification.md` — 実装・テスト・Sample による挙動の裏付け
- `kasane/handbook/cross/local-development-setup.md` — 現行 MAUI ビルドルートの確認
- `kasane/changes/rollout-user-docs/specs/user-skills/spec.md` — Skill 構造、frontmatter、閉世界性、翻訳ロックステップ、レビュー 4 層
- `kasane/changes/rollout-user-docs/design.md` Decision 1 / 3 / 7 — MAUI references 分割、source map、独立レビュー条件
- `kasane/changes/rollout-user-docs/deviation.md` — 全ビルド免除と既存 doc-structure baseline を含む合意済み差分
- `kasane/changes/rollout-user-docs/ui/brief.md` — MAUI Skill は新規 UI や画像を持たず、README 用既存 Sample 撮影だけが対象であること
- `ksn-review`、`csharp-impl-skill`（必須 2 reference を含む）、`maui-skill`、`maui-native-binding-skill`

## 検査証拠

- 指定の planned manifest では `ksdialogs-maui/SKILL.md` が core 8 concept + MAUI DI concept の全 9 本を source に持ち、7 references は design Decision 3 と一致した。各ファイルが実際に扱う内容にも未登録 source はなかった。
- task 3e を planned manifest で再実行した。MAUI 行に残る識別子は `A.min` から `SetIocConfig` まで、すべて `kasane/handbook/cross/user-skill-api-listing.md` の現行 MAUI 除外（platform 外、interop、標準型からの導出、確定済み低頻度 API）に exact 分類されている。
- `concepts-coverage-check.py`、`heading-parity-check.py`、`code-block-parity-check.py`、`frontmatter-check.py`、対象一覧を生成しての `link-resolution-check.py` はすべて成功した。英日 8 ファイルずつの相対構造は一致し、対応コードブロックは byte 一致した。ja frontmatter は `KsDialogs`、`.NET MAUI`、`Dialog`、`Loading`、`Toast`、`dependency injection`、`layout`、`transition` を trigger keyword として含む。
- `local-path-lint.py` と `identity-lint.py` は成功した。Skill 内の外部 URL は許可された `metadata.source` のみで、`kasane/`、ADR、change-id、interop 型、ローカル絶対パスの漏出はなかった。
- `doc-structure-lint.py` は既存 42 ファイル 261 件の baseline を報告したが、対象の MAUI Skill は報告対象に含まれない。この baseline の扱いは `deviation.md:6` に記録済みであり、本レビューの指摘にはしない。
- `dotnet test maui/KsDialogs.Maui.ApiSurfaceCheck/KsDialogs.Maui.ApiSurfaceCheck.csproj -f net10.0 --no-restore` は API surface check のビルドに成功した。`dotnet test maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj -f net10.0 --no-restore` は 133 passed / 0 failed / 0 skipped。製品コード・テスト不変更のため、proposal Impact と deviation に従い全 platform build は実行していない。
- 公開 C# facade、registry、attached property、iOS / Android gateway、Swift / Kotlin native bridge、bridge tests、MAUI tests、`samples/maui/` の DI・Dialog・transition・Loading・Toast 実経路を突合した。添付 10 属性と `GetX` / `SetX` / `XProperty` 導出規則、既定値復元 API、型付き結果と OS 差、非同期 configure、custom progress の例は現行実装と一致した。

## 指摘事項

### [🟠 Major] 確定掲載基準に対して公開 API 名が欠落している

**該当箇所**: `skills/en/ksdialogs-maui/references/transitions.md:3`、`skills/en/ksdialogs-maui/references/layout.md:3`、`skills/en/ksdialogs-maui/references/dialogs.md:3`、`skills/en/ksdialogs-maui/references/di-registration.md:3`（ja 対応箇所も同じ）
**問題点**: `kasane/handbook/cross/user-skill-api-listing.md:18` は、利用者が使う公開 API のプロパティ名・機能名を対応 Skill のどこかに最低 1 回掲載するよう要求し、同 `:36` は除外表にない未掲載名を自動除外しないとしている。しかし英日 MAUI Skill 全体に、API surface check が公開契約として直接コンパイルしている `Dialog.GetTransition`、`Dialog.TransitionProperty`、`DialogTransition.OverlayDuration`、`DialogTransitionEdge.Top` / `Start` / `End` が 0 件である（`maui/KsDialogs.Maui.ApiSurfaceCheck/DialogTransitionApiSurfaceChecks.cs:29-74`）。さらに、公開 enum の `DialogAlignment.Fill`（`maui/KsDialogs.Maui/Contract/DialogAlignment.cs:21-27`）、キャンセル結果を型で扱う `DialogResult<TResult>.Cancelled`（`maui/KsDialogs.Maui/Contract/DialogResult.cs:14-19`）、カスタム結果型の 1 行登録 `RegisterForDialog<TView, TViewModel, TResult>` も利用方法が現れない。これらは現行除外リストに無く、10 属性だけに適用した導出規則では transition API や enum case、結果型、3 型引数 overload を導出できない。task 3e の concept 抽出結果が全分類済みでも、現行公開コードからしか拾えないこの不足は解消されない。
**推奨修正**: 英日を同時に更新し、transition の読み出し・binding 対象・`OverlayDuration`、全 edge、`Fill`、`Cancelled` の分岐例、カスタム結果型用 3 型引数登録を、それぞれ該当 reference に簡潔に掲載する。意図的に載せない項目があるなら、実装上の利用経路と基準を示してオーナー判断を得たうえで handbook の exact 除外に追加する。

### [🟠 Major] 案内されている契約 interface の DI 注入を Skill 内だけでは構成できない

**該当箇所**: `skills/en/ksdialogs-maui/SKILL.md:12`、`skills/en/ksdialogs-maui/references/di-registration.md:1-89`（ja 対応箇所も同じ）
**問題点**: 冒頭はテスト・DI 構成で `IKsDialogs` / `IKsLoading` / `IKsToast` を `Dialog` / `Loading` / `Toast` で注入できると案内するが、DI reference に契約と実装を `IServiceCollection` へ登録する手順が一つもない。`AddKsDialogs` は fallback の merge と provider capture だけを行い（`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs:34-46`）、`RegisterForDialog` / `RegisterForLoading` / `RegisterForToast` は content View / ViewModel の配線であって 3 契約の service registration ではない。実テストも `IKsDialogs dialogs = new Dialog(...)` のように facade を明示生成している（`maui/KsDialogs.Maui.Tests/DialogDependencyInjectionTests.cs:31-39`）。したがって利用者が Skill の `MauiProgram` 例をそのまま実装して consumer の constructor に `IKsDialogs` を要求すると解決できず、仕様が要求する「両入口」のうち DI 入口が閉世界で完結していない。
**推奨修正**: `di-registration.md` に `IServiceCollection` 上で 3 契約を対応する公開 facade へ登録し、consumer constructor で受け取る完動例を追加する。併せて `AddKsDialogs` は契約 facade 自体を登録せず、fallback / provider capture の設定であることを明記し、推奨 lifetime と既定 facade との process-wide registry / settings 共有を説明する。

### [🟡 Minor] transition の観察可能な契約がレシピから抜けている

**該当箇所**: `skills/en/ksdialogs-maui/references/transitions.md:1-59`（ja 対応箇所も同じ）
**問題点**: preset と custom hook、fault / cancellation の吸収、timeout なしは記載されているが、source concept の利用判断に必要な次の契約がない: 未添付時と duration の既定は 250 ms、`None` は content だけを無演出にし overlay は既定時間で fade、content と overlay は並行して両方の完了を待つ、0・負値・`uint` millisecond 上限超過は例外でなく即時最終状態、hook は UI thread 上で layout 済み View に対して開始、添付値は初回 native layout で採用され以後の変更は表示中の 1 枚へ効かない。特に `None` と `overlayDuration` は名前の不足とも重なり、現在の文面だけでは「演出なし」と結果配送時点を誤解し得る。
**推奨修正**: 長い内部状態機械は持ち込まず、上記の利用者が選択・実装時に必要な観察可能契約を短い箇条書きまたは表で補う。

### [🟡 Minor] Loading の合流・完了・前後関係の契約が不足している

**該当箇所**: `skills/en/ksdialogs-maui/references/loading.md:1-27`、`:155-185`（ja 対応箇所も同じ）
**問題点**: 1 process 1 display の合流、世代、進捗、action failure、style/options は概ね一致する。一方で、`ShowAsync` が「操作ブロックが有効になった時点」で戻り entrance 完了を待たないこと、合流中は最初の content が維持されること、`StartAsync` は最後の参加者だけが撤去まで待つこと、Loading は Dialog / Toast より常に手前でユーザー操作では閉じず背面へ touch を通さないこと、既定 Loading は custom transition を選べないことが書かれていない。これらは非同期処理の後続順序と UI の操作可能性に直接効く source contract である。
**推奨修正**: 合流の戻り時点、先頭 content、前後関係、非 dismiss、built-in / custom transition 差を、冒頭の合流説明と設定節へ簡潔に追加する。

### [🟡 Minor] Toast の失敗モデル・時間モデル・既定配置が不足している

**該当箇所**: `skills/en/ksdialogs-maui/references/toast.md:1-18`、`:47-100`（ja 対応箇所も同じ）
**問題点**: fire-and-forget、非対話、touch-through、上限 clamp なし、独立した多重表示は正しい。しかし登録経路の未登録 VM / 値型 VM は呼び出し時点で同期例外になること（`maui/KsDialogs.Maui.Tests/ToastFacadeTests.cs:38-59`）、受理後の factory / host 取付失敗は呼び出し元へ返らずその 1 枚だけを破棄すること、duration は受理時点から背面中も消費され host 不在のまま満了すれば未表示で破棄されること、契約既定配置が可視領域の下部中央 + 上方向 80 であること、Loading が常に前面であることが書かれていない。利用者は同期例外を捕捉すべき経路と、fire-and-forget 後に観測不能な経路を区別できない。
**推奨修正**: 3 段階の失敗モデルを短い表にし、受理基準の duration、既定配置、Loading との前後関係を設定節または冒頭へ追加する。

## アクションプラン

1. 公開 API の未掲載名を英日ペアで補い、handbook の掲載基準に対する手動照合を再実施する。
2. 契約 interface 3 種の MAUI DI 登録・注入レシピを追加し、`AddKsDialogs` の責務境界を明確にする。
3. transition / Loading / Toast の利用者に観察可能な契約を、各 source concept から簡潔に補う。
4. heading / code-byte / frontmatter / closed-world / link / identity / task 3e 検査を planned manifest で再実行し、変更した `ksdialogs-maui` の独立レビューを再実施する。

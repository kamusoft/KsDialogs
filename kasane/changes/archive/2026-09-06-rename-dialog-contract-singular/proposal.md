# Proposal: rename-dialog-contract-singular

## Why

Dialog の表示契約だけが複数形 (`KsDialogs`、MAUI は `IKsDialogs`) で、Loading (`KsLoading`) / Toast (`KsToast`) の「Ks + 機能名 (単数)」と非対称になっている。探索で経緯を追った結果、この複数形は縦串実装 (2026-08-14) で製品名がそのまま契約名に流用されたもので、命名を検討した記録も意図もなかった (core/ADR-0034、exploration.md)。一般公開前の今のうちに 3 機能の契約名を同じ規則へ揃える。

## What Changes

- **契約型の改名** (4 形態): Swift `protocol KsDialogs` → `KsDialog`、Android / KMP `interface KsDialogs` → `KsDialog`、MAUI `IKsDialogs` → `IKsDialog`。旧名は残さない
- **契約名を冠する実装・fake の追随**: KMP internal `GatewayKsDialogs` → `GatewayKsDialog`、KMP テストの `FakeKsDialogs` → `FakeKsDialog`。ファイル名も型名に合わせる
- **境界規則**: 製品名 `KsDialogs` を接頭辞に持つ識別子 (`KsDialogsOptions` / `KsDialogsKmp` / `AddKsDialogs` / `KsDialogsInterop*` / `KsDialogsMauiBridge` / `KsDialogsInstaller` / `KsDialogsInitializer` / テスト・Sample の target 名) と、モジュール名・Kotlin パッケージ・C# namespace・NuGet ID は変えない (cross/ADR-0005)
- **docs の追随**: concepts (core / ios / android / kmp / maui の api 面)・handbook・README 2 枚・skills (en / ja)・core/ADR-0002 の現行照合。skills は今回に限り docs-refresh を経由せず直接修正する (オーナー指示)
- **lint の前提逆転**: docs-refresh の禁止トークンにある `KsDialog([^sA-Za-z0-9_]|$)` (単数形 = 誤表記) を外し、代わりに「契約の文脈で `KsDialogs` / `IKsDialogs` が型名として残っていないこと」を残存検査で担保する

影響する能力: dialog-contract (core)、ios-native、android-native、kmp-facade、maui-binding、user-docs (skills / README)、docs-refresh lint

## Non-Goals

- Loading / Toast の契約名変更 — 既に単数で規則どおり
- 製品名由来の識別子の改名 — 製品名は cross/ADR-0005 の写像表で確定済みで、契約とは別の軸
- 既定エントリ `Dialog` / `Loading` / `Toast` の改名 — 原典踏襲 (core/ADR-0002) で意図された命名
- docs-refresh の禁止トークン lint 全体の見直し — 今回は単数形ルールの逆転だけ。他パターンは現行どおり
- 進行中 change `proofread-user-skills-ja` の skills/ 校正内容 — 別 change。本 change の skills 修正はその commit 後に重ねる (ファイル衝突回避)

## Impact

- **破壊的変更**: 公開 API の型名改名。一般公開前 (cross/ADR-0009 lockstep・版間互換なし) のため外部利用者への影響はない。samples / ApiSurfaceCheck / テストは全て追随する
- **挙動変更なし**: 型名以外のシグネチャ・意味論は変えない。既存テストは名前の追随だけで全件通る想定
- **リスク**: (1) MAUI の参照が最多で置換漏れが出やすい → ビルド + ApiSurfaceCheck (旧名の負のコンパイル検証を含む) で機械的に検出 (2) KMP iosMain の cinterop 面は `KsDialogsInterop*` (製品名) なので改名対象外だが、境界規則の読み違えで巻き込まないよう tasks に境界の明示を入れる (3) skills/ は proofread-user-skills-ja と同じファイル群を触るため、順序を守らないと conflict する

## 級: M

公開 API の破壊的改名が 4 形態 + lint + docs 一式に跨る (挙動変更なし・UI なし)。

domain: cross (実際に触るドメイン: core / ios / android / kmp / maui — 各 domain-skills を結合)

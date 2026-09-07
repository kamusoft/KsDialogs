# Exploration: move-maui-dialog-options-to-contract

## 課題 / 動機

MAUI の公開契約型 `DialogOptions` (record。`kasane/concepts/maui/api/layout-surface.md` と `loading-surface.md` が公開型として扱い、添付プロパティの静的メタ属性を束ねて Native へ運ぶ) が `maui/KsDialogs.Maui/Internals/DialogOptions.cs` に置かれている。他の public 契約型 (`DialogPlacement` / `DialogTransition` / `DialogLayoutArea` / `ToastStyle` 等) は `maui/KsDialogs.Maui/Contract/` にあり、ファイル配置だけが揃っていない。可視性 (public) と namespace は正しく、利用者から見た API は変わらない。

発見の文脈: split-concepts-platform-surface の docs-refresh による Skill 再生成ワーカーの報告 (同 change の deviation.md「契約と実装の乖離 (Skill 再生成のワーカー報告から)」)。蒸留 (2026-09-06) でオーナーが簡易起票を指示。

## 検討した選択肢 (却下案と理由を含む)

- **A: `Contract/` へファイルを移すだけ (採用)** — namespace は `KsDialogs` のフラット構成で配置と連動しないため、利用者の `using` と公開 API は変わらない。csproj は既定 glob で Compile 項目の修正も不要
- **B: `Internals/` に残す** — 却下。`Internals/` に置いた経緯 (add-layout-spec 時点で internal だった) は add-loading (core/ADR-0022) の public 化で失効しており、残す理由がない

## 決定事項

- `maui/KsDialogs.Maui/Internals/DialogOptions.cs` を `maui/KsDialogs.Maui/Contract/DialogOptions.cs` へ移動する (中身は変えない)
- 経緯 (2026-09-06 探索で確認): add-layout-spec (2026-08-19) では `DialogOptions` は internal で、review-003 #7 も「internal である根拠を残す」としていた。add-loading で `IKsLoading.Options` の供給経路として public 化されたが、ファイルだけ `Internals/` に残った
- `Internals/` 配下の public 宣言は `DialogOptions` の 1 件のみ (棚卸し済み)。他に揃えるべき型はない
- archive 以外にファイルパスを参照する文書・lint・Skill はない

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

なし (簡易起票時の疑問点 3 件は 2026-09-06 の探索で解消し、決定事項に記録)。

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

ファイル移動のみ。触る能力 1 つ、公開 API 変更なし、`git mv` 1 回で可逆、UI なし。提案は作らず直接実装 (移動 → ビルド → MAUI テスト)。

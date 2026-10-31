# Exploration: add-root-readme-language-links

## 課題 / 動機

ルートの `README.md` と `README_ja.md` に、もう一方の言語版へ移るリンクが無い。姉妹リポジトリの KsSettingsView は、ルート README の H1 の直下に言語切替のリンクを置いている (`README.md` に `[日本語](https://github.com/kamusoft/KsSettingsView/blob/main/README_ja.md)`、`README_ja.md` に `[English](https://github.com/kamusoft/KsSettingsView/blob/main/README.md)`)。KsDialogs も同じ形にそろえる。

## 検討した選択肢 (却下案と理由を含む)

- **リンク先を相対パス (`README_ja.md`) にする** — 却下。ルートの `README.md` は `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` の `PackageReadmeFile` で NuGet パッケージにも入るため、相対リンクだと nuget.org のパッケージページで切れる。KsSettingsView と同じ GitHub の絶対 URL (`blob/main`) にする
- **`README.md` 側だけに足す** — 却下。日本語版を開いた読者が英語版に戻れず、英日の形もずれる。KsSettingsView と同じく両方向にする
- **`skills/README.md` / `skills/README_ja.md` にも相互リンクを足す** — 今回は対象外。Skill 一覧の表に English / 日本語の列があり、言語版への導線はすでにある

## 決定事項

- `README.md` の H1 (`# KsDialogs`) の直下に空行を挟んで `[日本語](https://github.com/kamusoft/KsDialogs/blob/main/README_ja.md)` を置く
- `README_ja.md` の H1 の直下に空行を挟んで `[English](https://github.com/kamusoft/KsDialogs/blob/main/README.md)` を置く
- 対象はルート README の 2 枚だけ。見出しは増やさない (docs-refresh の英日の節構成一致検査に影響しない)
- docs-refresh の README 用プロンプト (`.agents/skills/docs-refresh/references/prompt-readme.md`) は変えない (KsSettingsView 側も言語切替リンクの規約を持たない)
- 手続き: CLAUDE.md の「README 群の構成の見直しは承認済み change の実装として行う」に当たるため、docs-refresh ではなくこの change の実装として行う

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

なし

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (ルート README 2 枚へのリンク 1 行ずつの追加。挙動・公開 API・コードに触れず、いつでも戻せる)

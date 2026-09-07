# レビュー結果: rollout-user-docs (006 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

README 群・`.github/`・画像・今回の長命層・docs-refresh の付随修正を、delta specs、承認済み UI brief、deviation、現行コード・ビルド設定、移送元 README と突き合わせた。英日ロックステップ、源泉完全性、配布座標、Setup、Issue Forms、画像の採用条件、閉世界性、文書構造・識別情報のいずれにも修正必須の問題は見つからなかった。

本判定は tasks グループ 6.1 の「README 群・`.github`・長命層」bundle に対するもの。レビュー後に行うグループ 7〜9 (`samples/` README の廃止と参照付け替え、manifest の最終書き出し、最終検査) の完了判定は含めない。現時点の `skills/.manifest.json` が bootstrap 状態であることと `samples/` README が残っていることは、design の Migration Plan に従う中間状態なので指摘にしない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — 常時適用
- `kasane/handbook/cross/test-execution.md` — ビルド・テスト実行と完了判定の規範 (本変更の全ビルド免除は proposal による)
- `kasane/handbook/cross/sample-parity.md` — 撮影支援契約、14 デモ ID、画像比較条件
- `kasane/handbook/cross/local-development-setup.md` — 4 Sample の参照方式・ビルド・起動手順
- `kasane/handbook/cross/user-skill-api-listing.md` — API 名網羅検査の仕分けと閉世界の境界

## 検査結果

### 仕様・源泉の完全性

- proposal / design / tasks / deviation、3 本の delta spec、承認済み `ui/brief.md`、関連する accepted ADR・concepts・handbook を節単位で照合した。
- 廃止予定の `samples/README.md` と 4 ルートの README が持つ情報をファイル単位で確認した。撮影支援契約は `sample-parity.md`、参照方式とビルド・起動手順は `local-development-setup.md`、KMP iOS の 3 点リンクと `integrateLinkagePackage` は `ios-host-integration.md` に到達可能であり、移送対象の欠落はない。
- `/tmp/docs-refresh-ksdialogs-manifest-planned.json` を使った `concepts-coverage-check.py` は `concepts coverage OK`。予定 `targets` の源泉割当と `excluded` を含め、未参照 concept はない。
- `toast-semantics.md` の既定エントリ修正は実装の `Toast.instance` / `Toast.Instance` と一致する。KMP iOS concept は phase-10 PoC、現行 Gradle/Xcode 構成、kmp/ADR-0002 と一致する。

### 英日ペア・README・配布情報

- `heading-parity-check.py`: `en/ja heading structure OK`。
- `code-block-parity-check.py`: `code blocks byte-identical`。ルート README の最小コード 4 組は各 platform Skill の最小コードブロックとも byte 一致した。
- `frontmatter-check.py`: `frontmatter OK`。`link-resolution-check.py`: `All internal links resolve`。
- ルート README は要求された 9 見出しを順番どおり持ち、冒頭の配信準備中表記は 1 箇所、0.x の API 安定性表記は独立している。開発者向けビルド手順はなく、リポジトリ構成は指定 8 行で `samples/` だけリンクを持たない。
- Android version catalog / Gradle wrapper 2 本 / `ios/Package.swift` / MAUI csproj を実読し、iOS 17、Android API 24、compileSdk 36、Swift 6.3、Kotlin 2.4.10、AGP 9.3.0、Gradle 9.7.0、.NET 10、Microsoft.Maui.Controls 10.0.1 と README の表が一致した。
- SwiftPM `KsDialogs-SPM`、Maven `jp.kamusoft:ksdialogs` / `ksdialogs-compose` / `ksdialogs-kmp`、NuGet `KsDialogs.Maui` は README と対応 Skill の Setup で一致した。Kotlin 最小版未確定、KMP SwiftPM 連携 Alpha、prerelease lockstep の説明も delta spec と一致する。
- `skills/README.md` / `README_ja.md` は 5 Skill、コピー手順、片言語だけを選ぶ前提の 3 要素を持ち、英日で意味が等価だった。

### スクリーンショット

- `assets/` の 6 枚は `ui/brief.md` が承認した iOS / Android × Dialog / Loading / Toast の採用元と SHA-256 が対ごとに一致した。
- 6 枚を原寸で目視し、Dialog は `basic-dialog`、Loading は 50% / `Soon...`、Toast は 3 枚完全表示で比較条件が揃い、status / navigation 領域や個体・個人情報を含まないことを確認した。
- 撮影 revision `53914ccb9571998245d044f5f927aa6acb71d477` は現行 HEAD と一致し、その revision から product / Sample コードに差分がない。iOS Toast の手動操作は deviation と brief の記録どおりで、同じ表示経路を使う合意済み差分である。
- 英日 README は同じ 6 画像を `main` の絶対 URL で参照し、2 列 × 3 行、言語別 caption になっている。暫定 branch 名は proposal / design の明示どおりである。

### Issue Forms・貢献導線

- 3 本の Issue Forms を YAML として読み、指定された必須 ID がすべて `required: true`、bug / question の Platform が完全に同じ 7 択、3 本とも英語・日本語可の案内を持つことを確認した。
- `config.yml` の `blank_issues_enabled` は `false`。CONTRIBUTING 英日は PR を受け付けず Issue で受ける理由、3 種の Issue の書き方、相互リンクを持ち、意味が等価だった。

### 閉世界性・付随修正・静的 lint

- 予定 manifest の 66 Skill ファイルについて、`kasane/`、ADR 番号、Skill ルート外の相対リンクが 0 件。全 70 target について機械面 `KsDialogsInteropBridge` / `KsDialogsInteropResultType` が 0 件だった。
- `[付随修正]` は内部用語・ADR の grep だけを `skills/*` に限定し、機械面の名前は全 target、相対リンク境界は Skill に適用したまま保つ。ルート README の必須 `kasane/` 行と concepts 導線だけを許可するため、repository-docs と既存 docs-refresh 契約の衝突を解消し、Skill の閉世界性を弱めていない。`prompt-readme.md` も同じ境界を明記している。
- planned target 70 ファイルと `.github/`、docs-refresh、変更した concepts / handbook に `local-path-lint.py` と `identity-lint.py` を実行し、違反 0 件。
- 変更した長命文書 5 本への `doc-structure-lint.py --paths` は `構造 lint: 違反なし`。全体の既存 261 件 / 42 ファイルは deviation に記録済みの baseline と一致する。
- Sample の 4 実装から安定デモ ID を抽出し、各 14 件で完全一致した。bundle id / package / activity、4 Sample の参照方式、コマンドの project / scheme / artifact パスも現行設定と一致した。

製品コード・テストの変更はなく、proposal がこの場合の全ビルド・テスト免除を明示しているため、ビルドは実行していない。

## 指摘事項

なし。

## アクションプラン

- 本 bundle に対する修正は不要。
- design の順序どおり tasks グループ 7〜9を進め、最終状態で README 集合、廃止参照 0 件、manifest hash、`--readme-only` 差分なしを別途確認する。

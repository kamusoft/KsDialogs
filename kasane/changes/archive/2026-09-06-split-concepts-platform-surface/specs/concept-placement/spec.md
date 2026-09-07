# concept-placement (concepts の配置契約)

## ADDED Requirements

### Requirement: core の契約は platform 固有の識別子を持たない

`kasane/concepts/core/api/` の各 concept は、観察可能な挙動と保証を記述する SHALL。バッククォート表記の識別子は、4 つの Skill 範囲 (iOS / Android / MAUI / KMP の 3 側) すべての公開面に同綴り (MAUI の interface 接頭辞 `I` は同綴り扱い、大文字小文字の違いは別綴り) で存在することを実装コードで確認し `verification/core-contract-check.md` に確定した共通概念名と、そのメンバー名のうち同条件を満たすものに限る SHALL。既定シングルトン・形態別の別名・添付の面・例外の詳細型・framework 型・コード例を core に置くことを禁止する (SHALL NOT)。承認済みの platform 差分の挙動 (多段表示で下段を先に閉じたときの上段の扱い、呼び出し元キャンセルの観察のされ方、Android の戻るボタン、leading / trailing のレイアウト方向追随など) は core に残し、識別子を含まない散文または識別子を含まない表で記述する SHALL。各 core concept は末尾に「形態別の公開面」節を持ち、対応する platform concept へリンクする SHALL。

#### Scenario: 識別子を含む形態別テーブルの不在

- **GIVEN** 本 change 適用後の `core/api/*.md`
- **WHEN** 先頭セルが「形態」「platform」または形態名 (iOS / Android / MAUI / KMP) で始まる表の行を検索し、各行のバッククォート識別子を数える
- **THEN** 識別子を含む行が 0 件 (識別子を含まない差分の表は残ってよい)

#### Scenario: 共通概念名以外の識別子の不在

- **GIVEN** 本 change 適用後の `core/api/*.md` と、`verification/core-contract-check.md` に確定した共通概念名の一覧
- **WHEN** バッククォート識別子をすべて列挙し (STOP 語と数値を除く)、一覧 (およびそのメンバー名) と突き合わせる
- **THEN** 一覧に無い識別子が 0 件

#### Scenario: platform 差分の挙動の残存

- **GIVEN** 着手時点の `core/api/*.md` にある platform 差分の記述を列挙した `verification/platform-differences.md` (少なくとも: 多段表示の下段先閉じ / 呼び出し元キャンセルの観察 / MAUI にキャンセル経路が無いこと / Android の戻るボタン / leading・trailing の START・END 対応 / MAUI の添付が code-behind 限定であること) と、本 change 適用後の core
- **WHEN** 各項目の記述箇所を分割後の core で探す
- **THEN** すべて core に残っており、識別子を含まない形で書かれ、具体形を持つ platform concept の節へのリンクがある

#### Scenario: 形態別の公開面への導線

- **GIVEN** 本 change 適用後の core/api の任意の concept
- **WHEN** 末尾の「形態別の公開面」節を読む
- **THEN** ios / android / maui / kmp の対応 concept (kmp に対応 concept が無い機能は共有コード側の面が無い旨を書いた `kmp/api/dialog-surface.md`) への相対リンクがあり、いずれも解決する

### Requirement: platform の公開面 concept

`ios/api/`・`android/api/`・`maui/api/` に `dialog-surface.md` / `layout-surface.md` / `transition-surface.md` / `loading-surface.md` / `toast-surface.md` の 5 本、`kmp/api/` に `dialog-surface.md` / `loading-surface.md` / `toast-surface.md` の 3 本を置く SHALL。KMP の Swift 向け公開面 (型付き登録入口・`result:` ラベル・factory のオーバーロード) は既存の `kmp/api/ios-host-integration.md` に集約する SHALL。各 concept は冒頭で対応する core の契約へリンクし、扱う範囲が当該 platform の公開面 (名前・署名・コード例・framework 固有の注意) に限られ挙動の契約は core が正である旨を宣言する SHALL。既存の `maui/api/di-registration.md`・`kmp/api/ios-host-integration.md` は維持する SHALL。frontmatter は `type: concept` と `title` / `description` / `tags` / `timestamp` を持ち、本文冒頭に title と同じ h1 を置く SHALL。

#### Scenario: 新設 concept の存在と冒頭宣言

- **GIVEN** 本 change 適用後の `kasane/concepts/`
- **WHEN** 上記 18 本のパスを確認し、各ファイルの冒頭段落を読む
- **THEN** 18 本すべてが存在し、冒頭段落に対応する core concept への相対リンクと公開面限定の宣言があり、frontmatter の必須フィールドが揃っている

#### Scenario: platform 側の識別子は自 platform のもの

- **GIVEN** 本 change 適用後の `<p>/api/*.md` (p = ios / android / maui / kmp) と `verification/forbidden-tokens.json` の当該 platform の禁止トークン集合
- **WHEN** 各 concept のバッククォート識別子を禁止集合と突き合わせる
- **THEN** 一致が 0 件

### Requirement: 公開名の着地

着手時点のコミット SHA を `verification/baseline.md` に記録し、その時点の `core/api/*.md` から抽出したバッククォート識別子 (STOP 語と数値を除く全部) を、期待する移動先 (core / ios / android / maui / kmp / architecture、複数可) つきで `verification/identifier-ledger.md` に列挙する SHALL。分割後、台帳の各識別子は期待する移動先の concept に出現する (着地) か、理由つきで「意図して落とした」と記録される SHALL。他 platform の concept や `excluded` の concept への出現は着地と数えない SHALL。未説明の差分は 0 件である SHALL。

#### Scenario: 台帳の突き合わせ

- **GIVEN** `verification/baseline.md` の SHA で採取した台帳と、本 change 適用後の全 concept
- **WHEN** `verification/identifier-landing.py` を実行する
- **THEN** 全行が「着地」または「意図して落とした (理由あり)」のどちらかで、未説明 0 件

#### Scenario: 小文字 API の保存

- **GIVEN** 台帳
- **WHEN** `show` / `hide` / `cancelled` / `leading` / `trailing` のような小文字の識別子を探す
- **THEN** 台帳に含まれており、それぞれ着地または理由つきの除外になっている

### Requirement: 検証機構の記述は architecture

レイアウトの共通ケース表 (`core/layout-spec/cases.json`) の形・ケース ID・`approvedDiff` による OS 差の統制の記述は `core/architecture/layout-case-table.md` に置く SHALL。`core/api/layout-semantics.md` は共通ケース表を単一の正とする旨の 1 文と当該 concept へのリンクだけを持つ SHALL。rect 決定手順の数式はインライン code ではなく fenced code block で記述する SHALL。

#### Scenario: ケース表の記述の移動

- **GIVEN** 本 change 適用後の `core/api/layout-semantics.md` と `core/architecture/layout-case-table.md`
- **WHEN** ケース ID (`C` + 2 桁) と `approvedDiff` / `approvedBy` の出現箇所を検索する
- **THEN** `layout-semantics.md` に出現せず、`layout-case-table.md` に出現する

#### Scenario: 数式の表記

- **GIVEN** 本 change 適用後の `core/api/layout-semantics.md` の「最終 rect の決め方」節
- **WHEN** API 名網羅検査と同じ抽出規則で識別子を列挙する
- **THEN** `A.min` / `A.max` のような数式断片が候補に含まれない

### Requirement: 配置規則と目次の更新

`kasane/concepts/rules.md` の配置判断に「挙動の契約は core、公開名・署名・コード例・framework 固有の注意は `<platform>/api/`」の基準を追記する SHALL。`core/index.md`・`ios/index.md` (新設)・`android/index.md` (新設)・`maui/index.md`・`kmp/index.md`・ルート `concepts/index.md` (ios / android の「まだ概念なし」を解消) を更新し、`concepts/log.md` に本 change の再構成を追記する SHALL。

#### Scenario: 目次からの到達

- **GIVEN** 本 change 適用後の `kasane/concepts/index.md`
- **WHEN** ドメイン地図から各ドメインの index を辿る
- **THEN** 新設・移動した全 concept が該当ドメインの index に 1 行説明つきで列挙され、リンクがすべて解決する

#### Scenario: 配置判断の基準

- **GIVEN** 本 change 適用後の `rules.md`
- **WHEN** 配置判断の節を読む
- **THEN** 「契約は core / 公開面は `<platform>/api/`」の基準と cross/ADR-0014 への参照がある

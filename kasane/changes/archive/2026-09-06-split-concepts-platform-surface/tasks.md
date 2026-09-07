# Tasks: split-concepts-platform-surface

前提: rollout-user-docs が archive 済みであること。concepts の分割は ksn-concept モード 3 (作庭) の規律 (リンクの張り直し・index / log 更新までを一続きで行う) に従う。

## 1. 着手前の採取

- [x] 1.1 着手時点のコミット SHA を `verification/baseline.md` に記録し、`core/api/*.md` のバッククォート識別子を全部 (STOP 語と数値を除く) 抽出して期待する移動先つきの着地台帳 `verification/identifier-ledger.md` を作る (`verification/identifier-landing.py`。移動先の初期値は形態別テーブルの行・節の platform 名から付け、実装者が確認する) (→ Requirement: 公開名の着地)
- [x] 1.2 core にある platform 差分の挙動の記述を `verification/platform-differences.md` に列挙する (→ Requirement: core の契約は platform 固有の識別子を持たない)
- [x] 1.3 現行除外リストの「対象 Skill 外・機械検査由来」72 行から platform 別の禁止トークン集合 `verification/forbidden-tokens.json` の初期値を作る (→ Requirement: API 名網羅検査の候補から他 platform 名が消える)

## 2. concepts の分割 (機能単位で 5 巡。各巡で core 書き直し + platform 新設 + doc-structure lint)

- [x] 2.1 dialog: `core/api/registration-show-semantics.md` / `result-notification-semantics.md` / `multi-display-semantics.md` / `model-binding-semantics.md` を platform 固有の識別子を持たない文に書き直し、識別子を含む形態別テーブルとコード例を外して末尾に「形態別の公開面」節を置く。platform 差分の挙動 (下段先閉じ・キャンセルの観察・戻るボタン) は識別子なしで core に残す (→ Requirement: core の契約は platform 固有の識別子を持たない)
- [x] 2.2 dialog: `ios/api/dialog-surface.md` / `android/api/dialog-surface.md` / `maui/api/dialog-surface.md` / `kmp/api/dialog-surface.md` を新設し、core から外した自 platform の公開名・署名・コード例・注意を core の節順で配置する。kmp は共有コード側に layout / transition の添付の面が無い旨と ios / android の該当 concept へのリンクを持つ (→ Requirement: platform の公開面 concept)
- [x] 2.3 dialog: core の形態別テーブルの「KMP (Swift 向け公開面)」行 (`result:` ラベルの結果型指定・`UIView` / SwiftUI factory のオーバーロード) を `kmp/api/ios-host-integration.md` の「Swift 側で登録するもの」節へ移す (→ Requirement: platform の公開面 concept)
- [x] 2.4 layout: `core/api/layout-semantics.md` を書き直し (形態別の添付の面テーブル・`UIView` 等を外す、数式を fenced code block に)、「共通ケース表と OS 差の統制」節を `core/architecture/layout-case-table.md` へ移す (→ Requirement: core の契約は platform 固有の識別子を持たない / 検証機構の記述は architecture)
- [x] 2.5 layout: `ios/api/layout-surface.md` / `android/api/layout-surface.md` / `maui/api/layout-surface.md` を新設する (→ Requirement: platform の公開面 concept)
- [x] 2.6 transition: `core/api/transition-semantics.md` を書き直す (添付の面テーブル・フックの型テーブル・コード例を外す。呼び出し元キャンセルの観察と leading / trailing の方向追随は識別子なしの表・散文で残す) (→ Requirement: core の契約は platform 固有の識別子を持たない)
- [x] 2.7 transition: `ios/api/transition-surface.md` / `android/api/transition-surface.md` / `maui/api/transition-surface.md` を新設する (→ Requirement: platform の公開面 concept)
- [x] 2.8 loading: `core/api/loading-semantics.md` を書き直し (契約名 / 既定エントリの 4 綴り・`LoadingCoordinator` 等を外す)、`ios/api/loading-surface.md` / `android/api/loading-surface.md` / `maui/api/loading-surface.md` / `kmp/api/loading-surface.md` を新設する (→ Requirement: core の契約は platform 固有の識別子を持たない / platform の公開面 concept)
- [x] 2.9 toast: `core/api/toast-semantics.md` を書き直し (公開面テーブルを外す)、`ios/api/toast-surface.md` / `android/api/toast-surface.md` / `maui/api/toast-surface.md` / `kmp/api/toast-surface.md` を新設する (→ Requirement: core の契約は platform 固有の識別子を持たない / platform の公開面 concept)
- [x] 2.10 既存の `maui/api/di-registration.md` と `kmp/api/ios-host-integration.md` の「関連」節から新設 concept へリンクを張る (本文は変えない) (→ Requirement: platform の公開面 concept)

## 3. 配置規則・目次・履歴

- [x] 3.1 `concepts/rules.md` の配置判断に「契約は core / 公開面は `<platform>/api/`」の基準と cross/ADR-0014 への参照を追記する (→ Requirement: 配置規則と目次の更新)
- [x] 3.2 `core/index.md` (architecture/ 節の新設を含む)・`ios/index.md` (新設)・`android/index.md` (新設)・`maui/index.md`・`kmp/index.md`・ルート `concepts/index.md` を更新する (→ Requirement: 配置規則と目次の更新)
- [x] 3.3 `concepts/log.md` に再構成 (書き直し 8 本・新設 19 本・移動 1 節・`ios-host-integration.md` への集約) を追記する (→ Requirement: 配置規則と目次の更新)
- [x] 3.4 core/ADR-0009 (レイアウト検証のケース表) など長命層から `layout-semantics.md` の当該節を指す参照が無いか grep し、あれば `layout-case-table.md` へ付け替える (→ Requirement: 検証機構の記述は architecture)

## 4. 分割の検証

- [x] 4.1 `verification/identifier-landing.py` で着地台帳を突き合わせ、全行を「着地」または「意図して落とした (理由)」にし、未説明 0 件を確認する (→ Requirement: 公開名の着地)
- [x] 4.1b `verification/platform-differences.md` の各項目が core に識別子なしで残り、platform concept へのリンクを持つことを確認する (→ Requirement: core の契約は platform 固有の識別子を持たない)
- [x] 4.2 design.md Decision 2 の規則で共通概念名の確定一覧を `verification/core-contract-check.md` に書き、`core/api/*.md` に形態別テーブルと一覧外の識別子が無いことを検査して結果を同ファイルに残す (→ Requirement: core の契約は platform 固有の識別子を持たない)
- [x] 4.3 `verification/forbidden-tokens.json` に新設 surface 由来の他 platform 識別子を加え、各 platform concept に禁止トークンが無いことを検査する (→ Requirement: platform の公開面 concept)
- [x] 4.4 doc-structure / local-path / identity lint を `kasane/concepts` 全体に掛ける (→ Requirement: 配置規則と目次の更新)

## 5. manifest と Skill の再生成

- [x] 5.1 `skills/.manifest.json` を書き換える: `concepts` (30 本)、`targets` (design.md Decision 6 の完成形 33 キー)、`excluded` (2 本、理由つき)、`readmes` (変更なし) (→ Requirement: manifest 初期版)
- [x] 5.2 docs-refresh を `--all` で起動し、Step 4 の提示をオーナーが承認して 5 Skill × en/ja と README 4 枚を再生成する (→ Requirement: 源泉の再構成に伴う再生成)
- [x] 5.3 再生成前後の `skills/{en,ja}/` のファイル集合が一致することを確認する (→ Requirement: 源泉の再構成に伴う再生成)
- [x] 5.4 docs-refresh Step 6 の整合性チェック一式と identity / local-path lint を実行し、manifest のハッシュを最終状態に一致させる (→ Requirement: 源泉の再構成に伴う再生成 / manifest 初期版)

## 6. 除外リストの組み直し

- [x] 6.1 再生成した Skill (en / ja) と `api-coverage-check.py` の候補の両方に禁止トークンが無いことを検査し、結果を `verification/api-coverage-after.txt` に残す (→ Requirement: API 名網羅検査の候補から他 platform 名が消える)
- [x] 6.2 報告された候補を「掲載漏れ (Skill を修正)」「実判断の除外 (旧リストの理由を引き継ぐ)」「非 API token」に仕分け、掲載漏れ分は docs-refresh の項目指定で Skill を修正する (→ Requirement: API 名網羅検査の候補から他 platform 名が消える)
- [x] 6.3 ksn-concept の経路で `handbook/cross/user-skill-api-listing.md` の現行除外リストをゼロから書き直し (Decision 4)、`timestamp`・handbook index・`concepts/log.md` を更新して構造 lint を通す (→ Requirement: 利用者向け Skill の API 掲載基準)
- [x] 6.4 docs-refresh SKILL.md の 3e 注記 (既知の誤検出源 ①「他プラットフォームのトークン」) を、分割後は原則発生しない旨に改める (→ Requirement: 利用者向け Skill の API 掲載基準)

## 7. 仕上げ

- [x] 7.1 cross/ADR-0014 (proposed) の Decision 節に design.md Decision 1〜3 の内容が反映されていることを確認する (蒸留時の accepted 昇格の準備) (→ design.md ADR 候補)
- [x] 7.2 lint 一式 (local-path / identity / doc-structure) と `git diff --check` を通す
- [x] 7.3 見つかった契約と実装の乖離・付随修正を deviation.md に記録する

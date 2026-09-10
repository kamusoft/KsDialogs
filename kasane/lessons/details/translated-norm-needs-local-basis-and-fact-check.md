---
scope: spec-review
kind: pain
severity: normal
count: 3
first-seen: 2026-09-04
last-seen: 2026-09-08
evidence:
  - add-maui-nuget-distribution (design Decision 1 が翻案元 KsSettingsView の実測「.NET Android SDK が自 assembly 用 aar を生成し nupkg に入って利用者側で XA4301」を KsDialogs でも成り立つ前提として採録し、除去の後処理 (`KsExcludeGeneratedAarFromPackage`) と中身の検査を写した。代替案「消費者検証で観測してから」は『翻案元で確定済みで観測を待つ理由がない』と却下したが、実装で pack すると KsDialogs では aar が Release / Debug のどの出力にも生成されず、後処理は `Exists` 条件が不成立で一度も走らない (deviation.md)。許可パターンも未評価のまま休眠ターゲットとして残った (review-001 Suggestion 1)。翻案先で一度 pack して `ls bin/` すれば採録前に分かった)
  - adopt-docs-refresh (tasks 2.2 が翻案元 KsSettingsView の「platform / Sample ディレクトリに README を置かない」規範を「KsDialogs の phase-2 踏襲決定への参照に直す」と指示したが、その決定は phase-1 agenda / history に存在せず、しかも `samples/README.md` 等 6 本の README が実在し `kasane/config.yaml` の `ui.screenshot` がそれを正として参照している。実装ワーカーが停止報告形式で発見し、規範を落として追従対象の範囲だけを述べる形に倒した)
  - add-verification-ci (proposal Non-Goals / tasks 1.2 が翻案元 KsSettingsView の「Sample は `MauiVersion` 直書きのまま」を「Sample の csproj は触らず、解決版は推移的に 10.0.70 へ上がる」と読み替えて採録したが、翻案元の Sample は csproj に `<MauiVersion>10.0.70</MauiVersion>` を明示しており、KsDialogs の Sample は未明示 (workload set 既定の 10.0.20)。NuGet は推移参照の上位版へ持ち上げず NU1605 で restore が失敗する。実装ワーカーが停止報告形式で発見)
---

## ルール文

姉妹プロジェクトの文書を翻案する提案・design・tasks が、翻案元の規範 (「〜を置かない」「〜は禁止」) や翻案元の実測に基づく前提 (「SDK が〜を生成する」「〜が失敗する」) を翻案先にも書かせるとき、(1) 規範ならその根拠となる決定が翻案先の decisions / agenda 決定事項に実在するか、(2) 規範・前提とも翻案先の現状 (該当ファイルの実在・config からの参照・ビルド出力) と矛盾しないかを、`ls` / `grep` / 1 回のビルドの最小確認で確かめてから採録する。根拠が無い・現状と矛盾する規範は翻案先の記述から落とす (追従対象の範囲だけを述べる) か論点として agenda に立ててから書き、翻案先で成立が確認できない前提に基づく機構は「翻案先で観測してから足す」を既定にする。未成立の決定 (「phase-N で決まる予定」) を根拠に規範を書かない。守れたかは、design / tasks の当該箇所に翻案先での確認手段 (コマンド・ファイル) が添えてあることから判定する。

## 経緯

- 2026-09-08 add-maui-nuget-distribution: 3 件目。規範ではなく翻案元の実測に基づく前提 (自 assembly 用 aar の生成) を、翻案先で確かめずに機構ごと写した。害は休眠ターゲット 1 つと未評価の許可パターンに留まったが、代替案「観測してから足す」を却下した理由 (翻案元で確定済み) が翻案先では成り立っていなかった

- 2026-09-08 add-verification-ci: 翻案元の「`MauiVersion` 直書きのまま」は「プロパティ経由の書き方を保つ」の意味で、翻案元 Sample は値 10.0.70 を明示していた。翻案先では Sample が値を持たないため「推移的に上がる」という前提が NuGet の解決規則 (直接参照が下位なら NU1605) と矛盾し、`samples/maui` の restore が失敗した。翻案元の該当ファイル 1 本を開くか `dotnet msbuild -getProperty:MauiVersion` を 1 回打てば採録前に分かった

- 2026-09-04 adopt-docs-refresh: 翻案元の SKILL.md にあった README 設置の禁止規範は KsSettingsView の cross/ADR-0023 が根拠だったが、KsDialogs には対応する決定が無い。tasks は「phase-2 踏襲決定への参照に直す」と書いたが phase-2 はまだ議論されておらず、samples/ 配下の README は現役文書だった。近縁: [[handoff-item-not-checked-against-evidence]] (写す項目を現在の証跡と突き合わせる) / [[tool-premise-decision-needs-existence-probe]] (前提の存在プローブ)

- 2026-09-10 add-release-workflow (昇格後の観測、count 対象外): 翻案元の実測 (publish 11 分) を根拠にした公開待ちの上限 30 分を運用値としてそのまま写し、初回リリースで Android 枠の Central 同期 (約 60 分) に足りず失敗した (同じ run の再実行で整合し、fix-release-published-wait で 90 分・並行待ちへ)。design は「timeout は翻案元のまま置き初回の実測で詰める」と明記していたので判断としては意図的だが、ルール文の「翻案元の実測に基づく前提」には運用値 (上限・間隔) も含まれる

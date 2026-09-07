# phase-2-docs-rollout 議論履歴

## 2026-09-04: 論点の整理と、公開 API の両入口の使い分けの置き場所 (論点 1 前半)

- 論点数 10 で分割トリガー (8 個以上) が発火。サブフェーズに割っても README の最小コード例が Skill と逐語一致 (lint) のため独立完了できず、テーマもゴール内 → 出口は agenda の整理 (論点統合) とし、Toast 対応表 + throws 化の記載 / `@Throws` 2 件 / iOS 側手順 + リンク構成を統合して 7 件に
- 論点 1 の前提「原典 README を一次情報源」は phase-1 決定 (正は concepts) と衝突するため落とした。Skill の内容構成と README 節構成は踏襲で決定済みのため、KsDialogs 固有の残りは「両入口の使い分けの置き場所」と「README の固有節」
- 選択肢: A. README は既定エントリのみ、使い分けは Skill 冒頭 1 段落、DI レシピは MAUI / KMP だけ references / B. 全 4 Skill に DI レシピ節 / C. README にも使い分けを書く。判断軸は段階開示・追従できる源泉・閉世界性・README の純度
- 採用: A。理由は源泉 concepts が揃う範囲と一致し、iOS / Android の DI は利用者側の道具で源泉も糖衣もないため。ADR は起票しない (Skill 本文の書き分けで覆せる・境界と将来を縛らない。両入口の説明コストは core/ADR-0002 Consequences の既知事項)

## 2026-09-04: README の対応プラットフォーム表の中身 (論点 1 中盤)

- インストール座標は cross/ADR-0008 の表で確定済みのため判断なし。残りは対応プラットフォーム表と スクリーンショット
- 選択肢: A. 最小 OS + ビルドに使った toolchain の 3 列 + 利用側下限の注記 (翻案元と同形) / B. 最小 OS だけ / C. 表を置かない。判断軸は訪問者の判断可能性・3d の突合・版の混同・KMP の Kotlin 範囲が phase-7 待ちである影響
- 採用: A。3d の取得元 4 行 (phase-1) を活かせる・4 形態では「自分の環境で入るか」が最初の関心。KMP の Kotlin 範囲は暫定値の印つきで phase-7 に埋めてもらう。ADR は起票しない (表の列構成で覆すコストが低い)

## 2026-09-04: README のスクリーンショット (論点 1 後半、論点 1 完了)

- 翻案元は提案化途中の追加要望で 4 枚 (iOS / Android × Modern / Classic) を `assets/` に置き絶対 URL で参照。KsDialogs は Sample のデモ駆動モード (安定デモ ID 14 件) で機械的に撮れる
- 選択肢: A. 6 枚 (iOS / Android × Dialog / Loading / Toast) / B. 4 枚 (Loading は文で) / C. 載せない。判断軸は 3 機能 × Native 土台の可視化・README の長さ・撮影コスト・識別情報リスク・public 化前に解決できないもの (URL のブランチ名)
- 採用: A。URL のブランチ名は phase-3 へ申し送り (TODO 追加)。ADR は起票しない

## 2026-09-04: Reduce Motion の扱い (論点 2)

- 事実確認: コード 4 形態とも設定を参照せず、concepts / ADR に言及なし。「`none` を添付」の案内は中身側だけで覆いと既定 Loading / Toast には届かない (PB-TR-17・各「持たない機能」)
- 提示した選択肢 (A. 事実を文書化し自動縮退は簡易起票 / B. 案内のみ / C. 同梱) に対し、オーナーは「無視してよい・文書で言及しない・導入予定なし」と判断。論点は取り扱わないで閉じ、ADR は起票しない

## 2026-09-04: 移行 Skill の内容 — Toast 対応表と throws 化の記載 (論点 3)

- 事実: 未リリース (tag / CHANGELOG なし) のため iOS throws 化 (ADR-0033) の breaking に読者なし。Toast の移植元差分は toast-semantics に 4 箇所あり源泉が揃う。翻案元の api-mapping.md は内容クラス別の節 + 「対応先のないメンバー」節
- 選択肢: A. throws 化は落とし Toast は対応表 1 節 2 行 / B. throws 化を初回リリースノートへ / C. Toast は「対応先なし」1 行。判断軸は読者適合・源泉・phase-1 決定との整合
- 採用: A。ADR は起票しない

## 2026-09-04: KMP の `@Throws` 注意書きの置き場所 (論点 4)

- 事実: 注意書きの本文も線引きも result-notification-semantics「KMP→Swift 境界の補足」に既にあり (fix-kmp-ios-unhandled-exception-crash 由来)、KMP Sample の共有コードは全関数に宣言済み。決めるのは置き場所だけ
- 選択肢: A. 最小コードに宣言 + 本文 3 行 + 詳細は references / B. 本文に段落で詳しく / C. references だけ。判断軸はクラッシュ回避の確実さ・段階開示・源泉との対応
- 採用: A。ADR は起票しない

## 2026-09-04: KMP 利用者の iOS 側手順 (論点 5)

- 事実: `KotlinMultiplatformLinkedPackage` は KGP の SwiftPM 連携が `integrateLinkagePackage` で生成する合成パッケージ (手書き不要)。消費者側の SwiftPM 再宣言は不要 (発行 metadata で推移)
- 事実 (続き): 「手動 1 点」は登録 API 用の `KsDialogs-SPM` 参照。kmp concepts は未整備で Skill の源泉がない
- 選択肢: A. 前提 1 + 手順 3、kmp concept を新設して源泉に / B. concept なしで Sample README と PoC を出典に / C. phase-7 へ先送り。判断軸は追従・phase-1 原則・公開時の完成度・追加作業
- 採用: A。TODO に concept 新設 (ksn-concept) と phase-7 待ち項目を追加。ADR は起票しない

## 2026-09-04: `samples/` 配下 README の位置づけ (論点 6)

- 当初提示: A. 現状維持 + 4 枚からリンクしない (推奨) / B. ルート README からリンク / C. 廃止して移送。A を推した根拠は「cross/ADR-0010 が samples/README を正の片割れと定めている」
- オーナー是正: ADR は決定時の記録であって規約ではない。ADR を改訂して handbook へ移送するのが SSOT 的に良い → C で確定
- 移送先の割り当て (A 捨てる / B 撮影引数 → sample-parity.md / B' 参照方式とビルド → local-development-setup.md / C KMP iOS 統合 → 論点 5 の kmp concept / D fixtures README は対象外) を推奨どおり採用。ADR-0010 は accepted だが本文の直接修正をオーナーが許可
- 教訓: 「既存 ADR が定めている」を案を落とす理由にしない (メモリ adr-is-record-not-rule に保存)

## 2026-09-04: Issue Forms の Platform 選択肢 (論点 7、全論点完了)

- 翻案元は 3 本 (バグ / 提案 / 質問)・blank 無効・Platform 4 択。KsDialogs で変わるのは Platform だけ
- 選択肢: A. 形態 × ホスト OS の 7 択 / B. 4 形態 + Multiple の 5 択 / C. 形態とホストの 2 dropdown。判断軸は triage の精度・報告者の負担・見た目
- 採用: A。ADR は起票しない。これで phase-2 の論点は空になり、残る TODO は kmp concept 新設 (change の前提)・3e の仕分け基準の handbook 起こし・change への同梱事項・phase-3 / phase-7 待ちの申し送り・ksn-propose

## 2026-09-04: 提案化 (ksn-propose、rollout-user-docs L 級) と相方 spec-review の反映

- change `rollout-user-docs` を L 級で作成 (proposal / design 7 Decision / specs 3 能力 / tasks 35 / ui/brief)
- 相方 (codex) の spec-review は NEEDS_DISCUSSION、Major 6 / Minor 3 をすべて採用して反映 (証跡: `kasane/changes/rollout-user-docs/second-opinion-spec-001.md`)
- 決定事項の具体化 1 件: 対応プラットフォーム表の「KMP 行の Kotlin 範囲は暫定値の印」は、消費者下限の決定元が無いため「値を推測せず確定前と明記 (Android Native の Kotlin 最小版も同じ扱い、phase-7 で確定)」とした

# phase-8-consumer-verification 議論履歴

## 2026-09-09: 論点 1 KMP 消費者の置き方と macOS ランナーの所要時間

phase-7 の C1 / C2 (dry-run は `file://` + exact、検証範囲は Android `assembleRelease` → framework リンク → `xcodebuild` の 3 段) を実装する消費者の置き場を議論した。選択肢は A: `verification/kmp/` 1 つ・macOS 1 job で 3 段を順に通す / B: Android 消費者と iOS 消費者を別ディレクトリ・別 job / C: 置き場 1 つのまま workflow 内で Android job (Ubuntu) と iOS job (macOS) に分ける。判断軸は消費者としての忠実度・フィード準備の置き場 (全 publication の実解決には iOS publication の cinterop klib のため macOS が必須)・必須 check 名 (`consumer-<platform> / verify`)・macOS ランナーの所要時間・最小例の複製 (論点 4)・失敗の切り分け。A を採用: フィード準備が macOS 必須なので B / C は artifact の受け渡しが増えるだけで占有時間は減らず、1 プロジェクトなら shared モジュールと依存宣言の複製もなく check 名も 1 つで済む。public リポジトリで macOS ランナーは無料、他の消費者 job と並走するため壁時計の見込み 12〜18 分は許容。timeout は 30 分を初期値にし着手時に実測する。ADR は起票しない (CI 側の可逆な構成判断で選別 3 基準に当たらない)。

## 2026-09-09: 論点 2 Android 消費者の参照形

選択肢は A: Compose 系 `jp.kamusoft:ksdialogs` 1 行の app だけ (翻案元どおり) / B: Compose 系 app と `ksdialogs-core` 単独 app の 2 モジュールを 1 つの Gradle プロジェクト・1 job で回す / C: `-core` 単独の app だけ。判断軸は README の導入例 2 通り (cross/ADR-0019) の網羅・推移で本体が同版で届く検査 (cross/ADR-0009)・「core に Compose が混ざらない」保証の検査・最小例の同梱箇所・追加コスト。B を採用: KsDialogs は 2 artifact を維持するため利用者の書き方が 2 通りあり、A では `-core` 単独の解決と Compose 非混入を直接確かめられない。追加コストはモジュール 1 つと Release ビルド 1 回で job・check 名は増えない。最小例は共有ソースディレクトリに 1 か所。README の Android 導入例が ADR-0019 以前の名前のまま (docs-refresh 待ち) である点は論点 4 で扱う。ADR は起票しない (検証構成の判断で、ADR-0019 の保証を検査で裏取りする形)。

## 2026-09-09: 論点 3 MAUI 消費者の timeout とトリガー

トリガーは phase-4 の決定事項 (`main` 宛て PR で消費者検証を dry-run で足す、cross/ADR-0028 の逆流) と踏襲の決定事項 (release の dry-run / smoke) で既に決まっていることを確認し、phase-8 では追加判断をしないことにした。残る MAUI の timeout は 40 分 (翻案元どおり) / 30 分 (他の消費者 job と揃える) / 60 分 の 3 案を、実測 20 分見込みに対する余裕・ハング時の占有・本体 `maui / verify` の 35 分との整合で比較し 40 分を採用。`main` 宛て PR で macOS job が 6 つ並び同時実行上限 5 で待ちが出る点は実測時に記録する申し送りとした。ADR は起票しない。

## 2026-09-09: 論点 4 README 最小コード例の逐語同梱と lint の範囲

翻案元 `readme-example-lint.py` (英語 README の最小例の節だけ、対応表で完全一致、`README_ja` は対象外) の形を確認し、README の最小例 4 つが現行 API と対応が取れていることを grep で突き合わせた。選択肢は A: README の既存 4 ブロックだけを lint し、KMP のホスト側 (Swift / Android の View 登録) は concepts の例から書いて lint 対象外 / B: A + スキルの iOS 登録例も lint / C: README に KMP のホスト側の例を足して 6 ブロックを lint。判断軸は lint が守るもの・docs-refresh との結合・スコープ・C2 (3) の担保。A を採用: README の最小例は入口でありホスト側の登録はスキルの分担 (cross/ADR-0011・0012)、README の構成変更は phase-8 のスコープ外で phase-9 の docs-refresh に申し送る。運用は翻案元と同じく「lint が赤になったら docs-refresh の依頼者が消費者側も直す」。ADR は起票しない。これで論点は空になり、TODO は提案化のみ。

## 2026-09-09: 提案化 (ksn-propose) で追加された判断

change `add-consumer-verification` (L 級) の作成中に 2 件を決めた。(1) KMP 消費者の生成物 (linkage package と `VerificationApp/Package.swift`) の扱い: 自己レビューで concepts `kmp/api/ios-host-integration.md` の禁止事項 (生成物を無視しない) との衝突を検出し、A: 追跡しない / B: smoke 形 (https + exact) で追跡し消費者ビルドは作業コピーで再生成 / C: 追跡したまま `verification/kmp/` 内で再生成 を比較して B を採用 (design Decision 4)。相方レビュー (second-opinion-spec-001、codex) の指摘で、追跡物は合成 version の tag が公開されないため「構造確認用の非解決 fixture、ビルド前の再生成が必須」と位置づけを明記した。(2) README 最小例の一致検査を lint job に足すと cross/ADR-0021 (7 検査に固定) の改訂になる: 改訂案 (amends ドラフト起票) と従う案 (消費者 job 内で検査、`main` 宛て PR でしか走らない) を並べ、改訂案を採用して cross/ADR-0022 を proposed で起票。相方レビューはほかに MAUI 本体の版 (スペックに写した 10.0.70 は古く、現行は maui/ADR-0004 の workload 同梱版 10.0.20。cross/ADR-0018 との accepted 同士の衝突は蒸留で amends)、smoke / artifact / フィード準備の組み合わせ表、smoke 正ケースの Scenario 削除 (phase-9 で立てる)、Android SDK の引き継ぎ、KMP 検査の自己テスト、artifact の配置表を指摘し、9 件すべて採用した。

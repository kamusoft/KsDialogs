# Deviation: add-vertical-slice

実装フェーズ中にオーナー合意した spec との乖離。spec 本体は書き換えない (逆流修正禁止)。

- kmp-facade / Scenario「Swift から await した結果の型と値が正しい」: spec では Swift コードから KMP の show を直接 await する実証を要求 → 指示により縦串では未実証のまま phase-5 へ送る。理由: ダイアログを出す主役は共有コードであり、Swift 向け KMP 面 (View 登録経路・show・型付き結果) は一体の設計テーマとして phase-5 の API 表面確定で扱うべきため。実測済みの粗 (ObjC 境界でジェネリクス消失・sealed 網羅性喪失) も同テーマの入力とする。Scenario 自体の意味の再検討 (spec の先走り) を蒸留へ申し送る (2026-08-15)
- samples / Requirement「Sample から本体の内部実装を直接参照しない」: spec では公開 API のみの消費者境界を要求 → 指示により samples/kmp iosApp の `KsDialogsInteropBridge` 直接使用を縦串の暫定として合意。理由: KMP iOS 消費者向けの型付き公開登録経路が未設計 (構造的ギャップ)。登録経路の正式設計は phase-5 (上記の Swift 向け KMP 面と一体) (2026-08-15)
- dialog-contract / 多段表示 MD-b「下のダイアログを先に閉じる」: シナリオ表では期待値未確定の調査ケース → 指示により「**OS 差として明文化する**」方針に確定。iOS = 上下とも画面から消え、上の show は cancelled で確定 / Android = 下だけ閉じ、上は残って後から正常完了。両 OS 共通の保証は「各 show の結果が必ずちょうど1回返ること」で、下から閉じたときの見え方は platform 依存とする。前提の確認: MD-b はアプリコードが下の notifier を保持してプログラム的に報告した場合にのみ成立する (ユーザー操作だけでは起きない)。蒸留で multi-display-semantics.md への追記 + ADR 起票を行う (2026-08-15)

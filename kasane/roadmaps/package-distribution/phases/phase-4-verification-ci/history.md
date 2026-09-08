# phase-4-verification-ci 議論履歴

## 2026-09-08: KMP の検証 job の形と metadata compile

論点の番号を確定 (1: KMP job / 2: MAUI の Xcode・workload 固定 / 3: 実行件数の下限 / 4: `develop` の必須 status check)。翻案元 KsSettingsView には KMP 形態が無く、ここだけ翻案元が無い。

- 選択肢: A) macOS ランナー 1 job で androidHostTest + iosSimulatorArm64Test + metadata compile を全部 / B) Ubuntu (Android host) + macOS (iOS) の 2 job / C) Ubuntu のみ
- 採用: A。metadata compile はライブラリ側 job に直接載せる
- 理由 (保証範囲): 決定事項「緑 = ロジック全件 + native への配線がコンパイルできる」に対し、KMP の配線は iOS ターゲット (`localSwiftPackage` + framework リンク) そのもの
- 理由 (検査の担い手): iOS Simulator のテストは ObjC 公開面検査と負のコンパイル検証も担う (handbook cross/test-execution.md、実測 151 件 = iOS 75 + Android host 76、2026-09-07)。public 化で macOS ランナーは無料
- 理由 (metadata compile の置き場): phase-8 の消費者検証へ寄せると `main` 宛て PR でしか走らず `develop` push で見落とす
- ADR: 単独 ADR は起こさない。CI 保証範囲の KsDialogs 側 ADR 化 (cross/ADR-0026 の翻案) は roadmap の前提どおり蒸留時に決める

## 2026-09-08: MAUI job の Xcode 版と .NET workload 版の固定

- 発見: リポジトリ内に `global.json` が無く、親ディレクトリの `../global.json` (SDK 10.0.101 / workload set 10.0.101 = .NET for iOS 26.1.10502、推奨 Xcode 26.1) を暗黙に拾っていた。library-foundation の「Xcode 26.5 でビルド不可」はこの版の組の帰結
- 選択肢: A) KsSettingsView と同じ SDK 10.0.300 / workload set 10.0.300.3 (.NET for iOS 26.5.10284) / Xcode 26.5 / B) 10.0.101 を固定し Xcode 26.1 / C) `global.json` なしでランナー任せ
- 採用: A。repo 直下に `global.json` を新設。`Microsoft.Maui.Controls` は KsSettingsView と同じ 10.0.70 に固定 (オーナー指示)
- 理由: CI で緑の実績がある唯一の組。iOS Native job と Xcode 版が一致し verify-maui.yml を読み替えなしで逆流できる

## 2026-09-08: 実行件数の下限と CI に載せるテストルートの範囲

論点 3 を「実行件数の下限」から「下限 + CI に載せるテストルートの範囲」に広げた。KsSettingsView に無いテストルートが 3 つある (MAUI 橋渡し JVM 31 件 / MAUI 橋渡し XCTest 7 件 / Android instrumented 333 件)。

- 下限: 踏襲 (0 件 = fail、固定数値なし)。単位は Android = module × variant、KMP = ターゲットごと、iOS / MAUI = 合計
- MAUI 橋渡し 2 本: maui job に載せる (ロジックのテスト、コスト小)
- Android instrumented: 選択肢は 載せない / 別 job で API 36 の 1 台 / API 29 + 36 の 2 台。採用は別 job で 1 台。理由は JVM では走らない演出・配送順序・システムバーの Scenario を CI が見ること、Emulator 由来の不安定さを別 job に隔離できること。API 29 の 6 本は手元に残す
- 採用: 3 点セットをそのまま (オーナー了承)

## 2026-09-08: `develop` の必須 status check 登録

- 発見: phase-3 の申し送り「`develop` に必須 status check を登録」は翻案元 cross/ADR-0028 (accepted) の却下案。1 人開発・直 push・enforce_admins off の前提が KsDialogs でも同じで却下理由が有効
- 発見: リモートには `develop` しかなく、`main` の protection は phase-9 で `main` 作成直後にしか付けられない
- 選択肢: A) `develop` には登録せず `main` 用の check 名 10 件を確定して phase-9 へ申し送る / B) 申し送りどおり登録 (効力なし) / C) enforce_admins on + PR 運用 (ADR-0016 改訂)
- 採用: A。オーナーの確認「KsSettingsView と同じになるか」に対し、KsSettingsView の現状 (phase-13 後) と同じ形で件数だけ 7 → 10 と回答して了承
- 論点はこれで空。次は ksn-propose

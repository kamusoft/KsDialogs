# セカンドオピニオン: add-vertical-slice (spec-001)
**相方**: codex / **日付**: 2026-08-14 / **対象**: 提案一式 (proposal / design / specs 6能力 / tasks / ui-brief)
---
静的レビューの結果、実装着手前に修正が必要です。視覚仕様がデルタスペックにない点は意図どおりで、指摘対象としていません。

1. **Critical — [design.md「Decision 1 / Open Questions」](kasane/changes/archive/2026-08-15-add-vertical-slice/design.md:17)、[dialog-contract「型付き結果の show / VM 型キー」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/dialog-contract/spec.md:7)**  
   問題点: `show<VM, R>(vm)` では結果型 `R` が VM・factory・結果通知役のどれにも型として結び付いていません。レジストリも VM 型だけをキーにするため、同じ VM を異なる `R` で show でき、core/ADR-0003 の「報告型と受取型の不一致をコンパイル時に防ぐ」を保証できません。また、View が型付きの完了・キャンセルをどう報告するかが Open Question のままなので、OK/Cancel を実装する経路自体が未定です。  
   推奨修正: `VM : DialogRequest<R>`、または `register<VM, R> { vm, completion: DialogCompletion<R> -> View }` など、VM・結果型・通知役を静的に結び付ける契約を確定してください。Swift/Kotlin/C#/KMP ごとの完全なシグネチャと、型不一致が成立しないことの検証方針も追加してください。

2. **Major — [proposal.md「dialog-contract」](kasane/changes/archive/2026-08-15-add-vertical-slice/proposal.md:11)、各 platform spec「公開 API」**  
   問題点: core/ADR-0002 は全形態で契約 interface/protocol と既定 singleton の両方を要求し、core/ADR-0004 は両入口が同じレジストリを共有すると定めています。しかしデルタスペックで interface と singleton が明記されるのは KMP だけです。iOS・Android・MAUI は、実装者が singleton のみを作っても現 Scenario を満たせます。  
   推奨修正: 全形態について「契約 interface/protocol」「既定入口」「両入口のレジストリ共有」を Requirement 化し、片方で登録してもう片方から show できる Scenario を追加してください。公開名・可視性も API 対応表で固定すべきです。

3. **Major — [dialog-contract「未登録 VM 型の show は即エラー」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/dialog-contract/spec.md:35)、[design.md「Decision 2」](kasane/changes/archive/2026-08-15-add-vertical-slice/design.md:24)**  
   問題点: 「開発時に検出可能なエラー」「各言語の慣例に沿った例外/エラー」はテスト判定できません。Swift が `async throws` なのか precondition trap なのか、Kotlin の例外、MAUI の faulted Task、KMP/Swift へのエラー変換が未定です。特に trap を選ぶと tasks の通常ユニットテストでは確認困難です。  
   推奨修正: 形態別の失敗チャネルとエラー分類を明示し、「View は生成・表示されない」「show は指定エラーで失敗する」まで Scenario にしてください。未登録キーの表現も検証可能な安定値にします。

4. **Major — [proposal.md「show/dismiss」](kasane/changes/archive/2026-08-15-add-vertical-slice/proposal.md:12)、[dialog-contract「型付き結果の show」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/dialog-contract/spec.md:7)**  
   問題点: completed/cancelled の報告とダイアログの dismiss の関係が規定されていません。結果だけ返して View が残っても Scenario は文言上成立します。また、proposal/design/tasks にある `dismiss` が公開 API か内部 Bridge 操作か、dismiss 時の結果が cancelled か、重ね出し時にどの1枚を対象とするかも不明です。  
   推奨修正: 「結果を最初に確定した操作は、その show が所有するダイアログだけを閉じる」「プログラムによる dismiss の結果」「show の完了と dismiss アニメーションの順序保証」を明記してください。`dismiss` が内部専用なら、その可視性と show スコープとの対応を設計に固定してください。

5. **Major — [dialog-contract「show」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/dialog-contract/spec.md:9)、各 Native spec「公開 API」**  
   問題点: 引数に presentation host/context がない一方、iOS は提示元 ViewController、Android は Activity/Context、MAUI は Dispatcher を必要とします。バックグラウンドスレッドからの呼び出し、アクティブな画面がない場合、画面破棄中の挙動が未定です。「いつでも呼び出せる」API の実装方式と失敗条件が platform ごとに分岐します。  
   推奨修正: UI スレッドへの自動マーシャリング有無、提示 host の取得契約、host 不在・破棄時の失敗チャネルを Requirement/Scenario として定義してください。

6. **Major — [android-native「戻るボタンによるキャンセル」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/android-native/spec.md:16)、[design.md「Decision 9」](kasane/changes/archive/2026-08-15-add-vertical-slice/design.md:65)**  
   問題点: Android spec は戻る操作を無条件に cancelled としていますが、design の MD-d と concepts は「キーボード表示中は無視するか判断待ち」です。現 Requirement を実装すれば判断待ち事項を先に否定することになります。  
   推奨修正: 通常時とキーボード表示時を別 Scenario に分け、MD-d の期待値を実装前に決定してください。実測だけを目的とするなら、広すぎる Requirement を通常時に限定します。

7. **Major — [ios-native「KMP 委譲向け互換面」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/ios-native/spec.md:16)、[kmp-facade「Swift async」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/kmp-facade/spec.md:30)**  
   問題点: 任意のジェネリック結果と associated value を持つ Swift enum は、そのまま `@objc` 互換面へ出せません。現在の仕様は「同じ挙動」とだけ述べ、型消去、結果 DTO、型検証、エラー変換、exactly-once の境界を定義していません。素の suspend 公開のフォールバックは Swift 消費側の話であり、iosMain→Swift Native の cinterop 輸送契約を解決していません。  
   推奨修正: Native Swift API と内部 ObjC Bridge を明確に分離し、Bridge の非ジェネリック輸送表現、型復元責任、cancel/error の符号化、二重 completion 防止を設計・Scenario に追加してください。

8. **Major — [design.md「Open Questions」](kasane/changes/archive/2026-08-15-add-vertical-slice/design.md:84)、[tasks.md「4.7」](kasane/changes/archive/2026-08-15-add-vertical-slice/tasks.md:32)**  
   問題点: 結果通知役と同一ダイアログの再 show が「実装時に確定」のままです。Kasane の足場は実装開始後に凍結されるため、公開契約を実装者判断で決める構成になっています。また iOS deployment target は accepted な cross/ADR-0002 で iOS 17 と決まっているのに、正統手段がなければ TODO で済ませられるタスクになっています。  
   推奨修正: 通知役と「同じ VM 型／同じ VM インスタンス」の再 show を実装前に確定してください。deployment target は「生成物が iOS 17 を宣言していること」を受け入れ条件にし、満たせない場合は完了不可またはオーナー合意済み deviation としてください。TODO は代替になりません。

9. **Major — [dialog-contract 冒頭](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/dialog-contract/spec.md:3)、[tasks.md「2〜5」](kasane/changes/archive/2026-08-15-add-vertical-slice/tasks.md:9)**  
   問題点: core spec は全 Requirement が4形態に適用されるとしていますが、タスクで網羅的に検証するのは主に Native 2実装です。MAUI は fake gateway の結果変換、KMP は fake Presenter と一部疎通だけで、「毎回生成」「未登録」「二重報告」「外側タップ」「多段表示」が委譲経路込みでは検証されません。委譲していること自体は、adapter が意味論を壊さない証明にはなりません。  
   推奨修正: Requirement × Scenario × 形態の対応表を作り、各セルを自動テスト・手動確認・Native 継承のいずれで判定するか明記してください。継承扱いにする場合も、少なくとも各 adapter の引数・結果・show 同一性を検証する契約テストが必要です。

10. **Major — [samples「4ルート」](kasane/changes/archive/2026-08-15-add-vertical-slice/specs/samples/spec.md:7)、[tasks.md「7.2」](kasane/changes/archive/2026-08-15-add-vertical-slice/tasks.md:54)**  
    問題点: KMP は iOS/Android 両ターゲットと明記されていますが、MAUI Sample を iOS/Android の両方で確認するかが未定です。「4ルート」だけでは、実行対象は実質6セル（Native iOS、Native Android、MAUI iOS/Android、KMP iOS/Android）になりません。MAUI の片側だけで完了可能です。  
    推奨修正: 受け入れマトリクスを6セルで列挙し、各セルで completed/cancelled/外側タップと approved mock の照合を要求してください。

11. **Major — [design.md「Decision 9」](kasane/changes/archive/2026-08-15-add-vertical-slice/design.md:65)、[tasks.md「1.1 / 7.4 / 7.5」](kasane/changes/archive/2026-08-15-add-vertical-slice/tasks.md:5)**  
    問題点: MD-b は「実挙動記録枠」、MD-d は「判断待ち枠」で、テストの合否条件を持ちません。それでも「共通仕様シナリオ」と「4形態カバレッジ」に含まれるため、何をもってタスク完了か判定できません。  
    推奨修正: MD-b/MD-d を受け入れ Scenario ではなく調査ケースとして分離してください。期待値を決定して Scenario に昇格するか、今回保証しない事項として明記し、記録すべき証跡と終了条件を固定します。

12. **Minor — [tasks.md「6.5 / 7.2」](kasane/changes/archive/2026-08-15-add-vertical-slice/tasks.md:48)、[ui/brief.md「承認モック」](kasane/changes/archive/2026-08-15-add-vertical-slice/ui/brief.md:32)**  
    問題点: 最終スクリーンショットの保存先が roadmap の artifacts だけになっています。Kasane UI 規約では最終照合画像は変更内の `ui/verification/` に置き、brief に照合日・承認結果・妥協点を記録します。現在のタスクでは変更単位の視覚検証証跡が完成しません。  
    推奨修正: 6実行セルの最終画像を `ui/verification/` に保存し、brief 更新をタスク化してください。roadmap 側には必要ならリンクまたは複製を残します。

**総合判定: CHANGES_REQUESTED**

## 突き合わせ結果 (2026-08-14, ホスト側判定)

ホスト側自己レビュー (2周、指摘1件 = 多段表示の4形態カバレッジ不足 → tasks 7.5 で対応済み) との突き合わせ。

| # | 重要度 | 採否 | 判定理由 |
|---|---|---|---|
| 1 (型結び付け) | Critical | **採用** | core/ADR-0003 のコンパイル時型安全の主張と現 Decision 1 の形が実際に矛盾。実害シナリオ (同一 VM を異なる R で show) が具体的。設計追補としてユーザー提示 |
| 2 (interface+singleton 両対応の明記漏れ) | Major | **採用** | core/ADR-0002・0004 との整合漏れ。スペック追記で対応 |
| 3 (未登録エラーの検証可能性) | Major | **採用** | 「開発時に検出可能なエラー」はテスト判定不能。形態別チャネルの明示で対応 |
| 4 (dismiss と結果確定の関係) | Major | **採用** | 結果確定と View の閉鎖の結び付きが未規定という指摘は正当。スペック追記で対応 |
| 5 (提示 host・スレッド契約) | Major | **採用** | 実装リスクとして具体的。設計追補としてユーザー提示 |
| 6 (戻るボタンと MD-d の矛盾) | Major | **採用** | 自スペック内の実矛盾。Scenario を通常時に限定して対応 |
| 7 (@objc 互換面のジェネリック輸送) | Major | **採用** | 技術的に正確 (ジェネリック enum は ObjC へ出せない)。Bridge 輸送表現の設計追補で対応 |
| 8 (Open Questions の実装持ち越し・deployment target) | Major | **採用** | 足場凍結の規約と「実装時に確定」の構成が実際に矛盾。通知役・再 show を実装前に確定、deployment target は受け入れ条件化 |
| 9 (Requirement×形態の対応表) | Major | **採用** | ホスト側自己レビュー指摘 (7.5) と同根で相方がより体系的。対応表 + adapter 契約テストで対応 |
| 10 (受け入れ6セルマトリクス) | Major | **採用** | MAUI 両 OS の明記漏れは事実。6セル明記で対応 |
| 11 (MD-b/d の完了条件) | Major | **採用** | 調査ケースの終了条件未定義は正当。枠の分離と終了条件固定で対応 |
| 12 (ui/verification/ 置き場) | Minor | **採用** | ksn-core references/ui-artifacts.md の規約準拠。tasks 修正で対応 |

採用 12 / 降格 0 / 未解決 0。総合判定 CHANGES_REQUESTED を受け、設計判断を要する 3点 (#1+#8 の型結び付け・通知役・再 show、#5 の host/スレッド契約) はオーナーに提示し、残りは提案アーティファクトに反映する。

**反映記録**: 2026-08-14 採用12件を反映完了 — design.md (Decision 1・2 改訂、Decision 10〜12 追加、Open Questions 解消、deployment target 受け入れ条件化) / specs (Requirement 20・Scenario 31 に増補: 両対応入口・結果確定と閉鎖・呼び出しコンテキスト・再 show・型消去輸送・戻るボタン通常時限定・6セル) / tasks.md (検証対応表 1.2 追加、ui/verification/ 保存、各実装タスクへ展開)。設計判断3点 (型結び付け+DialogNotifier / host 自動解決+スレッド契約 / 再 show 独立重ね出し) は縦串の最小面としてオーナー承認済み。

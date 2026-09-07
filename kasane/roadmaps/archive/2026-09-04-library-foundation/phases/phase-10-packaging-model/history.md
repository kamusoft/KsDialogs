# phase-10-packaging-model 議論履歴

## 2026-08-16: 論点A① 配布単位と消費者ストーリー

旧8論点を6論点 (A〜F) に統合 (旧1+旧7→A、旧3+旧6→B) してから、論点Aを「①配布単位」「②SwiftPM チャネルの実現形」の2段に分けて議論。①を決定。

- **選択肢**: 案1 = 標準3チャネル (Maven Central / SwiftPM git tag / NuGet) のみで umbrella なし、KMP は Maven 1点 + iOS アプリ側 SwiftPM 1点の手動2点 / 案2 = KMP 向け umbrella SwiftPM 配布を追加 / 案3 = KMP framework を dynamic 化して Swift 実体同梱
- **採用**: 案1
- **理由**: 案2 の umbrella が効く「KMP 面を Swift だけから使う人」には Native iOS ライブラリが正解で想定利用者が不在。案3 は純 Native 併用時に Swift 実体が二重化し kmp/ADR-0002 の同一レジストリ共有が壊れる。案1 は全形態が標準機構のみで維持コスト最小、Android Native は kmp/ksdialogs-kmp/build.gradle.kts の `api("jp.kamusoft:ksdialogs:...")` により Maven 推移的依存で自動解決できることを実物で確認済み
- agenda の設問「KMP は3点入れる想定で正か」への答え: 手動は2点、Android Native 分は推移的依存で自動化

## 2026-08-16: 論点A② SwiftPM 配布チャネルの実現形

前提事実: SwiftPM の git 配布はリポジトリルート直下の Package.swift しか解決できない (サブディレクトリ指定は未サポート)。現状は ios/Package.swift のため、このままでは SwiftPM 配布が成立しない。ユーザーから「SwiftPM はサーバーに置くのではなく git 直接参照か」の確認質問があり、git 直接参照 + semver tag が事実上の唯一解であること (Swift Package Registry は実用例がほぼない) を共有した。

- **選択肢**: 案1 = ルート配布マニフェストへの一本化 (Package.swift をルートへ、ソースは path: で ios/ 参照、ios/Package.swift 廃止) / 案2 = 配布用ミラーリポジトリ / 案3 = Swift Package Registry
- **採用**: 案1
- **理由**: 消費者体験 (本体 URL + tag そのまま)・運用コストほぼゼロ・モノレポ tag = SwiftPM バージョンで論点B と好相性・論点C の PoC のリモート参照先が単純になる。案2 は同期 CI とタグ二重管理のコストがソース配布で足りる規模に見合わない。案3 は実用例が乏しい
- **cross/ADR-0004 との折り合い**: ルート禁止条項の本旨は「形態をまたぐ共通ビルドファイル」の防止で、ルート Package.swift は iOS 単独マニフェスト・ソースも ios/ に残るため分離の実質は保たれる。字面上の抵触は配布モデル ADR (cross/0008) で例外として明示し、蒸留時に cross/0004 の Consequences へ追記する
- **付随影響**: kmp の localSwiftPackage(../ios) はルート指しへ変更、iOS 開発は Xcode でルートを開く形になる

論点A①+A② の決着をもって配布モデル ADR を cross/0008 (proposed) として起票した。

## 2026-08-16: 論点C PoC 実施 (Kotlin SwiftPM 連携の配布時成立性)

オーナー承認済みの検証項目4つ (①リポジトリ外からの Maven 解決 ②リモート Swift 参照の成立性と消費者側設定 ③iOS アプリ通し ④パス残留/重複) を、スパイクブランチ `spike/phase-10-packaging-poc` + scratchpad の消費者プロジェクトで実測。**全項目成立**。詳細は artifacts/poc-swiftpm-remote-distribution.md。

要点:
- `swiftPMDependencies` にはリモート API `swiftPackage(url(...), from/exact/branch/revision(...), products)` があり (KGP 2.4.10、機能は Alpha)、発行 metadata で消費者へ推移的に伝わる。消費者の再宣言は不要
- `localSwiftPackage` のまま publish すると発行者マシンの絶対パスが伝播して消費者ビルドが壊れる (実測 + KGP ソース裏取り、未文書化)。発行時はリモート参照必須 → dev (ローカル参照) / publish (リモート参照) の切り替え機構が phase-11 の実装課題
- 消費者アプリで Basic Dialog の表示・型付き結果還流まで動作。Package.resolved は 1 pin でレジストリ1系統を維持
- バージョン固定 (`exact`) が KMP metadata → 消費者 Package.resolved まで機械的に届く。モノレポ tag と合わせた lockstep 強制が可能 (論点Bへの入力)
- 並行して ksn-scout の仕様調査 (公式 doc + KGP v2.4.10 ソース) を実施し、実測と一致を確認

併せて Kasane 議事とは別に、検証項目1の前提として mavenLocal publish 配線 (android/kmp)・ルート Package.swift 試作をスパイクブランチに作成した (main には未反映)。

## 2026-08-17: 論点C 決定 (配布時成立性は「成立」)

PoC 全4項目成立を受けてオーナーが昇格を承認。「Alpha であることは利用者に影響するか」の質問に対し、影響範囲を整理して回答した:

- 影響は **KMP 形態の消費者のみ・ビルド時のみ**。SwiftPM import の機構は発行 metadata を介して消費者の Gradle ビルド内で動くため、消費者も Alpha 機能の利用者になる (Kotlin 2.4+ 必須・metadata 形式の変動リスク・macOS + Xcode 必須・integrateLinkagePackage 手順)
- Native iOS / Android / MAUI の利用者には無関係。実行時成果物は普通の static リンクで Alpha 機構はバイナリに残らない (壊れる方向は「消費者ビルドが通らない」であり「動いていたアプリが壊れる」ではない)
- KMP がリンク時に Swift 実体へ委譲する代替手段は現状なく、「見直し条件付きで受け入れ」を採用 (見直し条件: Stable 化、または metadata 形式の破壊的変更)

決定は cross/0008 (proposed) に反映: 「前提: PoC で検証する」→「検証済み」へ更新し、発行時リモート参照必須・Alpha 受け入れと見直し条件を Consequences に追記。phase-11 への申し送り: dev/publish の参照切り替え機構、integrateLinkagePackage の利用者手順ドキュメント、サポート Kotlin バージョン範囲の宣言。

## 2026-08-17: 論点B バージョン・互換整合の担保

- **選択肢**: 案1 = 全形態 lockstep 単一バージョン (同一 x.y.z 一斉リリース・exact/厳密指定で機械強制・互換表なし) / 案2 = 形態ごと独立バージョン + 互換 range / 案3 = KMP↔Swift のみ exact の折衷
- **採用**: 案1
- **理由**: static リンク + レジストリ共有の構造では版ズレが実行時の契約不整合に直結する。PoC で `exact` が消費者の Package.resolved まで機械的に届くことを確認済みで、ポリシーでなく仕組みで強制できる。cross/0008 の「モノレポ tag = SwiftPM バージョン」とも整合し、tag 1本 = 全形態1リリースが最も素直。案2 の形態別リリースの柔軟性は tag 1本のモノレポでは活きず、互換表の維持コストだけが残る
- **deployment target の整理 (旧論点6由来)**: 配布物での iOS 17 担保は「Swift パッケージ platforms 宣言 + KMP metadata の iosMinimumDeploymentTarget」が正 (両方の伝搬を PoC で確認)。`-Xoverride-konan-properties` は klib に効かない内部ビルド詳細と位置づけ、配布物の担保手段には数えない
- ADR: cross/0009 として proposed で起票

## 2026-08-17: 論点E Swift 向け KMP 面の登録 API の置き場所

- **選択肢**: 案A = Swift パッケージ側に公開登録 API (機械面 KsDialogsInteropBridge は cinterop 委譲専用に残す) / 案B = klib (KMP) 側 / 案C = 両方
- **採用**: 案A
- **理由**: PoC で確定した構造 (KMP 消費者は klib から自分の framework をビルドする) により、klib 側の公開面は消費者の export 設定を強要し ObjC 経由で型も劣化する。消費者アプリは cross/0008 の「アプリ側 SwiftPM 1点」ですでに Swift パッケージをリンクしており、追加設定なしで届く。kmp/0002 (レジストリ実体は Native 側) とも整合し、登録入口が1つに保たれる
- **スコープ**: phase-10 で決めたのは置き場所まで。API の形 (名前・シグネチャ) の設計と Sample の機械面直接利用の差し替え (verify-001 ❌3 の解消) は phase-5 へ
- ADR: kmp/0003 として proposed で起票

## 2026-08-17: 論点D PoC 実施 (MAUI NuGet の native 成果物同梱)

3パッケージ構成 (案2: facade + binding 2件を TFM 条件付き NuGet 依存で接続) を仮採用してオーナー承認の4項目を実測。**全項目成立**。詳細は artifacts/poc-maui-nuget-packaging.md。

要点:
- binding の pack は SDK 標準経路がそのまま機能 (iOS: resources.zip に xcframework 丸ごと / Android: aar 2件同梱)。maui/ADR-0003 の回避策とは干渉しない
- facade の TFM 条件付き ProjectReference は pack 時に自動で NuGet 依存に変換され、消費者は `KsDialogs.Maui` 1点で両プラットフォームのビルドが通る
- iOS はシミュレータで Basic Dialog 表示 → completed(true) の実行時実証まで完了。Android は NuGet 経由ビルド成功まで (実行時は未実施)
- resources.zip 内 manifest に発行マシンの絶対パスが残るが SDK 標準挙動で実害なし (消費者ビルドで実証)
- pack が native ビルドを内包するため、リリース手順は「3プロジェクトの pack」で足りる見込み (phase-11 への入力)

## 2026-08-17: 論点D 決定 (MAUI NuGet は3パッケージ構成)

PoC 全4項目成立を受けてオーナーが昇格を承認。

- **選択肢**: 案2 = 3パッケージ構成 (facade + 輸送層 binding 2件を TFM 条件付き NuGet 依存で接続) / 案1 = 単一パッケージに全部同梱
- **採用**: 案2
- **理由**: binding の pack は SDK 標準経路がそのまま機能することを PoC で実証 (IsPackable=true のみで xcframework の resources.zip / aar 2件が同梱)。単一パッケージ案は自作 MSBuild を回避策の上に積む茨の道。輸送層が公開レジストリに見える点は Description の注意書きで受け止める
- ADR: maui/0004 として proposed で起票

## 2026-08-17: 論点F 決定 (リポジトリ内生成物は現状の追跡状態を正として確定)

- **選択肢**: 案1 = 現状維持を明文化 (KGP 生成の .gitignore + JetBrains 公式指示に従う) / 案2 = 生成物を全部 ignore して都度生成
- **採用**: 案1
- **理由**: 調査の結果、懸念だった「絶対パス含む」ファイル (swiftPMCheckout/workspace-state.json) は KGP 自身が生成する .gitignore で既に ignore 済みで、コミット対象に絶対パス残留がないことを検査で確認した。samples の KotlinMultiplatformLinkedPackage/ は公式ドキュメントが「コミットせよ」と明示しており、消すと fresh clone がそのまま開けなくなる。案2 は公式ガイダンスに逆行する
- **ADR なし**: 覆すコストが低く境界も越えないため選別3基準に該当せず、決定事項 + history の記録のみとした

これで全6論点が解消。フェーズの成果 = agenda の決定事項6件 + ADR 4件 (cross/0008・cross/0009・kmp/0003・maui/0004、いずれも proposed) + PoC レポート2本 (artifacts/)。research 完了マークは ksn-roadmap の責務として案内した。

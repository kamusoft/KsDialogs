---
id: 0006
title: 共有コードの型指定 show は commonMain の VM factory レジストリで解決し、View レジストリの Native 委譲は維持する
status: accepted
date: 2026-09-06
amends: 0002
---

## Context

KMP 共有コード (commonMain) の show は Dialog / Loading / Toast ともインスタンス渡しのみで、型指定 show (core/ADR-0019〜0021) を持たない。共有コードの `DialogViewRegistry` は同一性確認だけの空のハンドルで、登録の面を持たない。commonMain への型指定 show の公開は phase-6 の変更 (add-model-binding-di) の Non-Goals で「需要が出たら別変更で非破壊追加」と見送られていたが、利用者 (オーナー) から「KMP でも型引数だけで呼びたい」という需要が出た。

kmp/ADR-0002 はレジストリ実体を各 Native lib へ全委譲し、「KMP 層にレジストリ実体を持つ」案を Native 側の View レジストリとの二重化・キー不一致を理由に却下している。この却下理由は View factory (OS ごとに View の型が違う) を前提にしたもので、VM factory (共有コードの Kotlin クラスを生成する) には当てはまらない。一方、iOS 側で型指定 show を Native の経路に流すには、Kotlin の型 (`KClass`) からその ObjC クラスを引く橋渡しが必要になり、Kotlin/Native に素直な公開 API が無い。

## Decision

kmp/ADR-0002 の決定のうち「レジストリ実体は各 Native lib へ全委譲する」を、**View factory のレジストリは Native 委譲のまま、VM factory のレジストリは commonMain が持つ**形に置き換える。他の決定 (contract のみ commonMain・expect/actual の範囲・Native への委譲経路) は維持する。

- 共有コードのレジストリに **VM factory の登録口** (VM 型 `KClass` → factory) を設け、実体は commonMain が持つ。OS ごとの分岐は無い
- 共有コードの型指定 show (Dialog / Loading / Toast) は、**commonMain で VM factory から VM を生成し configure を適用してから、既存のインスタンス渡し show に流す**。Native の型指定経路や新たな橋渡しは使わない。順序保証 (VM 生成 → configure → 報告口紐付け → View → 提示) はインスタンス渡し show に入る前に生成・configure が済むことで Native と同じになる
- 型は `KClass<VM>` で渡し、configure は任意 (Dialog / Loading は `suspend`、Toast は同期)。動詞は show 1 本 (core/ADR-0020)
- **VM factory と configure は呼び出し元の文脈 (呼び出し元のコルーチン文脈、Toast は呼び出しスレッド) で実行し、UI スレッドへ移さない**。共有コードは UI スレッドの概念を持たず、Main dispatcher への hop を持ち込むと Fake 差し替えのテスト容易性と kmp/ADR-0002 の「最薄のファサード」を損なうため。core 契約 (core/ADR-0019・0021) の「VM factory と configure は View factory と同じ UI スレッド保証」は KMP 共有コードの面には及ばない — 利用者が VM を作ってからインスタンス渡し show するのと同じ形 (提案レビュー 2026-09-06 で確定)
- 未登録の型指定 show は構成ミスとして失敗する (core/ADR-0021 と同じ)。VM factory / configure の例外は提示に進まず呼び出し元へ伝播する
- 共有コードから型指定 show するなら VM factory は共有コードで登録する。Android Native の登録口で登録した VM factory は共有コードの型指定 show から見えない (共有コードのレジストリが正)
- Swift 向け KMP 面 (`Dialog.shared.kmp` 等) の型指定 show は設けない。共有コードの型指定 show のオーバーロードも `@HiddenFromObjC` で Swift / ObjC から隠す (共有 Kotlin コード専用の面。`KClass` 引数は Swift から扱えず、VM factory / configure の任意の例外は `@Throws` で Swift 境界に運べない)
- VM factory の生成物の実行時クラスは登録キーと一致していなければならず、違えば共有コード側で型不一致の構成ミスとして失敗させる (Native 側は生成物の実行時クラスで View factory を引き直すため、サブクラスを返す factory は未登録に化ける。橋渡しを変えずにこれを防ぐ)

## Alternatives Considered

- **各 OS の Native 面に VM factory を登録し、共有コードの型指定 show を Native の型指定経路に流す** — 却下。Android は Native の登録がそのまま効くが、iOS は Swift の KMP 面に「Kotlin の VM を作る factory」の登録口を新設し、さらに `KClass` から ObjC クラスを引く橋渡しが必要 (要検証)。同じ VM の factory を OS ごとに 2 か所書くことになる
- **Swift 向け KMP 面にも型指定 show を出す** — 却下。Swift 面は Native iOS ライブラリ (Swift パッケージ) 側にあり KMP framework に依存しないため、commonMain の VM factory 表に届かず、Swift 側にも VM factory 登録口が要る (factory の二重化)。iOS ホストは Kotlin の VM を Swift で生成してインスタンス渡し show すれば済み、後から非破壊で追加できる

## Consequences

- 正: 共有コードの VM の生成手順 (Koin の `get()` 等) を共有コードに 1 か所書けば、Android / iOS の両方で型指定 show が効く
- 正: iOS 側の cinterop 橋渡しが増えない。kmp/ADR-0002 の「commonMain VM の ObjC クラスがキーとして同一性を保つ」前提はそのまま
- 負: 共有コードのレジストリが「空のハンドル」から「VM factory の登録口」に育ち、commonMain の面積が増える
- 負: Android では Native レジストリの VM factory スロットと commonMain の VM factory の 2 か所が存在し、登録先を取り違えると共有コードの型指定 show が未登録として失敗する (失敗するので黙って通らない。公開面の文書で登録先を明記する)
- 負: Swift 向け KMP 面には型指定 show が無い非対称が残る (需要が出たら非破壊追加)
- 負: VM factory はキーと同じクラスを返さなければならず、Native の型指定 show (要求された型のエントリで View factory も引く) より制約が強い
- 負: 共有コードの VM factory / configure は UI スレッド保証を持たない (UI スレッドが要る処理は各 OS 側の View factory で行う)

## Revisit When

- Kotlin/Native が `KClass` から ObjC クラスを引く公開 API を提供し、Native 委譲一本に戻すコストが下がったとき
- iOS ホストから共有 VM を型だけで出したい需要が出たとき (Swift 面の型指定 show)

出典: kasane/changes/archive/2026-09-07-add-kmp-typed-show/exploration.md (課題 / 動機・検討した選択肢・決定事項) / kasane/changes/archive/2026-09-07-add-kmp-typed-show/second-opinion-spec-001.md (実行文脈・型の一致・Swift からの可視性の論点、2026-09-06 提案レビューで確定) / kasane/decisions/kmp/0002-thin-facade-native-registry.md (改訂対象) / kasane/changes/archive/2026-08-25-add-model-binding-di/proposal.md (Non-Goals での見送り)

現行照合: 2026-09-07 確認 (実装完了時)。VM factory の表は kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/ViewModelFactoryStore.kt (不変 Map の比較交換で原子的なスナップショット)、登録口は DialogViewRegistry.kt / LoadingViewRegistry.kt / ToastViewRegistry.kt の `registerViewModel`、型指定 show の前段は DialogGateway.kt (`GatewayKsDialog`) と新設の LoadingGateway.kt / ToastGateway.kt (`GatewayKsLoading` / `GatewayKsToast`) で、各 OS の actual gateway はその委譲先。実装で確定した帰結 3 点: ① 型指定 show / start だけでなく `registerViewModel` も `@HiddenFromObjC` で Swift / ObjC から隠した (`KClass` 引数を Swift 側で得る手段が framework に無く、公開後の取り下げが破壊的変更になるため — 独立レビューの指摘で確定。3 つのレジストリ protocol は ObjC 面ではメンバゼロのハンドルのまま。生成ヘッダを iosTest `ObjCApiSurfaceTests` で固定) ② 共有コードが自前で出す診断文言は VM factory 未登録と型不一致の 2 件で、cross/ADR-0015 に合わせて英語固定 (型不一致は生成物の型と「登録キーと同じクラスを返す」規則まで述べる) ③ Android の Native レジストリの VM factory スロットに登録したものが共有コードの型指定 show から見えないことは 3 機能とも androidHostTest (PB-KT-11) で固定。iOS の実 framework 越しの解決は samples/kmp の通しで確認 (証跡は archive の verification/sample-walkthrough/notes.md)。判定: 維持

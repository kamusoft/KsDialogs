# 実行時検証: 非 suspend `KsToast.show(viewModel)` の abort → NSError (A/B)

handbook/cross/runtime-behavior-verification.md に従い、修正前後の同一手順で A/B を 1 回実測した (2026-09-02)。

## 実環境

- KMP iOS Sample (`samples/kmp/iosApp`、bundle id `jp.kamusoft.ksdialogs.samples.kmp.ios`)
- iOS Simulator。起動中の端末は流用せず、専用に 1 台 boot して実測後に shutdown した
- 起動引数 `--demo custom-toast` で自動再生させ、タップ操作なしで同一手順を再現した

## 検証用の一時変更 (実測後にコピーで復元済み)

Swift から Kotlin の `Toast.instance.show(viewModel:)` を直接呼ぶ経路は Sample に無いため、
その境界を再現する一時コードを 2 箇所に置いた。

1. `samples/kmp/shared/.../SamplePresenter.kt`: レジストリに登録していない ViewModel 型
   (`UnregisteredToastViewModel`) を宣言し、それを `toast.show(viewModel)` に渡すだけの
   `showUnregisteredToast()` を追加 (この関数の `@Throws` の有無が A/B の切り替えになる)
2. `samples/kmp/iosApp/.../SampleMenuModel.swift`: `showCustomToast()` の先頭で
   `showUnregisteredToast()` を `do` / `catch` で呼び、前後と catch を `NSLog` で記録
   (Debug ビルドでは `assertionFailure` 自体が abort するため、判定はログで行った)

復元は scratchpad に取った修正後ファイルのコピーで戻した (git checkout は使っていない)。

## A: 修正前 (`@Throws` なし)

`KsToast.show(viewModel)` / `IosToastGateway.show(viewModel)` / 一時関数の `@Throws` を外したビルド。

生成 ObjC ヘッダ (`SampleShared.framework/Headers/SampleShared.h`) の `KsToast` の宣言:

```
- (void)showViewModel:(id<SampleSharedKsdialogs_kmpToastViewModel>)viewModel durationMs:(SampleSharedInt * _Nullable)durationMs placement:(SampleSharedKsdialogs_kmpDialogPlacement * _Nullable)placement __attribute__((swift_name("show(viewModel:durationMs:placement:)")));
```

`error:` 引数がなく、Swift からは非 throwing に見える。

実行結果 (抜粋は evidence/kmp-toast-throws-before.log):

- `KSD-VERIFY: before ...` の直後に
  `Function doesn't have or inherit @Throws annotation ... Program will be terminated.` が出て
  `Uncaught Kotlin exception: ...DialogException` でプロセスが終了した
- `caught` / `after` のログは出ていない。実測後のプロセス確認も 0 件 (消滅)

## B: 修正後 (`@Throws` あり)

生成 ObjC ヘッダの同じ宣言:

```
- (BOOL)showViewModel:(id<SampleSharedKsdialogs_kmpToastViewModel>)viewModel durationMs:(SampleSharedInt * _Nullable)durationMs placement:(SampleSharedKsdialogs_kmpDialogPlacement * _Nullable)placement error:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("show(viewModel:durationMs:placement:)")));
```

戻り値が `BOOL` になり `error:` 引数が付いた (Swift からは `throws`)。

実行結果 (抜粋は evidence/kmp-toast-throws-after.log):

- `KSD-VERIFY: caught Error Domain=KotlinException ... NSLocalizedDescription=ViewModel 型 ... の View factory が登録されていません。`
  が出て、続けて `KSD-VERIFY: after ...` まで進んだ
- 実測後のプロセス確認は生存。同じ起動でメニュー画面が操作可能なまま残った

## 判定と、A/B とヘッダ差分の役割分担

非 suspend の登録経路が、修正前は Kotlin/Native の未処理例外でプロセス終了、
修正後は Swift の `catch` に NSError として届く形に変わった。

ただし A ビルドでは `KsToast.show(viewModel)` / `IosToastGateway.show(viewModel)` /
一時関数 `showUnregisteredToast` の 3 箇所から同時に `@Throws` を外している。
Swift が実際に跨いだ境界は最外周の `SamplePresenter.showUnregisteredToast` であり
(before ログのスタックの `objc2kotlin_kfun:...#showUnregisteredToast`)、
Kotlin→Kotlin の内側呼び出しに `@Throws` は要らない。
したがって runtime の A/B が示すのは **「非 suspend の Kotlin→Swift 境界で
`@Throws` の有無が abort と NSError を分ける」という機構**であって、
ライブラリ側の宣言単独の寄与ではない。

**ライブラリの公開面がその機構に載ったこと**を示すのは生成 ObjC ヘッダの差分の側である —
`KsToast.show(viewModel:durationMs:placement:)` に `error:` 引数が出たこと (上の A / B 各節のヘッダ)
と、関数ごとの `@note` 行 (evidence/kmp-generated-objc-header.txt)。
2 つの証跡はこの役割分担で読む。

## `KsLoading` の 2 本 (suspend) の裏付け

runtime の A/B が覆うのは非 suspend の Toast 経路だけである。`KsLoading` の 2 本は
生成 ObjC ヘッダの差分で確認した。取得は `samples/kmp` で
`./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --rerun-tasks` を回すだけで、
Simulator も実行も要らない。

suspend では ObjC **シグネチャ**は `@Throws` の有無で変わらない
(completionHandler の `NSError` は元から付く)。変わるのは doc comment の `@note` 行で、
Kotlin/Native はその関数が NSError へ変換する例外クラスをここに列挙する。
同じ `KsLoading` の中で VM 経路と message 経路にはっきり差が出ている:

```
/// show(viewModel:placement:completionHandler_:) の直前
 * @note This method converts instances of DialogException, CancellationException to errors.

/// show(message:placement:completionHandler:) の直前
 * @note This method converts instances of CancellationException to errors.
```

`start` の 2 本も同じ対比になる。この 1 行が、宣言が効いていること (VM 経路で
`DialogException` が NSError 化されること) と、message 経路・`hide` / `setMessage` には
効かせていないこと (投げない関数に宣言を足さない、という合意済みスコープの意図) を同時に示す。

5 本すべての `@note` 行と、非 suspend の `KsToast` 2 本の対比は
evidence/kmp-generated-objc-header.txt に抜粋してある。

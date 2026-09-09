# KMP 消費者検証プロジェクト

KMP の配布物 (`jp.kamusoft:ksdialogs-kmp`) を、利用者と同じ経路 — 共有モジュールの Maven 依存 1 行と、iOS ホスト側の 3 点参照 — で消費するプロジェクト。本体のソースは参照しない。

| モジュール | 役割 |
|---|---|
| `shared` | 共有モジュール。`commonMain` の依存は公開座標 1 行だけで、ルート README の KMP 最小例をコンパイル対象に含む |
| `androidApp` | Android アプリ。`shared` だけに依存し、View の登録は推移的に届く Android Native API で行う |
| `iosApp` | iOS アプリ (Xcode project `VerificationKmp.xcodeproj`、scheme `VerificationKmp`) |

`iosApp` が参照するのは、共有モジュールの static framework `VerificationShared`、発行 metadata から生成される合成 package `KotlinMultiplatformLinkedPackage`、Swift 側の登録 API を使うローカル package `VerificationApp` の 3 点だけである。

## 追跡している生成物は非解決 fixture

次の 2 つは、Swift 参照が配信リポジトリの https URL + `exact("0.0.0-alpha.0")` になる形 (smoke 形) で追跡している。

- `iosApp/KotlinMultiplatformLinkedPackage/` — Xcode project が参照する統合物なので、clone 直後にも実体が要る
- `iosApp/VerificationApp/Package.swift` — `Package.swift.template` から作った同じ形

**この 2 つは構造確認用の fixture であり、そのままでは SwiftPM の解決が通らない。** 検証用の合成 version `0.0.0-alpha.0` の tag は配信リポジトリに公開されないため、Xcode で開いて参照構造を確認できるだけである。

消費者ビルドはこのディレクトリを作業ディレクトリへコピーし、コピーの中で

1. 合成 package を発行 metadata から再生成する (`XCODEPROJ_PATH` を渡した `:shared:integrateLinkagePackage`。Swift 参照は mode に応じて `file://` か https になる)
2. `VerificationApp/Package.swift` を `Package.swift.template` から生成する

の 2 つを済ませてから走らせる。**再生成なしにこのディレクトリを直接ビルドしない。** コピーの中で再生成することで、追跡している側は smoke 形のまま変わらず、dry-run の実行後にローカル絶対パスが作業ツリーへ残らない。

## Xcode project の統合をやり直すとき

合成 package の参照を Xcode project に入れ直す必要が出たら、このディレクトリで次を 1 回実行し、生成された subpackage の manifest の `url:` を上記の https へ戻してから追跡する。

```bash
XCODEPROJ_PATH="$PWD/iosApp/VerificationKmp.xcodeproj" \
  ./gradlew -Pksdialogs.mode=dry-run -Pksdialogs.reference=<ローカル Maven リポジトリ> \
            -Pksdialogs.version=0.0.0-alpha.0 :shared:integrateLinkagePackage
```

このタスクは Xcode project にシェルスクリプトのビルドフェーズが 1 つ以上あることを前提にする。`VerificationKmp` の "Build shared framework" フェーズがそれで、共有モジュールの framework を作るために `KSDIALOGS_MODE` / `KSDIALOGS_REFERENCE` / `KSDIALOGS_VERSION` を環境変数から受け取る (消費者ビルドのスクリプトが渡す)。

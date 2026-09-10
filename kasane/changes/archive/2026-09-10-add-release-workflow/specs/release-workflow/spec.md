# release-workflow デルタスペック

## ADDED Requirements

### Requirement: 手動起動と入力の検証
release workflow は `workflow_dispatch` で version を入力して起動する SHALL。version は `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N` (X / Y / Z / N は先頭ゼロを持たない数字) のみを受け付け、それ以外はソースの取得より前に失敗する。`dry-run` 入力 (既定 false) を持ち、true のときは publish 段以降を実行しない。`dry-run` が false のとき、起動ブランチが `main` でなければ失敗する。monorepo に同名の tag が存在し、それが起動 commit と異なる commit を指していれば失敗する (同じ commit を指していれば続行する)。配信リポジトリに同名の tag が存在し、その tag のツリーが起動 commit から生成したスナップショットと異なれば失敗する (同一なら続行する)。README のインストール例の version は検査しない (置換は workflow が行う)。

#### Scenario: 不正な version 形式は早期に失敗する
- **GIVEN** `1.0` / `v1.0.0` / `1.0.0-pre.1` / `1.0.0-SNAPSHOT` / `01.0.0` / `1.0.0-beta.01` のいずれかを version に与えた起動
- **WHEN** workflow が起動する
- **THEN** validate で失敗し、テスト・配布物の生成・publish は実行されない

#### Scenario: main 以外からの本番起動は失敗する
- **GIVEN** `dry-run` が false で、`develop` から起動した実行
- **WHEN** validate が実行される
- **THEN** 起動ブランチの理由で失敗し、後続の job は実行されない

#### Scenario: 別 commit を指す同名 tag があれば失敗する
- **GIVEN** monorepo に version と同名の tag が別の commit を指して存在する
- **WHEN** validate が実行される
- **THEN** tag の衝突として失敗する

#### Scenario: 配信リポジトリの同名 tag は publish の前に内容で判定する
- **GIVEN** 配信リポジトリに version と同名の tag が、起動 commit のスナップショットと異なる内容で存在する
- **WHEN** validate が実行される
- **THEN** 配布物の生成・publish に入る前に失敗する (内容が同一なら続行し、publish 段の tag push は skip される)

#### Scenario: dry-run 入力は publish 手前で止まる
- **GIVEN** `dry-run` を true にして任意のブランチから起動した実行
- **WHEN** workflow が完了する
- **THEN** validate・テスト・配布物の生成・消費者検証 (dry-run) までが実行され、publish・tag・Release・smoke・`develop` への commit は実行されず、配信先への書き込みは発生しない

### Requirement: 段の構成と順序
release workflow は validate → (test ∥ package) → dry-run → publish → 反映待ち → smoke の順に進む SHALL。test は本体検証の再利用可能 workflow (`verify-{ios,android,android-instrumented,kmp,maui}.yml`) をそのまま呼ぶ。package は入力の version を注入して iOS のスナップショット、Android の Maven ローカル発行物 (`ksdialogs-core` / `ksdialogs`)、MAUI の nupkg / snupkg を artifact として保存する。KMP の配布物は package 段では作らない。dry-run は消費者検証 workflow 4 本を `mode=dry-run` + version + artifact で呼び、`consumer-kmp` には Android の artifact を渡す。publish は test 5 job と dry-run 4 job のすべてが成功したときにのみ実行される。release workflow の実行はリポジトリ全体で直列化される (concurrency group、進行中の実行を打ち切らない。待ち行列に保持されるのは最新の 1 件で、3 件目以降の dispatch は古い待ちを置き換える)。publish job は自分の番になってから外部状態 (tag・公開済み version) を再検査してから書き込む。

#### Scenario: テストか dry-run が 1 つでも失敗すれば publish しない
- **GIVEN** test または dry-run のいずれか 1 job が失敗した実行
- **WHEN** workflow が進む
- **THEN** publish job は実行されず、配信先への書き込みは発生しない

#### Scenario: 同時に起動した 2 つの実行は直列になる
- **GIVEN** 同じ version で 2 回 dispatch した実行
- **WHEN** 両方が publish に到達しようとする
- **THEN** 後の実行は先の実行の完了を待ち、先の実行が publish を完了していれば後の実行の publish ステップはすべて skip される (3 件目の dispatch があれば待っていた 2 件目は取り消され、最新の 1 件だけが待つ)

#### Scenario: dry-run は publish する配布物そのものを検証する
- **GIVEN** package 段が保存した artifact
- **WHEN** dry-run の消費者検証と publish が実行される
- **THEN** MAUI は同じ nupkg が dry-run で解決され nuget.org へ push され、iOS は同じスナップショットが dry-run で解決され配信リポジトリへ commit される。Android は publish 時に署名つきで再ビルドされるため「Android 成果物の同一性」に従い、KMP は publish 段で初めて作られる

### Requirement: Android 成果物の同一性
Android の publish は署名鍵を持つ publish job が再ビルドした発行物を upload するため、package 段の発行物と同一のファイルではない SHALL NOT。publish job は upload の前に、再ビルドした発行物と package 段の発行物を署名ファイルとチェックサムを除いて比較し、差異があれば upload せずに失敗する SHALL。比較の対象は pom・Gradle module metadata・aar・sources jar・javadoc jar で、アーカイブはエントリ名と各エントリの内容で比較する。package 段の Android job と publish job は同じランナー OS・同じ JDK・同じ commit で実行される。KMP の発行物は比較の対象外である。

#### Scenario: 再ビルドの差異で upload を止める
- **GIVEN** package 段の発行物と publish job の再ビルド結果に署名以外の差異がある状態
- **WHEN** publish job の比較ステップが実行される
- **THEN** 差異のあるファイルが出力され、Maven Central への upload は実行されない

### Requirement: publish の順序
publish は 1 つの job で直列に、配信リポジトリへのスナップショット commit の push → Android の Maven Central への upload (自動 release せず保留) と検証の決着待ち → 配信リポジトリへの tag の push → KMP の発行 (Swift 参照は配信リポジトリの https URL + 入力 version の exact) と Maven Central への upload 保留と検証の決着待ち → nuget.org への push → Maven Central の release (Android → KMP) → monorepo の tag と GitHub Release の作成 → README / Skill の version 置換 commit の `develop` への push、の順に行う SHALL。nuget.org への push・Maven Central の release・monorepo の tag と Release は、それより前のすべてのステップが成功したときにのみ行われる。配信リポジトリの tag は KMP の発行より前に存在する。

#### Scenario: KMP の発行前に配信リポジトリの tag が存在する
- **GIVEN** publish job の実行
- **WHEN** KMP の発行ステップが始まる
- **THEN** 配信リポジトリには当該 version のスナップショット commit と tag が push 済みで、KMP の発行物の Swift 参照は配信リポジトリの https URL と当該 version の exact を持つ

#### Scenario: KMP の検証失敗では取り消せない操作に進まない
- **GIVEN** KMP の Maven upload の検証が FAILED になった実行
- **WHEN** publish job が終了する
- **THEN** nuget.org への push・Maven Central の release・monorepo の tag と Release は行われず、配信リポジトリの tag だけが残り、Android の保留 deployment は drop される

#### Scenario: 途中で失敗すれば monorepo の tag は作られない
- **GIVEN** nuget.org への push が失敗した実行
- **WHEN** publish job が終了する
- **THEN** monorepo に tag は存在せず、GitHub Release も作られず、Maven Central に当該 version は公開されていない

#### Scenario: スナップショット commit は Android の upload より前に push される
- **GIVEN** publish job の実行
- **WHEN** Android の Maven Central への upload が始まる
- **THEN** 配信リポジトリには当該 version のスナップショット commit が push 済みで、tag はまだ存在しない

### Requirement: Maven Central の 2 枠の deployment
Maven Central への発行は Android (`ksdialogs-core` / `ksdialogs`) と KMP (`ksdialogs-kmp` の全 publication) の 2 つの deployment で行い、それぞれの upload の直後に deployment が検証済み (VALIDATED) になるまで待ってから次のステップへ進む SHALL。検証待ちの決着が VALIDATED 以外 (FAILED / 上限超過) なら失敗する。両枠の deployment ID は upload の時点で得られなければ失敗し、同じ実行の再実行から枠ごとに参照できる形で保存する SHALL。release は nuget.org への push の後に、枠ごとに VALIDATED を再確認してから Android → KMP の順で行い、公開 (PUBLISHED) を待つ SHALL。再実行時に枠の前回 deployment ID があれば、upload の前にその状態を照会し、VALIDATED なら upload を skip して release へ、PUBLISHING なら PUBLISHED になるまで待って release を skip、PUBLISHED なら upload と release を skip、FAILED なら drop してから再 upload、NOT_FOUND なら再 upload する SHALL。publish job がいずれかのステップで失敗した場合、drop 可能な状態 (VALIDATED / FAILED) の deployment は両枠とも drop する SHALL。PUBLISHING / PUBLISHED の deployment は drop しない。

#### Scenario: upload 後は両枠とも保留状態で止まる
- **GIVEN** publish job の KMP upload と検証待ちのステップ
- **WHEN** ステップが成功する
- **THEN** Central Portal の Android と KMP の deployment はいずれも VALIDATED で、Maven Central には当該 version の 3 座標がまだ公開されていない

#### Scenario: NuGet push の後に 2 枠が release される
- **GIVEN** nuget.org への push が成功した実行
- **WHEN** Maven release ステップが実行される
- **THEN** Android の deployment が先に、KMP の deployment が次に PUBLISHING / PUBLISHED へ遷移し、以後 3 座標が Maven Central から取得できる

#### Scenario: 失敗時に保留 deployment が残らない
- **GIVEN** 2 枠の upload の後、nuget.org への push で失敗した実行
- **WHEN** publish job が終了する
- **THEN** Central Portal に当該 version の保留 deployment は Android / KMP のどちらも残っていない

#### Scenario: release の応答が失われても再実行で整合する
- **GIVEN** Android の release の要求はサーバーで受理されたが応答の前に step が失敗し、deployment が PUBLISHING のまま残った実行
- **WHEN** 同じ version で再実行する
- **THEN** 前回の Android の deployment ID から状態を照会し、drop も再 upload もせずに PUBLISHED を待ち、KMP 枠の状態に応じて以後のステップへ進む

#### Scenario: 検証中のまま release に進まない
- **GIVEN** upload 直後の deployment が VALIDATING の状態
- **WHEN** 検証待ちステップが実行される
- **THEN** VALIDATED になるまで待ってから次へ進み、上限内に決着しなければ失敗する

### Requirement: 署名の生成確認
Maven Central への各枠の upload の前に、署名ファイル (`.asc`) が発行物の各成果物 (Android: aar / pom / sources jar / javadoc jar / module metadata。KMP: 5 publication の pom / module metadata / jar / klib (cinterop klib を含む) / SwiftPM 連携メタデータ等の JSON) に対して生成されていることを確認する SHALL。生成されていなければ upload せずに失敗する。

#### Scenario: KMP の署名が 1 件欠けても upload しない
- **GIVEN** KMP の発行物のうち klib または JSON 1 件の `.asc` が無い状態
- **WHEN** KMP の署名確認ステップが実行される
- **THEN** 欠けたファイルが出力され、KMP の upload は実行されない

#### Scenario: 署名鍵が渡っていなければ upload しない
- **GIVEN** 署名鍵の secret が空の状態
- **WHEN** publish job の Android の署名確認ステップが実行される
- **THEN** `.asc` が無いことを理由に失敗し、upload も配信リポジトリの tag push も実行されない

### Requirement: nuget.org への push
nuget.org への push は Trusted Publishing (GitHub Actions の OIDC) で得た一時的な API key を用い、長期の API key を secret として保持しない SHALL。push は binding 2 件 (`KsDialogs.Binding.Android` / `KsDialogs.Binding.iOS`) を先に、facade (`KsDialogs.Maui`) を最後に行い、各パッケージの nupkg と snupkg を対で push する SHALL。同じ version が既に存在する場合は失敗とせず skip する SHALL。

#### Scenario: 3 パッケージが同じ version で公開される
- **GIVEN** publish job の NuGet push ステップ
- **WHEN** ステップが成功する
- **THEN** `KsDialogs.Maui` / `KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android` の当該 version が nuget.org に存在する

#### Scenario: binding の push 失敗で facade は公開されない
- **GIVEN** binding 1 件の push が成功した後、もう 1 件の push で失敗した実行
- **WHEN** publish job が終了する
- **THEN** facade `KsDialogs.Maui` の当該 version は nuget.org に存在せず、再実行では成功済みの binding は skip され残りが push される

### Requirement: tag と GitHub Release
publish のこれより前のステップの成功後、monorepo に接頭辞なしの version の tag を作り (配信リポジトリの同名 tag は KMP の発行前に作成済み)、GitHub Release を作る SHALL。Release 本文は GitHub の自動生成ノートを用い、`.github/release.yml` の分類 (ラベル `breaking` / `feature` / `fix` / `docs`、除外 `kasane` / `ci`) に従う。version に prerelease の suffix があれば Release を prerelease として作る SHALL。

#### Scenario: prerelease の suffix で prerelease になる
- **GIVEN** version `0.1.0-beta.1` の実行
- **WHEN** Release が作られる
- **THEN** Release は prerelease として印が付き、tag `0.1.0-beta.1` が monorepo と配信リポジトリの両方に存在する

#### Scenario: 正式版は prerelease にならない
- **GIVEN** version `0.1.0` の実行
- **WHEN** Release が作られる
- **THEN** Release は prerelease ではない

### Requirement: 同じ version での再実行
publish job のステップは冪等であり、失敗した実行を同じ version で「失敗した job から再実行」(同じ workflow run の再試行) したとき、既に完了している publish (配信リポジトリの commit と tag、Maven Central の公開、nuget.org の push、monorepo の tag と Release) を重複させず skip し、未完了のステップだけを行って完了できる SHALL。部分 publish の続行は同じ run の再試行に限り、新規の dispatch (初回の試行) で当該 version の外部状態 (配信リポジトリの tag、Maven Central の公開、nuget.org の存在) のいずれかが既にあり、かつ monorepo の tag が起動 commit に無い場合は、公開済みの binary と source の対応を保証できないため失敗する SHALL (monorepo の tag が起動 commit にあれば完了済みとして全ステップを skip する)。monorepo の tag が別の commit を指している場合、および配信リポジトリの同名 tag の内容が今回のスナップショットと異なる場合は失敗する。配信リポジトリの tag は KMP の発行に先立って作られるため、publish が KMP 以降で失敗した version を放棄する場合、その番号は欠番として再利用しない SHALL (tag の削除は任意で、回収の手段とはみなさない)。

#### Scenario: KMP の失敗後に同じ version で埋める
- **GIVEN** 配信リポジトリの tag push まで成功し KMP の upload で失敗した実行
- **WHEN** 同じ version で再実行する
- **THEN** スナップショット commit と tag は内容一致で skip され、Android の upload は前回の deployment の状態に応じて skip または再 upload され、KMP の発行と upload から先が完了する

#### Scenario: 部分 publish を同じ version で埋める
- **GIVEN** nuget.org への push まで成功し Maven release で失敗した実行
- **WHEN** 同じ version で再実行する
- **THEN** 配信リポジトリの commit と tag・nuget.org の push は skip され、2 枠の release → monorepo tag → Release → `develop` への commit が完了する

#### Scenario: 別 commit からの新規 dispatch は部分 publish を引き継がない
- **GIVEN** 前回の run が配信リポジトリの tag push まで成功して失敗し、その後 `main` が進んだ状態で同じ version を新規に dispatch した実行
- **WHEN** publish job の外部状態の再検査が実行される
- **THEN** 初回の試行で外部状態が既にあり monorepo の tag が無いことを理由に失敗し、前回の run を再実行するよう案内が出る。配信先への書き込みは発生しない

#### Scenario: 放棄した version の番号は再利用されない
- **GIVEN** KMP の upload で失敗し、再実行せずに放棄した version `X.Y.Z-beta.N`
- **WHEN** 次のリリースを行う
- **THEN** 次のリリースは別の番号 (`X.Y.Z-beta.N+1` 等) で dispatch され、放棄した番号の配信リポジトリの tag は残っていても monorepo の tag・Release・Maven・NuGet には存在しない

#### Scenario: 全て完了済みの再実行は何も重複させない
- **GIVEN** publish まで成功し smoke で失敗した実行
- **WHEN** 同じ version で「失敗した job から再実行」する
- **THEN** publish job は再実行されず (または全ステップ skip)、smoke だけが再実行される

### Requirement: README と Skill の version 置換
README (英語 / `README_ja`) と利用者向け Skill のインストール例の version は release workflow が置き換える SHALL。専用 script は対象行を行の形 (配信リポジトリの URL、Maven 座標 `ksdialogs-core` / `ksdialogs` / `ksdialogs-kmp`、NuGet ID `KsDialogs.Maui`) で見つけ、値がプレースホルダ `<version>` でも実値でも入力の version に置き換え、期待する行が揃わないファイルがあれば何も書き換えずに失敗する SHALL。package 段の MAUI job は pack の前に作業木で置換を行い (commit しない)、facade の nupkg に同梱される README は入力の version を持つ SHALL。publish job は Release の作成後に `develop` の先端で置換を行い、差分があれば検証 CI の lint job と同じ検査 (ローカル絶対パス・識別子・README 最小例の一致) を掛けてから commit して push する SHALL (この commit は `GITHUB_TOKEN` による push のため検証 CI を起動しない)。lint に失敗したら commit せず警告として報告する。push が拒否された場合は release を失敗にせず警告として報告する。`dry-run` の実行では `develop` への commit を行わない。

#### Scenario: nupkg の README は入力 version を持つ
- **GIVEN** 作業木の README のインストール例がプレースホルダまたは旧 version の状態
- **WHEN** package 段の MAUI job が pack する
- **THEN** facade の nupkg に同梱された README のインストール例は入力の version で、リポジトリの README は commit されていない

#### Scenario: publish 成功後に develop の README と Skill が新 version になる
- **GIVEN** publish の Release 作成まで成功した実行
- **WHEN** 置換 commit のステップが実行される
- **THEN** `develop` の README 2 枚と Skill のインストール例が入力の version になった commit が push され、`main` は変わらない。commit の前に検証 CI の lint job と同じ検査 (ローカル絶対パス・識別子・README 最小例の一致) が置換後の内容に対して実行され、通っている

#### Scenario: 該当行が見つからなければ失敗する
- **GIVEN** インストール例の行の形が変わり script の検出パターンに合わない README または Skill
- **WHEN** script を実行する
- **THEN** 置換せずに失敗し、検出できなかったファイルと対象が出力される

#### Scenario: develop への push の競合は release を失敗にしない
- **GIVEN** 置換 commit の push が `develop` の更新と競合して拒否された実行
- **WHEN** publish job が終了する
- **THEN** job は成功として終わり、Summarize に README の追従が行われなかった旨の警告が出る

### Requirement: 反映待ちと smoke
tag と Release の作成後、Maven Central の Android 2 座標 (`ksdialogs-core` / `ksdialogs`) と KMP の 5 publication (root / android / iOS 3 ターゲット) の POM、nuget.org の 3 Package ID に当該 version が取得可能になるまで待機 (上限あり) してから、消費者検証 workflow を `mode=smoke` + version で 4 形態について呼ぶ SHALL。smoke の失敗は workflow の失敗として報告され、作成済みの tag と Release は取り消されない。

#### Scenario: 反映を待ってから smoke する
- **GIVEN** publish 直後で Maven Central にまだ当該 version が同期されていない状態
- **WHEN** 反映待ち job が実行される
- **THEN** 10 件すべてで取得可能になるまで待ってから smoke 4 本が呼ばれ、上限内に反映されなければ失敗として報告される

#### Scenario: 公開レジストリから 4 形態が解決される
- **GIVEN** 反映待ちが成功した実行
- **WHEN** smoke 4 本が実行される
- **THEN** iOS は配信リポジトリの https + exact、Android と KMP は mavenCentral、MAUI は nuget.org から当該 version が解決され、消費者ビルドが成功する

#### Scenario: smoke 失敗でも tag は残る
- **GIVEN** smoke のいずれかが失敗した実行
- **WHEN** workflow が終了する
- **THEN** workflow は失敗として報告されるが、tag と Release は存在したままである

### Requirement: secrets と権限の範囲
配信先への認証情報 (Central Portal の User Token、署名鍵、nuget.org のユーザー名、配信リポジトリの deploy key) は GitHub Environment `release` に置き、publish job だけが参照する SHALL。Environment `release` は `main` ブランチからの参照に限定する。他の job は secrets を受け取らず、消費者検証 workflow の呼び出しに全 secrets を引き継ぐ指定 (`secrets: inherit`) を用いない SHALL。publish job 以外の権限は読み取りに限る。publish job の書き込み権限は monorepo の contents と OIDC の id-token に限る。

#### Scenario: publish 以外の job は書き込み手段を持たない
- **GIVEN** release workflow の定義
- **WHEN** 各 job の権限と secrets を確認する
- **THEN** `environment: release` と書き込み権限を持つのは publish job だけで、他の job は `contents: read` のみで secrets の参照を持たない

#### Scenario: main 以外から Environment は参照できない
- **GIVEN** Environment `release` の deployment branch policy
- **WHEN** `main` 以外のブランチの実行が publish job に到達しようとする
- **THEN** Environment の参照が拒否され、publish は実行されない

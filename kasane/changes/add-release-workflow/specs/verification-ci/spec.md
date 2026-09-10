# verification-ci デルタスペック

## ADDED Requirements

### Requirement: main のマージ保護
`main` への変更の取り込みは、10 job (`lint`、本体検証 5 job `ios / verify`・`android / verify`・`android-instrumented / verify`・`kmp / verify`・`maui / verify`、消費者検証 4 job `consumer-{ios,android,maui,kmp} / verify`) すべての成功を必須 status check とする pull_request 経由 SHALL とする (承認数 0、管理者のバイパスは許容する)。必須 status check は GitHub Actions の app に限定する (同名 check を出す他アプリで満たせない)。`main` の先端は最新リリース、またはリリース進行中 (リリース PR のマージ後、publish 成功まで) のリリース候補を表す。`develop` はリポジトリの既定ブランチのままで、必須 status check を持たず、force-push と削除だけを禁止する。

#### Scenario: 検査未通過のマージ拒否
- **GIVEN** 10 job のいずれかが失敗している `main` 宛ての PR
- **WHEN** マージしようとする
- **THEN** マージはブロックされる

#### Scenario: main への直 push の拒否
- **GIVEN** `main` への直接 push
- **WHEN** push を試みる
- **THEN** push は拒否される (admin バイパスを除く)

#### Scenario: main 宛て PR で 10 job が起動する
- **GIVEN** head が `develop` の `main` 宛て PR
- **WHEN** CI が実行される
- **THEN** lint・本体検証 5 job・消費者検証 4 job が起動し、status check 名が必須 check の 10 件と一致する

#### Scenario: develop は保護を変えない
- **GIVEN** `develop` への直接 push
- **WHEN** push を試みる
- **THEN** push は受け付けられ、必須 status check は要求されない

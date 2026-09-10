set-readme-version.py を作業木で実行した結果 (実行日 2026-09-10。実行後は複写で元に戻した)

# 1. 対象の列挙 (置換の対象と各ファイルの期待本数)

| ファイル | SwiftPM | ksdialogs-core | ksdialogs | ksdialogs-kmp | NuGet | 計 |
|---|---|---|---|---|---|---|
| README.md | 1 | 1 | 1 | 1 | 1 | 5 |
| README_ja.md | 1 | 1 | 1 | 1 | 1 | 5 |
| skills/en/ksdialogs-android/SKILL.md | | 1 | 1 | | | 2 |
| skills/ja/ksdialogs-android/SKILL.md | | 1 | 1 | | | 2 |
| skills/en/ksdialogs-kmp/SKILL.md | | | | 2 | | 2 |
| skills/ja/ksdialogs-kmp/SKILL.md | | | | 2 | | 2 |
| skills/en/ksdialogs-kmp/references/ios-host.md | | | | 1 | | 1 |
| skills/ja/ksdialogs-kmp/references/ios-host.md | | | | 1 | | 1 |
| skills/en/ksdialogs-kmp/references/android-host.md | | | 1 | | | 1 |
| skills/ja/ksdialogs-kmp/references/android-host.md | | | 1 | | | 1 |
| skills/en/ksdialogs-maui/SKILL.md | | | | | 1 | 1 |
| skills/ja/ksdialogs-maui/SKILL.md | | | | | 1 | 1 |
| skills/en/ksdialogs-aiforms-migration/SKILL.md | | | | | 1 | 1 |
| skills/ja/ksdialogs-aiforms-migration/SKILL.md | | | | | 1 | 1 |
| **計** | 2 | 4 | 6 | 8 | 6 | **26** |

skills/{en,ja}/ksdialogs-ios/SKILL.md は配信リポジトリの URL を散文で挙げるだけで
version を持たないため、対象に入らない。

# 2. 作業木での置換

  $ python3 scripts/release/set-readme-version.py 9.9.9-rc.7
  14 ファイルのインストール例 26 行を 9.9.9-rc.7 にした

  $ git diff --stat -- README.md README_ja.md skills/
  ... 14 files changed, 26 insertions(+), 26 deletions(-)

変更されたのはインストール例の行だけ (差分の全行が置換前後の対):

  -        exact: "<version>"
  +        exact: "9.9.9-rc.7"
  -    implementation("jp.kamusoft:ksdialogs-core:<version>")
  +    implementation("jp.kamusoft:ksdialogs-core:9.9.9-rc.7")
  -    implementation("jp.kamusoft:ksdialogs:<version>")
  +    implementation("jp.kamusoft:ksdialogs:9.9.9-rc.7")
  -<PackageReference Include="KsDialogs.Maui" Version="<version>" />
  +<PackageReference Include="KsDialogs.Maui" Version="9.9.9-rc.7" />
  -            api("jp.kamusoft:ksdialogs-kmp:<version>")
  +            api("jp.kamusoft:ksdialogs-kmp:9.9.9-rc.7")
  -1. Add the single `jp.kamusoft:ksdialogs-kmp:<version>` Maven dependency to the shared module as shown above.
  +1. Add the single `jp.kamusoft:ksdialogs-kmp:9.9.9-rc.7` Maven dependency to the shared module as shown above.

  $ python3 scripts/release/set-readme-version.py --check 9.9.9-rc.7
  インストール例 26 行が 9.9.9-rc.7 と一致する

# 3. 行の形を崩した場合 (何も書き換えないこと)

README.md の SwiftPM の解決方法の行をコメントに差し替えてから実行:

  $ python3 scripts/release/set-readme-version.py 1.1.1
  ::error::README.md: SwiftPM の依存宣言 が 0 行ある (1 行であるべき)
  対象行を確定できないため置換しない
  exit=1

他のファイルは 1 件も 1.1.1 に変わらず、前段の 9.9.9-rc.7 のままだった
(README_ja.md に 9.9.9-rc.7 が 5 件、skills/en/ksdialogs-maui/SKILL.md に 1 件)。

# 4. 復元

対象 14 ファイルは実行前に別ディレクトリへ複写しておき、確認後にそこから書き戻した
(`git checkout` は使わない)。復元後の `git status` に README / skills の変更は無い。

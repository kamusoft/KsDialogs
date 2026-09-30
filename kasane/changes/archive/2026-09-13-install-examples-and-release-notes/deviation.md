# deviation: install-examples-and-release-notes

デルタスペック・design からの乖離と、本 change に同梱した付随修正の記録。

## 付随修正

- [付随修正] `kasane/handbook/cross/local-development-setup.md`「リリース用スクリプトの自己テストを回す」: 削除済みの `set-readme-version.py --selftest` を案内していた手元手順を、`build-release-notes.py` / `check-time-budget.py` / `check-publish-step-order.py` の自己テストと `install-example-lint.py` に置き換えた。理由: 本 change のスクリプト撤去で参照が切れたため (2026-09-13)
- [付随修正] `kasane/handbook/cross/verification-ci.md`: lint job を「8 検査」と書いていた本文・表・description を 12 検査へ数え直し (先行 change backport-registry-wait-hardening の 3 検査 + 本 change のインストール例の契約の検査)、release 節の「publish job が `develop` へインストール例を反映する前に 3 検査を掛ける」の記述を現状 (反映 step は無い) に合わせた。理由: 本 change の step 削除で記述が現在形でなくなったため (2026-09-13)
- [付随修正] `kasane/handbook/index.md`: cross の件数を 13 件から 15 件へ直した。install-examples.md の追加に加え、release-procedure.md が数にも列挙にも入っていなかった数え落としを併せて直している。理由: 8.3 で件数を触る位置に既存の誤りが同居していたため (2026-09-13)
- [付随修正] `kasane/handbook/cross/release-procedure.md`「失敗したとき」: 「完了済みの古い run は再実行しない」の注意を削除した。この注意の理由は「古い run の再実行が `develop` のインストール例を古い version へ書き戻す」ことだけで、その step が本 change で無くなった。理由: 根拠を失った注意を残すと現在の挙動と食い違うため (2026-09-13)

## 乖離

なし。

## レビュー指摘の修正

- [レビュー指摘の修正] `.github/workflows/ci.yml`: インストール例の契約の検査を 2 step (`Install example lint` / `Install example lint selftest`) から 1 step にまとめ、自己テストを本検査より先に走らせた。tasks.md に無い修正。理由: cross/ADR-0029 の数え方・handbook `cross/verification-ci.md`・同 `local-development-setup.md` がいずれも「自己テストを本検査より先に同じ step で」と定めており、同 job の既存 4 検査もその形だったのに、新設分だけが外れていたため (2026-09-13)
- [レビュー指摘の修正] `scripts/install-example-lint.py`: `PROSE_DECLARATIONS` の各組について、コードブロックの外に宣言が実在することを `lint()` で検査するようにした (`check_prose_declarations`)。値は検査しない。自己テストに「一覧にある散文の宣言を失うと exit 1」を追加。tasks.md に無い修正。理由: 一覧が成功メッセージの本数表示にしか使われておらず、散文の宣言を失っても検査が緑のまま通っていたため (2026-09-13)
- [レビュー指摘の修正] `kasane/decisions/cross/0030-release-does-not-write-back-install-examples.md`: tasks 9.1〜9.3 が挙げる ADR は 0027 / 0028 / 0029 の 3 本で 0030 を含まないが、review-001 Major 3 を受けて cross/ADR-0024 への amends を起票した。理由: 置換機構の撤去が 0024 の publish 順序表 順 8 と 「tag と README の扱い」後半を現行でなくしたため (2026-09-13)

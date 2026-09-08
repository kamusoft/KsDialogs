# README のリンク参照の検査 (2026-09-08)

## 機械抽出 (anchor と mailto を除く非 HTTP(S) 参照)

```
python3 - <<'PY'
import re
for f in ['README.md','README_ja.md']:
    refs = re.findall(r'\]\(([^)]*)\)', open(f).read())
    non = [r for r in refs if not r.startswith(('http://','https://','#','mailto:'))]
    print(f, len(refs), len(non), non)
PY
```

改訂前 (各 12 件):

```
README.md    skills/README.md, skills/, assets/, LICENSE, ios/, android/, maui/, kmp/,
             kasane/, kasane/concepts/, AGENTS.md, .github/CONTRIBUTING.md
README_ja.md skills/README_ja.md, skills/, assets/, LICENSE, ios/, android/, maui/, kmp/,
             kasane/, kasane/concepts/, AGENTS.md, .github/CONTRIBUTING_ja.md
```

改訂後:

```
README.md total refs: 19 / non-HTTP(S) (anchor・mailto 除く): 0 []
README_ja.md total refs: 19 / non-HTTP(S) (anchor・mailto 除く): 0 []
```

ファイルは `https://github.com/kamusoft/KsDialogs/blob/develop/<path>`、
ディレクトリは `https://github.com/kamusoft/KsDialogs/tree/develop/<path>` に改めた。
画像は既に `https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/` の絶対 URL。

## 全 URL の取得 (curl -L、HTTP ステータス)

```
200  https://github.com/kamusoft/KsDialogs/blob/develop/.github/CONTRIBUTING.md
200  https://github.com/kamusoft/KsDialogs/blob/develop/.github/CONTRIBUTING_ja.md
200  https://github.com/kamusoft/KsDialogs/blob/develop/AGENTS.md
200  https://github.com/kamusoft/KsDialogs/blob/develop/LICENSE
200  https://github.com/kamusoft/KsDialogs/blob/develop/skills/README.md
200  https://github.com/kamusoft/KsDialogs/blob/develop/skills/README_ja.md
200  https://github.com/kamusoft/KsDialogs/tree/develop/android
200  https://github.com/kamusoft/KsDialogs/tree/develop/assets
200  https://github.com/kamusoft/KsDialogs/tree/develop/ios
200  https://github.com/kamusoft/KsDialogs/tree/develop/kasane
200  https://github.com/kamusoft/KsDialogs/tree/develop/kasane/concepts
200  https://github.com/kamusoft/KsDialogs/tree/develop/kmp
200  https://github.com/kamusoft/KsDialogs/tree/develop/maui
200  https://github.com/kamusoft/KsDialogs/tree/develop/skills
200  https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-dialog.png
200  https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-loading.png
200  https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-toast.png
200  https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-dialog.png
200  https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-loading.png
200  https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-toast.png
```

一意 20 URL がすべて 200。両 README の 19 参照はこのうちのいずれか (anchor `#agent-skills` を除く)。

## facade の nupkg への同梱

README 改訂後に facade を pack し直して確認:

```
cd maui && dotnet pack KsDialogs.Maui/KsDialogs.Maui.csproj -c Release -p:Version=0.1.0-alpha.1 -o <OUT>
unzip -l <OUT>/KsDialogs.Maui.0.1.0-alpha.1.nupkg | grep README
     7119  09-08-2026 11:26   README.md
```

- nupkg のルートに `README.md` があり、nuspec の `<readme>README.md</readme>` がそれを指す
- 取り出した中身は作業ツリーの `README.md` と一致 (`diff` で差分なし)
- 取り出した中身の非 HTTP(S) 参照は 0 件

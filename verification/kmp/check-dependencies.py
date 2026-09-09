#!/usr/bin/env python3
"""KMP 消費者が解決した KsDialogs の配布物と Swift 参照を検査する。

消費者ビルドの結果から次を確かめる。

1. Android アプリと共有モジュールの iOS 3 ターゲットの依存解決に、jp.kamusoft:ksdialogs-kmp の
   全 publication (root / android / iosArm64 / iosSimulatorArm64 / iosX64) が指定 version で
   現れること
2. Android アプリで推移的に入る jp.kamusoft:ksdialogs-core が ksdialogs-kmp と同じ version で
   あること
3. 参照先に置かれた KMP artifact の SwiftPM 連携 metadata の Swift 参照が、mode どおりの URL・
   指定 version の exact・iOS 17.0 の deployment target を持つこと
4. 発行 metadata から再生成された合成 package の依存 URL と、iOS ホスト側のローカル Swift
   package の依存 URL が同一で、いずれも指定 version の exact であること
5. xcodebuild の解決結果に配信リポジトリの pin が 1 つだけ現れること (2 つに割れていれば
   同じ配布物が別 package として二重に解決されている)

使い方:
    check-dependencies.py --expected-version <version> --expected-url <Swift 参照の URL>
                          --android-tree <:androidApp:dependencies の出力>
                          --ios-tree <ターゲット名>=<:shared:dependencies の出力> (3 回)
                          --linkage-package <KotlinMultiplatformLinkedPackage のパス>
                          --local-manifest <VerificationApp/Package.swift>
                          --xcodebuild-log <xcodebuild の出力>
                          [--metadata <swiftpm-metadata.json>]
    check-dependencies.py --selftest

--metadata を省くと 3 を検査しない (公開レジストリから解決する smoke では、参照先が
ローカルのディレクトリではないため metadata を読めない)。
"""

import argparse
import glob
import json
import os
import re
import sys

GROUP = "jp.kamusoft"
KMP_ARTIFACT = "ksdialogs-kmp"
CORE_ARTIFACT = "ksdialogs-core"

# 共有モジュールの iOS ターゲットと、対応する publication の artifact 名。
IOS_TARGET_ARTIFACTS = {
    "iosArm64": "ksdialogs-kmp-iosarm64",
    "iosSimulatorArm64": "ksdialogs-kmp-iossimulatorarm64",
    "iosX64": "ksdialogs-kmp-iosx64",
}
ANDROID_ARTIFACT = "ksdialogs-kmp-android"

# 全 publication (root + android + iOS 3 ターゲット)。
ALL_PUBLICATIONS = (KMP_ARTIFACT, ANDROID_ARTIFACT) + tuple(IOS_TARGET_ARTIFACTS.values())

# 発行 metadata が Swift 参照に載せる iOS の最低対象 OS。
EXPECTED_IOS_DEPLOYMENT = "17.0"

SWIFT_URL_RE = re.compile(r'url:\s*"(?P<url>[^"]+)"')
SWIFT_EXACT_RE = re.compile(r'exact:\s*"(?P<exact>[^"]+)"')

# xcodebuild が解決した package の pin。`<名前>: <URL または パス> @ <version>` の形で出る。
PIN_RE = re.compile(
    r"^\s*[^:\s]+:\s+(?P<url>\S*KsDialogs-SPM(?:\.git)?)\s+@\s+(?P<version>\S+)\s*$",
    re.MULTILINE,
)


def resolved_versions(tree_text, artifact):
    """依存ツリーから 1 つの座標の解決版を集める。

    版が競合したときの Gradle の表記 (「宣言 -> 解決」) では解決側を取る。
    """
    pattern = re.compile(
        re.escape(f"{GROUP}:{artifact}:") + r"(?P<declared>\S+)"
        r"(?:.*?->\s+(?P<resolved>\S+))?"
    )
    versions = set()
    for line in tree_text.splitlines():
        matched = pattern.search(line)
        if matched is None:
            continue
        versions.add(matched.group("resolved") or matched.group("declared"))
    return versions


def check_tree(name, tree_text, artifact, expected_version, rows, failures):
    versions = sorted(resolved_versions(tree_text, artifact))
    rows.append((name, f"{GROUP}:{artifact}", ", ".join(versions) or "(未解決)"))
    if not versions:
        failures.append(f"{name} の解決結果に {GROUP}:{artifact} が無い")
    elif versions != [expected_version]:
        failures.append(
            f"{name} の {GROUP}:{artifact} の解決版が要求と異なる: "
            f"要求 {expected_version} / 解決 {', '.join(versions)}"
        )


def read_swift_reference(manifest_path):
    """Swift のマニフェストから依存の URL と exact を読む。"""
    with open(manifest_path, encoding="utf-8") as f:
        text = f.read()
    url = SWIFT_URL_RE.search(text)
    exact = SWIFT_EXACT_RE.search(text)
    return (url.group("url") if url else None, exact.group("exact") if exact else None)


def find_linkage_manifest(linkage_package):
    """合成 package の中で Swift 参照を持つ subpackage のマニフェストを 1 つ特定する。

    ルートのマニフェストは subpackage を path で参照するだけで URL を持たない。
    """
    candidates = sorted(glob.glob(os.path.join(linkage_package, "subpackages", "*", "Package.swift")))
    if len(candidates) != 1:
        raise LookupError(
            f"合成 package の subpackage が 1 つではありません: {len(candidates)} 件 ({linkage_package})"
        )
    return candidates[0]


def check_metadata(metadata_path, expected_url, expected_version, rows, failures):
    with open(metadata_path, encoding="utf-8") as f:
        metadata = json.load(f)

    deployment = metadata.get("iosDeploymentVersion")
    rows.append(("metadata", "iosDeploymentVersion", str(deployment)))
    if deployment != EXPECTED_IOS_DEPLOYMENT:
        failures.append(
            f"発行 metadata の iOS deployment target が {EXPECTED_IOS_DEPLOYMENT} でない: {deployment}"
        )

    dependencies = metadata.get("dependencies") or []
    if len(dependencies) != 1:
        failures.append(f"発行 metadata の Swift 参照が 1 件でない: {len(dependencies)} 件")
        return

    dependency = dependencies[0]
    url = (dependency.get("repository") or {}).get("value")
    version = dependency.get("version") or {}
    exact = version.get("value") if str(version.get("type", "")).endswith("Exact") else None

    rows.append(("metadata", "Swift 参照の URL", str(url)))
    rows.append(("metadata", "Swift 参照の exact", str(exact)))

    if url != expected_url:
        failures.append(
            f"発行 metadata の Swift 参照の URL が mode と異なる: 期待 {expected_url} / 実際 {url}"
        )
    if exact != expected_version:
        failures.append(
            f"発行 metadata の Swift 参照の exact が要求 version と異なる: "
            f"期待 {expected_version} / 実際 {exact}"
        )


def check(
    expected_version,
    expected_url,
    android_tree,
    ios_trees,
    linkage_package,
    local_manifest,
    xcodebuild_log,
    metadata=None,
    out=sys.stdout,
    err=sys.stderr,
):
    rows = []
    failures = []

    with open(android_tree, encoding="utf-8") as f:
        android_text = f.read()
    check_tree("androidApp", android_text, KMP_ARTIFACT, expected_version, rows, failures)
    check_tree("androidApp", android_text, ANDROID_ARTIFACT, expected_version, rows, failures)
    check_tree("androidApp", android_text, CORE_ARTIFACT, expected_version, rows, failures)

    for target, tree_path in ios_trees:
        artifact = IOS_TARGET_ARTIFACTS.get(target)
        if artifact is None:
            failures.append(f"知らない iOS ターゲットの依存ツリーが渡された: {target}")
            continue
        with open(tree_path, encoding="utf-8") as f:
            text = f.read()
        check_tree(target, text, KMP_ARTIFACT, expected_version, rows, failures)
        check_tree(target, text, artifact, expected_version, rows, failures)

    given_targets = {target for target, _ in ios_trees}
    for target in IOS_TARGET_ARTIFACTS:
        if target not in given_targets:
            failures.append(f"{target} の依存ツリーが渡されていない (全 publication を確認できない)")

    if metadata is not None:
        check_metadata(metadata, expected_url, expected_version, rows, failures)
    else:
        rows.append(("metadata", "(検査しない)", "参照先がローカルのディレクトリでない"))

    linkage_manifest = find_linkage_manifest(linkage_package)
    linkage_url, linkage_exact = read_swift_reference(linkage_manifest)
    local_url, local_exact = read_swift_reference(local_manifest)
    rows.append(("合成 package", "Swift 参照", f"{linkage_url} @ {linkage_exact}"))
    rows.append(("ローカル package", "Swift 参照", f"{local_url} @ {local_exact}"))

    if linkage_url != expected_url:
        failures.append(
            f"合成 package の依存 URL が mode と異なる: 期待 {expected_url} / 実際 {linkage_url}"
        )
    if local_url != linkage_url:
        failures.append(
            f"合成 package と iOS ホスト側の依存 URL が一致しない: "
            f"合成 {linkage_url} / ホスト {local_url}"
        )
    for name, exact in (("合成 package", linkage_exact), ("ローカル package", local_exact)):
        if exact != expected_version:
            failures.append(
                f"{name} の exact が要求 version と異なる: 期待 {expected_version} / 実際 {exact}"
            )

    with open(xcodebuild_log, encoding="utf-8") as f:
        pins = PIN_RE.findall(f.read())
    rows.append(("xcodebuild", "配信リポジトリの pin", f"{len(pins)} 件"))
    for url, version in pins:
        rows.append(("xcodebuild", "pin", f"{url} @ {version}"))
    if len(pins) != 1:
        failures.append(f"xcodebuild の解決結果の配信リポジトリの pin が 1 つでない: {len(pins)} 件")

    width = max(len(row[0]) for row in rows)
    for name, key, value in rows:
        print(f"{name.ljust(width)}  {key}  {value}", file=out)

    if failures:
        print("", file=err)
        for failure in failures:
            print(f"エラー: {failure}", file=err)
        return 1

    print(
        f"全 {len(ALL_PUBLICATIONS)} publication と {CORE_ARTIFACT} が {expected_version} で解決され、"
        "Swift 参照は合成 package・ホスト側・発行 metadata で一致し、pin は 1 つ",
        file=out,
    )
    return 0


SELFTEST_VERSION = "1.2.3"
SELFTEST_URL = "file:///feed/KsDialogs-SPM"


def selftest_tree(entries):
    """依存ツリーの出力を組み立てる。entries は (artifact, version) の並び。"""
    lines = ["releaseRuntimeClasspath"]
    for artifact, version in entries:
        lines.append(f"+--- {GROUP}:{artifact}:{version}")
    return "\n".join(lines) + "\n"


def selftest_manifest(url, exact):
    return (
        "// swift-tools-version: 6.3\n"
        "import PackageDescription\n"
        "let package = Package(\n"
        "  name: \"selftest\",\n"
        "  dependencies: [\n"
        f"    .package(\n      url: \"{url}\",\n      exact: \"{exact}\"\n    )\n"
        "  ]\n"
        ")\n"
    )


def selftest_metadata(url, exact, deployment=EXPECTED_IOS_DEPLOYMENT):
    return {
        "iosDeploymentVersion": deployment,
        "dependencies": [
            {
                "repository": {"type": "SwiftPMDependency.Remote.Repository.Url", "value": url},
                "version": {"type": "SwiftPMDependency.Remote.Version.Exact", "value": exact},
                "products": [{"name": "KsDialogs"}],
                "packageName": "KsDialogs-SPM",
            }
        ],
    }


def selftest_log(pins):
    lines = ["Resolved source packages:"]
    for index in range(pins):
        lines.append(f"  KsDialogs{index or ''}: {SELFTEST_URL} @ {SELFTEST_VERSION}")
    return "\n".join(lines) + "\n"


def selftest():
    import io
    import tempfile

    failures = 0

    def report(ok, name, detail=""):
        nonlocal failures
        failures += 0 if ok else 1
        # 詳細は失敗したときだけ出す (通ったときの出力で結果が埋もれないようにする)。
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail and not ok else ''}")

    with tempfile.TemporaryDirectory() as tmp:
        def write(rel, text):
            path = os.path.join(tmp, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)
            return path

        def build(
            core_version=SELFTEST_VERSION,
            ios_artifacts=IOS_TARGET_ARTIFACTS,
            linkage_url=SELFTEST_URL,
            metadata_url=SELFTEST_URL,
            metadata_exact=SELFTEST_VERSION,
            metadata_deployment=EXPECTED_IOS_DEPLOYMENT,
            pins=1,
        ):
            android_tree = write(
                "android.txt",
                selftest_tree(
                    [
                        (KMP_ARTIFACT, SELFTEST_VERSION),
                        (ANDROID_ARTIFACT, SELFTEST_VERSION),
                        (CORE_ARTIFACT, core_version),
                    ]
                ),
            )
            ios_trees = []
            for target, artifact in ios_artifacts.items():
                entries = [(KMP_ARTIFACT, SELFTEST_VERSION)]
                if artifact is not None:
                    entries.append((artifact, SELFTEST_VERSION))
                ios_trees.append((target, write(f"{target}.txt", selftest_tree(entries))))

            write(
                "linkage/subpackages/jp_kamusoft_ksdialogs_kmp/Package.swift",
                selftest_manifest(linkage_url, SELFTEST_VERSION),
            )
            local_manifest = write(
                "local/Package.swift", selftest_manifest(SELFTEST_URL, SELFTEST_VERSION)
            )
            metadata_path = write(
                "metadata.json",
                json.dumps(selftest_metadata(metadata_url, metadata_exact, metadata_deployment)),
            )
            log = write("xcodebuild.log", selftest_log(pins))

            out, err = io.StringIO(), io.StringIO()
            code = check(
                SELFTEST_VERSION,
                SELFTEST_URL,
                android_tree,
                ios_trees,
                os.path.join(tmp, "linkage"),
                local_manifest,
                log,
                metadata=metadata_path,
                out=out,
                err=err,
            )
            return code, out.getvalue() + err.getvalue()

        print("[正の入力]")
        code, output = build()
        report(code == 0, "全 publication と Swift 参照が揃っていれば exit 0", output.strip())

        print("[負の入力]")
        missing = dict(IOS_TARGET_ARTIFACTS)
        missing["iosX64"] = None
        code, output = build(ios_artifacts=missing)
        report(code == 1, "iOS publication を 1 件欠けば exit 1")
        report(
            IOS_TARGET_ARTIFACTS["iosX64"] in output, "欠けた publication が理由として出る", output.strip()
        )

        code, output = build(core_version="9.9.9")
        report(code == 1, "ksdialogs-core が別版なら exit 1")
        report("解決版が要求と異なる" in output, "版の不一致が理由として出る", output.strip())

        code, output = build(metadata_exact="9.9.9")
        report(code == 1, "metadata の exact が違えば exit 1")
        code, output = build(metadata_url="https://example.invalid/Other")
        report(code == 1, "metadata の URL が違えば exit 1")
        code, output = build(metadata_deployment="16.0")
        report(code == 1, "metadata の deployment target が違えば exit 1")

        code, output = build(linkage_url="https://github.com/kamusoft/KsDialogs-SPM")
        report(code == 1, "合成 package とホスト側の URL が食い違えば exit 1")
        report("依存 URL が一致しない" in output, "URL の不一致が理由として出る", output.strip())

        code, output = build(pins=0)
        report(code == 1, "pin が 0 件なら exit 1")
        code, output = build(pins=2)
        report(code == 1, "pin が 2 件なら exit 1")
        report("pin が 1 つでない" in output, "pin の件数が理由として出る", output.strip())

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv):
    if "--selftest" in argv:
        return selftest()

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--expected-url", required=True)
    parser.add_argument("--android-tree", required=True)
    parser.add_argument("--ios-tree", action="append", default=[], metavar="ターゲット名=パス")
    parser.add_argument("--linkage-package", required=True)
    parser.add_argument("--local-manifest", required=True)
    parser.add_argument("--xcodebuild-log", required=True)
    parser.add_argument("--metadata")
    args = parser.parse_args(argv)

    ios_trees = []
    for item in args.ios_tree:
        target, separator, path = item.partition("=")
        if not separator:
            parser.error(f"--ios-tree は <ターゲット名>=<パス> の形です: {item}")
        ios_trees.append((target, path))

    return check(
        args.expected_version,
        args.expected_url,
        args.android_tree,
        ios_trees,
        args.linkage_package,
        args.local_manifest,
        args.xcodebuild_log,
        metadata=args.metadata,
    )


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

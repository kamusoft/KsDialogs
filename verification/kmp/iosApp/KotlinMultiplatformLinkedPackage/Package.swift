// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("17.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(path: "subpackages/jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0", package: "jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0")
      ]
    )
  ]
)

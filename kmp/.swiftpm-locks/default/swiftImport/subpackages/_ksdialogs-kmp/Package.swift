// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_ksdialogs-kmp",
  platforms: [
    .iOS("17.0")
  ],
  products: [
    .library(
      name: "_ksdialogs-kmp",
      type: .none,
      targets: ["_ksdialogs-kmp"]
    )
  ],
  dependencies: [
    .package(
      path: "../../../../../../ios"
    )
  ],
  targets: [
    .target(
      name: "_ksdialogs-kmp",
      dependencies: [
        .product(
          name: "KsDialogs",
          package: "ios"
        )
      ]
    )
  ]
)

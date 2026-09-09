import SwiftUI
import KsDialogs

struct ContentView: View {
    var body: some View {
        Button("Show toast") {
            Toast.shared.show(message: "Saved")
        }
    }
}

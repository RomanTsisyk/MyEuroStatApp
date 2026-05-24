import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinIOSKt.initKoinIos()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

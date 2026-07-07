import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Kotlin/Native prefixes `init*` functions with `do` in the ObjC/Swift
        // export (`init` is a reserved initializer family in ObjC).
        KoinIOSKt.doInitKoinIos()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

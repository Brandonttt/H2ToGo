import SwiftUI
import UIKit
import Shared

/// Muestra la app de Compose Multiplatform (módulo :shared) dentro de SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Backend en Azure por defecto. Para el backend local en el simulador:
        //   SIMCTL_CHILD_H2TOGO_API=http://localhost:8088/api/v1/ xcrun simctl launch booted com.htogo.app
        // (o en Xcode: Scheme > Run > Arguments > Environment Variables > H2TOGO_API).
        let api = ProcessInfo.processInfo.environment["H2TOGO_API"]
            ?? "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/"
        return MainViewControllerKt.MainViewController(baseUrl: api)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose maneja los márgenes seguros (safeDrawingPadding)
    }
}

import SwiftUI
import UIKit
import Shared

/// Muestra la app de Compose Multiplatform (módulo :shared) dentro de SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Backend en Azure. Para el backend local desde el simulador usa "http://localhost:8088/api/v1/".
        MainViewControllerKt.MainViewController(baseUrl: "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/")
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose maneja los márgenes seguros (safeDrawingPadding)
    }
}

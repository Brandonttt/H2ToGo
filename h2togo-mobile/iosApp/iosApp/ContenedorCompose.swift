import UIKit
import Shared

/// Muestra la app de Compose Multiplatform (módulo :shared) a pantalla completa; Compose maneja
/// los márgenes seguros. Las pantallas con encabezado oscuro piden íconos claros en la barra de
/// estado (hora, batería) y este contenedor los aplica.
class ContenedorCompose: UIViewController {
    private var iconosClaros = false {
        didSet { setNeedsStatusBarAppearanceUpdate() }
    }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        iconosClaros ? .lightContent : .darkContent
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // Backend en Azure por defecto. Para el backend local en el simulador:
        //   SIMCTL_CHILD_H2TOGO_API=http://localhost:8088/api/v1/ xcrun simctl launch booted com.htogo.app
        // (o en Xcode: Scheme > Run > Arguments > Environment Variables > H2TOGO_API).
        let api = ProcessInfo.processInfo.environment["H2TOGO_API"]
            ?? "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/"
        let compose = MainViewControllerKt.MainViewController(baseUrl: api) { [weak self] claros in
            self?.iconosClaros = claros.boolValue
        }
        addChild(compose)
        compose.view.frame = view.bounds
        compose.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(compose.view)
        compose.didMove(toParent: self)
    }
}

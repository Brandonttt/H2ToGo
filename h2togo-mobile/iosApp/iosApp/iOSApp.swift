import UIKit

/// Arranque con UIKit (no SwiftUI) para que el contenedor decida el color de la barra de estado:
/// SwiftUI ignora el estilo que piden los controladores hijos.
@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    /// Ventana para una escena restaurada de una versión anterior de la app (ver abajo).
    private var ventanaRespaldo: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Al actualizar desde la versión con SwiftUI, iOS restaura la escena guardada con su
        // configuración anterior y no crea SceneDelegate: la pantalla quedaría negra.
        NotificationCenter.default.addObserver(
            forName: UIScene.willConnectNotification, object: nil, queue: .main
        ) { [weak self] aviso in
            guard let escena = aviso.object as? UIWindowScene, !(escena.delegate is SceneDelegate) else { return }
            self?.ventanaRespaldo = SceneDelegate.crearVentana(en: escena)
        }
        return true
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuracion = UISceneConfiguration(name: "Default", sessionRole: connectingSceneSession.role)
        configuracion.delegateClass = SceneDelegate.self
        return configuracion
    }
}

/// Crea la ventana con la app de Compose ([ContenedorCompose]).
class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        window = SceneDelegate.crearVentana(en: windowScene)
    }

    static func crearVentana(en escena: UIWindowScene) -> UIWindow {
        let window = UIWindow(windowScene: escena)
        window.rootViewController = ContenedorCompose()
        window.makeKeyAndVisible()
        return window
    }
}

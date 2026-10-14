package com.htogo.shared

import platform.UIKit.UIDevice

actual val plataforma: String = UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion

package com.htogo.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.htogo.shared.data.API_AZURE

/** Punto de entrada que usa SwiftUI (iosApp/ContentView.swift). */
fun MainViewController(baseUrl: String = API_AZURE) = ComposeUIViewController { App(baseUrl) }

package com.sam.nox

import androidx.compose.ui.window.ComposeUIViewController

// Factory function em PascalCase: é o entry point consumido pelo lado Swift do
// app iOS (convenção do Compose Multiplatform), por isso o nome não segue camelCase.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }

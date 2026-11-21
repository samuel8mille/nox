plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    // Quality gates: formatação (ktlint) e análise estática (detekt).
    // Aplicados a cada subprojeto via `subprojects {}` abaixo.
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    // ── ktlint ──────────────────────────────────────────────────────────────
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        // Relatórios úteis no CI e para o desenvolvedor.
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
        filter {
            // Não analisa código gerado (build/) nem recursos de Compose.
            exclude { it.file.path.contains("/build/") }
        }
    }

    // ── detekt ──────────────────────────────────────────────────────────────
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        // Configuração única na raiz, versionada.
        config.setFrom(rootProject.file("detekt.yml"))
        // Mescla com os defaults do detekt (só sobrescreve o que o detekt.yml define).
        buildUponDefaultConfig = true
        autoCorrect = false
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            html.required.set(true)
            xml.required.set(true)
            sarif.required.set(true)
            txt.required.set(false)
        }
        exclude { it.file.path.contains("/build/") }
    }
}

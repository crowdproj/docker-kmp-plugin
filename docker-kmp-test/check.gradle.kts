plugins {
    alias(libs.plugins.kotlin.multiplatform)
}
kotlin { jvm { } }
afterEvaluate {
    println("=== PLUGINS AFTER EVALUATE ===")
    project.plugins.forEach { p ->
        println("  Plugin: ${p.javaClass.name} (id: ${p.javaClass.interfaces?.getOrNull(0)?.name ?: "?"})")
    }
}
println("=== PLUGINS DURING CONFIG ===")
project.plugins.forEach { p ->
    println("  Plugin: ${p.javaClass.name}")
}

package com.codingwithtashi.dailyprayer.ui

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** Guards the Tibetan-font rule: only ui/components/Text.kt may import Material's Text; everything else uses AppText. */
class UiTextImportGuardTest {
    private val relative = "src/main/java/com/codingwithtashi/dailyprayer/ui"

    private fun uiDir(): File {
        val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val candidates = generateSequence(start) { it.parentFile }.take(4)
            .flatMap { sequenceOf(File(it, relative), File(it, "app/$relative")) }
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not find $relative from working directory $start")
    }

    @Test fun onlyTextKtImportsMaterialText() {
        val offenders = uiDir().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.invariantSeparatorsPath.endsWith("/ui/components/Text.kt") }
            .filter { f -> f.readLines().any { it.trim() == "import androidx.compose.material3.Text" } }
            .map { it.name }
            .toList()
        assertWithMessage("Use AppText instead of material3.Text in: $offenders").that(offenders).isEmpty()
    }
}

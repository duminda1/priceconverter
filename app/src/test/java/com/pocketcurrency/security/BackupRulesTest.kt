package com.pocketcurrency.security

import com.pocketcurrency.util.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class BackupRulesTest {

    @Test
    fun backupRules_includeDefaultPrefs() {
        val xml = readXml("backup_rules.xml")
        val expected = "path=\"${Constants.PREFS_NAME}.xml\""

        assertTrue("backup_rules.xml should include $expected", xml.contains(expected))
    }

    @Test
    fun backupRules_excludeEncryptedPrefs() {
        val xml = readXml("backup_rules.xml")
        val expected = "path=\"${Constants.PREFS_SECURE_NAME}.xml\""

        assertFalse("backup_rules.xml should not include $expected", xml.contains(expected))
    }

    @Test
    fun dataExtractionRules_includeDefaultPrefs() {
        val xml = readXml("data_extraction_rules.xml")
        val expected = "path=\"${Constants.PREFS_NAME}.xml\""

        assertTrue("data_extraction_rules.xml should include $expected", xml.contains(expected))
    }

    @Test
    fun dataExtractionRules_excludeEncryptedPrefs() {
        val xml = readXml("data_extraction_rules.xml")
        val expected = "path=\"${Constants.PREFS_SECURE_NAME}.xml\""

        assertFalse("data_extraction_rules.xml should not include $expected", xml.contains(expected))
    }

    private fun readXml(fileName: String): String {
        val candidates = listOf(
            Paths.get("src/main/res/xml", fileName),
            Paths.get("app/src/main/res/xml", fileName)
        )
        val path = candidates.firstOrNull { Files.exists(it) }
            ?: error("Missing $fileName in expected paths: ${candidates.joinToString()}")

        return String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    }
}

package com.driveflow.driver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Hardening and integrity tests to guarantee:
 * 1. The app NEVER uses performAction or dispatchGesture (it strictly reads and shows, never clicks or acts).
 * 2. Manifest declares only least-privilege permissions.
 * 3. Data extraction and backup rules exclude all application data.
 */
class SecurityHardeningTest {

    @Test
    fun testAppNeverUsesPerformActionOrDispatchGesture() {
        val srcDir = File("src/main/java")
        val targetDir = if (srcDir.exists()) srcDir else File("app/src/main/java")
        assertTrue("Source directory must exist", targetDir.exists())

        val forbiddenKeywords = listOf("performAction", "dispatchGesture")
        val offendingFiles = mutableListOf<String>()

        targetDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            for (keyword in forbiddenKeywords) {
                if (text.contains(keyword)) {
                    offendingFiles.add("${file.name} contains forbidden call: $keyword")
                }
            }
        }

        assertTrue(
            "Found forbidden interaction methods in source files: $offendingFiles",
            offendingFiles.isEmpty()
        )
    }

    @Test
    fun testManifestDisallowsBackupAndDeclaresStrictPermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml").let {
            if (it.exists()) it else File("app/src/main/AndroidManifest.xml")
        }
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val manifestText = manifestFile.readText()

        // 1. Hardening: allowBackup must be false
        assertTrue(
            "AndroidManifest.xml must set android:allowBackup=\"false\"",
            manifestText.contains("android:allowBackup=\"false\"")
        )

        // 2. Strict least-privilege permissions:
        // Permitted: INTERNET, VIBRATE, SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS
        // Prohibited: broad storage, dangerous sensors, call phone, sms, location, camera
        val prohibitedPermissions = listOf(
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
            "READ_MEDIA_IMAGES",
            "RECORD_AUDIO",
            "CAMERA",
            "ACCESS_FINE_LOCATION",
            "ACCESS_COARSE_LOCATION",
            "READ_CONTACTS",
            "READ_SMS",
            "SEND_SMS"
        )
        for (prohibited in prohibitedPermissions) {
            assertFalse(
                "Manifest must NOT declare $prohibited",
                manifestText.contains(prohibited)
            )
        }
    }

    @Test
    fun testDataExtractionRulesExcludeAll() {
        val extractionFile = File("src/main/res/xml/data_extraction_rules.xml").let {
            if (it.exists()) it else File("app/src/main/res/xml/data_extraction_rules.xml")
        }
        assertTrue("data_extraction_rules.xml must exist", extractionFile.exists())
        val text = extractionFile.readText()
        assertTrue("Must have cloud-backup block", text.contains("<cloud-backup>"))
        assertTrue("Must have device-transfer block", text.contains("<device-transfer>"))
        assertTrue("Must exclude root", text.contains("<exclude domain=\"root\" path=\".\" />"))
        assertFalse("Must not include any domain", text.contains("<include"))
    }

    @Test
    fun testBackupRulesExcludeAll() {
        val backupFile = File("src/main/res/xml/backup_rules.xml").let {
            if (it.exists()) it else File("app/src/main/res/xml/backup_rules.xml")
        }
        assertTrue("backup_rules.xml must exist", backupFile.exists())
        val text = backupFile.readText()
        assertTrue("Must have full-backup-content block", text.contains("<full-backup-content>"))
        assertTrue("Must exclude root", text.contains("<exclude domain=\"root\" path=\".\" />"))
        assertFalse("Must not include any domain", text.contains("<include"))
    }
}

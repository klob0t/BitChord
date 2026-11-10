package com.music.bitchord.desktop

// This App was build by Chris Tambayong - Fumakill4

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class DesktopMacUpdatesTest {
    private fun release(tag: String, asset: String, draft: Boolean = false, prerelease: Boolean = false): JsonObject =
        Json.parseToJsonElement("""{"tag_name":"$tag","draft":$draft,"prerelease":$prerelease,"assets":[{"name":"$asset","state":"uploaded","browser_download_url":"https://github.com/fumakill4/BitChord/releases/download/$tag/$asset"}]}""") as JsonObject

    @Test fun aNewerWindowsOrIosReleaseDoesNotHideTheLatestMacInstaller() {
        val mac = release("macos-v1.9.1.E4", "BitChord-1.9.1.E4-macos-arm64.dmg")
        val selected = DesktopUpdateChecker.selectMacRelease(JsonArray(listOf(
            release("v2.0.0-windows", "BitChord-2.0.0-windows-x64-setup.exe"),
            release("v2.0.0-ios", "BitChord-iOS-v2.0.0.ipa"), mac,
        )), arm64 = true)
        assertEquals(mac, selected)
        assertTrue(DesktopUpdateChecker.macInstallerUrl(mac, true)!!.endsWith("-macos-arm64.dmg"))
    }

    @Test fun draftsPreviewsAndWrongArchitecturesAreSkipped() {
        val stable = release("macos-v1.9.1.E4", "BitChord-1.9.1.E4-macos-arm64.dmg")
        val selected = DesktopUpdateChecker.selectMacRelease(JsonArray(listOf(
            release("macos-v9.0.0", "BitChord-9.0.0-macos-arm64.dmg", draft = true),
            release("macos-v8.0.0", "BitChord-8.0.0-macos-arm64.dmg", prerelease = true),
            release("macos-v7.0.0", "BitChord-7.0.0-macos-x64.dmg"), stable,
        )), arm64 = true)
        assertEquals(stable, selected)
        assertNull(DesktopUpdateChecker.macInstallerUrl(stable, arm64 = false))
    }

    @Test fun anIntelMacIsOfferedTheX64InstallerFromAReleaseCarryingBoth() {
        val both = Json.parseToJsonElement("""{"tag_name":"macos-v1.9.1.E4","draft":false,"prerelease":false,"assets":[
            {"name":"BitChord-1.9.1.E4-macos-arm64.dmg","state":"uploaded","browser_download_url":"https://example.invalid/BitChord-1.9.1.E4-macos-arm64.dmg"},
            {"name":"BitChord-1.9.1.E4-macos-x64.dmg","state":"uploaded","browser_download_url":"https://example.invalid/BitChord-1.9.1.E4-macos-x64.dmg"}]}""") as JsonObject
        val armOnly = release("macos-v1.9.2.E1", "BitChord-1.9.2.E1-macos-arm64.dmg")
        // A newer release that has no Intel build must not be offered to an Intel Mac.
        assertEquals(both, DesktopUpdateChecker.selectMacRelease(JsonArray(listOf(armOnly, both)), arm64 = false))
        assertTrue(DesktopUpdateChecker.macInstallerUrl(both, arm64 = false)!!.endsWith("-macos-x64.dmg"))
        assertTrue(DesktopUpdateChecker.macInstallerUrl(both, arm64 = true)!!.endsWith("-macos-arm64.dmg"))
    }

    @Test fun editBuildComparisonAndExistingBetaOrderingRemainCorrect() {
        assertTrue(DesktopUpdateChecker.isNewer("1.9.1.E4", "1.9.1.E3"))
        assertFalse(DesktopUpdateChecker.isNewer("1.9.1.E4", "1.9.1.E4"))
        assertFalse(DesktopUpdateChecker.isNewer("1.9.1.E3", "1.9.1.E4"))
        assertTrue(DesktopUpdateChecker.isNewer("1.9.2", "1.9.1.E4"))
        assertTrue(DesktopUpdateChecker.isNewer("1.9.1", "1.9.1-beta1"))
        assertEquals("1.9.1.E4", DesktopUpdateChecker.releaseVersion("macos-v1.9.1.E4"))
    }
}

package com.sf.tadami.network.utils

/**
 * Pure user-agent helpers (no Android dependencies, unit tested).
 *
 * Cloudflare binds `cf_clearance` to the exact user agent that solved the challenge, and its
 * challenge script compares the user agent with what the browser engine reports (client hints,
 * platform, touch support...). A desktop UA on top of an Android WebView, or one claiming an old
 * Chrome on a recent engine, is an easy mismatch to spot: the challenge then loops even once
 * solved. The default UA is therefore the one Chrome for Android sends, with the installed
 * WebView's major version.
 */
object UserAgents {

    /** Used when the WebView version cannot be read (no WebView, or it is being updated). */
    const val FALLBACK_CHROME_MAJOR = 141

    /** Defaults shipped by older versions; a stored value equal to one of them means "use the default". */
    val LEGACY_DEFAULTS = setOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 Edg/88.0.705.63",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36",
    )

    private val CHROME_MAJOR_REGEX = """(?:Chrome|CriOS)/(\d+)""".toRegex()

    fun desktopChrome(major: Int): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Safari/537.36"
    }

    /** Chrome for Android's reduced user agent (the Android version and model are frozen). */
    fun mobileChrome(major: Int): String {
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Mobile Safari/537.36"
    }

    /**
     * WebView providers whose package version follows Chromium's (`153.0.8010.57`). Others, like
     * Huawei's `com.huawei.webview` (versions 14.x, 15.x), number their releases independently.
     */
    private val CHROMIUM_VERSIONED_PROVIDERS = listOf(
        "com.google.android.webview",
        "com.android.webview",
        "com.android.chrome",
        "com.chrome.",
        "org.chromium.",
        "org.bromite.",
        "app.vanadium.",
    )

    /** Lowest major accepted from a package version, below it the number isn't a Chromium one. */
    private const val MIN_PLAUSIBLE_CHROMIUM_MAJOR = 70

    /** Major version from a WebView package version name such as `153.0.8010.57`. */
    fun majorFromVersionName(versionName: String?): Int? {
        return versionName?.substringBefore('.')?.trim()?.toIntOrNull()?.takeIf { it > 0 }
    }

    /**
     * Chromium major version of a WebView provider package, or null when its version doesn't
     * follow Chromium's numbering.
     */
    fun chromiumMajorFromPackage(packageName: String?, versionName: String?): Int? {
        if (packageName == null || CHROMIUM_VERSIONED_PROVIDERS.none { packageName.startsWith(it) }) {
            return null
        }
        return majorFromVersionName(versionName)?.takeIf { it >= MIN_PLAUSIBLE_CHROMIUM_MAJOR }
    }

    fun chromeMajor(userAgent: String): Int? {
        return CHROME_MAJOR_REGEX.find(userAgent)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun isLegacyDefault(userAgent: String?): Boolean {
        return userAgent != null && userAgent.trim() in LEGACY_DEFAULTS
    }

    /**
     * The client hints a real Chrome would send with [userAgent], or null when the UA is not a
     * Chromium one (client hints are then left to the WebView).
     */
    fun clientHintsFor(userAgent: String): ClientHints? {
        val major = chromeMajor(userAgent) ?: return null
        val platform = when {
            "Windows" in userAgent -> "Windows"
            "Android" in userAgent -> "Android"
            "CrOS" in userAgent -> "Chrome OS"
            "Macintosh" in userAgent || "Mac OS X" in userAgent -> "macOS"
            "Linux" in userAgent -> "Linux"
            else -> return null
        }
        val mobile = platform == "Android" && "Mobile" in userAgent
        val platformVersion = when (platform) {
            // Windows 10 reports 10.0.0, Windows 11 reports 13+; the UA string can't tell them apart
            "Windows" -> "10.0.0"
            "Android" -> """Android (\d+(?:\.\d+)*)""".toRegex().find(userAgent)?.groupValues?.get(1) ?: ""
            "macOS" -> """Mac OS X (\d+(?:[._]\d+)*)""".toRegex().find(userAgent)?.groupValues?.get(1)
                ?.replace('_', '.') ?: ""
            else -> ""
        }
        val desktop = platform == "Windows" || platform == "macOS" || platform == "Linux" || platform == "Chrome OS"
        return ClientHints(
            brands = chromeBrands(major),
            fullVersion = "$major.0.0.0",
            platform = platform,
            platformVersion = platformVersion,
            mobile = mobile,
            architecture = if (desktop) "x86" else "",
            bitness = if (desktop) 64 else 0,
        )
    }

    private val GREASE_CHARS = listOf(" ", "(", ":", "-", ".", "/", ")", ";", "=", "?", "_")
    private val GREASE_VERSIONS = listOf("8", "99", "24")
    private val BRAND_ORDERS = listOf(
        listOf(0, 1, 2), listOf(0, 2, 1), listOf(1, 0, 2), listOf(1, 2, 0), listOf(2, 0, 1), listOf(2, 1, 0),
    )

    /**
     * The brands Chrome [major] announces: Chromium, Google Chrome and a "GREASE" brand, which
     * Chrome derives from its major version, as well as their order (Chromium's
     * `GetGreasedUserAgentBrandVersion` and `ShuffleBrandList`).
     */
    fun chromeBrands(major: Int): List<Brand> {
        val greaseVersion = GREASE_VERSIONS[major % GREASE_VERSIONS.size]
        val grease = "Not${GREASE_CHARS[major % GREASE_CHARS.size]}A${GREASE_CHARS[(major + 1) % GREASE_CHARS.size]}Brand"
        val brands = listOf(
            Brand(grease, greaseVersion, "$greaseVersion.0.0.0"),
            Brand("Chromium", major.toString(), "$major.0.0.0"),
            Brand("Google Chrome", major.toString(), "$major.0.0.0"),
        )
        val order = BRAND_ORDERS[major % BRAND_ORDERS.size]
        val shuffled = arrayOfNulls<Brand>(brands.size)
        brands.forEachIndexed { index, brand -> shuffled[order[index]] = brand }
        return shuffled.filterNotNull()
    }

    data class ClientHints(
        val brands: List<Brand>,
        val fullVersion: String,
        val platform: String,
        val platformVersion: String,
        val mobile: Boolean,
        val architecture: String,
        val bitness: Int,
    )

    data class Brand(val name: String, val majorVersion: String, val fullVersion: String)
}

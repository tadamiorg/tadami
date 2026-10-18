package com.sf.tadami.ui.webview.adblock

import java.util.Locale

/**
 * Reads the network rules of filter lists (Adblock Plus / uBlock Origin syntax) and hosts files into
 * [AdBlockRules]:
 * - `||example.com^`, with a path (`||example.com/ads/`) or options, and `@@` exceptions;
 * - `0.0.0.0 example.com` hosts lines and bare `example.com` lines, read as `||example.com^`;
 * - options `third-party`/`3p`, `~third-party`/`1p`, `popup`, `document`, `all`, the resource types,
 *   `domain=`/`from=`, `important` and `badfilter`.
 *
 * Cosmetic rules, scriptlets, URL patterns not anchored to a domain, regular expressions and rules with
 * other options are skipped: a rule only blocks when it's understood in full.
 */
class FilterParser {

    private val blocks = HashMap<String, HostRules>()
    private val exceptions = HashMap<String, HostRules>()
    private val badFilters = ArrayList<ParsedRule>()

    private class HostRules {
        var mask = 0

        // Most hosts only have simple rules
        var rules: LinkedHashSet<FilterRule>? = null
    }

    /** A rule of a list, for the host it's anchored to. */
    data class ParsedRule(val host: String, val exception: Boolean, val rule: FilterRule, val badFilter: Boolean = false)

    /**
     * Adds the rules of a list.
     *
     * @return how many rules it had
     */
    fun addAll(lines: Sequence<String>): Int {
        var count = 0
        lines.forEach { line ->
            val parsed = parseLine(line) ?: return@forEach
            count++
            if (parsed.badFilter) {
                badFilters += parsed
            } else {
                add(parsed)
            }
        }
        return count
    }

    private fun add(parsed: ParsedRule) {
        val hostRules = (if (parsed.exception) exceptions else blocks).getOrPut(parsed.host) { HostRules() }
        if (parsed.rule.isSimple) {
            hostRules.mask = hostRules.mask or parsed.rule.simpleMask
        } else {
            (hostRules.rules ?: LinkedHashSet<FilterRule>().also { hostRules.rules = it }) += parsed.rule
        }
    }

    fun build(siteOf: (String) -> String = AdBlockRules::registrableDomain): AdBlockRules {
        badFilters.forEach { bad ->
            val hostRules = (if (bad.exception) exceptions else blocks)[bad.host] ?: return@forEach
            if (bad.rule.isSimple) {
                hostRules.mask = hostRules.mask and bad.rule.simpleMask.inv()
            } else {
                hostRules.rules?.remove(bad.rule)
            }
        }
        return AdBlockRules(blocks.toTable(), exceptions.toTable(), siteOf)
    }

    private fun Map<String, HostRules>.toTable(): HostTable = HostTable.build(
        filterValues { it.mask != 0 || !it.rules.isNullOrEmpty() }
            .mapValues { (_, rules) -> rules.mask to rules.rules.orEmpty() },
    )

    companion object {
        private val COSMETIC = Regex("#@?[?$%]{0,2}#")
        private val PLAIN = FilterRule(FilterRule.ALL_KINDS)
        private val HOSTS_FILE_ADDRESSES = setOf("0.0.0.0", "127.0.0.1", "::", "::1")
        private val HOSTS_FILE_NAMES = setOf("localhost", "localhost.localdomain", "local", "broadcasthost", "0.0.0.0")
        private val SUBRESOURCE_TYPES = setOf(
            "script", "image", "stylesheet", "css", "object", "xmlhttprequest", "xhr", "subdocument", "frame",
            "ping", "beacon", "media", "font", "websocket", "other", "webrtc", "object-subrequest",
        )

        /** The rule of a line, or null for comments, cosmetic rules and anything not supported. */
        fun parseLine(raw: String): ParsedRule? {
            val line = raw.trim()
            if (line.isEmpty() || line[0] == '!' || line[0] == '[' || line[0] == '#') return null
            // Cosmetic rules, scriptlets and HTML filters
            if (line.indexOf('#') >= 0 && COSMETIC.containsMatchIn(line)) return null
            if (line.contains("$$") || line.contains("\$@\$")) return null

            val exception = line.startsWith("@@")
            val rule = if (exception) line.substring(2) else line
            if (!rule.startsWith("||")) return if (exception) null else parseHostsLine(line)

            val optionsStart = rule.lastIndexOf('$')
            val pattern = (if (optionsStart >= 0) rule.substring(2, optionsStart) else rule.substring(2)).lowercase(Locale.ROOT)
            val options = parseOptions(if (optionsStart >= 0) rule.substring(optionsStart + 1) else "") ?: return null

            val hostEnd = pattern.indexOfFirst { it == '^' || it == '/' || it == ':' || it == '?' || it == '|' || it == '*' }
                .takeIf { it >= 0 } ?: pattern.length
            val host = pattern.substring(0, hostEnd)
            if (!isHost(host)) return null
            val rest = pattern.substring(hostEnd).trimEnd('*')
            val path = when {
                rest.isEmpty() || rest == "^" -> null
                // A wildcard in the host, or a host prefix: not a domain
                rest[0] == '*' || rest[0] == '|' -> return null
                else -> rest
            }
            return ParsedRule(
                host = host,
                exception = exception,
                rule = FilterRule(options.flags, path, options.domains, options.notDomains),
                badFilter = options.badFilter,
            )
        }

        /** `0.0.0.0 host`, or a bare `host`. */
        private fun parseHostsLine(line: String): ParsedRule? {
            val content = line.substringBefore('#').trim()
            val space = content.indexOfFirst { it == ' ' || it == '\t' }
            val host = if (space < 0) {
                content
            } else {
                if (content.substring(0, space) !in HOSTS_FILE_ADDRESSES) return null
                content.substring(space + 1).trim().takeIf { rest -> rest.none { it == ' ' || it == '\t' } } ?: return null
            }.lowercase(Locale.ROOT).trimEnd('.')
            if (host in HOSTS_FILE_NAMES || !isHost(host)) return null
            return ParsedRule(host, exception = false, rule = PLAIN)
        }

        /** Dot-separated labels of letters, digits, `-` and `_`, at least two. */
        private fun isHost(host: String): Boolean {
            var labelStart = 0
            var dots = 0
            for (i in 0..host.length) {
                if (i == host.length || host[i] == '.') {
                    if (i == labelStart || host[labelStart] == '-' || host[i - 1] == '-') return false
                    if (i < host.length) dots++
                    labelStart = i + 1
                } else {
                    val c = host[i]
                    if (c !in 'a'..'z' && c !in '0'..'9' && c != '-' && c != '_') return false
                }
            }
            return dots > 0
        }

        private class Options(
            val flags: Int,
            val domains: List<String>,
            val notDomains: List<String>,
            val badFilter: Boolean,
        )

        /** Null when an option isn't supported, so the rule is skipped. */
        private fun parseOptions(text: String): Options? {
            var kinds = 0
            var negatedKind = false
            var party = 0
            var important = false
            var badFilter = false
            val domains = ArrayList<String>()
            val notDomains = ArrayList<String>()
            if (text.isNotEmpty()) {
                for (raw in text.split(',')) {
                    val option = raw.trim().lowercase(Locale.ROOT)
                    val negated = option.startsWith('~')
                    val name = option.removePrefix("~")
                    when {
                        name == "third-party" || name == "3p" ->
                            party = if (negated) FilterRule.FIRST_PARTY else FilterRule.THIRD_PARTY
                        name == "first-party" || name == "1p" ->
                            party = if (negated) FilterRule.THIRD_PARTY else FilterRule.FIRST_PARTY
                        negated && (name in SUBRESOURCE_TYPES || name == "document" || name == "doc" || name == "popup") ->
                            negatedKind = true
                        name in SUBRESOURCE_TYPES -> kinds = kinds or FilterRule.SUBRESOURCE
                        name == "document" || name == "doc" -> kinds = kinds or FilterRule.DOCUMENT
                        name == "popup" || name == "popunder" -> kinds = kinds or FilterRule.POPUP
                        name == "all" -> kinds = kinds or FilterRule.ALL_KINDS
                        name == "important" && !negated -> important = true
                        name == "badfilter" && !negated -> badFilter = true
                        // uBlock Origin's no-op option, for readability
                        name.isNotEmpty() && name.all { it == '_' } && !negated -> Unit
                        (name.startsWith("domain=") || name.startsWith("from=")) && !negated -> {
                            for (entry in name.substringAfter('=').split('|')) {
                                val exclude = entry.startsWith('~')
                                val domain = entry.removePrefix("~").trimEnd('.')
                                // Regular expressions and wildcards other than an entity (name.*)
                                if (domain.isEmpty() || domain.startsWith('/') || domain.removeSuffix(".*").contains('*')) {
                                    return null
                                }
                                if (exclude) notDomains += domain else domains += domain
                            }
                        }
                        else -> return null
                    }
                }
            }
            if (kinds == 0) {
                kinds = if (negatedKind) FilterRule.SUBRESOURCE else FilterRule.ALL_KINDS
            }
            return Options(
                flags = kinds or party or (if (important) FilterRule.IMPORTANT else 0),
                domains = domains,
                notDomains = notDomains,
                badFilter = badFilter,
            )
        }
    }
}

package com.sf.tadami.ui.webview.adblock

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** What a request is, for the rules that only apply to some kinds. */
enum class RequestKind {
    /** Anything a page loads: scripts, images, frames, XHR... The WebView doesn't tell them apart. */
    SUBRESOURCE,

    /** A navigation of a window to a page. */
    DOCUMENT,

    /** A page opening in a new window: a popup, or a navigation the screen turns into one. */
    POPUP,
}

/**
 * A request to check.
 *
 * @param tail what follows the host in the URL, lowercase: `:port` when not the default one, the path
 * and the query
 * @param contextHost the host of the document making the request (its frame, or the page opening a
 * popup), for `$third-party` and `$domain=`. Null when unknown: a block that depends on it doesn't
 * apply, an exception does.
 * @param pageHost the host of the page shown, for the pages a `$document` exception allows
 */
class AdBlockRequest(
    val host: String,
    val tail: String,
    val kind: RequestKind,
    val contextHost: String? = null,
    val pageHost: String? = null,
) {
    companion object {
        fun of(url: HttpUrl, kind: RequestKind, contextHost: String? = null, pageHost: String? = null): AdBlockRequest {
            val port = if (url.port != HttpUrl.defaultPort(url.scheme)) ":${url.port}" else ""
            val query = url.encodedQuery?.let { "?$it" } ?: ""
            return AdBlockRequest(
                host = url.host,
                tail = (port + url.encodedPath + query).lowercase(),
                kind = kind,
                contextHost = contextHost?.lowercase()?.trimEnd('.'),
                pageHost = pageHost?.lowercase()?.trimEnd('.'),
            )
        }

        fun of(url: String, kind: RequestKind, contextHost: String? = null, pageHost: String? = null): AdBlockRequest? =
            url.toHttpUrlOrNull()?.let { of(it, kind, contextHost, pageHost) }
    }
}

/**
 * A network rule of a filter list beyond a plain host, see [FilterParser].
 *
 * @param flags the kinds it applies to, its party and whether it's `$important`
 * @param path what must follow the host (ABP pattern: `*`, `^` and a final `|`), null for the whole host
 * @param domains the `$domain=` pages it's limited to (`name.*` for any public suffix)
 * @param notDomains the `$domain=~` pages it doesn't apply on
 */
data class FilterRule(
    val flags: Int,
    val path: String? = null,
    val domains: List<String> = emptyList(),
    val notDomains: List<String> = emptyList(),
) {
    val isSimple: Boolean get() = path == null && domains.isEmpty() && notDomains.isEmpty() && flags and IMPORTANT == 0

    /** Its kinds and party as the bits of a [HostTable] mask, for a [isSimple] rule. */
    val simpleMask: Int
        get() {
            val party = when (flags and PARTY_MASK) {
                FIRST_PARTY -> 1
                THIRD_PARTY -> 2
                else -> 0
            }
            return RequestKind.entries.fold(0) { mask, kind ->
                if (flags and kindBit(kind) != 0) mask or (1 shl (kind.ordinal * 3 + party)) else mask
            }
        }

    companion object {
        const val SUBRESOURCE = 1
        const val DOCUMENT = 2
        const val POPUP = 4
        const val ALL_KINDS = SUBRESOURCE or DOCUMENT or POPUP
        const val FIRST_PARTY = 8
        const val THIRD_PARTY = 16
        const val PARTY_MASK = FIRST_PARTY or THIRD_PARTY
        const val IMPORTANT = 32

        fun kindBit(kind: RequestKind): Int = 1 shl kind.ordinal
    }
}

/**
 * The hosts the rules of one sort (blocks or exceptions) are for, sorted in one byte array so a
 * hundred thousand of them take a few MB and load without building a String each.
 */
class HostTable internal constructor(
    private val hosts: ByteArray,
    private val ends: IntArray,
    private val masks: IntArray,
    private val ruleStarts: IntArray,
    private val rules: Array<FilterRule>,
) {
    val size: Int get() = ends.size
    val byteSize: Long get() = hosts.size + 4L * (ends.size + masks.size + ruleStarts.size)

    /** Index of the host made of [host] from [from] on, or -1. */
    fun indexOf(host: String, from: Int): Int {
        var low = 0
        var high = ends.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cmp = compare(mid, host, from)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return -1
    }

    fun mask(index: Int): Int = masks[index]

    fun rulesOf(index: Int): List<FilterRule> =
        if (ruleStarts[index] == ruleStarts[index + 1]) emptyList() else rules.asList().subList(ruleStarts[index], ruleStarts[index + 1])

    private fun compare(index: Int, host: String, from: Int): Int {
        var i = if (index == 0) 0 else ends[index - 1]
        val end = ends[index]
        var j = from
        while (i < end && j < host.length) {
            val diff = (hosts[i].toInt() and 0xFF) - host[j].code
            if (diff != 0) return diff
            i++
            j++
        }
        return (end - i) - (host.length - j)
    }

    internal fun write(out: DataOutputStream) {
        out.writeInt(ends.size)
        out.writeInt(hosts.size)
        out.write(hosts)
        ends.forEach(out::writeInt)
        masks.forEach(out::writeInt)
        ruleStarts.forEach(out::writeInt)
        out.writeInt(rules.size)
        rules.forEach { rule ->
            out.writeInt(rule.flags)
            out.writeBoolean(rule.path != null)
            rule.path?.let(out::writeUTF)
            out.writeShort(rule.domains.size)
            rule.domains.forEach(out::writeUTF)
            out.writeShort(rule.notDomains.size)
            rule.notDomains.forEach(out::writeUTF)
        }
    }

    companion object {
        val EMPTY = HostTable(ByteArray(0), IntArray(0), IntArray(0), IntArray(1), emptyArray())

        internal fun read(input: DataInputStream): HostTable {
            val count = input.readInt()
            val hosts = ByteArray(input.readInt()).also(input::readFully)
            val ends = IntArray(count) { input.readInt() }
            val masks = IntArray(count) { input.readInt() }
            val ruleStarts = IntArray(count + 1) { input.readInt() }
            val rules = Array(input.readInt()) {
                FilterRule(
                    flags = input.readInt(),
                    path = if (input.readBoolean()) input.readUTF() else null,
                    domains = List(input.readShort().toInt()) { input.readUTF() },
                    notDomains = List(input.readShort().toInt()) { input.readUTF() },
                )
            }
            if ((ends.lastOrNull() ?: 0) != hosts.size || ruleStarts.last() != rules.size) {
                throw IOException("Corrupted host table")
            }
            return HostTable(hosts, ends, masks, ruleStarts, rules)
        }

        /** [hostRules]: per host, the simple rules' mask and the other rules. */
        internal fun build(hostRules: Map<String, Pair<Int, Collection<FilterRule>>>): HostTable {
            val sorted = hostRules.keys.sorted()
            val hosts = java.io.ByteArrayOutputStream()
            val ends = IntArray(sorted.size)
            val masks = IntArray(sorted.size)
            val ruleStarts = IntArray(sorted.size + 1)
            val rules = ArrayList<FilterRule>()
            sorted.forEachIndexed { i, host ->
                hosts.write(host.toByteArray(Charsets.US_ASCII))
                ends[i] = hosts.size()
                val (mask, hostRuleList) = hostRules.getValue(host)
                masks[i] = mask
                ruleStarts[i] = rules.size
                rules.addAll(hostRuleList)
            }
            ruleStarts[sorted.size] = rules.size
            return HostTable(hosts.toByteArray(), ends, masks, ruleStarts, rules.toTypedArray())
        }
    }
}

/**
 * Compiled network rules of the filter lists: a request is blocked when a block rule matches it and
 * no exception does, unless the block is `$important`. Rules match the request host and its parent
 * domains (`||example.com^` also covers `ads.example.com`).
 *
 * @param siteOf the registrable domain of a host, for `$third-party` and `name.*` domains
 */
class AdBlockRules(
    val blocks: HostTable,
    val exceptions: HostTable,
    private val siteOf: (String) -> String = ::registrableDomain,
) {

    fun isBlocked(request: AdBlockRequest): Boolean {
        val host = request.host.lowercase().trimEnd('.')
        if (host.isEmpty() || NEVER_BLOCKED.any { host == it || host.endsWith(".$it") }) return false
        val context = Context(request, host)
        val block = match(blocks, context, lenient = false)
        if (block == NONE) return false
        if (block == IMPORTANT_MATCH) return true
        if (request.kind == RequestKind.SUBRESOURCE && request.pageHost?.let(::isPageAllowed) == true) return false
        return match(exceptions, context, lenient = true) == NONE
    }

    /** A `$document` exception for the page: nothing it loads is blocked. */
    private fun isPageAllowed(pageHost: String): Boolean {
        val docBits = 0b111 shl (RequestKind.DOCUMENT.ordinal * 3)
        return forEachSuffix(exceptions, pageHost) { index ->
            exceptions.mask(index) and docBits != 0 ||
                exceptions.rulesOf(index).any { it.path == null && it.flags and FilterRule.DOCUMENT != 0 }
        }
    }

    private inner class Context(val request: AdBlockRequest, val host: String) {
        private var computedParty = -1

        /** [FilterRule.FIRST_PARTY], [FilterRule.THIRD_PARTY], or 0 when unknown. */
        val party: Int
            get() {
                if (computedParty < 0) {
                    val contextHost = request.contextHost
                    computedParty = when {
                        contextHost.isNullOrEmpty() -> 0
                        siteOf(host) == siteOf(contextHost) -> FilterRule.FIRST_PARTY
                        else -> FilterRule.THIRD_PARTY
                    }
                }
                return computedParty
            }
    }

    private fun match(table: HostTable, context: Context, lenient: Boolean): Int {
        var result = NONE
        val kind = context.request.kind
        val kindBit = FilterRule.kindBit(kind)
        forEachSuffix(table, context.host) { index ->
            val mask = table.mask(index) shr (kind.ordinal * 3)
            if (mask and 0b111 != 0) {
                val matches = mask and 1 != 0 || when (context.party) {
                    FilterRule.FIRST_PARTY -> mask and 0b010 != 0
                    FilterRule.THIRD_PARTY -> mask and 0b100 != 0
                    else -> lenient
                }
                if (matches) result = MATCH
            }
            for (rule in table.rulesOf(index)) {
                if (rule.flags and kindBit == 0 || !rule.appliesTo(context, lenient)) continue
                if (rule.flags and FilterRule.IMPORTANT != 0) {
                    result = IMPORTANT_MATCH
                    return@forEachSuffix true
                }
                result = MATCH
            }
            false
        }
        return result
    }

    private fun FilterRule.appliesTo(context: Context, lenient: Boolean): Boolean {
        val party = flags and FilterRule.PARTY_MASK
        if (party != 0) {
            val actual = context.party
            if (actual == 0) {
                if (!lenient) return false
            } else if (actual != party) {
                return false
            }
        }
        if (domains.isNotEmpty() || notDomains.isNotEmpty()) {
            val contextHost = context.request.contextHost
            if (contextHost.isNullOrEmpty()) {
                if (!lenient) return false
            } else if (!domainsMatch(contextHost)) {
                return false
            }
        }
        return path == null || pathMatches(path, context.request.tail)
    }

    /** The most specific of the domains matching the page decides. */
    private fun FilterRule.domainsMatch(pageHost: String): Boolean {
        var best = -1
        var included = false
        fun consider(domain: String, include: Boolean) {
            val length = domainMatchLength(domain, pageHost)
            if (length > best) {
                best = length
                included = include
            }
        }
        domains.forEach { consider(it, true) }
        notDomains.forEach { consider(it, false) }
        return if (best < 0) domains.isEmpty() else included
    }

    /** How specific [domain] is when it matches [host], or -1. */
    private fun domainMatchLength(domain: String, host: String): Int {
        if (domain.endsWith(".*")) {
            val name = domain.dropLast(1)
            return if (siteOf(host).startsWith(name)) name.length else -1
        }
        return if (host == domain || host.endsWith(".$domain")) domain.length else -1
    }

    /**
     * Calls [action] with the index of each of [host] and its parent domains found in [table], until it
     * returns true.
     *
     * @return whether [action] returned true
     */
    private inline fun forEachSuffix(table: HostTable, host: String, action: (Int) -> Boolean): Boolean {
        if (table.size == 0) return false
        var from = 0
        while (from < host.length) {
            val index = table.indexOf(host, from)
            if (index >= 0 && action(index)) return true
            val dot = host.indexOf('.', from)
            if (dot < 0) break
            from = dot + 1
        }
        return false
    }

    fun write(out: DataOutputStream) {
        out.writeInt(MAGIC)
        out.writeInt(FORMAT_VERSION)
        blocks.write(out)
        exceptions.write(out)
    }

    companion object {
        private const val NONE = 0
        private const val MATCH = 1
        private const val IMPORTANT_MATCH = 2

        private const val MAGIC = 0x54414442 // TADB
        private const val FORMAT_VERSION = 1

        /** Anti-bot challenges must always load, whatever the lists say. */
        private val NEVER_BLOCKED = listOf("challenges.cloudflare.com", "hcaptcha.com", "recaptcha.net")

        val EMPTY = AdBlockRules(HostTable.EMPTY, HostTable.EMPTY)

        fun read(input: DataInputStream, siteOf: (String) -> String = ::registrableDomain): AdBlockRules {
            if (input.readInt() != MAGIC) throw IOException("Not compiled ad block rules")
            val version = input.readInt()
            if (version != FORMAT_VERSION) throw IOException("Unsupported ad block rules version $version")
            return AdBlockRules(HostTable.read(input), HostTable.read(input), siteOf)
        }

        /** Matches [path] (an ABP pattern) against the start of [tail]. */
        internal fun pathMatches(path: String, tail: String): Boolean {
            var p = 0
            var t = 0
            var starP = -1
            var starT = 0
            while (true) {
                if (p == path.length) return true
                val c = path[p]
                if (c == '*') {
                    starP = p
                    starT = t
                    p++
                    continue
                }
                if (c == '|' && p == path.length - 1) {
                    if (t == tail.length) return true
                } else if (t < tail.length && (if (c == '^') isSeparator(tail[t]) else tail[t] == c)) {
                    p++
                    t++
                    continue
                } else if (t == tail.length && c == '^') {
                    // The end of the URL is a separator too
                    p++
                    continue
                }
                if (starP < 0 || starT >= tail.length) return false
                starT++
                t = starT
                p = starP + 1
            }
        }

        private fun isSeparator(c: Char): Boolean =
            !(c.isLetterOrDigit() || c == '_' || c == '-' || c == '.' || c == '%')

        fun registrableDomain(host: String): String =
            "http://$host/".toHttpUrlOrNull()?.topPrivateDomain() ?: host
    }
}

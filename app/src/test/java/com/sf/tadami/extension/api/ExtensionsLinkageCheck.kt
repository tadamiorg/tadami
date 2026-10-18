package com.sf.tadami.extension.api

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.util.ExtensionsLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration

/**
 * Downloads every APK of the published extensions repo and resolves each class, method and field
 * their code references against the classes of the app (and of its libraries and Android), the way
 * ART links them: what doesn't resolve would fail with a LinkageError at runtime. The extensions the
 * app loads (lib versions [ExtensionsLoader.API_VERSION_MIN] to MAX) must link; the older ones must
 * be refused as LIB_TOO_OLD. Also fails when a method of an extension overrides, by accident, an app
 * method added after the lib version it was built against.
 *
 * Needs the network: not run with the unit tests, but by `./gradlew :app:extensionsLinkageCheck`
 * (`-Ptadami.extensionsRepo=<raw URL>` for another repo). Its report is in
 * `app/build/extensions-linkage/report.txt`.
 */
class ExtensionsLinkageCheck {

    private val repoUrl = System.getProperty("tadami.extensionsRepo")?.trimEnd('/') ?: DEFAULT_REPO
    private val workDir = File(System.getProperty("tadami.linkageDir") ?: "build/extensions-linkage")
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** A published APK, its lib version from its index entry or from its name */
    private class Apk(val name: String, val versionName: String?, val sha256: String?)

    @Test
    fun publishedExtensionsLinkAgainstTheApp() {
        val apks = publishedApks()
        assertTrue("No APK found in $repoUrl", apks.isNotEmpty())
        val report = StringBuilder("Extensions of $repoUrl against the app, lib ${ExtensionsLoader.API_VERSION_MIN} to ${ExtensionsLoader.API_VERSION_MAX}\n")
        val failures = mutableListOf<String>()
        var loadable = 0

        for (apk in apks) {
            val file = download(apk)
            val libVersion = apk.versionName?.let { LibVersion.fromVersionName(it) }
            val issue = ExtensionsLoader.libVersionIssue(libVersion)
            val checker = Checker(file, libVersion)
            val problems = checker.problems()
            val summary = "${apk.name}: lib $libVersion, ${checker.classCount} classes, ${checker.referenceCount} external references"
            when (issue) {
                null -> {
                    loadable++
                    if (problems.isEmpty()) {
                        report.append("OK   $summary, all resolve\n")
                    } else {
                        report.append("FAIL $summary, ${problems.size} problems:\n")
                        problems.forEach { report.append("       $it\n") }
                        failures += problems.map { "${apk.name}: $it" }
                    }
                }
                ExtensionLoadIssue.LIB_TOO_OLD -> report.append(
                    "OLD  $summary: refused as LIB_TOO_OLD" +
                        (if (problems.isEmpty()) "\n" else ", which keeps ${problems.size} problems from running, e.g. ${problems.first()}\n")
                )
                else -> {
                    report.append("FAIL $summary: refused as $issue\n")
                    failures += "${apk.name}: refused as $issue, a published extension must be loadable or LIB_TOO_OLD"
                }
            }
        }
        workDir.mkdirs()
        File(workDir, "report.txt").writeText(report.toString())
        println(report)
        assertTrue("No published extension the app can load", loadable > 0)
        assertTrue(failures.joinToString("\n", "Published extensions that wouldn't link against the app:\n"), failures.isEmpty())
    }

    // The published APKs

    /** Every APK of the repo's apk/ folder, those of the index with their version */
    private fun publishedApks(): List<Apk> {
        val index = Json.parseToJsonElement(get("$repoUrl/index.min.json").decodeToString()).jsonArray
            .map { it.jsonObject }
            .associateBy { it.string("apk")!! }
        val listed = listApkFolder()
        if (listed == null) println("Couldn't list $repoUrl/apk, only checking the ${index.size} APKs of its index")
        return (listed ?: index.keys.toList()).distinct().sorted().map { name ->
            val entry = index[name]
            Apk(
                name = name,
                // tadami-<lang>.<name>-v<versionName>.apk
                versionName = entry?.string("version") ?: name.substringAfterLast("-v").removeSuffix(".apk"),
                sha256 = entry?.string("sha256"),
            )
        }
    }

    /** The APK names of the repo's apk/ folder from the GitHub API, null when it can't tell */
    private fun listApkFolder(): List<String>? {
        val match = Regex("""https://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)""").matchEntire(repoUrl) ?: return null
        val (owner, repo, branch) = match.destructured
        return runCatching {
            Json.parseToJsonElement(get("https://api.github.com/repos/$owner/$repo/contents/apk?ref=$branch", github = true).decodeToString())
                .let { it as JsonArray }
                .map { it.jsonObject.string("name")!! }
                .filter { it.endsWith(".apk") }
        }.onFailure { println("GitHub API: $it") }.getOrNull()
    }

    private fun download(apk: Apk): File {
        val file = File(workDir, "apk/${apk.name}")
        if (!file.isFile || apk.sha256 != null && sha256(file.readBytes()) != apk.sha256) {
            val bytes = get("$repoUrl/apk/${apk.name}")
            apk.sha256?.let { expected ->
                check(sha256(bytes) == expected) { "${apk.name}: SHA-256 ${sha256(bytes)}, the index says $expected" }
            }
            file.parentFile.mkdirs()
            file.writeBytes(bytes)
        }
        return file
    }

    private fun get(url: String, github: Boolean = false): ByteArray {
        val request = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofMinutes(2)).apply {
            if (github) {
                header("Accept", "application/vnd.github+json")
                System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }
        }.build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        check(response.statusCode() == 200) { "GET $url: HTTP ${response.statusCode()}" }
        return response.body()
    }

    private fun JsonObject.string(key: String) = get(key)?.jsonPrimitive?.content

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // Linking

    /** A class, of the extension or of the app, by internal name */
    private class Info(
        val name: String,
        val superName: String?,
        val interfaces: List<String>,
        val access: Int,
        /** Access flags by name + descriptor */
        val methods: Map<String, Int>,
        /** Access flags by name + ":" + descriptor */
        val fields: Map<String, Int>,
        val inExtension: Boolean,
    )

    /** Resolves the references of the extension [apk], built against [libVersion] */
    private inner class Checker(apk: File, libVersion: LibVersion?) {
        private val dexClasses: Map<String, ClassDef> = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault()).let { container ->
            container.dexEntryNames.flatMap { container.getEntry(it)!!.dexFile.classes }
        }.associateBy { internalName(it.type) }

        private val baseline: Set<String> = libVersion?.let { File(baselinesDir, "$it.txt") }
            ?.takeIf { it.isFile }
            ?.readLines()
            ?.filter { it.isNotBlank() && !it.startsWith("#") }
            ?.map { it.split(' ').take(3).joinToString(" ") }
            ?.toSet()
            .orEmpty()

        val classCount get() = dexClasses.size
        var referenceCount = 0
            private set

        private val infos = HashMap<String, Info?>()

        private fun info(name: String): Info? = infos.getOrPut(name) {
            dexClasses[name]?.let { dex ->
                Info(
                    name = name,
                    superName = dex.superclass?.let(::internalName),
                    interfaces = dex.interfaces.map(::internalName),
                    access = dex.accessFlags,
                    methods = dex.methods.associate { it.name + descriptor(it.parameterTypes, it.returnType) to it.accessFlags },
                    fields = dex.fields.associate { it.name + ":" + it.type to it.accessFlags },
                    inExtension = true,
                )
            } ?: appClass(name)
        }

        fun problems(): List<String> = buildList {
            for ((name, dex) in dexClasses) {
                val info = info(name)!!
                (listOfNotNull(info.superName) + info.interfaces)
                    .filter { info(it) == null }
                    .forEach { add("$name extends $it: missing from the app") }

                dex.methods.forEach { method ->
                    method.implementation?.instructions?.forEach { instruction ->
                        val reference = (instruction as? ReferenceInstruction)?.reference ?: return@forEach
                        // The constant, INVOKE_STATIC, SGET_OBJECT..., not the mnemonic
                        val opcode = (instruction.opcode as Enum<*>).name
                        check(name, opcode, reference)?.let { add("${dex.type}->${method.name}: $it") }
                    }
                }
                addAll(missingImplementations(info))
                addAll(accidentalOverrides(info))
            }
        }.distinct()

        /** What's wrong with [reference] from the class [from], with the instruction [opcode], or null */
        private fun check(from: String, opcode: String, reference: Any): String? {
            when (reference) {
                is TypeReference -> {
                    val type = elementType(reference.type) ?: return null
                    if (dexClasses.containsKey(type)) return null
                    referenceCount++
                    return if (info(type) == null) "class $type: missing from the app" else null
                }
                is MethodReference -> {
                    val owner = elementType(reference.definingClass)
                    // Methods of arrays are Object's (clone...)
                    if (owner == null || reference.definingClass.startsWith("[")) return null
                    val desc = descriptor(reference.parameterTypes, reference.returnType)
                    val key = "$owner.${reference.name}$desc"
                    val resolved = resolveMethod(owner, reference.name, desc) ?: run {
                        if (info(owner)?.inExtension == true && !isFromApp(owner)) return null
                        referenceCount++
                        return "method $key: missing from the app"
                    }
                    if (resolved.second.inExtension) return null
                    referenceCount++
                    val isStatic = resolved.first and ACC_STATIC != 0
                    val wantsStatic = opcode.startsWith("INVOKE_STATIC")
                    return when {
                        isStatic != wantsStatic -> "method $key: ${if (isStatic) "static" else "not static"} in the app"
                        resolved.first and ACC_PRIVATE != 0 && resolved.second.name != from -> "method $key: private in the app"
                        else -> null
                    }
                }
                is FieldReference -> {
                    val owner = elementType(reference.definingClass) ?: return null
                    val key = "$owner.${reference.name}:${reference.type}"
                    val resolved = resolveField(owner, reference.name, reference.type) ?: run {
                        if (info(owner)?.inExtension == true && !isFromApp(owner)) return null
                        referenceCount++
                        return "field $key: missing from the app"
                    }
                    if (resolved.second.inExtension) return null
                    referenceCount++
                    val isStatic = resolved.first and ACC_STATIC != 0
                    val wantsStatic = opcode.startsWith("SGET") || opcode.startsWith("SPUT")
                    return when {
                        isStatic != wantsStatic -> "field $key: ${if (isStatic) "static" else "not static"} in the app"
                        resolved.first and ACC_PRIVATE != 0 && resolved.second.name != from -> "field $key: private in the app"
                        else -> null
                    }
                }
                else -> return null
            }
        }

        /** Whether the extension class [name] extends an app class, whose members it may use */
        private fun isFromApp(name: String) = supertypes(name).any { info(it)?.inExtension == false }

        /** The method [name][desc] [owner] resolves to, the way ART does, with its access flags */
        private fun resolveMethod(owner: String, name: String, desc: String): Pair<Int, Info>? {
            val key = name + desc
            if (name == "<init>") return info(owner)?.let { info -> info.methods[key]?.let { it to info } }
            generateSequence(info(owner)) { it.superName?.let(::info) }
                .firstNotNullOfOrNull { info -> info.methods[key]?.let { it to info } }
                ?.let { return it }
            val fromInterfaces = supertypes(owner).mapNotNull(::info)
                .filter { it.access and ACC_INTERFACE != 0 }
                .mapNotNull { info -> info.methods[key]?.let { it to info } }
                .filter { it.first and (ACC_PRIVATE or ACC_STATIC) == 0 }
            return fromInterfaces.firstOrNull { it.first and ACC_ABSTRACT == 0 } ?: fromInterfaces.firstOrNull()
        }

        /** The field [name]:[type] [owner] resolves to: the class, its interfaces, then its superclass */
        private fun resolveField(owner: String, name: String, type: String): Pair<Int, Info>? {
            val info = info(owner) ?: return null
            info.fields["$name:$type"]?.let { return it to info }
            info.interfaces.firstNotNullOfOrNull { resolveField(it, name, type) }?.let { return it }
            return info.superName?.let { resolveField(it, name, type) }
        }

        /** Every superclass and interface of [name] */
        private fun supertypes(name: String): Set<String> {
            val supertypes = linkedSetOf<String>()
            val queue = ArrayDeque(listOf(name))
            while (queue.isNotEmpty()) {
                val info = info(queue.removeFirst()) ?: continue
                (listOfNotNull(info.superName) + info.interfaces).filter { supertypes.add(it) }.forEach { queue.add(it) }
            }
            return supertypes
        }

        /** The abstract methods of the app a concrete class of the extension doesn't implement */
        private fun missingImplementations(info: Info): List<String> {
            if (info.access and (ACC_ABSTRACT or ACC_INTERFACE) != 0) return emptyList()
            val abstractMethods = supertypes(info.name).mapNotNull(::info)
                .filter { !it.inExtension }
                .flatMap { type -> type.methods.filter { it.value and ACC_ABSTRACT != 0 }.keys }
                .toSet()
            return abstractMethods.filter { key ->
                val name = key.substringBefore('(')
                val implementation = resolveMethod(info.name, name, key.substring(name.length))
                implementation == null || implementation.first and ACC_ABSTRACT != 0
            }.map { "${info.name} doesn't implement $it, abstract in the app: AbstractMethodError" }
        }

        /**
         * The methods of [info] with the name and descriptor of an app method its lib version
         * doesn't have: the app would call them instead of its own
         */
        private fun accidentalOverrides(info: Info): List<String> {
            if (baseline.isEmpty()) return emptyList()
            val appSupertypes = supertypes(info.name).mapNotNull(::info)
                .filter { !it.inExtension && it.name.startsWith("com/sf/tadami/") }
            return info.methods
                .filter { (key, access) -> access and (ACC_STATIC or ACC_PRIVATE) == 0 && !key.startsWith("<") }
                .keys
                .mapNotNull { key ->
                    val overridden = appSupertypes
                        .filter { type -> type.methods[key]?.let { it and (ACC_STATIC or ACC_PRIVATE) == 0 } == true }
                    // Meant to override one its lib version has, which a newer supertype may declare too
                    if (overridden.isEmpty() || overridden.any { type -> "${type.name} method $key" in baseline }) {
                        null
                    } else {
                        "${info.name}.$key overrides ${overridden.joinToString { "${it.name}.$key" }}, which its lib version doesn't have"
                    }
                }
        }
    }

    companion object {
        private const val DEFAULT_REPO = "https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"

        private val baselinesDir: File
            get() = File(System.getProperty("tadami.extensionsApiBaselines") ?: error("Run it with Gradle"))

        private val appInfos = HashMap<String, Info?>()

        /** An app, library or Android class from the test classpath, by internal name */
        private fun appClass(name: String): Info? = appInfos.getOrPut(name) {
            ExtensionsLinkageCheck::class.java.classLoader!!.getResourceAsStream("$name.class")?.use { input ->
                val node = ClassNode().also {
                    ClassReader(input.readBytes()).accept(it, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
                }
                Info(
                    name = node.name,
                    superName = node.superName,
                    interfaces = node.interfaces.toList(),
                    access = node.access,
                    methods = node.methods.associate { it.name + it.desc to it.access },
                    fields = node.fields.associate { it.name + ":" + it.desc to it.access },
                    inExtension = false,
                )
            }
        }

        /** com/a/B of Lcom/a/B; */
        private fun internalName(type: String) = type.removePrefix("L").removeSuffix(";")

        /** The class an array or class type stands for, null for a primitive one */
        private fun elementType(type: String): String? = type.trimStart('[').takeIf { it.startsWith("L") }?.let(::internalName)

        private fun descriptor(parameters: List<CharSequence>, returnType: String) =
            parameters.joinToString("", "(", ")") + returnType
    }
}

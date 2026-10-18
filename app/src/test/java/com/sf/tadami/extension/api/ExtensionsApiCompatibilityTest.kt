package com.sf.tadami.extension.api

import com.sf.tadami.extension.util.ExtensionsLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_ANNOTATION
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.jar.JarFile
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.Visibility
import kotlin.metadata.isInline
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.visibility

/**
 * Extensions compile against extensions-lib, a stub of the app's classes, and run against the app's
 * ones. Every class and member of the stub must exist in the app with a compatible JVM signature, or
 * the extensions using it fail with a LinkageError (NoSuchMethodError, AbstractMethodError...). So
 * must those of the lib versions already released, frozen in extensions-lib/api/<major.minor>.txt.
 *
 * Only reads class bytes, nothing is loaded: the stub's from the directory Gradle passes in
 * tadami.extensionsApiClasses, the app's from the test classpath.
 *
 * To fix a failure, give the app back the JVM signature extensions use: a @Deprecated(level = HIDDEN)
 * overload, @JvmOverloads or @JvmName, or an open member with a throwing default instead of an
 * abstract one. A break no published extension is affected by can go in extensions-lib/api/
 * known-breaks.txt, with its reason. See extensions-lib/README.md.
 */
class ExtensionsApiCompatibilityTest {

    @Test
    fun appProvidesTheStubClassesAndMembers() {
        assertNoProblems("extensions-lib", check(stub, compareConstants = true))
    }

    @Test
    fun stubHasNoPublicInlineFunction() {
        val problems = stubNodes.values.filter { isApiClass(it) }.flatMap { node ->
            inlineMembers(node).map { "${node.name} $it: public inline, extensions would get the stub's body" }
        }
        assertTrue(problems.joinToString("\n", "Extensions-lib can't have public inline members:\n"), problems.isEmpty())
    }

    @Test
    fun appProvidesTheReleasedLibVersions() {
        val dir = File(property("tadami.extensionsApiBaselines"))
        System.getProperty("tadami.updateApiBaseline")?.let { updateBaseline(dir, it) }

        val knownBreaks = readKnownBreaks(File(dir, KNOWN_BREAKS))
        val baselines = dir.listFiles { file -> BASELINE_NAME.matches(file.name) }.orEmpty().sortedBy { it.name }
        assertTrue("No baseline in $dir", baselines.isNotEmpty())

        val usedBreaks = mutableSetOf<String>()
        val problems = baselines.flatMap { file ->
            val lines = readBaselineLines(file).filter { line ->
                val breaks = listOf(keyOf(line), "${line.substringBefore(' ')} class").filter { it in knownBreaks }
                usedBreaks += breaks
                breaks.isEmpty()
            }
            check(parseBaseline(lines), compareConstants = false).map { "${file.name}: $it" }
        } + (knownBreaks.keys - usedBreaks).map { "$KNOWN_BREAKS: \"$it\" matches no line of the baselines" }
        assertNoProblems("the released lib versions", problems)
    }

    @Test
    fun apiVersionMaxIsTheLibVersion() {
        val libVersion = property("tadami.extensionsLibVersion")
        assertEquals(
            "ExtensionsLoader.API_VERSION_MAX must be the major.minor of extensionsLibVersion " +
                "($libVersion, in extensions-lib/gradle.properties)",
            majorMinor(libVersion),
            ExtensionsLoader.API_VERSION_MAX.toString(),
        )
    }

    /**
     * The app calls the sources through their suspend methods, whose defaults call the Rx fetches:
     * calling a fetch would bypass a source of lib 1.7 or later implementing only the suspend ones.
     * Only the sources themselves, in com.sf.tadami.source, call the fetches.
     */
    @Test
    fun appCallsTheSourcesThroughTheirSuspendMethods() {
        val calls = appClassFiles()
            .filterNot { (name, _) -> name.startsWith(SOURCE_PACKAGE) }
            .flatMap { (name, bytes) ->
                val node = ClassNode().also { ClassReader(bytes).accept(it, ClassReader.SKIP_FRAMES) }
                node.methods.flatMap { method ->
                    method.instructions.filterIsInstance<MethodInsnNode>()
                        .filter { it.name.removeSuffix("\$default") in RX_FETCHES && isSourceType(it.owner) }
                        .map { "$name.${method.name}: calls ${it.owner}.${it.name}" }
                }
            }
            .distinct()
            .toList()
        assertTrue(
            calls.joinToString("\n", "Call the suspend methods of the sources (getSearchAnime...) instead:\n"),
            calls.isEmpty(),
        )
    }

    /** A class of the extension API, with the members extensions may link against */
    private class ApiClass(
        val name: String,
        /** Its flags among [CLASS_FLAGS] */
        val access: Int,
        val superclass: String?,
        val interfaces: List<String>,
        val members: List<ApiMember>,
    )

    private class ApiMember(
        val isMethod: Boolean,
        val name: String,
        val desc: String,
        /** Its flags among [MEMBER_FLAGS] */
        val access: Int,
        /** The value of a constant field */
        val constant: Any? = null,
    ) {
        val kind get() = if (isMethod) "method" else "field"
        val signature get() = if (isMethod) "$name$desc" else "$name:$desc"
    }

    /** A member of the app that a member of the stub resolves to */
    private class Resolved(val access: Int, val constant: Any? = null)

    private val stub: List<ApiClass>
        get() = stubNodes.values.filter { isApiClass(it) }.map { it.toApiClass() }.sortedBy { it.name }

    // Checks

    /** What each class and member of [api] is missing in the app, one line each */
    private fun check(api: List<ApiClass>, compareConstants: Boolean): List<String> = buildList {
        for (cls in api) {
            val classKey = "${cls.name} class"
            val app = appClass(cls.name)
            if (app == null) {
                add("$classKey: missing from the app")
                continue
            }
            val appAccess = app.classAccess()
            if (cls.access and KIND_FLAGS != appAccess and KIND_FLAGS) {
                add("$classKey: ${kindName(cls.access)} in extensions-lib, ${kindName(appAccess)} in the app")
            }
            if (!isVisibilityKept(cls.access, appAccess)) add("$classKey: not ${visibilityName(cls.access)} in the app")
            if (cls.access and ACC_FINAL == 0 && appAccess and ACC_FINAL != 0) {
                add("$classKey: final in the app, extensions may extend it")
            }
            if (cls.access and ACC_ABSTRACT == 0 && appAccess and ACC_ABSTRACT != 0) {
                add("$classKey: abstract in the app, extensions may instantiate it")
            }

            val supertypes = appSupertypes(cls.name)
            cls.superclass?.takeIf { it !in supertypes }?.let { add("${cls.name} extends $it: not a supertype in the app") }
            cls.interfaces.filter { it !in supertypes }.forEach { add("${cls.name} implements $it: not a supertype in the app") }

            for (member in cls.members) {
                val key = "${cls.name} ${member.kind} ${member.signature}"
                val resolved = if (member.isMethod) {
                    resolveMethod(cls.name, member.name, member.desc)
                } else {
                    resolveField(cls.name, member.name, member.desc)
                }
                if (resolved == null) {
                    add("$key: missing from the app")
                    continue
                }
                if ((member.access xor resolved.access) and ACC_STATIC != 0) {
                    add("$key: ${if (member.access and ACC_STATIC != 0) "not static" else "static"} in the app")
                }
                if (!isVisibilityKept(member.access, resolved.access)) {
                    add("$key: not ${visibilityName(member.access)} in the app")
                }
                if (member.isMethod) {
                    val overridable = cls.access and ACC_FINAL == 0 && member.name != "<init>" &&
                        member.access and (ACC_STATIC or ACC_FINAL) == 0
                    if (overridable && resolved.access and ACC_FINAL != 0) {
                        add("$key: final in the app, extensions may override it")
                    }
                    if (member.access and ACC_ABSTRACT == 0 && resolved.access and ACC_ABSTRACT != 0) {
                        add("$key: abstract in the app, extensions may not implement it")
                    }
                } else {
                    if (member.access and ACC_FINAL == 0 && resolved.access and ACC_FINAL != 0) {
                        add("$key: final in the app, extensions may write it")
                    }
                    if (compareConstants && member.constant != null && member.constant != resolved.constant) {
                        add("$key: constant \"${member.constant}\" in extensions-lib, \"${resolved.constant}\" in the app")
                    }
                }
            }
        }
    }

    /** Resolves a method the way the JVM does: the class, its superclasses, then its interfaces */
    private fun resolveMethod(className: String, name: String, desc: String): Resolved? {
        fun ClassNode.find() = methods.firstOrNull { it.name == name && it.desc == desc }?.let { Resolved(it.access) }
        // Constructors aren't inherited
        if (name == "<init>") return appClass(className)?.find()

        generateSequence(appClass(className)) { node -> node.superName?.let { appClass(it) } }
            .firstNotNullOfOrNull { it.find() }
            ?.let { return it }
        val fromInterfaces = appSupertypes(className)
            .mapNotNull { appClass(it) }
            .filter { it.access and ACC_INTERFACE != 0 }
            .mapNotNull { it.find() }
            .filter { it.access and (ACC_PRIVATE or ACC_STATIC) == 0 }
        return fromInterfaces.firstOrNull { it.access and ACC_ABSTRACT == 0 } ?: fromInterfaces.firstOrNull()
    }

    /** Resolves a field the way the JVM does: the class, its interfaces, then its superclass */
    private fun resolveField(className: String, name: String, desc: String): Resolved? {
        val node = appClass(className) ?: return null
        node.fields.firstOrNull { it.name == name && it.desc == desc }?.let { return Resolved(it.access, it.value) }
        node.interfaces.firstNotNullOfOrNull { resolveField(it, name, desc) }?.let { return it }
        return node.superName?.let { resolveField(it, name, desc) }
    }

    /** Whether [owner] is a source, or the DefaultImpls of a source interface */
    private fun isSourceType(owner: String): Boolean {
        val type = owner.removeSuffix("\$DefaultImpls")
        return type == SOURCE || SOURCE in appSupertypes(type)
    }

    /** The class files of the app itself, by internal name, not those of its libraries */
    private fun appClassFiles(): Sequence<Pair<String, ByteArray>> {
        val root = File(ExtensionsLoader::class.java.protectionDomain!!.codeSource.location.toURI())
        return if (root.isDirectory) {
            root.walk().filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(root).invariantSeparatorsPath.removeSuffix(".class") to it.readBytes() }
        } else {
            val jar = JarFile(root)
            jar.entries().asSequence().filter { it.name.endsWith(".class") }
                .map { it.name.removeSuffix(".class") to jar.getInputStream(it).use { input -> input.readBytes() } }
        }.filter { (name, _) -> name.startsWith("com/sf/tadami/") }
    }

    /** Every superclass and interface of an app class, as far as the test classpath has them */
    private fun appSupertypes(className: String): Set<String> {
        val supertypes = linkedSetOf<String>()
        val queue = ArrayDeque(listOf(className))
        while (queue.isNotEmpty()) {
            val node = appClass(queue.removeFirst()) ?: continue
            (listOfNotNull(node.superName) + node.interfaces).filter { supertypes.add(it) }.forEach { queue.add(it) }
        }
        return supertypes
    }

    /** The public inline functions and properties of a stub class, from its Kotlin metadata */
    @Suppress("UNCHECKED_CAST")
    private fun inlineMembers(node: ClassNode): List<String> {
        val annotation = node.visibleAnnotations?.firstOrNull { it.desc == "Lkotlin/Metadata;" } ?: return emptyList()
        val values = annotation.values.chunked(2).associate { (name, value) -> name as String to value }
        val metadata = Metadata(
            kind = values["k"] as Int?,
            metadataVersion = (values["mv"] as List<Int>?)?.toIntArray(),
            data1 = (values["d1"] as List<String>?)?.toTypedArray(),
            data2 = (values["d2"] as List<String>?)?.toTypedArray(),
            extraString = values["xs"] as String?,
            packageName = values["pn"] as String?,
            extraInt = values["xi"] as Int?,
        )
        val container: KmDeclarationContainer = when (val kotlinClass = KotlinClassMetadata.readLenient(metadata)) {
            is KotlinClassMetadata.Class -> kotlinClass.kmClass
            is KotlinClassMetadata.FileFacade -> kotlinClass.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> kotlinClass.kmPackage
            else -> return emptyList()
        }
        val visible = setOf(Visibility.PUBLIC, Visibility.PROTECTED)
        val functions = container.functions.filter { it.visibility in visible && it.isInline }.map { "fun ${it.name}" }
        val properties = container.properties
            .filter { it.visibility in visible && (it.getter.isInline || it.setter?.isInline == true) }
            .map { "val ${it.name}" }
        return functions + properties
    }

    private fun assertNoProblems(what: String, problems: List<String>) {
        if (problems.isEmpty()) return
        fail(
            "The app doesn't provide what extensions link against ($what), they would fail with a " +
                "LinkageError:\n" + problems.joinToString("\n") { "  $it" } + "\n" +
                "Give the app back these JVM signatures (a @Deprecated(level = HIDDEN) overload, @JvmOverloads, " +
                "@JvmName, an open member with a throwing default instead of an abstract one), or fix the stub " +
                "in extensions-lib. See extensions-lib/README.md."
        )
    }

    // Baselines: one line per class, supertype and member, "<class> <kind> [<signature>] [<flags>]"

    private fun ApiClass.lines(): List<String> = buildList {
        add("$name class ${flagNames(access)}")
        superclass?.let { add("$name extends $it") }
        interfaces.forEach { add("$name implements $it") }
        members.forEach { add("$name ${it.kind} ${it.signature} ${flagNames(it.access)}") }
    }

    private fun parseBaseline(lines: List<String>): List<ApiClass> =
        lines.map { it.split(' ') }.groupBy { it[0] }.map { (name, classLines) ->
            val classLine = classLines.singleOrNull { it[1] == "class" } ?: error("$name has no class line")
            ApiClass(
                name = name,
                access = flags(classLine.drop(2)),
                superclass = classLines.firstOrNull { it[1] == "extends" }?.get(2),
                interfaces = classLines.filter { it[1] == "implements" }.map { it[2] },
                members = classLines.filter { it[1] == "method" || it[1] == "field" }.map { tokens ->
                    val isMethod = tokens[1] == "method"
                    val signature = tokens[2]
                    ApiMember(
                        isMethod = isMethod,
                        name = if (isMethod) signature.substringBefore('(') else signature.substringBefore(':'),
                        desc = if (isMethod) "(" + signature.substringAfter('(') else signature.substringAfter(':'),
                        access = flags(tokens.drop(3)),
                    )
                },
            )
        }

    /** The key of a baseline line, what known-breaks.txt lists: the line without its flags */
    private fun keyOf(line: String): String {
        val tokens = line.split(' ')
        return tokens.take(if (tokens[1] == "class") 2 else 3).joinToString(" ")
    }

    private fun readBaselineLines(file: File) = file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }

    /** known-breaks.txt: "<key> <reason>" lines, the key being a baseline line without its flags */
    private fun readKnownBreaks(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.associate { line ->
            val tokens = line.trim().split(' ')
            val keySize = if (tokens.getOrNull(1) == "class") 2 else 3
            val reason = tokens.drop(keySize).joinToString(" ")
            require(reason.isNotBlank()) { "$KNOWN_BREAKS: \"$line\" needs a reason" }
            tokens.take(keySize).joinToString(" ") to reason
        }
    }

    /**
     * Adds the stub to extensions-lib/api/<version>.txt, done when tagging lib-<version>.<patch>. Lines
     * are only added: a line kept from an earlier stub of the same version stays, merged with the
     * stub's one into the strictest flags.
     */
    private fun updateBaseline(dir: File, version: String) {
        val libVersion = property("tadami.extensionsLibVersion")
        require(version == majorMinor(libVersion)) {
            "-Ptadami.updateApiBaseline=$version, but extensions-lib is $libVersion: only ${majorMinor(libVersion)} can be written"
        }
        val file = File(dir, "$version.txt")
        val lines = sortedMapOf<String, String>()
        val existing = if (file.exists()) readBaselineLines(file) else emptyList()
        (existing + stub.flatMap { it.lines() }).forEach { line ->
            lines.merge(keyOf(line), line) { old, new -> mergeLines(old, new) }
        }
        file.writeText(
            "# extensions-lib $version: the classes and members extensions built against it may link against.\n" +
                "# The app must keep them (ExtensionsApiCompatibilityTest). Written with -Ptadami.updateApiBaseline=$version,\n" +
                "# which only adds lines: list a removed member in $KNOWN_BREAKS instead of deleting it here.\n" +
                lines.values.sorted().joinToString("\n", postfix = "\n")
        )
    }

    /** The strictest of two lines of a same key: what both lib versions let extensions do */
    private fun mergeLines(old: String, new: String): String {
        if (old == new) return old
        val key = keyOf(old)
        val flagsStart = key.split(' ').size
        val oldFlags = flags(old.split(' ').drop(flagsStart))
        val newFlags = flags(new.split(' ').drop(flagsStart))
        require((oldFlags xor newFlags) and (ACC_STATIC or KIND_FLAGS) == 0) {
            "\"$key\" changed from \"$old\" to \"$new\": edit the baseline by hand"
        }
        val visibility = if ((oldFlags or newFlags) and ACC_PUBLIC != 0) ACC_PUBLIC else ACC_PROTECTED
        val kept = (oldFlags and newFlags and (ACC_FINAL or ACC_ABSTRACT)) or (oldFlags and (ACC_STATIC or KIND_FLAGS))
        return "$key ${flagNames(visibility or kept)}"
    }

    private fun flagNames(access: Int) = FLAG_NAMES.filter { (flag, _) -> access and flag != 0 }.joinToString(" ") { it.second }

    private fun flags(names: List<String>) = names.fold(0) { access, name ->
        access or (FLAG_NAMES.firstOrNull { it.second == name }?.first ?: error("Unknown flag $name"))
    }

    // Classes

    private fun ClassNode.toApiClass() = ApiClass(
        name = name,
        access = classAccess() and CLASS_FLAGS,
        superclass = superName?.takeUnless { it == "java/lang/Object" },
        interfaces = interfaces.toList(),
        members = methods.filter { it.isApi() }.map { ApiMember(true, it.name, it.desc, it.access and MEMBER_FLAGS) } +
            fields.filter { it.isApi() }.map { ApiMember(false, it.name, it.desc, it.access and MEMBER_FLAGS, it.value) },
    )

    /** A public or protected class, not synthetic (serializers aside), anonymous, local nor in a hidden class */
    private fun isApiClass(node: ClassNode): Boolean {
        val inner = node.innerClasses.firstOrNull { it.name == node.name }
        return when {
            inner != null && (inner.innerName == null || inner.outerName == null) -> false
            node.access and ACC_SYNTHETIC != 0 && !node.name.endsWith("\$\$serializer") -> false
            node.name.endsWith("\$WhenMappings") -> false
            !isVisible(node.classAccess()) -> false
            inner == null -> true
            else -> stubNodes[inner.outerName]?.let { isApiClass(it) } ?: false
        }
    }

    /**
     * Public and protected members, with the synthetic default arguments variants extensions call,
     * but not the other synthetic ones nor the module-mangled internal ones (names with a $)
     */
    private fun MethodNode.isApi() = when {
        !isVisible(access) || access and ACC_BRIDGE != 0 -> false
        name.endsWith("\$default") -> true
        name == "<init>" -> access and ACC_SYNTHETIC == 0 || desc.endsWith(DEFAULT_ARGUMENTS_CONSTRUCTOR)
        else -> access and ACC_SYNTHETIC == 0 && '$' !in name
    }

    private fun FieldNode.isApi() = isVisible(access) && access and ACC_SYNTHETIC == 0 && '$' !in name

    /** The access flags of a class, those of its InnerClasses entry for a nested one */
    private fun ClassNode.classAccess() = innerClasses.firstOrNull { it.name == name }?.access ?: access

    private fun isVisible(access: Int) = access and (ACC_PUBLIC or ACC_PROTECTED) != 0

    private fun isVisibilityKept(stubAccess: Int, appAccess: Int) =
        if (stubAccess and ACC_PUBLIC != 0) appAccess and ACC_PUBLIC != 0 else isVisible(appAccess)

    private fun visibilityName(access: Int) = if (access and ACC_PUBLIC != 0) "public" else "protected"

    private fun kindName(access: Int) = when {
        access and ACC_ANNOTATION != 0 -> "an annotation"
        access and ACC_INTERFACE != 0 -> "an interface"
        access and ACC_ENUM != 0 -> "an enum"
        else -> "a class"
    }

    private fun majorMinor(version: String) = version.split('.').take(2).joinToString(".")

    private fun property(name: String) =
        System.getProperty(name) ?: error("System property $name is unset: run this test with Gradle")

    companion object {
        private const val KNOWN_BREAKS = "known-breaks.txt"
        private const val SOURCE_PACKAGE = "com/sf/tadami/source/"
        private const val SOURCE = "com/sf/tadami/source/Source"
        private val RX_FETCHES = setOf(
            "fetchSearch", "fetchLatest", "fetchAnimeDetails", "fetchEpisodesList", "fetchEpisode", "fetchEpisodeSources",
        )
        private val BASELINE_NAME = Regex("""\d+\.\d+\.txt""")
        private const val DEFAULT_ARGUMENTS_CONSTRUCTOR = "ILkotlin/jvm/internal/DefaultConstructorMarker;)V"

        private val FLAG_NAMES = listOf(
            ACC_PUBLIC to "public",
            ACC_PROTECTED to "protected",
            ACC_STATIC to "static",
            ACC_FINAL to "final",
            ACC_ABSTRACT to "abstract",
            ACC_INTERFACE to "interface",
            ACC_ENUM to "enum",
            ACC_ANNOTATION to "annotation",
        )
        private const val KIND_FLAGS = ACC_INTERFACE or ACC_ENUM or ACC_ANNOTATION
        private const val CLASS_FLAGS = ACC_PUBLIC or ACC_PROTECTED or ACC_FINAL or ACC_ABSTRACT or KIND_FLAGS
        private const val MEMBER_FLAGS = ACC_PUBLIC or ACC_PROTECTED or ACC_STATIC or ACC_FINAL or ACC_ABSTRACT

        /** The stub's classes, by internal name */
        private val stubNodes: Map<String, ClassNode> by lazy {
            val dirs = System.getProperty("tadami.extensionsApiClasses")
                ?: error("System property tadami.extensionsApiClasses is unset: run this test with Gradle")
            dirs.split(File.pathSeparator).filter { it.isNotEmpty() }
                .flatMap { dir -> File(dir).walk().filter { it.isFile && it.name.endsWith(".class") }.toList() }
                .map { readClass(it.readBytes()) }
                .associateBy { it.name }
                .also { check(it.isNotEmpty()) { "No class in $dirs" } }
        }

        private val appClasses = HashMap<String, ClassNode?>()

        /** An app (or library) class from the test classpath, by internal name */
        private fun appClass(name: String): ClassNode? = appClasses.getOrPut(name) {
            ExtensionsApiCompatibilityTest::class.java.classLoader!!
                .getResourceAsStream("$name.class")
                ?.use { readClass(it.readBytes()) }
        }

        private fun readClass(bytes: ByteArray) = ClassNode().also {
            ClassReader(bytes).accept(it, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
    }
}

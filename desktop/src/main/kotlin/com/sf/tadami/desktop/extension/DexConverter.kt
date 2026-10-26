package com.sf.tadami.desktop.extension

import com.googlecode.d2j.Method
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.dex.DexExceptionHandler
import com.googlecode.d2j.node.DexMethodNode
import com.googlecode.d2j.reader.MultiDexFileReader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Turns the dex of an extension APK into a JVM jar. Runs in the app's process: computing the stack
 * map frames looks up the classes the extension uses (OkHttp, Jsoup, the extension API...).
 */
object DexConverter {

    // dex2jar 2.4.38 names a common super class it can't find java/util/Object (fixed after that release)
    private const val BOGUS_OBJECT = "java/util/Object"

    @Throws(IOException::class)
    fun convert(apk: File, jar: File) {
        val errors = mutableListOf<String>()
        val handler = object : DexExceptionHandler {
            override fun handleFileException(e: Exception) {
                errors += e.toString()
            }

            override fun handleMethodTranslateException(method: Method, methodNode: DexMethodNode, mv: MethodVisitor, e: Exception) {
                errors += "$method: $e"
            }
        }
        val raw = File(jar.parentFile, jar.name + ".raw")
        raw.delete()
        Dex2jar.from(MultiDexFileReader.open(apk.readBytes()))
            .withExceptionHandler(handler)
            .reUseReg(false)
            .topoLogicalSort()
            .skipDebug(false)
            .optimizeSynchronized(false)
            .printIR(false)
            .noCode(false)
            .skipExceptions(false)
            .dontSanitizeNames(true)
            .computeFrames(true)
            .to(raw.toPath())
        if (errors.isNotEmpty()) {
            raw.delete()
            throw IOException("dex2jar failed on ${errors.size} methods: ${errors.take(3).joinToString()}")
        }
        fixClasses(raw, jar)
        raw.delete()
    }

    /**
     * Rewrites what dex2jar gets wrong in [input] into [output]: the bogus common super class, and
     * calls to a method of an interface outside the dex (kotlinx.serialization's GeneratedSerializer
     * for one) emitted as calls to a class method, which the JVM refuses once they run.
     */
    private fun fixClasses(input: File, output: File) {
        val remapper = SimpleRemapper(BOGUS_OBJECT, "java/lang/Object")
        ZipFile(input).use { zip ->
            // The interfaces among the classes of the extension itself
            val ownInterfaces = mutableSetOf<String>()
            val ownClasses = mutableSetOf<String>()
            for (entry in zip.entries()) {
                if (!entry.name.endsWith(".class")) continue
                val reader = ClassReader(zip.getInputStream(entry).use { it.readBytes() })
                ownClasses += reader.className
                if (reader.access and Opcodes.ACC_INTERFACE != 0) ownInterfaces += reader.className
            }
            val hostInterfaces = HashMap<String, Boolean>()
            fun isInterface(owner: String): Boolean? = when {
                owner.startsWith("[") -> false
                owner in ownClasses -> owner in ownInterfaces
                else -> hostInterfaces.getOrPut(owner) {
                    runCatching { Class.forName(owner.replace('/', '.'), false, javaClass.classLoader).isInterface }
                        .getOrNull() ?: return null
                }
            }
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    var bytes = zip.getInputStream(entry).use { it.readBytes() }
                    if (entry.name.endsWith(".class")) {
                        val writer = ClassWriter(0)
                        val fixer = object : ClassVisitor(Opcodes.ASM9, ClassRemapper(writer, remapper)) {
                            override fun visitMethod(
                                access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?,
                            ): MethodVisitor = object : MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                                    val itf = isInterface(owner) ?: isInterface
                                    val fixedOpcode = when {
                                        opcode == Opcodes.INVOKEVIRTUAL && itf -> Opcodes.INVOKEINTERFACE
                                        opcode == Opcodes.INVOKEINTERFACE && !itf -> Opcodes.INVOKEVIRTUAL
                                        else -> opcode
                                    }
                                    super.visitMethodInsn(fixedOpcode, owner, name, descriptor, itf)
                                }
                            }
                        }
                        ClassReader(bytes).accept(fixer, 0)
                        bytes = writer.toByteArray()
                    }
                    out.putNextEntry(ZipEntry(entry.name))
                    out.write(bytes)
                    out.closeEntry()
                }
            }
        }
    }
}

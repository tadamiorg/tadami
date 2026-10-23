package com.hippo.unifile

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * An in-memory [UniFile] for the JVM tests (UniFile's constructor is package-private: hence this package). A
 * folder holds its children by name; a file its bytes. [renameSuffix] is added to a rename's name, as a storage
 * folder does with a name already taken ("<name> (1)"); [failWrites] makes every write throw. A folder's two
 * settings go to the files created in it.
 */
class MemoryUniFile private constructor(
    private var parent: MemoryUniFile?,
    private var fileName: String,
    private val directory: Boolean,
) : UniFile(parent) {
    private val children = linkedMapOf<String, MemoryUniFile>()
    var bytes = ByteArray(0)
    var renameSuffix = ""
    var failWrites = false

    /** The names in this folder. */
    val names: Set<String>
        get() = children.keys.toSet()

    fun file(name: String, content: ByteArray): MemoryUniFile =
        MemoryUniFile(this, name, false).also { it.bytes = content; children[name] = it }

    fun folder(name: String): MemoryUniFile = MemoryUniFile(this, name, true).also { children[name] = it }

    override fun createFile(displayName: String): UniFile? {
        if (!directory || displayName in children) return null
        return file(displayName, ByteArray(0)).also {
            it.failWrites = failWrites
            it.renameSuffix = renameSuffix
        }
    }

    override fun createDirectory(displayName: String): UniFile? = if (directory) children[displayName] ?: folder(displayName) else null
    // No android.net.Uri on the JVM
    override fun getUri(): Uri = throw UnsupportedOperationException()
    override fun getName(): String = fileName
    override fun getType(): String? = null
    override fun getFilePath(): String = (parent?.filePath?.let { "$it/" } ?: "/") + fileName
    override fun isDirectory(): Boolean = directory
    override fun isFile(): Boolean = !directory
    override fun lastModified(): Long = 0L
    override fun length(): Long = bytes.size.toLong()
    override fun canRead(): Boolean = true
    override fun canWrite(): Boolean = true

    override fun delete(): Boolean {
        val from = parent ?: return false
        if (from.children[fileName] !== this) return false
        from.children.remove(fileName)
        parent = null
        return true
    }

    override fun exists(): Boolean = parent?.children?.get(fileName) === this || (parent == null && directory)
    override fun listFiles(): Array<UniFile> = children.values.toTypedArray()
    override fun listFiles(filter: FilenameFilter?): Array<UniFile> =
        children.values.filter { filter == null || it.fileName.let { name -> filter.accept(this, name) } }.toTypedArray()

    override fun findFile(displayName: String): UniFile? = children[displayName]

    override fun renameTo(displayName: String): Boolean {
        val from = parent ?: return false
        val target = displayName + renameSuffix
        if (target in from.children) return false
        from.children.remove(fileName)
        fileName = target
        from.children[target] = this
        return true
    }

    override fun openOutputStream(): OutputStream = openOutputStream(false)

    override fun openOutputStream(append: Boolean): OutputStream {
        if (failWrites) throw IOException("Write refused")
        return object : ByteArrayOutputStream() {
            override fun close() {
                bytes = if (append) bytes + toByteArray() else toByteArray()
            }
        }
    }

    override fun openInputStream(): InputStream = ByteArrayInputStream(bytes)

    override fun createRandomAccessFile(mode: String?): UniRandomAccessFile = throw IOException("Not supported")

    companion object {
        /** A storage folder, the root of its files. */
        fun root(name: String = "root") = MemoryUniFile(null, name, true)
    }
}

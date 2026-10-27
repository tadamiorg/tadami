package com.hippo.unifile

import com.sf.tadami.storage.StorageFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * An in-memory [StorageFile] for the JVM tests. A folder holds its children by name; a file its bytes.
 * [renameSuffix] is added to a rename's name, as a storage folder does with a name already taken
 * ("<name> (1)"); [failWrites] makes every write throw. A folder's two settings go to the files created in it.
 */
class MemoryUniFile private constructor(
    private val parent: MemoryUniFile?,
    private var fileName: String,
    private val directory: Boolean,
) : StorageFile() {
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

    override fun createFile(displayName: String): StorageFile? {
        if (!directory || displayName in children) return null
        return file(displayName, ByteArray(0)).also {
            it.failWrites = failWrites
            it.renameSuffix = renameSuffix
        }
    }

    override fun createDirectory(displayName: String): StorageFile? = if (directory) children[displayName] ?: folder(displayName) else null
    override val uri: String get() = "memory://$filePath"
    override val name: String get() = fileName
    override val type: String? get() = null
    override val filePath: String get() = (parent?.filePath?.let { "$it/" } ?: "/") + fileName
    override val parentFile: StorageFile? get() = parent
    override val isDirectory: Boolean get() = directory
    override val isFile: Boolean get() = !directory
    override fun lastModified(): Long = 0L
    override fun length(): Long = bytes.size.toLong()

    override fun delete(): Boolean {
        val from = parent ?: return false
        if (from.children[fileName] !== this) return false
        from.children.remove(fileName)
        return true
    }

    // A root is there; anything else while its folder holds it, the folder being there too
    override fun exists(): Boolean = parent?.let { it.children[fileName] === this && it.exists() } ?: true
    override fun listFiles(): Array<StorageFile> = children.values.toTypedArray()

    override fun findFile(displayName: String): StorageFile? = children[displayName]

    override fun renameTo(displayName: String): Boolean {
        val from = parent ?: return false
        val target = displayName + renameSuffix
        if (target in from.children) return false
        from.children.remove(fileName)
        fileName = target
        from.children[target] = this
        return true
    }

    override fun openOutputStream(append: Boolean): OutputStream {
        if (failWrites) throw IOException("Write refused")
        return object : ByteArrayOutputStream() {
            override fun close() {
                bytes = if (append) bytes + toByteArray() else toByteArray()
            }
        }
    }

    override fun openOutputAt(position: Long): OutputStream {
        if (failWrites) throw IOException("Write refused")
        val kept = bytes.copyOf(position.toInt().coerceAtMost(bytes.size))
        return object : ByteArrayOutputStream() {
            override fun close() {
                bytes = kept + toByteArray()
            }
        }
    }

    override fun openInputStream(): InputStream = ByteArrayInputStream(bytes)

    // Each instance is its own file
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    companion object {
        /** A storage folder, the root of its files. */
        fun root(name: String = "root") = MemoryUniFile(null, name, true)
    }
}

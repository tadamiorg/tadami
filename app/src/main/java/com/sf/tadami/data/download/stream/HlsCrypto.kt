package com.sf.tadami.data.download.stream

import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** EXT-X-KEY METHOD=AES-128: whole segments encrypted with AES-128-CBC and PKCS7 padding. */
object HlsCrypto {

    private const val BLOCK_SIZE = 16

    /**
     * The 16 bytes IV of a segment: its tag's IV (hex, with or without 0x), else its media sequence
     * number, big-endian. Media3's parser already fills in the sequence number as hex when the tag
     * has no IV, and [com.sf.tadami.network.player.HlsSegmentIvs] the file numbers of Kwik.
     */
    fun iv(hexIv: String?, mediaSequence: Long): ByteArray {
        if (hexIv == null) return ivOf(BigInteger.valueOf(mediaSequence))
        val digits = hexIv.trim().removePrefix("0x").removePrefix("0X")
        return ivOf(BigInteger(digits, 16))
    }

    private fun ivOf(value: BigInteger): ByteArray {
        // Without the sign byte BigInteger may add, left padded with zeros
        val bytes = value.toByteArray().takeLast(BLOCK_SIZE).toByteArray()
        return ByteArray(BLOCK_SIZE - bytes.size) + bytes
    }

    fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(key.size == BLOCK_SIZE) { "AES-128 key of ${key.size} bytes" }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }
}

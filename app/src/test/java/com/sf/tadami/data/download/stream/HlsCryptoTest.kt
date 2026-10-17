package com.sf.tadami.data.download.stream

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class HlsCryptoTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    @Test
    fun theTagIvIsUsedAsIs() {
        assertEquals(
            "000102030405060708090a0b0c0d0e0f",
            hex(HlsCrypto.iv("0x000102030405060708090A0B0C0D0E0F", mediaSequence = 99)),
        )
        // Without the prefix, and shorter: left padded
        assertEquals("000000000000000000000000000000ff", hex(HlsCrypto.iv("ff", mediaSequence = 99)))
        // The high bit set: no sign byte
        assertEquals("ffffffffffffffffffffffffffffffff", hex(HlsCrypto.iv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 0)))
    }

    @Test
    fun withoutATagIvTheMediaSequenceNumberIs() {
        assertEquals("00000000000000000000000000000000", hex(HlsCrypto.iv(null, mediaSequence = 0)))
        assertEquals("0000000000000000000000000000012c", hex(HlsCrypto.iv(null, mediaSequence = 300)))
        // As Media3's parser writes it: the sequence number in hex
        assertArrayEquals(HlsCrypto.iv(null, 300), HlsCrypto.iv(java.lang.Long.toHexString(300), 0))
    }

    @Test
    fun segmentsAreDecrypted() {
        val key = ByteArray(16) { it.toByte() }
        val iv = HlsCrypto.iv(null, mediaSequence = 7)
        val segment = ByteArray(1000) { (it % 251).toByte() }
        val encrypted = Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            doFinal(segment)
        }

        assertArrayEquals(segment, HlsCrypto.decrypt(encrypted, key, iv))
    }
}

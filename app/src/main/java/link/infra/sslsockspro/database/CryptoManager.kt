package link.infra.sslsockspro.database

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.nio.ByteBuffer

//@RequiresApi(Build.VERSION_CODES.M)
class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getDecryptedCipherForIV(iv: ByteArray): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
        }
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry("secret", null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator.getInstance(ALGORITHM).apply {
            init(
                KeyGenParameterSpec.Builder(
                    "secret",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    fun encrypt(bytes: ByteArray, outputStream: OutputStream): ByteArray {
        val encryptCipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }

        val encryptedBytes = encryptCipher.doFinal(bytes)
        val dbuf = ByteBuffer.allocate(4);
        dbuf.putInt(encryptedBytes.size)
        val size = dbuf.array()
        outputStream.use {
            it.write(encryptCipher.iv.size)
            it.write(encryptCipher.iv)
            it.write(size)
            //it.write(encryptedBytes.size)
            it.write(encryptedBytes)
        }
        return encryptedBytes
    }

    fun decrypt(inputStream: InputStream): ByteArray {
        return inputStream.use {
            val ivSize = it.read()
            val iv = ByteArray(ivSize)
            it.read(iv)

            val size = ByteArray(4)
            //val encryptedByteSize = it.read(size)
            it.read(size) // read 4 bytes
            val dbuf = ByteBuffer.wrap(size); // big-endian by default
            val encryptedByteSize = dbuf.int;
            val encryptedBytes = ByteArray(encryptedByteSize)
            it.read(encryptedBytes)

            getDecryptedCipherForIV(iv).doFinal(encryptedBytes)
        }
    }

    companion object {
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

        private const val key = "C3oZ4kL+AXfuMT2CtJg6CJlKRdWZY5xcm29tis9gwj0="

        fun decryptServerProfile(encrypted: ByteArray): ByteArray {
            return decryptServerProfile(key, encrypted)
        }

        fun decodeBase64(data: String): ByteArray {
            return Base64.decode(data, Base64.DEFAULT)
        }

        fun decryptServerProfile(key: String, encrypted: ByteArray): ByteArray {
            val raw = decodeBase64(key)
            val sKeySpec = SecretKeySpec(raw, "AES")
            val cipher = Cipher.getInstance("AES").apply {
                init(Cipher.DECRYPT_MODE, sKeySpec)
            }
            return cipher.doFinal(encrypted)
        }

    }
}
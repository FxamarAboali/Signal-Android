/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.backup.v2.stream

import org.signal.core.util.readFully
import org.signal.core.util.readNBytesOrThrow
import org.signal.core.util.readVarInt32
import org.signal.core.util.stream.MacInputStream
import org.signal.core.util.stream.TruncatingInputStream
import org.thoughtcrime.securesms.backup.v2.proto.Frame
import org.whispersystems.signalservice.api.backup.BackupKey
import org.whispersystems.signalservice.api.push.ServiceId.ACI
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides the ability to read backup frames in a streaming fashion from a target [InputStream].
 * As it's being read, it will be both decrypted and uncompressed. Specifically, the data is decrypted,
 * that decrypted data is gunzipped, then that data is read as frames.
 */
class EncryptedBackupReader(
  key: BackupKey,
  aci: ACI,
  streamLength: Long,
  dataStream: () -> InputStream
) : Iterator<Frame>, AutoCloseable {

  var next: Frame? = null
  val stream: InputStream

  init {
    val keyMaterial = key.deriveSecrets(aci)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
      init(Cipher.DECRYPT_MODE, SecretKeySpec(keyMaterial.cipherKey, "AES"), IvParameterSpec(keyMaterial.iv))
    }

    validateMac(keyMaterial.macKey, streamLength, dataStream())

    stream = GZIPInputStream(
      CipherInputStream(
        TruncatingInputStream(
          wrapped = dataStream(),
          maxBytes = streamLength - MAC_SIZE
        ),
        cipher
      )
    )

    next = read()
  }

  override fun hasNext(): Boolean {
    return next != null
  }

  override fun next(): Frame {
    next?.let { out ->
      next = read()
      return out
    } ?: throw NoSuchElementException()
  }

  private fun read(): Frame? {
    try {
      val length = stream.readVarInt32().also { if (it < 0) return null }
      val frameBytes: ByteArray = stream.readNBytesOrThrow(length)

      return Frame.ADAPTER.decode(frameBytes)
    } catch (e: EOFException) {
      return null
    }
  }

  override fun close() {
    stream.close()
  }

  companion object {
    const val MAC_SIZE = 32

    fun validateMac(macKey: ByteArray, streamLength: Long, dataStream: InputStream) {
      val mac = Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(macKey, "HmacSHA256"))
      }

      val macStream = MacInputStream(
        wrapped = TruncatingInputStream(dataStream, maxBytes = streamLength - MAC_SIZE),
        mac = mac
      )

      macStream.readFully(false)

      val calculatedMac = macStream.mac.doFinal()
      val expectedMac = dataStream.readNBytesOrThrow(MAC_SIZE)

      if (!calculatedMac.contentEquals(expectedMac)) {
        throw IOException("Invalid MAC!")
      }
    }
  }
}

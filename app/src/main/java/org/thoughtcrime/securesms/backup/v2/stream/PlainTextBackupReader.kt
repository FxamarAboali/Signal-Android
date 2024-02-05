/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.backup.v2.stream

import org.signal.core.util.readNBytesOrThrow
import org.signal.core.util.readVarInt32
import org.thoughtcrime.securesms.backup.v2.proto.Frame
import java.io.EOFException
import java.io.InputStream

/**
 * Reads a plaintext backup import stream one frame at a time.
 */
class PlainTextBackupReader(val inputStream: InputStream) : Iterator<Frame> {

  var next: Frame? = null

  init {
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
      val length = inputStream.readVarInt32().also { if (it < 0) return null }
      val frameBytes: ByteArray = inputStream.readNBytesOrThrow(length)

      return Frame.ADAPTER.decode(frameBytes)
    } catch (e: EOFException) {
      return null
    }
  }
}

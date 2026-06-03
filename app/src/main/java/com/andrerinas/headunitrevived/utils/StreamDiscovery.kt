package com.andrerinas.headunitrevived.utils

import android.content.Context
import android.media.AudioManager
import android.os.Build

object StreamDiscovery {
    data class StreamInfo(val id: Int, val label: String)

    private val STANDARD_STREAMS = mapOf(
        0 to Pair("STREAM_VOICE_CALL", 1),
        1 to Pair("STREAM_SYSTEM", 1),
        2 to Pair("STREAM_RING", 1),
        3 to Pair("STREAM_MUSIC", 1),
        4 to Pair("STREAM_ALARM", 1),
        5 to Pair("STREAM_NOTIFICATION", 1),
        6 to Pair("STREAM_BLUETOOTH_SCO", 1),
        7 to Pair("STREAM_SYSTEM_ENFORCED", 1),
        8 to Pair("STREAM_DTMF", 1),
        9 to Pair("STREAM_TTS", 1),
        10 to Pair("STREAM_ACCESSIBILITY", 26)
    )

    fun discover(context: Context): List<StreamInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val upperBound = getNumStreamTypes()
        val cap = if (upperBound != null && upperBound > 0) upperBound else 20

        val result = mutableListOf<StreamInfo>()
        var consecutiveMisses = 0

        for (id in 0 until cap) {
            if (streamExists(audioManager, id)) {
                consecutiveMisses = 0
                result.add(StreamInfo(id, resolveStreamName(id)))
            } else {
                if (upperBound == null) {
                    consecutiveMisses++
                    if (consecutiveMisses >= 5) break
                }
            }
        }

        return result
    }

    private fun streamExists(audioManager: AudioManager, streamId: Int): Boolean {
        return try {
            audioManager.getStreamMaxVolume(streamId) >= 0
        } catch (e: Exception) {
            false
        }
    }

    private fun getNumStreamTypes(): Int? {
        return try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            val method = audioSystemClass.getMethod("getNumStreamTypes")
            val count = method.invoke(null) as? Int
            if (count != null && count > 0) count else null
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveStreamName(streamId: Int): String {
        val reflectedName = try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            val method = audioSystemClass.getMethod("getStreamName", Int::class.javaPrimitiveType)
            method.invoke(null, streamId) as? String
        } catch (e: Exception) {
            null
        }
        if (!reflectedName.isNullOrEmpty()) return reflectedName

        val entry = STANDARD_STREAMS[streamId]
        if (entry != null && Build.VERSION.SDK_INT >= entry.second) {
            return entry.first
        }

        return "STREAM_$streamId"
    }
}

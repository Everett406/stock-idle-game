package com.stockidle.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.stockidle.game.R

/**
 * 音效管理：SoundPool + 预生成的 res/raw 短音效。
 * 所有音效都是程序合成的短 WAV，体积小、无版权问题。
 */
class SoundManager(private val context: Context) {

    enum class Sfx { TRADE, COIN, NEWS, UNLOCK, PRESTIGE, ERROR, CLICK }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<Sfx, Int> = mapOf(
        Sfx.TRADE to soundPool.load(context, R.raw.sfx_trade, 1),
        Sfx.COIN to soundPool.load(context, R.raw.sfx_coin, 1),
        Sfx.NEWS to soundPool.load(context, R.raw.sfx_news, 1),
        Sfx.UNLOCK to soundPool.load(context, R.raw.sfx_unlock, 1),
        Sfx.PRESTIGE to soundPool.load(context, R.raw.sfx_prestige, 1),
        Sfx.ERROR to soundPool.load(context, R.raw.sfx_error, 1),
        Sfx.CLICK to soundPool.load(context, R.raw.sfx_click, 1),
    )

    fun play(sfx: Sfx, volume: Float = 0.9f) {
        val id = ids[sfx] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() { soundPool.release() }
}

#!/usr/bin/env python3
"""
生成游戏全部音效（44.1kHz 16bit 单声道 WAV），输出到 app/src/main/res/raw/。
全部为程序合成，无版权问题：
  sfx_trade    成交声：双音上扬
  sfx_coin     金币声：金属琶音
  sfx_news     新闻提示：双哔
  sfx_unlock   解锁：上行琶音+泛音
  sfx_prestige 转生：恢弘和弦渐强+闪光
  sfx_error    错误：低频蜂鸣
  sfx_click    点击：短促嗒声
"""
import math
import os
import random
import struct
import wave

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def env(t, dur, attack=0.005, release=0.05):
    """简单包络：快攻 + 指数衰减"""
    if t < attack:
        return t / attack
    rel_start = dur - release
    if t > rel_start:
        return max(0.0, (dur - t) / release)
    return math.exp(-3.0 * (t - attack) / max(dur * 0.6, 1e-6))


def synth(name, dur, gen, gain=0.8):
    n = int(SR * dur)
    frames = bytearray()
    rnd = random.Random(42)
    for i in range(n):
        t = i / SR
        v = gen(t, dur, rnd)
        v = max(-1.0, min(1.0, v * gain))
        frames += struct.pack("<h", int(v * 32767))
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(frames))
    print("生成", path, f"{dur:.2f}s")


def tone(freq, t, wave_type="sine"):
    ph = 2 * math.pi * freq * t
    if wave_type == "sine":
        return math.sin(ph)
    if wave_type == "square":
        return 0.5 * math.copysign(1.0, math.sin(ph))
    if wave_type == "tri":
        return 2 / math.pi * math.asin(math.sin(ph))
    return math.sin(ph)


# ---------- 成交声：660->990Hz 双音 ----------
def gen_trade(t, dur, rnd):
    f = 660.0 if t < dur / 2 else 990.0
    return tone(f, t) * env(t, dur, 0.004, 0.06) * 0.9 + tone(f * 2, t) * env(t, dur) * 0.15


# ---------- 金币声：1318/1568/2093 金属琶音 ----------
def gen_coin(t, dur, rnd):
    notes = [(0.0, 1318.5), (0.07, 1568.0), (0.14, 2093.0)]
    v = 0.0
    for start, f in notes:
        if t >= start:
            lt = t - start
            v += (tone(f, lt) * 0.6 + tone(f * 1.5, lt) * 0.25) * env(lt, 0.32, 0.003, 0.12)
    return v * 0.8


# ---------- 新闻提示：880Hz 双哔 ----------
def gen_news(t, dur, rnd):
    v = 0.0
    for start in (0.0, 0.14):
        if t >= start:
            lt = t - start
            v += tone(880.0, lt, "square") * env(lt, 0.10, 0.004, 0.03) * 0.5
    return v


# ---------- 解锁：C5-E5-G5-C6 上行琶音 + 闪光泛音 ----------
def gen_unlock(t, dur, rnd):
    notes = [(0.0, 523.25), (0.09, 659.25), (0.18, 783.99), (0.27, 1046.5)]
    v = 0.0
    for start, f in notes:
        if t >= start:
            lt = t - start
            v += (tone(f, lt) + tone(f * 2, lt) * 0.3) * env(lt, 0.55, 0.004, 0.25)
    if t > 0.27:
        v += tone(2093.0, t) * env(t - 0.27, 0.6, 0.01, 0.5) * 0.12
    return v * 0.75


# ---------- 转生：C-E-G-C 大和弦渐强 + 低频鼓 + 星光 ----------
def gen_prestige(t, dur, rnd):
    v = 0.0
    chord = [130.81, 164.81, 196.0, 261.63, 329.63, 392.0, 523.25]
    swell = min(1.0, t / 0.9)
    for i, f in enumerate(chord):
        v += tone(f, t) * swell * (0.16 - i * 0.015) * env(t, dur, 0.02, 1.2)
    # 低频鼓
    if t < 0.5:
        v += math.sin(2 * math.pi * (90 - 60 * t / 0.5) * t) * (1 - t / 0.5) * 0.5
    # 星光闪烁
    if t > 1.2:
        for k, start in enumerate((1.2, 1.35, 1.5, 1.65, 1.8)):
            if t >= start:
                lt = t - start
                v += tone(1046.5 * (2 ** (k % 3) / 2), lt) * env(lt, 0.3, 0.005, 0.15) * 0.1
    return v * 0.85


# ---------- 错误：220Hz 锯齿蜂鸣 ----------
def gen_error(t, dur, rnd):
    ph = (t * 220.0) % 1.0
    saw = 2.0 * ph - 1.0
    return saw * 0.35 * env(t, dur, 0.005, 0.05)


# ---------- 点击：短促嗒 ----------
def gen_click(t, dur, rnd):
    v = tone(1250.0, t) * env(t, 0.045, 0.001, 0.03) * 0.7
    v += (rnd.random() * 2 - 1) * env(t, 0.02, 0.001, 0.015) * 0.15
    return v


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    synth("sfx_trade.wav", 0.22, gen_trade)
    synth("sfx_coin.wav", 0.55, gen_coin)
    synth("sfx_news.wav", 0.30, gen_news, gain=0.5)
    synth("sfx_unlock.wav", 0.95, gen_unlock)
    synth("sfx_prestige.wav", 2.4, gen_prestige)
    synth("sfx_error.wav", 0.18, gen_error, gain=0.5)
    synth("sfx_click.wav", 0.05, gen_click, gain=0.5)
    print("全部音效生成完成")

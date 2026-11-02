package ir.hoboho.shooting.core.common

/** Injectable clock so repositories and tests never call System directly. */
interface Clock {
    fun nowEpochMilli(): Long
}

object SystemClock : Clock {
    override fun nowEpochMilli(): Long = System.currentTimeMillis()
}

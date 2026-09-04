package com.producto.timer

/** Records invocations instead of playing a real sound/haptic, for use in unit tests. */
class FakeSessionNotifier : SessionNotifier {
    var notifyCount = 0
        private set

    override fun notifySessionComplete() {
        notifyCount++
    }
}

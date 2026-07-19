package com.sf.tadami.ui.animeinfos.episode.cast

/**
 * Phone↔TV cast compatibility contract. This is a monotonic protocol version, independent of the
 * marketing versionName, bumped only when the wire contract between the sender (this phone app) and
 * the Tadami Terebi receiver changes in a way an older receiver can't honor (new customData keys,
 * new control-channel messages, etc.).
 *
 * The phone advertises [MIN_RECEIVER_VERSION] in the cast load customData; the receiver compares it
 * against the version it implements and forces the user to update the TV app when it is too old.
 */
object CastProtocol {
    /** Protocol version this sender speaks. */
    const val SENDER_VERSION = 1

    /** Oldest receiver protocol this sender can drive correctly. Receivers below this must update. */
    const val MIN_RECEIVER_VERSION = 2
}

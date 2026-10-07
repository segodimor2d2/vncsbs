package com.rec.vncsbs

internal fun shouldActivateLeader(
    isAtKey: Boolean,
    isAtCharacter: Boolean,
    shiftPressed: Boolean,
    eventTime: Long,
    shiftPressedAt: Long?
): Boolean = shiftPressed && (
    isAtKey || (isAtCharacter && shiftPressedAt != null && shiftPressedAt < eventTime)
)

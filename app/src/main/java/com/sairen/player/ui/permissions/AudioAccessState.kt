package com.sairen.player.ui.permissions

/** Where the audio-permission flow currently stands. */
enum class AudioAccessState {
    GRANTED,

    /** The system will still show the dialog (possibly after our rationale). */
    RATIONALE,

    /** The user denied twice (or a policy blocked it); only Settings can help now. */
    PERMANENTLY_DENIED,
}

/**
 * Pure permission-flow decisions.
 *
 * Keeping this out of the Composable (and out of Android APIs) means the two
 * subtle cases — "denied but still askable" and "denied for good" — are unit
 * tested instead of being discovered on a device.
 */
object PermissionDecision {

    /** State before any request in this session. */
    fun initial(hasPermission: Boolean): AudioAccessState =
        if (hasPermission) AudioAccessState.GRANTED else AudioAccessState.RATIONALE

    /**
     * @param hasPermission whether the permission is granted *after* the dialog.
     * @param shouldShowRationale `shouldShowRequestPermissionRationale` result;
     *   `false` right after a denial means the system will not ask again.
     */
    fun afterRequestResult(hasPermission: Boolean, shouldShowRationale: Boolean): AudioAccessState =
        when {
            hasPermission -> AudioAccessState.GRANTED
            shouldShowRationale -> AudioAccessState.RATIONALE
            else -> AudioAccessState.PERMANENTLY_DENIED
        }

    /** Re-evaluation after returning from system settings or another activity. */
    fun afterResume(hasPermission: Boolean, current: AudioAccessState): AudioAccessState =
        if (hasPermission) AudioAccessState.GRANTED else current

    /** Whether the media notification rationale dialog should be shown. */
    fun shouldPromptForNotifications(
        requiresRuntimePermission: Boolean,
        hasPermission: Boolean,
        alreadyPromptedThisSession: Boolean,
    ): Boolean = requiresRuntimePermission && !hasPermission && !alreadyPromptedThisSession
}

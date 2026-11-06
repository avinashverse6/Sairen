package com.sairen.player.ui.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionDecisionTest {

    @Test
    fun `granted permission starts in the granted state`() {
        assertEquals(AudioAccessState.GRANTED, PermissionDecision.initial(hasPermission = true))
    }

    @Test
    fun `missing permission starts at the rationale screen, never at the system dialog`() {
        assertEquals(AudioAccessState.RATIONALE, PermissionDecision.initial(hasPermission = false))
    }

    @Test
    fun `granting in the system dialog yields granted`() {
        assertEquals(
            AudioAccessState.GRANTED,
            PermissionDecision.afterRequestResult(hasPermission = true, shouldShowRationale = false),
        )
    }

    @Test
    fun `first denial keeps the rationale path available`() {
        assertEquals(
            AudioAccessState.RATIONALE,
            PermissionDecision.afterRequestResult(hasPermission = false, shouldShowRationale = true),
        )
    }

    @Test
    fun `second denial switches to the open-settings path`() {
        assertEquals(
            AudioAccessState.PERMANENTLY_DENIED,
            PermissionDecision.afterRequestResult(hasPermission = false, shouldShowRationale = false),
        )
    }

    @Test
    fun `returning from settings upgrades to granted`() {
        assertEquals(
            AudioAccessState.GRANTED,
            PermissionDecision.afterResume(
                hasPermission = true,
                current = AudioAccessState.PERMANENTLY_DENIED,
            ),
        )
    }

    @Test
    fun `returning from settings without a grant keeps the current state`() {
        assertEquals(
            AudioAccessState.PERMANENTLY_DENIED,
            PermissionDecision.afterResume(
                hasPermission = false,
                current = AudioAccessState.PERMANENTLY_DENIED,
            ),
        )
    }

    @Test
    fun `notification rationale is only shown once, when it is needed`() {
        assertTrue(
            PermissionDecision.shouldPromptForNotifications(
                requiresRuntimePermission = true,
                hasPermission = false,
                alreadyPromptedThisSession = false,
            ),
        )
        assertFalse(
            PermissionDecision.shouldPromptForNotifications(
                requiresRuntimePermission = true,
                hasPermission = false,
                alreadyPromptedThisSession = true,
            ),
        )
        assertFalse(
            PermissionDecision.shouldPromptForNotifications(
                requiresRuntimePermission = true,
                hasPermission = true,
                alreadyPromptedThisSession = false,
            ),
        )
        // API < 33: no runtime notification permission, so never prompt.
        assertFalse(
            PermissionDecision.shouldPromptForNotifications(
                requiresRuntimePermission = false,
                hasPermission = true,
                alreadyPromptedThisSession = false,
            ),
        )
    }
}

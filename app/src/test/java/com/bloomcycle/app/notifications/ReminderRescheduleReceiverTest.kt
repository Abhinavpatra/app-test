package com.bloomcycle.app.notifications

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRescheduleReceiverTest {

    @Test
    fun `a reboot reschedules the reminder`() {
        assertTrue(ReminderRescheduleReceiver.handles(Intent.ACTION_BOOT_COMPLETED))
    }

    @Test
    fun `a timezone or clock change reschedules the reminder`() {
        assertTrue(ReminderRescheduleReceiver.handles(Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(ReminderRescheduleReceiver.handles(Intent.ACTION_TIME_CHANGED))
    }

    @Test
    fun `anything else is ignored`() {
        assertFalse(ReminderRescheduleReceiver.handles(Intent.ACTION_PACKAGE_ADDED))
        assertFalse(ReminderRescheduleReceiver.handles("com.bloomcycle.app.MADE_UP"))
        assertFalse(ReminderRescheduleReceiver.handles(null))
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
import com.example.hardware.NotificationHelper
import com.example.model.AgeGroup
import com.example.model.DecayLevelMode
import com.example.model.TargetPlatform
import com.example.ui.ScreenDecayViewModel
import com.example.util.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tự Phân Hủy Độ Hấp Dẫn", appName)
    }

    @Test
    fun `verify sha256 pin, age group thresholds, 4 target platforms and 4 decay levels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ScreenDecayRepository(context)
        val hardwareManager = HardwareManager(context)
        val notificationHelper = NotificationHelper(context)
        val viewModel = ScreenDecayViewModel(repository, hardwareManager, notificationHelper)

        // Default PIN "1234" verification
        assertTrue(CryptoUtils.verifyPin("1234", viewModel.config.value.pinSha256))

        // Verify all 4 target platforms are enabled by default
        assertEquals(4, viewModel.sessionState.value.enabledTargets.size)
        assertTrue(viewModel.sessionState.value.enabledTargets.contains(TargetPlatform.YOUTUBE_SHORTS))
        assertTrue(viewModel.sessionState.value.enabledTargets.contains(TargetPlatform.TIKTOK_WEB))
        assertTrue(viewModel.sessionState.value.enabledTargets.contains(TargetPlatform.FACEBOOK_REELS))
        assertTrue(viewModel.sessionState.value.enabledTargets.contains(TargetPlatform.WEB_GAME))

        // Verify 4 decay levels in KHKT Demo Mode
        viewModel.selectDecayLevelMode(DecayLevelMode.LEVEL_0)
        assertEquals(0, viewModel.sessionState.value.currentDecayLevel)
        assertFalse(viewModel.sessionState.value.hideLikeAndViewCounts)

        viewModel.selectDecayLevelMode(DecayLevelMode.LEVEL_2)
        assertEquals(2, viewModel.sessionState.value.currentDecayLevel)
        assertTrue(viewModel.sessionState.value.hideLikeAndViewCounts)

        viewModel.selectDecayLevelMode(DecayLevelMode.LEVEL_3)
        assertEquals(3, viewModel.sessionState.value.currentDecayLevel)
        assertEquals(900L, viewModel.sessionState.value.infiniteScrollDelayMs)

        // Age group 1 (3-6): 15 minutes
        viewModel.setAgeGroup(AgeGroup.TODDLER)
        assertEquals(15, viewModel.sessionState.value.targetMinutes)

        // Age group 2 (7-9): 25 minutes & accelerometer rep counting > 6.5 m/s²
        viewModel.setAgeGroup(AgeGroup.PRIMARY)
        assertEquals(25, viewModel.sessionState.value.targetMinutes)
        viewModel.activateMovementStationDirectly()
        assertTrue(viewModel.sessionState.value.isMovementStationActive)
        viewModel.onAccelerometerChanged(7.2f)
        assertEquals(1, viewModel.stretchReps.value)

        // Age group 3 (10-12): 30 minutes
        viewModel.setAgeGroup(AgeGroup.MIDDLE_SCHOOL)
        assertEquals(30, viewModel.sessionState.value.targetMinutes)
    }
}

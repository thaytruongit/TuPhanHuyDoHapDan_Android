package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
import com.example.model.AgeGroup
import com.example.ui.ScreenDecayViewModel
import com.example.util.CryptoUtils
import org.junit.Assert.assertEquals
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
        assertEquals("Hạ Nhiệt Màn Hình", appName)
    }

    @Test
    fun `verify sha256 pin and age group thresholds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ScreenDecayRepository(context)
        val hardwareManager = HardwareManager(context)
        val viewModel = ScreenDecayViewModel(repository, hardwareManager)

        // Default PIN "1234" verification
        assertTrue(CryptoUtils.verifyPin("1234", viewModel.config.value.pinSha256))

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

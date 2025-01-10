package com.idi.vaccinetracker.e2etest

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.idi.vaccinetracker.common.data.database.ParticipantRoomDatabaseConfig
import com.idi.vaccinetracker.e2etest.helper.awaitAirplaneModeSettings
import com.idi.vaccinetracker.e2etest.helper.context
import com.idi.vaccinetracker.e2etest.helper.goThroughRegistrationFlow
import com.idi.vaccinetracker.setup.SetupFlowActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineRegisterTest {

    @get:Rule
    //the screen to start from
    var activityRule = ActivityScenarioRule(SetupFlowActivity::class.java)

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            //for now we will test offline use with an empty database
            context.deleteDatabase(ParticipantRoomDatabaseConfig.FILE_NAME)
        }
    }

    @Before
    fun setUp() {
        Intents.init()
    }

    @Test
    fun testParticipantRegisterFlowSuccessOffline() = runBlocking {
        activityRule.awaitAirplaneModeSettings(true)
        delay(2000)
        goThroughRegistrationFlow(true)
    }


    @After
    fun teardown() {
        Intents.release()
    }
}

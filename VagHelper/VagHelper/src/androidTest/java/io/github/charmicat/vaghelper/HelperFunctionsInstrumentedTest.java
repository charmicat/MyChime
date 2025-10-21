package io.github.charmicat.vaghelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class HelperFunctionsInstrumentedTest {

    private Context testContext;
    private Resources testResources;

    @Before
    public void setUp() {
        testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("io.github.charmicat.vaghelper.test", testContext.getPackageName());
        testResources = testContext.getResources();
    }

    @Test
    public void isServiceRunning() {
        assertTrue(HelperFunctions.isServiceRunning(testContext, "com.google.android.gms.auth.setup.devicesignals.LockScreenService", true));
    }

    @Test
    public void isIntentAvailable() {
        assertTrue(HelperFunctions.isIntentAvailable(testContext, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA, true));
    }

    @Test
    public void rateApp() {
        Intent intent = HelperFunctions.rateApp(testContext, true);
        assertNotNull(intent);
    }

    @Test
    public void playAudio() {
        assertNotNull(testResources);
        assertTrue(HelperFunctions.playAudio(testContext, R.raw.casiochime));
    }
}
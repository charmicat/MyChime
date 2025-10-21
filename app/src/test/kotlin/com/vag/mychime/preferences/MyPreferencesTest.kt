import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vag.mychime.preferences.MyPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
class MyPreferencesTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var myPreferences: MyPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        myPreferences = MyPreferences()
    }

    @Test
    fun savesPreferenceValueCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        myPreferences.savePreferences(context, "testKey", "testValue")
        assertEquals("testValue", sharedPreferences.getString("testKey", null))
    }

    @Test
    fun restoresPreferenceValueCorrectly() {
        sharedPreferences.edit().putString("testKey", "testValue").apply()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val restoredValue = myPreferences.restorePreferences(context, "testKey")
        assertEquals("testValue", restoredValue)
    }

    @Test
    fun returnsEmptyStringWhenKeyDoesNotExist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val restoredValue = myPreferences.restorePreferences(context, "nonExistentKey")
        assertEquals("", restoredValue)
    }

    @Test
    fun doesNotThrowWhenSharedPreferenceChangedIsCalled() {
        myPreferences.onSharedPreferenceChanged(sharedPreferences, "testKey")
        org.junit.Assert.assertTrue(true)
    }
}

package org.ledor

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.ui.lists.ChangePinsFields
import org.ledor.ui.lists.ChangePinsState
import org.ledor.ui.theme.KalendarTheme
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** B-40: поля окна «Изменить ПИН-коды». */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ru-w400dp-h860dp-xxhdpi")
class ChangePinsTest {
    @get:Rule val rule = createComposeRule()
    private fun s(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    /** Простой код для общины («0770») принимается; Мастер-ПИН остаётся длинным. */
    @Test fun simpleReadPinIsAccepted() {
        val st = ChangePinsState("493817", "61803398", masterStored = true)
        rule.setContent { KalendarTheme { Surface { ChangePinsFields(st, null) } } }
        assertTrue(st.canSave)
        rule.onNodeWithText("493817").performTextReplacement("0770")
        rule.waitForIdle()
        assertEquals("0770", st.read)
        assertTrue(st.canSave)
        assertEquals("61803398", st.current)
        rule.onRoot().captureRoboImage("build/screens/ru/14b_change_pins.png")
    }

    @Test fun rulesForNewCodes() {
        val st = ChangePinsState("0770", "61803398", masterStored = true)
        st.read = "077"; assertFalse(st.canSave)
        st.read = "0770"; st.master = "12345"; assertFalse(st.canSave)
        st.master = "0770"; st.read = "0770"; assertFalse(st.canSave)
        st.master = "27182818"; assertTrue(st.canSave)
    }

    /** Мастер-ПИН на телефоне не сохранён — окно просит текущий; без него сохранить нельзя. */
    @Test fun asksCurrentMasterWhenUnknown() {
        val st = ChangePinsState("493817", "", masterStored = false)
        rule.setContent { KalendarTheme { Surface(Modifier.padding(16.dp)) { ChangePinsFields(st, null) } } }
        rule.onNodeWithText(s(R.string.current_master_pin)).assertExists()
        st.master = "27182818"
        assertFalse(st.canSave)
        st.current = "61803398"
        assertTrue(st.canSave)
    }
}

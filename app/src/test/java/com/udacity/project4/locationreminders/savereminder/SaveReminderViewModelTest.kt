package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    // Provide testing to the SaveReminderView and its live data objects

    // Subject under test
    private lateinit var viewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var dataSource: FakeDataSource

    private lateinit var context: Context

    // Request to execute all background tasks synchronously under the same thread
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Given a fresh ViewModel
        val remindersList = mutableListOf<ReminderDTO>()
        dataSource = FakeDataSource(remindersList)
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        stopKoin()
    }

    // Naming convention: SubjectUnderTest_actionOrInput_resultState

    @Test
    fun addNewReminder_goodData_saveReminder() {
        val reminder = ReminderDataItem(
            "Reminder Title",
            "Reminder Description",
            "Location",
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showToast.value, `is`(context.getString(R.string.reminder_saved)))
    }

    @Test
    fun addNewReminder_emptyDescription_saveReminder() {
        val reminder = ReminderDataItem(
            "Reminder Title",
            "",
            "Location",
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showToast.value, `is`(context.getString(R.string.reminder_saved)))
    }

    @Test
    fun addNewReminder_nullDescription_saveReminder() {
        val reminder = ReminderDataItem(
            "Reminder Title",
            null,
            "Location",
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showToast.value, `is`(context.getString(R.string.reminder_saved)))
    }

    @Test
    fun addNewReminder_nullTitle_errorRequireTitle() {
        val reminder = ReminderDataItem(
            null,
            "Reminder Description",
            "Location",
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))
    }

    @Test
    fun addNewReminder_emptyTitle_errorRequireTitle() {
        val reminder = ReminderDataItem(
            "",
            "Reminder Description",
            "Location",
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))
    }

    @Test
    fun addNewReminder_nullLocation_errorRequireLocation() {
        val reminder = ReminderDataItem(
            "Title",
            "Reminder Description",
            null,
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
    }

    @Test
    fun addNewReminder_emptyLocation_errorRequireLocation() {
        val reminder = ReminderDataItem(
            "Title",
            "Reminder Description",
            "",
            51.4930762,
            -0.1487444
        )
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
    }
}
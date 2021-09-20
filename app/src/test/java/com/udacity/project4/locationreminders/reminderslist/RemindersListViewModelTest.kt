package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // provide testing to the RemindersListViewModel and its live data objects

    // Subject under test
    private lateinit var viewModel: RemindersListViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var dataSource: FakeDataSource

    private lateinit var applicationContext: Application

    private val remindersList = mutableListOf<ReminderDTO>()


    // Request to execute all background tasks synchronously under the same thread
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // this viewmodel uses coroutine so some extra settings are required
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        applicationContext = ApplicationProvider.getApplicationContext()
        // Given a fresh ViewModel
        dataSource = FakeDataSource(remindersList)
        viewModel = RemindersListViewModel(applicationContext, dataSource)
        stopKoin()
    }

    // Naming convention: SubjectUnderTest_actionOrInput_resultState
    @Test
    fun loadReminders_emptylist_NoDataTrue() = mainCoroutineRule.runBlockingTest {
        remindersList.clear()
        viewModel.loadReminders()
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_nonEmptyList_checkLoading() = mainCoroutineRule.runBlockingTest {
        // pause to check the loading state
        mainCoroutineRule.pauseDispatcher()

        viewModel.loadReminders()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_nonEmptyList_NoDataFalse() = mainCoroutineRule.runBlockingTest {
        remindersList.clear()
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444
        )
        remindersList.add(reminder)
        viewModel.loadReminders()
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_nonEmptyList_shouldReturnError() = mainCoroutineRule.runBlockingTest {
        dataSource.setShouldReturnError(true)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444
        )
        remindersList.add(reminder)
        viewModel.loadReminders()
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Data source error"))
    }
}
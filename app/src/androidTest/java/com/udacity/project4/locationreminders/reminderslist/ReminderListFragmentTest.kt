package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var reminderDataSource: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var applicationContext: Application

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun DataBindingIdlingResource.monitorReminderListFragment(fragmentScenario: FragmentScenario<ReminderListFragment>) {
        fragmentScenario.onFragment { fragment -> activity = fragment.requireActivity() }
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        applicationContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    applicationContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    applicationContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(applicationContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        reminderDataSource = get()

        //clear the data to start fresh
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
    }

    // Test the navigation of the fragments.
    @Test
    fun onReminderListFragment_clickFab_NavigateToSaveReminderFragment() {
        // Given - on ReminderListFragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - click on the fab
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - navigate to the SaveReminderFragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }


    //  test the displayed data on the UI.
    @Test
    fun saveRemainder_LaunchFragment_DisplayReminder() = runBlockingTest {
        // GIVEN - save a reminder. (Create and Read test)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444,
            "titleA"
        )

        runBlocking {
            reminderDataSource.saveReminder(reminder)
        }

        // WHEN - Navigate to the fragment
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        dataBindingIdlingResource.monitorReminderListFragment(scenario)

        // THEN - reminder is displayed on the UI
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withText(reminder.title)).check(matches(withText(reminder.title)))
        onView(withText(reminder.description)).check(matches(withText(reminder.description)))
        onView(withText(reminder.location)).check(matches(withText(reminder.location)))
    }

    // testing for the error messages.
    @Test
    fun emptyReminderList_loadReminderListFragment_showsNoDataTextView() {
        // GIVEN - @Before we have already clear everything, we have no additional setup for this test
        // Do nothing.

        // WHEN - Launching the ReminderListFragment
        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)

        // THEN - noDataTextView is shown
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

}
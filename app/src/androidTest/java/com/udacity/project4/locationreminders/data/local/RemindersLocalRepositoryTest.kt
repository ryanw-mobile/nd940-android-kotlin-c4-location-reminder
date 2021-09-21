package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Add testing implementation to the RemindersLocalRepository.kt
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    // Here I just do a set of CRUD tests.

    @Test
    fun saveReminder_GetReminderById_returnsSuccess() = runBlocking {
        // GIVEN - save a reminder. (Create and Read test)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444,
            "titleA"
        )
        repository.saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val result = repository.getReminder(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun updateReminder_GetReminderById_ReturnsSuccess() = runBlocking {
        // GIVEN - save and then update a reminder. (update test)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444,
            "titleA"
        )
        repository.saveReminder(reminder)

        val reminderB = ReminderDTO(
            "Reminder Title B",
            "Reminder Description B",
            "Location B",
            51.3930762,
            -0.2487444,
            "titleA"
        )
        repository.saveReminder(reminderB)

        // WHEN - Get the reminder by id from the database.
        val result = repository.getReminder(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.id, `is`(reminderB.id))
        assertThat(result.data.title, `is`(reminderB.title))
        assertThat(result.data.description, `is`(reminderB.description))
        assertThat(result.data.location, `is`(reminderB.location))
        assertThat(result.data.latitude, `is`(reminderB.latitude))
        assertThat(result.data.longitude, `is`(reminderB.longitude))
    }

    @Test
    fun deleteAllReminder_GetReminder_ReturnsError() = runBlocking {
        // GIVEN - delete all reminder. (Delete test)
        repository.deleteAllReminders()

        // WHEN - Get the reminder by id from the database.
        val result = repository.getReminder("randomId")

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @After
    fun closeDb() = database.close()
}
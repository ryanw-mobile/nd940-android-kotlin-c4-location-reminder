package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    //    Testing implementation to the RemindersDao.kt

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    // Here I just do a set of CRUD tests.

    @Test
    fun saveReminder_getReminderById_returnsSuccess() = runBlockingTest {
        // GIVEN - save a reminder. (Create and Read test)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444,
            "titleA"
        )
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun updateReminder_getReminderById_returnsSuccess() = runBlockingTest {
        // GIVEN - save and then update a reminder. (update test)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444,
            "titleA"
        )
        database.reminderDao().saveReminder(reminder)

        val reminderB = ReminderDTO(
            "Reminder Title B",
            "Reminder Description B",
            "Location B",
            51.3930762,
            -0.2487444,
            "titleA"
        )
        database.reminderDao().saveReminder(reminderB)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminderB.id))
        assertThat(loaded.title, `is`(reminderB.title))
        assertThat(loaded.description, `is`(reminderB.description))
        assertThat(loaded.location, `is`(reminderB.location))
        assertThat(loaded.latitude, `is`(reminderB.latitude))
        assertThat(loaded.longitude, `is`(reminderB.longitude))
    }

    @Test
    fun deleteAllReminder_getReminderById_returnsError() = runBlockingTest {
        // GIVEN - delete all reminder. (Delete test)
        val reminder = ReminderDTO(
            "Reminder Title A",
            "Reminder Description A",
            "Location",
            51.4930762,
            -0.1487444,
            "titleA"
        )
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteAllReminders()

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded, `is`(nullValue()))
    }

    @After
    fun closeDb() = database.close()
}
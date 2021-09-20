package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    // A fake data source to act as a double to the real data source
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Reminder not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        // Check the list to find a reminder with matching id
        val reminder = reminders?.find {
            it.id == id
        }

        return if (reminder != null)
            Result.Success(reminder)
        else Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

}
package com.gchat.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.widget.Toast
import com.gchat.domain.model.ExtractedEntity
import java.util.*

/**
 * Utility class for handling Android Intents for extracted entities
 */
object EntityIntentHandler {
    
    /**
     * Handle entity action by launching appropriate intent
     */
    fun handleEntityAction(context: Context, entity: ExtractedEntity) {
        android.util.Log.d("EntityIntentHandler", "handleEntityAction called for entity type: ${entity.type}")
        try {
            when (entity) {
                is ExtractedEntity.ActionItem -> {
                    android.util.Log.d("EntityIntentHandler", "Handling ActionItem: ${entity.task}")
                    handleActionItem(context, entity)
                }
                is ExtractedEntity.DateTime -> {
                    android.util.Log.d("EntityIntentHandler", "Handling DateTime: ${entity.dateTime}")
                    handleDateTime(context, entity)
                }
                is ExtractedEntity.Contact -> {
                    android.util.Log.d("EntityIntentHandler", "Handling Contact: ${entity.name}")
                    handleContact(context, entity)
                }
                is ExtractedEntity.Location -> {
                    android.util.Log.d("EntityIntentHandler", "Handling Location: ${entity.address}")
                    handleLocation(context, entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EntityIntentHandler", "Failed to handle entity action: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Handle action item - open task/reminder app
     */
    private fun handleActionItem(context: Context, entity: ExtractedEntity.ActionItem) {
        // Try to open Google Tasks or other task apps
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, entity.task)
            
            // If due date is set, create as event
            entity.dueDate?.let { dueDate ->
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dueDate)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dueDate + (60 * 60 * 1000)) // 1 hour duration
            }
            
            // Set reminder based on priority
            val reminderMinutes = when (entity.priority) {
                ExtractedEntity.ActionItem.Priority.HIGH -> 60 // 1 hour before
                ExtractedEntity.ActionItem.Priority.MEDIUM -> 24 * 60 // 1 day before
                ExtractedEntity.ActionItem.Priority.LOW -> 7 * 24 * 60 // 1 week before
            }
            putExtra(CalendarContract.Reminders.MINUTES, reminderMinutes)
            putExtra(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: Create a reminder instead
            val reminderIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, entity.task)
                putExtra(CalendarContract.Events.DESCRIPTION, "Task from gChat")
            }
            context.startActivity(reminderIntent)
        }
    }
    
    /**
     * Handle date/time - add to calendar
     */
    private fun handleDateTime(context: Context, entity: ExtractedEntity.DateTime) {
        android.util.Log.d("EntityIntentHandler", "Creating calendar intent for time: ${entity.dateTime}")
        
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, entity.dateTime)
            
            if (entity.isRange && entity.endDateTime != null) {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, entity.endDateTime)
            } else {
                // Default 1 hour duration
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, entity.dateTime + (60 * 60 * 1000))
            }
            
            entity.description?.let {
                putExtra(CalendarContract.Events.TITLE, it)
            } ?: putExtra(CalendarContract.Events.TITLE, "Event")
        }
        
        val resolvedActivity = intent.resolveActivity(context.packageManager)
        android.util.Log.d("EntityIntentHandler", "Calendar intent resolves to: $resolvedActivity")
        
        if (resolvedActivity != null) {
            try {
                context.startActivity(intent)
                android.util.Log.d("EntityIntentHandler", "Calendar intent launched successfully")
            } catch (e: Exception) {
                android.util.Log.e("EntityIntentHandler", "Failed to launch calendar: ${e.message}", e)
                Toast.makeText(context, "Failed to open calendar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.w("EntityIntentHandler", "No calendar app found on device")
            Toast.makeText(context, "No calendar app installed. Please install Google Calendar from Play Store.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Handle contact - add to contacts
     */
    private fun handleContact(context: Context, entity: ExtractedEntity.Contact) {
        android.util.Log.d("EntityIntentHandler", "Creating contact intent for: ${entity.name ?: "Unknown"}")
        
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            
            entity.name?.let {
                putExtra(ContactsContract.Intents.Insert.NAME, it)
                android.util.Log.d("EntityIntentHandler", "  - Name: $it")
            }
            
            entity.email?.let {
                putExtra(ContactsContract.Intents.Insert.EMAIL, it)
                putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                android.util.Log.d("EntityIntentHandler", "  - Email: $it")
            }
            
            entity.phone?.let {
                putExtra(ContactsContract.Intents.Insert.PHONE, it)
                putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                android.util.Log.d("EntityIntentHandler", "  - Phone: $it")
            }
        }
        
        val resolvedActivity = intent.resolveActivity(context.packageManager)
        android.util.Log.d("EntityIntentHandler", "Contacts intent resolves to: $resolvedActivity")
        
        if (resolvedActivity != null) {
            try {
                context.startActivity(intent)
                android.util.Log.d("EntityIntentHandler", "Contacts intent launched successfully")
            } catch (e: Exception) {
                android.util.Log.e("EntityIntentHandler", "Failed to launch contacts: ${e.message}", e)
                Toast.makeText(context, "Failed to open contacts: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.w("EntityIntentHandler", "No contacts app found on device")
            Toast.makeText(context, "No contacts app installed. Emulator may not have Contacts pre-installed.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Handle location - open in maps
     */
    private fun handleLocation(context: Context, entity: ExtractedEntity.Location) {
        val uri = if (entity.latitude != null && entity.longitude != null) {
            // Use lat/long if available
            Uri.parse("geo:${entity.latitude},${entity.longitude}?q=${Uri.encode(entity.placeName ?: entity.address)}")
        } else {
            // Use address for search
            Uri.parse("geo:0,0?q=${Uri.encode(entity.address)}")
        }
        
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps") // Prefer Google Maps
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to any maps app
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(fallbackIntent)
            } else {
                Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Check if action can be handled
     */
    fun canHandleEntity(context: Context, entity: ExtractedEntity): Boolean {
        return try {
            val intent = when (entity) {
                is ExtractedEntity.ActionItem, is ExtractedEntity.DateTime -> {
                    Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                    }
                }
                is ExtractedEntity.Contact -> {
                    Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                    }
                }
                is ExtractedEntity.Location -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test"))
                }
            }
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            false
        }
    }
}


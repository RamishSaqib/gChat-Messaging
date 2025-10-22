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
        try {
            when (entity) {
                is ExtractedEntity.ActionItem -> handleActionItem(context, entity)
                is ExtractedEntity.DateTime -> handleDateTime(context, entity)
                is ExtractedEntity.Contact -> handleContact(context, entity)
                is ExtractedEntity.Location -> handleLocation(context, entity)
            }
        } catch (e: Exception) {
            android.util.Log.e("EntityIntentHandler", "Failed to handle entity action", e)
            Toast.makeText(context, "No app available to handle this action", Toast.LENGTH_SHORT).show()
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
            }
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle contact - add to contacts
     */
    private fun handleContact(context: Context, entity: ExtractedEntity.Contact) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            
            entity.name?.let {
                putExtra(ContactsContract.Intents.Insert.NAME, it)
            }
            
            entity.email?.let {
                putExtra(ContactsContract.Intents.Insert.EMAIL, it)
                putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
            }
            
            entity.phone?.let {
                putExtra(ContactsContract.Intents.Insert.PHONE, it)
                putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            }
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No contacts app found", Toast.LENGTH_SHORT).show()
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


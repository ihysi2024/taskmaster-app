package com.example.finalprojectprototype.calendar;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

public class CalendarHelper {

    public static void queryCalendars(ContentResolver contentResolver) {
        // Define the projection (fields you want to retrieve)
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
        };

        // Query calendars
        Cursor cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null, // No selection clause
                null, // No selection arguments
                null  // Default sort order
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long calendarId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID));
                String calendarName = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.NAME));
                String accountName = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME));
                String accountType = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE));

                // Log or use calendar information as needed
                Log.d("CalendarInfo", "Calendar ID: " + calendarId + ", Name: " + calendarName + ", Account: " + accountName + " (" + accountType + ")");

                // You can further query events for each calendar if needed
                queryEventsForCalendar(contentResolver, calendarId);
            }
            cursor.close();
        }
    }

    public static void queryEventsForCalendar(ContentResolver contentResolver, long calendarId) {
        // Define the projection (fields you want to retrieve)
        String[] eventProjection = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
        };

        // Query events for a specific calendar
        Uri.Builder builder = CalendarContract.Events.CONTENT_URI.buildUpon();
        builder.appendPath("calendars");
        builder.appendPath(Long.toString(calendarId));
        Uri uri = builder.build();

        Cursor eventCursor = contentResolver.query(
                uri,
                eventProjection,
                null, // No selection clause
                null, // No selection arguments
                null  // Default sort order
        );

        if (eventCursor != null) {
            while (eventCursor.moveToNext()) {
                long eventId = eventCursor.getLong(eventCursor.getColumnIndex(CalendarContract.Events._ID));
                String eventTitle = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.TITLE));
                String eventDescription = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
                long eventStart = eventCursor.getLong(eventCursor.getColumnIndex(CalendarContract.Events.DTSTART));
                long eventEnd = eventCursor.getLong(eventCursor.getColumnIndex(CalendarContract.Events.DTEND));

                // Log or use event information as needed
                Log.d("EventInfo", "Event ID: " + eventId + ", Title: " + eventTitle + ", Description: " + eventDescription + ", Start: " + eventStart + ", End: " + eventEnd);
            }
            eventCursor.close();
        }
    }
}

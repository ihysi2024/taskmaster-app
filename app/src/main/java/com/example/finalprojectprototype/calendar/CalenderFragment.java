package com.example.finalprojectprototype.calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.Manifest;
import android.widget.CalendarView;
import android.widget.Toast;

import com.example.finalprojectprototype.R;
import com.example.finalprojectprototype.tasks.Task;
import com.example.finalprojectprototype.tasks.TasksAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CalenderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CalenderFragment extends Fragment {
    boolean readCalendar = false;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private CalendarView calendarView;
    ArrayList<Task> allTasks;

    CalendarAdapter tasksAdapter;
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int REQUEST_CODE_WRITE_CALENDAR = 123;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    ArrayList<Task> calendarTasks;
    Date startingDay;
    Date endingDay;
    boolean dateSelected = false;
    private CollectionReference dbTasks;
    // The indices for the projection array above.
    public CalenderFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CalenderFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CalenderFragment newInstance(String param1, String param2) {
        CalenderFragment fragment = new CalenderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        calendarTasks = new ArrayList<>();
        allTasks = new ArrayList<>();
        FirebaseApp.initializeApp(getContext());
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbTasks = db.collection(getActivity().getIntent().getExtras().getString("user") + "_tasks");
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            readCalendar = true;
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_CALENDAR},
                    REQUEST_CODE_PERMISSION);
        }
        else {
            // Permission already granted, proceed with your operations
            queryCalendarData();
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_CALENDAR},
                    REQUEST_CODE_WRITE_CALENDAR);
        } else {
            // Permission already granted, proceed with your calendar operation
            //addCalendarEvent();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Run query

       return inflater.inflate(R.layout.fragment_calender, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        calendarView = view.findViewById(R.id.calendarView);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Log.d("CLICKED", "");
                makeListEmpty();
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);

                // Calculate the first day of the week
                Calendar startOfWeek = (Calendar) selectedDate.clone();
                int currentDayOfWeek = startOfWeek.get(Calendar.DAY_OF_WEEK);
                int daysToSubtract = currentDayOfWeek - startOfWeek.getFirstDayOfWeek();
                if (daysToSubtract < 0) {
                    daysToSubtract += 7; // Adjust if first day of week is after current day
                }
                startOfWeek.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
                startingDay = startOfWeek.getTime();

                // Calculate the last day of the week
                Calendar endOfWeek = (Calendar) startOfWeek.clone();
                endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
                endingDay = endOfWeek.getTime();

                Log.d("STARTING", startingDay.toString());
                Log.d("ENDING", endingDay.toString());

                populateWeekList();
            }
        });
        Collections.sort(calendarTasks, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {
                int dateComparison = task1.getDueDate().compareTo(task2.getDueDate());
                if (dateComparison == 0) {
                    // If dates are the same, compare urgency
                    return Integer.compare(task1.getUrgency().getValue(), task2.getUrgency().getValue());
                }
                return dateComparison;
            }
        });
        RecyclerView recyclerView = view.findViewById(R.id.week_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksAdapter = new CalendarAdapter(calendarTasks);
        recyclerView.setAdapter(tasksAdapter);
    }

    private void makeListEmpty() {
        calendarTasks.clear();
        Log.d("SIZE", String.valueOf(calendarTasks.size()));
        tasksAdapter.notifyDataSetChanged();
    }

    public void populateWeekList() {
        dbTasks.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Timestamp ts = (Timestamp) document.getData().get("dueDate");
                            Date dueDate = ts.toDate();

                            Log.d("STARTING", startingDay.toString());
                            Log.d("ENDING", endingDay.toString());
                            Log.d("CURRENT", dueDate.toString());

                            if (dueDate.before(endingDay) && dueDate.after(startingDay)) {
                                Log.d("HERE", "TRUE");
                                String name = document.getString("name");
                                String desc = document.getString("desc");
                                String urgencyString = document.getString("urgency");

                                Task.UrgencyLevel urgency = null;
                                if (urgencyString != null) {
                                    switch (urgencyString) {
                                        case "LEVEL_1":
                                            urgency = Task.UrgencyLevel.LEVEL_1;
                                            break;
                                        case "LEVEL_2":
                                            urgency = Task.UrgencyLevel.LEVEL_2;
                                            break;
                                        case "LEVEL_3":
                                            urgency = Task.UrgencyLevel.LEVEL_3;
                                            break;
                                        case "LEVEL_4":
                                            urgency = Task.UrgencyLevel.LEVEL_4;
                                            break;
                                        case "LEVEL_5":
                                            urgency = Task.UrgencyLevel.LEVEL_5;
                                            break;
                                    }
                                }

                                if (name != null && desc != null && urgency != null) {
                                    Task t = new Task(name, desc, dueDate, urgency);
                                    calendarTasks.add(t);
                                }
                            }
                        }
                        tasksAdapter.notifyDataSetChanged(); // Notify adapter of data changes
                    }
                });
    }
    private void addCalendarEvent() {
        // Get the content resolver
        ContentResolver contentResolver = getActivity().getContentResolver();

        // Define the event details
        ContentValues eventValues = new ContentValues();
        dbTasks.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int i = 0;
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            eventValues.put(CalendarContract.Events.CALENDAR_ID, i); // Use the correct calendar ID
                            eventValues.put(CalendarContract.Events.TITLE, document.getData().get("name").toString());
                            eventValues.put(CalendarContract.Events.DESCRIPTION, document.getData().get("desc").toString());


                            // Set the event start and end time
                            Timestamp endTime = (Timestamp) document.getData().get("dueDate");
                            Date endDate = endTime.toDate();
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(endDate.getYear(), endDate.getMonth(), endDate.getSeconds());

                            Calendar startTime = Calendar.getInstance();
                            startTime.set(endDate.getYear(), endDate.getMonth(), endDate.getDay());
                            startTime.set(Calendar.HOUR_OF_DAY, 0);
                            startTime.set(Calendar.MINUTE, 0);
                            startTime.set(Calendar.SECOND, 0);
                            startTime.set(Calendar.MILLISECOND, 0);


                            eventValues.put(CalendarContract.Events.DTSTART, startTime.getTimeInMillis());
                            eventValues.put(CalendarContract.Events.DTEND, calendar.getTimeInMillis());

                            // Set timezone
                            eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

                            // Insert the event into the calendar
                            Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues);

                            if (uri != null) {
                                long eventId = Long.parseLong(uri.getLastPathSegment());
                                Log.d("CalendarEvent", "Event added with ID: " + eventId);
                                // Update the calendar view to display the new event
                            } else {
                                Toast.makeText(getContext(), "Failed to add event", Toast.LENGTH_SHORT).show();
                            }
                        }
                        i++;
                    }
                });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            // Check if permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with your operations
                if (readCalendar) {
                    queryCalendarData();
                }
                else {
                    addCalendarEvent();
                }
            } else {
                // Permission denied, handle the case where user declines the permission
                // You may inform the user or disable functionality that depends on the permission
            }
        }
    }

    private void queryCalendarData() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        // Query calendars
        CalendarHelper.queryCalendars(contentResolver);
    }

    private void addTask(Task t) {

        calendarTasks.add(t);
        /**
        Collections.sort(calendarTasks, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {
                int dateComparison = task1.getDueDate().compareTo(task2.getDueDate());
                if (dateComparison == 0) {
                    // If dates are the same, compare urgency
                    return Integer.compare(task1.getUrgency().getValue(), task2.getUrgency().getValue());
                }
                return dateComparison;
            }
        });
        tasksAdapter.notifyDataSetChanged();
         **/
    }


}



package com.example.finalprojectprototype.calendar;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import com.google.android.gms.tasks.OnFailureListener;
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
 * Visualizes the Calendar View and the tasks associated with the
 * selected week. Users can also choose to add individual tasks by selecting them.
 */
public class CalenderFragment extends Fragment {

    private static final int REQUEST_CODE_SHARE = 1;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private CalendarView calendarView;

    private CalendarAdapter tasksAdapter;
    private FirebaseFirestore db;
    ArrayList<Task> calendarTasks;
    Date startingDay;
    Date endingDay;
    private CollectionReference dbTasks;

    public CalenderFragment() {
        // Required empty public constructor
    }

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
        FirebaseApp.initializeApp(getContext());

        db = FirebaseFirestore.getInstance();
        dbTasks = db.collection(getActivity().getIntent().getExtras().getString("user") + "_tasks");

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

                populateWeekList();
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.week_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksAdapter = new CalendarAdapter(calendarTasks);
        recyclerView.setAdapter(tasksAdapter);

        shareTask();
    }

    private void makeListEmpty() {
        calendarTasks.clear();
        tasksAdapter.notifyDataSetChanged();
    }

    public void shareTask() {
        tasksAdapter.setOnClickListener(new CalendarAdapter.OnClickListener() {
            @Override
            public void onClick(int position, Task model) {
                String task = "Name: " + calendarTasks.get(position).getName() + "\n"
                        + "Description: " + calendarTasks.get(position).getDesc() + "\n"
                        + "Due Date: " + calendarTasks.get(position).getDueDate().toString() + "\n"
                        + "Urgency Level: " + calendarTasks.get(position).getUrgency();
                Log.d("task", task);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, task);
                startActivityForResult(Intent.createChooser(shareIntent, "Share via"), REQUEST_CODE_SHARE);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SHARE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getActivity(), "Share completed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Share not completed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void populateWeekList() {
        dbTasks.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Timestamp ts = (Timestamp) document.getData().get("dueDate");
                            Date dueDate = ts.toDate();

                            if (dueDate.before(endingDay) && dueDate.after(startingDay)) {
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
                                    addTask(t);
                                }
                            }
                        }
                        tasksAdapter.notifyDataSetChanged(); // Notify adapter of data changes
                    }
                });
    }


    private void addTask(Task t) {
        calendarTasks.add(t);
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
    }

}



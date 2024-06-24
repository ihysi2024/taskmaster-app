package com.example.finalprojectprototype.tasks;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.finalprojectprototype.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Represents the interface that allows a user to create, edit, and delete a task.
 * Users can select a task to edit it, or select the check box on the task to
 * mark it as completed and remove it from the list. Overdue items on the
 * task list are highlighted in purple.
 */
public class TasksFragment extends Fragment {


    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private static final int REQUEST_CODE = 100;
    private ArrayList<Task> allTasks;

    private TasksAdapter tasksAdapter;
    private View card;
    private LinearLayout linearLayout;
    private FirebaseFirestore db;

    private String documentID;
    private RecyclerView recyclerView;
    private ArrayList<String> dueToday = new ArrayList<>();
    private CollectionReference dbTasks;

    //TODO: make new firebase called username_weekof_() with "all tasks" and "tasks left"

    private CollectionReference dbProductivity;
    private Date startingDay;
    private Date endingDay;
    private Calendar calendar;

    public TasksFragment() {
        // Required empty public constructor
    }

    public static TasksFragment newInstance(String param1, String param2) {
        TasksFragment fragment = new TasksFragment();
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
        allTasks = new ArrayList<>();
        FirebaseApp.initializeApp(getContext());
        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        // Set the calendar to the start of the week (Sunday in this case)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        // Optional: Clear the time part to get the date only
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Calendar startOfWeek = (Calendar) calendar.clone();
        int currentDayOfWeek = startOfWeek.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = currentDayOfWeek - startOfWeek.getFirstDayOfWeek();
        if (daysToSubtract < 0) {
            daysToSubtract += 7;
        }
        startOfWeek.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
        startingDay = startOfWeek.getTime();

        // Calculate the last day of the week
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
        endingDay = endOfWeek.getTime();

        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
        String currentWeek = dateFormat.format(calendar.getTime());


        dbProductivity = db.collection(getActivity().getIntent().getExtras().getString("user") + "_" + currentWeek);
        dbTasks = db.collection(getActivity().getIntent().getExtras().getString("user") + "_tasks");

        new TaskNotifs().execute();
        populateList();
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Collections.sort(allTasks, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {
                int dateComparison = Integer.compare(task1.getDueDate().getDay(), task2.getDueDate().getDay());
                if (dateComparison == 0) {
                    return Integer.compare(task2.getUrgency().getValue(), task1.getUrgency().getValue());
                }
                return dateComparison;
            }
        });
        tasksAdapter = new TasksAdapter(allTasks, getActivity().getIntent().getExtras().getString("user"));
        recyclerView = view.findViewById(R.id.task_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(tasksAdapter);
        editTask(getLayoutInflater(), recyclerView);

    }

    /**
     * Populate the list of tasks to be visualized in the Recycler View
     */
    public void populateList() {
        dbTasks.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        int counter = 0;
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String name = document.getData().get("name").toString();
                            String desc = document.getData().get("desc").toString();
                            Task.UrgencyLevel urgency = null;
                            if (document.getData().get("urgency").toString().equals("LEVEL_1")) {
                                urgency = Task.UrgencyLevel.LEVEL_1;
                            }
                            else if (document.getData().get("urgency").equals("LEVEL_2")) {
                                urgency = Task.UrgencyLevel.LEVEL_2;
                            }
                            else if (document.getData().get("urgency").equals("LEVEL_3")) {
                                urgency = Task.UrgencyLevel.LEVEL_3;
                            }
                            else if (document.getData().get("urgency").equals("LEVEL_4")) {
                                urgency = Task.UrgencyLevel.LEVEL_4;
                            }
                            else if (document.getData().get("urgency").equals("LEVEL_5")) {
                                urgency = Task.UrgencyLevel.LEVEL_5;
                            }
                            Timestamp strDate = (Timestamp) document.getData().get("dueDate");
                            Date dueDate = strDate.toDate();
                            Task t = new Task(name, desc, dueDate, urgency);

                            // add this task to the list
                            addTask(t);

                            // count the number of tasks occurring this week
                            if (dueDate.before(endingDay) && dueDate.after(startingDay)) {
                                counter++;
                            }

                        }

                        // store the number of tasks in the Productivity Tracker DB
                        Map<String, Object> data = new HashMap<>();
                        data.put("all tasks", counter);
                        data.put("tasks completed", 0);

                        checkIfCollectionIsEmpty(new CollectionEmptyCheckCallback() {
                            @Override
                            public void onResult(boolean isEmpty) {
                                if (isEmpty) {
                                    dbProductivity
                                            .add(data)
                                            .addOnSuccessListener(documentReference -> Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId()))
                                            .addOnFailureListener(e -> Log.w("Firestore", "Error adding document", e));

                                    Log.d("EMPTY", "Collection is empty");
                                    // Do something if collection is empty
                                } else {
                                    getWeekID(new FirestoreCallback() {
                                                  @Override
                                                  public void onCallback(String documentId) {
                                                      if (documentId != null) {
                                                          DocumentReference docRef = dbProductivity.document(documentId);
                                                          docRef.get()
                                                                  .addOnCompleteListener(task -> {
                                                                      if (task.isSuccessful()) {
                                                                          DocumentSnapshot document = task.getResult();
                                                                          if (document.exists()) {
                                                                              // Retrieve the field value
                                                                              Object allTasks = document.get("all tasks");
                                                                              Object tasksCompleted = document.get("tasks completed");

                                                                              //data.put("all tasks", Integer.parseInt(allTasks.toString()) + Integer.parseInt(data.get("all tasks").toString()));
                                                                              data.put("tasks completed", tasksCompleted);
                                                                              docRef.update(data)
                                                                                      .addOnSuccessListener(aVoid -> Log.d("Firestore", "DocumentSnapshot successfully updated!"))
                                                                                      .addOnFailureListener(e -> Log.w("Firestore", "Error updating document", e));
                                                                              Log.d("ID", "Field value: " + allTasks);
                                                                          } else {
                                                                              Log.d("ID", "No such document");
                                                                          }
                                                                      } else {
                                                                          Log.w("ID", "Error getting document", task.getException());
                                                                      }
                                                                  });
                                                      }
                                                  }
                                              });
                                    Log.d("EMPTY", "Collection is not empty");
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.w("EMPTY", "Error checking collection", e);
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error getting documents.", e);
                    }
                });
    }
    public interface FirestoreCallback {
        void onCallback(String documentId);
    }

    public void getWeekID(FirestoreCallback firestoreCallback) {
        dbProductivity
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String docID = "";
                    if (!queryDocumentSnapshots.isEmpty()) {
                        docID = queryDocumentSnapshots.getDocuments().get(0).getId();
                    }
                    firestoreCallback.onCallback(docID);
                })
                .addOnFailureListener(e -> {
                    Log.w("DOCUMENT", "Error getting documents", e);
                    firestoreCallback.onCallback(null); // Return null in case of error
                });
    }

    public interface CollectionEmptyCheckCallback {
        void onResult(boolean isEmpty);
        void onError(Exception e);
    }

    public void checkIfCollectionIsEmpty(CollectionEmptyCheckCallback callback) {
        // Query the collection with a limit of 1
        dbProductivity
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        boolean isEmpty = querySnapshot.isEmpty();
                        if (isEmpty) {
                            Log.d("EMPTY", "The collection is empty");
                        } else {
                            Log.d("EMPTY", "The collection is not empty");
                        }
                        callback.onResult(isEmpty);
                    } else {
                        Log.w("EMPTY", "Error getting documents: ", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        card = inflater.inflate(R.layout.card, container, false);
        linearLayout = card.findViewById(R.id.linearLayout);
        View tasks =  inflater.inflate(R.layout.fragment_tasks, container, false);
        addTaskToView(inflater, tasks);
        return tasks;
    }

    private void addTaskToView(LayoutInflater inflater, View tasks) {
        FloatingActionButton fab = tasks.findViewById(R.id.add_tasks);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View dialogView = inflater.inflate(R.layout.add_task_dialog, null);
                dialogView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.rounded_dialog));
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(dialogView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditText inpName = dialogView.findViewById(R.id.editTextTaskName);
                                EditText inpDesc = dialogView.findViewById(R.id.editTextDescription);
                                DatePicker inpDate = dialogView.findViewById(R.id.datePickerDueDate);
                                Task.UrgencyLevel inpUrgency = Task.UrgencyLevel.LEVEL_1;
                                RadioButton one = dialogView.findViewById(R.id.radioButton1);
                                RadioButton two = dialogView.findViewById(R.id.radioButton2);
                                RadioButton three = dialogView.findViewById(R.id.radioButton3);
                                RadioButton four = dialogView.findViewById(R.id.radioButton4);
                                RadioButton five = dialogView.findViewById(R.id.radioButton5);


                                if (one.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_1;
                                } else if (two.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_2;
                                } else if (three.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_3;
                                } else if (four.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_4;
                                } else if (five.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_5;
                                }
                                int day = inpDate.getDayOfMonth();
                                int month = inpDate.getMonth();
                                int year = inpDate.getYear();

                                // Create a Calendar instance and set the selected date
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, month, day);

                                // Convert Calendar to Date object
                                Date selectedDate = calendar.getTime();
                                Task t = new Task(inpName.getText().toString(),
                                        inpDesc.getText().toString(),
                                        selectedDate,
                                        inpUrgency);

                                addTaskForUser(t);

                                addTask(t);

                                if (selectedDate.before(endingDay) && selectedDate.after(startingDay)) {
                                    int counter = 1;
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("all tasks", counter);
                                    data.put("tasks completed", 0);

                                    checkIfCollectionIsEmpty(new CollectionEmptyCheckCallback() {
                                        @Override
                                        public void onResult(boolean isEmpty) {
                                            if (isEmpty) {
                                                dbProductivity
                                                        .add(data)
                                                        .addOnSuccessListener(documentReference -> Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId()))
                                                        .addOnFailureListener(e -> Log.w("Firestore", "Error adding document", e));

                                                Log.d("EMPTY", "Collection is empty");
                                                // Do something if collection is empty
                                            } else {
                                                getWeekID(new FirestoreCallback() {
                                                    @Override
                                                    public void onCallback(String documentId) {
                                                        if (documentId != null) {
                                                            DocumentReference docRef = dbProductivity.document(documentId);
                                                            docRef.get()
                                                                    .addOnCompleteListener(task -> {
                                                                        if (task.isSuccessful()) {
                                                                            DocumentSnapshot document = task.getResult();
                                                                            if (document.exists()) {
                                                                                // Retrieve the field value
                                                                                Object allTasks = document.get("all tasks");
                                                                                Object tasksCompleted = document.get("tasks completed");
                                                                                int added = Integer.parseInt(allTasks.toString()) + 1;
                                                                                data.put("all tasks", added);
                                                                                data.put("tasks completed", tasksCompleted);
                                                                                docRef.update(data)
                                                                                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "DocumentSnapshot successfully updated!"))
                                                                                        .addOnFailureListener(e -> Log.w("Firestore", "Error updating document", e));
                                                                                Log.d("ID", "Field value: " + allTasks);
                                                                            } else {
                                                                                Log.d("ID", "No such document");
                                                                            }
                                                                        } else {
                                                                            Log.w("ID", "Error getting document", task.getException());
                                                                        }
                                                                    });
                                                        }
                                                        Log.d("EMPTY", "Collection is not empty");
                                                        // Do something if collection is not empty
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.w("EMPTY", "Error checking collection", e);
                                            // Handle error
                                        }
                                    });

                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                // Create and show the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    /**
     * Allow the user to edit the task on click
     * @param inflater
     * @param rv
     */
    public void editTask(LayoutInflater inflater, RecyclerView rv) {
        tasksAdapter.setOnClickListener(new TasksAdapter.OnClickListener() {
            @Override
            public void onClick(int position, Task model) {
                Task taskToEdit = allTasks.get(position);
                View dialogView = inflater.inflate(R.layout.add_task_dialog, null);
                dialogView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.rounded_dialog));
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                EditText inpName = dialogView.findViewById(R.id.editTextTaskName);
                inpName.setText(taskToEdit.getName());
                EditText inpDesc = dialogView.findViewById(R.id.editTextDescription);
                inpDesc.setText(taskToEdit.getDesc());
                DatePicker inpDate = dialogView.findViewById(R.id.datePickerDueDate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(taskToEdit.getDueDate());

                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                inpDate.init(year, month, day, null);
                RadioButton urgency1 = dialogView.findViewById(R.id.radioButton1);
                RadioButton urgency2 = dialogView.findViewById(R.id.radioButton2);
                RadioButton urgency3 = dialogView.findViewById(R.id.radioButton3);
                RadioButton urgency4 = dialogView.findViewById(R.id.radioButton4);
                RadioButton urgency5 = dialogView.findViewById(R.id.radioButton5);

                Task.UrgencyLevel inpUrgency = taskToEdit.getUrgency();
                if (inpUrgency == Task.UrgencyLevel.LEVEL_1) {
                    urgency1.setChecked(true);
                }
                else if (inpUrgency == Task.UrgencyLevel.LEVEL_2) {
                    urgency2.setChecked(true);
                }
                else if (inpUrgency == Task.UrgencyLevel.LEVEL_3) {
                    urgency3.setChecked(true);
                }
                else if (inpUrgency == Task.UrgencyLevel.LEVEL_4) {
                    urgency4.setChecked(true);
                }
                else if (inpUrgency == Task.UrgencyLevel.LEVEL_5) {
                    urgency5.setChecked(true);
                }

                dbTasks.get().addOnCompleteListener(taskSnapshot -> {
                    if (taskSnapshot.isSuccessful()) {
                        for (QueryDocumentSnapshot document : taskSnapshot.getResult()) {
                            Timestamp ts = (Timestamp) document.getData().get("dueDate");
                            if ((document.getData().get("name").toString().equals(taskToEdit.getName().toString()))
                                && (document.getData().get("desc").toString().equals(taskToEdit.getDesc().toString()))
                                    && (String.valueOf(ts.getSeconds()).equals(String.valueOf(taskToEdit.getDueDate().getTime()).substring(0, String.valueOf(taskToEdit.getDueDate().getTime()).length() - 3)))
                                    && (document.getData().get("urgency").toString().equals(taskToEdit.getUrgency().toString()))) {
                                documentID = document.getId();
                            }
                        }
                    }
                });
                builder.setView(dialogView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                EditText inpName = dialogView.findViewById(R.id.editTextTaskName);
                                EditText inpDesc = dialogView.findViewById(R.id.editTextDescription);
                                DatePicker inpDate = dialogView.findViewById(R.id.datePickerDueDate);
                                Task.UrgencyLevel inpUrgency = Task.UrgencyLevel.LEVEL_1;
                                RadioButton one = dialogView.findViewById(R.id.radioButton1);
                                RadioButton two = dialogView.findViewById(R.id.radioButton2);
                                RadioButton three =  dialogView.findViewById(R.id.radioButton3);
                                RadioButton four = dialogView.findViewById(R.id.radioButton4);
                                RadioButton five = dialogView.findViewById(R.id.radioButton5);


                                if (one.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_1;
                                } else if (two.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_2;
                                } else if (three.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_3;
                                } else if (four.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_4;
                                } else if (five.isChecked()) {
                                    inpUrgency = Task.UrgencyLevel.LEVEL_5;
                                }
                                int day = inpDate.getDayOfMonth();
                                int month = inpDate.getMonth();
                                int year = inpDate.getYear();

                                // Create a Calendar instance and set the selected date
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, month, day);

                                // Convert Calendar to Date object
                                Date selectedDate = calendar.getTime();
                                Task t = new Task(inpName.getText().toString(),
                                        inpDesc.getText().toString(),
                                        selectedDate,
                                        inpUrgency);

                                Map<String, Object> updatedData = new HashMap<>();
                                updatedData.put("name", inpName.getText().toString());
                                updatedData.put("desc", inpDesc.getText().toString());
                                updatedData.put("dueDate", selectedDate); // Example: Update dueDate to current date/time
                                updatedData.put("urgency", inpUrgency); // Example: Update urgency to 2 (or any other value)
                                editTaskForDB(documentID, updatedData, rv, position, t);
                        }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                // Create and show the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    public void editTaskForDB(String id, Map<String, Object> task, RecyclerView rv, int position, Task t) {

        DocumentReference docRef = dbTasks.document(id);
        docRef.update(task)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        allTasks.set(position, t);
                        rv.getAdapter().notifyItemChanged(position);
                        // Handle successful update
                        Log.d("Revision Complete", "Document updated successfully!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure
                        Log.w("Revision Incomplete", "Error updating document", e);
                    }
                });
    }

    private void addTaskForUser(Task t) {
        dbTasks.add(t).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                // after the data addition is successful
                // we are displaying a success toast message.
                Toast.makeText(getContext(), "Your Place has been added to Firebase Firestore", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // this method is called when the data addition process is failed.
                // displaying a toast message when data addition is failed.
                Toast.makeText(getContext(), "Fail to add place \n" + e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTask(Task t) {
        allTasks.add(t);
        Collections.sort(allTasks, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {
                int dateComparison = Integer.compare(task1.getDueDate().getDay(), task2.getDueDate().getDay());
                if (dateComparison == 0) {
                    return Integer.compare(task2.getUrgency().getValue(), task1.getUrgency().getValue());
                }
                return dateComparison;
            }
        });

        tasksAdapter.notifyDataSetChanged();
    }

    private class TaskNotifs extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {

            dbTasks.get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Calendar calendar = Calendar.getInstance();

                                Timestamp ts = (Timestamp) document.getData().get("dueDate");
                                long seconds = ts.getSeconds();
                                int nanoseconds = ts.getNanoseconds(); // Example nanoseconds part

                                // Convert the timestamp to milliseconds
                                long milliseconds = (seconds * 1000) + (nanoseconds / 1000000);
                                Calendar calendarFromTimestamp = Calendar.getInstance();
                                calendarFromTimestamp.setTimeInMillis(milliseconds);
                                calendarFromTimestamp.setTimeZone(TimeZone.getTimeZone("UTC"));

                                boolean sameDay = isSameDay(calendar, calendarFromTimestamp);
                                if (sameDay) {
                                    dueToday.add(document.getData().get("name").toString());
                                }
                            }
                            String alertMessage = "";

                            if (dueToday.size() > 0) {

                                for (String msg: dueToday) {
                                    alertMessage = alertMessage + msg + "\n";
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }});
            return dueToday;
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                    cal1.get(Calendar.DAY_OF_MONTH) == (cal2.get(Calendar.DAY_OF_MONTH));
        }
    }


}
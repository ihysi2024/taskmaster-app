package com.example.finalprojectprototype.tasks;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.finalprojectprototype.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TasksFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TasksFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ArrayList<Task> allTasks;

    TasksAdapter tasksAdapter;
    View card;
    LinearLayout linearLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference dbTasks;
    public TasksFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TasksFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        dbTasks = db.collection(getActivity().getIntent().getExtras().getString("user") + "_tasks");
        populateList();
        Log.d("SIZE", String.valueOf(allTasks.size()));
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Collections.sort(allTasks, new Comparator<Task>() {
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
        tasksAdapter = new TasksAdapter(allTasks);
        RecyclerView recyclerView = view.findViewById(R.id.task_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(tasksAdapter);

    }

    private void addMarker(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        // Parse the given date
        // Get current date
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        boolean isToday = date != null && date.before(currentDate);

        Log.d("IS TODAY", String.valueOf(isToday));
        if (isToday) {
            addDrawableToCard(R.drawable.overdue);
        }
        // Add 2 days to the current date
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        Date twoDaysLater = calendar.getTime();

        Log.d("TWO DAYS", twoDaysLater.toString());
        Log.d("CURRENT DATE", date.toString());
        // Compare dates
        boolean isWithinTwoDays = date != null && date.after(currentDate) && date.before(twoDaysLater);

        Log.d("TRUE", String.valueOf(isWithinTwoDays));
        if (isWithinTwoDays) {
            Log.d("IS WITHIN 2 DAYS", "");
            addDrawableToCard(R.drawable.high_priority_task);
        }

    }

    private void addDrawableToCard(int drawableResId) {
        // Create an ImageView
        ImageView imageView = new ImageView(getContext());

        // Set the drawable resource to the ImageView
        Drawable drawable = ContextCompat.getDrawable(getActivity(), drawableResId);
        imageView.setImageDrawable(drawable);

        // Define the layout parameters for the ImageView
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        // Position the ImageView (for example, top-right corner)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        // Add the ImageView to the RelativeLayout
        linearLayout.addView(imageView, layoutParams);
    }

    public void populateList() {
        dbTasks.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String name = document.getData().get("name").toString();
                            String desc = document.getData().get("desc").toString();
                            Task.UrgencyLevel urgency = null;
                            if (document.getData().get("urgency") == "LEVEL_1") {
                                urgency = Task.UrgencyLevel.LEVEL_1;
                            }
                            else if (document.getData().get("urgency") == "LEVEL_2") {
                                urgency = Task.UrgencyLevel.LEVEL_2;
                            }
                            else if (document.getData().get("urgency") == "LEVEL_3") {
                                urgency = Task.UrgencyLevel.LEVEL_3;
                            }
                            else if (document.getData().get("urgency") == "LEVEL_4") {
                                urgency = Task.UrgencyLevel.LEVEL_4;
                            }
                            else if (document.getData().get("urgency") == "LEVEL_5") {
                                urgency = Task.UrgencyLevel.LEVEL_5;
                            }
                            Timestamp strDate = (Timestamp) document.getData().get("dueDate");
                            Date dueDate = strDate.toDate();
                            Task t = new Task(name, desc, dueDate, urgency);
                            addTask(t);
                            Log.d("SIZE", String.valueOf(allTasks.size()));
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error getting documents.", e);
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

                                addTaskForUser(t);

                                addTask(t);
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
        return tasks;
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
                int dateComparison = task1.getDueDate().compareTo(task2.getDueDate());
                if (dateComparison == 0) {
                    // If dates are the same, compare urgency
                    return Integer.compare(task1.getUrgency().getValue(), task2.getUrgency().getValue());
                }
                return dateComparison;
            }
        });
        addMarker(t.getDueDate());
        tasksAdapter.notifyDataSetChanged();
    }
}
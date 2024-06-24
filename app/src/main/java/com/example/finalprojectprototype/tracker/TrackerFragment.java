package com.example.finalprojectprototype.tracker;

import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.finalprojectprototype.R;
import com.example.finalprojectprototype.tasks.TasksFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Allows the user to track their productivity and have it be visualized in image view
 * markers.
 */
public class TrackerFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference dbProductivity;
    private Calendar calendar;


    public TrackerFragment() {
        // Required empty public constructor
    }

    public static TrackerFragment newInstance(String param1, String param2) {
        TrackerFragment fragment = new TrackerFragment();
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
        FirebaseApp.initializeApp(getContext());
        mAuth = FirebaseAuth.getInstance();
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
            daysToSubtract += 7; // Adjust if first day of week is after current day
        }
        startOfWeek.add(Calendar.DAY_OF_MONTH, -daysToSubtract);

        // Calculate the last day of the week
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);

        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
        String currentWeek = dateFormat.format(calendar.getTime());
        dbProductivity = db.collection(getActivity().getIntent().getExtras().getString("user") + "_" + currentWeek);

    }

    public void getWeekID(TasksFragment.FirestoreCallback firestoreCallback) {
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



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tracker, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getWeekID(new TasksFragment.FirestoreCallback() {
            @Override
            public void onCallback(String documentId) {
                if (documentId != null) {
                    DocumentReference docRef = dbProductivity.document(documentId);
                    docRef.get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        ImageView imageView = view.findViewById(R.id.image); // Replace with your ImageView ID
                                        TextView product = view.findViewById(R.id.progress);
                                        TextView taskLeft = view.findViewById(R.id.tasksLeft);
                                        Drawable goodProg = getResources().getDrawable(R.drawable.good_progress); // Replace with your drawable resource
                                        Drawable badProg = getResources().getDrawable(R.drawable.bad_progress); // Replace with your drawable resource
                                        Drawable pending = getResources().getDrawable(R.drawable.pending_progress); // Replace with your drawable resource

                                        int allTasks = Integer.parseInt(document.getData().get("all tasks").toString());
                                        int tasksDone = Integer.parseInt(document.getData().get("tasks completed").toString());
                                        if (allTasks > 0) {
                                            taskLeft.setText(String.valueOf(allTasks - tasksDone));
                                            double percDone = (double) tasksDone / allTasks;
                                            Log.d("PERC", String.valueOf(percDone));
                                            if (percDone <= 0.75 && percDone >= 0.25) {
                                                product.setText("Good Productivity");
                                                imageView.setImageDrawable(pending);
                                            } else if (percDone > 0.75) {
                                                imageView.setImageDrawable(goodProg);
                                                product.setText("Peak Productivity");
                                            } else {
                                                imageView.setImageDrawable(badProg);
                                                product.setText("Low Productivity");
                                            }
                                        }
                                        else {
                                            taskLeft.setText(String.valueOf(0));
                                        }
                                    }
                                }
                            });
                }
            }
        });
    }
}
package com.example.finalprojectprototype.tasks;

import android.content.Context;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectprototype.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.LoadBundleTaskProgress;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TasksViewHolder> {
    private ArrayList<Task> emplist;
    private OnClickListener onClickListener;
    private Context context;
    FirebaseFirestore db;
    CollectionReference dbTasks;
    String user;

    Calendar calendar;
    Date startingDay;
    Date endingDay;
    CollectionReference dbProductivity;
    public TasksAdapter(ArrayList<Task> emplist, String username) {
        this.emplist = emplist;
        this.user = username;
        db = FirebaseFirestore.getInstance();
        dbTasks = db.collection(user + "_tasks");
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
        startingDay = startOfWeek.getTime();

        // Calculate the last day of the week
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
        endingDay = endOfWeek.getTime();

        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
        String currentWeek = dateFormat.format(calendar.getTime());
        dbProductivity = db.collection(user + "_" + currentWeek);

    }

    @Override
    public TasksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card, parent, false);
        return new TasksViewHolder(itemView);
    }


    @Override
    public int getItemCount() {
        return emplist.size();
    }

    @Override
    public void onBindViewHolder(TasksViewHolder holder, int position) {
        Task currentEmp = emplist.get(position);
        holder.name.setText(currentEmp.getName());
        holder.desc.setText(currentEmp.getDesc());
        Date date = currentEmp.getDueDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(date);
        holder.dueDate.setText(formattedDate);
        Task.UrgencyLevel urg = currentEmp.getUrgency();
        if (urg == Task.UrgencyLevel.LEVEL_1) {
            holder.urgency.setText("!");
        }
        else if (urg == Task.UrgencyLevel.LEVEL_2) {
            holder.urgency.setText("!!");
        }
        else if (urg == Task.UrgencyLevel.LEVEL_3) {
            holder.urgency.setText("!!!");
        }
        else if (urg == Task.UrgencyLevel.LEVEL_4) {
            holder.urgency.setText("!!!!");
        }
        else if (urg == Task.UrgencyLevel.LEVEL_5) {
            holder.urgency.setText("!!!!!");
        }

        Collections.sort(emplist, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {
                int dateComparison = Integer.compare(task1.getDueDate().getDay(), task2.getDueDate().getDay());
                Log.d("HELLO", String.valueOf(task1.getDueDate().getDay()));
                Log.d("HELLO", String.valueOf(task2.getDueDate().getDay()));
                Log.d("HELLO", String.valueOf(dateComparison));
                if (dateComparison == 0) {
                    Log.d("HELLO", String.valueOf(task2.getDueDate()));
                    Log.d("HELLO", String.valueOf(task1.getUrgency()));
                    // If dates are the same, compare urgency
                    return Integer.compare(task2.getUrgency().getValue(), task1.getUrgency().getValue());
                }
                return dateComparison;
            }
        });

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date currentDate = calendar.getTime();

        Log.d("NAME", currentEmp.getName());
        Log.d("DATE", String.valueOf(date.getDay()));
        Log.d("CURRENT DATE", String.valueOf(currentDate.getDate()));

        boolean isToday = ((date.getDate() - 1) < currentDate.getDate());

        Log.d("DATE", String.valueOf(date.getDate()));
        Log.d("DATE", String.valueOf(currentDate.getDate()));
        Log.d("ISTODAY", String.valueOf(isToday));
        if (isToday) {
            Log.d("HELLO", "");
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.light_purple));
            holder.name.setTextColor(Color.BLACK);
            holder.desc.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.dueDate.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.urgency.setTextColor(context.getResources().getColor(android.R.color.white));
        }
        else {
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.white));
            holder.name.setTextColor(Color.BLACK);
            holder.desc.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.dueDate.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.urgency.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }

        holder.checkBox.setOnCheckedChangeListener(null); // Clear any existing listener

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                removeTask(position, currentEmp.getName(), currentEmp.getDesc(),
                        currentEmp.getDueDate(), currentEmp.getUrgency());
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener != null) {
                    onClickListener.onClick(position, currentEmp);
                }
            }
        });
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

    // TODO: update the "tasks left" field to be 1 less than its original value
    private void removeTask(int position, String name, String description,
                            Date dueDate, Task.UrgencyLevel urgency) {

        Query query = dbTasks
                .whereEqualTo("name", name)
                .whereEqualTo("desc", description)
                .whereEqualTo("dueDate", dueDate)
                .whereEqualTo("urgency", urgency.toString());

        query.get().addOnCompleteListener(taskSnapshot -> {
            if (taskSnapshot.isSuccessful()) {
                for (QueryDocumentSnapshot document : taskSnapshot.getResult()) {
                    Log.d("DOCUMENT", document.getData().get("name").toString());
                    // Delete the document from Firestore
                    dbTasks.document(document.getId()).delete()
                            .addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    // Remove from local list and notify adapter
                                    emplist.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, emplist.size());
                                } else {
                                    // Handle delete failure
                                }
                            });
                    Timestamp strDate = (Timestamp) document.getData().get("dueDate");
                    Date date = strDate.toDate();


                    if (dueDate.before(endingDay) && dueDate.after(startingDay)) {
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
                                                        // Retrieve the field value
                                                        Object allTasks = document.get("all tasks");
                                                        Object tasksCompleted = document.get("tasks completed");
                                                        Map<String, Object> data = new HashMap<>();
                                                        data.put("all tasks", allTasks);
                                                        int added = Integer.parseInt(tasksCompleted.toString()) + 1;
                                                        if (added == Integer.parseInt(allTasks.toString())) {
                                                            data.put("all tasks", 0);
                                                            data.put("tasks completed", 0);
                                                        }
                                                        else {
                                                            data.put("tasks completed", added);
                                                        }
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
                    }

                }
            } else {
                // Handle query failure
            }
        });

    }
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(int position, Task model);
    }


    public static class TasksViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView desc;
        private TextView dueDate;

        private TextView urgency;

        private CardView cardView;

        private CheckBox checkBox;

        public TasksViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewTaskName);
            desc = itemView.findViewById(R.id.textViewDescription);
            dueDate = itemView.findViewById(R.id.textViewDueDate);
            urgency = itemView.findViewById(R.id.textViewUrgency);
            cardView = itemView.findViewById(R.id.card_view);
            checkBox = itemView.findViewById(R.id.checkBoxCompleted);
        }

    }
}

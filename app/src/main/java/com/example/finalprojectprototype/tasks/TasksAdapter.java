package com.example.finalprojectprototype.tasks;

import android.content.Context;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectprototype.R;
import com.google.firebase.firestore.LoadBundleTaskProgress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TasksViewHolder> {
    private ArrayList<Task> emplist;
    private OnClickListener onClickListener;
    private Context context;
    public TasksAdapter(ArrayList<Task> emplist) {
        this.emplist = emplist;
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

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        boolean isToday = date != null && date.before(currentDate);

        if (isToday) {
            Log.d("HELLO", "");
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.light_purple));
            holder.name.setTextColor(Color.BLACK);
            holder.desc.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.dueDate.setTextColor(context.getResources().getColor(android.R.color.black));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener != null) {
                    onClickListener.onClick(position, currentEmp);
                }
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

        private CardView cardView;

        public TasksViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewTaskName);
            desc = itemView.findViewById(R.id.textViewDescription);
            dueDate = itemView.findViewById(R.id.textViewDueDate);
            cardView = itemView.findViewById(R.id.card_view);
        }

    }
}

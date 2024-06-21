package com.example.finalprojectprototype.calendar;

import android.content.Context;
import android.icu.util.Calendar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectprototype.R;
import com.example.finalprojectprototype.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private ArrayList<Task> emplist;
    private CalendarAdapter.OnClickListener onClickListener;
    private Context context;
    public CalendarAdapter(ArrayList<Task> emplist) {
        this.emplist = emplist;
    }

    @Override
    public CalendarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_card, parent, false);
        return new CalendarViewHolder(itemView);
    }


    @Override
    public int getItemCount() {
        return emplist.size();
    }

    @Override
    public void onBindViewHolder(CalendarViewHolder holder, int position) {
        Task currentEmp = emplist.get(position);
        holder.name.setText(currentEmp.getName());
        Date date = currentEmp.getDueDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(date);
        holder.dueDate.setText(formattedDate);

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

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


    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView dueDate;

        public CalendarViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.calendarName);
            dueDate = itemView.findViewById(R.id.calendarDueDate);
        }

    }
}

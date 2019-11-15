package com.khusainov.rinat.audiorecorder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khusainov.rinat.audiorecorder.model.Record;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecorderHodler> {

    private List<Record> mRecords;

    public RecordAdapter(List<Record> records) {
        mRecords = records;
    }

    @NonNull
    @Override
    public RecorderHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_item, parent, false);
        return new RecorderHodler(root);
    }

    @Override
    public void onBindViewHolder(@NonNull RecorderHodler holder, int position) {
        Record record = mRecords.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    static class RecorderHodler extends RecyclerView.ViewHolder {

        private TextView mNameTextView;
        private TextView mLengthTextView;
        private TextView mDateTextView;

        public RecorderHodler(@NonNull View itemView) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.tv_name);
            mLengthTextView = itemView.findViewById(R.id.tv_length);
            mDateTextView = itemView.findViewById(R.id.tv_date);
        }

        void bind(Record record) {
            mNameTextView.setText(record.getName());
            mLengthTextView.setText(String.valueOf(record.getLength()));
            mDateTextView.setText(record.getDate().toString());
        }
    }
}

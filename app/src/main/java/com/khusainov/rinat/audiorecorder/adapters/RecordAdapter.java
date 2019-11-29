package com.khusainov.rinat.audiorecorder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khusainov.rinat.audiorecorder.ui.OnItemClickListener;
import com.khusainov.rinat.audiorecorder.R;

import java.io.File;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecorderHodler> {

    private List<File> mRecords;
    private OnItemClickListener mOnItemClickListener;

    public RecordAdapter(List<File> records, OnItemClickListener onItemClickListener) {
        mRecords = records;
        mOnItemClickListener = onItemClickListener;
    }

    public void addData(List<File> files) {
        mRecords.clear();
        mRecords.addAll(files);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecorderHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.li_item, parent, false);
        return new RecorderHodler(root);
    }

    @Override
    public void onBindViewHolder(@NonNull RecorderHodler holder, int position) {
        File record = mRecords.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    class RecorderHodler extends RecyclerView.ViewHolder {

        private TextView mNameTextView;

        public RecorderHodler(@NonNull View itemView) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.tv_name);
        }

        void bind(final File file) {
            mNameTextView.setText(file.getName());

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnItemClickListener.onClick(getAdapterPosition());
                }
            });
        }
    }
}

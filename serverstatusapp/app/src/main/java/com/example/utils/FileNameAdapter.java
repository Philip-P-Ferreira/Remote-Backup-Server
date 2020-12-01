package com.example.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.serverstatusapp.R;

import java.util.HashMap;

import static com.example.serverstatusapp.DashBoardActivity.getStorageResponse;
import static com.example.serverstatusapp.DashBoardActivity.refresh;
import static com.example.utils.ServerProtocol.FILENAME_KEY;
import static com.example.utils.ServerProtocol.REMOVE_FILE_VAL;


public class FileNameAdapter extends ListAdapter<String, FileNameAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView fileNameView;
        public Button deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            fileNameView = itemView.findViewById(R.id.fileNameView);
            deleteButton = itemView.findViewById(R.id.deleteFileButton);
            deleteButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            HashMap<String,String> reqMap = new HashMap<>();
            reqMap.put(FILENAME_KEY, fileNameView.getText().toString());

            getStorageResponse(REMOVE_FILE_VAL, reqMap);
            refresh();
        }
    }

    public FileNameAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View fileListView = inflater.inflate(R.layout.file_cell_layout, parent, false);

        return new ViewHolder(fileListView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String fileName = getItem(position);

        TextView textView = holder.fileNameView;
        textView.setText(fileName);

    }

    public static final DiffUtil.ItemCallback<String> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<String>() {
                @Override
                public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return areItemsTheSame(oldItem, newItem);
                }
            };
}

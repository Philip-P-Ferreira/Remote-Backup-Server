package com.example.serverstatusapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.utils.ServerProtocol.*;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.utils.FileNameAdapter;
import com.example.utils.NetworkThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class DashBoardActivity extends AppCompatActivity {

    static TextView storageTxt;
    static FileNameAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        // set member vars
        storageTxt = findViewById(R.id.storageUsageText);
        RecyclerView fileListView = findViewById(R.id.fileListView);

        adapter = new FileNameAdapter();
        fileListView.setAdapter(adapter);
        fileListView.setLayoutManager(new LinearLayoutManager(this));

        refresh();
    }

    public static String getStorageResponse(String reqType, HashMap<String,String> reqMap) {

        NetworkThread netThread = new NetworkThread(reqType, reqMap);
        Thread storageResThread = new Thread(netThread);
        storageResThread.start();
        try {
            storageResThread.join();
        } catch (InterruptedException ignored) {
        }

        return netThread.getResponse();
    }

    public static String getStorageResponse(String reqType) {
        return getStorageResponse(reqType, new HashMap<String, String>());
    }

    public static void getFileList() {

        HashMap<String,String> resMap = createProtocolMap(getStorageResponse(LIST_FILES_VAL));
        ArrayList<String> returnList = new ArrayList<>();

        String filesStr = resMap.get(FILE_LIST_KEY);
        if (filesStr != null) {
            returnList.addAll(Arrays.asList(filesStr.split(FILE_NAME_DELIM)));
        }
        adapter.submitList(returnList);
    }

    public static void getStats() {
        HashMap<String,String> resMap = createProtocolMap(getStorageResponse(GET_STATS_VAL));
        HashMap<String,String> statsMap;

        String filesStr = resMap.get(STATS_KEY);
        if (filesStr != null) {
            statsMap = createProtocolMap(resMap.get(STATS_KEY), STATS_PAIR_DELIM, STATS_PAIR_SEPARATOR);

            long free = Long.parseLong(statsMap.get(STATS_FREE_SPACE_KEY));
            long total = Long.parseLong(statsMap.get(STATS_MAX_CAPACITY_KEY));
            Date lastwrite = new Date(Long.parseLong(statsMap.get(STATS_LAST_WRITE_KEY)));

            int percentUsed = 1 - (int)(free/total);

            String formatByte = formatByteSize(total);
            storageTxt.setText(percentUsed + "% used (of " + formatByte + ")\nLast modified: " + lastwrite.toString());
        }

    }

    public static void refresh() {
        getFileList();
        getStats();
    }

    public void onRefreshClick(View view) {
        refresh();
    }

    private static String formatByteSize(long byteSize)
    {
        int BYTE_CONVERSION_NUM = 1000;
        String[] BYTE_SUFFIX_ARR = {"B", "kB", "MB", "GB", "TB"};

        int suffixCount = 0;
        while (byteSize > BYTE_CONVERSION_NUM)
        {
            ++suffixCount;
            byteSize /= BYTE_CONVERSION_NUM;
        }

        return Long.toString(byteSize) + ' ' + BYTE_SUFFIX_ARR[suffixCount];
    }
}
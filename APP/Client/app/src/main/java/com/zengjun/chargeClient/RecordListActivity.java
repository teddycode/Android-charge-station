package com.zengjun.chargeClient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class RecordListActivity extends AppCompatActivity {

    private TextView tvTitle;
    private Button btn_ok;
    private ArrayAdapter<String> adapter;
    private ListView list;
    private Intent intent;
    private Bundle bundle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);

        // 获取intent数据
        intent = getIntent();
        String title = intent.getStringExtra("title");
        bundle = intent.getExtras();
        String[] str = bundle.getStringArray("records");
        InitUI(title,str);
    }

    public void InitUI(String title,String[] record){

        list = findViewById(R.id.list_records);
        adapter = new ArrayAdapter<>(this, R.layout.record_detail);

        setTitle(title);
        list.setAdapter(adapter);
        for(String str:record){
            if(str != null){
                adapter.add(str);
            }
        }

        btn_ok = findViewById(R.id.btn_rec_cancel);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}

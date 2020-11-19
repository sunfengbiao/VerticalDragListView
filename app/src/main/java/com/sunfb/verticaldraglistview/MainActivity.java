package com.sunfb.verticaldraglistview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rcv_list;
    private MyRecycleAdapter adapter ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }
    private void initView(){
        rcv_list =findViewById(R.id.rcv_list);
        LinearLayoutManager linearLayoutManager =new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        adapter =new MyRecycleAdapter(this,initData());
        rcv_list.setLayoutManager(linearLayoutManager);
        rcv_list.setAdapter(adapter);

    }
    private List<String> initData(){
        List<String> data =new ArrayList<>();
        for(int i =0 ;i<200;i++){
            data.add("i -->"+i);
        }
        return data;
    }
}
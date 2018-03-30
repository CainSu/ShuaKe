package com.example.lyh_adt.shuake;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Map;


/**
 * Created by 10439 on 2018/3/22/0022.
 */

public class ShuaActivity extends AppCompatActivity implements View.OnClickListener {
    private int selectedCourse = -1;
    private String username;
    private TextView tv_usrname;
    private ListView listview;
    private ChaoXing chaoXing = new ChaoXing();
    private CourseListAdapter mAdapter = null;
    private LinkedList<String> courseList = null;
    private LinkedList<String> courseLinks = null;
    private Button btn_start;
    private Button btn_stop;
    private Intent it1;
    private String cookies;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shua);



        //Intent
        Intent it = getIntent();
        Bundle bd = it.getExtras();

        username = bd.getCharSequence("username").toString();
        cookies =bd.getCharSequence("cookies").toString();

        Map<String,LinkedList<String>> map = chaoXing.getCourseList(cookies);
        courseList = map.get("courseList");
        courseLinks = map.get("courseLinks");
        Log.i("ADT","courseList"+courseList.toString());
        bindViews();

        tv_usrname.setText(username);
        mAdapter = new CourseListAdapter((LinkedList<String>)courseList,ShuaActivity.this);
        listview.setAdapter(mAdapter);


    }

    private void bindViews(){
        tv_usrname = (TextView)findViewById(R.id.tv_usrname);
        listview = (ListView)findViewById(R.id.listview);
        btn_start = (Button)findViewById(R.id.btn_start);
        btn_stop = (Button)findViewById(R.id.btn_stop);

        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent,View view,int position,long id){
                mAdapter.select(position);
                selectedCourse = position;
            }
        });
    }

    @Override
    public void onClick(View v){
        Log.i("ADT","onClick");
        switch (v.getId()){
            case R.id.btn_start:
                Log.i("ADT","btnstart");
                it1 = new Intent(ShuaActivity.this,ChaoXing.class);

                Bundle b1 = new Bundle();
                b1.putChar("param",'s');
                b1.putInt("startindex",selectedCourse);
                b1.putString("myCookies",cookies);
                b1.putSerializable("courseLinks",courseLinks);
                it1.putExtras(b1);
                startService(it1);
                break;
            case R.id.btn_stop:
                it1 = new Intent(ShuaActivity.this,ChaoXing.class);
                //Bundle b2 = new Bundle();
                //b2.putChar("param",'t');
                //it1.putExtras(b2);
                startService(it1);

                stopService(it1);
                break;
        }
    }
}

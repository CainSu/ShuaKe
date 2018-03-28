package com.example.lyh_adt.shuake;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedList;

/**
 * Created by 10439 on 2018/3/24/0024.
 */

public class CourseListAdapter extends BaseAdapter {

    private LinkedList<String> courseList;
    private Context mContext;
    private int selecteditem = -1;

    public CourseListAdapter(){}

    public CourseListAdapter(LinkedList<String> courseList,Context mContext){
        this.courseList = courseList;
        this.mContext = mContext;
    }

    @Override
    public Object getItem(int position){
        return null;
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public int getCount(){
        return courseList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        TextView tcourse = null;
        if (convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_view,parent,false);
            tcourse =(TextView)convertView.findViewById(R.id.list_content);
            convertView.setTag(tcourse);
        }else {
            tcourse = (TextView) convertView.getTag();
        }
        tcourse.setText(courseList.get(position));

        if (position == selecteditem){
            tcourse.setBackgroundColor(Color.parseColor("#00FF00"));
        }else{
            tcourse.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
        return convertView;

    }

    public void set(LinkedList<String> tcourseList){
        courseList = tcourseList;
        notifyDataSetChanged();
    }

    public void select(int position){
        selecteditem = position;
        notifyDataSetChanged();
    }
}

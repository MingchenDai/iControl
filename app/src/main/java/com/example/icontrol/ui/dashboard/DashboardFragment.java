package com.example.icontrol.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.icontrol.R;

public class DashboardFragment extends Fragment {

    public Spinner dashboard_spinner_switch_status_1;
    public Spinner dashboard_spinner_switch_status_2;
    public Spinner dashboard_spinner_bar_status_1;
    public Spinner dashboard_spinner_bar_status_2;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView=inflater.inflate(R.layout.fragment_dashboard,container,false);
        dashboard_spinner_switch_status_1=rootView.findViewById(R.id.dashboard_spinner_switch_status_1);
        dashboard_spinner_switch_status_2=rootView.findViewById(R.id.dashboard_spinner_switch_status_2);
        dashboard_spinner_bar_status_1=rootView.findViewById(R.id.dashboard_spinner_bar_status_1);
        dashboard_spinner_bar_status_2=rootView.findViewById(R.id.dashboard_spinner_bar_status_2);
        sharedPreferences = requireContext().getSharedPreferences("config", Context.MODE_PRIVATE);
        editor=sharedPreferences.edit();
        dashboardSpinnerInit(dashboard_spinner_switch_status_1,"status_switch_1");
        dashboardSpinnerInit(dashboard_spinner_switch_status_2,"status_switch_2");
        dashboardSpinnerInit(dashboard_spinner_bar_status_1,"status_bar_1");
        dashboardSpinnerInit(dashboard_spinner_bar_status_2,"status_bar_2");
        dashboard_spinner_switch_status_1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem=parent.getItemAtPosition(position).toString();
                if(selectedItem.equals("开")){
                    editor.putInt("status_switch_1",0);
                }
                else if(selectedItem.equals("关")){
                    editor.putInt("status_switch_1",1);
                }
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        dashboard_spinner_switch_status_2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem=parent.getItemAtPosition(position).toString();
                if(selectedItem.equals("开")){
                    editor.putInt("status_switch_2",0);
                }
                else if(selectedItem.equals("关")){
                    editor.putInt("status_switch_2",1);
                }
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        dashboard_spinner_bar_status_1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem=parent.getItemAtPosition(position).toString();
                if(selectedItem.equals("开")){
                    editor.putInt("status_bar_1",0);
                }
                else if(selectedItem.equals("关")){
                    editor.putInt("status_bar_1",1);
                }
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        dashboard_spinner_bar_status_2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem=parent.getItemAtPosition(position).toString();
                if(selectedItem.equals("开")){
                    editor.putInt("status_bar_2",0);
                }
                else if(selectedItem.equals("关")){
                    editor.putInt("status_bar_2",1);
                }
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void dashboardSpinnerInit(Spinner spinner,String key){
        int currentItem=sharedPreferences.getInt(key,-1);
        if((currentItem==0)||(currentItem==1)){
            //spinner.setSelection(currentItem,true);
            spinner.post(() -> spinner.setSelection(currentItem));
        }
    }
}
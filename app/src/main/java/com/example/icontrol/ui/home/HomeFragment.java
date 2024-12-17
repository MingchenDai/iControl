package com.example.icontrol.ui.home;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.icontrol.R;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class HomeFragment extends Fragment {

    private ImageButton home_button_switch_1;
    private ImageButton home_button_switch_2;
    private ImageButton home_button_refresh;
    private TextView home_textview_switch_status_1;
    private TextView home_textview_switch_status_2;
    private TextView home_textview_bluetooth_status;
    private ImageView home_imageview_bluetooth_status;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final String[] PERMISSIONS={
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
    };
    private BluetoothSocket bluetoothSocket=null;
    private final BluetoothDevice bluetoothDevice=bluetoothAdapter.getRemoteDevice("00:24:07:00:41:18");//00:24:07:00:41:18
    private int bluetoothAvailability=0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView=inflater.inflate(R.layout.fragment_home,container,false);
        home_button_switch_1=rootView.findViewById(R.id.home_button_switch_1);
        home_button_switch_2=rootView.findViewById(R.id.home_button_switch_2);
        home_button_refresh= rootView.findViewById(R.id.home_button_refresh);
        home_textview_switch_status_1=rootView.findViewById(R.id.home_textview_switch_status_1);
        home_textview_switch_status_2=rootView.findViewById(R.id.home_textview_switch_status_2);
        home_textview_bluetooth_status=rootView.findViewById(R.id.home_bluetooth_status);
        home_imageview_bluetooth_status=rootView.findViewById(R.id.home_imageview_bluetooth_status);
        homeInit();
        home_button_switch_1.setOnClickListener(v->homeImageButtonAction(home_button_switch_1,home_textview_switch_status_1,1));
        home_button_switch_2.setOnClickListener(v->homeImageButtonAction(home_button_switch_2,home_textview_switch_status_2,2));
        home_button_refresh.setOnClickListener(v->homeReset());

        return rootView;
    }

    private void homeInit(){
        homeBluetoothInit();
        if(bluetoothAvailability>0){
            homeBluetoothConnect();
        }
        homeImageButtonInit(home_button_switch_1,home_textview_switch_status_1,"status_switch_1");
        homeImageButtonInit(home_button_switch_2,home_textview_switch_status_2,"status_switch_2");
    }

    private void homeReset(){
        bluetoothAvailability=0;
        try{
            bluetoothSocket=null;
        }
        catch(Exception e){
            Log.e(TAG,"homeReset",e);
        }
        homeInit();
    }

    private void homeSharedPreferencesInit(){
        sharedPreferences=requireContext().getSharedPreferences("config", Context.MODE_PRIVATE);
        editor=sharedPreferences.edit();
    }

    private void homeSharedPreferencesDelete(){
        editor.commit();
        sharedPreferences=null;
    }

    private void homeImageButtonAction(ImageButton imageButton,TextView textView,int signal){
        if(bluetoothAvailability!=2){
            home_textview_bluetooth_status.setText(R.string.home_bluetooth_send_unavailable);
        }
        else{
            String switch_key="status_switch_"+signal;
            String bar_key="status_bar_"+signal;
            homeSharedPreferencesInit();
            signal+=(sharedPreferences.getInt(switch_key,-1)+sharedPreferences.getInt(bar_key,-1))%2*2;
            boolean flag=homeBluetoothSend(signal);
            if(flag){
                int status = 1-sharedPreferences.getInt(switch_key, -1);
                editor.putInt(switch_key,status);
                homeSharedPreferencesDelete();
                homeImageButtonInit(imageButton,textView,switch_key);
            }
            else{
                home_textview_bluetooth_status.setText(R.string.home_bluetooth_send_fail);
            }
        }
    }

    private void homeImageButtonInit(ImageButton imageButton,TextView textView,String key){
        homeSharedPreferencesInit();
        int status=sharedPreferences.getInt(key,-1);
        if(status==1){
            textView.setTextColor(ContextCompat.getColor(requireContext(),R.color.switch_inactivated));
            textView.setText(R.string.home_switch_status_off);
            imageButton.setImageResource(R.drawable.switch_inactivated);
        }
        else if(status==0){
            textView.setTextColor(ContextCompat.getColor(requireContext(),R.color.switch_activated));
            textView.setText(R.string.home_switch_status_on);
            imageButton.setImageResource(R.drawable.switch_activated);
        }
        else{
            textView.setTextColor(ContextCompat.getColor(requireContext(),R.color.switch_unavailable));
            textView.setText(R.string.home_switch_status_unavailable);
            imageButton.setImageResource(R.drawable.switch_unavailable);
        }
        homeSharedPreferencesDelete();
    }

    private void homeRefreshUI(int imageResource,int text){
        Handler handler=new Handler(Looper.getMainLooper());
        handler.post(()->{
            home_imageview_bluetooth_status.setImageResource(imageResource);
            home_textview_bluetooth_status.setText(text);
        });
    }

    private void homeBluetoothInit(){
        homeRefreshUI(R.drawable.bluetooth_ready,R.string.home_bluetooth_status_initializing);
        if(bluetoothAdapter==null) {
            homeRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_unsupported);
            bluetoothAvailability=-1;
            return;
        }
        if((Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)&&(!homeBluetoothAuthorized())){
            requestMultiplePermissionsLauncher.launch(PERMISSIONS);
        }
        homeBluetoothEnable();
    }

    private final ActivityResultLauncher<String[]>
    requestMultiplePermissionsLauncher=registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            isGranted->{
                if(isGranted.containsValue(false)){
                    bluetoothAvailability=-2;
                    homeRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_unauthorized);
                }
                else{
                    homeBluetoothEnable();
                }
            }
    );

    private void homeBluetoothEnable(){
        if(bluetoothAdapter.isEnabled()){
            homeRefreshUI(R.drawable.bluetooth_ready,R.string.home_bluetooth_status_ready);
            bluetoothAvailability=1;
        }
        else{
            homeRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_disabled);
            bluetoothAvailability=-3;
        }
    }

    private boolean homeBluetoothAuthorized(){
        boolean scanPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED;
        boolean connectPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED;
        boolean advertisePermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_ADVERTISE
        ) == PackageManager.PERMISSION_GRANTED;
        boolean adminPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED;
        boolean Permission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED;
        return (scanPermission)&&(connectPermission)&&(advertisePermission)&&(adminPermission)&&(Permission);
    }

    private boolean homeBluetoothConnected(BluetoothAdapter bluetoothAdapter){
        try{
            Method method=bluetoothAdapter.getClass().getDeclaredMethod("getConnectionState",(Class[])null);
            int state=(int)method.invoke(bluetoothAdapter,(Object[])null);
            if(state==BluetoothAdapter.STATE_CONNECTED){
                return true;
            }
        }
        catch(NoSuchMethodException|IllegalAccessException|InvocationTargetException e){
            Log.e(TAG,"homeBluetoothConnected",e);
        }
        return false;
    }

    private void homeBluetoothConnect(){
        homeRefreshUI(R.drawable.bluetooth_searching,R.string.home_bluetooth_status_connecting);
        if(ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED){
            homeRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_unauthorized);
        }
        else{
            ConnectThread connectThread=new ConnectThread(bluetoothSocket,true);
            connectThread.start();
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket bluetoothSocket;
        private boolean activateConnect;
        private ConnectThread(BluetoothSocket bluetoothSocket,boolean connect){
            this.bluetoothSocket=bluetoothSocket;
            this.activateConnect=connect;
        }

        @Override
        public void run(){
            if(activateConnect){
                try{
                    bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                }
                catch(Exception e){
                    Log.e(TAG,"ConnectThread",e);
                }
                try{
                    Thread.sleep(500);
                }
                catch(InterruptedException e){
                    Log.e(TAG,"ConnectThread",e);
                }
                new Thread(){
                    @Override
                    public void run(){
                        try{
                            bluetoothSocket.connect();
                            homeRefreshUI(R.drawable.bluetooth_connected,R.string.home_bluetooth_connect_success);
                            bluetoothAvailability=2;
                        }
                        catch(IOException e){
                            homeRefreshUI(R.drawable.bluetooth_unconnected,R.string.home_bluetooth_connect_fail);
                            bluetoothAvailability=1;
                            Log.e(TAG,"ConnectThread",e);
                        }
                    }
                }.start();
            }
        }
    }

    private boolean homeBluetoothSend(int signal){
        if(ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            homeRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_unauthorized);
            return false;
        }
        else{
            try {
                bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                OutputStream outputStream=bluetoothSocket.getOutputStream();
                bluetoothSocket.getOutputStream().write(signal);
            }
            catch(IOException e){
                Log.e(TAG,"homeBluetoothSend",e);
                return false;
            }
            return true;
        }
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
    }
}
package com.example.icontrol.ui.home;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.icontrol.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private int bluetoothAvailability=0;
    private boolean hasInitialized=false;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView=inflater.inflate(R.layout.fragment_home,container,false);

        home_button_switch_1=rootView.findViewById(R.id.home_button_switch_1);
        home_button_switch_2=rootView.findViewById(R.id.home_button_switch_2);
        home_button_refresh=rootView.findViewById(R.id.home_button_refresh);
        home_textview_switch_status_1=rootView.findViewById(R.id.home_textview_switch_status_1);
        home_textview_switch_status_2=rootView.findViewById(R.id.home_textview_switch_status_2);
        home_textview_bluetooth_status=rootView.findViewById(R.id.home_bluetooth_status);
        home_imageview_bluetooth_status=rootView.findViewById(R.id.home_imageview_bluetooth_status);
        init(hasInitialized);
        home_button_switch_1.setOnClickListener(v -> homeButtonAction(home_button_switch_1,home_textview_switch_status_1,1));
        home_button_switch_2.setOnClickListener(v -> homeButtonAction(home_button_switch_2,home_textview_switch_status_2,2));
        home_button_refresh.setOnClickListener(v -> init(false));
        return rootView;
    }

    private void init(boolean judge){
        if(judge&&(bluetoothAvailability==2)){
            homeBluetoothRefreshUI(R.drawable.bluetooth_connected,R.string.home_bluetooth_status_ready);
        }
        else{
            homeBluetoothInit();
            if(bluetoothAvailability>0){
                homeBluetoothConnect();
            }
        }
        homeButtonInit(home_button_switch_1,home_textview_switch_status_1,"status_switch_1");
        homeButtonInit(home_button_switch_2,home_textview_switch_status_2,"status_switch_2");
        hasInitialized=true;
    }

    public void homeButtonAction(ImageButton imageButton,TextView textView,int signal){
        if(bluetoothAvailability==2){
            String switch_key="switch_status_"+signal;
            String bar_key="bar_status_"+signal;
            homeSharedPreferencesInit();
            signal+=(sharedPreferences.getInt(switch_key,-1)+sharedPreferences.getInt(bar_key,-1))%2*2;
            boolean flag=homeBluetoothSend(String.valueOf(signal));
            if(flag){
                int status = 1-sharedPreferences.getInt(switch_key, -1);
                editor.putInt(switch_key,status);
                editor.commit();
                homeButtonChange(imageButton,textView,status);
            }
            else{
                home_textview_bluetooth_status.setText(R.string.home_bluetooth_send_fail);
            }
        }
        else{
            home_textview_bluetooth_status.setText(R.string.home_bluetooth_send_unavailable);
        }
    }

    private void homeButtonInit(ImageButton imageButton,TextView textView,String key){
        homeSharedPreferencesInit();
        int status = sharedPreferences.getInt(key, -1);
        homeButtonChange(imageButton,textView,status);
    }

    private void homeButtonChange(ImageButton imageButton,TextView textView,int status){
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
    }

    private void homeBluetoothInit(){
        home_textview_bluetooth_status.setText(R.string.home_bluetooth_status_initializing);
        if(bluetoothAdapter==null){
            bluetoothAvailability=-1;
            homeBluetoothRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_unsupported);
            return;
        }
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) {
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
            if((!scanPermission)||(!advertisePermission)||(!connectPermission)||(!adminPermission)||(!Permission)){
                requestMultiplePermissionsLauncher.launch(PERMISSIONS);
            }
        }
        homeBluetoothEnable();
    }

    private final ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher=registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            isGranted ->{
                if(isGranted.containsValue(false)){
                    bluetoothAvailability=-2;
                    homeBluetoothRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_unauthorized);
                }
                else{
                    homeBluetoothEnable();
                }
            }
    );

    private void homeBluetoothEnable(){
        if(bluetoothAdapter.isEnabled()){
            homeBluetoothRefreshUI(R.drawable.bluetooth_ready,R.string.home_bluetooth_status_ready);
            bluetoothAvailability=1;
        }
        else{
            homeBluetoothRefreshUI(R.drawable.bluetooth_unavailable,R.string.home_bluetooth_status_init_fail_disabled);
            bluetoothAvailability=-3;
        }
    }

    private void homeSharedPreferencesInit(){
         sharedPreferences=requireContext().getSharedPreferences("config", Context.MODE_PRIVATE);
         editor=sharedPreferences.edit();
    }

    private void homeBluetoothConnect(){
        homeBluetoothRefreshUI(R.drawable.bluetooth_searching,R.string.home_bluetooth_status_connecting);
        bluetoothAdapter.startDiscovery();
        try{
            BluetoothSocket bluetoothSocket=bluetoothAdapter.getRemoteDevice("00:24:07:00:41:18").createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            bluetoothSocket.connect();
            bluetoothAvailability=2;
            homeBluetoothRefreshUI(R.drawable.bluetooth_connected,R.string.home_bluetooth_status_ready);
        }
        catch(IOException e){
            homeBluetoothRefreshUI(R.drawable.bluetooth_ready,R.string.home_bluetooth_connect_fail);
        }
    }

    private void homeBluetoothRefreshUI(int imageResource,int text){
        Handler handler=new Handler(Looper.getMainLooper());
        handler.post(() -> {
            home_imageview_bluetooth_status.setImageResource(imageResource);
            home_textview_bluetooth_status.setText(text);
        });
    }

    public boolean homeBluetoothSend(String signal){
        try{
            BluetoothSocket bluetoothSocket=bluetoothAdapter.getRemoteDevice("00:24:07:00:41:18").createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            OutputStream outputStream=bluetoothSocket.getOutputStream();
            outputStream.write(signal.getBytes());
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
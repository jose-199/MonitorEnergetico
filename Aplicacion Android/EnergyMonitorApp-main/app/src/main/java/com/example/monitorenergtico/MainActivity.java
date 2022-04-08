package com.example.monitorenergtico;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements Listener{

    FragmentTransaction transaction;
    Fragment fragmentLogin, fragmentNewAccout,fragmentOptions,fragmentConfigureDeviceBt,
             fragmentViewDevices,fragmentViewEnergyCons, fragmentConsumptionHistory;
    FragmentContainerView fragmentContainerMqttStatus;
    ImageView imgError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.

        fragmentLogin = new Login();
        fragmentNewAccout = new NewUser();
        fragmentOptions = new Options();
        fragmentConfigureDeviceBt = new ConfigureDeviceBt();
        fragmentViewDevices = new ViewDevices();
        fragmentViewEnergyCons = new ViewEnergyConsumption();
        fragmentConsumptionHistory = new ConsumptionHistory();
        fragmentContainerMqttStatus = findViewById(R.id.fragmentContainerMqttStatus);
        imgError = findViewById(R.id.imageViewError);
    }

    @Override
    public void setConfDevice() {
        fragmentContainerMqttStatus.setVisibility(View.GONE);
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer,fragmentConfigureDeviceBt);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void setViewDevices() {
        fragmentContainerMqttStatus.setVisibility(View.GONE);
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer,fragmentViewDevices);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void setViewEnergyCons(String name, String topic) {
        Bundle data = new Bundle();
        data.putString("name",name);
        data.putString("topic",topic);
        fragmentViewEnergyCons.setArguments(data);
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer,fragmentViewEnergyCons);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void setConsHistory() {
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer,fragmentConsumptionHistory);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void setOptions() {
        fragmentContainerMqttStatus.setVisibility(View.GONE);
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer,fragmentOptions).commit();
    }

    @Override
    public void InfConn(boolean isConnected) {
        Bundle data = new Bundle();
        data.putString("MqttCon",String.valueOf(isConnected));
        fragmentOptions.setArguments(data);
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer,fragmentOptions);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
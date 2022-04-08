package com.example.monitorenergtico;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class ConfigureDeviceBt extends Fragment {

    Listener listener;

    ConstraintLayout constraintLayoutConnBt, constraintLayoutBtOk;
    ListView listViewPairedDevices;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String device = null, address = null;

    private ProgressDialog progress;

    EditText editTextSIID, editTextWPass, editTextRate, editTextNameDevi;
    RadioButton radioButtonBtnGTQ, radioButtonBtnUSD;
    Spinner SpiPayday;
    String payday="1";
    Switch switchAddDevi;
    Button btnConnW, btnSaveConf;
    ImageButton imgBtnGoBack;
    TextView textViewStatusWifi;
    ImageView imageViewStatusWifi;
    String msgBt="";
    String confMeasDevi[];

    public ConfigureDeviceBt() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_configure_device_bt,
                container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBtConnected) {
            isBtConnected = false;
            listViewPairedDevices.setAdapter(null);
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextSIID = view.findViewById(R.id.editTextSiid);
        editTextWPass = view.findViewById(R.id.editTextWPass);
        editTextRate = view.findViewById(R.id.editTextRate);
        editTextNameDevi = view.findViewById(R.id.editTextNameDevi);
        radioButtonBtnGTQ = view.findViewById(R.id.radioButtonGTQ);
        radioButtonBtnUSD = view.findViewById(R.id.radioButtonUSD);
        SpiPayday = view.findViewById(R.id.spinnerDevices);
        switchAddDevi = view.findViewById(R.id.switchAddDevi);
        btnConnW = view.findViewById(R.id.btnConnW);
        btnSaveConf = view.findViewById(R.id.btnSaveConf);
        imgBtnGoBack = view.findViewById(R.id.imgBtnGoBack);
        imageViewStatusWifi = view.findViewById(R.id.imageViewStatusWifi);
        textViewStatusWifi = view.findViewById(R.id.textViewStatusWifi);

        constraintLayoutConnBt = view.findViewById(R.id.ConstraintLayoutConnBt);
        constraintLayoutBtOk = view.findViewById(R.id.ConstraintLayoutBtOk);
        listViewPairedDevices = view.findViewById(R.id.listViewPairedDevi);

        //Show paired devices to connect
        constraintLayoutConnBt.setVisibility(View.VISIBLE);    //hide paired devices list
        constraintLayoutBtOk.setVisibility(View.GONE);  //show configuration layout

        //Fill payday spinner
        ArrayAdapter<CharSequence> adapterSpi = ArrayAdapter.createFromResource(getActivity(),
                R.array.days, R.layout.spinner_item);
        adapterSpi.setDropDownViewResource(R.layout.spinner_item);
        SpiPayday.setAdapter(adapterSpi);
        SpiPayday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                payday = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Send WiFi credentials button
        btnConnW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String siid = editTextSIID.getText().toString();
                String pass = editTextWPass.getText().toString();
                //ask if all fields are fill
                if (!siid.isEmpty() && !pass.isEmpty()){
                    //Send WiFi credentials throuth Bluetooth
                    writeBt("ssid:"+siid+'\n');
                    writeBt("pass:"+pass+'\n');
                    textViewStatusWifi.setText("Conectando...");
                    textViewStatusWifi.setTextColor(Color.parseColor("#FFFFFF"));
                    imageViewStatusWifi.setVisibility(View.GONE);
                    btnConnW.setClickable(false);
                    btnSaveConf.setClickable(false);
                }else{
                    Toast.makeText(getActivity(),"Debe llenar todos los campos",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Send configurations button
        btnSaveConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currency="";
                String rate = editTextRate.getText().toString();
                String name = editTextNameDevi.getText().toString();
                String timezone = TimeZone.getDefault().getID();
                if (radioButtonBtnGTQ.isChecked()==true){currency="GTQ";}
                else if(radioButtonBtnUSD.isChecked()==true){currency="USD";}
                //ask if all fields are fill
                if (!rate.isEmpty() && !currency.isEmpty() && !name.isEmpty()){
                    //Send all configurations throuth Bluetooth
                    writeBt("rate:"+rate+'\n');
                    writeBt("currency:"+currency+'\n');
                    writeBt("payday:"+payday+'\n');
                    writeBt("name:"+name+'\n');
                    writeBt("timezone:"+timezone+'\n');
                    //Save this device in app
                    if (switchAddDevi.isChecked()){
                        AddDevice(name+'\n'+device.trim()+'\n'+address);
                    }else{
                        ErraseDevice(name+'\n'+device.trim()+'\n'+address);
                    }
                }else{
                    Toast.makeText(getActivity(),"Debe llenar todos los campos",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgBtnGoBack.setOnClickListener(v -> listener.setOptions());

        startBt();

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener){
            listener = (Listener) context;
        }
    }

    public void startBt(){
        ArrayList Devices = new ArrayList();
        ArrayList AddressDevices = new ArrayList();

        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(!myBluetooth.isEnabled())
        {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }

        pairedDevices = myBluetooth.getBondedDevices();

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {   //Show only measurement devices
                if (bt.getName().length()>=14) {
                    if (bt.getName().substring(0,11).equals("ENER-ESP32-")){
                        Devices.add("\n"+ bt.getName() + "\n"); //Save name devices
                        AddressDevices.add(bt.getAddress());      //Save MAC address devices
                    }
                }
            }
        }
        else
        {
            Toast.makeText(getActivity(),
                    "Primero debe emparejar el dispositivo a configurar",
                    Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, Devices);
        listViewPairedDevices.setAdapter(adapter);
        listViewPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                address = AddressDevices.get(i).toString();
                device = Devices.get(i).toString();
                new ConnectBT().execute(); //Call the class to connect
            }
        });
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            //show a progress dialog
            progress = ProgressDialog.show(getActivity(), "Conectando...",
                    "Espere mientras se establece la conexón");
        }

        //while the progress dialog is shown, the connection is done in background
        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    //get the mobile bluetooth device
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    //connects to the device's address and checks if it's available
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    //create a RFCOMM (SPP) connection
                    btSocket = dispositivo.createRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        //after the doInBackground, it checks if everything went fine
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getActivity(),
                        "¡Error! No se pudo establecer la conexión. Intente de nuevo",
                        Toast.LENGTH_LONG).show();

            }
            else
            {
                Toast.makeText(getActivity(), "Bluetooth conectado con éxito",
                        Toast.LENGTH_LONG).show();
                new listenerBt().execute();  //Start Bluetooth listener
                isBtConnected = true;
                constraintLayoutConnBt.setVisibility(View.GONE);  //hide paired devices list
                constraintLayoutBtOk.setVisibility(View.VISIBLE); //show configuration layout
                writeBt("?");  //Ask to device for internet's connection information
            }
            progress.dismiss();
        }
    }

    private class listenerBt extends AsyncTask<Void, String, String>{

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(Void... voids) {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int len;
            try {
                while (isBtConnected) {
                    len = btSocket.getInputStream().read(buffer);
                    byte[] data = Arrays.copyOf(buffer, len);
                    String readMessage = new String(data);
                    publishProgress(readMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            readBt(values[0]);
        }
    }

    private void writeBt(String inf) {            //Write bluetooth socket function
        try {
            btSocket.getOutputStream().write(inf.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readBt(String inf) {       //Read bluetooth socket function
        msgBt=msgBt+inf;
        if (msgBt.charAt(msgBt.length()-1) == '\n') {
            msgBt = msgBt.replaceAll("\n", "").trim();
            confMeasDevi = msgBt.split(":");
            if (msgBt.equals("I")) {                        //Bt device has Internet access
                textViewStatusWifi.setText("Con conexión a Internet");
                imageViewStatusWifi.setImageResource(R.drawable.green_point);
                textViewStatusWifi.setTextColor(Color.parseColor("#32CD32"));
                imageViewStatusWifi.setVisibility(View.VISIBLE);
                btnConnW.setClickable(true);
                btnSaveConf.setClickable(true);
            } else if (msgBt.equals("i")) {               //Bt device has not Internet access
                textViewStatusWifi.setText("Sin conexión a Internet");
                imageViewStatusWifi.setImageResource(R.drawable.red_point);
                textViewStatusWifi.setTextColor(Color.parseColor("#F13A37"));
                imageViewStatusWifi.setVisibility(View.VISIBLE);
                btnConnW.setClickable(true);
                btnSaveConf.setClickable(true);
            } else if (msgBt.equals("cred_wrg")) {
                Toast.makeText(getActivity(), "Nombre de red o contaseña incorrectos",
                        Toast.LENGTH_LONG).show();
                btnConnW.setClickable(true);
            } else if (msgBt.equals("ok")) {
                //configuration has been saved
                Toast.makeText(getActivity(), "Configuración guardada con éxito",
                        Toast.LENGTH_LONG).show();
                listener.setOptions();
            }else if (confMeasDevi.length>1) {
                if (confMeasDevi[0].equals("rate")) {
                    //Bt device has send its rat
                    editTextRate.setText(confMeasDevi[1]);
                } else if (confMeasDevi[0].equals("currency")) {
                    //Bt device has send its currency
                    if (confMeasDevi[1].equals("GTQ")) {
                        radioButtonBtnGTQ.setChecked(true);
                    } else if (confMeasDevi[1].equals("USD")) {
                        radioButtonBtnUSD.setChecked(true);
                    }
                } else if (confMeasDevi[0].equals("payday")) {
                    //Bt device has send its daypay
                    SpiPayday.setSelection(Integer.parseInt(confMeasDevi[1]) - 1);
                } else if (confMeasDevi[0].equals("name")) {
                    //Bt device has send its name
                    editTextNameDevi.setText(confMeasDevi[1]);
                }
            }
            msgBt="";
        }
    }

    private String AskDeviceSaved(SharedPreferences preferences, ArrayList arrayListDevices,
                                  String device, Boolean delete) {
        Set<String> set=null;
        try {
            set = preferences.getStringSet("data", null);
        }catch (Exception e){

        }
        if (set==null) {
            return "empty";
        } else {
            arrayListDevices = new ArrayList<String>(set);
            int i=0;
            for (Object inArrayDevice: arrayListDevices) {
                //compare only MAC Address device, last 17 charts
                String MACinArrayDevice = inArrayDevice.toString().substring(
                        inArrayDevice.toString().length() - 17);
                String MACinNewDevice = device.substring(device.length() - 17);
                if (MACinArrayDevice.equals(MACinNewDevice)) {
                    if (delete){
                        //if device it's going to be errased method returns its position
                        return String.valueOf(i);
                    }
                    //verify if name match
                    String nameList, name;
                    nameList = inArrayDevice.toString().split("\n")[0];
                    name = device.split("\n")[0];
                    if (name.equals(nameList)){
                        return "saved";
                    } else {                      // name has changed and need to be update
                        return "c"+String.valueOf(i);
                    }

                }
                i++;
            }
            return "not saved";
        }
    }
    private void AddDevice(String device){
        SharedPreferences preferences= getActivity().getSharedPreferences(
                "devices",Context.MODE_PRIVATE);
        ArrayList arrayListDevices = new ArrayList();
        Set<String> setHashSet = new HashSet<>();

        //Verify if device is already saved or there are any data saved in phone
        String status = AskDeviceSaved(preferences, arrayListDevices, device,false);

        if (!status.equals("saved")){               //save device if isn't still saved
            SharedPreferences.Editor editor = preferences.edit();
            //if there are any data store in phone then load data and write a new value
            if (status.equals("not saved") || status.charAt(0)=='c'){
                Set<String> setArray=null;
                setArray = preferences.getStringSet("data", null);
                arrayListDevices = new ArrayList<String>(setArray);
            }
            if (status.charAt(0)=='c'){
                int index = Integer.parseInt(status.substring(1));  //get index to update
                arrayListDevices.set(index,device);     //update data
            }else {
                arrayListDevices.add(device);          //write new value
            }
            setHashSet.addAll(arrayListDevices);
            editor.putStringSet("data", setHashSet);
            editor.apply();
        }
    }

    private void ErraseDevice(String device){
        SharedPreferences preferences= getActivity().getSharedPreferences(
                "devices",Context.MODE_PRIVATE);
        ArrayList arrayListDevices = new ArrayList();
        //Verify if device is already saved or there are any data saved in phone
        String status = AskDeviceSaved(preferences, arrayListDevices, device,true);

        if (!status.equals("empty") && !status.equals("not saved")) {
            Set<String> setHashSet = new HashSet<>();

            SharedPreferences.Editor editor = preferences.edit();
            Set<String> setArray=null;
            setArray = preferences.getStringSet("data", null);
            arrayListDevices = new ArrayList<String>(setArray);
            arrayListDevices.remove(Integer.parseInt(status));        //erase device

            setHashSet.addAll(arrayListDevices);
            editor.putStringSet("data", setHashSet);
            editor.apply();
        }
    }
}
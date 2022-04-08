package com.example.monitorenergtico;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ConsumptionHistory extends Fragment {

    DatePickerDialog datePickerDialog;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date dateIni, dateFinal;
    Button btnDateIni, btnDateFinal, btnConsult;
    RadioButton radioButtonDates, radioButtonDay;
    TextView textViewDateFinal, textViewHistEnergy, textViewHistCharge;
    Spinner spinnerDevices;

    Boolean isDateIni;
    String device, nameDevice;

    public ConsumptionHistory() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("history", this,
                new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                textViewHistEnergy.setText(result.getString("energy"));
                textViewHistCharge.setText(result.getString("charge"));
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_consumption_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnDateIni = view.findViewById(R.id.btnDateIni);
        btnDateFinal = view.findViewById(R.id.btnDateFinal);
        btnConsult = view.findViewById(R.id.btnConsult);
        radioButtonDates = view.findViewById(R.id.radioButtonDates);
        radioButtonDay = view.findViewById(R.id.radioButtonDay);
        textViewDateFinal = view.findViewById(R.id.textDateFinal);
        textViewHistEnergy = view.findViewById(R.id.textViewHistEnergy);
        textViewHistCharge = view.findViewById(R.id.textViewHistCharge);
        spinnerDevices = view.findViewById(R.id.spinnerDevices);
        initDatePicker();
        initSpinner();

        btnDateIni.setOnClickListener(view1 ->{
            isDateIni=true;
            datePickerDialog.show();
        });

        btnDateFinal.setOnClickListener(view1 ->{
            isDateIni=false;
            datePickerDialog.show();
        });

        radioButtonDates.setOnClickListener(view1 -> {
            btnDateFinal.setVisibility(View.VISIBLE);
            textViewDateFinal.setVisibility(View.VISIBLE);
        });

        radioButtonDay.setOnClickListener(view1 -> {
            btnDateFinal.setVisibility(View.GONE);
            textViewDateFinal.setVisibility(View.GONE);
        });

        btnConsult.setOnClickListener(view1 -> {

            if ((radioButtonDates.isChecked() && (dateIni==null || dateFinal==null))
                || (radioButtonDay.isChecked() && dateIni==null)) {
                Toast.makeText(getActivity(),"Debe llenar todos los campos",Toast.LENGTH_SHORT).show();
            } else{
                if (radioButtonDates.isChecked()){
                    if (dateIni.before(dateFinal)){
                        consult(false);
                    } else {
                        Toast.makeText(getActivity(), "La fecha inicial debe ser mayor a la final",
                                Toast.LENGTH_LONG).show();
                    }
                } else if (radioButtonDay.isChecked()){
                    consult(true);
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        dateIni=null;
        dateFinal=null;
        btnDateIni.setText("--/--/--");
        btnDateFinal.setText("--/--/--");
        radioButtonDates.setChecked(true);
        btnDateFinal.setVisibility(View.VISIBLE);
        textViewDateFinal.setVisibility(View.VISIBLE);
    }

    private void consult(boolean isDay){
        Bundle bundle = new Bundle();
        bundle.putString("device",device);
        bundle.putString("name",nameDevice);
        bundle.putString("dateIni",simpleDateFormat.format(dateIni));
        if (isDay){
            bundle.putString("type","req-data-day");
        }else{
            bundle.putString("type","req-data");
            bundle.putString("dateFinal",simpleDateFormat.format(dateFinal));
        }
        getParentFragmentManager().setFragmentResult("consult",bundle);
    }

    private void initSpinner() {
        //get devices
        SharedPreferences preferences= getActivity().getSharedPreferences("devices", Context.MODE_PRIVATE);
        ArrayList arrayListDevices = new ArrayList();
        Set<String> setArray=null;
        try {
            setArray = preferences.getStringSet("data", null);
            arrayListDevices = new ArrayList<String>(setArray);
        }catch (Exception e){

        }
        ArrayList arrayListShowDevi = new ArrayList();
        ArrayList arrayListDevi = new ArrayList();
        for(Object item:arrayListDevices){
            String[] nameDevi = item.toString().split("\n");
            arrayListShowDevi.add(nameDevi[0]);
            arrayListDevi.add(nameDevi[1]);
        }
        device = (String) arrayListDevi.get(0);
        nameDevice = (String) arrayListShowDevi.get(0);
        //Fill payday spinner
        ArrayAdapter<CharSequence> adapterSpi = new ArrayAdapter(getActivity(), R.layout.spinner_item,arrayListShowDevi);
        adapterSpi.setDropDownViewResource(R.layout.spinner_item);
        spinnerDevices.setAdapter(adapterSpi);
        spinnerDevices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                device = (String) arrayListDevi.get(i);
                nameDevice = (String) adapterView.getItemAtPosition(i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    private void initDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (datePicker, year, month, day) -> {
            String date = (day+"/"+(month+1)+"/"+year);
            if (isDateIni) { //Button Date Init was presed
                btnDateIni.setText(date);
                try {
                    dateIni = simpleDateFormat.parse(year+"-"+(month+1)+"-"+day);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else{ //Button Date Final was presed
                btnDateFinal.setText(date);
                try {
                    dateFinal = simpleDateFormat.parse(year+"-"+(month+1)+"-"+day);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int mounth = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int style = AlertDialog.THEME_HOLO_DARK;
        datePickerDialog = new DatePickerDialog(getActivity(),style,dateSetListener,year,mounth,day);
    }
}
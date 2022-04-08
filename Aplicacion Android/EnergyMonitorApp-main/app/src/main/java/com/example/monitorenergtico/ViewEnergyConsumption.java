package com.example.monitorenergtico;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewEnergyConsumption#newInstance} factory method to
 * create an instance of this fragment.
 */

public class ViewEnergyConsumption extends Fragment {

    TextView name;
    TextView textViewVoltage, textViewCurrent, textViewPower, textViewEnergy, textViewCharge;
    ProgressBar progressBarVoltage,progressBarCurrent,progressBarPower;
    LineChart mpLineChart;
    ArrayList<Entry> dataValues = new ArrayList<Entry>();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    private Thread MeasureThread;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "name";
    private static final String ARG_PARAM2 = "topic";

    // TODO: Rename and change types of parameters
    private String mName;
    private String mTopic;

    public ViewEnergyConsumption() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param name Parameter 1.
     * @param topic Parameter 2.
     * @return A new instance of fragment ViewEnergyConsumption.
     */
    // TODO: Rename and change types and number of parameters
    public static ViewEnergyConsumption newInstance(String name, String topic) {
        ViewEnergyConsumption fragment = new ViewEnergyConsumption();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, "name");
        args.putString(ARG_PARAM2, "topic");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mName = getArguments().getString(ARG_PARAM1);
            mTopic = getArguments().getString(ARG_PARAM2);
        }
        getParentFragmentManager().setFragmentResultListener("voltage",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String voltage = result.getString("voltage");
                double porcent = Double.parseDouble(voltage)/260*70;
                textViewVoltage.setText(voltage);
                progressBarVoltage.setProgress((int) porcent);
            }
        });
        getParentFragmentManager().setFragmentResultListener("current",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String current = result.getString("current");
                double porcent=1;
                if (Double.parseDouble(current)<=15){
                    porcent = Double.parseDouble(current)/15*70;
                } else if (Double.parseDouble(current)>15){
                    porcent = 70;
                }
                if (porcent>0 && porcent<1){
                    porcent = 1;
                }
                textViewCurrent.setText(current);
                progressBarCurrent.setProgress((int) porcent);
            }

        });
        getParentFragmentManager().setFragmentResultListener("power",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String power = result.getString("power");
                double porcent=1;
                if (Double.parseDouble(power)<=1800){
                    porcent = Double.parseDouble(power)/1800*70;
                } else if (Double.parseDouble(power)>1800){
                    porcent = 70;
                }
                if (porcent>0 && porcent<1){
                    porcent = 1;
                }
                textViewPower.setText(power);
                progressBarPower.setProgress((int) porcent);
            }
        });
        getParentFragmentManager().setFragmentResultListener("energy",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String energy = result.getString("energy");
                textViewEnergy.setText(energy + " KWh");
                Float energyf = Float.parseFloat(energy);
                addDataGraph(energyf);
            }
        });
        getParentFragmentManager().setFragmentResultListener("charge",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String charge = result.getString("charge");
                textViewCharge.setText(charge);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_energy_consumption, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        name = view.findViewById(R.id.textViewName);
        textViewVoltage = view.findViewById(R.id.textViewVolt);
        textViewCurrent = view.findViewById(R.id.textViewCurrent);
        textViewPower = view.findViewById(R.id.textViewPower);
        textViewEnergy = view.findViewById(R.id.textViewEnergy);
        textViewCharge = view.findViewById(R.id.textViewCharge);
        progressBarVoltage = view.findViewById(R.id.circular_pb_volt);
        progressBarCurrent = view.findViewById(R.id.circular_pb_curr);
        progressBarPower = view.findViewById(R.id.circular_pb_power);
        mpLineChart=(LineChart) view.findViewById(R.id.linechart);

        name.setText(mName);
        graphStart();

        MeasureThread = new NewThread();
        MeasureThread.setName("req measurements");
        MeasureThread.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        MeasureThread.interrupt();
        progressBarVoltage.setProgress(0);
        progressBarCurrent.setProgress(0);
        progressBarPower.setProgress(0);
        dataValues.clear();
        Bundle loginBundle = new Bundle();
        loginBundle.putString("unsuscribe","");
        getParentFragmentManager().setFragmentResult("unsuscribe",loginBundle);
    }

    public void graphStart(){
        //custom graph
        mpLineChart.setBackgroundColor(Color.parseColor("#00FFFFFF"));
        mpLineChart.setNoDataText("No Data");
        mpLineChart.setNoDataTextColor(Color.WHITE);
        mpLineChart.getAxisLeft().setDrawGridLines(false);
        mpLineChart.getAxisRight().setDrawGridLines(false);
        mpLineChart.getXAxis().setDrawGridLines(false);
        mpLineChart.getAxisLeft().setTextColor(Color.WHITE);
        mpLineChart.getAxisRight().setTextColor(Color.parseColor("#00FFFFFF"));
        mpLineChart.getXAxis().setTextColor(Color.WHITE);
        mpLineChart.getXAxis().setDrawAxisLine(false);
        mpLineChart.getAxisRight().setDrawAxisLine(false);
        mpLineChart.getAxisLeft().setDrawAxisLine(false);
        mpLineChart.getXAxis().setDrawLabels(true);
        mpLineChart.getLegend().setTextColor(Color.WHITE);
        mpLineChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        mpLineChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        mpLineChart.getLegend().setDrawInside(false);
        //graph description
        Description description = new Description();
        description.setText("");
        mpLineChart.setDescription(description);
        //graph axis formatter
        mpLineChart.getXAxis().setValueFormatter(new XAxisFormatter());
        mpLineChart.getAxisLeft().setValueFormatter(new YAxisFormatter());
        //mpLineChart.getAxisLeft().setStartAtZero(true);
    }

    public void addDataGraph(Float value){
        LineDataSet lineDataSet1 = new LineDataSet(dataValues,"Energy");
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);

        String NowTime = simpleDateFormat.format(new Date());
        NowTime = NowTime.replace(":","");
        dataValues.add(new Entry(Float.parseFloat(NowTime),value));

        LineData data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        //custom line
        lineDataSet1.setLineWidth(3);
        //lineDataSet1.setColor(Color.parseColor("#E91E61")); 7A0BC0
        lineDataSet1.setColor(Color.parseColor("#E91E61"));
        lineDataSet1.setDrawCircles(false);
        lineDataSet1.setDrawValues(false);
        lineDataSet1.setValueTextColor(Color.WHITE);

        LineData newdata = new LineData(dataSets);
        mpLineChart.setData(newdata);
        mpLineChart.invalidate();
    }

    class XAxisFormatter extends ValueFormatter {

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            axis.setLabelCount(2,true);
            String time="";
            if (Float.toString(value).charAt(5)=='.'){
                time = "0"+Float.toString(value).charAt(0)+":"+
                        Float.toString(value).charAt(1)+""+
                        Float.toString(value).charAt(2)+":"+
                        Float.toString(value).charAt(3)+""+
                        Float.toString(value).charAt(4);
            }else {
                time = Float.toString(value).charAt(0) + "" +
                        Float.toString(value).charAt(1) + ":" +
                        Float.toString(value).charAt(2) + "" +
                        Float.toString(value).charAt(3) + ":" +
                        Float.toString(value).charAt(4) + "" +
                        Float.toString(value).charAt(5);
            }
            return time;
        }
    }

    class YAxisFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            String resp="";
            if (value<= 0){
                resp = "";
            } else {
                resp = String.format("%.3f", value)+" KWh";
            }
            return resp;
        }
    }

    private class NewThread extends Thread {
        @Override
        public void run(){
            while(true) {
                //send info to get device's mqtt messages
                try {
                    Bundle bundle = new Bundle();
                    bundle.putString("topic",mTopic);
                    getParentFragmentManager().setFragmentResult("req_measur", bundle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
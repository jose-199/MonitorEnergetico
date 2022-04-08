package com.example.monitorenergtico;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Set;

public class ViewDevices extends Fragment {

    Listener listener;

    ListView listViewDevices;
    TextView textViewInfo;
    public ViewDevices() {
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
        return inflater.inflate(R.layout.fragment_view_devices, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewDevices = view.findViewById(R.id.listViewDevices);
        textViewInfo = view.findViewById(R.id.textViewInfo);

        textViewInfo.setText("Seleccione el dispositivo a visualizar:");

        SharedPreferences preferences= getActivity().getSharedPreferences(
                "devices", Context.MODE_PRIVATE);
        ArrayList arrayListDevices = new ArrayList();
        Set<String> setArray=null;
        try {
            setArray = preferences.getStringSet("data", null);
            arrayListDevices = new ArrayList<String>(setArray);
        }catch (Exception e){

        }
        if (arrayListDevices.isEmpty()){
            textViewInfo.setText("Para visualizar un dispositivo primero debe " +
                    "configurarlo y agregarlo a su visualizador.");
        }else{
            ArrayList arrayListShowDevi = new ArrayList();
            for(Object item:arrayListDevices){
                String[] nameDevi = item.toString().split("\n");
                arrayListShowDevi.add('\n'+nameDevi[0]+'\n'+nameDevi[1]+'\n');
            }
            arrayListShowDevi.add('\n'+"Consultar historial"+'\n');
            final ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_list_item_1, arrayListShowDevi);
            listViewDevices.setAdapter(adapter);

            listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (adapterView.getItemAtPosition(i).toString().trim().equals(
                            "Consultar historial")){
                        listener.setConsHistory();
                    }else {
                        String name = adapterView.getItemAtPosition(i).toString();
                        String topic = adapterView.getItemAtPosition(i).toString();
                        name = name.split("\n")[1];
                        topic = topic.split("\n")[2];
                        //send info to get device's mqtt messages
                        Bundle bundle = new Bundle();
                        bundle.putString("topic",topic);
                        getParentFragmentManager().setFragmentResult("topic",bundle);

                        listener.setViewEnergyCons(name,topic);
                    }
                }
            });
        }
    }
}
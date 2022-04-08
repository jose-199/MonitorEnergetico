package com.example.monitorenergtico;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MQTTservice extends Fragment {

    TextView status;
    SecureData secureData;
    MqttAndroidClient mqttClient;
    Listener listener;
    String deviceTopic=null,deviceTopicHis=null,dateHis=null;
    public MQTTservice() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("req_measur",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey,
                                         @NonNull Bundle bundle) {
                //save topic and subscribe
                String device = "energy/devices/"+bundle.getString("topic");
                try {
                    mqttClient.publish(device,"visualization".getBytes(),
                            0,false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener("topic",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey,
                                         @NonNull Bundle bundle) {
                //save topic and subscribe
                deviceTopic = "energy/devices/"+bundle.getString("topic");
                setSubscription(deviceTopic+"/+");
            }
        });

        getParentFragmentManager().setFragmentResultListener("unsuscribe",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey,
                                         @NonNull Bundle bundle) {
                if (!deviceTopic.equals(null)) {
                    deleteSubscription(deviceTopic + "/+");
                    deviceTopic = null;
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener("consult",
                this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey,
                                         @NonNull Bundle bundle) {
                deviceTopicHis = "energy/devices/" +bundle.getString("device");
                setSubscription(deviceTopicHis);
                String dateIni = bundle.getString("dateIni");
                String type = bundle.getString("type");
                String name = bundle.getString("name");
                String message="";
                if (type=="req-data-day") {
                    message = type+'\n'+name+'\n'+dateIni;
                    dateHis=dateIni;
                }else if(type=="req-data"){
                    String dateFinal = bundle.getString("dateFinal");
                    message = type+'\n'+name+'\n'+dateIni+'\n'+dateFinal;
                    dateHis=dateIni+"-"+dateFinal;
                }
                try {
                    mqttClient.publish(deviceTopicHis,message.getBytes(),
                            0,false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_m_q_t_tservice, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        secureData = new SecureData();
        status = view.findViewById(R.id.textStatus);

        connMqtt();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener){
            listener = (Listener) context;
        }
    }

    public void connMqtt(){

        String clientId = MqttClient.generateClientId();
        mqttClient = new MqttAndroidClient(getActivity().getApplicationContext(),
                                           getCredentialsMqtt()[0],
                                           clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(getCredentialsMqtt()[1]);
        options.setPassword(getCredentialsMqtt()[2].toCharArray());

        try {
            status.setText("Estableciendo conexion con el servidor...");
            IMqttToken token = mqttClient.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    status.setTextColor(Color.parseColor("#32CD32"));
                    status.setText("Conexion establecida");
                    listener.InfConn(true);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    listener.InfConn(false);
                    status.setTextColor(Color.parseColor("#F13A37"));
                    status.setText("Error, no se pudo establecer la conexión con el " +
                            "servidor, verifique su conexión a Internet. No se podra " +
                            "visualizar el consumo energético");
                    //Toast.makeText(getActivity(),"error",Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message)
                    throws Exception {
                //subText.setText(new String(message.getPayload()));
                handMessage(topic,new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void setSubscription(String topic){
        try{
            mqttClient.subscribe(topic,0);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    private void deleteSubscription(String topic){
        try{
            mqttClient.unsubscribe(topic);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void handMessage(String topic, String msg){
        String[] info = msg.split(String.valueOf('\n'));
        if(topic.equals(deviceTopic+"/voltage")){
            Bundle loginBundle = new Bundle();
            loginBundle.putString("voltage",info[0]);
            getParentFragmentManager().setFragmentResult("voltage",loginBundle);
        } else if (topic.equals(deviceTopic+"/current")){
            Bundle loginBundle = new Bundle();
            loginBundle.putString("current",info[0]);
            getParentFragmentManager().setFragmentResult("current",loginBundle);
        } else if (topic.equals(deviceTopic+"/power")){
            Bundle loginBundle = new Bundle();
            loginBundle.putString("power",info[0]);
            getParentFragmentManager().setFragmentResult("power",loginBundle);
        } else if (topic.equals(deviceTopic+"/energy")){
            Bundle loginBundle = new Bundle();
            loginBundle.putString("energy",info[0]);
            getParentFragmentManager().setFragmentResult("energy",loginBundle);
        } else if (topic.equals(deviceTopic+"/charge")){
            Bundle loginBundle = new Bundle();
            loginBundle.putString("charge",info[0]);
            getParentFragmentManager().setFragmentResult("charge",loginBundle);
        }
        if(topic.equals(deviceTopicHis) && info[0].equals("res-data") &&
                info[1].equals(dateHis)){
            dateHis=null;
            Bundle loginBundle = new Bundle();
            loginBundle.putString("energy",info[2]);
            loginBundle.putString("charge",info[3]);
            getParentFragmentManager().setFragmentResult("history",loginBundle);
        }
    }

    private String[] getCredentialsMqtt(){
        String[] encryCredentials;
        String[] desencryCredentials = {"","","",""};
        InputStream mqtt_credentials =
                this.getResources().openRawResource(R.raw.mqtt_credentials);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(mqtt_credentials));
        String line,info="";
        try {
            while ((line = reader.readLine()) != null) {
                info=info+line+'\n';
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        encryCredentials = info.split(String.valueOf('\n'));
        try {
            desencryCredentials[0]=
                    secureData.desencrypt(encryCredentials[0],encryCredentials[3]);
            desencryCredentials[1]=
                    secureData.desencrypt(encryCredentials[1],encryCredentials[3]);
            desencryCredentials[2]=
                    secureData.desencrypt(encryCredentials[2],encryCredentials[3]);
        }catch (Exception e){
            e.printStackTrace();
        }
        return desencryCredentials;
    }
}
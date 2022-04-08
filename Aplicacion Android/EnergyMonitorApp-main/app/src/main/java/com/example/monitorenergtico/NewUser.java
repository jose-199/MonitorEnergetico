package com.example.monitorenergtico;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.regex.Pattern;

public class NewUser extends Fragment {

    SecureData secureData;
    Listener listener;
    EditText EdiTextNewUser,EdiTextNewName,EdiTextNewPass,EdiTextNewRate;
    Button btnNewAcc;
    ImageButton ImgBtnGoBack;
    RadioButton radioButtonGTQ, radioButtonUSD;
    Spinner SpiDay;
    String payday="1", newuser;

    public NewUser() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("res_newuser", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String[] info = result.getStringArray("res");
                if (info[1].equals(newuser) && info[2].equals("ok")){
                    Toast.makeText(getActivity(),"Cuenta creada con éxito",Toast.LENGTH_LONG).show();

                    //Put the new user in EditText user from login
                    Bundle bundle = new Bundle();
                    bundle.putString("user",newuser);
                    getParentFragmentManager().setFragmentResult("setUser",bundle);

                    //Go to login fragment
                    //listener.setLogin();

                } else if (info[1].equals(newuser) && info[2].equals("exist")) {
                    Toast.makeText(getActivity(),"El usuario "+ newuser +" ya existe",Toast.LENGTH_LONG).show();
                    btnNewAcc.setEnabled(true);
                    EdiTextNewUser.setText("");
                } else if (info[1].equals(newuser) && info[2].equals("error")) {
                    Toast.makeText(getActivity(),"Se ha producido un error al crear la cuenta",Toast.LENGTH_SHORT).show();
                    btnNewAcc.setEnabled(true);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        secureData = new SecureData();

        EdiTextNewUser = view.findViewById(R.id.EditTextCreUsu);
        EdiTextNewName = view.findViewById(R.id.EditTextCreNom);
        EdiTextNewPass = view.findViewById(R.id.EditTextCreContra);
        EdiTextNewRate = view.findViewById(R.id.EditTextTarifa);
        radioButtonGTQ = view.findViewById(R.id.rBtnGTQ);
        radioButtonUSD = view.findViewById(R.id.rBtnUSD);
        SpiDay = view.findViewById(R.id.spinnerDay);
        ImgBtnGoBack = view.findViewById(R.id.imageButtonGoBack);
        btnNewAcc = view.findViewById(R.id.buttonCrearCuenta);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.days, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpiDay.setAdapter(adapter);
        SpiDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                payday = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImgBtnGoBack.setOnClickListener(v -> getActivity().onBackPressed());

        btnNewAcc.setOnClickListener(v -> ActBtnCreateNewAccount());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof Listener){
            listener = (Listener) context;
        }
    }

    public void ActBtnCreateNewAccount(){
        String user = EdiTextNewUser.getText().toString();
        String name = EdiTextNewName.getText().toString();
        String pass = secureData.get_SHA_512(EdiTextNewPass.getText().toString());
        String rate = EdiTextNewRate.getText().toString();
        String currency="";
        if (radioButtonGTQ.isChecked()==true){currency="GTQ";} else if(radioButtonUSD.isChecked()==true){currency="USD";}
        if (!user.isEmpty() && !name.isEmpty() && !rate.isEmpty() && !EdiTextNewPass.getText().toString().isEmpty() && (radioButtonGTQ.isChecked() || radioButtonUSD.isChecked())) {
            if (validEmail(user)){
                newuser = user;
                Bundle bundle = new Bundle();
                bundle.putString("user",user);
                bundle.putString("name",name);
                bundle.putString("pass",pass);
                bundle.putString("rate",rate);
                bundle.putString("currency",currency);
                bundle.putString("payday",payday);
                getParentFragmentManager().setFragmentResult("newuser",bundle);
                btnNewAcc.setEnabled(false);
            } else {
                Toast.makeText(getActivity(),"El usuario debe ser un correo electrónico valido",Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(),"Debe rellenar todos los campos",Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validEmail(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }

}
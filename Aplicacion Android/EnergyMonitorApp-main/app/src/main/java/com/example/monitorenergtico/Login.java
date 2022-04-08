package com.example.monitorenergtico;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

public class Login extends Fragment{

    SecureData secureData;
    Listener listener;
    EditText editTextLoginUser, editTextLoginPass;
    Button btnLogin, btnNewAccount;
    TextView textViewInf, textViewQuestion;

    String loginUser;
    public Login() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("res_login", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String[] info = result.getStringArray("res");
                if (info[1].equals(loginUser) && info[2].equals("ok")){
                    Toast.makeText(getActivity(),"Sesión iniciada con éxito",Toast.LENGTH_SHORT).show();
                    listener.setOptions();
                } else if (info[1].equals(loginUser) && info[2].equals("not")) {
                    Toast.makeText(getActivity(),"Usuario o contraseña incorrectos",Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    editTextLoginPass.setText("");
                    editTextLoginUser.setText("");
                } else if (info[1].equals(loginUser) && info[2].equals("error")) {
                    Toast.makeText(getActivity(),"Se ha producido un error al iniciar sesión",Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    editTextLoginPass.setText("");
                    editTextLoginUser.setText("");
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener("setUser", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String user = result.getString("user");
                textViewQuestion.setVisibility(View.GONE);
                btnNewAccount.setVisibility(View.GONE);
                editTextLoginUser.setText(user);
                textViewInf.setTextSize(20);
                textViewInf.setText("Cuenta creada con éxito, ahora debe iniciar sesión usando su contraseña");
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnLogin = view.findViewById(R.id.buttonIniSesion);
        btnNewAccount = view.findViewById(R.id.button_Ir_a_CreCue);
        editTextLoginUser = view.findViewById(R.id.EditTextUsu);
        editTextLoginPass = view.findViewById(R.id.EditTextContra);
        textViewInf = view.findViewById(R.id.textViewInf);
        textViewQuestion = view.findViewById(R.id.textViewQuestion);

        secureData = new SecureData();


        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = editTextLoginUser.getText().toString();
                String pass = secureData.get_SHA_512(editTextLoginPass.getText().toString());
                if (!user.isEmpty() && !editTextLoginPass.getText().toString().isEmpty()) {
                    if (validEmail(user)){
                        loginUser=user;
                        Bundle bundle = new Bundle();
                        bundle.putString("user",user);
                        bundle.putString("pass",pass);
                        getParentFragmentManager().setFragmentResult("login",bundle);
                        btnLogin.setEnabled(false);
                    } else {
                        Toast.makeText(getActivity(),"El usuario debe de ser un correo electronico valido",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(),"Debe llenar todos los campos",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //listener.setNewAccount();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener){
            listener = (Listener) context;
        }
    }

    private boolean validEmail(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }
}
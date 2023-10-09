package com.example.clientserverapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientPCActivity extends AppCompatActivity {

    String IPAddress, PortNumber;

    TextView connectionState;
    LinearLayout layoutInputMsg, layoutIPAddress;

    EditText IP_Address, Port_Number, Msg;
    ProgressBar progressBar;

    ListView message_list;
    List<String> allMessages;
    ArrayAdapter arrayAdapter;

    Button connectToServer;

    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser user = auth.getCurrentUser();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference refMessage = database.getReference("Message");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_pc);

        connectionState = findViewById(R.id.connectionState);
        layoutInputMsg = findViewById(R.id.layoutInputMsg);
        layoutIPAddress = findViewById(R.id.layoutIPAddress);

        IP_Address = findViewById(R.id.inputIP);
        Port_Number = findViewById(R.id.inputPort);
        Msg = findViewById(R.id.inputMSG);

        connectToServer = findViewById(R.id.connectBtn);
        progressBar = findViewById(R.id.progress_circular);

        message_list = findViewById(R.id.message_list);
        allMessages = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allMessages);
        message_list.setAdapter(arrayAdapter);
    }

    public void sendMessage(View view) {

        String MSG = Msg.getText().toString();
        if (MSG.isEmpty()) {
            Toast.makeText(this, "Enter Your Message", Toast.LENGTH_SHORT).show();
            return;
        }

        ClientPCThread clientThread = new ClientPCThread(IPAddress, PortNumber, MSG);
        Thread thread = new Thread(clientThread);
        thread.start();
    }

    public void connectToServer(View view) {
        IPAddress = IP_Address.getText().toString();
        if (IPAddress.isEmpty()) {
            Toast.makeText(this, "Enter IP Address", Toast.LENGTH_SHORT).show();
            return;
        }

        PortNumber = Port_Number.getText().toString();
        if (PortNumber.isEmpty()) {
            Toast.makeText(this, "Enter Port Number", Toast.LENGTH_SHORT).show();
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                Socket socket = new Socket(IPAddress, Integer.parseInt(PortNumber));
                DataInputStream DIS = new DataInputStream(socket.getInputStream());
                DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());

                String msgin = "";
                DOS.writeUTF("Connect");
                msgin = DIS.readUTF();
                if (msgin.equals("OK")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionState.setText("Connection");
                            connectionState.setBackgroundColor(getResources().getColor(R.color.green));
                            message_list.setVisibility(View.VISIBLE);
                            layoutInputMsg.setVisibility(View.VISIBLE);
                            layoutIPAddress.setVisibility(View.GONE);
                            connectToServer.setVisibility(View.GONE);
                            // loadAllMessage(IPAddress, PortNumber);
                        }
                    });
                }
                DOS.flush();
                DIS.close();
                DOS.close();
                socket.close();

            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(this, "The connection has been disconnected", Toast.LENGTH_SHORT).show();
        ClientPCThread clientThread = new ClientPCThread(IPAddress, PortNumber, "END");
        Thread thread = new Thread(clientThread);
        thread.start();
    }

    public void uploadMessage(Message message) {
        String txtIPAddress = IPAddress.replace(".", "_");
        refMessage
                .child(user.getUid())
                .child(txtIPAddress)
                .child(PortNumber)
                .child(String.valueOf(System.currentTimeMillis()))
                .setValue(message);
    }

    class ClientPCThread implements Runnable {
        String IPAddress, PortNumber, MSG;

        public ClientPCThread(String IPAddress, String portNumber, String MSG) {
            this.IPAddress = IPAddress;
            PortNumber = portNumber;
            this.MSG = MSG;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(IPAddress, Integer.parseInt(PortNumber));
                DataInputStream DIS = new DataInputStream(socket.getInputStream());
                DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
                String msgin = "";
                while (!msgin.equals("END")) {
                    DOS.writeUTF(MSG);

                    Message ClientMessage = new Message("Client", MSG, getTime());
                    allMessages.add(ClientMessage.toString());
                    runOnUiThread(() -> {

                        Msg.setText("");
                        uploadMessage(ClientMessage);
                        arrayAdapter.notifyDataSetChanged();
                    });

                    msgin = DIS.readUTF();
                    String finalMsgin = msgin;

                    Message Servermessage = new Message("Server", finalMsgin, getTime());
                    allMessages.add(Servermessage.toString());
                    runOnUiThread(() -> {
                        uploadMessage(Servermessage);
                        arrayAdapter.notifyDataSetChanged();
                    });
                    DOS.flush();
                    DIS.close();
                    DOS.close();
                    socket.close();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ClientPCActivity.this, "The server has been disconnected", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }

        String getTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            return sdf.format(new Date());
        }
    }
}
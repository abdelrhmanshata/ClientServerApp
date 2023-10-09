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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClientPhoneActivity extends AppCompatActivity {

    public static final int SERVERPORT = 8010;
    String IPAddress;
    TextView connectionState;
    LinearLayout layoutInputMsg, layoutIPAddress;
    EditText IP_Address, Msg;
    ProgressBar progressBar;
    ListView message_list;
    List<String> allMessages;
    ArrayAdapter arrayAdapter;
    Button connectToServer;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser user = auth.getCurrentUser();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference refMessage = database.getReference("Message");
    private ClientThread clientThread;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_phone);

        connectionState = findViewById(R.id.connectionState);
        layoutInputMsg = findViewById(R.id.layoutInputMsg);
        layoutIPAddress = findViewById(R.id.layoutIPAddress);

        IP_Address = findViewById(R.id.inputIP);
        Msg = findViewById(R.id.inputMSG);

        connectToServer = findViewById(R.id.connectBtn);
        progressBar = findViewById(R.id.progress_circular);

        message_list = findViewById(R.id.message_list);
        allMessages = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allMessages);
        message_list.setAdapter(arrayAdapter);
    }

    public void connectToServer(View view) {
        IPAddress = IP_Address.getText().toString();
        if (IPAddress.isEmpty()) {
            Toast.makeText(this, "Enter IP Address", Toast.LENGTH_SHORT).show();
            return;
        }

        clientThread = new ClientThread(IPAddress);
        thread = new Thread(clientThread);
        thread.start();

        connectionState.setText("Connection");
        connectionState.setBackgroundColor(getResources().getColor(R.color.green));
        message_list.setVisibility(View.VISIBLE);
        layoutInputMsg.setVisibility(View.VISIBLE);

        layoutIPAddress.setVisibility(View.GONE);
        connectToServer.setVisibility(View.GONE);

        showMessage("Server", "Connecting to Server...");
    }

    public void clientSendMessage(View view) {
        String MSG = Msg.getText().toString();
        if (MSG.isEmpty()) {
            Toast.makeText(this, "Enter Your Message", Toast.LENGTH_SHORT).show();
            return;
        }
        showMessage("Client", MSG);
        if (null != clientThread) {
            clientThread.sendMessage(MSG);
            Message Message = new Message("Client", MSG, getTime());
            uploadMessage(Message);
        }
    }

    public void showMessage(String Sender, final String message) {
        Message Message = new Message(Sender, message, getTime());
        allMessages.add(Message.toString());
        arrayAdapter.notifyDataSetChanged();
    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    public void uploadMessage(Message message) {
        String txtIPAddress = IPAddress.replace(".", "_");
        refMessage
                .child(user.getUid())
                .child(txtIPAddress)
                .child(String.valueOf(SERVERPORT))
                .child(String.valueOf(System.currentTimeMillis()))
                .setValue(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
            thread.interrupt();
            thread=null;
        }
    }

    class ClientThread implements Runnable {
        private String SERVER_IP;
        private Socket socket;
        private BufferedReader input;
        public ClientThread(String SERVER_IP) {
            this.SERVER_IP = SERVER_IP;
        }
        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                while (!Thread.currentThread().isInterrupted()) {
                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        String finalMessage = message;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMessage("Server", finalMessage);
                                layoutInputMsg.setVisibility(View.GONE);
                                connectionState.setText("Disconnected");
                                connectionState.setBackgroundColor(getResources().getColor(R.color.red));
                            }
                        });
                        break;
                    }
                    String finalMessage1 = message;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessage("Server", finalMessage1);
                            Message Message = new Message("Server", finalMessage1, getTime());
                            uploadMessage(Message);
                        }
                    });
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println(message);
                            runOnUiThread(() -> {
                                Msg.setText("");
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
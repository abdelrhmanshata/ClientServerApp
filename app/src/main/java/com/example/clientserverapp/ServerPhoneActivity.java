package com.example.clientserverapp;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServerPhoneActivity extends AppCompatActivity {

    public static final int SERVER_PORT = 8010;
    Thread serverThread = null;
    String IP_Address;
    TextView connectionState, ipAddress, portNumber;
    LinearLayout layoutIPAddress, layoutInputMsg;
    ListView message_list;
    List<String> allMessages;
    ArrayAdapter arrayAdapter;
    EditText inputMSG;
    private ServerSocket serverSocket;
    private Socket tempClientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_phone);

        connectionState = findViewById(R.id.connectionState);

        ipAddress = findViewById(R.id.ipAddress);
        portNumber = findViewById(R.id.portNumber);

        layoutIPAddress = findViewById(R.id.layoutIPAddress);
        inputMSG = findViewById(R.id.inputMSG);

        layoutInputMsg = findViewById(R.id.layoutInputMsg);

        message_list = findViewById(R.id.message_list);
        allMessages = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allMessages);
        message_list.setAdapter(arrayAdapter);

        // Get IP Address
        WifiManager wm = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IP_Address = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

    }

    public void ServerRunning(View view) {
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    public void serverSendMessage(View view) {
        String MSG = inputMSG.getText().toString();
        if (MSG.isEmpty()) {
            Toast.makeText(this, "Enter Your Message", Toast.LENGTH_SHORT).show();
            return;
        }
        showMessage("Server", MSG);
        sendMessage(MSG);
    }

    private void sendMessage(final String message) {
        try {
            if (null != tempClientSocket) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out = null;
                        try {
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        out.println(message);
                        runOnUiThread(() -> {
                            inputMSG.setText("");
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMessage(String Sender, final String message) {
        Message Message = new Message(Sender, message, getTime());
        allMessages.add(Message.toString());
        arrayAdapter.notifyDataSetChanged();
    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerThread implements Runnable {
        public void run() {
            Socket socket;
            try {

                serverSocket = new ServerSocket(SERVER_PORT);
                //serverSocket = new ServerSocket(SERVER_PORT, 0, InetAddress.getByName("localhost"));
                runOnUiThread(() -> {
                    findViewById(R.id.serverRunbutton).setVisibility(View.GONE);
                    connectionState.setText("Connection");
                    connectionState.setBackgroundColor(getResources().getColor(R.color.green));
                    layoutIPAddress.setVisibility(View.VISIBLE);
                    ipAddress.setText("IP     : " + IP_Address);
                    //ipAddress.setText("IP     : " + serverSocket.getLocalSocketAddress().toString());
                    portNumber.setText("Port : " + SERVER_PORT);

                    message_list.setVisibility(View.VISIBLE);
                    showMessage("Server", "Server has been started");

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> showMessage("Server", "Error Communicating to Client :" + e.getMessage()));
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> showMessage("Server", "Error Connecting to Client!!"));
            }
            runOnUiThread(() -> {
                showMessage("Server", "Connected to Client!!");
                connectionState.setText("Connection");
                connectionState.setBackgroundColor(getResources().getColor(R.color.green));
                layoutInputMsg.setVisibility(View.VISIBLE);
            });
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Client Disconnected";
                        String finalRead = read;
                        runOnUiThread(() -> {
                            showMessage("Client", finalRead);
                            layoutInputMsg.setVisibility(View.GONE);
                            connectionState.setText("Disconnected");
                            connectionState.setBackgroundColor(getResources().getColor(R.color.red));
                        });
                        break;
                    }
                    String finalRead1 = read;
                    runOnUiThread(() -> showMessage("Client", finalRead1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

package com.ResivoJe.PceleV3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import com.ResivoJe.PceleV3.database.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class DeviceController {
    private static final String TAG = "DeviceController";

    private final static String START = "S";
    private final static String STOP = "X";
    private final static String GET = "G";
    private final static String BEGIN = "<";
    private final static String END = ">";

    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    private InputStream inStream;
    private OutputStream outStream;

    private TerminalActivity terminalActivity;
    private String address;

    public DeviceController(TerminalActivity terminalActivity, String address) {
        this.terminalActivity = terminalActivity;
        this.address = address;
    }

    public boolean connect() {
        try {
            if (btSocket == null || !isBtConnected) {
                myBluetooth = BluetoothAdapter.getDefaultAdapter();                     //get the mobile bluetooth device
                BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);     //connects to the device's address and checks if it's available
                btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                btSocket.connect();                                                     //start connection
                isBtConnected = true;
                try {
                    inStream = btSocket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    outStream = btSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }
            }
        }
        catch (IOException e) {
            isBtConnected = false;//if the try failed, you can check the exception here
            Log.e(TAG, "Error occurred when connecting", e);
        }
        return isBtConnected;
    }

    public void disconnect() {
        if (btSocket != null) {
            try {
                btSocket.close(); //close connection
                inStream.close();
                outStream.close();
                isBtConnected = false;
            } catch (IOException e) {
                Log.e(TAG, "Error occured when disconnecting", e);
            }
        }
    }

    public static String getStopCommand() {
        return BEGIN + STOP + END;
    }

    public static String getReadCommand() {
        return BEGIN + GET + END;
    }

    public static String getParamCommand(Parameters parameters) {
        String command =
                BEGIN +
                START + ";" +
                "T" + parameters.getTime() + ";" +
                "I" + parameters.getImpuls() + ";" +
                "P" + parameters.getPause() + ";" +
                "F" + parameters.getFrequency() + ";" +
                "V" + (parameters.getVoltage() * 7 + (parameters.getVoltage() / 10)) +
                END;
        return command;
    }

    public String send(String command) {
        String responseString = null;
        try {
            byte[] data = (command + "\r\n").getBytes();
            outStream.write(data);
            outStream.flush();

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                // Do nothing
            }
            int available = inStream.available();
            byte[] response = new byte[available];
            inStream.read(response);
            responseString = new String(response);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return responseString;
    }

}

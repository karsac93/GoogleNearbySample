package nearby.mst.com.googlenearbysample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private ConnectionsClient connectionsClient;
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private TextView textView;
    private EditText editText;
    private Button sendButton;
    private static final String SENDER = "DTNSender";
    private static final String RECEIVER = "DTNReceiver";
    private String clientEndPointId = null;

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            }
        }
    }

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textview);
        editText = findViewById(R.id.message);
        sendButton = findViewById(R.id.sendBtn);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionsClient.stopDiscovery();
                startAdvertising();
                if (clientEndPointId != null) {
                    String msg = String.valueOf(editText.getText());
                    if (msg != null && msg.length() > 0)
                        try {
                            connectionsClient.sendPayload(clientEndPointId, Payload.fromBytes(msg.getBytes("UTF-8")));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                }
            }
        });

        connectionsClient = Nearby.getConnectionsClient(this);
        startDiscovery();
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpoint, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.d(TAG, "In onEndpointFound:" + endpoint + " Info:" + discoveredEndpointInfo.getEndpointName());
            connectionsClient.requestConnection(RECEIVER, endpoint, connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(String s) {

        }
    };

    private void startDiscovery() {
        connectionsClient.startDiscovery(getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY));
    }

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            try {
                String message = new String(payload.asBytes(), "UTF-8");
                Log.d(TAG, "Message obtained:" + message);
                if (message != null)
                    textView.setText(message);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS)
                Log.d(TAG, "Inside onPayloadtransferUpdate, Success");
            else
                Log.d(TAG, "Inside onPayloadtransferUpdate, Fail");
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpoint, ConnectionInfo connectionInfo) {
            Log.d(TAG, "Connection Accepted, inside connectionLifecycleCallback");
            connectionsClient.acceptConnection(endpoint, payloadCallback);
            Log.d(TAG, "Opponent Name:" + connectionInfo.getEndpointName());
        }

        @Override
        public void onConnectionResult(String endPoint, ConnectionResolution connectionResolution) {
            if (connectionResolution.getStatus().isSuccess()) {
                connectionsClient.stopAdvertising();
                clientEndPointId = endPoint;
                Log.d(TAG, "onConnectResult, Success");
            } else
                Log.d(TAG, "onConnectResult, Fail");
        }

        @Override
        public void onDisconnected(String s) {
            connectionsClient.stopAdvertising();
            textView.setText(getString(R.string.display_text));
            startDiscovery();
            Log.d(TAG, "Disconnected");
        }
    };

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop, stopping all discovery and advertising");
        connectionsClient.stopAllEndpoints();
        super.onStop();
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(SENDER, getPackageName(), connectionLifecycleCallback, new AdvertisingOptions(STRATEGY));
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Log.d(TAG, "Permission Granted");
            else {
                Toast.makeText(this, "Please grant permission to proceed further !", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
    }
}

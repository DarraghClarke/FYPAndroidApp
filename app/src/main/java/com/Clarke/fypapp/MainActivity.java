package com.Clarke.fypapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 0;

    Button select,upload,chooseOrchestrator;
    TextView selectedFileName,responseField;
    File selectedFile;
    ImageButton sendMessage;
    EditText TextInput;
    Switch secureSwitch;
    OrchestratorSocketController orchestratorSocketController = null;
    long startTime,endTime;//these are just used for test timings

    /**
     * On launch this creates the applications interface and button listeners
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        select = findViewById(R.id.select);
        upload = findViewById(R.id.upload);
        chooseOrchestrator = findViewById(R.id.chooseLocation);
        sendMessage = findViewById(R.id.sendMessage);
        selectedFileName = findViewById(R.id.textView);
        responseField = findViewById(R.id.responseField);
        TextInput = findViewById(R.id.TextInput);
        secureSwitch = findViewById(R.id.secureMode);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        select.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          // start file chooser
                                          Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                          intent.setType("*/*");
                                          intent.addCategory(Intent.CATEGORY_OPENABLE);
                                          startActivityForResult(
                                                  Intent.createChooser(intent, "Select a file to upload"),
                                                  FILE_SELECT_CODE);
                                      }
                                  }

        );

        chooseOrchestrator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.setTitle("Set Orchestrator Address");

                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseOrchestrator.setText(input.getText().toString());
                        if (orchestratorSocketController == null) {
                            System.out.println("tried llaunching");
                            startClient();

                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (orchestratorSocketController == null) {
                    startClient();
                }
                sendFile(selectedFile);
                responseField.setText("launched the orchestratorSocketController");

            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (orchestratorSocketController == null) {
                    startClient();
                }
                sendString(TextInput.getText().toString());


            }
        });

        /**
         * This timer is used to check if there has been any response from the service host, and is just used for testing
         */
        new Timer().schedule(
                new TimerTask() {

                    @Override
                    public void run() {
                        if (orchestratorSocketController != null && !orchestratorSocketController.getNodeSocketInformation().equals("")) {
                            endTime = System.nanoTime();
                            responseField.setText(orchestratorSocketController.getNodeSocketInformation());
                            System.out.println(endTime - startTime + " The time from creation of orchestrator controller to resposne from the service ");
                        }
                    }
                }, 1000, 1000);

    }

    /**
     * This method is used to launch the Client that connects to the orchestrator
     */
    public void startClient() {
        startTime = System.nanoTime();
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //desiredServiceName here can be changed to reflect any desired service
                    orchestratorSocketController = new OrchestratorSocketController(new URI(chooseOrchestrator.getText().toString()), "docker.tar");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                //if secure mode is on, this will get the SSL information
                if (secureSwitch.isChecked()) {
                    SSLSocketFactory factory = getSSLConextFromAndroidKeystore().getSocketFactory();
                    orchestratorSocketController.setSocketFactory(factory);
                }
                orchestratorSocketController.run();
            }
        }).start();
    }

    /**
     * This passes any String input from the user down to the websockets to send it to the service
     * @param message any command message
     */
    public void sendString(String message) {
        if (orchestratorSocketController != null) {
            orchestratorSocketController.sendMessageOnNodeSocket(message);
        }
    }

    /**
     * This passes any file input from the user down to the websockets to send it to the service
     * @param file any file
     */
    public void sendFile(File file) {
        if (orchestratorSocketController != null && selectedFile != null) {
            orchestratorSocketController.sendFileOnNodeSocket(file);
        }
    }

    /**
     * This method is just used to get a file from the users storage so that they can send it to the
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            Uri content_describer = data.getData();
            String scheme = content_describer.getScheme();
            if (scheme.equals("file")) {
                selectedFileName.setText(content_describer.getLastPathSegment());
            } else if (scheme.equals("content")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(content_describer, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        selectedFileName.setText(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }


    /**
     * this method loads up the keystore and returns SSL Context
     *
     * @return the SSLContext information
     */
    private SSLContext getSSLConextFromAndroidKeystore() {
        // load up the key store
        String storePassword = "storepassword";
        String keyPassword = "keypassword";

        KeyStore ks;
        SSLContext sslContext;
        try {
            KeyStore keystore = KeyStore.getInstance("BKS");//note keystore is BKS format on android
            InputStream in = getResources().openRawResource(R.raw.keystore);
            try {
                keystore.load(in, storePassword.toCharArray());
            } finally {
                in.close();
            }
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(keystore, keyPassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keystore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            throw new IllegalArgumentException();
        }
        return sslContext;
    }
}

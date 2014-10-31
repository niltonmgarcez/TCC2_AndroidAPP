package com.example.beaconmintest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
      
      



import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
      






//---add this---
import com.estimote.sdk.Beacon;
      
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Utils;
import com.estimote.sdk.BeaconManager.MonitoringListener;
import com.estimote.sdk.Region;
      
public class MainActivity extends Activity {
      
    //Define a UUID (universally unique identifier - padr�o para dispositivos do tipo Beacon)
	//e os valores de MAJOR e MINOR. Os mesmos podem ser usados para filtrar um ou mais Beacons
	private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
	private static final int ESTIMOTE_MAJOR = 24568;
	private static final int ESTIMOTE_MINOR = 5286;
	  
    //Define uma regi�o estabelecendo os crit�rios dos Beacons a serem descobertos)
	//Para encontrar TODOS os Beacons presentes na regi�o definida, passamos null nos 2 �ltimo par�metros (major e minor)
	private static final Region ALL_ESTIMOTE_BEACONS = 
    		new Region("regionId", ESTIMOTE_PROXIMITY_UUID, ESTIMOTE_MAJOR, ESTIMOTE_MINOR);
      
    //TAG a ser usada como t�tulo dos notifica��es. Criada aqui para facilitar futuras altera��es
	protected static final String TAG = "EstimoteiBeacon";
    private static final int NOTIFICATION_ID = 123;
      
    //Cria��o dos EditText que ser�o vinculados aos declarados no XML e acessados pelo Resources
    EditText txtUUID1;
    TextView txtTeste;  
    
    String lstrNomeArq;
    File arq;
    byte[] dados;
    int count = 0;
    
    //Declara objetos para o servi�o de gerenciamento dos Beacons e de notifica��es
    BeaconManager beaconManager;
    NotificationManager notificationManager;
      
    //Sobrescreve o m�todo onCreate da Activity para j� na sua cria��o criar os objetos necess�rios
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      
        //pega as refer�ncias dos EditText criados no XML e acessados no Resources
        txtUUID1 = (EditText)
            findViewById(R.id.txtTeste);
        
        txtTeste = (TextView)
                findViewById(R.id.textView1);
        
        txtTeste.setText("");
            
      
        //Criar o servi�o de gerenciamento dos Beacons e de notifica��es
        beaconManager = new BeaconManager(this);
        notificationManager = (NotificationManager)
            getSystemService(
                Context.NOTIFICATION_SERVICE);
      
        //Seta o per�odo de scanneamento do beaconManager, definindo o intervalo de leitura dos Beacons
        //beaconManager.setBackgroundScanPeriod(3000, 5000);
        beaconManager.setForegroundScanPeriod(150, 0);
      
        //Define um listener para monitorar os beacons e fazer sua leitura
        //� um Callback que � invocado quando Beacons s�o descobertos na regi�o definida
        beaconManager.setMonitoringListener(new
        MonitoringListener() {
            //Sobrescreve o m�todo onEnteredRegion que ocorre quando o dispositivo entra na regi�o de um dos Beacons definidos
        	@Override
            public void onEnteredRegion(Region region, List<Beacon> beacons)
            {
        		//Se a aplica��o est� em primeiro plano gera uma mensagem do tipo Toast.MakeTest                
            	if (isAppInForeground(getApplicationContext())) {
                    Toast.makeText(
                        getApplicationContext(),
                        "Entered region",
                        Toast.LENGTH_LONG).show();
                    try {
                    	//Starta um intervalo de Beacons (daqueles Beacons descobertos e registrados por beaconManager.SetRanging)
                        beaconManager.startRanging(
                                    ALL_ESTIMOTE_BEACONS);
                    } catch (RemoteException e) {
                        Log.e(TAG,
                            "Cannot start ranging", e);
                    }
                //Se apica��o n�o est� em primeiro plano ao ingressar numa regi�o, gera uma notifica��o ao inv�s de mensagem    
                } else {
                    postNotification("Entered region");
                }
            }
      
        	//Sobrescreve o m�todo onExitRegion que ocorre quando o dispositivo sai de todas as regi�es dos Beacons definidos
            @Override
            public void onExitedRegion(Region region) {
            	//Se a aplica��o est� em primeiro plano gera uma mensagem do tipo Toast.MakeTest
            	if (isAppInForeground(getApplicationContext())) {
                    Toast.makeText(
                        getApplicationContext(),
                        "Exited region",
                        Toast.LENGTH_LONG).show();
                  //Se apica��o n�o est� em primeiro plano ao ingressar numa regi�o, gera uma notifica��o ao inv�s de mensagem    
                } else {
                    postNotification("Exited region");
                }
                //Desativa o intervalo de Beacons (desfaz o registro e os tira da lista de dispositivos descobertos)
                try {
                    beaconManager.stopRanging(
                            ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e(TAG,
                        e.getLocalizedMessage());
                }
      
                //Executa a limpeza dos �ltimos valores dos Beacons e colocados nos EditText. Foi criado dentro de um UI Thread
                //para garantir a sua execu��o imediata logo ap�s detectar que n�o se est� em nenhuma regi�o de cobertura
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        txtUUID1.setText("");
                        txtTeste.setText("");

                    }
                });
            }
        });
      
        //Cria um novo intervalo de Beacons descobertos
        beaconManager.setRangingListener(new
        //Define um listener para monitorar quando um novo conjunto de Beacons � descoberto e registrado
        BeaconManager.RangingListener() {
            @Override public void onBeaconsDiscovered(
            Region region, final List<Beacon> beacons)
            {
                Log.d(TAG, "Ranged beacons: " +
                    beacons);
      
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (beacons.size() > 0) {
                            Beacon iBeacon1 = null,
                                   iBeacon2 = null;
                            //Pega o primeiro Beacon
                            iBeacon1 = beacons.get(0);
      
                            if (beacons.size()>1) {
                            	//Pega o primeiro Beacon
                                iBeacon2 =
                                    beacons.get(1);
                            }
                            //Primeiro Beacon: L� as valores de UUID, Major, Minor e RSSI/Dist�ncia passando para os EditText 
                            notificationManager.cancel(NOTIFICATION_ID);
                            
                            
                            txtUUID1.setText(
                                    String.valueOf(Math.min(Utils.computeAccuracy(iBeacon1), 70.0)));                                
                            txtTeste.append("*" +
                                    String.valueOf(Math.min(Utils.computeAccuracy(iBeacon1), 70.0)));
                            
                            count++;
                            
                            if(count==150){
                                
                                String ARQUIVO = "teste9.txt";
                                
                                try {
                                    try {
                                        File f = new File(Environment.getExternalStorageDirectory()+"/"+ARQUIVO);
                                        //postNotification(Environment.getExternalStorageDirectory().toString());
                                        FileOutputStream out = new FileOutputStream(f);
                                        out.write(txtTeste.getText().toString().getBytes());
                                        out.flush();
                                        out.close();
                                        postNotification("GRAVEI!!!");
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                    }
                                } catch (Exception e) {
                                    System.out.println(e.toString());
                                }
                            	
                            	
                            }
                            //Segundo Beacon: L� as valores de UUID, Major, Minor e RSSI/Dist�ncia passando para os EditText
                            if (iBeacon2!=null) {
                            	//txtUUID1.setText(
                                //        String.valueOf(Math.min(Utils.computeAccuracy(iBeacon2), 70.0))); 
                            } else {
                                //Se n�o tem o segundo Beacons, limpa os EditText equivalentes
                                //txtUUID2.setText("");
                            }
                        } else {
                        	//Se n�o tem o segundo Beacons, limpa os EditText equivalentes
                            txtUUID1.setText("");
                        }
                    }
                });
            }
        });
    }
     
    //Sobrescreve o m�todo onStart da Activity
    @Override
    protected void onStart() {
        super.onStart();
        //Cancela qualquer notifica��o que esteja vigente 
        notificationManager.cancel(NOTIFICATION_ID);
        //Conecta o beaconManager ao servi�o de leitura dos Beacons
        beaconManager.connect(new
        BeaconManager.ServiceReadyCallback() {
            //Sobrescreve o m�todo onServiceReady dando um start no servi�o de monitoramento (ou um erro caso n�o consiga)
        	@Override
            public void onServiceReady() {
                try {
                    beaconManager.startMonitoring(
                            ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.d(TAG,
                    "Error while starting monitoring");
                }
            }
        });
    }
      
    //Sobrescreve o m�todo onDestroy da Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Cancela qualquer notifica��o que esteja vigente 
        notificationManager.cancel(NOTIFICATION_ID);
        //Desconecta o beaconManager do servi�o de leitura dos Beacons
        beaconManager.disconnect();
    }
    
    //Sobrescreve o m�todo onStop da Activity
    @Override
    protected void onStop() {
        super.onStop();
        try {
        	//Limpa o intervalo de Beacons descobertos (desregistra) do beaconManager
            beaconManager.stopRanging(
                        ALL_ESTIMOTE_BEACONS);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot stop", e);
        }
    }
    
    //M�todo auxiliar para definir se a aplica��o est� em primeiro plano
    public static boolean isAppInForeground(
    Context context) {
        List<RunningTaskInfo> task = ((ActivityManager)
            context.getSystemService(
                Context.ACTIVITY_SERVICE))
                .getRunningTasks(1);
        if (task.isEmpty()) {
            return false;
        }
        return task
                .get(0)
                .topActivity
                .getPackageName()
                .equalsIgnoreCase(
                    context.getPackageName());
    }
    
    //M�todo auxiliar para gerar notifica��es caso a aplica��o esteja em segundo plano e n�o for poss�vel exibir mensagens
    private void postNotification(String msg) {
        Intent notifyIntent = new
            Intent(MainActivity.this,
            MainActivity.class);
      
        notifyIntent.setFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent =
            PendingIntent.getActivities(
                MainActivity.this, 0, new Intent[] {
                notifyIntent },
                PendingIntent.FLAG_UPDATE_CURRENT);
      
        Notification notification = new
            Notification.Builder(MainActivity.this)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Monitoring Region")
            .setContentText(msg)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build();
        notification.defaults |=
            Notification.DEFAULT_SOUND;
        notification.defaults |=
            Notification.DEFAULT_LIGHTS;
        notificationManager.notify(NOTIFICATION_ID,
            notification);
    }
      
}
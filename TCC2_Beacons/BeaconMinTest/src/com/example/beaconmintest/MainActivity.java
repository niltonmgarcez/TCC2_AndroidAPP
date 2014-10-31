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
      
    //Define a UUID (universally unique identifier - padrão para dispositivos do tipo Beacon)
	//e os valores de MAJOR e MINOR. Os mesmos podem ser usados para filtrar um ou mais Beacons
	private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
	private static final int ESTIMOTE_MAJOR = 24568;
	private static final int ESTIMOTE_MINOR = 5286;
	  
    //Define uma região estabelecendo os critérios dos Beacons a serem descobertos)
	//Para encontrar TODOS os Beacons presentes na região definida, passamos null nos 2 último parâmetros (major e minor)
	private static final Region ALL_ESTIMOTE_BEACONS = 
    		new Region("regionId", ESTIMOTE_PROXIMITY_UUID, ESTIMOTE_MAJOR, ESTIMOTE_MINOR);
      
    //TAG a ser usada como título dos notificações. Criada aqui para facilitar futuras alterações
	protected static final String TAG = "EstimoteiBeacon";
    private static final int NOTIFICATION_ID = 123;
      
    //Criação dos EditText que serão vinculados aos declarados no XML e acessados pelo Resources
    EditText txtUUID1;
    TextView txtTeste;  
    
    String lstrNomeArq;
    File arq;
    byte[] dados;
    int count = 0;
    
    //Declara objetos para o serviço de gerenciamento dos Beacons e de notificações
    BeaconManager beaconManager;
    NotificationManager notificationManager;
      
    //Sobrescreve o método onCreate da Activity para já na sua criação criar os objetos necessários
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      
        //pega as referências dos EditText criados no XML e acessados no Resources
        txtUUID1 = (EditText)
            findViewById(R.id.txtTeste);
        
        txtTeste = (TextView)
                findViewById(R.id.textView1);
        
        txtTeste.setText("");
            
      
        //Criar o serviço de gerenciamento dos Beacons e de notificações
        beaconManager = new BeaconManager(this);
        notificationManager = (NotificationManager)
            getSystemService(
                Context.NOTIFICATION_SERVICE);
      
        //Seta o período de scanneamento do beaconManager, definindo o intervalo de leitura dos Beacons
        //beaconManager.setBackgroundScanPeriod(3000, 5000);
        beaconManager.setForegroundScanPeriod(150, 0);
      
        //Define um listener para monitorar os beacons e fazer sua leitura
        //É um Callback que é invocado quando Beacons são descobertos na região definida
        beaconManager.setMonitoringListener(new
        MonitoringListener() {
            //Sobrescreve o método onEnteredRegion que ocorre quando o dispositivo entra na região de um dos Beacons definidos
        	@Override
            public void onEnteredRegion(Region region, List<Beacon> beacons)
            {
        		//Se a aplicação está em primeiro plano gera uma mensagem do tipo Toast.MakeTest                
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
                //Se apicação não está em primeiro plano ao ingressar numa região, gera uma notificação ao invés de mensagem    
                } else {
                    postNotification("Entered region");
                }
            }
      
        	//Sobrescreve o método onExitRegion que ocorre quando o dispositivo sai de todas as regiões dos Beacons definidos
            @Override
            public void onExitedRegion(Region region) {
            	//Se a aplicação está em primeiro plano gera uma mensagem do tipo Toast.MakeTest
            	if (isAppInForeground(getApplicationContext())) {
                    Toast.makeText(
                        getApplicationContext(),
                        "Exited region",
                        Toast.LENGTH_LONG).show();
                  //Se apicação não está em primeiro plano ao ingressar numa região, gera uma notificação ao invés de mensagem    
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
      
                //Executa a limpeza dos últimos valores dos Beacons e colocados nos EditText. Foi criado dentro de um UI Thread
                //para garantir a sua execução imediata logo após detectar que não se está em nenhuma região de cobertura
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
        //Define um listener para monitorar quando um novo conjunto de Beacons é descoberto e registrado
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
                            //Primeiro Beacon: Lê as valores de UUID, Major, Minor e RSSI/Distância passando para os EditText 
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
                            //Segundo Beacon: Lê as valores de UUID, Major, Minor e RSSI/Distância passando para os EditText
                            if (iBeacon2!=null) {
                            	//txtUUID1.setText(
                                //        String.valueOf(Math.min(Utils.computeAccuracy(iBeacon2), 70.0))); 
                            } else {
                                //Se não tem o segundo Beacons, limpa os EditText equivalentes
                                //txtUUID2.setText("");
                            }
                        } else {
                        	//Se não tem o segundo Beacons, limpa os EditText equivalentes
                            txtUUID1.setText("");
                        }
                    }
                });
            }
        });
    }
     
    //Sobrescreve o método onStart da Activity
    @Override
    protected void onStart() {
        super.onStart();
        //Cancela qualquer notificação que esteja vigente 
        notificationManager.cancel(NOTIFICATION_ID);
        //Conecta o beaconManager ao serviço de leitura dos Beacons
        beaconManager.connect(new
        BeaconManager.ServiceReadyCallback() {
            //Sobrescreve o método onServiceReady dando um start no serviço de monitoramento (ou um erro caso não consiga)
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
      
    //Sobrescreve o método onDestroy da Activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Cancela qualquer notificação que esteja vigente 
        notificationManager.cancel(NOTIFICATION_ID);
        //Desconecta o beaconManager do serviço de leitura dos Beacons
        beaconManager.disconnect();
    }
    
    //Sobrescreve o método onStop da Activity
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
    
    //Método auxiliar para definir se a aplicação está em primeiro plano
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
    
    //Método auxiliar para gerar notificações caso a aplicação esteja em segundo plano e não for possível exibir mensagens
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
package edu.uw.ask710.yama;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;
import static android.provider.Telephony.Sms.Intents.getMessagesFromIntent;

/**
 * Created by Anirudh Subramanyam on 11/2/2017.
 */

//custom BrodcastReceiver for sending and receiving messages
public class MessageReceiver extends BroadcastReceiver{

    public static final String TAG = "RECEIVER";
    private static final String NOTIFICATION_CHANNEL_ID = "my_channel_01";
    private static final int PENDING_ID = 1;
    private static final int NOTIFICATION_ID = 2;
    public static final String NOTIFICATION_REPLY = "reply";
    public static final String PREFERENCE_KEY = "my_preferences";

    //Checks if message is sent and receives new messages (based on pending intent)
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == Composing.ACTION_SMS_STATUS) { //check if message is sent or not.
            Log.v(TAG, "Sending SMS Status");
            if(getResultCode() == Activity.RESULT_OK){
                Toast.makeText(context, "Message sent!", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, "Error sending message", Toast.LENGTH_LONG).show();
            }
        }else if(intent.getExtras() != null){ //receiving new messages
            SmsMessage[] otherMessages = getMessagesFromIntent(intent);
            for(int i = 0; i < otherMessages.length; i++){
                String oldNumber = otherMessages[i].getDisplayOriginatingAddress();
                String oldMessage = otherMessages[i].getDisplayMessageBody();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if(prefs.getBoolean("pref_auto_reply", true)){// auto reply if enabled in preferences.
//                    Log.v(TAG, "Working after changing");
                    String autoMessage = prefs.getString("pref_reply", null);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Composing.ACTION_SMS_STATUS);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sendIntent, 0);

                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(oldNumber, null, autoMessage, pendingIntent, null);
                }

                showNotification(context, oldNumber, oldMessage);
            }

        }
    }


// shows notifications for new messages based on preferences.
    public static void showNotification(Context context, String number, String message){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_new_message)
                        .setContentTitle(number)
                        .setContentText(message)
                        .setChannel(NOTIFICATION_CHANNEL_ID)
                        .setAutoCancel(true);

        //for Oreo if needed.
//        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.0){
//            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Demo channel", NotifactionManager.IMPORTANCE_HIGH);
//            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.createNotificationChannel(channel);
//        }else{
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mBuilder.setVibrate(new long[]{250, 0, 250, 0});
        mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

//        }

        Intent resultIntent = new Intent(context, Reading.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(Reading.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(PENDING_ID,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        mBuilder.addAction(0, "View", pendingIntent);

        //If preference **not** set to auto-reply
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!(prefs.getBoolean("pref_auto_reply", true))){
            Intent replyIntent = new Intent(context, Composing.class);
            replyIntent.putExtra(NOTIFICATION_REPLY, number);
            stackBuilder.addNextIntent(replyIntent);
            PendingIntent repiIntent = stackBuilder.getPendingIntent(PENDING_ID + 1, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.addAction(0, "Reply", repiIntent);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }
}

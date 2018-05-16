package mobi.roomz;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import mobi.roomz.db.DBHandler;
import mobi.roomz.db.consts_interface;
import mobi.roomz.ui.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.parse.ParseBroadcastReceiver;
import com.parse.ParsePush;

public class MyPushReceiver extends ParseBroadcastReceiver implements consts_interface {
	private static final String TAG = "MyPushReceiver";
	private Context context;
	private String action, channel_id, channel_name, nickname, old_nickname, message, guest_id, nick_in_channel, new_room_name, kicked_user_nickname;
	private DBHandler database;
	private long message_time;
	private MediaPlayer mp = null;
	private int secured;
	private final static DateFormat DATETIME_FORMATTER_DAY = new SimpleDateFormat("EEEE, MMM dd, yyyy");
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		database = new DBHandler(context);
		try {
			action = intent.getAction();
			channel_id = intent.getExtras().getString("com.parse.Channel");
			JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
			try {
				channel_name = json.getString("channel_name");
			} catch (Exception e1) {
			}
			try {
				nickname = json.getString("nickname");
			} catch (Exception e1) {
			}
			try {
				old_nickname = json.getString("old_nickname");
			} catch (Exception e1) {
			}
			try {
				kicked_user_nickname = json.getString("kicked_user_nickname");
			} catch (Exception e1) {
			}
			try {
				message = json.getString("message");
			} catch (Exception e1) {
			}
			try {
				guest_id = json.getString("guest_id");
			} catch (Exception e) {
			}
			try {
				new_room_name = json.getString("new_room_name");
			} catch (Exception e) {
			}
			try {
				message_time = json.getLong("message_time");
			} catch (Exception e) {
			}
			try {
				secured = json.getInt("secured");
			} catch (Exception e) {
			}
			
			nick_in_channel = database.get_nickname_in_channel(channel_id);
			
			// room-mate message.
			if (action.equals(PUSH_ACTION_ROOMMATE_MESSAGE)) {
				action_room_mate_message();
			} else {
				// system message.
				if (action.equals(PUSH_ACTION_SYSTEM_MESSAGE)) {
					action_system_message();
				} else {
					// a nickname has been changed in the room.
					if (action.equals(PUSH_ACTION_NICK_CHANGE)) {
						action_nickname_changed();
					} else {
						// room name has been changed.
						if (action.equals(PUSH_ACTION_ROOM_NAME_CHANGE)) {
							action_room_name_changed();
						} else {
							// a user was kicked.
							if (action.equals(PUSH_ACTION_KICK_USER)) {
								action_user_kicked();
							}
						}
					}
				}
			}
			database.increment_CHANNELS_TABLE_msg_unseen_counter_column(channel_id);
		} catch (JSONException e) {
			Log.d(TAG, "JSONException: " + e.getMessage());
		}
	}
	
	
	
	private void action_room_mate_message() {
		// if the message was sent by someone that is not the current user.
		if (!guest_id.equals(database.get_guestId_in_channel(channel_id))) {
			
			// if user was mentioned.
			if (message.contains("@" + nick_in_channel + " ")) {
				// display a system message before the first message of the day (showing date).
				if (database.first_message_of_day(channel_id)) {
					database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
				}
				database.addMessageToChannel(channel_id, nickname, message, guest_id, 1, message_time);
				
				// if channel is set to "generate notification = 1".
				int gen_notification = database.can_generate_notification(channel_id);
				if (gen_notification == 1) {
					generateNotification_mentioned(context, channel_id, channel_name, nickname, nick_in_channel, message);
				}
				
				// normal message - user was not mentioned.
			} else {
				// display a system message before the first message of the day (showing date).
				if (database.first_message_of_day(channel_id)) {
					database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
				}
				database.addMessageToChannel(channel_id, nickname, message, guest_id, 1, message_time);
				
				
				// if channel is set to "generate notification = 1".
				int gen_notification = database.can_generate_notification(channel_id);
				if (gen_notification == 1) {
					generateNotification(context, action, channel_id, channel_name, nickname, message);
				}
			}
			
		} 
		Intent push_received_intent = new Intent();
		push_received_intent.setAction(PUSH_INNERRECEIVER_INTENT_MSG_RECEIVED);
		context.sendBroadcast(push_received_intent);
	}
	
	private void action_system_message() {
		if (!nickname.equals(nick_in_channel)) {
			
			// user joined.
			if (message.equals(PUSH_KEYPHRASE_USER_JOINED)) {
				message = nickname + " has joined the room";
				
				// display a system message before the first message of the day (showing date).
				if (database.first_message_of_day(channel_id)) {
					database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
				}
				database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
			} 
			
			// user left.
			if (message.equals(PUSH_KEYPHRASE_USER_LEFT_ROOM)) {
				message = nickname + " has Left the room";
				
				// display a system message before the first message of the day (showing date).
				if (database.first_message_of_day(channel_id)) {
					database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
				}
				database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
			}
			
			
	//		database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
		}
		// All participants clear room messages command.
			if (message.equals(PUSH_KEYPHRASE_WIPE_ROOM)) {
				if (nickname.equals(nick_in_channel)) {
					message = "Room was wiped";	
				} else {
					message = "Room was wiped";
					database.clear_chat_messages_by_admin(channel_id);
				}
			}
		// change room security settings.
		if (message.equals(PUSH_KEYPHRASE_SECURED_ROOM)) {
			if (secured == 1) {
				message = "Room is now secured, screen-shots are not possible.";
				database.update_secured_column_SECURE(channel_id);	
				// display a system message before the first message of the day (showing date).
				if (database.first_message_of_day(channel_id)) {
					database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
				}
				database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
			} else {
				message = "Room is now Non-secure, screen-shots are possible.";
				database.update_secured_column_UNSECURE(channel_id);	
				// display a system message before the first message of the day (showing date).
				if (database.first_message_of_day(channel_id)) {
					database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
				}
				database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
			}
			
		}
		
		if (!nickname.equals(nick_in_channel)) {
			// generate notification.
			int gen_notification = database.can_generate_notification(channel_id);
			if (gen_notification == 1) {
				generateNotification(context, action, channel_id, channel_name, nickname, message);
			}
		}
		
		
		
		Intent push_received_intent = new Intent();
		push_received_intent.setAction(PUSH_INNERRECEIVER_INTENT_MSG_RECEIVED);
		context.sendBroadcast(push_received_intent);
		
	}
	
	private void action_nickname_changed() {
		// if the user (self) changed nickname.
		if (guest_id.equals(database.get_guestId_in_channel(channel_id))) {
			database.change_nickname_in_channel(channel_id, nickname);
			database.change_roommate_nickname_in_channel(guest_id, nickname);
			// if room-mate changed nickname.
		} else {
			database.change_roommate_nickname_in_channel(guest_id, nickname);
		}
		message = old_nickname + " changed nickname to \'" + nickname + "\'";
		// display a system message before the first message of the day (showing date).
		if (database.first_message_of_day(channel_id)) {
			database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
		}
		database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
		
		if (!old_nickname.equals(nick_in_channel)) {
			// generate notification.
			int gen_notification = database.can_generate_notification(channel_id);
			if (gen_notification == 1) {
				generateNotification(context, action, channel_id, channel_name, nickname, message);
			}
		}
		
		
		Intent push_received_nickname_changed_intent = new Intent();
		push_received_nickname_changed_intent.setAction(PUSH_INNERRECEIVER_INTENT_NICKNAME_CHANGED);
		push_received_nickname_changed_intent.putExtra("guest_id", guest_id);
		context.sendBroadcast(push_received_nickname_changed_intent);
		
	}
	
	private void action_room_name_changed() {
		
		// add change to the database.
		database.change_room_name(channel_id, new_room_name);
		message = nickname + " changed room's name to \'" + new_room_name + "\'";
		// display a system message before the first message of the day (showing date).
		if (database.first_message_of_day(channel_id)) {
			database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
		}
		database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
	
		if (!nickname.equals(nick_in_channel)) {
			// generate notification.
			int generateNotification = database.can_generate_notification(channel_id);
			if (generateNotification == 1) {
				generateNotification(context, action, channel_id, channel_name, nickname, message);
			}
		}
		
		
		
		// send room name changed broadcast.
		Intent push_room_name_changed_intent = new Intent();
		push_room_name_changed_intent.setAction(PUSH_INNERRECEIVER_INTENT_ROOM_NAME_CHANGED);
		context.sendBroadcast(push_room_name_changed_intent);
	}
	
	private void action_user_kicked () {
		// user was kicked.
		if (database.get_guestId_in_channel(channel_id).equals(guest_id)) {
			
			// will be transfered in the intent bundle for later comparison (inner receivers).
			String my_guest_id = database.get_guestId_in_channel(channel_id);
			
			// deletes table entries related to the room.
			database.leave_room(channel_id);
			ParsePush.unsubscribeInBackground(channel_id);
			int gen_notification = database.can_generate_notification(channel_id);
			if (gen_notification == 1) {
				generateNotification_kicked(context, channel_id, channel_name);
			}
			
			// send user kicked broadcast.
			Intent push_user_kicked_intent = new Intent();
			push_user_kicked_intent.setAction(PUSH_INNERRECEIVER_INTENT_USER_KICKED);
			push_user_kicked_intent.putExtra("channel_id", channel_id);
			push_user_kicked_intent.putExtra("guest_id", guest_id);
			push_user_kicked_intent.putExtra("my_guest_id", my_guest_id);
			push_user_kicked_intent.putExtra("message", "You were kicked from " + channel_name);
			context.sendBroadcast(push_user_kicked_intent);
			
		} else {
			// room mate was kicked.
			message = kicked_user_nickname + " was kicked by " + nickname + "";
			
			// display a system message before the first message of the day (showing date).
			if (database.first_message_of_day(channel_id)) {
				database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, DATETIME_FORMATTER_DAY.format(new Date(message_time)), PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time-1);
			}
			database.addMessageToChannel(channel_id, PUSH_ACTION_SYSTEM_MESSAGE, message, PUSH_ACTION_SYSTEM_MESSAGE, 1, message_time);
			if (!nickname.equals(nick_in_channel)) {
				// generate notification.
				int gen_notification = database.can_generate_notification(channel_id);
				if (gen_notification == 1) {
					generateNotification(context, action, channel_id, channel_name, kicked_user_nickname, message);
				}
			}
			
			// send user kicked broadcast.
			Intent push_user_kicked_intent = new Intent();
			push_user_kicked_intent.setAction(PUSH_INNERRECEIVER_INTENT_USER_KICKED);
			push_user_kicked_intent.putExtra("channel_id", channel_id);
			push_user_kicked_intent.putExtra("guest_id", guest_id);
			push_user_kicked_intent.putExtra("room_name", channel_name);
			context.sendBroadcast(push_user_kicked_intent);
		}
	}
	
//	public static List<String> comparison_channel_id_arraylist = new ArrayList<String>();
//	public static List<String> events = new ArrayList<String>();
	 @SuppressLint("NewApi")
	private static void generateNotification(Context context, String action, String channel_id, String channel_name, String nickname, String message) {
		 DBHandler database = new DBHandler(context);
         Bitmap icon1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon); 
         
         NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
         
         NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(  
        		 context).setAutoCancel(true)  
                   .setContentTitle("Roomz")  
                   .setSmallIcon(R.drawable.icon)
                   .setLargeIcon(icon1); 
         
         
         if (action.equals(PUSH_ACTION_ROOMMATE_MESSAGE)) {
  			CONSTS_PUSH_EVENTS_ARRAYLIST.add(nickname + ": " + message);
  			mBuilder.setTicker(nickname + ": " + message);
  			} else {
  				CONSTS_PUSH_EVENTS_ARRAYLIST.add(message);
  				mBuilder.setTicker(message);
  			}
          
         CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.add(channel_id);
         Intent resultIntent = new Intent(context, MainActivity.class); 
         Boolean multiple_rooms = false;
          
  			String channel_id_1 = CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.get(0);
  			for (int i = 0; i < CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.size(); i++) {
				
  				// multiple rooms.
  				if (!CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.get(i).equals(channel_id_1)) {
					
  					multiple_rooms = true;
  				//single room.
				} else {
					multiple_rooms = false;
				}
			}
  			
  			if (CONSTS_PUSH_EVENTS_ARRAYLIST.size() == 1) {
  				mBuilder.setSubText(CONSTS_PUSH_EVENTS_ARRAYLIST.size() + " new message");
			} else {
				mBuilder.setSubText(CONSTS_PUSH_EVENTS_ARRAYLIST.size() + " new messages");
			}
  			
  		// multiple rooms.
  			if (multiple_rooms) {
  				mBuilder.setContentText("New messages");
				// Sets a title for the Inbox style big view 
				inboxStyle.setBigContentTitle("New messages");
				
				//single room.
			} else {
				mBuilder.setContentText(channel_name);
				// Sets a title for the Inbox style big view 
				inboxStyle.setBigContentTitle(channel_name);
				resultIntent = new Intent(context, MainActivity.class);
				resultIntent.putExtra("push_channel_id", channel_id);
				resultIntent.putExtra("user_was_kicked", false);
			}
         
         // Moves events into the big view  
         for (int i = 0; i < CONSTS_PUSH_EVENTS_ARRAYLIST.size(); i++) {  
 
              inboxStyle.addLine(CONSTS_PUSH_EVENTS_ARRAYLIST.get(i).toString());  
         }  
         // Moves the big view style object into the notification object.  
         mBuilder.setStyle(inboxStyle);  
 
         // Creates an explicit intent for an Activity in your app  
         
        
         // The stack builder object will contain an artificial back stack 
         // for the started Activity.  
         // This ensures that navigating backward from the Activity leads out of  
         // your application to the Home screen.  
         TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);  
 
         // Adds the back stack for the Intent (but not the Intent itself)  
         stackBuilder.addParentStack(MainActivity.class);  
 
         // Adds the Intent that starts the Activity to the top of the stack  
         stackBuilder.addNextIntent(resultIntent);  
         PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT| android.content.Intent.FLAG_ACTIVITY_NEW_TASK);  
         mBuilder.setContentIntent(resultPendingIntent);  
 
         NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);  

         Notification notification = mBuilder.build();
      // Clear the notification after been launched
 		notification.flags |= Notification.FLAG_AUTO_CANCEL;
 		
 		// notification light.
 		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
 		notification.ledARGB = 0xff00ff00;
 		notification.ledOnMS = 1000;
 		notification.ledOffMS = 3000;
 		
 	// Play default notification sound
 	//	notification.defaults |= Notification.DEFAULT_SOUND;
 		
 		if (!database.is_channel_muted(channel_id)) {
 			if (database.play_notification_sound(channel_id) == 1) {
	 	//		notification.sound = Uri.parse("android.resource://" + context.getPackageName() + "your_sound_file_name.mp3");
	 			notification.sound = Uri.parse("android.resource://mobi.roomz/" + R.raw.message);
	 	 		notification.defaults |= Notification.DEFAULT_VIBRATE;
	 	 		database.play_notification_sound_update(channel_id, 0);
	 	 		mNotificationManager.notify(100, notification);
			} else {
				 // mId allows you to update the notification later on.  
		         mNotificationManager.notify(100, notification);
			}
		} else {
			mNotificationManager.notify(100, notification);
		}
 		
	}
	
	 private static void generateNotification_kicked(Context context, String channel_id, String channel_name) {
         Bitmap icon1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon); 
         
         
         NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(  
        		 context).setAutoCancel(true)  
                   .setContentTitle(channel_name)  
                   .setSmallIcon(R.drawable.icon)
                   .setLargeIcon(icon1) 
                   .setContentText("You were kicked from " + channel_name);  
         
  			mBuilder.setTicker("you were kicked from " + channel_name);
          
         Intent resultIntent = new Intent(context, MainActivity.class);
		resultIntent = new Intent(context, MainActivity.class);
		resultIntent.putExtra("user_was_kicked", true);
		resultIntent.putExtra("channel_id", channel_id);
		resultIntent.putExtra("room_name", channel_name);
			
        
         // The stack builder object will contain an artificial back stack 
         // for the started Activity.  
         // This ensures that navigating backward from the Activity leads out of  
         // your application to the Home screen.  
         TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);  
 
         // Adds the back stack for the Intent (but not the Intent itself)  
         stackBuilder.addParentStack(MainActivity.class);  
 
         // Adds the Intent that starts the Activity to the top of the stack  
         stackBuilder.addNextIntent(resultIntent);  
         PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT| android.content.Intent.FLAG_ACTIVITY_NEW_TASK);  
         mBuilder.setContentIntent(resultPendingIntent);  
 
         NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);  

         Notification notification = mBuilder.build();
      // Clear the notification after been launched
 		notification.flags |= Notification.FLAG_AUTO_CANCEL;
 		
 		// notification light.
 		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
 		notification.ledARGB = 0xff00ff00;
 		notification.ledOnMS = 1000;
 		notification.ledOffMS = 3000;
 		
 	// Play default notification sound
 	//	notification.defaults |= Notification.DEFAULT_SOUND;
 		
 
//		notification.sound = Uri.parse("android.resource://" + context.getPackageName() + "your_sound_file_name.mp3");
		notification.sound = Uri.parse("android.resource://mobi.roomz/" + R.raw.message);
 		notification.defaults |= Notification.DEFAULT_VIBRATE;
 		mNotificationManager.notify(101, notification);
	
	}
	 
	 private static void generateNotification_mentioned(Context context, String channel_id, String channel_name, String nickname, String nick_in_channel, String message) {
         Bitmap icon1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon); 
         DBHandler database = new DBHandler(context);
         
         NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(  
        		 context).setAutoCancel(true)  
                   .setContentTitle(channel_name)  
                   .setSmallIcon(R.drawable.icon)
                   .setLargeIcon(icon1) 
                   .setContentText("@ From " + nickname + " >> " + message.subSequence((nick_in_channel.length()+1), message.length()));  
         
  		mBuilder.setTicker("@ From " + nickname + " >> " + message.subSequence((nick_in_channel.length()+1), message.length()));
          
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent = new Intent(context, MainActivity.class);
		resultIntent.putExtra("push_channel_id", channel_id);
		resultIntent.putExtra("user_was_kicked", false);
			
        
         // The stack builder object will contain an artificial back stack 
         // for the started Activity.  
         // This ensures that navigating backward from the Activity leads out of  
         // your application to the Home screen.  
         TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);  
 
         // Adds the back stack for the Intent (but not the Intent itself)  
         stackBuilder.addParentStack(MainActivity.class);  
 
         // Adds the Intent that starts the Activity to the top of the stack  
         stackBuilder.addNextIntent(resultIntent);  
         PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT| android.content.Intent.FLAG_ACTIVITY_NEW_TASK);  
         mBuilder.setContentIntent(resultPendingIntent);  
 
         NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);  

         Notification notification = mBuilder.build();
      // Clear the notification after been launched
 		notification.flags |= Notification.FLAG_AUTO_CANCEL;
 		
 		// notification light.
 		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
 		notification.ledARGB = 0xff00ff00;
 		notification.ledOnMS = 500;
 		notification.ledOffMS = 1500;
 		
 	// Play default notification sound
 	//	notification.defaults |= Notification.DEFAULT_SOUND;
 		
 		if (!database.is_channel_muted(channel_id)) {
// 			notification.sound = Uri.parse("android.resource://" + context.getPackageName() + "your_sound_file_name.mp3");
 			notification.sound = Uri.parse("android.resource://mobi.roomz/" + R.raw.mentioned);
 	 		notification.defaults |= Notification.DEFAULT_VIBRATE;
 	 		mNotificationManager.notify(102, notification);
 		} else {
 			mNotificationManager.notify(102, notification);
		}
	}
	 
	 
	 
	 /*
	 
	private void generate_mentioned_notification () {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
												.setSmallIcon(R.drawable.icon)
												.setContentTitle(channel_name);
												mBuilder.setContentText("@ From " + nickname + " >> " + message.subSequence((nick_in_channel.length()+1), message.length()));

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra("push_channel_id", channel_id);

		stackBuilder.addNextIntent(intent);

		// Gets a PendingIntent containing the entire back stack
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_ONE_SHOT
						| android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

		mBuilder.setContentIntent(resultPendingIntent);
		Notification notification = mBuilder.build();
		
		// Clear the notification after been launched
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		// notification light.
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 500;
		notification.ledOffMS = 1500;

		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(PUSH_NOTIFICATION_ID, notification);
	}
	
	private void generate_notification_sound() {
		mp = MediaPlayer.create(context, R.raw.message);
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		switch (audioManager.getRingerMode()) {
		case AudioManager.RINGER_MODE_VIBRATE:
			if (database.play_notification_sound() == PLAY) {
				if (!database.is_channel_muted(channel_id)) {
					v.vibrate(1000);
				}
				
			}
			break;
		case AudioManager.RINGER_MODE_NORMAL:
			if (database.play_notification_sound() == PLAY) {
				if (!database.is_channel_muted(channel_id)) {
					try {
						mp.start();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
					v.vibrate(1000);
				}
			}
			break;
		}
	}
	
	private void generate_mentioned_notification_sound() {
		mp = MediaPlayer.create(context, R.raw.mentioned);
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		switch (audioManager.getRingerMode()) {
		case AudioManager.RINGER_MODE_VIBRATE:
			v.vibrate(1000);
			break;
		case AudioManager.RINGER_MODE_NORMAL:
				try {
					mp.start();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				v.vibrate(1000);
			break;
		}
	}

	*/
	
	
}

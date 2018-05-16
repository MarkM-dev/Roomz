package mobi.roomz.ui;

import java.io.File;
import java.io.FileOutputStream;

import mobi.roomz.MyCursorAdapter;
import mobi.roomz.R;
import mobi.roomz.db.DBHandler;
import mobi.roomz.db.consts_interface;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.appsflyer.AppsFlyerLib;
import com.parse.ParseAnalytics;
import com.parse.ParseObject;
import com.parse.ParsePush;

public class MainActivity extends Activity implements consts_interface {
	
	private DBHandler database = null;
	private MyCursorAdapter adapter = null;
	private SharedPreferences pref;
	private ListView listView;
	private String receivedAction;
	
	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ParseAnalytics.trackAppOpened(getIntent());
		AppsFlyerLib.sendTracking(getApplicationContext());
		
		pref =  getSharedPreferences("settings", MODE_PRIVATE);
		database = new DBHandler(this);
		database.initialize_notification_play_value();
		
		Intent intent = getIntent();
		receivedAction = intent.getAction();
		// if this is a first app run.
		if (pref.getBoolean("fte", true)) {
			
	//		Bitmap bitmap = BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.wallpaper);
	//		save_default_wallpaper_to_dir(bitmap);
			
			
		// user entered via deep-link.
		if (receivedAction != null) {
			if (receivedAction.equals("android.intent.action.VIEW")) {
				Intent join_intent = new Intent(MainActivity.this, JoinPage.class);
				join_intent.putExtra("channel_id", intent.getDataString());
				startActivityForResult(join_intent, 10);
			}	else {
				Intent fte_intent = new Intent(MainActivity.this, FTE.class);
				startActivityForResult(fte_intent, 9);
			}
		} else {
			Intent fte_intent = new Intent(MainActivity.this, FTE.class);
			startActivityForResult(fte_intent, 9);
		}
			
			
		} else {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
			mNotificationManager.cancel(100);
			CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST.clear();
			CONSTS_PUSH_EVENTS_ARRAYLIST.clear();
			
			
			
			// user entered via deep-link.
			if (receivedAction != null) {
				if (receivedAction.equals("android.intent.action.VIEW")) {
					Intent join_intent = new Intent(MainActivity.this, JoinPage.class);
					join_intent.putExtra("channel_id", intent.getDataString());
					startActivityForResult(join_intent, 10);
				}	
			}
			
			
			String push_channel_id = intent.getStringExtra("push_channel_id");
			if (push_channel_id != null) {
				if (!push_channel_id.equals("null")) {
					Intent chatPage_intent = new Intent(MainActivity.this, ChatPage.class);
					chatPage_intent.putExtra("channel_id", push_channel_id);
					intent.putExtra(push_channel_id, "null");
					startActivityForResult(chatPage_intent, 11);
				}
			}
			
			Boolean user_was_kicked = intent.getBooleanExtra("user_was_kicked", false);
			if (user_was_kicked) {
				String _channel_id = intent.getStringExtra("channel_id");
				String _room_name = intent.getStringExtra("room_name");
				try {
					ParsePush.unsubscribeInBackground(_channel_id);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				try {
					database.leave_room(_channel_id);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				try {
					Cursor cursor = database.get_all_channels();
					adapter.changeCursor(cursor);
				} catch (Exception e) {
				}
				
				ContextThemeWrapper ctw = new ContextThemeWrapper(MainActivity.this, R.style.Theme_Base);
				AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				try {
					if (_room_name != null) {
						if (!_room_name.equals("null")) {
							builder.setTitle("Kicked");
							builder.setMessage("You were kicked from " + _room_name + ".");	
						} else {
							builder.setMessage("You are no longer in this room.");	
						} 
					} else {
						builder.setMessage("You are no longer in this room.");	
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
					
				
				builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.show();
			}
			
			ActionBar actionBar = getActionBar();
			actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.MyBackground_orange_red)));
			actionBar.setTitle("Roomz");
			actionBar.setIcon(R.drawable.icon);
			
			listView = (ListView) findViewById(R.id.mListView);
			registerForContextMenu(listView);
			
			Cursor cursor = database.get_all_channels();
			adapter = new MyCursorAdapter(this, cursor);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					
					Intent intent = getIntent();
					
					Intent chatPage_intent = new Intent(MainActivity.this, ChatPage.class);
					String chan_id = database.get_channel_id(id);
					if (receivedAction != null) {
						if(receivedAction.equals(Intent.ACTION_SEND)){
							//content is being shared	
							String receivedText = intent.getStringExtra(Intent.EXTRA_TEXT);
							chatPage_intent.putExtra("channel_id", chan_id);
							chatPage_intent.putExtra("shared_text", receivedText);
							startActivityForResult(chatPage_intent, 11);
						} else {
							chatPage_intent.putExtra("channel_id", chan_id);
							chatPage_intent.putExtra("shared_text", "null");
							startActivityForResult(chatPage_intent, 10);
						}
					} else {
						chatPage_intent.putExtra("channel_id", chan_id);
						chatPage_intent.putExtra("shared_text", "null");
						startActivityForResult(chatPage_intent, 10);
					}
					
				}
			});
		}
	}
	
		// saves image to app's image dir.
		private void save_default_wallpaper_to_dir(Bitmap finalBitmap) {
		    String root = Environment.getExternalStorageDirectory().toString();
		    File myDir = new File(root + "/roomz_wallpapers");    
		    myDir.mkdirs();
			String fname = "wallpaper.jpg";
			
		    File file = new File (myDir, fname);
		    if (file.exists ()) file.delete (); 
		    try {
		    	
	           FileOutputStream out = new FileOutputStream(file);
	           finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
	           out.flush();
	           out.close();
		    } catch (Exception e) {
		           e.printStackTrace();
		    }
		}
	
	private void checkIfNoItems() {
		
			TextView no_items = (TextView)findViewById(R.id.main_no_items_textView);
			int size = listView.getCount();
			if (size < 1) { // if no items exist.
				no_items.setVisibility(1);
			} else {
				no_items.setVisibility(View.GONE);
			}			
		 
	}
	
	private BroadcastReceiver my_msg_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Cursor cursor = database.get_all_channels();
			adapter.changeCursor(cursor);
		}
	};
	
	private BroadcastReceiver room_name_changed_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Cursor cursor = database.get_all_channels();
			adapter.changeCursor(cursor);
		}
	};
	
	private BroadcastReceiver user_kicked_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String _channel_id = intent.getStringExtra("channel_id");
			String _guest_id = intent.getStringExtra("guest_id");
			String message = intent.getStringExtra("message");
			String my_guest_id = intent.getStringExtra("my_guest_id");
			
			// user (self) was kicked.
			if (_guest_id.equals(my_guest_id)) {
				ParsePush.unsubscribeInBackground(_channel_id);
				try {
					database.leave_room(_channel_id);
				} catch (Exception e) {
				}
				
				Cursor cursor = database.get_all_channels();
				adapter.changeCursor(cursor);
				
				ContextThemeWrapper ctw = new ContextThemeWrapper(MainActivity.this, R.style.Theme_Base);
				AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setTitle("Not in the room.");
				builder.setMessage(message);
				builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.show();
			} else {
				// room mate was kicked.
				Cursor cursor = database.get_all_channels();
				adapter.changeCursor(cursor);
			}
		}
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		if (!pref.getBoolean("fte", true)) {
			checkIfNoItems();
		}
		registerReceiver(my_msg_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_MSG_RECEIVED));
		registerReceiver(user_kicked_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_USER_KICKED));
		registerReceiver(room_name_changed_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_ROOM_NAME_CHANGED));
//		database.play_notification_sound_update(0);
	}
	@Override
	protected void onPause() {
		super.onPause();
//		database.play_notification_sound_update(1);
		unregisterReceiver(my_msg_receiver);
		unregisterReceiver(user_kicked_receiver);
		unregisterReceiver(room_name_changed_receiver);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if (pref.getBoolean("fte", true)) {
			Intent fte_intent = new Intent(MainActivity.this, FTE.class);
			startActivityForResult(fte_intent, 9);
		} else {
			/**
			 * In case of coming back from onStop() we'd like to refresh the list
			 * because the user might have received messages while MainActivity
			 * was in the background.
			 */
		//		Cursor cursor = database.get_all_channels();
		//		adapter.changeCursor(cursor);
	//		Bundle savedInstanceState = null;
	//		this.onCreate(savedInstanceState);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_host:
			Intent host_intent = new Intent(MainActivity.this, HostPage.class);
			startActivityForResult(host_intent, 10);
			break;
		case R.id.menu_join:
			Intent join_intent = new Intent(MainActivity.this, JoinPage.class);
			startActivityForResult(join_intent, 10);
			break;
		//case R.id.menu_settings:
	
//	break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
	//	setTheme(R.style.Theme_Base);
		getMenuInflater().inflate(R.menu.main_context_items_menu, menu);
		
		// if the channel is muted, change the 'mute' item text to "Unmute".
		if (database.is_channel_muted(database.get_channel_id(info.id))) {
			MenuItem mute_menu_item = (MenuItem) menu.findItem(R.id.main_mute_contextMenuItem);
			mute_menu_item.setTitle("Unmute");			
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo(); 
		final String channel_id = database.get_channel_id(info.id);
		switch (item.getItemId()) {
		case R.id.main_share_contextMenuItem:
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(channel_id);
			builder.setMessage("This room's ID is "+channel_id+".\n\nPeople can use this ID to join your room.\n");
			builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent share_intent = new Intent(Intent.ACTION_SEND);
					share_intent.setType("text/plain");
					share_intent.putExtra(Intent.EXTRA_TEXT, "Join my room! http://roomz.mobi/"+ channel_id);
					String title = "Select an app to invite someone";
					// Create intent to show chooser
					Intent chooser = Intent.createChooser(share_intent, title);
					// Verify the intent will resolve to at least one activity
					if (chooser.resolveActivity(getPackageManager()) != null) {
					    startActivity(chooser);
					} else {
						startActivity(share_intent);
					}
				}
			});
			builder.setNeutralButton("Copy", new DialogInterface.OnClickListener() {
				@SuppressLint("NewApi")
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int sdk = android.os.Build.VERSION.SDK_INT;
					if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
					    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					    clipboard.setText(channel_id);
					} else {
					    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
					    android.content.ClipData clip = android.content.ClipData.newPlainText("label",channel_id);
					    clipboard.setPrimaryClip(clip);
					}
					Toast.makeText(getApplicationContext(), "copied to clipboard", Toast.LENGTH_SHORT).show();
				}
			});
			builder.show();
			return true;
		case R.id.main_mute_contextMenuItem:
			if (database.is_channel_muted(database.get_channel_id(info.id))) {
				database.update_mute_column_UNMUTE(database.get_channel_id(info.id));
				Toast.makeText(getApplicationContext(), "un-muted", Toast.LENGTH_LONG).show();
				Cursor cursor = database.get_all_channels();
				adapter.changeCursor(cursor);
			} else {
	//			final CharSequence[] items = {" 8 Hours "," One week "," Forever "," And ever "};
				final CharSequence[] items = {" Forever "};
				new AlertDialog.Builder(MainActivity.this).setTitle("Mute")
				.setIcon(android.R.drawable.ic_lock_silent_mode)
				.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						Cursor cursor;
					/*	Intent alarmIntent = new Intent(MainActivity.this, MyMuteReceiver.class);
						PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
						AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
					*/
						switch(item) {
						case 0:
							// mute forever.
							database.update_mute_column_MUTE(database.get_channel_id(info.id));
							cursor = database.get_all_channels();
							adapter.changeCursor(cursor);
							Toast.makeText(getApplicationContext(), "muted", Toast.LENGTH_LONG).show();
							// mute 8 hours.
						/*	alarmIntent.putExtra("channel_id", database.get_channel_id(info.id));
							alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 28800000, pendingIntent);
							database.update_mute_column_MUTE(database.get_channel_id(info.id));
							cursor = database.get_all_channels();
							adapter.changeCursor(cursor);
							Toast.makeText(getApplicationContext(), "muted for 8 hours", Toast.LENGTH_LONG).show();
						*/
					//		Toast.makeText(getApplicationContext(), "only \"Forever\" works", Toast.LENGTH_LONG).show();
							break;
				/*		case 1:
							// mute one week.
							alarmIntent.putExtra("channel_id", database.get_channel_id(info.id));
							alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 604800000, pendingIntent);
							database.update_mute_column_MUTE(database.get_channel_id(info.id));
							cursor = database.get_all_channels();
							adapter.changeCursor(cursor);
							Toast.makeText(getApplicationContext(), "muted for one week", Toast.LENGTH_LONG).show();
						
							Toast.makeText(getApplicationContext(), "only \"Forever\" works", Toast.LENGTH_LONG).show();
							break;
						case 2:
							// mute forever.
							database.update_mute_column_MUTE(database.get_channel_id(info.id));
							cursor = database.get_all_channels();
							adapter.changeCursor(cursor);
							Toast.makeText(getApplicationContext(), "muted", Toast.LENGTH_LONG).show();
							break;
						case 3:
							// mute 5 seconds.
							
							alarmIntent.putExtra("channel_id", database.get_channel_id(info.id));
							alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent);
							database.update_mute_column_MUTE(database.get_channel_id(info.id));
							cursor = database.get_all_channels();
							adapter.changeCursor(cursor);
							Toast.makeText(getApplicationContext(), "muted for 5 seconds", Toast.LENGTH_LONG).show();
							Toast.makeText(getApplicationContext(), "only \"Forever\" works", Toast.LENGTH_LONG).show();
							break;*/
							
						}
						dialog.dismiss();   
					}
				}).create().show();
			}
			return true;
		case R.id.main_leave_contextMenuItem:
			if (isNetworkConnected()) {
				final String channel_name = database.get_channel_name(database.get_channel_id(info.id));
				new AlertDialog.Builder(MainActivity.this).setTitle(channel_name)
				.setMessage("Are you sure you want to leave?").setPositiveButton("Leave", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ParsePush.unsubscribeInBackground(channel_id);
						ParseObject	guestPointer = ParseObject.createWithoutData("Guest", database.get_guestId_in_channel(channel_id));
						guestPointer.deleteInBackground();
						
						String nickname = database.get_nickname_in_channel(channel_id);
						JSONObject data = new JSONObject();
						try {
							data.put("action", PUSH_ACTION_SYSTEM_MESSAGE);
							data.put("channel_name", channel_name);
							data.put("nickname", nickname);
							data.put("message", PUSH_KEYPHRASE_USER_LEFT_ROOM);
							data.put("message_time", System.currentTimeMillis());
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
						ParsePush push = new ParsePush();
						push.setChannel(channel_id);
						push.setData(data);
						push.sendInBackground();
						
						database.leave_room(channel_id);
						
						// delete wallpaper.
						try {
							String root = Environment.getExternalStorageDirectory().toString();
							File myDir = new File(root + "/roomz_wallpapers");    
							myDir.mkdirs();
							String fname = channel_id+".jpg";
							File file = new File (myDir, fname);
							if (file.exists ())	{
								file.delete ();
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
						
						Toast.makeText(MainActivity.this, "left " + channel_name, Toast.LENGTH_LONG).show();
						ParseAnalytics.trackEvent("main_context_leave");
						Cursor cursor = database.get_all_channels();
						adapter.changeCursor(cursor);
						checkIfNoItems();
					}
				}).setNeutralButton("Cancel", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create().show();
			}
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	private boolean isNetworkConnected() {
		  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni == null) {
		   // There are no active networks.
			  ContextThemeWrapper ctw = new ContextThemeWrapper(MainActivity.this, R.style.Theme_Base);
				AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setTitle("No connection.");
				builder.setMessage("An internet connection is required to leave a room.");
				builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.show();
		   return false;
		  } else {
			  return true;
		  }
	 }
}

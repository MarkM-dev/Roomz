package mobi.roomz.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

import mobi.roomz.R;
import mobi.roomz.db.DBHandler;
import mobi.roomz.db.consts_interface;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.parse.SendCallback;

@SuppressLint("NewApi")
public class ChatInfo extends Activity implements consts_interface, OnClickListener {

	private Intent intent;
	private String channel_id, my_guest_id;
	private DBHandler database = null;
	private TextView change_nickname_textView, change_roomName_textView, room_id_textView, wipe_room_textView, secure_room_textView;
	private ListView listView;
	private UsersCursorAdapter adapter;
	private static final int SELECT_WALLPAPER = 1;
	
	private BroadcastReceiver room_name_changed_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String new_room_name = database.get_channel_name(channel_id);
			Toast.makeText(getApplicationContext(), "room name changed to " + new_room_name, Toast.LENGTH_LONG).show();
			update_roomName_button_text(new_room_name);
		}
	};
	
	private BroadcastReceiver nickname_changed_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// user (self) changed nickname.
			if (intent.getStringExtra("guest_id").equals(database.get_guestId_in_channel(channel_id))) {
				set_nickname_button_text();
				Toast.makeText(getApplicationContext(), "nickname changed", Toast.LENGTH_LONG).show();
				Cursor cursor = database.get_all_users_in_channel(channel_id);
				adapter.changeCursor(cursor);
				// room-mate changed nickname.
			} else {
				Cursor cursor = database.get_all_users_in_channel(channel_id);
				adapter.changeCursor(cursor);
			}
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
				Intent mainActivity_user_kicked_intent = new Intent(ChatInfo.this, MainActivity.class);
				mainActivity_user_kicked_intent.putExtra("channel_id", _channel_id);
				mainActivity_user_kicked_intent.putExtra("guest_id", _guest_id);
				mainActivity_user_kicked_intent.putExtra("message", message);
				mainActivity_user_kicked_intent.putExtra("user_was_kicked", true);
				startActivity(mainActivity_user_kicked_intent);
				finish();
			} else {
				// room mate was kicked.
				refresh_channel_users_from_parse_and_refresh_cursor();
			}
		}
	};
	
	
	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_info);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("Room Service");
		actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.MyBackground_orange_red)));
		actionBar.setIcon(R.drawable.icon);
		
		intent = getIntent();
		channel_id = intent.getStringExtra("channel_id");
		database = new DBHandler(this);
		
		if (database.is_channel_secured(channel_id)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}
		
		room_id_textView = (TextView)findViewById(R.id.chatInfo_roomId_button);
		change_nickname_textView = (TextView)findViewById(R.id.chatInfo_change_nickname_textView);
		change_roomName_textView = (TextView)findViewById(R.id.chatInfo_change_roomName_textView);
		TextView change_wallpaper_textView = (TextView)findViewById(R.id.chatInfo_change_wallpaper_textView);
		TextView clear_room_messages_textView = (TextView)findViewById(R.id.chatInfo_clear_room_messages_textView);
		wipe_room_textView = (TextView)findViewById(R.id.chatInfo_wipe_room_textView);
		secure_room_textView = (TextView)findViewById(R.id.chatInfo_secure_room_textView);
	
		listView = (ListView) findViewById(R.id.chatInfo_listView);
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openContextMenu(view);
			}
		});
		
		my_guest_id = database.get_guestId_in_channel(channel_id);
		
		Cursor cursor = database.get_all_users_in_channel(channel_id);
		adapter = new UsersCursorAdapter(this, cursor);
		listView.setAdapter(adapter);
		
		
		set_roomId_button_text();
		set_roomName_button_text();
		set_nickname_button_text();
		set_wipeRoom_button_text();
		set_secureRoom_button_text();
		
		room_id_textView.setOnClickListener(this);
		change_roomName_textView.setOnClickListener(this);
		change_nickname_textView.setOnClickListener(this);
		clear_room_messages_textView.setOnClickListener(this);
		change_wallpaper_textView.setOnClickListener(this);
		wipe_room_textView.setOnClickListener(this);
		secure_room_textView.setOnClickListener(this);
		refresh_channel_users_from_parse_and_refresh_cursor();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(room_name_changed_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_ROOM_NAME_CHANGED));
		registerReceiver(nickname_changed_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_NICKNAME_CHANGED));
		registerReceiver(user_kicked_receiver, new IntentFilter(PUSH_INNERRECEIVER_INTENT_USER_KICKED));
//		database.play_notification_sound_update(0);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(room_name_changed_receiver);
		unregisterReceiver(nickname_changed_receiver);
		unregisterReceiver(user_kicked_receiver);
//		database.play_notification_sound_update(1);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.chatInfo_roomId_button:
//			ContextThemeWrapper ctw = new ContextThemeWrapper(ChatPage.this, R.style.Theme_Base);
			AlertDialog.Builder builder = new AlertDialog.Builder(ChatInfo.this);
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(channel_id);
			builder.setMessage("This room's ID is "+channel_id+".\n\nPeople can use this ID to join your room.\n");
			builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent share_intent = new Intent(Intent.ACTION_SEND);
					share_intent.setType("text/plain");
					share_intent.putExtra(Intent.EXTRA_TEXT, "Join my room! roomz.mobi/"+ channel_id);
					startActivity(share_intent);
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
			break;
		case R.id.chatInfo_change_nickname_textView:
			change_nickname();
			break;
		case R.id.chatInfo_change_roomName_textView:
			change_room_name();
			break;
		case R.id.chatInfo_clear_room_messages_textView:
			clear_room_messages();
			break;
		case R.id.chatInfo_change_wallpaper_textView:
			change_wallpaper();
			break;
		case R.id.chatInfo_wipe_room_textView:
			wipe_room();
			break;
		case R.id.chatInfo_secure_room_textView:
			secure_room();
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_info_optionsmenu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case android.R.id.home:
			finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		// checks which menu to inflate.
		if (database.is_admin(database.get_guestId_in_channel(channel_id))) {
			if (!database.get_guestId(info.id).equals(database.get_guestId_in_channel(channel_id))) {
				getMenuInflater().inflate(R.menu.chat_info_admin_contextmenu, menu);	
			}
		} else {
			
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo(); 
		switch (item.getItemId()) {
		case R.id.chatInfo_admin_kick_menuItem:
			
				String nickname = database.get_nickname(info.id);
				new AlertDialog.Builder(ChatInfo.this)
				.setTitle(nickname)
				.setMessage("Kick " + nickname + "?")
				.setPositiveButton("Kick", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (isNetworkConnected()) {
							kick_user(info);
						}
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create().show();
				
			
			break;
		
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	private void kick_user(AdapterContextMenuInfo info) {
		ParseObject	guestPointer = ParseObject.createWithoutData("Guest", database.get_guestId(info.id));
		guestPointer.deleteInBackground();
		
		String kicked_user_nickname = database.get_nickname(info.id);
		JSONObject data = new JSONObject();
		try {
			data.put("action", PUSH_ACTION_KICK_USER);
			data.put("nickname", database.get_nickname_in_channel(channel_id));
			data.put("kicked_user_nickname", kicked_user_nickname);
			data.put("guest_id", database.get_guestId(info.id));
			data.put("channel_name", database.get_channel_name(channel_id));
			data.put("message", PUSH_KEYPHRASE_USER_KICKED);
			data.put("message_time", System.currentTimeMillis());
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		ParsePush push = new ParsePush();
		push.setChannel(channel_id);
		push.setData(data);
		push.sendInBackground();
		ParseAnalytics.trackEvent("admin_kick");
		Toast.makeText(getApplicationContext(), kicked_user_nickname + " was kicked", Toast.LENGTH_LONG).show();
	}

	private void refresh_channel_users_from_parse_and_refresh_cursor () {
		if (isNetworkConnected()) {
			ParseObject	roomPointer = ParseObject.createWithoutData("Room", channel_id);
			ParseQuery<ParseObject> get_users_query = ParseQuery.getQuery("Guest");
			get_users_query.whereEqualTo("room", roomPointer);
			get_users_query.findInBackground(new FindCallback<ParseObject>() {
				@Override
				public void done(List<ParseObject> objects, ParseException e) {
					if (e == null) {
						database.delete_users_table(channel_id);
						for (int i = 0; i < objects.size(); i++) {
	                        Object object = objects.get(i);
	                        String nickname = ((ParseObject) object).getString("nickname");
	                        int admin = ((ParseObject) object).getInt("admin");
	                        long last_seen = ((ParseObject) object).getLong("last_seen");
	                        String guest_id = ((ParseObject) object).getObjectId();
	                        database.addChannelUser(channel_id, nickname, guest_id, admin, last_seen);
	                   }
						Cursor cursor = database.get_all_users_in_channel(channel_id);
						adapter.changeCursor(cursor);
						
						// If the user is admin, show the muffle button.
						if (database.is_admin(database.get_guestId_in_channel(channel_id))) {
							wipe_room_textView.setVisibility(1);
							secure_room_textView.setVisibility(1);
						} else {
							wipe_room_textView.setVisibility(View.GONE);
							secure_room_textView.setVisibility(View.GONE);
						}
					}
				}
			});
		}
	}
	
	private class UsersCursorAdapter extends CursorAdapter implements consts_interface {
		final LayoutInflater inflator;
		
		public UsersCursorAdapter(Context context, Cursor c) {
			super(context, c, true);
			inflator = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ChatInfo_user_list_item_viewHolder viewHolder = (ChatInfo_user_list_item_viewHolder) view.getTag();
			viewHolder.set_user_item_textView(cursor.getString(cursor.getColumnIndex(NICK_NAME)));
			// if the user is self.
			if (cursor.getString(cursor.getColumnIndex(GUEST_ID)).equals(my_guest_id)) {
				viewHolder.setTimeStamp_string("(You)");
				
			} else {
/////////////////////////// Time display ////////////////////////////////////////////
				long last_seen = cursor.getLong(cursor.getColumnIndex(LAST_SEEN));
				// if user is online.
				if (last_seen == 0) {
					viewHolder.setTimeStamp_online();
				} else {
					Calendar c = Calendar.getInstance();
					c.getTimeInMillis();
					String current_day = String.format("%te",c); // This will give date like 22 February 2012
					String current_month = String.format("%B",c);
					String current_year = String.format("%tY",c);
					
					c.setTimeInMillis(last_seen);//set your saved timestamp
					String that_day=String.format("%te",c); //this will convert timestamp into format like 22 February 2012
					String that_month = String.format("%B",c);
					String that_year = String.format("%tY",c);
					
					if (that_day.equals(current_day) && that_month.equals(current_month) && that_year.equals(current_year)) {
						viewHolder.setTimeStamp_time(cursor.getLong(cursor.getColumnIndex(LAST_SEEN)));
					} else {
						if (Integer.valueOf(that_day) == (Integer.valueOf(current_day)-1) && that_month.equals(current_month) && that_year.equals(current_year)) {
							viewHolder.setTimeStamp_yesterday(cursor.getLong(cursor.getColumnIndex(LAST_SEEN)));
						} else {
							viewHolder.setTimeStamp_day(cursor.getLong(cursor.getColumnIndex(LAST_SEEN)));
						}
					}
				}
/////////////////////////// Time display ////////////////////////////////////////////
				
				
			}
			
			// if admin.
			if (cursor.getInt(cursor.getColumnIndex(ADMIN)) == 1) {
				viewHolder.set_user_item_admin_textView("HOST");
			} else {
				viewHolder.set_user_item_admin_textView("");
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
			View view = inflator.inflate(R.layout.chatinfo_user_list_item_layout, null);
			ChatInfo_user_list_item_viewHolder viewHolder = new ChatInfo_user_list_item_viewHolder(view);
			view.setTag(viewHolder);

			return view;
		}

	}
	
	private boolean isNetworkConnected() {
		  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni == null) {
		   // There are no active networks.
			  ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
			  AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
			  builder.setIcon(android.R.drawable.ic_dialog_alert);
			  builder.setTitle("No connection.");
			  builder.setMessage("An internet connection is required for this action.");
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
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////// Change wall-paper functions //////////////////////////////////////////////
	public void change_wallpaper () {
		// select a file
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select wallpaper"), SELECT_WALLPAPER);
        
        
	}
	
	 public void onActivityResult(int requestCode, int resultCode, Intent data) {
	        if (resultCode == RESULT_OK) {
	            if (requestCode == SELECT_WALLPAPER) {
	            	Uri selectedImageUri = data.getData();
	            	
	            	String image_path = "path";
	            	if (Build.VERSION.RELEASE.equals("4.4.2")){
	            		image_path = getImagePath(selectedImageUri);    
	                } else {
	                    if (Build.VERSION.RELEASE.equals("4.4.4")) {
	    	                image_path = getImagePath_kitkat(selectedImageUri);
						} else {
							 if (Build.VERSION.SDK_INT <19){
								 image_path = getImagePath(selectedImageUri);
							 } else {
					                image_path = getImagePath_kitkat(selectedImageUri);
						        }
						}
	                }
	            	
	        		Bitmap selected_wallpaper_bitmap = BitmapFactory.decodeFile(image_path);
	            	SaveImage(selected_wallpaper_bitmap);
	            }
	        }
	    }
	 
	 private String getImagePath_kitkat(Uri uri) {

		    String id = uri.getLastPathSegment().split(":")[1]; 
		    final String[] imageColumns = {MediaStore.Images.Media.DATA };
		    final String imageOrderBy = null;

		    Uri uri1 = getUri();
		    String selectedImagePath = "path";

		    Cursor imageCursor = managedQuery(uri1, imageColumns,
		          MediaStore.Images.Media._ID + "="+id, null, imageOrderBy);

		    if (imageCursor.moveToFirst()) {
		        selectedImagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
		        return selectedImagePath;
		    } else {
				return null;
			}
	 }
	// By using this method get the Uri of Internal/External Storage for Media
	 private Uri getUri() {
	     String state = Environment.getExternalStorageState();
	     if(!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED))
	         return MediaStore.Images.Media.INTERNAL_CONTENT_URI;

	     return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	 }
	 
	  /**
	     * helper to retrieve the path of an image URI
	     */
	    public String getImagePath(Uri uri) {
    		String[] fileColumn = { MediaStore.Images.Media.DATA };
    		Cursor imageCursor = getContentResolver().query(uri, fileColumn, null, null, null);
    		imageCursor.moveToFirst();
    		String picturePath = imageCursor.getString(0);
    		return picturePath;
	    }
	
	// saves image to app's image dir.
	private void SaveImage(Bitmap bitmap) {
	    String root = Environment.getExternalStorageDirectory().toString();
	    File myDir = new File(root + "/roomz_wallpapers");    
	    myDir.mkdirs();
	    
    	String fname = channel_id + ".jpg";
		
	    File file = new File (myDir, fname);
	    if (file.exists ()) file.delete (); 
	    try {
           FileOutputStream out = new FileOutputStream(file);
           bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
           out.flush();
           out.close();
           database.update_wallpaper_value(channel_id, fname);
           ParseAnalytics.trackEvent("wallpaper_change");
           Toast.makeText(ChatInfo.this, "wallpaper changed", Toast.LENGTH_LONG).show();
	    } catch (Exception e) {
	           e.printStackTrace();
	           Toast.makeText(ChatInfo.this, "something went wrong - wallpaper not changed", Toast.LENGTH_LONG).show();
	    }
	}
		///////////////////////////////////////// Change wall-paper functions //////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void set_roomId_button_text () {
		// make the 10 char id bold.
		SpannableStringBuilder sb_roomId = new SpannableStringBuilder("Room ID: " + channel_id);
		StyleSpan iss = new StyleSpan(android.graphics.Typeface.BOLD);
		sb_roomId.setSpan(iss, 9, 19, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // make last 10 characters Bold.
		room_id_textView.setText(sb_roomId);
	}
	
	private void set_roomName_button_text () {
		String room_name = database.get_channel_name(channel_id);
		int room_name_length = room_name.length();
		// make the room name bold.
		SpannableStringBuilder sb_roomName = new SpannableStringBuilder("Room name: " + room_name);
		StyleSpan iss = new StyleSpan(android.graphics.Typeface.BOLD);
		sb_roomName.setSpan(iss, 11, 11+room_name_length, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // make last 10 characters Bold.
		change_roomName_textView.setText(sb_roomName);
	}
	
	private void update_roomName_button_text (String new_room_name) {
		int new_room_name_length = new_room_name.length();
		// make the room name bold.
		SpannableStringBuilder sb_new_roomName = new SpannableStringBuilder("Room name: " + new_room_name);
		StyleSpan iss = new StyleSpan(android.graphics.Typeface.BOLD);
		sb_new_roomName.setSpan(iss, 11, 11+new_room_name_length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		change_roomName_textView.setText(sb_new_roomName);
	}
	
	private void set_nickname_button_text () {
		String nick_in_channel = database.get_nickname_in_channel(channel_id);
		int nick_length = nick_in_channel.length();
		// make the nickname bold.
		SpannableStringBuilder sb_nickname = new SpannableStringBuilder("Nickname: " + nick_in_channel);
		StyleSpan iss = new StyleSpan(android.graphics.Typeface.BOLD);
		sb_nickname.setSpan(iss, 10, 10+nick_length, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // make last 10 characters Bold.
		change_nickname_textView.setText(sb_nickname);
	}
	
	private void set_wipeRoom_button_text () {
		final SpannableStringBuilder sb = new SpannableStringBuilder("Host - Wipe Room");
		final ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(202,67,67)); 
		// Span to set text color to some RGB value
	//	final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD); 
		// Span to make text bold
		sb.setSpan(fcs, 0, 6, Spannable.SPAN_INCLUSIVE_INCLUSIVE); 
		// Set the text color for first 6 characters
	//	sb.setSpan(bss, 0, 6, Spannable.SPAN_INCLUSIVE_INCLUSIVE); 
		wipe_room_textView.setText(sb);
	}
	
	private void set_secureRoom_button_text () {
		final SpannableStringBuilder sb = new SpannableStringBuilder("Host - Secure Room");
		final ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(202,67,67)); 
		// Span to set text color to some RGB value
	//	final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD); 
		// Span to make text bold
		sb.setSpan(fcs, 0, 6, Spannable.SPAN_INCLUSIVE_INCLUSIVE); 
		// Set the text color for first 6 characters
	//	sb.setSpan(bss, 0, 6, Spannable.SPAN_INCLUSIVE_INCLUSIVE); 
		secure_room_textView.setText(sb);
	}
	
	private void change_room_name () {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(ChatInfo.this);
        alertDialog.setTitle(database.get_channel_name(channel_id));
        alertDialog.setMessage("Enter new room name:");
        final EditText room_user_input = new EditText(ChatInfo.this);  
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                              LinearLayout.LayoutParams.WRAP_CONTENT,
                              LinearLayout.LayoutParams.WRAP_CONTENT);
        room_user_input.setLayoutParams(lp);
        room_user_input.setText(database.get_channel_name(channel_id));
        room_user_input.selectAll();
        alertDialog.setView(room_user_input); 
        alertDialog.setIcon(android.R.drawable.ic_menu_edit);
        alertDialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
   	 			public void onClick(DialogInterface dialog,int which) {
   	 			// check if nickname field is empty.
            		final String new_room_name = room_user_input.getText().toString().trim();
            		if (new_room_name.equals("")) {
            			Toast toast = Toast.makeText(ChatInfo.this, "room name cannot be empty", Toast.LENGTH_LONG);
    					toast.setGravity(Gravity.CENTER, 0, 0);
    					toast.show();
    					change_room_name();
						} else {
							if (isNetworkConnected()) {
								// show loading pop-up.
								ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
								final ProgressDialog progress = new ProgressDialog(ctw);
								progress.setMessage("changing room name..");
								progress.setCancelable(false);
								progress.show();
								
								// change nickname in parse database.
								ParseObject	roomtPointer = ParseObject.createWithoutData("Room", channel_id);
								roomtPointer.put("name", new_room_name);
								roomtPointer.saveInBackground(new SaveCallback() {
									@Override
									public void done(ParseException e) {
										if (e == null) {
											// send push about room name change.
											JSONObject data = new JSONObject();
											try {
												data.put("action", PUSH_ACTION_ROOM_NAME_CHANGE);
												data.put("channel_name", database.get_channel_name(channel_id));
												data.put("nickname", database.get_nickname_in_channel(channel_id));
												data.put("new_room_name", new_room_name);
												data.put("message", PUSH_KEYPHRASE_ROOMNAME_CHANGE);
												data.put("message_time", System.currentTimeMillis());
											} catch (JSONException e1) {
												e1.printStackTrace();
											}
											ParsePush push = new ParsePush();
											push.setChannel(channel_id);
											push.setData(data);
											push.sendInBackground(new SendCallback() {
												@Override
												public void done(ParseException e) {
													ParseAnalytics.trackEvent("room_name_change");
													progress.dismiss();
												}
											});
										} else {
											Toast.makeText(getApplicationContext(), "couldn't change nickname - check connection.", Toast.LENGTH_LONG).show();
										}
									}
								});
							}
						}
                    }
                });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
        
        // pop up keyboard.
        room_user_input.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(room_user_input, 0);
            }
        },200);
	}
	
	private void change_nickname () {
		
		 AlertDialog.Builder alertDialog = new AlertDialog.Builder(ChatInfo.this);
         alertDialog.setTitle(database.get_nickname_in_channel(channel_id));
         alertDialog.setMessage("Enter your new nickname:");
         final EditText nickname_user_input = new EditText(ChatInfo.this);
         nickname_user_input.setText(database.get_nickname_in_channel(channel_id));
         nickname_user_input.selectAll();
         LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                               LinearLayout.LayoutParams.WRAP_CONTENT,
                               LinearLayout.LayoutParams.WRAP_CONTENT);
         nickname_user_input.setLayoutParams(lp);
         alertDialog.setView(nickname_user_input); 
         alertDialog.setIcon(android.R.drawable.ic_menu_edit);
         alertDialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
    	 			public void onClick(DialogInterface dialog,int which) {
    	 				
    	 				final String new_nickname = nickname_user_input.getText().toString().trim();
    	 				// check if nickname field is empty.
						if (new_nickname.equals("")) {
							Toast toast = Toast.makeText(ChatInfo.this, "nickname field cannot be empty", Toast.LENGTH_LONG);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							change_nickname();
						} else {
								
	    	 				// check if user is trying to change to the same nickname.
	    	 				if (database.check_if_new_nickname_is_same(channel_id, new_nickname)) {
	    	 					ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
								AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
								builder.setIcon(android.R.drawable.ic_dialog_alert);
								builder.setTitle("Same nickname.");
								builder.setMessage("your nickname is already '" + new_nickname + "'.");
								builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
								builder.show();
							
							} else {
								
								if (database.check_if_nickname_exists_in_channel (channel_id , new_nickname)) {
									ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
									AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
									builder.setIcon(android.R.drawable.ic_dialog_alert);
									builder.setTitle("Duplicate nickname.");
									builder.setMessage("'" + new_nickname + "' already exists in the room.");
									builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
										}
									});
									builder.show();
								} else {
									if (isNetworkConnected()) {
										
										// show loading pop-up.
										ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
										final ProgressDialog progress = new ProgressDialog(ctw);
										progress.setMessage("changing nickname..");
										progress.setCancelable(false);
										progress.show();
										
										// change nickname in parse database.
										ParseObject	guestPointer = ParseObject.createWithoutData("Guest", database.get_guestId_in_channel(channel_id));
										guestPointer.put("nickname", new_nickname);
										guestPointer.saveInBackground(new SaveCallback() {
											@Override
											public void done(ParseException e) {
												if (e == null) {
													// send push about nickname change.
													JSONObject data = new JSONObject();
													try {
														data.put("action", PUSH_ACTION_NICK_CHANGE);
														data.put("channel_name", database.get_channel_name(channel_id));
														data.put("old_nickname", database.get_nickname_in_channel(channel_id));
														data.put("nickname", new_nickname);
														data.put("guest_id", database.get_guestId_in_channel(channel_id));
														data.put("message",  PUSH_KEYPHRASE_NICK_CHANGE);
														data.put("message_time", System.currentTimeMillis());
													} catch (JSONException e1) {
														e1.printStackTrace();
													}
													ParsePush push = new ParsePush();
													push.setChannel(channel_id);
													push.setData(data);
													push.sendInBackground(new SendCallback() {
														@Override
														public void done(ParseException e) {
															if (e == null) {
																progress.dismiss();
																ParseAnalytics.trackEvent("nickname_change");
															}
														}
													});
												} else {
													Toast.makeText(getApplicationContext(), "couldn't change nickname - check connection.", Toast.LENGTH_LONG).show();
												}
											}
										});
										
									}
									
								}
								
								
							}
	    	 			}
                     }
                 });
         alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         dialog.cancel();
                     }
                 });
         alertDialog.show();
         nickname_user_input.postDelayed(new Runnable() {
             @Override
             public void run() {
                 InputMethodManager keyboard = (InputMethodManager)
                 getSystemService(Context.INPUT_METHOD_SERVICE);
                 keyboard.showSoftInput(nickname_user_input, 0);
             }
         },200);
	}
	
	private void clear_room_messages() {
		new AlertDialog.Builder(ChatInfo.this)
		.setIcon(android.R.drawable.ic_menu_delete)
		.setTitle(database.get_channel_name(channel_id))
		.setMessage("clear room messages?").setPositiveButton("Clear", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				database.clear_chat_messages(channel_id);
				ParseAnalytics.trackEvent("clear_room_messages");
				Toast.makeText(ChatInfo.this, "messages cleared", Toast.LENGTH_LONG).show();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create().show();
	}
	
	private void wipe_room() {
		new AlertDialog.Builder(ChatInfo.this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Wipe")
		.setMessage("This clears all the messages from this room for everyone in it.\n\nWipe room?").setPositiveButton("Wipe", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (isNetworkConnected()) {
					// show loading pop-up.
					ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
					final ProgressDialog progress = new ProgressDialog(ctw);
					progress.setMessage("Wiping..");
					progress.setCancelable(false);
					progress.show();
					// send push about room name change.
					JSONObject data = new JSONObject();
					try {
						data.put("action", PUSH_ACTION_SYSTEM_MESSAGE);
						data.put("nickname", database.get_nickname_in_channel(channel_id));
						data.put("message", PUSH_KEYPHRASE_WIPE_ROOM);
						data.put("message_time", System.currentTimeMillis());
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
					ParsePush push = new ParsePush();
					push.setChannel(channel_id);
					push.setData(data);
					push.sendInBackground(new SendCallback() {
						@Override
						public void done(ParseException e) {
							if (e == null) {
								database.clear_chat_messages_by_admin(channel_id);
								progress.dismiss();
								ParseAnalytics.trackEvent("wipe_room");
								Toast.makeText(ChatInfo.this, "room wiped", Toast.LENGTH_LONG).show();	
							} else {
								Toast.makeText(getApplicationContext(), "something went wrong - room not wiped.", Toast.LENGTH_LONG).show();
							}
						}
					});
					
				}
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create().show();
	}
	
	private void secure_room() {
		Builder secure_alert_dialog = new AlertDialog.Builder(ChatInfo.this);
		secure_alert_dialog.setIcon(android.R.drawable.ic_dialog_alert);
		if (database.is_channel_secured(channel_id)) {
			secure_alert_dialog.setTitle(database.get_channel_name(channel_id));
			secure_alert_dialog.setMessage("In a secured room, users cannot take screenshots.\n\nRoom is secured.");
			secure_alert_dialog.setPositiveButton("Un-secure", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (isNetworkConnected()) {
						// show loading pop-up.
						ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
						final ProgressDialog progress = new ProgressDialog(ctw);
						progress.setMessage("changing room to Non-Secured..");
						progress.setCancelable(false);
						progress.show();
						
						// change to non-secured in parse database.
						ParseObject	roomtPointer = ParseObject.createWithoutData("Room", channel_id);
						roomtPointer.put("secured", 0);
						roomtPointer.saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									// send push about room name change.
									JSONObject data = new JSONObject();
									try {
										data.put("action", PUSH_ACTION_SYSTEM_MESSAGE);
										data.put("nickname", database.get_nickname_in_channel(channel_id));
										data.put("secured", 0);
										data.put("message", PUSH_KEYPHRASE_SECURED_ROOM);
										data.put("message_time", System.currentTimeMillis());
									} catch (JSONException e1) {
										e1.printStackTrace();
									}
									ParsePush push = new ParsePush();
									push.setChannel(channel_id);
									push.setData(data);
									push.sendInBackground(new SendCallback() {
										@Override
										public void done(ParseException e) {
											if (e == null) {
												progress.dismiss();
												ParseAnalytics.trackEvent("un-secure_room");
												Toast.makeText(ChatInfo.this, "room is now Non-Secure.", Toast.LENGTH_LONG).show();	
											} else {
												Toast.makeText(getApplicationContext(), "something went wrong - changes didn't take effect.", Toast.LENGTH_LONG).show();
											}
										}
									});
								} else {
									Toast.makeText(getApplicationContext(), "changes didn't take effect. - check connection.", Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				}
			});
		} else {
			secure_alert_dialog.setTitle(database.get_channel_name(channel_id));
			secure_alert_dialog.setMessage("In a secured room, users cannot take screenshots.\n\nRoom is not secured.");
			secure_alert_dialog.setPositiveButton("Secure", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (isNetworkConnected()) {
						// show loading pop-up.
						ContextThemeWrapper ctw = new ContextThemeWrapper(ChatInfo.this, R.style.Theme_Base);
						final ProgressDialog progress = new ProgressDialog(ctw);
						progress.setMessage("changing room to Secured..");
						progress.setCancelable(false);
						progress.show();
						
						// change to non-secured in parse database.
						ParseObject	roomtPointer = ParseObject.createWithoutData("Room", channel_id);
						roomtPointer.put("secured", 1);
						roomtPointer.saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									// send push about room name change.
									JSONObject data = new JSONObject();
									try {
										data.put("action", PUSH_ACTION_SYSTEM_MESSAGE);
										data.put("nickname", database.get_nickname_in_channel(channel_id));
										data.put("secured", 1);
										data.put("message", PUSH_KEYPHRASE_SECURED_ROOM);
										data.put("message_time", System.currentTimeMillis());
									} catch (JSONException e1) {
										e1.printStackTrace();
									}
									ParsePush push = new ParsePush();
									push.setChannel(channel_id);
									push.setData(data);
									push.sendInBackground(new SendCallback() {
										@Override
										public void done(ParseException e) {
											if (e == null) {
												progress.dismiss();
												ParseAnalytics.trackEvent("secure_room");
												Toast.makeText(ChatInfo.this, "room is now Secured.", Toast.LENGTH_LONG).show();	
											} else {
												Toast.makeText(getApplicationContext(), "something went wrong - changes didn't take effect.", Toast.LENGTH_LONG).show();
											}
										}
									});
								} else {
									Toast.makeText(getApplicationContext(), "changes didn't take effect. - check connection.", Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				}
			});
		}
		secure_alert_dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create().show();
	}
}

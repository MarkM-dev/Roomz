package mobi.roomz.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 *         Database structure: 3 tables:
 * 
 *         channels_table (_id, channel_id, channel_name, nick_in_channel, guest_id, wallpaper, last_seen, msg_unread_counter, secured, mute);
 *         channel_messages_table (_id, channel_id, nickname, time, message, guest_id, sent);
 *         channel_users_table (_id, channel_id, nickname, guest_id, admin, last_seen); for @ tagging.
 *         
 *         notification_table (_id, channel_id, play_notification);
 * 									// 1 or 0.		
 * 
 *         channel_id refers to the auto-incremental id given by the cloud
 *         service.
 * 
 */
public class DBHelper extends SQLiteOpenHelper implements consts_interface {

	public DBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create_channels_table = "CREATE TABLE " 
				+ CHANNELS_TABLE + " ("
				+ _ID + " INTEGER PRIMARY KEY , " 
				+ CHANNEL_ID + " TEXT , "
				+ CHANNEL_NAME + " TEXT , " 
				+ NICK_IN_CHANNEL + " TEXT , " 
				+ GUEST_ID + " TEXT , " 
				+ WALLPAPER + " TEXT , " 
				+ LAST_SEEN + " INTEGER , " 
				+ MSG_UNREAD_COUNTER + " INTEGER , "
				+ SECURED + " INTEGER , "
				+ MUTE + " INTEGER );";

		String create_channel_messages_table = "CREATE TABLE "
				+ CHANNEL_MESSAGES_TABLE + " (" 
				+ _ID + " INTEGER PRIMARY KEY , " 
				+ CHANNEL_ID + " TEXT , "
				+ NICK_NAME + " TEXT , " 
				+ TIME + " TIMESTAMP , "
				+ MESSAGE + " TEXT , "
				+ GUEST_ID + " TEXT , "
				+ SENT + " INTEGER );";

		String create_channel_user_table = "CREATE TABLE "
				+ CHANNEL_USERS_TABLE + " (" 
				+ _ID + " INTEGER PRIMARY KEY , "
				+ CHANNEL_ID + " TEXT , " 
				+ NICK_NAME + " TEXT , " 
				+ GUEST_ID + " TEXT , " 
				+ ADMIN + " TEXT , " 
				+ LAST_SEEN + " INTEGER );";
		
		String create_notification_table = "CREATE TABLE "
				+ NOTIFICATION_TABLE + " (" 
				+ _ID + " INTEGER PRIMARY KEY , "
				+ CHANNEL_ID + " TEXT , " 
				+ GENERATE_NOTIFICATION + " INTEGER , " 
				+ PLAY_NOTIFICATION + " INTEGER );";
		
		try {
			db.execSQL(create_channels_table);
			db.execSQL(create_channel_messages_table);
			db.execSQL(create_channel_user_table);
			db.execSQL(create_notification_table);
		} catch (SQLiteException e) {
			e.getCause();
			Log.e("DBHelper", "COULD NOT CREATE TABLES");
		}

	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		 
	}
	
	
}

package mobi.roomz.db;

import java.util.ArrayList;
import java.util.List;

public interface consts_interface {

	public final static int DB_VERSION = 1;
	
	// db consts.
	public static final String ROOMZ_DB_NAME = "roomzdb.db";
	public static final String _ID = "_id";
	// channels table.
	public static final String PARSE_CHANNELS_TABLE = "channels";
	public static final String PARSE_CHANNELS_USERS_CULOMN = "users";
	public static final String PARSE_CHANNEL_MESSAGES_TABLE = "channelMessages";
	
	public static final String CHANNELS_TABLE = "channels_table";
	public static final String CHANNEL_ID = "channel_id";
	public static final String CHANNEL_NAME = "channel_name";
	public static final String NICK_IN_CHANNEL = "nick_in_channel";
	public static final String GUEST_ID = "guest_id";
	public static final String ADMIN = "admin";
	public static final String WALLPAPER = "wallpaper";
	public static final String LAST_SEEN = "last_seen";
	public static final String MSG_UNREAD_COUNTER = "msg_unread_counter";
	public static final String SECURED = "secured";
	public static final String MUTE = "mute";
	
	// channel messages table.
	public static final String CHANNEL_MESSAGES_TABLE = "channel_messages_table";
	public static final String NICK_NAME = "nickname";
	public static final String TIME = "time";
	public static final String MESSAGE = "message";
	public static final String SENT = "sent";

	// play notificati
	public static final String NOTIFICATION_TABLE = "notification_table";
	public static final String GENERATE_NOTIFICATION = "generate_notification";
	public static final String PLAY_NOTIFICATION = "play_notification";
	public static final int PLAY = 1;
	public static final int DONT_PLAY = 0;
	
	
	// channel users table.
	public static final String CHANNEL_USERS_TABLE = "channel_users_table";
	
	// Parse push actins
	public static final String PUSH_ACTION_KICK_USER = "mobi.roomz.KICK_USER";
	public static final String PUSH_ACTION_SYSTEM_MESSAGE = "mobi.roomz.SYSTEM_MESSAGE";
	public static final String PUSH_ACTION_ROOM_NAME_CHANGE = "mobi.roomz.ROOM_NAME_CHANGE";
	public static final String PUSH_ACTION_NICK_CHANGE = "mobi.roomz.NICK_CHANGE";
	public static final String PUSH_ACTION_ROOMMATE_MESSAGE = "mobi.roomz.ROOMMATE_MESSAGE";
	public static final String PUSH_INNERRECEIVER_INTENT_MSG_RECEIVED = "mobi.roomz.INNERRECEIVER_ROOMMATE_MESSAGE";
	public static final String PUSH_INNERRECEIVER_INTENT_ROOM_NAME_CHANGED = "mobi.roomz.INNERRECEIVER_INTENT_ROOM_NAME_CHANGED";
	public static final String PUSH_INNERRECEIVER_INTENT_NICKNAME_CHANGED = "mobi.roomz.INNERRECEIVER_INTENT_NICKNAME_CHANGED";
	public static final String PUSH_INNERRECEIVER_INTENT_USER_KICKED = "mobi.roomz.INNERRECEIVER_INTENT_USER_KICKED";
	public static final int PUSH_NOTIFICATION_ID = 001;
	
	
	
	public static final String PUSH_KEYPHRASE_USER_JOINED = "mobi.roomz.USER_JOINED";
	public static final String PUSH_KEYPHRASE_USER_LEFT_ROOM = "mobi.roomz.USER_LEFT_ROOM";
	public static final String PUSH_KEYPHRASE_NICK_CHANGE = "mobi.roomz.NICK_CHANGE";
	public static final String PUSH_KEYPHRASE_ROOMNAME_CHANGE = "mobi.roomz.ROOMNAME_CHANGE";
	public static final String PUSH_KEYPHRASE_USER_KICKED = "mobi.roomz.USER_KICKED";
	public static final String PUSH_KEYPHRASE_WIPE_ROOM = "mobi.roomz.WIPE_ROOM";
	public static final String PUSH_KEYPHRASE_SECURED_ROOM = "mobi.roomz.SECURED_ROOM";
	
	public static List<String> CONSTS_PUSH_COMPARISON_CHANNEL_ID_ARRAYLIST = new ArrayList<String>();
	public static List<String> CONSTS_PUSH_EVENTS_ARRAYLIST = new ArrayList<String>();
}

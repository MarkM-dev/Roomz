package mobi.roomz;

import mobi.roomz.db.DBHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyMuteReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "timer!", Toast.LENGTH_LONG).show();
	    String channel_id = intent.getStringExtra("channel_id");
		DBHandler database = new DBHandler(context);
		database.update_mute_column_UNMUTE(channel_id);
	}

}

package mobi.roomz.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Text;

import com.rockerhieu.emojicon.EmojiconTextView;

import mobi.roomz.R;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Main_item_viewHolder {

	private final static DateFormat DATETIME_FORMATTER_DAY = new SimpleDateFormat("dd/MM/yyyy");
	private final static DateFormat DATETIME_FORMATTER_TIME = new SimpleDateFormat("HH:mm");
	private String channel_id;
	private TextView roomName_textView, time_textView, unseen_msg_counter_textView;
	private EmojiconTextView lastMsg_emojiconTextView;
	private ImageView mute_icon_imageView, secured_icon_imageView;
	private long timestamp;

	public Main_item_viewHolder(View view) {

		this.roomName_textView = (TextView) view.findViewById(R.id.item_layout_roomName_textView);
		this.time_textView = (TextView) view.findViewById(R.id.item_layout_time_textView);
		this.lastMsg_emojiconTextView = (EmojiconTextView) view.findViewById(R.id.item_layout_lastMsg_emojiconTextView);
		this.unseen_msg_counter_textView = (TextView) view.findViewById(R.id.item_layout_unseen_msg_counter_textView);
		this.mute_icon_imageView = (ImageView) view.findViewById(R.id.item_layout_mute_icon_imageView);
		this.secured_icon_imageView = (ImageView) view.findViewById(R.id.item_layout_secured_icon_imageView);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimeStamp_day(long timestamp) {
		this.timestamp = timestamp;
		time_textView.setText(DATETIME_FORMATTER_DAY.format(new Date(timestamp)));
	}
	public void setTimeStamp_time(long timestamp) {
		this.timestamp = timestamp;
		time_textView.setText(DATETIME_FORMATTER_TIME.format(new Date(timestamp)));
	}
	public void setTimeStamp_String(String timestamp) {
		time_textView.setText(timestamp);
	}

	public String getChannel_id() {
		return channel_id;
	}

	public void setChannel_id(String channel_id) {
		this.channel_id = channel_id;
	}

	public void setRoomName_textView(String roomName_textView) {
		this.roomName_textView.setText(roomName_textView);
	}

	public TextView getRoomName_textView() {
		return roomName_textView;
	}

	public void setTime_textView(String time_textView) {
		this.time_textView.setText(time_textView);
	}

	public TextView getTime_textView() {
		return time_textView;
	}

	public void setLastMsg_textView(String lastMsg_textView) {
		this.lastMsg_emojiconTextView.setText(lastMsg_textView);
	}

	public TextView getLastMsg_textView() {
		return lastMsg_emojiconTextView;
	}

	public TextView getUnseen_msg_counter_textView() {
		return unseen_msg_counter_textView;
	}
	
	public void setUnseen_msg_counter_textView(String counter) {
		this.unseen_msg_counter_textView.setText(counter);
	}
	public void set_mute_icon_visible() {
		mute_icon_imageView.setVisibility(1);
	}
	public void set_mute_icon_gone() {
		mute_icon_imageView.setVisibility(View.GONE);
	}
	public void set_secured_icon_visible() {
		secured_icon_imageView.setVisibility(1);
	}
	public void set_secured_icon_gone() {
		secured_icon_imageView.setVisibility(View.GONE);
	}
}

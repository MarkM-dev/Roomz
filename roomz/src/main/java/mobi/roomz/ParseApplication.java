package mobi.roomz;

import android.app.Application;

import com.appsflyer.AppsFlyerLib;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseUser;

public class ParseApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		ParseCrashReporting.enable(this);
		// Add your initialization code here
		Parse.initialize(this, "KMZLXZoNCw3rSTbDEOgc9ikw075zomAHIEH3UK3E", "QI2uAHoamA3ZYtPHr0atgTbk1vTHRokCpLyRVOb5");
		ParseUser.enableAutomaticUser();
	//	PushService.setDefaultPushCallback(this, ChatPage.class);
		
		AppsFlyerLib.setAppsFlyerKey("bMac9tcyjwkSNMX7B7JPZQ");
	}

	
}

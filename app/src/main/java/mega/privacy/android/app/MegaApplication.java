package mega.privacy.android.app;

//import com.google.android.gms.analytics.GoogleAnalytics;
//import com.google.android.gms.analytics.Logger.LogLevel;
//import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.NotificationBuilder;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;


public class MegaApplication extends Application implements MegaListenerInterface, MegaChatListenerInterface{
	final String TAG = "MegaApplication";
	static final String USER_AGENT = "MEGAAndroid/3.2.2_148";

	DatabaseHandler dbH;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	String localIpAddress = "";
	BackgroundRequestListener requestListener;
	final static private String APP_KEY = "6tioyn8ka5l6hty";
	final static private String APP_SECRET = "hfzgdtrma231qdm";


	MegaChatApiAndroid megaChatApi = null;
	private NotificationBuilder notificationBuilder;
//	static final String GA_PROPERTY_ID = "UA-59254318-1";
//	
//	/**
//	 * Enum used to identify the tracker that needs to be used for tracking.
//	 *
//	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
//	 * storing them all in Application object helps ensure that they are created only once per
//	 * application instance.
//	 */
//	public enum TrackerName {
//	  APP_TRACKER/*, // Tracker used only in this app.
//	  GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
//	  ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
//	  */
//	}
//
//	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
	
	class BackgroundRequestListener implements MegaRequestListenerInterface
	{

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("BackgroundRequestListener:onRequestStart: " + request.getRequestString());
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			log("BackgroundRequestListener:onRequestUpdate: " + request.getRequestString());
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("BackgroundRequestListener:onRequestFinish: " + request.getRequestString() + "____" + e.getErrorCode() + "___" + request.getParamType());
			if (e.getErrorCode() == MegaError.API_ESID){
				if (request.getType() == MegaRequest.TYPE_LOGOUT){
					log("type_logout");
					AccountController.logout(getApplicationContext(), getMegaApi(), getMegaChatApi(), false);
				}
			}
			else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
				if (e.getErrorCode() == MegaError.API_OK){
					if (megaApi != null){
						log("BackgroundRequestListener:onRequestFinish: enableTransferResumption ");
						log("BackgroundRequestListener:onRequestFinish: enableTransferResumption - Session: " + megaApi.dumpSession());
//						megaApi.enableTransferResumption();
					}
				}
			}
			else if(request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
				if (e.getErrorCode() == MegaError.API_OK){

					if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME||request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
						log("BackgroundRequestListener:onRequestFinish: Name: "+request.getText());
						if (megaApi != null){
							if(request.getEmail()!=null){
								log("BackgroundRequestListener:onRequestFinish: Email: "+request.getEmail());
								MegaUser user = megaApi.getContact(request.getEmail());
								if (user != null) {
									log("BackgroundRequestListener:onRequestFinish: User handle: "+user.getHandle());
									log("Visibility: "+user.getVisibility()); //If user visibity == MegaUser.VISIBILITY_UNKNOW then, non contact
									if(user.getVisibility()!=MegaUser.VISIBILITY_VISIBLE){
										log("BackgroundRequestListener:onRequestFinish: Non-contact");
										if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
											dbH.setNonContactEmail(request.getEmail(), user.getHandle()+"");
											dbH.setNonContactFirstName(request.getText(), user.getHandle()+"");
										}
										else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
											dbH.setNonContactLastName(request.getText(), user.getHandle()+"");
										}
									}
									else{
										log("BackgroundRequestListener:onRequestFinish: The user is or was CONTACT: "+user.getEmail());
									}
								}
								else{
									log("BackgroundRequestListener:onRequestFinish: User is NULL");
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("BackgroundRequestListener: onRequestTemporaryError: " + request.getRequestString());
		}
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		MegaApiAndroid.addLoggerObject(new AndroidLogger());
		MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		megaApi = getMegaApi();
		megaApiFolder = getMegaApiFolder();

		MegaChatApiAndroid.setLoggerObject(new AndroidChatLogger());
		MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);

		Util.setContext(getApplicationContext());
		boolean fileLoggerSDK = false;
		if (dbH != null) {
			MegaAttributes attrs = dbH.getAttributes();
			if (attrs != null) {
				if (attrs.getFileLoggerSDK() != null) {
					try {
						fileLoggerSDK = Boolean.parseBoolean(attrs.getFileLoggerSDK());
					} catch (Exception e) {
						fileLoggerSDK = false;
					}
				} else {
					fileLoggerSDK = false;
				}
			} else {
				fileLoggerSDK = false;
			}
		}

		if (Util.DEBUG){
			MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
		}
		else {
			Util.setFileLoggerSDK(fileLoggerSDK);
			if (fileLoggerSDK) {
				MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
			} else {
				MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL);
			}
		}

		boolean fileLoggerKarere = false;
		if (dbH != null) {
			MegaAttributes attrs = dbH.getAttributes();
			if (attrs != null) {
				if (attrs.getFileLoggerKarere() != null) {
					try {
						fileLoggerKarere = Boolean.parseBoolean(attrs.getFileLoggerKarere());
					} catch (Exception e) {
						fileLoggerKarere = false;
					}
				} else {
					fileLoggerKarere = false;
				}
			} else {
				fileLoggerKarere = false;
			}
		}

		if (Util.DEBUG){
			MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);
		}
		else {
			Util.setFileLoggerKarere(fileLoggerKarere);
			if (fileLoggerKarere) {
				MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);
			} else {
				MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR);
			}
		}

		notificationBuilder =  NotificationBuilder.newInstance(this, megaApi);
		
//		initializeGA();
		
//		new MegaTest(getMegaApi()).start();
	}	
	
//	private void initializeGA(){
//		// Set the log level to verbose.
//		GoogleAnalytics.getInstance(this).getLogger().setLogLevel(LogLevel.VERBOSE);
//	}
	
	public MegaApiAndroid getMegaApiFolder(){
		if (megaApiFolder == null){
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
			
			megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
			megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
		}
		
		return megaApiFolder;
	}

	public MegaChatApiAndroid getMegaChatApi(){
		if (megaChatApi == null){
			if (megaApi == null){
				getMegaApi();
			}
			else{
				megaChatApi = new MegaChatApiAndroid(megaApi);
			}
		}

		return megaChatApi;
	}

	public void disableMegaChatApi(){
		megaChatApi = null;
	}
	
	public MegaApiAndroid getMegaApi()
	{
		if(megaApi == null)
		{
			log("MEGAAPI = null");
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApi = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
			
			megaApi.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
			megaApi.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
			
			requestListener = new BackgroundRequestListener();
			log("ADD REQUESTLISTENER");
			megaApi.addRequestListener(requestListener);
			megaApi.addListener(this);

//			DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//			if (dbH.getCredentials() != null){
//				megaChatApi = new MegaChatApiAndroid(megaApi, true);
//			}
//			else{
//				megaChatApi = new MegaChatApiAndroid(megaApi, false);
//			}

			if(Util.isChatEnabled()){
				megaChatApi = new MegaChatApiAndroid(megaApi);
				megaChatApi.addChatListener(this);
			}

			String language = Locale.getDefault().toString();
			boolean languageString = megaApi.setLanguage(language);
			log("Result: "+languageString+" Language: "+language);
			if(languageString==false){
				language = Locale.getDefault().getLanguage();
				languageString = megaApi.setLanguage(language);
				log("2--Result: "+languageString+" Language: "+language);
			}
		}
		
		return megaApi;
	}

	public static boolean isActivityVisible() {
		log("isActivityVisible() => " + activityVisible);
		return activityVisible;
	}

	public static void setFirstConnect(boolean firstConnect){
		MegaApplication.firstConnect = firstConnect;
	}

	public static boolean isFirstConnect(){
		return firstConnect;
	}


	public static long getFirstTs() {
		return firstTs;
	}

	public static void setFirstTs(long firstTs) {
		MegaApplication.firstTs = firstTs;
	}

	public static void activityResumed() {
		log("activityResumed()");
		activityVisible = true;
	}

	public static void activityPaused() {
		log("activityPaused()");
		activityVisible = false;
	}

	private static boolean activityVisible = false;
	private static boolean isLoggingIn = false;
	private static boolean firstConnect = true;
	private static boolean recentChatsFragmentVisible = false;
	public static boolean isFireBaseConnection = false;
	private static long firstTs = -1;

	private static long openChatId = -1;

	public static boolean isLoggingIn() {
		return isLoggingIn;
	}

	public static void setLoggingIn(boolean loggingIn) {
		isLoggingIn = loggingIn;
	}

	public static void setRecentChatsFragmentVisible(boolean recentChatsFragmentVisible){
		MegaApplication.recentChatsFragmentVisible = recentChatsFragmentVisible;
	}

	public static void setOpenChatId(long openChatId){
		MegaApplication.openChatId = openChatId;
	}
	
	
//	synchronized Tracker getTracker(TrackerName trackerId) {
//		if (!mTrackers.containsKey(trackerId)) {
//
//			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//			Tracker t = null;
//			if (trackerId == TrackerName.APP_TRACKER){
//				t = analytics.newTracker(GA_PROPERTY_ID);
//			}
////			Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PROPERTY_ID)
////					: (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
////							: analytics.newTracker(R.xml.ecommerce_tracker);
//					mTrackers.put(trackerId, t);
//					
//		}
//	
//		return mTrackers.get(trackerId);
//	}


	public static long getOpenChatId() {
		return openChatId;
	}

	public String getLocalIpAddress(){
		return localIpAddress;
	}
	
	public void setLocalIpAddress(String ip){
		localIpAddress = ip;
	}
	
	public static void log(String message) {
		Util.log("MegaApplication", message);
	}



	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("type_logout: " + e.getErrorCode() + "__" + request.getParamType());
			if (e.getErrorCode() == MegaError.API_ESID){
				log("calling ManagerActivity.logout");
				AccountController.logout(getApplicationContext(), getMegaApi(), getMegaChatApi(), false);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdate");
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		if (megaApi == null){
			megaApi = getMegaApi();
		}

		if (updatedNodes != null) {
			log("updatedNodes: " + updatedNodes.size());

			for (int i = 0; i < updatedNodes.size(); i++) {
				MegaNode n = updatedNodes.get(i);
				if (n.isInShare() && n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE)){
					log("updatedNodes name: " + n.getName() + " isInshared: " + n.isInShare() + " getchanges: " + n.getChanges() + " haschanged(TYPE_INSHARE): " + n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE));

					try {
						ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
						String name = "";
						for(int j=0; j<sharesIncoming.size();j++) {
							MegaShare mS = sharesIncoming.get(j);
							if (mS.getNodeHandle() == n.getHandle()) {
								MegaUser user = megaApi.getContact(mS.getUser());
								if (user != null) {
									MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));

									if (contactDB != null) {
										if (!contactDB.getName().equals("")) {
											name = contactDB.getName() + " " + contactDB.getLastName();

										} else {
											name = user.getEmail();

										}
									} else {
										log("The contactDB is null: ");
										name = user.getEmail();

									}
								} else {
									name = user.getEmail();
								}
							}
						}

						String source = "<b>"+n.getName()+"</b> "+getString(R.string.incoming_folder_notification)+" "+name;
						Spanned notificationContent = Html.fromHtml(source,0);

						int notificationId = Constants.NOTIFICATION_PUSH_CLOUD_DRIVE;

						Intent intent = new Intent(this, ManagerActivityLollipop.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
						PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
								PendingIntent.FLAG_ONE_SHOT);

						Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
								.setSmallIcon(R.drawable.ic_stat_notify_download)
								.setContentTitle(getString(R.string.title_incoming_folder_notification))
								.setContentText(notificationContent)
								.setStyle(new NotificationCompat.BigTextStyle()
										.bigText(notificationContent))
								.setAutoCancel(true)
								.setSound(defaultSoundUri)
								.setColor(ContextCompat.getColor(this,R.color.mega))
								.setContentIntent(pendingIntent);

						Drawable d = getDrawable(R.drawable.ic_folder_incoming);
							notificationBuilder.setLargeIcon(((BitmapDrawable)d).getBitmap());

						NotificationManager notificationManager =
								(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

						notificationManager.notify(notificationId, notificationBuilder.build());
					}
					catch(Exception e){
						log("Exception: "+e.toString());
					}
				}
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer,
			byte[] buffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
		log("onContactRequestUpdate");

		try {
			if (requests == null) {
				log("Return REQUESTS are NULL");
				return;
			}
			MegaContactRequest crToShow = null;
			boolean showNotification = false;
			for (int i = 0; i < requests.size(); i++) {
				MegaContactRequest cr = requests.get(i);
				if (cr != null) {
					if ((cr.getStatus() == MegaContactRequest.STATUS_UNRESOLVED) && (!cr.isOutgoing())) {
						showNotification = true;
						crToShow = cr;
						log("onContactRequestUpdate: " + cr.getSourceEmail() + " cr.isOutgoing: " + cr.isOutgoing() + " cr.getStatus: " + cr.getStatus());
					}
				}
			}

			if (showNotification) {

				String notificationContent;
				if(crToShow!=null){
					notificationContent = crToShow.getSourceEmail();
				}
				else{
					log("Return because the request is NULL");
					return;
				}

				int notificationId = Constants.NOTIFICATION_PUSH_CONTACT;

				Intent intent = new Intent(this, ManagerActivityLollipop.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.setAction(Constants.ACTION_IPC);
				PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
						PendingIntent.FLAG_ONE_SHOT);

				Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_stat_notify_download)
						.setContentTitle(getString(R.string.title_contact_request_notification))
						.setContentText(notificationContent)
						.setStyle(new NotificationCompat.BigTextStyle()
								.bigText(notificationContent))
						.setAutoCancel(true)
						.setSound(defaultSoundUri)
						.setColor(ContextCompat.getColor(this,R.color.mega))
						.setContentIntent(pendingIntent);

				if(crToShow!=null){
					Bitmap largeIcon = createDefaultAvatar(crToShow.getSourceEmail());
					if(largeIcon!=null){
						notificationBuilder.setLargeIcon(largeIcon);
					}
				}

				NotificationManager notificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

				notificationManager.notify(notificationId, notificationBuilder.build());
			}
		}
		catch(Exception e){
			log("Exception when showing IPC request: "+e.getMessage());
		}
	}

	public Bitmap createDefaultAvatar(String email){
		log("createDefaultAvatar()");

		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);

		if(email!=null){
			if(!email.isEmpty()){
				char title = email.charAt(0);
				String firstLetter = new String(title+"");

				if(!firstLetter.equals("(")){

					log("Draw letter: "+firstLetter);
					Paint text = new Paint();
					Typeface face = Typeface.SANS_SERIF;
					text.setTypeface(face);
					text.setAntiAlias(true);
					text.setSubpixelText(true);
					text.setStyle(Paint.Style.FILL);
					text.setColor(Color.WHITE);
					text.setTextSize(150);
					text.setTextAlign(Paint.Align.CENTER);

					Rect r = new Rect();
					c.getClipBounds(r);
					int cHeight = r.height();
					int cWidth = r.width();
					text.setTextAlign(Paint.Align.LEFT);
					text.getTextBounds(firstLetter, 0, firstLetter.length(), r);
					float x = 0;
					float y = 0;
					if(firstLetter.toUpperCase(Locale.getDefault()).equals("A")){
						x = cWidth / 2f - r.width() / 2f - r.left - 10;
						y = cHeight / 2f + r.height() / 2f - r.bottom + 10;
					}
					else{
						x = cWidth / 2f - r.width() / 2f - r.left;
						y = cHeight / 2f + r.height() / 2f - r.bottom;
					}

					c.drawText(firstLetter.toUpperCase(Locale.getDefault()), x, y, text);
				}

			}
		}
		return defaultAvatar;
	}

	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

		if(item==null){
			log("onChatListItemUpdate: item is NULL --> return");
		}

		log("onChatListItemUpdate: "+item.getTitle()+ " chat id: "+item.getChatId());
		if (megaApi == null){
			megaApi = getMegaApi();
		}

		if (megaChatApi == null){
			megaChatApi = getMegaChatApi();
		}

		log("Unread count is: "+item.getUnreadCount());

		if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_TS) && (item.getUnreadCount() != 0)){

			try {
				if(isFireBaseConnection){
					log("Show notification ALWAYS");
					showNotification(item);
					firstTs=-1;
				}
				else{
					if (!recentChatsFragmentVisible) {
						if (openChatId != item.getChatId()) {
							if (isFirstConnect()) {
								log("onChatListItemUpdateMegaApplication. FIRSTCONNECT " + item.getTitle() + "Unread count: " + item.getUnreadCount() + " hasChanged(CHANGE_TYPE_UNREAD_COUNT): " + item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT) + " hasChanged(CHANGE_TYPE_LAST_TS):" + item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_TS));
							} else {
								log("onChatListItemUpdateMegaApplication. NOTFIRSTCONNECT " + item.getTitle() + "Unread count: " + item.getUnreadCount() + " hasChanged(CHANGE_TYPE_UNREAD_COUNT): " + item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT) + " hasChanged(CHANGE_TYPE_LAST_TS):" + item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_TS));

								if(firstTs==-1){
									log("First TS is -1 -- SHOW NOTIF");
									showNotification(item);
								}
								else{
									if(firstTs>item.getLastTimestamp()){
										log("DO NOT SHOW NOTIF - FirstTS when logging "+firstTs+ " > item timestamp: "+item.getLastTimestamp());
									}
									else{
										log("FirstTS when logging "+firstTs+ " < item timestamp: "+item.getLastTimestamp());
										showNotification(item);
										firstTs=-1;
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e){
				log("Exception when trying to show chat notification");
			}

		}
	}

	public void showNotification(MegaChatListItem item){
		log("showNotification: "+item.getTitle()+ " message: "+item.getLastMessage());

		ChatSettings chatSettings = dbH.getChatSettings();
		String email = megaChatApi.getContactEmail(item.getPeerHandle());

		if(chatSettings!=null){
			if(chatSettings.getNotificationsEnabled().equals("true")){
				log("Notifications ON for all chats");

				ChatItemPreferences chatItemPreferences = dbH.findChatPreferencesByHandle(String.valueOf(item.getChatId()));

				if(chatItemPreferences==null){
					log("No preferences for this item");
					String soundString = chatSettings.getNotificationsSound();
					Uri uri = Uri.parse(soundString);
					log("Uri: "+uri);

					if(soundString.equals("true")||soundString.equals("")){

						Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						notificationBuilder.sendBundledNotification(defaultSoundUri2, item, chatSettings.getVibrationEnabled(), email);
					}
					else if(soundString.equals("-1")){
						log("Silent notification");
						notificationBuilder.sendBundledNotification(null, item, chatSettings.getVibrationEnabled(), email);
					}
					else{
						Ringtone sound = RingtoneManager.getRingtone(this, uri);
						if(sound==null){
							log("Sound is null");
							notificationBuilder.sendBundledNotification(null, item, chatSettings.getVibrationEnabled(), email);
						}
						else{
							notificationBuilder.sendBundledNotification(uri, item, chatSettings.getVibrationEnabled(), email);
						}
					}
				}
				else{
					log("Preferences FOUND for this item");
					if(chatItemPreferences.getNotificationsEnabled().equals("true")){
						log("Notifications ON for this chat");
						String soundString = chatItemPreferences.getNotificationsSound();
						Uri uri = Uri.parse(soundString);
						log("Uri: "+uri);

						if(soundString.equals("true")){

							Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							notificationBuilder.sendBundledNotification(defaultSoundUri2, item, chatSettings.getVibrationEnabled(), email);
						}
						else if(soundString.equals("-1")){
							log("Silent notification");
							notificationBuilder.sendBundledNotification(null, item, chatSettings.getVibrationEnabled(), email);
						}
						else{
							Ringtone sound = RingtoneManager.getRingtone(this, uri);
							if(sound==null){
								log("Sound is null");
								notificationBuilder.sendBundledNotification(null, item, chatSettings.getVibrationEnabled(), email);
							}
							else{
								notificationBuilder.sendBundledNotification(uri, item, chatSettings.getVibrationEnabled(), email);

							}
						}
					}
					else{
						log("Notifications OFF for this chats");
					}
				}
			}
			else{
				log("Notifications OFF");
			}
		}
		else{
			log("Notifications DEFAULT ON");

			Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			notificationBuilder.sendBundledNotification(defaultSoundUri2, item, "true", email);
		}
	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {
		log("onChatInitStateUpdate");
	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {
		log("onChatOnlineStatusUpdate");
	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
		log("onChatPresenceConfigUpdate");
	}

	public void sendSignalPresenceActivity(){
		log("sendSignalPresenceActivity");
		if(Util.isChatEnabled()){
			if (megaChatApi != null){
				if(megaChatApi.isSignalActivityRequired()){
					megaChatApi.signalPresenceActivity();
				}
			}
		}
	}
}

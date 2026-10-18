package com.mochikibun.monsthub;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import org.json.*;
public final class DeadlineScheduler {
 public static final String CHANNEL="deadlines-v1";
 public static String token(JSONObject r){return r.optString("due")+"@"+r.optInt("lead_hours",24);}
 public static boolean complete(HubState s,String table,JSONObject r)throws Exception{
  if(table.equals("goals"))return r.getInt("current")>=r.getInt("target");
  if(table.equals("orb_plans"))return r.getBoolean("received");
  if(s.accounts().length()==0)return false;JSONObject p=r.getJSONObject("progress");for(int i=0;i<s.accounts().length();i++)if(p.optInt(s.accounts().getJSONObject(i).getString("id"),0)!=3)return false;return true;
 }
 public static Instant reminder(JSONObject r){return OffsetDateTime.parse(r.optString("due")).toInstant().minusSeconds(r.optInt("lead_hours",24)*3600L);}
 private static PendingIntent pending(Context c,String table,String id){Intent i=new Intent(c,DeadlineReceiver.class).setAction("com.mochikibun.monsthub.DEADLINE").setData(new Uri.Builder().scheme("monsthub").authority("deadline").appendPath(table).appendPath(id).build());return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 public static void schedule(Context c){try{NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"登録した目標・ミッションの期限",NotificationManager.IMPORTANCE_DEFAULT));HubState s=new HubStore(c).load();AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Instant now=Instant.now();for(String table:new String[]{"missions","goals","orb_plans"}){JSONArray rows=s.planner().list(table);for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);PendingIntent pi=pending(c,table,r.getString("id"));alarms.cancel(pi);if(!r.getBoolean("notify")||r.getString("due").isEmpty()||complete(s,table,r)||token(r).equals(r.optString("notified_due")))continue;Instant due=OffsetDateTime.parse(r.getString("due")).toInstant();if(!due.isAfter(now))continue;long at=Math.max(now.toEpochMilli()+1000,reminder(r).toEpochMilli());alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}}}catch(Exception ignored){/* Current saved data remains intact even if alarms are unavailable. */}}
 public static void deliver(Context c,Intent intent){try{if(intent.getData()==null)return;java.util.List<String> parts=intent.getData().getPathSegments();if(parts.size()!=2)return;String table=parts.get(0),id=parts.get(1);if(!java.util.Arrays.asList("missions","goals","orb_plans").contains(table))return;HubStore store=new HubStore(c);HubState s=store.load();JSONObject r=s.planner().find(table,id);if(!r.getBoolean("notify")||r.getString("due").isEmpty()||complete(s,table,r)||token(r).equals(r.optString("notified_due")))return;Instant now=Instant.now();if(!OffsetDateTime.parse(r.getString("due")).toInstant().isAfter(now))return;if(reminder(r).isAfter(now)){schedule(c);return;}if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
  String destination=table.equals("missions")?"missions":table.equals("goals")?"goals":"orb_plans";Intent open=new Intent(c,HubActivity.class).putExtra("destination",destination).setData(intent.getData());PendingIntent tap=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"登録した目標・ミッションの期限",NotificationManager.IMPORTANCE_DEFAULT));Notification notification=new Notification.Builder(c,CHANNEL).setSmallIcon(com.mochikibun.monsthub.R.drawable.ic_notification).setContentTitle(r.getString("name")).setContentText("登録した期限："+r.getString("due")).setContentIntent(tap).setAutoCancel(true).build();nm.notify(table+":"+id,1,notification);r.put("notified_due",token(r));store.save(s);
 }catch(Exception ignored){/* Deleted or invalid records never produce a notification. */}}
}

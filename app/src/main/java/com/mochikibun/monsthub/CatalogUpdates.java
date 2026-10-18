package com.mochikibun.monsthub;
import android.app.job.*;
import android.content.*;
import java.time.*;
public final class CatalogUpdates {
 public static final int PERIODIC=7401,ON_OPEN=7402;
 public static final long INTERVAL=24*60*60*1000L;
 public static boolean stale(String updated,Instant now){try{return !Instant.parse(updated).plusMillis(INTERVAL).isAfter(now);}catch(Exception e){return true;}}
 public static void schedule(Context c){try{HubState s=new HubStore(c).load();JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(!s.root.optBoolean("catalog_auto",true)){jobs.cancel(PERIODIC);jobs.cancel(ON_OPEN);return;}int network=s.root.optBoolean("catalog_unmetered",true)?JobInfo.NETWORK_TYPE_UNMETERED:JobInfo.NETWORK_TYPE_ANY;JobInfo current=jobs.getPendingJob(PERIODIC);if(current==null||current.getNetworkType()!=network){jobs.schedule(new JobInfo.Builder(PERIODIC,new ComponentName(c,CatalogUpdateService.class)).setRequiredNetworkType(network).setPersisted(true).setPeriodic(INTERVAL).build());}}catch(Exception ignored){}}
 public static void onOpen(Context c){schedule(c);try{HubState s=new HubStore(c).load();if(!s.root.optBoolean("catalog_auto",true))return;JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(jobs.getPendingJob(ON_OPEN)!=null)return;int network=s.root.optBoolean("catalog_unmetered",true)?JobInfo.NETWORK_TYPE_UNMETERED:JobInfo.NETWORK_TYPE_ANY;jobs.schedule(new JobInfo.Builder(ON_OPEN,new ComponentName(c,CatalogUpdateService.class)).setRequiredNetworkType(network).setMinimumLatency(1000).build());}catch(Exception ignored){}}
}

package com.mochikibun.monsthub;
import android.app.job.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
public final class CatalogUpdateService extends JobService {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final ConcurrentHashMap<Integer,Future<?>> tasks=new ConcurrentHashMap<>();
 private final ConcurrentHashMap<Integer,AtomicBoolean> cancellations=new ConcurrentHashMap<>();
 @Override public boolean onStartJob(JobParameters params){AtomicBoolean cancelled=new AtomicBoolean(false);cancellations.put(params.getJobId(),cancelled);Future<?> task=worker.submit(()->{boolean retry=false;try{HubState s=new HubStore(this).load();if(s.root.optBoolean("catalog_auto",true)&&!cancelled.get()){try(CatalogRepository repository=new CatalogRepository(this)){if(CatalogUpdates.stale(repository.updated(),Instant.now()))repository.update();}}}catch(Exception e){retry=true;}finally{tasks.remove(params.getJobId());cancellations.remove(params.getJobId());if(!cancelled.get())jobFinished(params,retry);}});tasks.put(params.getJobId(),task);return true;}
 @Override public boolean onStopJob(JobParameters params){AtomicBoolean cancelled=cancellations.get(params.getJobId());if(cancelled!=null)cancelled.set(true);Future<?> task=tasks.get(params.getJobId());if(task!=null)task.cancel(true);return true;}
 @Override public void onDestroy(){for(AtomicBoolean cancelled:cancellations.values())cancelled.set(true);worker.shutdownNow();super.onDestroy();}
}

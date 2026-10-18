package com.mochikibun.monsthub;
import android.content.*;
public final class DeadlineReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){if("com.mochikibun.monsthub.DEADLINE".equals(intent.getAction()))DeadlineScheduler.deliver(context,intent);else {DeadlineScheduler.schedule(context);CatalogUpdates.schedule(context);}}
}

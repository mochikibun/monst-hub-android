package com.mochikibun.monsthub;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
/** Fetch visible icons only. Names and count controls keep working offline. */
public final class CatalogIcons implements AutoCloseable {
 private final File directory;private final ExecutorService workers=Executors.newFixedThreadPool(3);private final Handler main=new Handler(Looper.getMainLooper());private volatile boolean closed;
 private final ConcurrentHashMap<String,Long> failed=new ConcurrentHashMap<>();
 private final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(8*1024*1024){@Override protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
 public CatalogIcons(Context c){directory=new File(c.getCacheDir(),"catalog-icons-v1");directory.mkdirs();}
 public static String url(String token){return token!=null&&token.matches("[A-Za-z0-9_-]{1,100}")?"https://img.gamewith.jp/article_tools/monst/gacha/"+token+".jpg":"";}
 public void load(ImageView view,String token){view.setTag(token);view.setImageDrawable(new ColorDrawable(0xff292929));if(url(token).isEmpty()||closed)return;Bitmap cached=memory.get(token);if(cached!=null){view.setImageBitmap(cached);return;}if(System.currentTimeMillis()-failed.getOrDefault(token,0L)<60000)return;WeakReference<ImageView> weak=new WeakReference<>(view);workers.execute(()->{Bitmap bitmap=null;try{File file=new File(directory,token+".jpg");if(file.exists())bitmap=decode(file);if(bitmap==null){HttpURLConnection con=(HttpURLConnection)new URL(url(token)).openConnection();con.setConnectTimeout(8000);con.setReadTimeout(8000);try{if(con.getResponseCode()!=200)throw new IOException("Icon unavailable");try(InputStream in=con.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted()||closed)throw new IOException("Stopped");bytes.write(b,0,n);if(bytes.size()>2_000_000)throw new IOException("Icon too large");}File temp=File.createTempFile("icon-",".tmp",directory);try{try(OutputStream out=new FileOutputStream(temp)){bytes.writeTo(out);}bitmap=decode(temp);if(bitmap==null)throw new IOException("Invalid icon");if(!temp.renameTo(file))temp.delete();trim();}finally{if(temp.exists())temp.delete();}}}finally{con.disconnect();}}if(bitmap!=null)memory.put(token,bitmap);}catch(Exception e){failed.put(token,System.currentTimeMillis());}Bitmap result=bitmap;if(result!=null&&!closed)main.post(()->{ImageView target=weak.get();if(target!=null&&!closed&&token.equals(target.getTag()))target.setImageBitmap(result);});});}
 private Bitmap decode(File file){BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),opt);if(opt.outWidth<1||opt.outHeight<1||opt.outWidth>4096||opt.outHeight>4096)return null;opt.inSampleSize=1;while(Math.max(opt.outWidth,opt.outHeight)/opt.inSampleSize>256)opt.inSampleSize*=2;opt.inJustDecodeBounds=false;return BitmapFactory.decodeFile(file.getAbsolutePath(),opt);}
 private void trim(){File[] files=directory.listFiles((dir,name)->name.endsWith(".jpg"));if(files==null)return;long total=0;for(File f:files)total+=f.length();if(total<=48*1024*1024)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File f:files){long length=f.length();if(f.delete())total-=length;if(total<=40*1024*1024)break;}}
 @Override public void close(){closed=true;workers.shutdownNow();main.removeCallbacksAndMessages(null);memory.evictAll();}
}

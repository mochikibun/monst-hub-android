package com.mochikibun.monsthub;
import android.content.Context;
import android.content.SharedPreferences;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class FeedRepository {
 private static final String BASE="https://raw.githubusercontent.com/mochikibun/monst-hub-feed/main/public/";
 private final SharedPreferences cache;
 public FeedRepository(Context c){cache=c.getSharedPreferences("feed-v1",Context.MODE_PRIVATE);}
 public static final class Result {
  public FeedParser.Feed feed; public String warning="", error=""; public boolean cached=false;
 }
 public Result load(){
  Result r=new Result();
  try { String raw=get("campaigns.json");r.feed=FeedParser.parse(raw);cache.edit().putString("campaigns",raw).apply(); }
  catch(Exception e){r.error="データを取得できませんでした。通信環境を確認し、更新ボタンで再試行してください。";
   try {r.feed=FeedParser.parse(cache.getString("campaigns",""));r.cached=true;}catch(Exception ignored){} }
  try {r.warning=FeedParser.statusWarning(get("status.json"));}
  catch(Exception e){r.warning="配信状態を確認できません。データが完全かどうか不明です。";}
  return r;
 }
 private String get(String file)throws Exception {
  HttpURLConnection con=(HttpURLConnection)new URL(BASE+file).openConnection();
  con.setConnectTimeout(12000);con.setReadTimeout(12000);con.setRequestProperty("Accept","application/json");
  try {if(con.getResponseCode()!=200)throw new IOException("HTTP "+con.getResponseCode());
   try(InputStream in=con.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] buf=new byte[8192];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>2000000)throw new IOException("Feed too large");out.write(buf,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
   }
  }finally{con.disconnect();}
 }
}

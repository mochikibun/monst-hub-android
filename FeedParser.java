package com.mochikibun.monsthub;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
public final class FeedParser {
 public static final class Feed {
  public final List<Campaign> campaigns; public final String generatedAt;
  Feed(List<Campaign> c,String g){campaigns=Collections.unmodifiableList(c);generatedAt=g;}
 }
 public static Feed parse(String raw) throws Exception {
  JSONObject root=new JSONObject(raw);
  if(root.getInt("schema_version")!=1) throw new IllegalArgumentException("Unsupported schema");
  JSONArray items=root.getJSONArray("campaigns"); List<Campaign> result=new ArrayList<>();
  for(int i=0;i<items.length();i++) {
   JSONObject o=items.getJSONObject(i); Map<LocalDate,String> attrs=new HashMap<>();
   JSONArray ds=o.optJSONArray("daily_attributes");
   if(ds!=null) for(int j=0;j<ds.length();j++){JSONObject d=ds.getJSONObject(j);attrs.put(LocalDate.parse(d.getString("date")),d.getString("attribute"));}
   String category=o.getString("category");
   if(!Arrays.asList("archive","temple","normal","other").contains(category))category="other";
   JSONObject source=o.optJSONObject("source");
   result.add(new Campaign(o.getString("name"),category,LocalDate.parse(o.getString("start_date")),LocalDate.parse(o.getString("end_date")),attrs,source==null?"":source.optString("url","")));
  }
  return new Feed(result,root.getString("generated_at"));
 }
 public static String statusWarning(String raw) throws Exception {
  JSONObject o=new JSONObject(raw);
  if(o.getInt("schema_version")!=1) throw new IllegalArgumentException("Unsupported status schema");
  String s=o.getString("status");
  if(s.equals("ok"))return "";
  if(s.equals("degraded"))return "配信データが不完全な可能性があります。公式情報もご確認ください。";
  return "配信状態に注意が必要です（"+s+"）。公式情報もご確認ください。";
 }
}

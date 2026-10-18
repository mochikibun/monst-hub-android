package com.mochikibun.monsthub;
import java.time.LocalDate;
import java.util.Map;
public final class Campaign {
 public final String name, category, sourceUrl;
 public final LocalDate start, end;
 public final Map<LocalDate,String> attributes;
 public Campaign(String n,String c,LocalDate s,LocalDate e,Map<LocalDate,String> a,String url) {
  if(e.isBefore(s)) throw new IllegalArgumentException("Invalid period");
  name=n;category=c;start=s;end=e;attributes=java.util.Collections.unmodifiableMap(new java.util.HashMap<>(a));sourceUrl=url;
 }
 public boolean active(LocalDate day,String filter) { return !day.isBefore(start)&&!day.isAfter(end)&&(filter.equals("all")||category.equals(filter)); }
 public String attribute(LocalDate day) { return attributes.getOrDefault(day, "指定なし"); }
 public static String categoryLabel(String key) { switch(key) { case "archive":return "書庫"; case "temple":return "神殿"; case "normal":return "ノマクエ"; default:return "その他"; } }
}

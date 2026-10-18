package com.mochikibun.monsthub;
import org.json.*;
import java.time.*;
import java.util.*;
import java.net.URI;

/** Pure local account state. No game login or network writes. */
public final class HubState {
 public static final int MAX_ACCOUNTS=100, MAX_ORBS=99999999;
 final JSONObject root;
 private HubState(JSONObject root){this.root=root;}
 public static HubState create() throws Exception {
  JSONObject r=new JSONObject();r.put("format","monst-hub-backup");r.put("schema_version",2);
  r.put("accounts",new JSONArray());r.put("tasks",new JSONArray());r.put("days",new JSONObject());r.put("orb_history",new JSONArray());r.put("reset_hour",4);
  HubState s=new HubState(r);PlannerData.ensure(r);
  for(String name:new String[]{"ログイン","日課","書庫","神殿"})s.addTask(name);
  return s;
 }
 public static HubState parse(String json)throws Exception {
  JSONObject r=new JSONObject(json);
  if(!"monst-hub-backup".equals(r.getString("format"))||(r.getInt("schema_version")!=1&&r.getInt("schema_version")!=2))throw new IllegalArgumentException("MONST HUB形式のバックアップではありません");
  HubState s=new HubState(r);if(r.getInt("schema_version")==1){PlannerData.ensure(r);r.put("schema_version",2);}s.validate();PlannerData.validate(s);return s;
 }
 public HubState copy()throws Exception{return parse(json());}
 public PlannerData planner(){return new PlannerData(this);}
 public LocalDate bondDay(Instant now){return now.atZone(ZoneId.of("Asia/Tokyo")).minusHours(root.optInt("bond_reset_hour",resetHour())).toLocalDate();}
 public String json(){return root.toString();}
 public JSONArray accounts()throws Exception{return root.getJSONArray("accounts");}
 public JSONArray tasks()throws Exception{return root.getJSONArray("tasks");}
 public JSONArray history()throws Exception{return root.getJSONArray("orb_history");}
 public int resetHour(){return root.optInt("reset_hour",0);}
 public void setResetHour(int h)throws Exception{if(h<0||h>23)throw new IllegalArgumentException("0〜23時を指定してください");root.put("reset_hour",h);}
 public LocalDate day(Instant now){return now.atZone(ZoneId.of("Asia/Tokyo")).minusHours(resetHour()).toLocalDate();}
 private static java.util.List<String> keysOf(JSONObject o){java.util.List<String> list=new ArrayList<>();java.util.Iterator<String> it=o.keys();while(it.hasNext())list.add(it.next());return list;}
 private static String id(){return UUID.randomUUID().toString();}
 private static String text(String s,int max){if(s==null||s.trim().isEmpty()||s.length()>max)throw new IllegalArgumentException("文字数を確認してください（1〜"+max+"文字）");return s.trim();}
 public static boolean validUrl(String s){try{if(s.isEmpty())return true;URI u=new URI(s);return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getHost()!=null&&u.getUserInfo()==null&&s.length()<=4000;}catch(Exception e){return false;}}
 public String addAccount(String name)throws Exception {
  if(accounts().length()>=MAX_ACCOUNTS)throw new IllegalArgumentException("登録できるのは100垢までです");
  JSONObject a=new JSONObject();String key=id();a.put("id",key);a.put("name",text(name,60));a.put("memo","");a.put("url","");a.put("orbs",0);accounts().put(a);return key;
 }
 public JSONObject account(String id)throws Exception{for(int i=0;i<accounts().length();i++){JSONObject a=accounts().getJSONObject(i);if(a.getString("id").equals(id))return a;}throw new IllegalArgumentException("アカウントが見つかりません");}
 public void editAccount(String id,String name,String memo,String url)throws Exception {
  String n=text(name,60);if(memo.length()>2000||!validUrl(url))throw new IllegalArgumentException("メモは2000文字以内、URLはhttp/httpsの形式で入力してください");
  JSONObject a=account(id);a.put("name",n);a.put("memo",memo);a.put("url",url);
 }
 public void deleteAccount(String id)throws Exception{
  account(id);JSONArray a=accounts();for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id)){a.remove(i);break;}
  JSONObject days=root.getJSONObject("days");for(String day:new ArrayList<>(keysOf(days))){JSONObject d=days.getJSONObject(day);d.remove(id);if(d.length()==0)days.remove(day);}
  planner().deleteAccountReferences(id);
  JSONArray h=history();for(int i=h.length()-1;i>=0;i--)if(id.equals(h.getJSONObject(i).getString("account_id")))h.remove(i);
 }
 public String addTask(String name)throws Exception{if(tasks().length()>=30)throw new IllegalArgumentException("日課は30項目までです");String key=id();tasks().put(new JSONObject().put("id",key).put("name",text(name,60)));return key;}
 public void deleteTask(String key)throws Exception{
  JSONArray a=tasks();boolean found=false;for(int i=0;i<a.length();i++)if(key.equals(a.getJSONObject(i).getString("id"))){a.remove(i);found=true;break;}if(!found)throw new IllegalArgumentException("日課が見つかりません");
  JSONObject ds=root.getJSONObject("days");for(String d:keysOf(ds)){JSONObject day=ds.getJSONObject(d);for(String account:keysOf(day))day.getJSONObject(account).remove(key);}
 }
 private boolean taskExists(String key)throws Exception{if(key.equals("bond"))return true;for(int i=0;i<tasks().length();i++)if(tasks().getJSONObject(i).getString("id").equals(key))return true;return false;}
 public boolean checked(LocalDate date,String account,String task){JSONObject d=root.optJSONObject("days");JSONObject row=d==null?null:d.optJSONObject(date.toString());JSONObject a=row==null?null:row.optJSONObject(account);return a!=null&&a.optBoolean(task,false);}
 public void check(LocalDate date,String id,String task,boolean value)throws Exception{
  account(id);if(!taskExists(task))throw new IllegalArgumentException("日課が見つかりません");
  JSONObject ds=root.getJSONObject("days");JSONObject row=ds.optJSONObject(date.toString());if(row==null){row=new JSONObject();ds.put(date.toString(),row);}JSONObject a=row.optJSONObject(id);if(a==null){a=new JSONObject();row.put(id,a);}a.put(task,value);
 }
 public int completed(LocalDate day,String id)throws Exception{int n=0;for(int i=0;i<tasks().length();i++)if(checked(day,id,tasks().getJSONObject(i).getString("id")))n++;return n;}
 public int completedAccounts(LocalDate day)throws Exception{int n=0;if(tasks().length()==0)return 0;for(int i=0;i<accounts().length();i++)if(completed(day,accounts().getJSONObject(i).getString("id"))==tasks().length())n++;return n;}
 public long totalOrbs()throws Exception{long n=0;for(int i=0;i<accounts().length();i++)n+=accounts().getJSONObject(i).getInt("orbs");return n;}
 public void setOrbs(String id,int balance,String memo,Instant now)throws Exception{
  if(balance<0||balance>MAX_ORBS)throw new IllegalArgumentException("オーブ数は0〜99,999,999で入力してください");if(memo.length()>200)throw new IllegalArgumentException("理由は200文字以内で入力してください");JSONObject a=account(id);int before=a.getInt("orbs");if(before==balance)return;
  JSONObject item=new JSONObject().put("account_id",id).put("at",now.toString()).put("delta",balance-before).put("balance",balance).put("memo",memo);
  a.put("orbs",balance);JSONArray h=history();h.put(item);if(h.length()>10000)h.remove(0);
 }
 private static void require(boolean b)throws Exception{if(!b)throw new IllegalArgumentException("バックアップの内容が不正です");}
 private static int integer(JSONObject o,String key,int min,int max)throws Exception{Object x=o.get(key);require(x instanceof Number&&((Number)x).doubleValue()==((Number)x).longValue());long v=((Number)x).longValue();require(v>=min&&v<=max);return (int)v;}
 private static String string(JSONObject o,String key,int max,boolean empty)throws Exception{Object x=o.get(key);require(x instanceof String);String s=(String)x;require(s.length()<=max&&(empty||!s.trim().isEmpty()));return s;}
 private void validate()throws Exception{
  integer(root,"schema_version",2,2);integer(root,"reset_hour",0,23);JSONArray as=accounts(),ts=tasks();require(as.length()<=MAX_ACCOUNTS&&ts.length()<=30);Set<String> ids=new HashSet<>(),keys=new HashSet<>();keys.add("bond");
  for(int i=0;i<as.length();i++){JSONObject a=as.getJSONObject(i);String id=string(a,"id",100,false);require(ids.add(id));string(a,"name",60,false);string(a,"memo",2000,true);require(validUrl(string(a,"url",4000,true)));integer(a,"orbs",0,MAX_ORBS);}
  for(int i=0;i<ts.length();i++){JSONObject t=ts.getJSONObject(i);require(keys.add(string(t,"id",100,false)));string(t,"name",60,false);}
  JSONObject ds=root.getJSONObject("days");require(ds.length()<=10000);
  for(String d:keysOf(ds)){LocalDate.parse(d);JSONObject row=ds.getJSONObject(d);for(String id:keysOf(row)){require(ids.contains(id));JSONObject flags=row.getJSONObject(id);for(String task:keysOf(flags)){require(keys.contains(task));require(flags.get(task) instanceof Boolean);}}}
  JSONArray h=history();require(h.length()<=10000);for(int i=0;i<h.length();i++){JSONObject item=h.getJSONObject(i);require(ids.contains(item.getString("account_id")));Instant.parse(item.getString("at"));integer(item,"delta",-MAX_ORBS,MAX_ORBS);integer(item,"balance",0,MAX_ORBS);string(item,"memo",200,true);}
 }
}

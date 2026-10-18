package com.mochikibun.monsthub;
import org.json.*;
import java.time.*;
import java.util.*;
/** Extra manager records live in the same atomic backup as accounts. */
public final class PlannerData {
 public static final String[] TABLES={"inventory","teams","missions","goals","formations","orb_plans","custom_characters"};
 private final HubState s;
 PlannerData(HubState s){this.s=s;}
 static void ensure(JSONObject root)throws Exception{if(!root.has("planner")){JSONObject p=new JSONObject();for(String k:TABLES)p.put(k,new JSONArray());root.put("planner",p);}if(!root.has("bond_reset_hour"))root.put("bond_reset_hour",root.optInt("reset_hour",4));}
 public JSONArray list(String table)throws Exception{return s.root.getJSONObject("planner").getJSONArray(table);}
 public JSONObject find(String table,String id)throws Exception{JSONArray a=list(table);for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id))return a.getJSONObject(i);throw new IllegalArgumentException("記録が見つかりません");}
 public static String id(){return UUID.randomUUID().toString();}
 public void put(String table,JSONObject record)throws Exception{JSONArray a=list(table);String id=record.getString("id");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id)){a.put(i,record);return;}if(a.length()>=10000)throw new IllegalArgumentException("記録の上限に達しました");a.put(record);}
 public void delete(String table,String id)throws Exception{JSONArray a=list(table);for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id)){a.remove(i);return;}}
 public int owned(String catalogId,String accountId)throws Exception{int n=0;JSONArray a=list("inventory");for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(r.getString("catalog_id").equals(catalogId)&&(accountId.isEmpty()||r.getString("account_id").equals(accountId)))n+=r.getInt("count");}return n;}
 public Set<String> ownedIds(String account)throws Exception{Set<String> ids=new HashSet<>();JSONArray a=list("inventory");for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(account.isEmpty()||r.getString("account_id").equals(account))ids.add(r.getString("catalog_id"));}return ids;}
 public int adjustOwned(JSONObject character,String accountId,int delta)throws Exception{
  s.account(accountId);if(delta!=1&&delta!=-1)throw new IllegalArgumentException("増減は1体ずつです");String cid=character.getString("id");JSONArray rows=list("inventory");int total=owned(cid,accountId);JSONObject quick=null;java.util.List<JSONObject> detailed=new java.util.ArrayList<>();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(cid.equals(row.getString("catalog_id"))&&accountId.equals(row.getString("account_id"))){if(row.optBoolean("quick",false)&&row.getInt("luck")==0&&!row.getBoolean("trained")&&row.getString("fruit").isEmpty()&&row.getString("target_fruit").isEmpty()&&row.getString("memo").isEmpty())quick=row;else detailed.add(row);}}
  if(delta>0){if(total>=9999)throw new IllegalArgumentException("所持数はこの垢・キャラ合計で9999体までです");if(quick!=null)quick.put("count",quick.getInt("count")+1);else put("inventory",new JSONObject().put("id",id()).put("account_id",accountId).put("catalog_id",cid).put("name",character.getString("name")).put("element",character.getString("element")).put("form",character.getString("form")).put("icon",character.optString("icon",character.optString("no",""))).put("count",1).put("luck",0).put("trained",false).put("fruit","").put("target_fruit","").put("memo","").put("quick",true));return total+1;}
  if(total==0)return 0;JSONObject target=quick;if(target==null){if(detailed.size()>1)throw new IllegalArgumentException("育成記録が複数あります。詳細から減らす記録を選んでください");target=detailed.get(0);}int count=target.getInt("count");if(count==1)delete("inventory",target.getString("id"));else target.put("count",count-1);return total-1;
 }
 public void teamCheck(String id,String task,boolean value,Instant now)throws Exception{JSONObject t=find("teams",id);JSONArray members=t.getJSONArray("members");LocalDate date=task.equals("bond")?s.bondDay(now):s.day(now);for(int i=0;i<members.length();i++)s.check(date,members.getString(i),task,value);}
 public long expectedOrbs(String account)throws Exception{long sum=0;JSONArray a=list("orb_plans");for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(r.getString("account_id").equals(account)&&!r.getBoolean("received"))sum+=r.getInt("amount");}return sum;}
 public int orbTarget(String account)throws Exception{return s.account(account).optInt("orb_target",0);}
 public void setOrbTarget(String account,int value)throws Exception{if(value<0||value>HubState.MAX_ORBS)throw new IllegalArgumentException("目標数が範囲外です");s.account(account).put("orb_target",value);}
 public void collect(String id,Instant now)throws Exception{JSONObject p=find("orb_plans",id);if(p.getBoolean("received"))throw new IllegalArgumentException("すでに回収済みです");String account=p.getString("account_id");long next=(long)s.account(account).getInt("orbs")+p.getInt("amount");if(next>HubState.MAX_ORBS)throw new IllegalArgumentException("残高上限を超えます");s.setOrbs(account,(int)next,p.getString("name"),now);p.put("received",true);}
 public void deleteAccountReferences(String id)throws Exception{for(String table:new String[]{"inventory","goals","orb_plans"}){JSONArray a=list(table);for(int i=a.length()-1;i>=0;i--)if(id.equals(a.getJSONObject(i).optString("account_id")))a.remove(i);}JSONArray teams=list("teams");for(int i=teams.length()-1;i>=0;i--){JSONArray m=teams.getJSONObject(i).getJSONArray("members");for(int j=m.length()-1;j>=0;j--)if(id.equals(m.getString(j)))m.remove(j);if(m.length()==0)teams.remove(i);}JSONArray ms=list("missions");for(int i=0;i<ms.length();i++)ms.getJSONObject(i).getJSONObject("progress").remove(id);JSONArray fs=list("formations");for(int i=0;i<fs.length();i++){JSONArray m=fs.getJSONObject(i).getJSONArray("members");for(int j=m.length()-1;j>=0;j--)if(id.equals(m.getJSONObject(j).getString("account_id")))m.remove(j);}}
 public static void validate(HubState s)throws Exception{
  check(s.root.has("planner"));num(s.root,"bond_reset_hour",0,23);if(s.root.has("catalog_auto"))bool(s.root,"catalog_auto");if(s.root.has("catalog_unmetered"))bool(s.root,"catalog_unmetered");PlannerData p=s.planner();
  for(String table:TABLES){JSONArray a=p.list(table);check(a.length()<=10000);Set<String> seen=new HashSet<>();for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);check(seen.add(str(r,"id",100,false)));
   switch(table){
    case "custom_characters":check(r.getString("id").startsWith("custom:"));str(r,"name",300,false);check(java.util.Arrays.asList("火","水","木","光","闇").contains(str(r,"element",10,false)));str(r,"form",100,true);str(r,"no",40,true);break;
    case "inventory":account(s,r);str(r,"catalog_id",100,false);str(r,"name",300,false);str(r,"element",10,true);str(r,"form",100,true);num(r,"count",1,9999);num(r,"luck",0,999);bool(r,"trained");str(r,"fruit",1000,true);str(r,"target_fruit",1000,true);str(r,"memo",3000,true);break;
    case "teams":str(r,"name",100,false);JSONArray m=r.getJSONArray("members");check(m.length()>=1&&m.length()<=4);Set<String> members=new HashSet<>();for(int j=0;j<m.length();j++){s.account(m.getString(j));check(members.add(m.getString(j)));}break;
    case "missions":str(r,"name",200,false);YearMonth.parse(str(r,"period",7,false));due(r);JSONObject progress=r.getJSONObject("progress");for(Iterator<String> it=progress.keys();it.hasNext();){String key=it.next();s.account(key);num(progress,key,0,3);}str(r,"memo",3000,true);break;
    case "goals":account(s,r);str(r,"name",200,false);num(r,"current",0,999999);num(r,"target",1,999999);due(r);str(r,"memo",3000,true);break;
    case "formations":str(r,"name",200,false);str(r,"memo",6000,true);JSONArray f=r.getJSONArray("members");check(f.length()<=4);for(int j=0;j<f.length();j++){JSONObject slot=f.getJSONObject(j);account(s,slot);str(slot,"character",300,false);}String image=str(r,"image",2000000,true);if(!image.isEmpty())java.util.Base64.getDecoder().decode(image);break;
    case "orb_plans":account(s,r);str(r,"name",200,false);num(r,"amount",1,HubState.MAX_ORBS);bool(r,"received");due(r);break;
   }
  }}
  for(int i=0;i<s.accounts().length();i++){JSONObject a=s.accounts().getJSONObject(i);if(a.has("orb_target"))num(a,"orb_target",0,HubState.MAX_ORBS);}
 }
 private static void account(HubState s,JSONObject r)throws Exception{s.account(str(r,"account_id",100,false));}
 private static void due(JSONObject r)throws Exception{String d=str(r,"due",40,true);if(!d.isEmpty())OffsetDateTime.parse(d);bool(r,"notify");num(r,"lead_hours",0,168);if(r.has("notified_due"))str(r,"notified_due",80,true);}
 public static String str(JSONObject r,String k,int max,boolean empty)throws Exception{Object v=r.get(k);check(v instanceof String);String x=(String)v;check(x.length()<=max&&(empty||!x.trim().isEmpty()));return x;}
 public static int num(JSONObject r,String k,int min,int max)throws Exception{Object v=r.get(k);check(v instanceof Number);long n=((Number)v).longValue();check(((Number)v).doubleValue()==n&&n>=min&&n<=max);return (int)n;}
 private static void bool(JSONObject r,String k)throws Exception{check(r.get(k) instanceof Boolean);}
 private static void check(boolean ok){if(!ok)throw new IllegalArgumentException("管理データの入力内容・形式を確認してください");}
}

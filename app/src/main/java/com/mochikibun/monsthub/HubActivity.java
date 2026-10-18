package com.mochikibun.monsthub;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;
import java.io.*;
import org.json.*;

public final class HubActivity extends Activity {
 private static final int BG=0xff0b1220,CARD=0xff192a41,BLUE=0xff6bbdff,WHITE=0xffeef5ff,MUTED=0xffa4b6cb;
 private static final int EXPORT=410,IMPORT=411;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private HubStore store;private HubState state;private String page="home",accountFilter="";private LocalDate day;
 private LinearLayout body;private ScrollView scroll;private boolean fileBusy;
 private final Runnable clock=new Runnable(){public void run(){rollover();handler.postDelayed(this,15000);}};
 interface Edit {void apply(HubState s)throws Exception;}
 @Override public void onCreate(Bundle b){super.onCreate(b);store=new HubStore(this);
  if(b!=null){page=b.getString("page","home");accountFilter=b.getString("accountFilter","");}
  try{state=store.load();day=state.day(Instant.now());render();}catch(Exception e){recovery();}
 }
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("page",page);b.putString("accountFilter",accountFilter);}
 @Override protected void onResume(){super.onResume();handler.removeCallbacks(clock);handler.post(clock);}
 @Override protected void onPause(){handler.removeCallbacks(clock);super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy();}
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(8),dp(7),dp(8),dp(7));return t;}
 private void title(String s){TextView t=text(s,24,WHITE);t.setTypeface(null,Typeface.BOLD);body.addView(t);}
 private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(BLUE);b.setBackground(bg(CARD));b.setMinHeight(dp(52));b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setOnClickListener(v->action.run());return b;}
 private void action(LinearLayout box,String s,Runnable action){Button b=button(s,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));box.addView(b,lp);}
 private void space(LinearLayout box){Space s=new Space(this);box.addView(s,new LinearLayout.LayoutParams(1,dp(10)));}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(10),dp(10),dp(10),dp(10));l.setBackground(bg(CARD));body.addView(l);space(body);return l;}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void error(Exception e){new AlertDialog.Builder(this).setTitle("保存・読み込みを完了できませんでした").setMessage(e.getMessage()==null?"入力内容またはファイルを確認してください":e.getMessage()).setPositiveButton("閉じる",null).show();}
 private void navigate(String p){page=p;accountFilter="";render();}
 private void change(Edit edit,boolean rerender){try{HubState copy=state.copy();edit.apply(copy);store.save(copy);state=copy;if(rerender)render();}catch(Exception e){error(e);render();}}
 private void rollover(){if(state==null)return;LocalDate now=state.day(Instant.now());if(!now.equals(day)){day=now;render();}}
 private boolean currentDay(){LocalDate now=state.day(Instant.now());if(!now.equals(day)){day=now;render();toast("新しい日付に切り替わりました。もう一度操作してください");return false;}return true;}
 private void base(){LinearLayout root=column();root.setBackgroundColor(BG);root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});scroll=new ScrollView(this);scroll.setFillViewport(true);body=column();body.setPadding(dp(16),dp(12),dp(16),dp(28));scroll.addView(body);root.addView(scroll);setContentView(root);root.requestApplyInsets();}
 private void recovery(){state=null;base();title("データを読み込めませんでした");body.addView(text("保存データは上書きしていません。バックアップがあれば復元できます。",15,WHITE));action(body,"バックアップを選んで復元",()->importFile());action(body,"もう一度読み込む",()->{try{state=store.load();day=state.day(Instant.now());render();}catch(Exception e){error(e);}});}
 private void render(){if(state==null)return;try{base();if(!page.equals("home"))action(body,"‹ ホーム",()->navigate("home"));
  switch(page){case "accounts":accounts();break;case "daily":daily(false);break;case "bond":daily(true);break;case "orbs":orbs();break;case "urls":urls();break;case "settings":settings();break;default:home();}
 }catch(Exception e){error(e);}}
 private void home()throws Exception {
  title("MONST HUB");body.addView(text("PERSONAL MONST MANAGER  •  v0.2",12,MUTED));
  LinearLayout stats=new LinearLayout(this);stat(stats,"登録アカウント",String.valueOf(state.accounts().length()));stat(stats,"日課完了",state.completedAccounts(day)+" / "+state.accounts().length());stat(stats,"合計オーブ",String.format(Locale.JAPAN,"%,d",state.totalOrbs()));body.addView(stats);space(body);
  body.addView(text(day+" の記録  •  日本時間 "+state.resetHour()+"時切替",13,MUTED));
  menuPair("サブ垢管理","名前・メモ",()->navigate("accounts"),"日課チェック","今日の進捗",()->navigate("daily"));
  menuPair("オーブ管理","残高・増減履歴",()->navigate("orbs"),"キャンペーン","月間カレンダー",()->startActivity(new Intent(this,MainActivity.class)));
  menuPair("マルチURL管理","URLを保存・起動",()->navigate("urls"),"設定・バックアップ","保存・復元",()->navigate("settings"));
  menu("絆チェック","今日の完了・まとめてリセット",()->navigate("bond"));
  body.addView(text("各項目は手動で記録します。ゲームのデータとは自動連携しません。",12,MUTED));
 }
 private void stat(LinearLayout row,String name,String value){LinearLayout c=column();c.setBackground(bg(CARD));TextView v=text(value,21,BLUE);v.setGravity(Gravity.CENTER);c.addView(v);TextView n=text(name,10,MUTED);n.setGravity(Gravity.CENTER);c.addView(n);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),0,dp(2),0);row.addView(c,lp);}
 private void menuPair(String a,String subA,Runnable actionA,String b,String subB,Runnable actionB){LinearLayout r=new LinearLayout(this);Button left=button(a+"\n"+subA,actionA),right=button(b+"\n"+subB,actionB);left.setMinHeight(dp(100));right.setMinHeight(dp(100));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(0,0,dp(5),0);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1);rp.setMargins(dp(5),0,0,0);r.addView(left,lp);r.addView(right,rp);body.addView(r,new LinearLayout.LayoutParams(-1,dp(116)));space(body);}
 private void menu(String name,String subtitle,Runnable run){action(body,name+"\n"+subtitle,run);space(body);}
 private void accounts()throws Exception {title("サブ垢管理");body.addView(text(state.accounts().length()+"垢登録中／名前をタップして編集",13,MUTED));action(body,"＋ アカウントを追加",()->editAccount(null));
  if(state.accounts().length()==0)body.addView(text("最初のアカウントを追加してください",15,WHITE));
  for(int i=0;i<state.accounts().length();i++){JSONObject a=state.accounts().getJSONObject(i);String id=a.getString("id");LinearLayout c=card();action(c,a.getString("name")+"  ›",()->editAccount(id));c.addView(text("オーブ "+a.getInt("orbs")+"  •  日課 "+state.completed(day,id)+"/"+state.tasks().length()+"  •  絆 "+(state.checked(day,id,"bond")?"完了":"未完了"),13,BLUE));if(!a.getString("memo").isEmpty())c.addView(text(a.getString("memo"),14,MUTED));}
 }
 private EditText input(LinearLayout box,String label,String value,boolean numeric){box.addView(text(label,13,MUTED));EditText e=new EditText(this);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setText(value);e.setTextSize(16);e.setSingleLine(numeric);if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);e.setPadding(dp(8),dp(8),dp(8),dp(8));box.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
 private LinearLayout form(){LinearLayout l=column();l.setPadding(dp(18),dp(6),dp(18),dp(12));return l;}
 private ScrollView formScroll(LinearLayout l){ScrollView sc=new ScrollView(this);sc.addView(l);return sc;}
 private void editAccount(String id){try{JSONObject a=id==null?null:state.account(id);LinearLayout l=form();EditText n=input(l,"アカウント名（60文字以内）",a==null?"":a.getString("name"),false);EditText m=input(l,"メモ（2000文字以内）",a==null?"":a.getString("memo"),false);
  AlertDialog d=new AlertDialog.Builder(this).setTitle(id==null?"アカウント追加":"アカウント編集").setView(formScroll(l)).setPositiveButton("保存",null).setNegativeButton("キャンセル",null).create();
  if(id!=null)action(l,"このアカウントを削除",()->new AlertDialog.Builder(this).setTitle("アカウントを削除しますか？").setMessage("この垢の日課・絆・オーブ履歴・URLも削除します。ほかの垢は残ります。").setNegativeButton("キャンセル",null).setPositiveButton("削除",(v,w)->{change(s->s.deleteAccount(id),true);d.dismiss();}).show());
  d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{try{HubState copy=state.copy();if(id==null){String key=copy.addAccount(n.getText().toString());copy.editAccount(key,n.getText().toString(),m.getText().toString(),"");}else copy.editAccount(id,n.getText().toString(),m.getText().toString(),state.account(id).getString("url"));store.save(copy);state=copy;d.dismiss();render();}catch(Exception e){error(e);}}));d.show();
 }catch(Exception e){error(e);}}
 private void filter()throws Exception {if(state.accounts().length()==0)return;Spinner sp=new Spinner(this);String[] labels=new String[state.accounts().length()+1],ids=new String[labels.length];labels[0]="すべてのアカウント";ids[0]="";int selected=0;for(int i=1;i<labels.length;i++){JSONObject a=state.accounts().getJSONObject(i-1);labels[i]=a.getString("name");ids[i]=a.getString("id");if(ids[i].equals(accountFilter))selected=i;}
  ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);sp.setAdapter(adapter);sp.setSelection(selected);body.addView(sp,new LinearLayout.LayoutParams(-1,dp(52)));sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long row){if(!ids[pos].equals(accountFilter)){accountFilter=ids[pos];render();}}});}
 private void daily(boolean bond)throws Exception {title(bond?"絆チェック":"日課チェック");body.addView(text(day+" の記録／毎朝"+state.resetHour()+"時に切替（日本時間）",13,MUTED));
  if(bond)action(body,"今日の絆チェックをすべてリセット",()->resetBond());else action(body,"日課の項目を編集",()->taskEditor());filter();
  if(state.accounts().length()==0)body.addView(text("サブ垢管理でアカウントを追加してください",15,WHITE));
  for(int i=0;i<state.accounts().length();i++){JSONObject a=state.accounts().getJSONObject(i);String id=a.getString("id");if(!accountFilter.isEmpty()&&!accountFilter.equals(id))continue;LinearLayout c=card();c.addView(text(a.getString("name"),18,WHITE));
   if(bond)check(c,"今日の絆が完了",id,"bond");else {for(int j=0;j<state.tasks().length();j++){JSONObject t=state.tasks().getJSONObject(j);check(c,t.getString("name"),id,t.getString("id"));}if(state.tasks().length()==0)c.addView(text("「日課の項目を編集」から追加できます",14,MUTED));}
  }
 }
 private void check(LinearLayout box,String label,String id,String key){CheckBox cb=new CheckBox(this);cb.setText(label);cb.setTextSize(16);cb.setTextColor(WHITE);cb.setMinHeight(dp(52));cb.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));cb.setChecked(state.checked(day,id,key));cb.setOnCheckedChangeListener((v,on)->{if(currentDay())change(s->s.check(day,id,key,on),false);});box.addView(cb);}
 private void resetBond(){if(!currentDay())return;new AlertDialog.Builder(this).setTitle("今日の絆をリセットしますか？").setMessage("すべての垢の当日分を未完了に戻します。過去の記録と日課チェックは残ります。").setNegativeButton("キャンセル",null).setPositiveButton("リセット",(d,w)->{if(currentDay())change(s->{for(int i=0;i<s.accounts().length();i++)s.check(day,s.accounts().getJSONObject(i).getString("id"),"bond",false);},true);}).show();}
 private void taskEditor(){try{final AlertDialog[] editor=new AlertDialog[1];LinearLayout l=form();l.addView(text("全アカウント共通の項目です。削除した項目のチェック記録も消えます。",13,MUTED));for(int i=0;i<state.tasks().length();i++){JSONObject t=state.tasks().getJSONObject(i);String id=t.getString("id");action(l,t.getString("name")+" を削除",()->new AlertDialog.Builder(this).setTitle("日課を削除しますか？").setMessage(t.optString("name")).setNegativeButton("キャンセル",null).setPositiveButton("削除",(v,w)->{change(s->s.deleteTask(id),true);if(editor[0]!=null)editor[0].dismiss();}).show());}
  EditText n=input(l,"追加する日課の名前","",false);AlertDialog d=new AlertDialog.Builder(this).setTitle("日課の項目").setView(formScroll(l)).setNegativeButton("閉じる",(a,b)->render()).setPositiveButton("追加",null).create();editor[0]=d;d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{HubState copy=state.copy();copy.addTask(n.getText().toString());store.save(copy);state=copy;d.dismiss();render();}catch(Exception e){error(e);}}));d.show();
 }catch(Exception e){error(e);}}
 private void orbs()throws Exception {title("オーブ管理");body.addView(text("合計 "+String.format(Locale.JAPAN,"%,d",state.totalOrbs())+" オーブ",20,BLUE));body.addView(text("残高をタップして入力、または＋／−で増減を記録",13,MUTED));filter();
  for(int i=0;i<state.accounts().length();i++){JSONObject a=state.accounts().getJSONObject(i);String id=a.getString("id");if(!accountFilter.isEmpty()&&!accountFilter.equals(id))continue;LinearLayout c=card();action(c,a.getString("name")+"  •  "+a.getInt("orbs")+" オーブ",()->orbEdit(id,0));LinearLayout r=new LinearLayout(this);r.addView(button("＋ 増やす",()->orbEdit(id,1)),new LinearLayout.LayoutParams(0,-2,1));r.addView(button("− 減らす",()->orbEdit(id,-1)),new LinearLayout.LayoutParams(0,-2,1));c.addView(r);}
  body.addView(text("最近の増減（最大50件表示）",18,WHITE));int shown=0;JSONArray h=state.history();for(int i=h.length()-1;i>=0&&shown<50;i--){JSONObject item=h.getJSONObject(i);String id=item.getString("account_id");if(!accountFilter.isEmpty()&&!accountFilter.equals(id))continue;shown++;String at=Instant.parse(item.getString("at")).atZone(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));int delta=item.getInt("delta");body.addView(text(at+"  "+state.account(id).getString("name")+"  "+(delta>0?"+":"")+delta+" → "+item.getInt("balance")+"\n"+item.getString("memo"),14,MUTED));}if(shown==0)body.addView(text("まだ履歴はありません",14,MUTED));
 }
 private void orbEdit(String id,int mode){try{JSONObject a=state.account(id);LinearLayout l=form();EditText amount=input(l,mode==0?"現在のオーブ数":mode>0?"増やす数":"減らす数",mode==0?String.valueOf(a.getInt("orbs")):"",true);EditText memo=input(l,"理由・メモ（任意／200文字以内）","",false);
  AlertDialog d=new AlertDialog.Builder(this).setTitle(a.getString("name")).setView(formScroll(l)).setNegativeButton("キャンセル",null).setPositiveButton("保存",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{long value=Long.parseLong(amount.getText().toString().trim());if(value<0||value>HubState.MAX_ORBS)throw new IllegalArgumentException("0〜99,999,999で入力してください");long result=mode==0?value:state.account(id).getInt("orbs")+value*mode;if(result<0||result>HubState.MAX_ORBS)throw new IllegalArgumentException("残高が範囲外になります");HubState copy=state.copy();copy.setOrbs(id,(int)result,memo.getText().toString(),Instant.now());store.save(copy);state=copy;d.dismiss();render();}catch(NumberFormatException e){toast("数字を入力してください");}catch(Exception e){error(e);}}));d.show();
 }catch(Exception e){error(e);}}
 private void urls()throws Exception {title("マルチURL管理");body.addView(text("自分用のURLをアカウントごとに保存します。URLの発行やほかの人への共有は行いません。",13,MUTED));filter();
  for(int i=0;i<state.accounts().length();i++){JSONObject a=state.accounts().getJSONObject(i);String id=a.getString("id"),url=a.getString("url");if(!accountFilter.isEmpty()&&!accountFilter.equals(id))continue;LinearLayout c=card();c.addView(text(a.getString("name"),18,WHITE));TextView link=text(url.isEmpty()?"URL未登録":url,13,MUTED);link.setTextIsSelectable(true);c.addView(link);action(c,"URLを編集",()->urlEdit(id));if(!url.isEmpty()){action(c,"URLを開く ↗",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("このURLを開けるアプリがありません");}});action(c,"URLをコピー",()->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("マルチURL",url));toast("コピーしました");});}}
 }
 private void urlEdit(String id){try{LinearLayout l=form();EditText url=input(l,"マルチURL（http / https）",state.account(id).getString("url"),false);AlertDialog d=new AlertDialog.Builder(this).setTitle("URLを保存").setView(formScroll(l)).setNegativeButton("キャンセル",null).setPositiveButton("保存",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{HubState copy=state.copy();JSONObject a=copy.account(id);copy.editAccount(id,a.getString("name"),a.getString("memo"),url.getText().toString().trim());store.save(copy);state=copy;d.dismiss();render();}catch(Exception e){error(e);}}));d.show();}catch(Exception e){error(e);}}
 private void settings(){title("設定・バックアップ");body.addView(text("日課・絆の切替：毎朝"+state.resetHour()+"時（日本時間）\n切替前の記録は保存され、次の日は未完了から始まります。",15,WHITE));
  action(body,"切替時刻を変更",()->{String[] hs=new String[24];for(int i=0;i<24;i++)hs[i]=i+"時";new AlertDialog.Builder(this).setTitle("日課・絆の切替時刻（日本時間）").setSingleChoiceItems(hs,state.resetHour(),(d,w)->{change(s->s.setResetHour(w),false);day=state.day(Instant.now());d.dismiss();render();}).setNegativeButton("キャンセル",null).show();});
  action(body,fileBusy?"ファイル処理中…":"バックアップを保存（JSON）",()->exportFile());action(body,"バックアップから復元",()->importFile());
  body.addView(text("保存内容：アカウント、日課・絆の記録、オーブ残高と履歴、URL、設定。\nバックアップは選んだ場所に保存します。復元は今の管理データを丸ごと置き換えます。",14,MUTED));
  body.addView(text("データはこの端末内に保存しています。アンインストールや機種変更の前にバックアップしてください。別の人は自分のバックアップを使えます。",14,MUTED));
  body.addView(text("MONST HUB v0.2.0\n個人用・非公式管理アプリ",13,MUTED));
 }
 private void exportFile(){if(fileBusy||state==null)return;try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"monst-hub-"+LocalDate.now(ZoneId.of("Asia/Tokyo"))+".json");startActivityForResult(i,EXPORT);}catch(Exception e){error(e);}}
 private void importFile(){if(fileBusy)return;try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,IMPORT);}catch(Exception e){error(e);}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(request!=EXPORT&&request!=IMPORT)return;fileBusy=true;
  if(request==EXPORT){String snapshot=state.json();io.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("保存先を開けません");out.write(snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8));handler.post(()->{fileBusy=false;if(!isDestroyed())toast("バックアップを保存しました");});}catch(Exception e){handler.post(()->{fileBusy=false;if(!isDestroyed())error(e);});}});}
  else io.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){HubState restored=HubState.parse(HubStore.read(in));handler.post(()->{fileBusy=false;if(isDestroyed())return;confirmRestore(restored);});}catch(Exception e){handler.post(()->{fileBusy=false;if(!isDestroyed())error(e);});}});
 }
 private void confirmRestore(HubState restored){try{new AlertDialog.Builder(this).setTitle("バックアップを復元しますか？").setMessage(restored.accounts().length()+"垢のデータで、現在の管理データを置き換えます。必要なら先に今のデータをバックアップしてください。").setNegativeButton("キャンセル",null).setPositiveButton("復元",(d,w)->{try{store.save(restored);state=restored;day=state.day(Instant.now());page="home";accountFilter="";render();toast("復元しました");}catch(Exception e){error(e);}}).show();}catch(Exception e){error(e);}}
}

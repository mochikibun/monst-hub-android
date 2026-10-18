package com.mochikibun.monsthub;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
public final class MainActivity extends Activity {
 private static final int BG=Color.rgb(8,8,8),CARD=Color.rgb(22,22,22),ACCENT=Color.rgb(244,244,244),TEXT=Color.rgb(244,244,244),MUTED=Color.rgb(155,155,155);
 private static final ZoneId JST=ZoneId.of("Asia/Tokyo");
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private LocalDate selected=LocalDate.now(JST); private YearMonth month=YearMonth.from(selected);
 private String filter="all";private FeedRepository.Result data;private boolean loading;
 private LinearLayout root,body;private ScrollView scroll; private TextView eventHeading;
 @Override public void onCreate(Bundle state){super.onCreate(state);
  if(state!=null){selected=LocalDate.parse(state.getString("selected",selected.toString()));month=YearMonth.parse(state.getString("month",month.toString()));filter=state.getString("filter","all");}
  render();refresh();
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("selected",selected.toString());out.putString("month",month.toString());out.putString("filter",filter);}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
 private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));d.setStroke(dp(1),Color.rgb(48,48,48));return d;}
 private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
 private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(ACCENT);b.setBackground(bg(CARD));b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(48));b.setPadding(dp(6),0,dp(6),0);b.setOnClickListener(v->action.run());return b;}
 private void addSpace(LinearLayout l){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(10)));}
 private void refresh(){if(loading)return;loading=true;render();FeedRepository repo=new FeedRepository(getApplicationContext());worker.execute(()->{FeedRepository.Result result=repo.load();runOnUiThread(()->{if(isDestroyed())return;data=result;loading=false;render();});});}
 private String time(String raw){try{return OffsetDateTime.parse(raw).atZoneSameInstant(JST).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm 'JST'"));}catch(Exception e){return raw;}}
 private void render(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(10),dp(12),dp(24));scroll.addView(body);root.addView(scroll);setContentView(root);root.requestApplyInsets();
  TextView title=label("MONST HUB",27,ACCENT);title.setTypeface(null,Typeface.BOLD);body.addView(title);
  body.addView(label("キャンペーンカレンダー • 日本時間",13,MUTED));
  Button update=button(loading?"取得中…":"↻ データを更新",()->refresh());update.setEnabled(!loading);body.addView(update);
  if(data!=null){
   if(!data.error.isEmpty())notice(data.error+(data.cached?"\n前回取得データを表示しています。":""));
   if(!data.warning.isEmpty())notice(data.warning);
   if(data.feed!=null)body.addView(label("データ最終更新："+time(data.feed.generatedAt),12,MUTED));
  }
  addSpace(body); LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
  nav.addView(button("‹ 前月",()->move(-1)),new LinearLayout.LayoutParams(0,dp(48),1));
  TextView heading=label(month.getYear()+"年"+month.getMonthValue()+"月",20,TEXT);heading.setGravity(Gravity.CENTER);heading.setTypeface(null,Typeface.BOLD);nav.addView(heading,new LinearLayout.LayoutParams(0,dp(48),2));
  nav.addView(button("翌月 ›",()->move(1)),new LinearLayout.LayoutParams(0,dp(48),1));body.addView(nav);
  LinearLayout jumps=new LinearLayout(this);
  jumps.addView(button("今日",()->{selected=LocalDate.now(JST);month=YearMonth.from(selected);render();}),new LinearLayout.LayoutParams(0,dp(48),1));
  jumps.addView(button("2026年10月",()->{month=YearMonth.of(2026,10);selected=month.atDay(18);render();}),new LinearLayout.LayoutParams(0,dp(48),1));body.addView(jumps);
  addSpace(body);HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);
  String[] keys={"all","archive","temple","normal","other"};for(String key:keys){Button b=button(key.equals("all")?"すべて":Campaign.categoryLabel(key),()->{filter=key;render();});if(filter.equals(key)){b.setBackground(bg(ACCENT));b.setTextColor(BG);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(76),dp(48));lp.setMargins(0,0,dp(6),0);row.addView(b,lp);}filters.addView(row);body.addView(filters);addSpace(body);
  calendar();body.addView(label("数字の下は選択カテゴリの件数／●は今日",11,MUTED));addSpace(body);
  eventHeading=label(selected.format(DateTimeFormatter.ofPattern("M月d日（E）",Locale.JAPAN))+" のイベント",20,ACCENT);body.addView(eventHeading);
  if(data==null||data.feed==null)body.addView(label(loading?"データを読み込んでいます…":"表示できるデータがありません。上の更新ボタンで再試行してください。",15,TEXT));
  else {int count=0;for(Campaign c:data.feed.campaigns)if(c.active(selected,filter)){count++;event(c);}if(count==0)body.addView(label("該当するイベントは配信データにありません。\n未掲載・取得漏れの可能性もあるため、公式情報をご確認ください。",14,MUTED));}
  addSpace(body);body.addView(label("非公式の補助アプリです。開催時間・詳細は公式情報をご確認ください。",12,MUTED));
 }
 private void notice(String s){TextView t=label(s,14,Color.rgb(225,225,225));t.setBackground(bg(Color.rgb(35,35,35)));body.addView(t);addSpace(body);}
 private void move(int by){month=month.plusMonths(by);selected=month.atDay(Math.min(selected.getDayOfMonth(),month.lengthOfMonth()));render();}
 private void calendar(){
  LinearLayout days=new LinearLayout(this);for(String w:new String[]{"日","月","火","水","木","金","土"}){TextView t=label(w,13,MUTED);t.setGravity(Gravity.CENTER);days.addView(t,new LinearLayout.LayoutParams(0,dp(32),1));}body.addView(days);
  int offset=month.atDay(1).getDayOfWeek().getValue()%7;int rows=(offset+month.lengthOfMonth()+6)/7;
  for(int r=0;r<rows;r++){LinearLayout line=new LinearLayout(this);for(int col=0;col<7;col++){int day=r*7+col-offset+1;
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(64),1);lp.setMargins(dp(1),dp(2),dp(1),dp(2));
   if(day<1||day>month.lengthOfMonth()){line.addView(new Space(this),lp);continue;}
   LocalDate date=month.atDay(day);int count=0;if(data!=null&&data.feed!=null)for(Campaign c:data.feed.campaigns)if(c.active(date,filter))count++;
   boolean known=data!=null&&data.feed!=null;String mark=date.equals(LocalDate.now(JST))?"●":"";
   Button b=button(day+mark+"\n"+(known?count+"件":"—"),()->{selected=date;render();scroll.post(()->scroll.smoothScrollTo(0,eventHeading.getTop()));});b.setTextSize(12);b.setGravity(Gravity.CENTER);b.setContentDescription(date+" "+(known?count+"件":"データ未取得"));
   if(selected.equals(date)){b.setBackground(bg(ACCENT));b.setTextColor(BG);}line.addView(b,lp);
  }body.addView(line);}
 }
 private void event(Campaign c){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackground(bg(CARD));
  card.addView(label(Campaign.categoryLabel(c.category)+"  •  当日の属性："+c.attribute(selected),13,ACCENT));TextView n=label(c.name,18,TEXT);n.setTypeface(null,Typeface.BOLD);card.addView(n);
  card.addView(label(c.start+" 〜 "+c.end+"（両日を含む）",12,MUTED));
  if(c.sourceUrl.startsWith("https://")){card.addView(button("掲載元を開く ↗",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(c.sourceUrl)));}catch(Exception e){Toast.makeText(this,"ブラウザーを開けませんでした",Toast.LENGTH_SHORT).show();}}));}
  body.addView(card);addSpace(body);
 }
}

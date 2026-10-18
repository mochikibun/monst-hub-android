package com.mochikibun.monsthub;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public class FeedTest {
 private String fixture(String file)throws Exception {try(InputStream in=getClass().getResourceAsStream("/"+file)){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 @Test public void realFeedAndInclusiveDates()throws Exception {
  FeedParser.Feed feed=FeedParser.parse(fixture("campaigns.json"));assertEquals(7,feed.campaigns.size());
  Campaign first=feed.campaigns.get(0);assertTrue(first.active(LocalDate.parse("2026-10-16"),"all"));assertTrue(first.active(LocalDate.parse("2026-10-20"),"archive"));assertFalse(first.active(LocalDate.parse("2026-10-21"),"all"));assertFalse(first.active(LocalDate.parse("2026-10-18"),"temple"));assertEquals("火",first.attribute(LocalDate.parse("2026-10-18")));assertEquals("指定なし",first.attribute(LocalDate.parse("2026-10-17")));
 }
 @Test public void actualDegradedStatus()throws Exception {assertTrue(FeedParser.statusWarning(fixture("status.json")).contains("不完全"));assertEquals("",FeedParser.statusWarning("{\"schema_version\":1,\"status\":\"ok\"}"));}
 @Test(expected=Exception.class)public void malformedFeedRejected()throws Exception{FeedParser.parse("{broken");}
 @Test(expected=Exception.class)public void unknownSchemaRejected()throws Exception{FeedParser.parse("{\"schema_version\":2,\"campaigns\":[]}");}
 @Test public void emptyFeedIsValid()throws Exception{assertTrue(FeedParser.parse("{\"schema_version\":1,\"generated_at\":\"2026-10-18T00:00:00+09:00\",\"campaigns\":[]}").campaigns.isEmpty());}
 @Test public void octoberGridAndLeapYear(){assertEquals(4,YearMonth.of(2026,10).atDay(1).getDayOfWeek().getValue()%7);assertEquals(31,YearMonth.of(2026,10).lengthOfMonth());assertEquals(29,YearMonth.of(2028,2).lengthOfMonth());}
}

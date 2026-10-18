package com.mochikibun.monsthub;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Atomic writes: never replace a valid file with an incomplete write. */
public final class HubStore {
 private final AtomicFile file;
 public HubStore(Context c){file=new AtomicFile(new File(c.getFilesDir(),"hub-state-v1.json"));}
 public HubState load()throws Exception{
  if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists()){HubState s=HubState.create();save(s);return s;}
  try(InputStream in=file.openRead()){return HubState.parse(read(in));}
 }
 public void save(HubState state)throws Exception {
  HubState.parse(state.json());byte[] bytes=state.json().getBytes(StandardCharsets.UTF_8);if(bytes.length>20_000_000)throw new IOException("データ量が保存上限を超えています");
  FileOutputStream stream=null;try{stream=file.startWrite();stream.write(bytes);file.finishWrite(stream);}catch(Exception e){if(stream!=null)file.failWrite(stream);throw e;}
 }
 public static String read(InputStream in)throws Exception{
  if(in==null)throw new IOException("ファイルを開けません");try(ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>20_000_000)throw new IOException("ファイルは20MB以内にしてください");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}
 }
}

package com.soudi.xmleditor;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import android.view.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public class MainActivity extends Activity {
 EditText editor, search; Uri file; int next=0; boolean binary=false; String rootPath;
 public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,32,16,16);
 LinearLayout bar=new LinearLayout(this);
 Button open=new Button(this);open.setText("Open");bar.addView(open);
 Button overwrite=new Button(this);overwrite.setText("Save");bar.addView(overwrite);overwrite.setOnClickListener(v->saveOriginal());
 Button save=new Button(this);save.setText("Save as");bar.addView(save);root.addView(bar);
 Button rootOpen=new Button(this);rootOpen.setText("Open as root");root.addView(rootOpen);rootOpen.setOnClickListener(v->chooseRoot());
 search=new EditText(this);search.setSingleLine(true);search.setHint("Search XML");root.addView(search);
 Button find=new Button(this);find.setText("Find next");root.addView(find);
 editor=new EditText(this);editor.setGravity(Gravity.TOP);editor.setTypeface(android.graphics.Typeface.MONOSPACE);editor.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
 open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
 save.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(binary?"application/octet-stream":"application/xml");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,binary?"edited.abx":"edited.xml");startActivityForResult(i,2);});
 find.setOnClickListener(v->{String q=search.getText().toString(),s=editor.getText().toString();if(q.isEmpty())return;int p=s.indexOf(q,Math.min(next,s.length()));if(p<0)p=s.indexOf(q);if(p>=0){editor.requestFocus();editor.setSelection(p,p+q.length());next=p+q.length();}else toast("No match");});
 if(Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData()!=null) load(getIntent().getData());
 }
 void load(Uri uri){ Intent d=new Intent();d.setData(uri);onActivityResult(1,RESULT_OK,d); }
 void saveOriginal(){
 if(rootPath!=null){saveRoot();return;}
 if(file==null){toast("Open a document first");return;}
 new AlertDialog.Builder(this).setTitle("Overwrite document?").setMessage("Save changes to the opened document.").setNegativeButton("Cancel",null).setPositiveButton("Save",(dialog,which)->{
 try{
 byte[] output=binary?AbxWriter.encode(editor.getText().toString()):editor.getText().toString().getBytes(StandardCharsets.UTF_8);
 try(OutputStream out=getContentResolver().openOutputStream(file,"wt")){if(out==null)throw new IOException("Provider unavailable");out.write(output);}
 toast("Saved original document");
 }catch(Exception e){toast("Cannot save original: "+e.getMessage()+". Use Save As.");}
 }).show();
 }
 String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
 byte[] readAll(InputStream in)throws IOException{try(ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 byte[] rootCommand(String command,byte[] input)throws Exception{
 java.lang.Process p=new ProcessBuilder("su","-c",command).redirectErrorStream(false).start();
 java.util.concurrent.FutureTask<byte[]> errors=new java.util.concurrent.FutureTask<>(()->readAll(p.getErrorStream()));new Thread(errors).start();
 try{try(OutputStream out=p.getOutputStream()){if(input!=null)out.write(input);}
 byte[] result=readAll(p.getInputStream());int code=p.waitFor();byte[] err=errors.get();if(code!=0)throw new IOException("su exit "+code+": "+new String(err,StandardCharsets.UTF_8));return result;}finally{p.destroy();}
 }
 void chooseRoot(){
 EditText path=new EditText(this);path.setSingleLine(true);path.setHint("/absolute/path/file.xml");
 new AlertDialog.Builder(this).setTitle("Open as root").setView(path).setNegativeButton("Cancel",null).setPositiveButton("Open",(d,w)->{
 String target=path.getText().toString().trim();if(!target.startsWith("/")||target.indexOf(0)>=0){toast("Enter an absolute path");return;}
 new Thread(()->{try{byte[] data=rootCommand("test -f "+quote(target)+" && cat "+quote(target),null);
 boolean abx=data.length>=4&&data[0]==65&&data[1]==66&&data[2]==88&&data[3]==0;
 String decoded=abx?AbxReader.decode(data):new String(data,StandardCharsets.UTF_8);
 runOnUiThread(()->{editor.setText(decoded);binary=abx;rootPath=target;file=null;next=0;toast("Opened as root");});
 }catch(Exception e){runOnUiThread(()->toast(e.toString()));}}).start();
 }).show();
 }
 void saveRoot(){
 final String target=rootPath;
 new AlertDialog.Builder(this).setTitle("Overwrite as root?").setMessage(target+"\nA backup will be created before writing. System files may be overwritten by Android itself.").setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
 final String xml=editor.getText().toString();final boolean abx=binary;
 new Thread(()->{try{byte[] output=abx?AbxWriter.encode(xml):xml.getBytes(StandardCharsets.UTF_8);
 String q=quote(target);String backup=quote(target+".editor-backup-"+System.currentTimeMillis());
 rootCommand("test -f "+q+" && cp -p "+q+" "+backup+" && cat > "+q,output);
 byte[] check=rootCommand("cat "+q,null);if(!java.util.Arrays.equals(output,check))throw new IOException("Read-back verification failed; backup: "+backup);
 runOnUiThread(()->toast("Saved as root; backup created"));
 }catch(Exception e){runOnUiThread(()->toast(e.toString()));}}).start();
 }).show();
 }
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{if(r==1){try(InputStream in=getContentResolver().openInputStream(d.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);byte[] data=out.toByteArray();if(data.length>=4&&data[0]==65&&data[1]==66&&data[2]==88&&data[3]==0){editor.setText(AbxReader.decode(data));rootPath=null;file=d.getData();binary=true;next=0;toast("ABX opened");return;}binary=false;editor.setText(new String(data,StandardCharsets.UTF_8));file=d.getData();next=0;}}else if(r==2){byte[] output=binary?AbxWriter.encode(editor.getText().toString()):editor.getText().toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=getContentResolver().openOutputStream(d.getData(),"wt")){out.write(output);}toast("Saved");}}catch(Exception e){toast(e.toString());}}
}

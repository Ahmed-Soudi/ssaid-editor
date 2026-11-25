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
 EditText editor, search; Uri file; int next=0; boolean binary=false;
 public void onCreate(Bundle b){super.onCreate(b);
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,32,16,16);
 LinearLayout bar=new LinearLayout(this);
 Button open=new Button(this);open.setText("Open");bar.addView(open);
 Button save=new Button(this);save.setText("Save as");bar.addView(save);root.addView(bar);
 search=new EditText(this);search.setSingleLine(true);search.setHint("Search XML");root.addView(search);
 Button find=new Button(this);find.setText("Find next");root.addView(find);
 editor=new EditText(this);editor.setGravity(Gravity.TOP);editor.setTypeface(android.graphics.Typeface.MONOSPACE);editor.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
 open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
 save.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(binary?"application/octet-stream":"application/xml");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,binary?"edited.abx":"edited.xml");startActivityForResult(i,2);});
 find.setOnClickListener(v->{String q=search.getText().toString(),s=editor.getText().toString();if(q.isEmpty())return;int p=s.indexOf(q,Math.min(next,s.length()));if(p<0)p=s.indexOf(q);if(p>=0){editor.requestFocus();editor.setSelection(p,p+q.length());next=p+q.length();}else toast("No match");});
 }
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{if(r==1){try(InputStream in=getContentResolver().openInputStream(d.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);byte[] data=out.toByteArray();if(data.length>=4&&data[0]==65&&data[1]==66&&data[2]==88&&data[3]==0){editor.setText(AbxReader.decode(data));binary=true;next=0;toast("ABX opened");return;}binary=false;editor.setText(new String(data,StandardCharsets.UTF_8));next=0;}}else if(r==2){byte[] output=binary?AbxWriter.encode(editor.getText().toString()):editor.getText().toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=getContentResolver().openOutputStream(d.getData(),"wt")){out.write(output);}toast("Saved");}}catch(Exception e){toast(e.toString());}}
}

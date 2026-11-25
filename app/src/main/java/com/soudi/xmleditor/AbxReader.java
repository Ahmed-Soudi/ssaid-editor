package com.soudi.xmleditor;
import java.io.*;
import java.util.*;
import android.util.Xml;
import org.xmlpull.v1.XmlSerializer;

public final class AbxReader {
 public static String decode(byte[] bytes) throws IOException {
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
  if(in.readInt()!=0x41425800) throw new IOException("Not ABX v0");
  ArrayList<String> pool=new ArrayList<>();
  StringWriter out=new StringWriter();
  XmlSerializer xml=Xml.newSerializer(); xml.setOutput(out);
  xml.startDocument("UTF-8",null);
  ArrayDeque<String> tags=new ArrayDeque<>();
  while(in.available()>0){
   int token=in.readUnsignedByte(), kind=token&15, type=token&240;
   if(kind==0) continue;
   if(kind==1){if(!tags.isEmpty())throw new IOException("Unclosed tags");xml.endDocument();return out.toString();}
   String value=read(in,pool,type);
   switch(kind){
    case 2: xml.startTag(null,value);tags.push(value);break;
    case 3: if(tags.isEmpty()||!tags.pop().equals(value))throw new IOException("Mismatched tag");xml.endTag(null,value);break;
    case 15: throw new IOException("Unexpected attribute encoding");
    case 4: xml.text(value);break;
    case 5: xml.cdsect(value);break;
    case 9: xml.comment(value);break;
    default: throw new IOException("Unsupported token "+kind);
   }
   while(kind==2 && in.available()>0){
    in.mark(1);int a=in.readUnsignedByte();if((a&15)!=15){in.reset();break;}
    String name=intern(in,pool);xml.attribute(null,name,read(in,pool,a&240));
   }
  }
  throw new IOException("Missing end document");
 }
 private static String intern(DataInputStream in,ArrayList<String> pool)throws IOException{
  int index=in.readUnsignedShort();if(index==65535){String s=in.readUTF();if(pool.size()<65535)pool.add(s);return s;}
  if(index>=pool.size())throw new IOException("Invalid string reference");return pool.get(index);
 }
 private static String read(DataInputStream in,ArrayList<String> pool,int t)throws IOException{
  switch(t){
   case 16:return "";
   case 32:return in.readUTF();
   case 48:return intern(in,pool);
   case 64:case 80:byte[] b=new byte[in.readUnsignedShort()];in.readFully(b);if(t==80)return android.util.Base64.encodeToString(b,2);StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return s.toString();
   case 96:return Integer.toString(in.readInt());
   case 112:return Integer.toHexString(in.readInt());
   case 128:return Long.toString(in.readLong());
   case 144:return Long.toHexString(in.readLong());
   case 160:return Float.toString(in.readFloat());
   case 176:return Double.toString(in.readDouble());
   case 192:return "true";
   case 208:return "false";
   default:throw new IOException("Unsupported value type "+t);
  }
 }
}

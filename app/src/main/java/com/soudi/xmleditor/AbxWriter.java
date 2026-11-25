package com.soudi.xmleditor;
import java.io.*;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;

public final class AbxWriter {
 private static void name(DataOutputStream out,String s)throws IOException{
  out.writeShort(65535);out.writeUTF(s);
 }
 public static byte[] encode(String document)throws Exception{
  XmlPullParser p=Xml.newPullParser();
  p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
  p.setInput(new StringReader(document));
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  DataOutputStream out=new DataOutputStream(bytes);
  out.writeInt(0x41425800);out.writeByte(0x10);
  int event;
  while((event=p.nextToken())!=XmlPullParser.END_DOCUMENT){
   switch(event){
    case XmlPullParser.START_TAG:
     out.writeByte(0x32);name(out,p.getName());
     for(int i=0;i<p.getAttributeCount();i++){
      out.writeByte(0x2f);name(out,p.getAttributeName(i));out.writeUTF(p.getAttributeValue(i));
     }break;
    case XmlPullParser.END_TAG:out.writeByte(0x33);name(out,p.getName());break;
    case XmlPullParser.TEXT:case XmlPullParser.IGNORABLE_WHITESPACE:case XmlPullParser.ENTITY_REF:
     out.writeByte(0x24);out.writeUTF(p.getText());break;
    case XmlPullParser.CDSECT:out.writeByte(0x25);out.writeUTF(p.getText());break;
    case XmlPullParser.COMMENT:out.writeByte(0x29);out.writeUTF(p.getText());break;
    default:throw new IOException("Unsupported XML event "+event);
   }
  }
  out.writeByte(0x11);out.flush();return bytes.toByteArray();
 }
}

package com.rokid.simplesip.gb28181;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: zhuohf
 * Version: V0.1 2018/2/19
 */
public class XMLUtil {

    public static final String XML_MANSCDP_TYPE ="Application/MANSCDP+xml";

    public static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";

    private static XStream xstream = new XStream(new PureJavaReflectionProvider(new FieldDictionary(new PartialSeqFieldKeySorter())));

    public static String convertBeanToXml(BaseMessage message) {
        xstream.autodetectAnnotations(true);
        String xml = xstream.toXML(message);
        return XML_HEAD + xml;
    }

    public static Object convertXmlToBean(String message) {
        return "";
    }


    public static String getSubUtilSimple(String soap, String rgex){
        Pattern pattern = Pattern.compile(rgex);
        Matcher m = pattern.matcher(soap);
        while(m.find()){
            return m.group(1);
        }
        return "";
    }

//
//    public static Map<String,String> parseXml(HttpServletRequest request){
//
//        Map<String,String> messageMap=new HashMap<String, String>();
//
//        InputStream inputStream=null;
//        try {
//            //读取request Stream信息
//            inputStream=request.getInputStream();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        SAXReader reader = new SAXReader();
//        Document document=null;
//        try {
//            document = reader.read(inputStream);
//        } catch (DocumentException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        Element root=document.getRootElement();
//        List<Element> elementsList=root.elements();
//
//        for(Element e:elementsList){
//            messageMap.put(e.getName(),e.getText());
//        }
//        try {
//            inputStream.close();
//            inputStream=null;
//        } catch (IOException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//
//        return messageMap;
//    }


}

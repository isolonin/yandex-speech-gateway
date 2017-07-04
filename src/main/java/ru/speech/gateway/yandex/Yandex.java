package ru.speech.gateway.yandex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.omg.CORBA_2_3.portable.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author ivan
 */
public class Yandex {
    private static final Logger LOG = LoggerFactory.getLogger(Yandex.class);
    
    public static List<String> speechToTextList(String key, String fileName){
        try {
            //curl -v -X POST -H "Content-Type: audio/x-wav" --data-binary "@/tmp/test.wav" 
            //"https://asr.yandex.net/asr_xml?uuid=d66f657f39fa436a8cd6c378746f6354&
            //key=e88f99d1-d019-467b-b80c-9fb05563c688&
            //topic=queries"
            
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpEntity httpEntity = EntityBuilder.create()
                    .setFile(new File(fileName))
                    .build();
            HttpPost post = new HttpPost("https://asr.yandex.net/asr_xml?uuid=d66f657f39fa436a8cd6c378746f6354&key="+key+"&topic=queries");
            post.setHeader("Content-Type", "audio/x-wav");
            post.setEntity(httpEntity);
            HttpResponse response = httpClient.execute(post);            
            String xmlContent = EntityUtils.toString(response.getEntity());
            LOG.info("yandex resul {}:{}",response.getStatusLine(), xmlContent);
            
            List<String> yandexResultToList = yandexResultToList(xmlContent);
            return yandexResultToList;
        } catch (IOException ex) {
            LOG.error("Exception ",ex);
        }
        return null;
    }
    
    public static List<String> yandexResultToList(String xml){
        List<String> result = new ArrayList<>();
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
            doc.getDocumentElement().normalize();
            NodeList recognitionResults = doc.getElementsByTagName("recognitionResults");
            if(recognitionResults != null && recognitionResults.getLength() > 0){
                Node recognitionResult = recognitionResults.item(0);
                NamedNodeMap attributes = recognitionResult.getAttributes();
                Node success = attributes.getNamedItem("success");
                if(success.getNodeValue().equals("1")){
                    NodeList childNodes = recognitionResult.getChildNodes();
                    if(childNodes != null && childNodes.getLength() > 0){
                        for(int i=0; i<childNodes.getLength(); i++){
                            Node chlidNode = childNodes.item(i);
                            if(chlidNode != null && chlidNode.getNodeType() == Node.ELEMENT_NODE){
                                Element child = (Element)chlidNode;
                                result.add(child.getTextContent());
                            }
                        }
                    }
                }else {
                    LOG.warn("Can't parse");
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOG.error("Exception {}",ex);
        }
        return result;
    }
}

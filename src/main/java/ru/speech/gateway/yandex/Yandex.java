package ru.speech.gateway.yandex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
    private static final String CACHEPATH = "cache/";
    private static final String FILEUSERATTR = "user:speech-text";
    
    public static String textToSpeech(String fileName, String key, String text, String format, String speaker, String emotion){
        try{
            //curl -v "https://tts.voicetech.yandex.net/generate?format=mp3&lang=ru-RU&speaker=oksana&
            //key=e88f99d1-d019-467b-b80c-9fb05563c688&emotion=good" -G --data-urlencode 
            //"text=Здравствуйте. Вас приветствует сеть 112. Продиктуйте номер машины" > speech.mp3
            
            File file = new File(CACHEPATH+fileName);            
            
            //Если файл есть в кэше, проверяем текст записи и дату модификации
            if(file.exists()){                
                Date lastModified = new Date(file.lastModified());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                Calendar currentTime = Calendar.getInstance();
                currentTime.setTime(new Date());
                currentTime.add(Calendar.DAY_OF_MONTH, -3);
                if(lastModified.after(currentTime.getTime())){
                    LOG.info("File \"{}\" exist in cache. Last modified {}", file.getAbsoluteFile(), sdf.format(lastModified));
                
                    //Проверяем текст записи в файле
                    try{
                        String attrFileText = new String((byte[])Files.getAttribute(file.toPath(), FILEUSERATTR));                        
                        if(attrFileText.equalsIgnoreCase(text)){
                            LOG.info("Return from cache");
                            return file.getAbsolutePath();
                        }else {
                            LOG.info("File attr {} diff between \"{}\" and \"{}\"", FILEUSERATTR, attrFileText, text);
                        }
                    }catch(Exception ex){
                        LOG.warn("Files.getAttribute exception: {}", ex.getMessage());
                    }
                }else {
                    LOG.info("File \"{}\" EXPIRED in cache. Last modified {}", file.getAbsoluteFile(), sdf.format(lastModified));
                }
                LOG.info("Rewrite file");
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("https://tts.voicetech.yandex.net/generate?format=");
            sb.append(format);
            sb.append("&lang=ru-RU&speaker=");
            sb.append(speaker);
            sb.append("&key=");
            sb.append(key);
            sb.append("&emotion=");
            sb.append(emotion);
            sb.append("&text=");
            sb.append(URLEncoder.encode(text, "UTF-8"));
            
            LOG.info("Send to Yandex:\n{}",sb.toString());
            HttpClient httpClient = HttpClientBuilder.create().build();            
            HttpGet get = new HttpGet(sb.toString());
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            Header xYaRequestId = response.getFirstHeader("X-YaRequestId");
            LOG.info("yandex resul {} ({})",response.getStatusLine(), xYaRequestId.getValue());
            
            if(entity != null) {
                InputStream is = entity.getContent();
                FileOutputStream fos = new FileOutputStream(file);
                int inByte;
                while((inByte = is.read()) != -1)
                     fos.write(inByte);
                is.close();
                fos.close();
            }
            Files.setAttribute(file.toPath(), FILEUSERATTR, text.getBytes("UTF-8"));
            return file.getAbsolutePath();
        }catch(Exception ex){
            LOG.error("Exception ",ex);
        }
        return null;
    }
    
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
            Header xYaRequestId = response.getFirstHeader("X-YaRequestId");            
            LOG.info("yandex resul {} ({}):\n{}",response.getStatusLine(), xYaRequestId.getValue(), xmlContent);
            
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
                    LOG.warn("Yandex return empty");
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOG.error("Exception {}",ex);
        }
        return result;
    }
}

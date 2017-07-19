package ru.speech.gateway.asterisk;

import org.apache.commons.codec.digest.Md5Crypt;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.speech.gateway.exceptions.KeyNotFound;
import ru.speech.gateway.yandex.Yandex;

/**
 *
 * @author ivan
 */
public class AGITextToSpeech extends BaseAgiScript{
    private static final Logger LOG = LoggerFactory.getLogger(AGITextToSpeech.class);
    
    public static String getValueByParameters(AgiRequest ar, String key, boolean isRequired, String defaultValue) throws KeyNotFound{
        String result = getValueByParameters(ar, key, isRequired);
        if(result != null){
            return result;
        }
        return defaultValue;
    }
    public static String getValueByParameters(AgiRequest ar, String key, boolean isRequired) throws KeyNotFound{
        String[] keyResult = ar.getParameterValues(key);
        if(isRequired && keyResult.length <= 0){
            throw new KeyNotFound("AGI "+key+" is not defined");
        }
        if(keyResult.length > 0){
            return keyResult[0];
        }
        return null;
    }
    
    @Override
    public void service(AgiRequest ar, AgiChannel ac) {
        try{
            Thread.currentThread().setName(ar.getCallerIdNumber());
            
            String key = getValueByParameters(ar, "key", true);
            String text = getValueByParameters(ar, "text", true);
            String format = getValueByParameters(ar, "format", false, "wav");
            String speaker = getValueByParameters(ar, "speaker", false, "oksana");
            String emotion = getValueByParameters(ar, "emotion", false, "good");
            String fileName = getValueByParameters(ar, "file", false);
            if(fileName == null){
                fileName = Md5Crypt.md5Crypt(text.getBytes())+"."+format;
            }
            String filePath = Yandex.textToSpeech(fileName, key, text, format, speaker, emotion);
            if(filePath != null){
                String ext = filePath.replaceFirst("\\..*$", "");
                int result = exec("Playback", filePath.replaceFirst("\\.(.*)$", ",$1"));
                LOG.info("exec return {}", result);
            }            
        }catch(KeyNotFound ex){
            LOG.error(ex.getMessage());
        }catch(Exception ex){
            LOG.error("Exception ", ex);
        }
    }
}

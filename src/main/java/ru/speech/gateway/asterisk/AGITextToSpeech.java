package ru.speech.gateway.asterisk;

import java.util.UUID;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
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
            String emotion = getValueByParameters(ar, "emotion", false, "neutral");            
            String fileName = getValueByParameters(ar, "file", false);
            if(fileName == null){
                fileName = UUID.randomUUID().toString();
            }
            Yandex.textToSpeech(fileName, key, text, format, speaker, emotion);
        }catch(KeyNotFound ex){
            LOG.error(ex.getMessage());
        }catch(Exception ex){
            LOG.error("Exception ", ex);
        }
    }
    
    @Deprecated
    private Format detectFormat(){
        try {            
            String audionativeformat = getFullVariable("${CHANNEL(audionativeformat)}");
            LOG.debug("audionativeformat: {}", audionativeformat);
            if(audionativeformat.matches("(silk|sln)12")){
                return new Format("sln12", 12000);
            }
            if(audionativeformat.matches("(speex|slin|silk)16|g722|siren7")){
                return new Format("sln16", 16000);
            }
            if(audionativeformat.matches("(speex|slin|celt)32|siren14")){
                return new Format("sln32", 32000);
            }
            if(audionativeformat.matches("(celt|slin)44")){
                return new Format("sln44", 44100);
            }
            if(audionativeformat.matches("(celt|slin)48")){
                return new Format("sln48", 48000);
            }
            return new Format("sln", 8000);
        } catch (AgiException ex) {
            LOG.error("Exception {}",ex);
        }
        return null;
    }
    
    private class Format{
        String name;
        Integer freq;

        public Format(String name, Integer freq) {
            this.name = name;
            this.freq = freq;
        }

        @Override
        public String toString() {
            return name+"/"+freq;
        }   
    }
}

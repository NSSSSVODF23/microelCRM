package com.microel.trackerbackend.misc.autosupport.schema.preprocessors;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PreprocessorMetadataProvider {

    private final Map<PreprocessorType, IPreprocessor> preprocessorMap = new HashMap<>();

    public PreprocessorMetadataProvider(){
        List<IPreprocessor> preprocessors = List.of(
                new UserInfoPreprocessor()
        );
        registerPreprocessors(preprocessors.toArray(IPreprocessor[]::new));
    }

    private void registerPreprocessors(IPreprocessor... preprocessors){
        for (IPreprocessor preprocessor : preprocessors) {
            preprocessorMap.put(preprocessor.type(), preprocessor);
        }
    }

    public Map<PreprocessorType, List<String>> getAllOutputs() {
        Map<PreprocessorType, List<String>> outputMap = new HashMap<>();
        for (PreprocessorType preprocessorType : PreprocessorType.values()) {
            IPreprocessor preprocessor = preprocessorMap.get(preprocessorType);
            if (preprocessor == null) throw new RuntimeException(preprocessorType.getValue() + " не зарегистрирован в PreprocessorMetadataProvider");
            outputMap.put(preprocessorType, preprocessor.getOutputValues());
        }
        return outputMap;
    }
}

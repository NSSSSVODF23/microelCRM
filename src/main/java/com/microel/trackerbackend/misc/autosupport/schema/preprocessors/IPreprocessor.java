package com.microel.trackerbackend.misc.autosupport.schema.preprocessors;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;

import java.util.List;
import java.util.Map;

public interface IPreprocessor {
    PreprocessorType type();

    List<String> getOutputValues();

    Map<String, String> process(AutoSupportContext context, Long userId);
}

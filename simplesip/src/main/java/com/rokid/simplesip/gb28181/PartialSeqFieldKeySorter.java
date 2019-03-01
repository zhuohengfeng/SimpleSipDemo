package com.rokid.simplesip.gb28181;

import com.thoughtworks.xstream.converters.reflection.FieldKey;
import com.thoughtworks.xstream.converters.reflection.FieldKeySorter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Author: zhuohf
 * Version: V0.1 2018/2/19
 */
public class PartialSeqFieldKeySorter implements FieldKeySorter {
    @Override
    public Map sort(Class type, Map keyedByFieldKey) {
        Annotation sequence = type.getAnnotation(XMLSequence.class);
        if (sequence != null) {
            final String[] fieldsOrder = ((XMLSequence) sequence).value();
            Map<FieldKey, Field> custom = new LinkedHashMap<>();
            Map<FieldKey, Field> notCustom = new LinkedHashMap<>();
            Set<Map.Entry<FieldKey, Field>> fields = keyedByFieldKey.entrySet();
            for (String fieldName : fieldsOrder) {
                if (fieldName != null) {
                    for (Map.Entry<FieldKey, Field> fieldEntry : fields) {
                        if (fieldName.equals(fieldEntry.getKey().getFieldName())) {
                            custom.put(fieldEntry.getKey(), fieldEntry.getValue());
                        } else {
                            notCustom.put(fieldEntry.getKey(), fieldEntry.getValue());
                        }
                    }
                }
            }
            custom.putAll(notCustom);
            return custom;
        } else {
            return keyedByFieldKey;
        }
    }
}

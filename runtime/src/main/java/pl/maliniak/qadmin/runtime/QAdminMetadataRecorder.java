package pl.maliniak.qadmin.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Map;

@Recorder
public class QAdminMetadataRecorder {

    public RuntimeValue<QAdminMetadataRegistry> createRegistry(Map<String, Map<String, String>> metadata) {
        return new RuntimeValue<>(new QAdminMetadataRegistry(metadata));
    }
}

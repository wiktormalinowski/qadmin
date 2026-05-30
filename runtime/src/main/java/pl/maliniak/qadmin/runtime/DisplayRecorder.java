package pl.maliniak.qadmin.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Map;

@Recorder
public class DisplayRecorder {

    public RuntimeValue<DisplayRegistry> createRegistry(Map<String, String> displays) {
        return new RuntimeValue<>(new DisplayRegistry(displays));
    }
}

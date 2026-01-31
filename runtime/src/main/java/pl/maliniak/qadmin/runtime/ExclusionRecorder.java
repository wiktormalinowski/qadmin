package pl.maliniak.qadmin.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Set;

@Recorder
public class ExclusionRecorder {

    public RuntimeValue<ExclusionRegistry> createRegistry(Set<String> exclusions) {
        return new RuntimeValue<>(new ExclusionRegistry(exclusions));
    }
}

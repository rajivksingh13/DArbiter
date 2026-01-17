package ai.titli.darbiter.service;

import ai.titli.darbiter.model.ScanResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScanStore {
    private final Map<String, ScanResult> store = new ConcurrentHashMap<>();

    public void save(ScanResult result) {
        store.put(result.getScanId(), result);
    }

    public Optional<ScanResult> find(String scanId) {
        return Optional.ofNullable(store.get(scanId));
    }
}

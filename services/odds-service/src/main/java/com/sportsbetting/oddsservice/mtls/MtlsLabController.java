package com.sportsbetting.oddsservice.mtls;

import com.example.mtls.core.MtlsMaterialService;
import com.example.mtls.core.MtlsReloadResult;
import com.example.mtls.core.ReloadableSslContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/mtls")
@Profile("mtls-lab")
@ConditionalOnProperty(prefix = "mtls", name = "enabled", havingValue = "true")
public class MtlsLabController {

    private final MtlsMaterialService materialService;
    private final ReloadableSslContext reloadableSslContext;

    public MtlsLabController(MtlsMaterialService materialService, ReloadableSslContext reloadableSslContext) {
        this.materialService = materialService;
        this.reloadableSslContext = reloadableSslContext;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "odds-service");
        body.put("sslReady", reloadableSslContext.isReady());
        reloadableSslContext.getFingerprint().ifPresent(fp -> body.put("fingerprint", fp));
        materialService.getCurrent().ifPresent(m -> {
            body.put("version", m.version());
            body.put("daysUntilExpiry", m.secondsUntilExpiry() / 86400);
            body.put("needsRotation", materialService.needsRotation());
        });
        return body;
    }

    @PostMapping("/reload")
    public Map<String, Object> reload() {
        MtlsReloadResult result = materialService.reload();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", result.success());
        body.put("message", result.message());
        body.put("fingerprint", result.fingerprint());
        body.put("sslReady", reloadableSslContext.isReady());
        return body;
    }
}

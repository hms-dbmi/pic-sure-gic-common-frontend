package edu.harvard.hms.avillach.passthru.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FrontendResourceController {
    private final FrontendResourceService service;

    @Autowired
    public FrontendResourceController(FrontendResourceService service) {
        this.service = service;
    }

    @GetMapping("/sites")
    public ResponseEntity<List<CommonAreaResource>> getSites() {
        return ResponseEntity.ok(service.getSites());
    }
}

package edu.harvard.hms.avillach.passthru.resource;

import edu.harvard.hms.avillach.passthru.remote.RemoteResource;
import edu.harvard.hms.avillach.passthru.status.ResourceStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrontendResourceService {
    private final ResourceStatusService resourceStatusService;
    private final List<RemoteResource> remoteResources;

    @Autowired
    public FrontendResourceService(ResourceStatusService resourceStatusService, List<RemoteResource> remoteResources) {
        this.resourceStatusService = resourceStatusService;
        this.remoteResources = remoteResources;
    }

    public List<CommonAreaResource> getSites() {
        return remoteResources.stream().map(resource ->
            new CommonAreaResource(
                resource.name(), resource.common().toString(),
                !resourceStatusService.isSiteDown(resource.base())
            )
        ).toList();
    }
}

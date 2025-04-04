package edu.harvard.hms.avillach.passthru.status;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class ResourceStatusServiceTest {

    @MockitoBean
    ResourceStatusRepository repository;

    @Autowired
    ResourceStatusService subject;

    @Test
    void shouldMarkUnstableAsOffline() {
        URI site = URI.create("foo.invalid");
        ResourceStatus unstable = new ResourceStatus(site, Status.Unstable, 1);
        Mockito.when(repository.getStatus(site)).thenReturn(unstable);

        subject.markAsOffline(site);
        Mockito.verify(repository, Mockito.times(1)).setStatus(site, Status.Offline);
        Runnable verifyUnStable = () -> Mockito.verify(repository, Mockito.times(1)).setStatusAndBackoff(site, Status.Unstable, 2);
        Awaitility.await()
            .pollDelay(2100, TimeUnit.MILLISECONDS)
            .atMost(2500, TimeUnit.MILLISECONDS)
            .until(() -> noExceptionThrown(verifyUnStable));
    }

    @Test
    void shouldMarkOnlineAsOffline() {
        URI site = URI.create("foo.invalid");
        ResourceStatus unstable = new ResourceStatus(site, Status.Online, 1);
        Mockito.when(repository.getStatus(site)).thenReturn(unstable);

        subject.markAsOffline(site);
        Mockito.verify(repository, Mockito.times(1)).setStatus(site, Status.Offline);
        Runnable verifyUnStable = () -> Mockito.verify(repository, Mockito.times(1)).setStatusAndBackoff(site, Status.Unstable, 1);
        Awaitility.await()
            .pollDelay(1100, TimeUnit.MILLISECONDS)
            .atMost(1500, TimeUnit.MILLISECONDS)
            .until(() -> noExceptionThrown(verifyUnStable));
    }

    @Test
    void shouldMarkUnstableAsOnline() {
        URI site = URI.create("foo.invalid");
        ResourceStatus unstable = new ResourceStatus(site, Status.Unstable, 1);
        Mockito.when(repository.getStatus(site)).thenReturn(unstable);

        subject.markAsOnline(site);
        Mockito.verify(repository, Mockito.times(1)).setStatusAndBackoff(site, Status.Online, 1);
    }

    @Test
    void shouldNoOp() {
        URI site = URI.create("foo.invalid");
        ResourceStatus unstable = new ResourceStatus(site, Status.Online, 1);
        Mockito.when(repository.getStatus(site)).thenReturn(unstable);

        subject.markAsOnline(site);
        Mockito.verify(repository, Mockito.times(0)).setStatusAndBackoff(site, Status.Online, 1);
    }

    private boolean noExceptionThrown(Runnable lambda) {
        try {
            lambda.run();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
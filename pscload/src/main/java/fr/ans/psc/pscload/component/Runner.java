/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.component;

import java.io.File;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import fr.ans.psc.pscload.service.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.EmailTemplate;
import fr.ans.psc.pscload.model.LoadProcess;
import fr.ans.psc.pscload.model.Stage;
import fr.ans.psc.pscload.service.EmailService;
import fr.ans.psc.pscload.state.ChangesApplied;
import fr.ans.psc.pscload.state.DiffComputed;
import fr.ans.psc.pscload.state.ProcessState;
import fr.ans.psc.pscload.state.ReadyToComputeDiff;
import fr.ans.psc.pscload.state.ReadyToExtract;
import fr.ans.psc.pscload.state.SerializationInterrupted;
import fr.ans.psc.pscload.state.Submitted;
import fr.ans.psc.pscload.state.UploadInterrupted;
import fr.ans.psc.pscload.state.UploadingChanges;
import fr.ans.psc.pscload.state.exception.ExtractTriggeringException;
import fr.ans.psc.pscload.state.exception.LoadProcessException;
import fr.ans.psc.pscload.state.exception.SerFileGenerationException;
import fr.ans.psc.pscload.state.exception.UploadException;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class Scheduler.
 */
@Slf4j
@Component
public class Runner {

    @Autowired
    private ProcessRegistry processRegistry;

    @Autowired
    private CustomMetrics customMetrics;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MessageProducer messageProducer;

    @Value("${enable.scheduler:true}")
    private boolean enabled;

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    @Value("${cert.path}")
    private String certfile;

    @Value("${key.path}")
    private String keyfile;

    @Value("${ca.path}")
    private String cafile;

    @Value("${keystore.password:mysecret}")
    private String kspwd;

    @Value("${files.directory}")
    private String filesDirectory;

    @Value("${use.x509.auth:true}")
    private boolean useX509Auth;

    @Value("${api.base.url}")
    private String apiBaseUrl;

    @Value("${deactivation.excluded.profession.codes:}")
    private String[] excludedProfessions;

    @Value("${pscextract.base.url}")
    private String pscextractBaseUrl;

    @Value("${process.expiration.delay}")
    private Long expirationDelay;


    /**
     * Run.
     *
     * @throws DuplicateKeyException the duplicate key exception
     */
    @Scheduled(cron = "${schedule.cron.expression}", zone = "${schedule.cron.timeZone}")
    public void runScheduler() throws DuplicateKeyException {
        if (enabled) {
            runProcess();
        }
    }

    /**
     * Run process.
     *
     * @throws DuplicateKeyException the duplicate key exception
     */
    public void runProcess() throws DuplicateKeyException {
        if (isProcessExpired()) {
            // clear registry if latest is expired
            processRegistry.clear();

            // register new process with Idle state
            String id = Integer.toString(processRegistry.nextId());
            ProcessState idle;
            if (useX509Auth) {
                idle = new Submitted(keyfile, certfile, cafile, kspwd, extractDownloadUrl, filesDirectory);
            } else {
                idle = new Submitted(extractDownloadUrl, filesDirectory);
            }
            LoadProcess process = new LoadProcess(idle);
            processRegistry.register(id, process);
            try {
                // Step 1 : Download
                process.nextStep();
                process.setState(new ReadyToExtract());
                customMetrics.setStageMetric(Stage.READY_TO_EXTRACT);
                // Step 2 : Extract
                process.nextStep();
                process.setState(new ReadyToComputeDiff(customMetrics));
                customMetrics.setStageMetric(Stage.READY_TO_COMPUTE);
                // Step 4 : Load maps and compute diff
                process.nextStep();
                // check if differences exists
                if (process.isRemainingPsOrStructuresInMaps()) {
                    process.setState(new DiffComputed(customMetrics));
                    // Step 3 : publish metrics
                    process.nextStep();
                    customMetrics.setStageMetric(Stage.DIFF_COMPUTED);
                    // End of scheduled steps
                } else {
                    File txtfile = new File(process.getExtractedFilename());
                    txtfile.delete();
                    File lockfile = new File(process.getTmpMapsPath());
                    lockfile.delete();
                    log.info("No differences with previous upload, unregistering process...");
                    processRegistry.unregister(id);
                }
            } catch (LoadProcessException e) {
                log.error("Error when loading RASS data", e);
                customMetrics.setStageMetric(customMetrics.getStageMetricValue());
                emailService.sendMail(EmailTemplate.INTERRUPTED_PROCESS);
                processRegistry.unregister(id);
            }
        } else {
            log.warn("A process is already running !");
        }
    }

    /**
     * Run full process.
     *
     * @throws DuplicateKeyException the duplicate key exception
     */
    @Async("processExecutor")
    public void runFullProcess() throws DuplicateKeyException {
        runProcess();
    }

    /**
     * Run continue.
     */
    @Async("processExecutor")
    public void runContinue(LoadProcess process, List<String> excludedOperations) {
        try {
            // upload changes
            log.info("Received request to process in Runner.runContinue()");
            process.setState(new UploadingChanges(excludedProfessions, apiBaseUrl, messageProducer, excludedOperations));
            customMetrics.resetSizeMetrics();
            customMetrics.setStageMetric(Stage.UPLOAD_CHANGES_STARTED);
            process.nextStep();
            process.setState(new ChangesApplied(customMetrics, pscextractBaseUrl, emailService));
            // Step 5 : call pscload
            process.nextStep();
            processRegistry.unregister(process.getId());
            customMetrics.setStageMetric(Stage.FINISHED);
        } catch (LoadProcessException e) {
            // error during uploading
            if (e.getClass().equals(UploadException.class)) {
                log.error("error when uploading changes", e);
                process.setState(new UploadInterrupted());
                customMetrics.setStageMetric(Stage.UPLOAD_CHANGES_FINISHED);
                emailService.sendMail(EmailTemplate.UPLOAD_REST_INTERRUPTION);
            } else {
                // error during ChangesAppliedState
                handleChangesAppliedStateExceptions(process, e);
            }
        }
    }

    /**
     * run ending operations
     */
    @Async("processExecutor")
    public void runEnding(LoadProcess process) {
        try {
            process.nextStep();
            processRegistry.unregister(process.getId());
            customMetrics.setStageMetric(Stage.FINISHED);
        } catch (LoadProcessException e) {
            // error during serialization/deserialization
            handleChangesAppliedStateExceptions(process, e);
        }
    }

    private void handleChangesAppliedStateExceptions(LoadProcess process, LoadProcessException e) {
        // error during serialization/deserialization
        if (e.getClass().equals(SerFileGenerationException.class)) {
            log.warn("Error when (de)serializing");
            process.setState(new SerializationInterrupted());
            customMetrics.setStageMetric(Stage.UPLOAD_CHANGES_STARTED);
            emailService.sendMail(EmailTemplate.SERIALIZATION_FAILURE);
            // error when triggering extract
        } else if (e.getClass().equals(ExtractTriggeringException.class)) {
            log.warn("Error when triggering pscextract", e);
            customMetrics.setStageMetric(Stage.UPLOAD_CHANGES_FINISHED);
            emailService.sendMail(EmailTemplate.TRIGGER_EXTRACT_FAILED);
            processRegistry.unregister(process.getId());
        }
    }


    private boolean isProcessExpired() {
        if (processRegistry.isEmpty()) {
            return true;
        }

        // states that have successfully passed external control (through prometheus alerts
        // and which are still running, for a reason or an other, should NOT be unregistered
        // They must be handled manually
        if (!processRegistry.getCurrentProcess().getState().isExpirable()) {
            return false;
        }
        Date lastProcessDate = new Date(processRegistry.getCurrentProcess().getTimestamp());
        Date now = new Date();
        return now.after(
                Date.from(lastProcessDate.toInstant().plus(Duration.ofHours(expirationDelay))));
    }
}

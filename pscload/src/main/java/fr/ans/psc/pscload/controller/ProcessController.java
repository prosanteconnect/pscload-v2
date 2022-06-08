/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ans.psc.pscload.service.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.ans.psc.pscload.component.ProcessRegistry;
import fr.ans.psc.pscload.component.Runner;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.LoadProcess;
import fr.ans.psc.pscload.model.ProcessInfo;
import fr.ans.psc.pscload.service.EmailService;
import fr.ans.psc.pscload.state.ChangesApplied;
import fr.ans.psc.pscload.state.DiffComputed;
import fr.ans.psc.pscload.state.SerializationInterrupted;
import fr.ans.psc.pscload.state.UploadInterrupted;
import fr.ans.psc.pscload.state.UploadingChanges;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class ProcessController.
 */
@Slf4j
@RestController
public class ProcessController {

    @Value("${files.directory}")
    private String filesDirectory;

    @Value("${api.base.url}")
    private String apiBaseUrl;

    @Value("${deactivation.excluded.profession.codes:}")
    private String[] excludedProfessions;

    @Value("${pscextract.base.url}")
    private String pscextractBaseUrl;

    @Autowired
    private Runner runner;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private CustomMetrics customMetrics;

    @Autowired
    private EmailService emailService;

    private final ProcessRegistry registry;

    /**
     * Instantiates a new process controller.
     *
     * @param registry the registry
     */
    public ProcessController(ProcessRegistry registry) {
        super();
        this.registry = registry;
    }

    /**
     * Continue process.
     *
     * @return the  result
     */
    @PostMapping(value = "/process/continue")
    public ResponseEntity<Void> continueProcess(@RequestParam(value = "exclude", required = false) List<String> excludedOperations) {
        LoadProcess process = registry.getCurrentProcess();
        ResponseEntity<Void> result;
        if (process != null) {
            if (process.getState().getClass().equals(DiffComputed.class)) {
                if (excludedOperations != null) {
                    excludedOperations.replaceAll(String::toUpperCase);
                }
                // launch process in a separate thread because this method is annoted Async
                runner.runContinue(process, excludedOperations);
                result = new ResponseEntity<>(HttpStatus.ACCEPTED);
                // Response OK
                return result;
            }
            // Conflict if process is not in the expected state.
            log.warn("can't continue: process is not at DiffComputed state");
            result = new ResponseEntity<>(HttpStatus.CONFLICT);
        } else {
            log.warn("can't continue: no process is registered");
            result = new ResponseEntity<>(HttpStatus.TOO_EARLY);
        }
        return result;
    }

    /**
     * Resume process.
     *
     * @return the deferred result
     */
    @PostMapping(value = "/process/resume")
    public ResponseEntity<Void> resumeProcess(@RequestParam(value = "exclude", required = false) List<String> excludedOperations) {
        // We can call continue process because it contains the updated maps to apply.
        LoadProcess process = registry.getCurrentProcess();
        ResponseEntity<Void> response;

        if (process != null) {
            if (process.getState().getClass().equals(UploadInterrupted.class)) {
                process.setState(new UploadingChanges(excludedProfessions, apiBaseUrl, messageProducer, excludedOperations));
                runner.runContinue(process, excludedOperations);
                response = new ResponseEntity<>(HttpStatus.ACCEPTED);
                return response;
            }
            // Conflict if process is not in the expected state.
            response = new ResponseEntity<>(HttpStatus.CONFLICT);

        } else {
            response = new ResponseEntity<>(HttpStatus.TOO_EARLY);
        }
        return response;
    }

    /**
     * Abort process.
     *
     * @return the response entity
     */
    @PostMapping(value = "/process/abort")
    public ResponseEntity<Void> abortProcess() {
        LoadProcess process = registry.getCurrentProcess();
        if (process != null) {
            if (process.getTmpMapsPath() != null) {
                File lockFile = new File(process.getTmpMapsPath());
                lockFile.delete();
                log.info("lock file deleted");
            }
        }
        registry.clear();
        log.info("registry cleared");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * info on process.
     *
     * @return the result
     */
    @GetMapping(value = "/process/info")
    public ResponseEntity<List<ProcessInfo>> processInfo(@RequestParam(value = "details",
            required = false, defaultValue = "false") boolean withDetails) {
        List<LoadProcess> processes = registry.list();
        log.info("param details = {}", withDetails);
        List<ProcessInfo> processesInfos = new ArrayList<>();
        processes.forEach(process -> processesInfos.add(process.getProcessInfos(withDetails)));
        return new ResponseEntity<>(processesInfos, HttpStatus.OK);
    }

    /**
     * Resume ending operations (ser generation, etc)
     *
     * @return the response entity
     */
    @PostMapping(value = "/process/resume/ending-operations")
    public ResponseEntity<Void> resumeEndingOperations() {
        LoadProcess process = registry.getCurrentProcess();
        ResponseEntity<Void> response;

        if (process != null) {
            if (process.getState().getClass().equals(SerializationInterrupted.class)) {
                process.setState(new ChangesApplied(customMetrics, pscextractBaseUrl, emailService));
                // launch process in a separate thread
                runner.runEnding(process);
                response = new ResponseEntity<Void>(HttpStatus.ACCEPTED);

                return response;
            }
            // Conflict if process is not in the expected state.
            response = new ResponseEntity<Void>(HttpStatus.CONFLICT);
        } else {
            response = new ResponseEntity<Void>(HttpStatus.TOO_EARLY);
        }
        return response;
    }
}

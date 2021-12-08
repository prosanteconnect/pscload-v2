package fr.ans.psc.pscload.component;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.service.LoadProcess;
import fr.ans.psc.pscload.state.FileDownloaded;
import fr.ans.psc.pscload.state.FileExtracted;
import fr.ans.psc.pscload.state.Idle;
import fr.ans.psc.pscload.state.exception.LoadProcessException;

/**
 * The type Scheduler.
 */
@Component
public class Scheduler {

	@Autowired
	private ProcessRegistry processRegistry;

	@Autowired
	private CustomMetrics customMetrics;

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

	/**
	 * Run the process
	 */
	@Scheduled(fixedDelayString = "${schedule.rate.ms}")
	public void run() throws GeneralSecurityException, IOException {
		if (enabled) {
			if (processRegistry.isEmpty()) {
				LoadProcess process = new LoadProcess(new Idle(true, keyfile, certfile, cafile));
				String id = Integer.toString(processRegistry.nextId());
				processRegistry.register(id, process);
				try {
					// Step 1 : Download
					process.runtask();
					process.setState(new FileDownloaded());
					customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(10);
					// Step 2 : Extract
					process.runtask();
					process.setState(new FileExtracted());
					customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(20);
				} catch (LoadProcessException e) {
					// TODO log
					processRegistry.unregister(id);
				}
			}else {
				//TODO  ajouter un log et ne rien faire car un process est déjà en cours.
			}
		}
	}
}
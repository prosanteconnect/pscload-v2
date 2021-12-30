/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import fr.ans.psc.pscload.component.ProcessRegistry;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.service.LoadProcess;
import fr.ans.psc.pscload.state.ChangesApplied;
import fr.ans.psc.pscload.state.ProcessState;
import fr.ans.psc.pscload.state.UploadingChanges;
import fr.ans.psc.pscload.state.exception.LoadProcessException;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class PscloadApplication.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class PscloadApplication {

	/** The registry. */
	@Autowired
	private ProcessRegistry registry;

	/** The custom metrics. */
	@Autowired
	private CustomMetrics customMetrics;
	
	@Autowired
	private Kryo kryo;

	@Value("${files.directory:.}")
	private String filesDirectory;
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(PscloadApplication.class, args);
		applicationContext.start();
	}

	/**
	 * The listener interface for receiving contextStarted events.
	 * The class that is interested in processing a contextStarted
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addContextStartedListener<code> method. When
	 * the contextStarted event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see ContextStartedEvent
	 */
	@Component
	class ContextStartedListener implements ApplicationListener<ContextRefreshedEvent> {


		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			// Load registry if exists
			log.info("Search registry to restore");
			File registryFile = new File(filesDirectory + File.separator + "registry.ser");
			if (registryFile.exists()) {
				try {
					FileInputStream fileInputStream = new FileInputStream(registryFile);
					Input input = new Input(fileInputStream);
					registry.read(kryo, input);
					input.close();
					registryFile.delete();
				} catch (IOException | KryoException e) {
					log.warn("Unable to restore registry, start with an empty registry", e);
					registryFile.delete();
					registry.clear();
				}

				// RESUME PROCESS
				LoadProcess process = registry.getCurrentProcess();
				if (process != null) {
					Class<? extends ProcessState> stateClass = process.getState().getClass();
					if (stateClass.equals(UploadingChanges.class)) {
							ForkJoinPool.commonPool().submit(() -> {
								try {
									// upload changes
									customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(60);
									process.nextStep();
									process.setState(new ChangesApplied());
									customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(70);
									// Step 5 : call pscload
									process.nextStep();
									registry.unregister(process.getId());
									customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(0);
								} catch (LoadProcessException e) {
									log.error("error when uploading changes", e);
								}
							});
					} else {
						log.info("Stage is not resumable, process is aborted");
						registry.clear();
					} 
				}
			}
		}
	}
	
	/**
	 * The listener interface for receiving contextClosed events.
	 * The class that is interested in processing a contextClosed
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addContextClosedListener<code> method. When
	 * the contextClosed event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see ContextClosedEvent
	 */
	@Component
	class ContextClosedListener implements ApplicationListener<ContextClosedEvent>{

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			// Wait for upload finished
			ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
			// Save the registry if not empty
			if (!registry.isEmpty()) {
				log.info("Try to save registry");
				try {
					File registryFile = new File(filesDirectory + File.separator + "registry.ser");
					FileOutputStream fileOutputStream = new FileOutputStream(registryFile);
					Output output = new Output(fileOutputStream);
					registry.write(kryo, output);
					output.close();
				} catch (IOException e) {
					log.error("Unable to save registry", e);
				} 
			}
		}
	}
}

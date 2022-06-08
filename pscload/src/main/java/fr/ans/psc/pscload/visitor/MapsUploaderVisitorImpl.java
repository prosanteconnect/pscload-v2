/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.visitor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.ans.psc.pscload.model.operations.OperationType;
import fr.ans.psc.pscload.service.MessageProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import fr.ans.psc.ApiClient;
import fr.ans.psc.api.PsApi;
import fr.ans.psc.model.Profession;
import fr.ans.psc.pscload.model.entities.Professionnel;
import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.model.operations.PsCreateMap;
import fr.ans.psc.pscload.model.operations.PsDeleteMap;
import fr.ans.psc.pscload.model.operations.PsUpdateMap;
import fr.ans.psc.pscload.state.exception.LockedMapException;
import fr.ans.psc.pscload.state.exception.UploadException;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class MapsUploaderVisitorImpl.
 */
@Slf4j
public class MapsUploaderVisitorImpl implements MapsVisitor {

	private String[] excludedProfessions;

	private PsApi psApi;

	private MessageProducer messageProducer;

	@Value("${snitch}")
	private boolean debug;

	/**
	 * Instantiates a new maps uploader visitor impl.
	 *
	 * @param excludedProfessions the excluded professions
	 * @param apiBaseUrl          the api base url
	 */
	public MapsUploaderVisitorImpl(String[] excludedProfessions, String apiBaseUrl, MessageProducer messageProducer) {
		super();
		this.excludedProfessions = excludedProfessions;
		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath(apiBaseUrl);
		this.psApi = new PsApi(apiClient);
		this.messageProducer = messageProducer;

	}

	@Override
	public void visit(PsCreateMap map) {
		Collection<RassEntity> items = map.values();
		items.stream().forEach(item -> {
			try {
				if (map.isLocked()) {
					log.info("Map is locked during shutdown");
					long id = 0L;
					try {
						id = Thread.currentThread().getId();
						Thread.currentThread().stop();
					} catch (ThreadDeath ignore) {
						log.info("Thread {} is stopped", id);
					}
					throw new LockedMapException();
				}
				psApi.createNewPs((Professionnel) item);
				map.remove(item.getInternalId());
				messageProducer.sendPsMessage((Professionnel) item, OperationType.CREATE);
			} catch (RestClientResponseException e) {
				log.error("error when {} : {}, return code : {}", map.getOperation().toString(), item.getInternalId(),
						e.getLocalizedMessage());
				item.setReturnStatus(e.getRawStatusCode());
			} catch (RestClientException e) {
				log.error("error when {} : {}, return message : {}", map.getOperation().toString(),
						item.getInternalId(), e.getLocalizedMessage());
				throw new UploadException(e);
			}
		});

	}

	@Override
	public void visit(PsDeleteMap map) {
		Collection<RassEntity> items = map.values();
		items.stream().forEach(item -> {
			try {
				if (map.isLocked()) {
					log.info("Map is locked during shutdown");
					long id = 0L;
					try {
						id = Thread.currentThread().getId();
						Thread.currentThread().stop();
					} catch (ThreadDeath ignore) {
						log.info("Thread {} is stopped", id);
					}
					throw new LockedMapException();
				}
				Professionnel prof = (Professionnel) item;
				List<Profession> psExPros = prof.getProfessions();
				AtomicBoolean deletable = new AtomicBoolean(true);
				psExPros.forEach(exerciceProfessionnel -> {
					if (excludedProfessions != null && Arrays.stream(excludedProfessions)
							.anyMatch(profession -> exerciceProfessionnel.getCode().equals(profession))) {
						deletable.set(false);
					}
				});
				if (deletable.get()) {
					psApi.deletePsById(URLEncoder.encode(item.getInternalId(), StandardCharsets.UTF_8));
				}
				// remove anyway : extract Ps from maps either successful or ignored
				map.remove(item.getInternalId());
				messageProducer.sendPsMessage((Professionnel) item, OperationType.DELETE);
			} catch (RestClientResponseException e) {
				log.error("error when {} : {}, return code : {}", map.getOperation().toString(), item.getInternalId(),
						e.getLocalizedMessage());
				item.setReturnStatus(e.getRawStatusCode());
			} catch (RestClientException e) {
				log.error("error when {} : {}, return message : {}", map.getOperation().toString(),
						item.getInternalId(), e.getLocalizedMessage());
				throw new UploadException(e);
			}
		});

	}

	@Override
	public void visit(PsUpdateMap map) {
		Collection<RassEntity> items = map.values();
		items.stream().forEach(item -> {
			try {
				if (map.isLocked()) {
					log.info("Map is locked during shutdown");
					long id = 0L;
					try {
						id = Thread.currentThread().getId();
						Thread.currentThread().stop();
					} catch (ThreadDeath ignore) {
						log.info("Thread {} is stopped", id);
					}
					throw new LockedMapException();
				}
				psApi.updatePs((Professionnel) item);
				map.remove(item.getInternalId());
				map.getOldValues().remove(item.getInternalId());
				messageProducer.sendPsMessage((Professionnel) item, OperationType.UPDATE);
			} catch (RestClientResponseException e) {
				log.error("error when {} : {}, return code : {}", map.getOperation().toString(), item.getInternalId(),
						e.getLocalizedMessage());
				item.setReturnStatus(e.getRawStatusCode());
			} catch (RestClientException e) {
				log.error("error when {} : {}, return message : {}", map.getOperation().toString(),
						item.getInternalId(), e.getLocalizedMessage());
				throw new UploadException(e);
			}
		});

	}

}

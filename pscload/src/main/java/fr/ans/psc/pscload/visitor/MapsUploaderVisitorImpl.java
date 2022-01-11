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

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import fr.ans.psc.ApiClient;
import fr.ans.psc.api.PsApi;
import fr.ans.psc.api.StructureApi;
import fr.ans.psc.model.Profession;
import fr.ans.psc.pscload.model.entities.Professionnel;
import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.model.entities.Structure;
import fr.ans.psc.pscload.model.operations.PsCreateMap;
import fr.ans.psc.pscload.model.operations.PsDeleteMap;
import fr.ans.psc.pscload.model.operations.PsUpdateMap;
import fr.ans.psc.pscload.model.operations.StructureCreateMap;
import fr.ans.psc.pscload.model.operations.StructureUpdateMap;
import fr.ans.psc.pscload.state.exception.UploadException;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class MapsUploaderVisitorImpl.
 */
@Slf4j
public class MapsUploaderVisitorImpl implements MapsVisitor {

	private String[] excludedProfessions;

	private StructureApi structureApi;

	private PsApi psApi;

	/**
	 * Instantiates a new maps uploader visitor impl.
	 *
	 * @param excludedProfessions the excluded professions
	 * @param apiBaseUrl the api base url
	 */
	public MapsUploaderVisitorImpl(String[] excludedProfessions, String apiBaseUrl) {
		super();
		this.excludedProfessions = excludedProfessions;
		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath(apiBaseUrl);
		this.structureApi = new StructureApi(apiClient);
		this.psApi = new PsApi(apiClient);
	}

	@Override
	public void visit(PsCreateMap map) {
		Collection<RassEntity> items = map.values();
		items.parallelStream().forEach(item -> {
			try {
				psApi.createNewPs((Professionnel) item);
				map.remove(item.getInternalId());
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
		items.parallelStream().forEach(item -> {
			try {
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
					// remove PS from map if status 2xx
					map.remove(item.getInternalId());
				}
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
		items.parallelStream().forEach(item -> {
			try {
				psApi.updatePs((Professionnel) item);
				map.remove(item.getInternalId());
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
	public void visit(StructureCreateMap map) {
		Collection<RassEntity> items = map.values();
		items.parallelStream().forEach(item -> {
			try {
				structureApi.createNewStructure((Structure) item);
				// Remove structure from map if return code is 201
				map.remove(item.getInternalId());
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
	public void visit(StructureUpdateMap map) {
		Collection<RassEntity> items = map.values();
		items.parallelStream().forEach(item -> {
			try {
				structureApi.updateStructure((Structure) item);
				map.remove(item.getInternalId());
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
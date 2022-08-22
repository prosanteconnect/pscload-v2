/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.state;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.model.operations.OperationMap;
import fr.ans.psc.pscload.service.MessageProducer;
import fr.ans.psc.pscload.state.exception.LoadProcessException;
import fr.ans.psc.pscload.state.exception.LockedMapException;
import fr.ans.psc.pscload.state.exception.UploadException;
import fr.ans.psc.pscload.visitor.MapsUploaderVisitorImpl;
import fr.ans.psc.pscload.visitor.MapsVisitor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Class UploadingChanges.
 */
@Slf4j
public class UploadingChanges extends ProcessState {


    private String[] excludedProfessions;

    private String apiBaseUrl;

    private MessageProducer messageProducer;

    private List<String> excludedOperations;

    /**
     * Instantiates a new Uploading Changes.
     *
     * @param excludedProfessions the excluded professions
     * @param apiBaseUrl          the api base url
     */
    public UploadingChanges(String[] excludedProfessions, String apiBaseUrl, MessageProducer messageProducer) {
        this(excludedProfessions, apiBaseUrl, messageProducer, null);
    }

    public UploadingChanges(String[] excludedProfessions, String apiBaseUrl, MessageProducer messageProducer, List<String> excludedOperations) {
        this.excludedProfessions = excludedProfessions;
        this.apiBaseUrl = apiBaseUrl;
        this.messageProducer = messageProducer;
        this.excludedOperations = excludedOperations;
    }

    /**
     * Instantiates a new uploading changes.
     */
    public UploadingChanges() {
		super();
	}

	@Override
    public void nextStep() throws LoadProcessException {
        log.info("calling API...");
    	
		MapsVisitor visitor = new MapsUploaderVisitorImpl(excludedProfessions, apiBaseUrl, messageProducer);

        List<OperationMap<String, RassEntity>> processMaps = process.getMaps();
        if (excludedOperations != null) {
            String ops = String.join(",", excludedOperations);
            log.info("these operations won't be processed : {}", ops);
            processMaps.removeIf(map -> excludedOperations.contains(map.getOperation().name()));
        }

		try {
            for (OperationMap<String, RassEntity> map : processMaps) {
                map.accept(visitor);
            }
            log.info("API operations done.");
        } catch (LockedMapException e) {
            log.error("Shutdown was initiated during Uploading Changes stage.");
            throw new UploadException("Shutdown was initiated during Uploading Changes stage.");
        }

    }

    @Override
	public void write(Kryo kryo, Output output) {
    	kryo.writeClassAndObject(output, excludedProfessions);
    	kryo.writeClassAndObject(output, excludedOperations);
    	output.writeString(apiBaseUrl);
    }

    @Override
    @SuppressWarnings("unchecked")
	public void read(Kryo kryo, Input input) {
        excludedProfessions = (String[]) kryo.readClassAndObject(input);
        excludedOperations = (List<String>) kryo.readClassAndObject(input);
        apiBaseUrl = input.readString();
    }

    @Override
    public boolean isAlreadyComputed() {
        return true;
    }

    @Override
    public boolean isExpirable() { return false;}
}

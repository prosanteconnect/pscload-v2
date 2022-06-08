/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.state;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import fr.ans.psc.pscload.service.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import fr.ans.psc.pscload.PscloadApplication;
import fr.ans.psc.pscload.component.DuplicateKeyException;
import fr.ans.psc.pscload.component.ProcessRegistry;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.LoadProcess;
import fr.ans.psc.pscload.model.MapsHandler;
import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.model.operations.OperationMap;
import fr.ans.psc.pscload.service.EmailService;
import fr.ans.psc.pscload.utils.FileUtils;
import fr.ans.psc.pscload.model.operations.OperationType;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class ChangesAppliedTest.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PscloadApplication.class)
@AutoConfigureMockMvc
public class ChangesAppliedTest {

    @Autowired
    private CustomMetrics customMetrics;

    @Autowired
    private ProcessRegistry registry;

	@Autowired
	private EmailService emailService;

    @Autowired
    private MessageProducer messageProducer;

    /**
     * The http api mock server.
     */
    @RegisterExtension
    static WireMockExtension httpMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/api"))
            .configureStaticDsl(true).build();

    /**
     * Register pg properties.
     *
     * @param propertiesRegistry the properties registry
     */
    // For use with mockMvc
    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("deactivation.excluded.profession.codes", () -> "0");
        propertiesRegistry.add("pscextract.base.url", () -> httpMockServer.baseUrl());
        propertiesRegistry.add("files.directory", ()-> Thread.currentThread().getContextClassLoader().getResource("work").getPath());

    }

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() {
        File outputfolder = new File(Thread.currentThread().getContextClassLoader().getResource("work").getPath());
        File[] files = outputfolder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                f.delete();
            }
        }

        httpMockServer.stubFor(any(urlMatching("/generate-extract"))
                .willReturn(aResponse().withStatus(200)));
    }

    /**
     * Changes applied.
     *
     * @throws DuplicateKeyException the duplicate key exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    // CAS 100% PASSANT : pas de message généré, appel à extract
    @Test
    @DisplayName("Changes applied with no errors")
    public void changesApplied() throws DuplicateKeyException, IOException, ClassNotFoundException {
        httpMockServer.stubFor(post("/v2/ps").willReturn(aResponse().withStatus(200)));

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String rootpath = cl.getResource("work").getPath();
        File mapser = new File(rootpath + File.separator + "maps.ser");
        if (mapser.exists()) {
            mapser.delete();
        }
        //Day 1 : Generate old ser file
        LoadProcess p = new LoadProcess(new ReadyToComputeDiff(customMetrics));
        File extractFile1 = FileUtils.copyFileToWorkspace("Extraction_ProSanteConnect_Personne_activite_202112120512.txt");
        p.setExtractedFilename(extractFile1.getPath());
        p.nextStep();
        String[] exclusions = {"90"};
        p.setState(new UploadingChanges(exclusions, httpMockServer.baseUrl(), messageProducer));
        p.getState().setProcess(p);
        p.nextStep();
        p.setState(new ChangesApplied(customMetrics, httpMockServer.baseUrl(), emailService) );
        p.getState().setProcess(p);
        p.nextStep();

        // SET UP : updates ok, 2 different 4xx on Ps, 5xx on structure
        httpMockServer.stubFor(post("/v2/ps")
                .willReturn(aResponse().withStatus(409)));
        httpMockServer.stubFor(put("/v2/ps")
                .willReturn(aResponse().withStatus(200)));
        httpMockServer.stubFor(delete("/v2/ps/810107592585")
                .willReturn(aResponse().withStatus(410)));
        // Day 2 : Compute diff
        LoadProcess p2 = new LoadProcess(new ReadyToComputeDiff(customMetrics));
        registry.register(Integer.toString(registry.nextId()), p2);
        File extractFile2 = FileUtils.copyFileToWorkspace("Extraction_ProSanteConnect_Personne_activite_202112120515.txt");
        p2.setExtractedFilename(extractFile2.getPath());
        p2.nextStep();
        p2.setState(new DiffComputed(customMetrics));
        p2.nextStep();
        // Day 2 : upload changes

        p2.setState(new UploadingChanges(exclusions, httpMockServer.baseUrl(), messageProducer));
        p2.getState().setProcess(p2);
        p2.nextStep();

        OperationMap<String, RassEntity> psToCreate = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.CREATE))
				.findFirst().get();
        OperationMap<String, RassEntity> psToUpdate = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.UPDATE))
				.findFirst().get();
        OperationMap<String, RassEntity> psToDelete = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.DELETE))
				.findFirst().get();
        // 2xx return status should have been removed from update map
        assertEquals(1, psToCreate.size());
        assertEquals(1, psToDelete.size());
        assertEquals(0, psToUpdate.size());
        assertEquals(HttpStatus.CONFLICT.value(), psToCreate.get("810100375103").getReturnStatus());
        assertEquals(HttpStatus.GONE.value(), psToDelete.get("810107592585").getReturnStatus());

        // Apply changes and generate new ser
        p2.setState(new ChangesApplied(customMetrics, httpMockServer.baseUrl(), emailService));
        p2.getState().setProcess(p2);
        p2.nextStep();

        // check ser file : 409 create should be in, 410 delete should not, 5xx are in the previous state
        MapsHandler serializedMaps = new MapsHandler();
        serializedMaps.deserializeMaps(mapser.getAbsolutePath());
        assert serializedMaps.getPsMap().get("810100375103") != null;
        assert serializedMaps.getPsMap().get("810107592585") == null;
    }

}

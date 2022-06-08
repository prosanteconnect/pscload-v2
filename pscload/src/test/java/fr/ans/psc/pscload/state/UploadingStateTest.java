/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.state;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import fr.ans.psc.pscload.service.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import fr.ans.psc.pscload.PscloadApplication;
import fr.ans.psc.pscload.component.ProcessRegistry;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.LoadProcess;
import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.model.operations.OperationMap;
import fr.ans.psc.pscload.service.EmailService;
import fr.ans.psc.pscload.utils.FileUtils;
import fr.ans.psc.pscload.model.operations.OperationType;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class DiffComputedStateTest.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PscloadApplication.class)
@AutoConfigureMockMvc
public class UploadingStateTest {

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
    static WireMockExtension httpApiMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/api"))
            .build();

    /**
     * Register pg properties.
     *
     * @param propertiesRegistry the properties registry
     */
    // For use with mockMvc
    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("api.base.url",
                () -> httpApiMockServer.baseUrl());
        propertiesRegistry.add("deactivation.excluded.profession.codes", () -> "0");
        propertiesRegistry.add("pscextract.base.url", () -> httpApiMockServer.baseUrl());
        propertiesRegistry.add("files.directory", ()-> Thread.currentThread().getContextClassLoader().getResource("work").getPath());
    }

	/**
	 * Setup.
	 *
	 * @throws Exception the exception
	 */
	@BeforeEach
	void setup() {
		registry.clear();
		// clear work directory
		File outputfolder = new File(Thread.currentThread().getContextClassLoader().getResource("work").getPath());
		File[] files = outputfolder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				f.delete();
			}
		}
	}

    /**
     * Upload changes delete PS.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("Call delete API with return code 200")
    void uploadChangesDeletePS() throws Exception {



        httpApiMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
        //Test
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String rootpath = cl.getResource("work").getPath();
        File mapser = new File(rootpath + File.separator + "maps.ser");
        if (mapser.exists()) {
            mapser.delete();
        }
        //Day 1 : Generate old ser file
        LoadProcess p = new LoadProcess(new ReadyToComputeDiff(customMetrics));
        File extractFile1 = FileUtils.copyFileToWorkspace("Extraction_ProSanteConnect_Personne_activite_202112120513.txt");
        p.setExtractedFilename(extractFile1.getPath());
        p.getState().setProcess(p);
        p.nextStep();
        String[] exclusions = {"90"};
        p.setState(new UploadingChanges(exclusions, httpApiMockServer.baseUrl(), messageProducer));
        p.getState().setProcess(p);
        p.nextStep();
        p.setState(new ChangesApplied(customMetrics, httpApiMockServer.baseUrl(), emailService));
        p.getState().setProcess(p);
        p.nextStep();


        httpApiMockServer.stubFor(delete("/v2/ps/810107592544")
                .willReturn(aResponse().withStatus(200)));
        httpApiMockServer.stubFor(any(urlMatching("/generate-extract")).willReturn(aResponse().withStatus(200)));
        // Day 2 : Compute diff (1 delete)
        LoadProcess p2 = new LoadProcess(new ReadyToComputeDiff(customMetrics));
        registry.register(Integer.toString(registry.nextId()), p2);
        File extractFile2 = FileUtils.copyFileToWorkspace("Extraction_ProSanteConnect_Personne_activite_202112120514.txt");
        p2.setExtractedFilename(extractFile2.getPath());
        p2.getState().setProcess(p2);
        p2.nextStep();
        // Day 2 : upload changes (1 delete)
        p2.setState(new UploadingChanges(exclusions, httpApiMockServer.baseUrl(), messageProducer));
        p2.getState().setProcess(p2);
        p2.nextStep();
		OperationMap<String, RassEntity> psToCreate2 = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.CREATE))
				.findFirst().get();
        OperationMap<String, RassEntity> psToDelete2 = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.DELETE))
				.findFirst().get();
        OperationMap<String, RassEntity> psToUpdate2 = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.UPDATE))
				.findFirst().get();
        assertEquals(0, psToCreate2.size());
        assertEquals(0, psToDelete2.size());
        assertEquals(0, psToUpdate2.size());

    }

    /**
     * Upload changes delete PS 404.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("Call delete API with return code 410")
    void uploadChangesDeletePS410() throws Exception {
        httpApiMockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
        //Test
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String rootpath = cl.getResource(".").getPath();
        File mapser = new File(rootpath + File.separator + "maps.ser");
        if (mapser.exists()) {
            mapser.delete();
        }
        //Day 1 : Generate old ser file
        LoadProcess p = new LoadProcess(new ReadyToComputeDiff(customMetrics));
        File extractFile1 = FileUtils.copyFileToWorkspace("Extraction_ProSanteConnect_Personne_activite_202112120513.txt");
        p.setExtractedFilename(extractFile1.getPath());
        p.nextStep();
        String[] exclusions = {"90"};
        p.setState(new UploadingChanges(exclusions, httpApiMockServer.baseUrl(), messageProducer));
        p.getState().setProcess(p);
        p.nextStep();
        p.setState(new ChangesApplied(customMetrics, httpApiMockServer.baseUrl(), emailService));
        p.getState().setProcess(p);
        p.nextStep();


        httpApiMockServer.stubFor(delete("/v2/ps/810107592544")
                .willReturn(aResponse().withStatus(410)));
        httpApiMockServer.stubFor(any(urlMatching("/generate-extract")).willReturn(aResponse().withStatus(200)));
        // Day 2 : Compute diff (1 delete)
        LoadProcess p2 = new LoadProcess(new ReadyToComputeDiff(customMetrics));
        registry.register(Integer.toString(registry.nextId()), p2);
        File extractFile2 = FileUtils.copyFileToWorkspace("Extraction_ProSanteConnect_Personne_activite_202112120514.txt");
        p2.setExtractedFilename(extractFile2.getPath());
        p2.nextStep();
        p2.setState(new DiffComputed(customMetrics));
        p2.nextStep();
        // Day 2 : upload changes (1 delete)

        p2.setState(new UploadingChanges(exclusions, httpApiMockServer.baseUrl(), messageProducer));
        p2.nextStep();
		OperationMap<String, RassEntity> psToCreate2 = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.CREATE))
				.findFirst().get();
        OperationMap<String, RassEntity> psToDelete2 = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.DELETE))
				.findFirst().get();
        OperationMap<String, RassEntity> psToUpdate2 = p2.getMaps().stream().filter(map -> map.getOperation().equals(OperationType.UPDATE))
				.findFirst().get();
        assertEquals(0, psToCreate2.size());
        assertEquals(1, psToDelete2.size());
        assertEquals(0, psToUpdate2.size());
        assertEquals(HttpStatus.GONE.value(), psToDelete2.get("810107592544").getReturnStatus());

    }
}

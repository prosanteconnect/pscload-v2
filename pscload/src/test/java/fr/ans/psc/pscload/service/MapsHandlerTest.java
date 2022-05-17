/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.service;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import fr.ans.psc.pscload.PscloadApplication;
import fr.ans.psc.pscload.model.MapsHandler;
import fr.ans.psc.pscload.model.entities.ExerciceProfessionnel;
import fr.ans.psc.pscload.model.entities.Professionnel;
import fr.ans.psc.pscload.model.entities.SituationExercice;
import fr.ans.psc.pscload.model.entities.Structure;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class MapsHandlerTest.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PscloadApplication.class)
@AutoConfigureMockMvc
public class MapsHandlerTest {

    /** The http mock server. */
    @RegisterExtension
    static WireMockExtension httpMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().usingFilesUnderClasspath("wiremock")).build();

    /**
     * Serializer test.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    @Test
    @DisplayName("Serialize and deserialize file")
    public void serializerTest() throws IOException, ClassNotFoundException {
    	//Log.TRACE();
        File initialFile = new File(Thread.currentThread().getContextClassLoader()
                .getResource("Extraction_ProSanteConnect_Personne_activite_202112120512.txt").getPath());

        MapsHandler initialMaps = new MapsHandler();
        initialMaps.loadMapsFromFile(initialFile);
        assertEquals(5, initialMaps.getPsMap().size());

        File serializedFile = new File(Thread.currentThread().getContextClassLoader()
                .getResource("work").getPath() + File.separator + "maps.ser");
        initialMaps.serializeMaps(serializedFile.getAbsolutePath());

        MapsHandler deserializedMaps = new MapsHandler();
        deserializedMaps.deserializeMaps(serializedFile.getAbsolutePath());
        assertEquals(initialMaps, deserializedMaps);
    }

    /**
     * Line generator.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("test line generator")
    public void lineGenerator() throws IOException {
        File initialFile = new File(Thread.currentThread().getContextClassLoader()
                .getResource("Extraction_ProSanteConnect_Personne_activite_202112120512.txt").getPath());

        MapsHandler initialMaps = new MapsHandler();
        initialMaps.loadMapsFromFile(initialFile);

        Professionnel professionnel = initialMaps.getPsMap().get("0012800728");
        ExerciceProfessionnel exerciceProfessionnel = professionnel.getExercicesProfessionels().get(0);
        SituationExercice situationExercice = exerciceProfessionnel.getSituationsExercice().get(0);
        Structure structure = (Structure) situationExercice.getStructure();

        String line = initialMaps.generateLine(professionnel, exerciceProfessionnel, situationExercice, structure);
        String expectedLine = "0|012800728|0012800728|EVRARD|Patrice''|10/03/1968|||||||M|28|C||EVRARD|PATRICE|||L|SA42|||39806996300013||||C39806996300013|SARL PATRICE EVRARD||SARL PATRICE EVRARD||||BD|CHARLES DE GAULLE|CENTRE COMMERCIAL CARREFOUR|01000 BOURG EN BRESSE|01000||||||||339806996300013|ARS/CPAM/CPAM||\n";
        assertEquals(expectedLine, line);
    }

    /**
     * Generate txt file.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("generate txt file from ser")
    public void generateTxtFile() throws IOException {
        File initialFile = new File(Thread.currentThread().getContextClassLoader()
                .getResource("Extraction_ProSanteConnect_Personne_activite_202112120512.txt").getPath());

        MapsHandler initialMaps = new MapsHandler();
        initialMaps.loadMapsFromFile(initialFile);

        File generatedTxtFile = new File(Thread.currentThread().getContextClassLoader()
                .getResource("work").getPath() + File.separator + "generated.txt");
        generatedTxtFile = initialMaps.generateTxtFile(generatedTxtFile.getAbsolutePath());

        MapsHandler generatedMaps = new MapsHandler();
        generatedMaps.loadMapsFromFile(generatedTxtFile);

        assertEquals(initialMaps.getPsMap().size(), generatedMaps.getPsMap().size());

        initialMaps.getPsMap().values().forEach(professionnel -> {
            Professionnel generatedPs = generatedMaps.getPsMap().get(professionnel.getInternalId());
            assert professionnel.equals(generatedPs);
        });
    }

    @Test
    @DisplayName("check order impact on hashCode and equals methods")
    public void checkDifferentOrderForPs() throws IOException {
        File file1 = new File(Thread.currentThread().getContextClassLoader()
                .getResource("2WorkSituationsOrder1").getPath());
        MapsHandler order1Map = new MapsHandler();
        order1Map.loadMapsFromFile(file1);

        File file2 = new File(Thread.currentThread().getContextClassLoader()
                .getResource("2WorkSituationsOrder2").getPath());
        MapsHandler order2Map = new MapsHandler();
        order2Map.loadMapsFromFile(file2);

        assertEquals(1,order1Map.getPsMap().size());
        assertEquals(1, order2Map.getPsMap().size());

        MapDifference<String, Professionnel> diffPs = Maps.difference(order1Map.getPsMap(), order2Map.getPsMap());
        assertEquals(0, diffPs.entriesDiffering().size());
        assertEquals(order1Map.getPsMap().get("810107592544").hashCode(), order2Map.getPsMap().get("810107592544").hashCode());
        assertEquals(order1Map.getPsMap().get("810107592544"), order2Map.getPsMap().get("810107592544"));
    }
}

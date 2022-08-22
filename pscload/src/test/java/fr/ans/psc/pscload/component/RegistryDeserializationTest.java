package fr.ans.psc.pscload.component;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import fr.ans.psc.pscload.PscloadApplication;
import fr.ans.psc.pscload.model.LoadProcess;
import fr.ans.psc.pscload.model.entities.*;
import fr.ans.psc.pscload.model.operations.*;
import fr.ans.psc.pscload.state.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@SpringBootTest
@ContextConfiguration(classes = PscloadApplication.class)
public class RegistryDeserializationTest {

    private static Kryo kryo;

    static {
        kryo = new Kryo();
        OperationMapSerializer operationMapSerializer = new OperationMapSerializer();
        kryo.register(HashMap.class, 9);
        kryo.register(ArrayList.class, 10);
        kryo.register(Professionnel.class, 11);
        kryo.register(ExerciceProfessionnel.class, 12);
        kryo.register(SavoirFaire.class, 13);
        kryo.register(SituationExercice.class, 14);
        kryo.register(Structure.class, 16);
        kryo.register(ProcessRegistry.class, 17);
        kryo.register(LoadProcess.class, 18);
        kryo.register(ProcessState.class, 19);
        kryo.register(Submitted.class, 20);
        kryo.register(DiffComputed.class, 21);
        kryo.register(ReadyToComputeDiff.class, 22);
        kryo.register(ReadyToExtract.class, 23);
        kryo.register(UploadingChanges.class, 24);
        kryo.register(ChangesApplied.class, 25);
        kryo.register(String[].class, 27);
        kryo.register(ConcurrentHashMap.class, 28);
        kryo.register(UploadInterrupted.class, 29);
        kryo.register(SerializationInterrupted.class, 30);
        kryo.register(OperationMap.class, operationMapSerializer, 31);
        kryo.register(PsCreateMap.class, 32);
        kryo.register(PsUpdateMap.class, operationMapSerializer, 33);
        kryo.register(PsDeleteMap.class, 34);
    }

    @Autowired
    private ProcessRegistry registry;


    /**
     * Read registry after shutdown test.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Disabled
    @Test
    public void readRegistryAfterShutdownTest() throws IOException {
//        File registryFile = FileUtils.copyFileToWorkspace("registry.ser");
        File registryFile = new File(Thread.currentThread().getContextClassLoader().getResource("work/registry.ser").getPath());
        FileInputStream fileInputStream = new FileInputStream(registryFile);
        Input input = new Input(fileInputStream);
        registry.read(kryo, input);
        input.close();

        assertTrue(registry.getCurrentProcess().isRemainingPsOrStructuresInMaps());
    }
}

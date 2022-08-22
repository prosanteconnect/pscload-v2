/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import fr.ans.psc.model.FirstName;
import org.apache.any23.encoding.TikaEncodingDetector;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.univocity.parsers.common.DataProcessingException;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.ObjectRowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import fr.ans.psc.model.Profession;
import fr.ans.psc.pscload.model.entities.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class MapsHandler.
 */
@Getter
@Setter

/**
 * Can equal.
 *
 * @param other the other
 * @return true, if successful
 */
@EqualsAndHashCode()
@Slf4j
public class MapsHandler implements KryoSerializable {

	private static Kryo kryo;

	static {
		kryo = new Kryo();
		kryo.register(HashMap.class, 9);
		kryo.register(ArrayList.class, 10);
		kryo.register(Professionnel.class, 11);
		kryo.register(ExerciceProfessionnel.class, 12);
		kryo.register(SavoirFaire.class, 13);
		kryo.register(SituationExercice.class, 14);
		kryo.register(Structure.class, 16);
		kryo.register(FirstName.class, 35);
	}

	private static final int ROW_LENGTH = RassItems.values().length + 1;

	private Map<String, Professionnel> psMap = new HashMap<>();

	/**
	 * Load maps from file.
	 *
	 * @param file the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void loadMapsFromFile(File file) throws IOException, IllegalArgumentException, DataProcessingException {
		log.info("loading {} into list of Ps", file.getName());

		// ObjectRowProcessor converts the parsed values and gives you the resulting
		// row.
		ObjectRowProcessor rowProcessor = new ObjectRowProcessor() {
			@Override
			public void rowProcessed(Object[] objects, ParsingContext parsingContext) {

				String[] items = Arrays.asList(objects).toArray(new String[ROW_LENGTH]);
				// test if exists by nationalId (item 2)
				Professionnel psMapped = psMap.get(items[RassItems.NATIONAL_ID.column]);
				if (psMapped == null) {
					// create PS and add to map
					Professionnel psRow = new Professionnel(items, true);
					psMap.put(psRow.getNationalId(), psRow);
				} else {
					// if ps exists then add expro and situ exe.
					Optional<Profession> p = psMapped.getProfessionByCodeAndCategory(
							items[RassItems.EX_PRO_CODE.column], items[RassItems.CATEGORY_CODE.column]);
					if (p.isPresent()) {
						// add worksituation : it can't exists, otherwise it is a duplicate entry.
						SituationExercice situ = new SituationExercice(items);
						p.get().addWorkSituationsItem(situ);
					} else {
						// Add profession and worksituation
						ExerciceProfessionnel exepro = new ExerciceProfessionnel(items);
						psMapped.addProfessionsItem(exepro);

					}
				}
			}
		};

		CsvParserSettings parserSettings = new CsvParserSettings();
		parserSettings.getFormat().setLineSeparator("\n");
		parserSettings.getFormat().setDelimiter('|');
		parserSettings.setProcessor(rowProcessor);
		parserSettings.setHeaderExtractionEnabled(true);
		parserSettings.setNullValue("");

		CsvParser parser = new CsvParser(parserSettings);

		// get file charset to secure data encoding
		try (InputStream is = new FileInputStream(file)) {
			Charset detectedCharset = Charset.forName(new TikaEncodingDetector().guessEncoding(is));
			log.debug("detected charset is : " + detectedCharset.displayName());
			parser.parse(new BufferedReader(new FileReader(file, detectedCharset)));
		} catch (IOException e) {
			throw new IOException("Encoding detection failure", e);
		}
		log.info("loading complete!");
	}

	/**
	 * Serialize maps.
	 *
	 * @param filename the filename
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void serializeMaps(String filename) throws IOException {
		File mapsFile = new File(filename);
		FileOutputStream fileOutputStream = new FileOutputStream(mapsFile);
		Output output = new Output(fileOutputStream);
		write(kryo, output);
		output.close();
	}

	/**
	 * Deserialize maps.
	 *
	 * @param filename the filename
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	public void deserializeMaps(String filename) throws IOException, ClassNotFoundException {
		FileInputStream fileInputStream = new FileInputStream(filename);
		Input input = new Input(fileInputStream);
		read(kryo, input);
		input.close();
	}


	/**
	 * Generate txt file.
	 *
	 * @param fileName the file name
	 * @return the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public File generateTxtFile(String fileName) throws IOException {
		File txtFile = new File(fileName);
		Writer writer = new FileWriter(txtFile, StandardCharsets.UTF_8);

		String header = "Type d'identifiant PP|Identifiant PP|Identification nationale PP|Nom de famille|Prénoms|" +
				"Date de naissance|Code commune de naissance|Code pays de naissance|Lieu de naissance|Code sexe|" +
				"Téléphone (coord. correspondance)|Adresse e-mail (coord. correspondance)|Code civilité|Code profession|" +
				"Code catégorie professionnelle|Code civilité d'exercice|Nom d'exercice|Prénom d'exercice|" +
				"Code type savoir-faire|Code savoir-faire|Code mode exercice|Code secteur d'activité|" +
				"Code section tableau pharmaciens|Code rôle|Numéro SIRET site|Numéro SIREN site|Numéro FINESS site|" +
				"Numéro FINESS établissement juridique|Identifiant technique de la structure|Raison sociale site|" +
				"Enseigne commerciale site|Complément destinataire (coord. structure)|" +
				"Complément point géographique (coord. structure)|Numéro Voie (coord. structure)|" +
				"Indice répétition voie (coord. structure)|Code type de voie (coord. structure)|" +
				"Libellé Voie (coord. structure)|Mention distribution (coord. structure)|" +
				"Bureau cedex (coord. structure)|Code postal (coord. structure)|Code commune (coord. structure)|" +
				"Code pays (coord. structure)|Téléphone (coord. structure)|Téléphone 2 (coord. structure)|" +
				"Télécopie (coord. structure)|Adresse e-mail (coord. structure)|Code département (coord. structure)|" +
				"Ancien identifiant de la structure|Autorité d'enregistrement|\n";

		writer.write(header);

		for (Professionnel professionnel : psMap.values()) {
			for (ExerciceProfessionnel exerciceProfessionnel : professionnel.getExercicesProfessionels()) {
				for (SituationExercice situationExercice : exerciceProfessionnel.getSituationsExercice()) {
					Structure structure = (Structure) situationExercice.getStructure();
					writer.write(generateLine(professionnel, exerciceProfessionnel, situationExercice, structure));
				}
			}
		}
		writer.flush();
		writer.close();
		return txtFile;
	}

	/**
	 * Generate line.
	 *
	 * @param professionnel the professionnel
	 * @param exerciceProfessionnel the exercice professionnel
	 * @param situationExercice the situation exercice
	 * @param structure the structure
	 * @return the string
	 */
	public String generateLine (Professionnel professionnel, ExerciceProfessionnel exerciceProfessionnel,
								SituationExercice situationExercice, Structure structure) {

		StringBuilder sb = new StringBuilder();
		String[] items = new String[RassItems.values().length];

		for (SavoirFaire savoirFaire : exerciceProfessionnel.getSavoirFaire()) {
			professionnel.setProfessionnelItems(items);
			exerciceProfessionnel.setExerciceProfessionnelItems(items);
			savoirFaire.setSavoirFaireItems(items);
			situationExercice.setSituationExerciceItems(items);
			if (structure != null) {
				structure.setStructureItems(items);
			} else {
				// because registration column comes AFTER structure columns in csv, it may have a value
				// we don't want to erase it in loop
				// therefore the null check
				IntStream.range(RassItems.SITE_SIRET.column, RassItems.values().length).forEach(i -> {
					if (items[i] == null) {
						items[i] = "";
					}
				});
			}
			sb.append(String.join("|", items))
					.append("|")
					.append("\n");
		}
		return sb.toString();

	}

	@Override
	public void write(Kryo kryo, Output output) {
		kryo.writeObject(output, psMap);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Kryo kryo, Input input) {
		psMap = (Map<String, Professionnel>) kryo.readObject(input, HashMap.class);
	}

	/**
	 * Clear maps.
	 */
	public void clearMaps() {
		psMap.clear();
	}
}

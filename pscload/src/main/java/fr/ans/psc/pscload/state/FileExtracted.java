package fr.ans.psc.pscload.state;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.any23.encoding.TikaEncodingDetector;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.ObjectRowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import fr.ans.psc.model.Profession;
import fr.ans.psc.pscload.model.ExerciceProfessionnel;
import fr.ans.psc.pscload.model.Professionnel;
import fr.ans.psc.pscload.model.SituationExercice;
import fr.ans.psc.pscload.model.Structure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileExtracted extends ProcessState {

	private static final int ROW_LENGTH = 50;

	private static final long serialVersionUID = 1208602116799660764L;

	private final Map<String, Professionnel> newPsMap = new HashMap<>();

	private final Map<String, Structure> newStructureMap = new HashMap<>();

	private Map<String, Professionnel> oldPsMap = new HashMap<>();

	private Map<String, Structure> oldStructureMap = new HashMap<>();

	public FileExtracted() {
		super();
	}
    
	@Override
	public void runTask() {
		// TODO load maps
		File fileToLoad = new File(process.getExtractedFilename());
		try {
			loadMapsFromTextFile(fileToLoad);
			// we serialize new map now in a temp file (maps.{timestamp}.lock
			File tmpmaps = new File(fileToLoad.getParent() + File.separator + "maps." + process.getTimestamp() + ".lock");
			serialize(tmpmaps.getPath());
			// deserialize the old file if exists
			File maps = new File(fileToLoad.getParent() + File.separator + "maps.ser");
			if (maps.exists()) {
				deserialize(fileToLoad.getParent() + File.separator + "maps.ser");
			}
			// Launch diff
			process.setPsMap(diffPsMaps(oldPsMap, newPsMap));
			process.setStructureMap(diffStructureMaps(oldStructureMap, newStructureMap));
			// Rename serialized file
			maps.delete();
			tmpmaps.renameTo(maps);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Diff PS maps.
	 *
	 * @param original OG PS map
	 * @param revised  the revised PS map
	 * @return the map difference
	 */
	public MapDifference<String, Professionnel> diffPsMaps(Map<String, Professionnel> original, Map<String, Professionnel> revised) {
	    MapDifference<String, Professionnel> psDiff = Maps.difference(original, revised);
	    return psDiff;
	}

	/**
	 * Diff structure maps.
	 *
	 * @param original the original
	 * @param revised  the revised
	 * @return the map difference
	 */
	public MapDifference<String, Structure> diffStructureMaps(Map<String, Structure> original, Map<String, Structure> revised) {
	    MapDifference<String, Structure> structureDiff = Maps.difference(original, revised);	
	    return structureDiff;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		//TODO save metrics
		out.writeObject(newPsMap);
		out.writeObject(newStructureMap);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		//TODO restore metrics
		oldPsMap = (Map<String, Professionnel>) in.readObject();
		oldStructureMap = (Map<String, Structure>) in.readObject();

	}

	private void loadMapsFromTextFile(File file)
			throws IOException {
		log.info("loading {} into list of Ps", file.getName());
		newPsMap.clear();
		newStructureMap.clear();
		// ObjectRowProcessor converts the parsed values and gives you the resulting
		// row.
		ObjectRowProcessor rowProcessor = new ObjectRowProcessor() {
			@Override
			public void rowProcessed(Object[] objects, ParsingContext parsingContext) {
				if (objects.length != ROW_LENGTH) {
					throw new IllegalArgumentException();
				}
				String[] items = Arrays.asList(objects).toArray(new String[ROW_LENGTH]);
				// test if exists by nationalId (item 2)
				Professionnel psMapped = newPsMap.get(items[2]);
				if (psMapped == null) {
					// create PS and add to map
					Professionnel psRow = new Professionnel(items, true);
					newPsMap.put(psRow.getNationalId(), psRow);
				} else {
					// if ps exists then add expro and situ exe.
					Optional<Profession> p = psMapped.getProfessionByCodeAndCategory(items[13], items[14]);
					if (p.isPresent()) {
						// add worksituation : it can't exists, otherwise it is a duplicate entry.
						SituationExercice situ = new SituationExercice(items);
						p.get().addWorkSituationsItem(situ);
					} else {
						// Add profession and worksituation
						ExerciceProfessionnel exepro = new ExerciceProfessionnel(items);
						psMapped.addProfessionsItem(exepro);
						;
					}
				}
				// get structure in map by its reference from row
				if (newStructureMap.get(items[28]) == null) {
					Structure newStructure = new Structure(items);
					newStructureMap.put(newStructure.getStructureTechnicalId(), newStructure);
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
		InputStream is = new FileInputStream(file);
		try {
			Charset detectedCharset = Charset.forName(new TikaEncodingDetector().guessEncoding(is));
			parser.parse(new BufferedReader(new FileReader(file, detectedCharset)));
		} catch (IOException e) {
			throw new IOException("Encoding detection failure", e);
		}
		log.info("loading complete!");
	}

	private void serialize(String filename) throws IOException {
		File mapsFile = new File(filename);
		FileOutputStream fileOutputStream = new FileOutputStream(mapsFile);
		ObjectOutputStream oos = new ObjectOutputStream(fileOutputStream);
		writeExternal(oos);
		oos.close();
	}

	private void deserialize(String filename) throws IOException, ClassNotFoundException {
		FileInputStream fileInputStream = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fileInputStream);
		readExternal(ois);
		ois.close();
	}

}

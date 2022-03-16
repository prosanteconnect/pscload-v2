/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.model.operations;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.visitor.MapsVisitor;
import fr.ans.psc.pscload.visitor.OperationType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class StructureUpdateMap.
 */
public class StructureUpdateMap implements OperationMap<String, RassEntity> {

	private Map<String, RassEntity> newValues;
	private Map<String, RassEntity> oldValues;

	/**
	 * Instantiates a new structure update map.
	 */
	public StructureUpdateMap() {
	}

	@Override
	public OperationType getOperation() {
		return OperationType.STRUCTURE_UPDATE;
	}

	@Override
	public void accept(MapsVisitor visitor) {
		visitor.visit(this);
		
	}

	@Override
	public void saveNewValue(String key, RassEntity value) {
		if (newValues == null) {
			newValues = new ConcurrentHashMap<>();
		}
		newValues.put(key, value);
	}

	@Override
	public RassEntity getNewValue(String key) {
		if (newValues == null) {
			newValues = new ConcurrentHashMap<>();
		}
		return newValues.get(key);
	}

	@Override
	public void saveOldValue(String key, RassEntity value) {
		if (oldValues == null) {
			oldValues = new ConcurrentHashMap<>();
		}
		oldValues.put(key, value);
	}

	@Override
	public RassEntity getOldValue(String key) {
		if (oldValues == null) {
			oldValues = new ConcurrentHashMap<>();
		}
		return oldValues.get(key);
	}

	@Override
	public Map<String, RassEntity> getNewValues() {
		if (newValues == null) {
			newValues = new ConcurrentHashMap<>();
		}
		return newValues;
	}

	@Override
	public Map<String, RassEntity> getOldValues() {
		if (oldValues == null) {
			oldValues = new ConcurrentHashMap<>();
		}
		return oldValues;
	}

	@Override
	public void write(Kryo kryo, Output output) {
		kryo.writeObjectOrNull(output, newValues, ConcurrentHashMap.class);
		kryo.writeObjectOrNull(output, oldValues, ConcurrentHashMap.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Kryo kryo, Input input) {
		newValues = (Map<String, RassEntity>) kryo.readObjectOrNull(input, ConcurrentHashMap.class);
		oldValues = (Map<String, RassEntity>) kryo.readObjectOrNull(input, ConcurrentHashMap.class);
	}

}

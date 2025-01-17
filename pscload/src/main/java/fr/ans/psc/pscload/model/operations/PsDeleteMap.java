/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.model.operations;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import fr.ans.psc.pscload.model.entities.RassEntity;
import fr.ans.psc.pscload.visitor.MapsVisitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class PsDeleteMap.
 */
public class PsDeleteMap extends OperationMap<String, RassEntity> {

	private Map<String , RassEntity> newValues;

	/**
	 * Instantiates a new ps delete map.
	 */
	public PsDeleteMap() {
		super();
	}

	/**
	 * Instantiates a new ps delete map.
	 *
	 * @param operation the operation
	 */
	public PsDeleteMap(OperationType operation) {
		super(operation);

	}


	@Override
	public OperationType getOperation() {
		return OperationType.DELETE;
	}

	@Override
	public void accept(MapsVisitor visitor) {
		visitor.visit(this);
		
	}

}

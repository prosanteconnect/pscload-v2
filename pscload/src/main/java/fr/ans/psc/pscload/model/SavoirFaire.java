/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.ans.psc.model.Expertise;
import lombok.EqualsAndHashCode;

/**
 * Can equal.
 *
 * @param other the other
 * @return true, if successful
 */
@EqualsAndHashCode(callSuper = true)
public class SavoirFaire extends Expertise implements Externalizable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8385751071373736733L;

	   /**
   	 * Instantiates a new savoir faire.
   	 */
   	public SavoirFaire() {
		super();
	}

	/**
	 * Instantiates a new savoir faire.
	 *
	 * @param items the items
	 */
	public SavoirFaire(String[] items){
		   super();
	        setTypeCode(items[18]);
	        setCode(items[19]);
	    }

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(getTypeCode());
		out.writeObject(getCode());
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		setCode((String) in.readObject());
		setTypeCode((String) in.readObject());
	}
}

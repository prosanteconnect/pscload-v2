/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.model.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import fr.ans.psc.model.Profession;
import fr.ans.psc.model.Ps;

/**
 * Can equal.
 *
 * @param other the other
 * @return true, if successful
 */
public class Professionnel extends Ps implements RassEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2859304844064466893L;
	
	/**
	 * returnStatus after failure in change request
	 */
	private int returnStatus = 100;

	/**
	 * Instantiates a new professionnel.
	 */
	public Professionnel() {
		super();
	}

	/**
	 * Instantiates a new professionnel.
	 *
	 * @param items the items
	 * @param deep the deep
	 */
	public Professionnel(String[] items, boolean deep) {
		super();
		setIdType(items[RassItems.ID_TYPE.column]);
		setId(items[RassItems.ID.column]);
		setNationalId(items[RassItems.NATIONAL_ID.column]);
		setLastName(items[RassItems.LAST_NAME.column]);
		setFirstName(items[RassItems.FIRST_NAME.column]);
		setDateOfBirth(items[RassItems.DOB.column]);
		setBirthAddressCode(items[RassItems.BIRTH_ADDRESS_CODE.column]);
		setBirthCountryCode(items[RassItems.BIRTH_COUNTRY_CODE.column]);
		setBirthAddress(items[RassItems.BIRTH_ADDRESS.column]);
		setGenderCode(items[RassItems.GENDER_CODE.column]);
		setPhone(items[RassItems.PHONE.column]);
		setEmail(items[RassItems.EMAIL.column]);
		setSalutationCode(items[RassItems.SALUTATION_CODE.column]);
		if (deep) {
			addProfessionsItem(new ExerciceProfessionnel(items));
		}
	}

	public void setProfessionnelItems(String[] items) {
		items[RassItems.ID_TYPE.column] = getIdType();
		items[RassItems.ID.column] = getId();
		items[RassItems.NATIONAL_ID.column] = getNationalId();
		items[RassItems.LAST_NAME.column] = getLastName();
		items[RassItems.FIRST_NAME.column] = getFirstName();
		items[RassItems.DOB.column] = getDateOfBirth();
		items[RassItems.BIRTH_ADDRESS_CODE.column] = getBirthAddressCode();
		items[RassItems.BIRTH_COUNTRY_CODE.column] = getBirthCountryCode();
		items[RassItems.BIRTH_ADDRESS.column] = getBirthAddress();
		items[RassItems.GENDER_CODE.column] = getGenderCode();
		items[RassItems.PHONE.column] = getPhone();
		items[RassItems.EMAIL.column] = getEmail();
		items[RassItems.SALUTATION_CODE.column] = getSalutationCode();
	}

	/**
	 * Gets the profession by code and category.
	 *
	 * @param code the code
	 * @param category the category
	 * @return the profession by code and category
	 */
	public Optional<Profession> getProfessionByCodeAndCategory(String code, String category) {
		return getProfessions().stream()
				.filter(exo -> exo.getCode().concat(exo.getCategoryCode()).equals(code.concat(category))).findAny();

	}

	@Override
	public int getReturnStatus() {
		return returnStatus;
	}

	@Override
	public void setReturnStatus(int returnStatus) {
		this.returnStatus = returnStatus;
	}

	@Override
	public String getInternalId() {
		return getNationalId();
	}
	
	@Override
	public String getIdType() {
		return super.getIdType();
	}

	public List<ExerciceProfessionnel> getExercicesProfessionels() {
		List<Profession> professions = getProfessions();
		List<ExerciceProfessionnel> exercicesProfessionnels = new ArrayList<>();
		professions.forEach(profession -> exercicesProfessionnels.add((ExerciceProfessionnel) profession));
		return exercicesProfessionnels;
	}
  
	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getIdType(), getId(), getNationalId(), getLastName(), getFirstName(),
				getDateOfBirth(), getBirthAddressCode(), getBirthCountryCode(), getBirthAddress(),
				getGenderCode(), getPhone(), getEmail(), getSalutationCode(),
				getExercicesProfessionels().stream().map(ExerciceProfessionnel::hashCode).reduce(0, Integer::sum));
	}
}

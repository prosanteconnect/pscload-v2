/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.model.entities;

/**
 * The Enum RassItems.
 */
public enum RassItems {
    
    /** The id type. */
    ID_TYPE(0),
    
    /** The id. */
    ID(1),
    
    /** The national id. */
    NATIONAL_ID(2),
    
    /** The last name. */
    LAST_NAME(3),
    
    /** The first name. */
    FIRST_NAMES(4),
    
    /** The dob. */
    DOB(5),
    
    /** The birth address code. */
    BIRTH_ADDRESS_CODE(6),
    
    /** The birth country code. */
    BIRTH_COUNTRY_CODE(7),
    
    /** The birth address. */
    BIRTH_ADDRESS(8),
    
    /** The gender code. */
    GENDER_CODE(9),
    
    /** The phone. */
    PHONE(10),
    
    /** The email. */
    EMAIL(11),
    
    /** The salutation code. */
    SALUTATION_CODE(12),
    
    /** The ex pro code. */
    EX_PRO_CODE(13),
    
    /** The category code. */
    CATEGORY_CODE(14),
    
    /** The ex pro salutation code. */
    EX_PRO_SALUTATION_CODE(15),
    
    /** The ex pro last name. */
    EX_PRO_LAST_NAME(16),
    
    /** The ex pro first name. */
    EX_PRO_FIRST_NAME(17),
    
    /** The expertise type code. */
    EXPERTISE_TYPE_CODE(18),
    
    /** The expertise code. */
    EXPERTISE_CODE(19),
    
    /** The situation mode code. */
    SITUATION_MODE_CODE(20),
    
    /** The activity sector code. */
    ACTIVITY_SECTOR_CODE(21),
    
    /** The pharmacist table section code. */
    PHARMACIST_TABLE_SECTION_CODE(22),
    
    /** The situation role code. */
    SITUATION_ROLE_CODE(23),

    /** The site siret. */
    SITE_SIRET(24),
    
    /** The site siren. */
    SITE_SIREN(25),
    
    /** The site finess. */
    SITE_FINESS(26),
    
    /** The legal establishment finess. */
    LEGAL_ESTABLISHMENT_FINESS(27),
    
    /** The structure technical id. */
    STRUCTURE_TECHNICAL_ID(28),
    
    /** The legal commercial name. */
    LEGAL_COMMERCIAL_NAME(29),
    
    /** The public commercial name. */
    PUBLIC_COMMERCIAL_NAME(30),
    
    /** The recipient additional info. */
    RECIPIENT_ADDITIONAL_INFO(31),
    
    /** The geo location additional info. */
    GEO_LOCATION_ADDITIONAL_INFO(32),
    
    /** The street number. */
    STREET_NUMBER(33),
    
    /** The street number repetition index. */
    STREET_NUMBER_REPETITION_INDEX(34),
    
    /** The street category code. */
    STREET_CATEGORY_CODE(35),
    
    /** The street label. */
    STREET_LABEL(36),
    
    /** The distribution mention. */
    DISTRIBUTION_MENTION(37),
    
    /** The cedex office. */
    CEDEX_OFFICE(38),
    
    /** The postal code. */
    POSTAL_CODE(39),
    
    /** The commune code. */
    COMMUNE_CODE(40),
    
    /** The country code. */
    COUNTRY_CODE(41),
    
    /** The structure phone. */
    STRUCTURE_PHONE(42),
    
    /** The structure phone 2. */
    STRUCTURE_PHONE_2(43),
    
    /** The structure fax. */
    STRUCTURE_FAX(44),
    
    /** The structure email. */
    STRUCTURE_EMAIL(45),
    
    /** The department code. */
    DEPARTMENT_CODE(46),
    
    /** The old structure id. */
    OLD_STRUCTURE_ID(47),
    
    /** The registration authority. */
    REGISTRATION_AUTHORITY(48),

    /**
     * The activity kind code
     */
    ACTIVITY_KIND_CODE(49);

    /** The column. */
    public int column;
    
    /**
     * Instantiates a new rass items.
     *
     * @param column the column
     */
    RassItems(int column) {
        this.column = column;
    }
}

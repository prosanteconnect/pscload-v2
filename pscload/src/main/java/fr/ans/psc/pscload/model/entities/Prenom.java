package fr.ans.psc.pscload.model.entities;

import fr.ans.psc.model.FirstName;

import java.util.ArrayList;
import java.util.List;

public class Prenom extends fr.ans.psc.model.FirstName {
  private static final int FIRST_NAME_COUNT = 3;
  String firstName;
  Integer order;

  public Prenom() {
  }

  public Prenom(String firstName, Integer order) {
    this.firstName = firstName;
    this.order = order;
  }

  public Prenom(String firstName) {
    this.firstName = firstName;
    this.order = 0;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  /**
   * Transforms a string of first names separated by an apostrophe (')
   * into a list of FirstName objects, in the same order as they appear in the string.
   * @param string a string of first names separated by an apostrophe (') and containing exactly FIRST_NAME_COUNT-1 apostrophes (')
   * @return list of FirstName objects
   */
  public static List<FirstName> stringToList(String string) {
    String[] firstNameStrings = string.split("'");
    List<FirstName> firstNames = new ArrayList<>(3);
    for (int i = 0; i < FIRST_NAME_COUNT; i++) {
      if(i < firstNameStrings.length) {
        firstNames.add(new FirstName(firstNameStrings[i], i));
      } else {
        firstNames.add(new FirstName("", i));
      }
    }
    return firstNames;
  }

  /**
   * Transforms a list of FirstName objects into a string of first names separated by an apostrophe ('),
   * and containing exactly FIRST_NAME_COUNT-1 apostrophes (') even if the list contains less than FIRST_NAME_COUNT names.
   * @param firstNames list of FirstName objects
   * @return a string of first names separated by an apostrophe (') and containing exactly FIRST_NAME_COUNT-1 apostrophes (')
   */
  public static String listToString(List<FirstName> firstNames) {
    StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i < FIRST_NAME_COUNT; i++) {
      if (i < firstNames.size()) stringBuilder.append(firstNames.get(i).getFirstName());
      stringBuilder.append("'");
    }
    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    return stringBuilder.toString();
  }
}

package nl.siegmann.epublib.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.util.StringUtil;

/**
 * Represents one of the authors of the book
 *
 * @author paul
 *
 */
public class CreatorContributor implements Serializable {

    private static final long serialVersionUID = 6663408501416574200L;

    private String firstname;
    private String lastname;
    private ArrayList<Relator> relators = new ArrayList<Relator>();

    public CreatorContributor(String singleName) {
        this("", singleName);
    }

    public CreatorContributor(String firstname, String lastname) {
        this(firstname, lastname, "author");
    }

    public CreatorContributor(String firstname, String lastname, String roleName) {
        this.firstname = firstname;
        this.lastname = lastname;
        
        addRoleByName(roleName);
    }

    public CreatorContributor(String firstname, String lastname, List<String> roleNames) {
        this.firstname = firstname;
        this.lastname = lastname;

        for (String roleName : roleNames) {
            addRoleByName(roleName);
        }
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String toString() {
        return lastname + ", " + firstname;
    }

    public int hashCode() {
        return StringUtil.hashCode(firstname, lastname);
    }

    public boolean equals(Object authorObject) {
        if (!(authorObject instanceof CreatorContributor)) {
            return false;
        }
        CreatorContributor other = (CreatorContributor) authorObject;
        return StringUtil.equals(firstname, other.firstname)
                && StringUtil.equals(lastname, other.lastname);
    }

    public Relator addRoleByCode(String code) {
        Relator result = Relator.byCode(code);

        if (result == null) {
            result = Relator.AUTHOR;
        }

        relators.add(result);

        return result;
    }

    public Relator addRoleByName(String name) {
        Relator result = Relator.byName(name);

        if (result == null) {
            result = Relator.AUTHOR;
        }

        relators.add(result);

        return result;
    }

    public List<Relator> getRelators() {
        return relators;
    }

    public void setRelators(ArrayList<Relator> relators) {
        this.relators = relators;
    }
}

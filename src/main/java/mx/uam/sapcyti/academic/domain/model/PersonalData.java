package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Identity data shared by Student and Professor aggregates (BC-02).
 */
@Embeddable
public class PersonalData {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "first_last_name", nullable = false, length = 100)
    private String firstLastName;

    @Column(name = "second_last_name", length = 100)
    private String secondLastName;

    @Column(length = 100)
    private String nationality;

    protected PersonalData() {
        // For JPA
    }

    public PersonalData(String firstName, String firstLastName, String secondLastName, String nationality) {
        this.firstName = firstName;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.nationality = nationality;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFirstLastName() {
        return firstLastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public String getNationality() {
        return nationality;
    }
}

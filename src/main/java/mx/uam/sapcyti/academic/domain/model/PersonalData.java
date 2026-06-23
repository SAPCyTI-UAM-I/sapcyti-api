package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * Identity and contact data shared by Student and Professor aggregates (BC-02).
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

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "phone_extension", length = 10)
    private String phoneExtension;

    protected PersonalData() {
        // For JPA
    }

    public PersonalData(
            String firstName,
            String firstLastName,
            String secondLastName,
            String nationality,
            LocalDate birthDate,
            String phone,
            String phoneExtension) {
        this.firstName = firstName;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.phone = phone;
        this.phoneExtension = phoneExtension;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhoneExtension() {
        return phoneExtension;
    }
}

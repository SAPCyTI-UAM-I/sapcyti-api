package mx.uam.sapcyti.exam.domain.model;

/**
 * Token inside the thesis file name (exam-requests.schema.json#/definitions/DegreeNameToken).
 */
public enum DegreeNameToken {
    Esp,
    Mtria,
    Docto;

    public static DegreeNameToken forDegree(Degree degree) {
        return switch (degree) {
            case ESPECIALIZACION -> Esp;
            case MAESTRIA -> Mtria;
            case DOCTORADO -> Docto;
        };
    }
}

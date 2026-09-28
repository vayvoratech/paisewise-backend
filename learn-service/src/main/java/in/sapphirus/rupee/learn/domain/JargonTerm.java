package in.sapphirus.rupee.learn.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "jargon_terms")
public class JargonTerm {

    @Id
    private String term; // natural key, e.g. "Fund Manager"

    @Column(length = 1000)
    private String definition;
    @Column(length = 1000)
    private String analogy;
    @Column(length = 1000)
    private String example;

    protected JargonTerm() {}

    public JargonTerm(String term, String definition, String analogy, String example) {
        this.term = term;
        this.definition = definition;
        this.analogy = analogy;
        this.example = example;
    }

    public String getTerm() { return term; }
    public String getDefinition() { return definition; }
    public String getAnalogy() { return analogy; }
    public String getExample() { return example; }
}

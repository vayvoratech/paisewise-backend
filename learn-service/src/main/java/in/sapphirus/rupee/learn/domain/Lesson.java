package in.sapphirus.rupee.learn.domain;

import jakarta.persistence.*;

/** A lesson. Body segments + jargon words are stored as JSON text for flexibility. */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    private String id; // e.g. "mf-3"

    private String chapter;
    private int chapterNo;
    private int index;
    private int total;
    private String title;
    private int quizXp;

    /** JSON array of segments (see seed). */
    @Column(length = 4000)
    private String segmentsJson;

    /** JSON array of jargon words appearing in the body. */
    @Column(length = 1000)
    private String jargonWordsJson;

    protected Lesson() {}

    public Lesson(String id, String chapter, int chapterNo, int index, int total, String title,
                  int quizXp, String segmentsJson, String jargonWordsJson) {
        this.id = id;
        this.chapter = chapter;
        this.chapterNo = chapterNo;
        this.index = index;
        this.total = total;
        this.title = title;
        this.quizXp = quizXp;
        this.segmentsJson = segmentsJson;
        this.jargonWordsJson = jargonWordsJson;
    }

    public String getId() { return id; }
    public String getChapter() { return chapter; }
    public int getChapterNo() { return chapterNo; }
    public int getIndex() { return index; }
    public int getTotal() { return total; }
    public String getTitle() { return title; }
    public int getQuizXp() { return quizXp; }
    public String getSegmentsJson() { return segmentsJson; }
    public String getJargonWordsJson() { return jargonWordsJson; }
}

package in.sapphirus.rupee.learn.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    private String id;

    @Column(length = 500)
    private String prompt;
    private int seconds;
    private int xp;
    private int orderNo;

    /** JSON array of options: [{key,text,correct}]. */
    @Column(length = 2000)
    private String optionsJson;

    @Column(length = 1000)
    private String explanation;

    protected QuizQuestion() {}

    public QuizQuestion(String id, String prompt, int seconds, int xp, int orderNo,
                        String optionsJson, String explanation) {
        this.id = id;
        this.prompt = prompt;
        this.seconds = seconds;
        this.xp = xp;
        this.orderNo = orderNo;
        this.optionsJson = optionsJson;
        this.explanation = explanation;
    }

    public String getId() { return id; }
    public String getPrompt() { return prompt; }
    public int getSeconds() { return seconds; }
    public int getXp() { return xp; }
    public int getOrderNo() { return orderNo; }
    public String getOptionsJson() { return optionsJson; }
    public String getExplanation() { return explanation; }
}

package edu.connexion3a8.entities.gamification;

import java.sql.Timestamp;
import java.util.Objects;

public class QuestionOption {
    private long id;
    private long questionId;
    private String optionText;
    private boolean isCorrect;
    private int optionOrder;
    private Timestamp createdAt;

    public QuestionOption() {
    }

    public QuestionOption(long questionId, String optionText, boolean isCorrect, int optionOrder) {
        this.questionId = questionId;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
        this.optionOrder = optionOrder;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(long questionId) {
        this.questionId = questionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public int getOptionOrder() {
        return optionOrder;
    }

    public void setOptionOrder(int optionOrder) {
        this.optionOrder = optionOrder;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QuestionOption that = (QuestionOption) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "QuestionOption{" +
                "id=" + id +
                ", questionId=" + questionId +
                ", optionText='" + optionText + '\'' +
                ", isCorrect=" + isCorrect +
                ", optionOrder=" + optionOrder +
                '}';
    }
}

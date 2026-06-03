package com.edunac.mentora.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QuestionForm {

    private Integer id;
    private String content;
    private String difficulty;
    private String questionType;
    private BigDecimal score;
    private List<String> optionContents = new ArrayList<>();
    private Integer correctOptionIndex;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public List<String> getOptionContents() {
        return optionContents;
    }

    public void setOptionContents(List<String> optionContents) {
        this.optionContents = optionContents;
    }

    public Integer getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(Integer correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }
}

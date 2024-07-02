package com.microel.trackerbackend.storage.entities.users;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;
    private String userLogin;
    @Column(columnDefinition = "text")
    private String text;
    @Nullable
    private Float rating;
    private Timestamp timestamp;
    private String source;

    public static Review of(String userLogin, String text, String source) {
        Review review = new Review();
        review.setUserLogin(userLogin);
        review.setText(text);
        review.setTimestamp(Timestamp.from(Instant.now()));
        review.setSource(source);
        return review;
    }
}

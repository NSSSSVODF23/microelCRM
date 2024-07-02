package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.storage.dispatchers.ReviewDispatcher;
import com.microel.trackerbackend.storage.entities.users.Review;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("api/private/review")
public class ReviewRequestController {
    private final ReviewDispatcher reviewDispatcher;

    public ReviewRequestController(ReviewDispatcher reviewDispatcher) {
        this.reviewDispatcher = reviewDispatcher;
    }

    @GetMapping("{login}")
    public ResponseEntity<List<Review>> getReviews(@PathVariable String login){
        return ResponseEntity.ok(reviewDispatcher.getReviews(login));
    }
}

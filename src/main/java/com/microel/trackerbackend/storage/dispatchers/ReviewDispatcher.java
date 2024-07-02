package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.TimeFrame;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.storage.entities.users.Review;
import com.microel.trackerbackend.storage.repositories.ReviewRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class ReviewDispatcher {
    private final ReviewRepository reviewRepository;

    public ReviewDispatcher(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> getTodayReviews(String userLogin){
        DateRange dateRange = DateRange.of(TimeFrame.TODAY);
        return reviewRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userLogin"), userLogin),
                cb.between(root.get("timestamp"), dateRange.start(), dateRange.end())
        ));
    }

    @Transactional
    public Review createReview(String userLogin, String text, String source){
        Review review = Review.of(
                userLogin,
                text,
                source
        );
        return reviewRepository.save(review);
    }

    public List<Review> getReviews(String login) {
        return reviewRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userLogin"), login)
        ), Sort.by(Sort.Direction.DESC, "timestamp"));
    }
}

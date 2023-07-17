package com.microel.trackerbackend.storage.entities.salary;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paid_action_templates")
public class PaidActionTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paidActionTemplateId;
    @ManyToOne()
    @JoinColumn(name = "f_action_id")
    private PaidAction action;
    private Float count;

    @Getter
    @Setter
    public static class Form {
        private Long actionId;
        private Float count;

        public boolean isValid() {
            return actionId != null && count != null && count > 0.0f;
        }
    }
}

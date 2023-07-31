package com.microel.trackerbackend.storage.entities.salary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.AbstractForm;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paid_works")
public class PaidWork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paidWorkId;
    @Column(length = 256)
    private String name;
    @Column(columnDefinition = "text default ''")
    private String description;
    @JoinColumn(columnDefinition = "integer default 0")
    private Integer position;
    @ManyToOne
    @JoinColumn(name = "f_parent_group_id")
    @JsonIgnore
    private PaidWorkGroup parentGroup;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "f_paid_work_id")
    @BatchSize(size = 25)
    private List<PaidActionTemplate> actions;

    public List<Long> getPath() {
        List<Long> path = new ArrayList<>();
        PaidWorkGroup parent = parentGroup;
        while (parent != null) {
            path.add(parent.getPaidWorkGroupId());
            parent = parent.getParentGroup();
        }
        Collections.reverse(path);
        return path;
    }

    @Getter
    @Setter
    public static class Form implements AbstractForm {
        private String name;
        @Nullable
        private String description;
        @Nullable
        private Long parentGroupId;
        private List<PaidActionTemplate.Form> actions;

        public boolean isValid() {
            return name != null && !name.isBlank() && actions != null && actions.size() > 0;
        }
    }
}

package com.microel.trackerbackend.storage.entities.salary;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "paid_work_groups")
public class PaidWorkGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paidWorkGroupId;
    @Column(length = 256)
    private String name;
    @Column(columnDefinition = "text default ''")
    private String description;
    private boolean isRoot;
    @JsonIgnore
    @OneToMany(mappedBy = "parentGroup")
    @BatchSize(size = 25)
    private List<PaidWorkGroup> childrenGroups;
    @JsonIgnore
    @OneToMany(mappedBy = "parentGroup")
    @BatchSize(size = 25)
    private List<PaidWork> paidWorks;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "f_parent_group_id")
    @Nullable
    private PaidWorkGroup parentGroup;
    @JoinColumn(columnDefinition = "integer default 0")
    private Integer position;

    public List<PaidWork> setPaidWorks(List<PaidWork> paidWorks){
        this.paidWorks = paidWorks;
        this.paidWorks.forEach(paidWork -> paidWork.setParentGroup(this));
        return paidWorks;
    }

    public void clearPaidWorks() {
        this.paidWorks.forEach(paidWork -> paidWork.setParentGroup(null));
        this.paidWorks = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Form {
        private String name;
        @Nullable
        private String description;
        @Nullable
        private Long parentGroupId;

        public boolean isValid(){
            return name != null && !name.isBlank();
        }
    }

    public List<Long> getPath(){
        List<Long> path = new ArrayList<>();
        PaidWorkGroup parent = parentGroup;
        while(parent != null){
            path.add(parent.paidWorkGroupId);
            parent = parent.parentGroup;
        }
        Collections.reverse(path);
        return path;
    }
}

package com.microel.trackerbackend.storage.entities.salary;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Класс объекта бд, описывающий какие-либо платные действия совершаемые монтажниками на задаче
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paid_actions")
public class PaidAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paidActionId;
    private UUID identifier;
    @Column(length = 256)
    private String name;
    @Column(columnDefinition = "text default ''")
    private String description;
    private Timestamp created;
    @ManyToOne
    @JoinColumn(name = "f_creator_employee")
    @BatchSize(size = 25)
    private Employee creator;
    @Column(columnDefinition = "boolean default false")
    private Boolean edited;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @Enumerated(EnumType.STRING)
    private Unit unit;
    private Float cost;

    @ManyToMany
    @BatchSize(size = 25)
    private List<PaidWork> paidWorks;


    public enum Unit{
        AMOUNT("AMOUNT"),
        METRES("METRES"),
        KILOGRAMS("KILOGRAMS");

        private String unit;
        Unit(String unit){
            this.unit = unit;
        }
    }

    @Getter
    @Setter
    public static class Form{
        private String name;
        private String description;
        private Unit unit;
        private Float cost;

        // Проверка на заполненность
        public boolean fullFilled(){
            return (name != null && !name.isBlank()) && unit != null && (cost != null && cost > 0);
        }

        // Проверка всех полей на совпадение с PaidAction объектом
        public boolean fullEqual(PaidAction paidAction){
            return this.name.equals(paidAction.name) && this.description.equals(paidAction.description) && this.unit.equals(paidAction.unit) && this.cost.equals(paidAction.cost);
        }
    }

    @Getter
    @Setter
    public static class Filter{
        @Nullable
        private String nameQuery;
        @Nullable
        private Boolean includeDeleted;
    }
}

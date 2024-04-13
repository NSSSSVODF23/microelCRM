package com.microel.trackerbackend.storage.entities.tariff;

import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "auto_tariffs")
public class AutoTariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long autoTariffId;
    private Integer externalId;
    private String name;
    private Float cost;
    private String description;
    private Boolean isService;
    @ManyToOne()
    @JoinColumn(name = "f_target_class_id")
    private Wireframe targetClass;
    @ManyToOne()
    @JoinColumn(name = "f_target_type_id")
    private TaskStage targetType;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_created_id")
    private EmployeeIntervention created;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_updated_id")
    private EmployeeIntervention updated;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_deleted_id")
    private EmployeeIntervention deleted;
    private Boolean isDeleted;

    public AutoTariff update(Form form, Wireframe targetClass, TaskStage targetType, Employee updater){
        this.setExternalId(form.getExternalId());
        this.setName(form.getName());
        this.setCost(form.getCost());
        this.setDescription(form.getDescription());
        this.setTargetClass(targetClass);
        this.setTargetType(targetType);
        this.setUpdated(EmployeeIntervention.from(updater));
        return this;
    }

    public AutoTariff delete(Employee deleter){
        this.setDeleted(EmployeeIntervention.from(deleter));
        this.setIsDeleted(true);
        return this;
    }

    public static AutoTariff of(Form form, Wireframe targetClass, TaskStage targetType, Employee creator){
        AutoTariff autoTariff = new AutoTariff();
        autoTariff.setExternalId(form.getExternalId());
        autoTariff.setName(form.getName());
        autoTariff.setCost(form.getCost());
        autoTariff.setDescription(form.getDescription());
        autoTariff.setIsService(form.getIsService());
        autoTariff.setTargetClass(targetClass);
        autoTariff.setTargetType(targetType);
        autoTariff.setCreated(EmployeeIntervention.from(creator));
        autoTariff.setUpdated(EmployeeIntervention.from(creator));
        autoTariff.setIsDeleted(false);
        return autoTariff;
    }

    @Data
    public static class Form {
        private Integer externalId;
        private String name;
        private Float cost;
        private String description;
        private Boolean isService;
        private Long targetClassId;
        private String targetType;
    }

}

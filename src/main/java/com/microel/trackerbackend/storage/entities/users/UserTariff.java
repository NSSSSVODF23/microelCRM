package com.microel.trackerbackend.storage.entities.users;

import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "user_tariff")
public class UserTariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTariffId;
    private String baseName;
    private String baseId;
    private String name;
    private Boolean isService;
    private Float price;
    private Integer paymentPeriod; // in months
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "created_by_employee_intervention_id")
    private EmployeeIntervention createdBy;
    @OneToOne(cascade  =  {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name  = "edited_by_employee_intervention_id")
    @Nullable
    private EmployeeIntervention editedBy;
    private Boolean deleted;

    public String getPriceLabel(){
        return String.format("%s руб./%s", getPrice(), getPaymentPeriod() == 1? "мес." : getPaymentPeriod() + " мес.");
    }

    public static UserTariff of(Form form, Employee creator){
        final UserTariff userTariff = new UserTariff();
        userTariff.setBaseName(form.getBaseName());
        userTariff.setBaseId(form.getBaseId());
        userTariff.setName(form.getName());
        userTariff.setIsService(form.getIsService());
        userTariff.setPrice(form.getPrice());
        userTariff.setPaymentPeriod(form.getPaymentPeriod());
        userTariff.setCreatedBy(EmployeeIntervention.from(creator));
        userTariff.setDeleted(false);
        return userTariff;
    }

    public void update(Form form, Employee employee) {
        this.setName(form.getName());
        this.setIsService(form.getIsService());
        this.setPrice(form.getPrice());
        this.setPaymentPeriod(form.getPaymentPeriod());
        this.setEditedBy(EmployeeIntervention.from(employee));
    }

    public void delete(Employee employee) {
        this.setDeleted(true);
        this.setEditedBy(EmployeeIntervention.from(employee));
    }

    @Data
    public static class Form {
        private String baseName;
        private String baseId;
        private String name;
        private Boolean isService;
        private Float price;
        private Integer paymentPeriod;
    }
}

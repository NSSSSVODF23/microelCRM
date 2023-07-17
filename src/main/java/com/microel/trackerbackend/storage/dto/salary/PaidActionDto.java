package com.microel.trackerbackend.storage.dto.salary;

import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PaidActionDto {
    private Long paidActionId;
    private UUID identifier;
    private String name;
    private String description;
    private Timestamp created;
    private EmployeeDto creator;
    private Boolean edited;
    private Boolean deleted;
    private PaidAction.Unit unit;
    private Float cost;
}

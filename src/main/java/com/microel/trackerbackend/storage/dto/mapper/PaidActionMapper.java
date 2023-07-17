package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.salary.PaidActionDto;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import org.springframework.lang.Nullable;

public class PaidActionMapper {
    @Nullable
    public static PaidActionDto toDto(@Nullable PaidAction paidAction){
        if(paidAction == null) return null;
        return PaidActionDto.builder()
                .paidActionId(paidAction.getPaidActionId())
                .identifier(paidAction.getIdentifier())
                .name(paidAction.getName())
                .description(paidAction.getDescription())
                .created(paidAction.getCreated())
                .creator(EmployeeMapper.toDto(paidAction.getCreator()))
                .edited(paidAction.getEdited())
                .deleted(paidAction.getDeleted())
                .unit(paidAction.getUnit())
                .cost(paidAction.getCost())
                .build();
    }

    @Nullable
    public static PaidAction fromDto(@Nullable PaidActionDto paidAction) {
        if(paidAction == null) return null;
        return PaidAction.builder()
                .paidActionId(paidAction.getPaidActionId())
                .identifier(paidAction.getIdentifier())
                .name(paidAction.getName())
                .description(paidAction.getDescription())
                .created(paidAction.getCreated())
                .creator(EmployeeMapper.fromDto(paidAction.getCreator()))
                .edited(paidAction.getEdited())
                .deleted(paidAction.getDeleted())
                .unit(paidAction.getUnit())
                .cost(paidAction.getCost())
                .build();
    }
}

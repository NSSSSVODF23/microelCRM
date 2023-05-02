package com.microel.trackerbackend.modules.transport;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ChangeTaskObserversDTO {
    private Set<Long> departmentObservers;
    private Set<String> personalObservers;
}

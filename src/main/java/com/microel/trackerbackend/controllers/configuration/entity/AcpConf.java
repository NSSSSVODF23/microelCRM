package com.microel.trackerbackend.controllers.configuration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.controllers.configuration.AbstractConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcpConf implements AbstractConfiguration {

    private String acpFlexConnectorEndpoint;

    @Override
    @JsonIgnore
    public Boolean isFilled() {
        if (acpFlexConnectorEndpoint == null || acpFlexConnectorEndpoint.isBlank()) return false;
        return true;
    }

    @Override
    public String toString() {
        return "AcpConf{" +
                "acpFlexConnectorEndpoint='" + acpFlexConnectorEndpoint + '\'' +
                '}';
    }
}

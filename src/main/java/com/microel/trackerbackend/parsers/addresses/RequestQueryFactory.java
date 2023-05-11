package com.microel.trackerbackend.parsers.addresses;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.lang.Nullable;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

@Getter
@Setter
public class RequestQueryFactory {
    private final String streetsGetterURI = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/streets";
    private final String housesGetterURI = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/numbers";
    private final Boolean actual = true;
    private final Integer itemsPerPage = 50;
    private LocalityIdentifier localityIdentifier;
    private Integer page;

    public RequestQueryFactory(LocalityIdentifier localityIdentifier) {
        this.localityIdentifier = localityIdentifier;
    }

    @Nullable
    public URL createStreetsGetterUrl(Integer page) {
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(streetsGetterURI);

            final String areaCode = localityIdentifier.getAreaCode();
            final String cityCode = localityIdentifier.getCityCode();
            final String regionCode = localityIdentifier.getRegionCode();
            final String settlementCode = localityIdentifier.getSettlementCode();

            uriBuilder.addParameter("actual", actual.toString());
            uriBuilder.addParameter("itemsPerPage", itemsPerPage.toString());
            uriBuilder.addParameter("page", page.toString());

            if (areaCode != null) {
                uriBuilder.addParameter("areaCode", areaCode);
            }
            if (cityCode != null) {
                uriBuilder.addParameter("cityCode", cityCode);
            }
            if (settlementCode != null) {
                uriBuilder.addParameter("settlementCode", settlementCode);
            }
            if (regionCode != null) {
                uriBuilder.addParameter("regionCode", regionCode);
            }

            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException ignored) {
            return null;
        }
    }

    public URL createHousesGetterUrl(Integer currentPage, String streetCode) {
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(housesGetterURI);

            final String areaCode = localityIdentifier.getAreaCode();
            final String cityCode = localityIdentifier.getCityCode();
            final String regionCode = localityIdentifier.getRegionCode();
            final String settlementCode = localityIdentifier.getSettlementCode();

            uriBuilder.addParameter("actual", actual.toString());
            uriBuilder.addParameter("itemsPerPage", itemsPerPage.toString());
            uriBuilder.addParameter("page", currentPage.toString());
            uriBuilder.addParameter("streetCode", streetCode);


            if (areaCode != null) {
                uriBuilder.addParameter("areaCode", areaCode);
            }
            if (cityCode != null) {
                uriBuilder.addParameter("cityCode", cityCode);
            }
            if (settlementCode != null) {
                uriBuilder.addParameter("settlementCode", settlementCode);
            }
            if (regionCode != null) {
                uriBuilder.addParameter("regionCode", regionCode);
            }

            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException ignored) {
            return null;
        }
    }
}

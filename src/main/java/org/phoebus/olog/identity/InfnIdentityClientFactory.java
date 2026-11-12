package org.phoebus.olog.identity;

import org.phoebus.olog.identity.api.IdentityApi;
import org.phoebus.olog.identity.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfnIdentityClientFactory {

    private final String baseUrl;

    public InfnIdentityClientFactory(
            @Value("${infn.identity.base-url:https://api.app.infn.it}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public IdentityApi create(String bearerToken) {
        ApiClient client = new ApiClient().setBasePath(baseUrl);
        client.setBearerToken(bearerToken);
        return new IdentityApi(client);
    }
}

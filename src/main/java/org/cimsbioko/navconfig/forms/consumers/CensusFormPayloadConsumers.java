package org.cimsbioko.navconfig.forms.consumers;

import org.cimsbioko.model.core.Individual;
import org.cimsbioko.model.core.Location;
import org.cimsbioko.navconfig.ProjectFormFields;
import org.cimsbioko.navconfig.forms.LaunchContext;
import org.cimsbioko.navconfig.forms.UsedByJSConfig;
import org.cimsbioko.navconfig.forms.adapters.IndividualFormAdapter;
import org.cimsbioko.navconfig.forms.adapters.LocationFormAdapter;
import org.cimsbioko.repository.DataWrapper;
import org.cimsbioko.repository.GatewayRegistry;
import org.cimsbioko.repository.gateway.IndividualGateway;
import org.cimsbioko.repository.gateway.LocationGateway;

import java.util.Map;

import static org.cimsbioko.navconfig.BiokoHierarchy.HOUSEHOLD;

public class CensusFormPayloadConsumers {

    private static Location insertOrUpdateLocation(Map<String, String> formPayload) {
        Location location = LocationFormAdapter.fromForm(formPayload);
        GatewayRegistry.getLocationGateway().insertOrUpdate(location);
        return location;
    }

    private static Individual insertOrUpdateIndividual(Map<String, String> formPayLoad) {
        Individual individual = IndividualFormAdapter.fromForm(formPayLoad);
        IndividualGateway individualGateway = GatewayRegistry.getIndividualGateway();
        individualGateway.insertOrUpdate(individual);
        return individual;
    }

    @UsedByJSConfig
    public static class AddLocation implements FormPayloadConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {
            insertOrUpdateLocation(formPayload);
            return new ConsumerResult(true);
        }

        @Override
        public void augmentInstancePayload(Map<String, String> formPayload) {
            formPayload.put(ProjectFormFields.General.ENTITY_EXTID, formPayload.get(ProjectFormFields.Locations.LOCATION_EXTID));
        }
    }

    @UsedByJSConfig
    public static class AddMemberOfHousehold extends DefaultConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {
            insertOrUpdateIndividual(formPayload);
            return super.consumeFormPayload(formPayload, ctx);
        }
    }

    @UsedByJSConfig
    public static class AddHeadOfHousehold extends DefaultConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {

            LocationGateway locationGateway = GatewayRegistry.getLocationGateway();

            Individual individual = insertOrUpdateIndividual(formPayload);

            // Update the name of the location with the head's last name
            DataWrapper selectedLocation = ctx.getHierarchyPath().get(HOUSEHOLD);
            Location location = locationGateway.getFirst(locationGateway.findById(selectedLocation.getUuid()));
            String locationName = individual.getLastName();
            location.setName(locationName);
            selectedLocation.setName(locationName);
            locationGateway.insertOrUpdate(location);

            return new ConsumerResult(true);
        }

        @Override
        public void augmentInstancePayload(Map<String, String> formPayload) {
            // head of the household is always "self" to the head of household
            formPayload.put(ProjectFormFields.Individuals.RELATIONSHIP_TO_HEAD, "1");
        }
    }
}

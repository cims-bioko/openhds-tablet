package org.openhds.mobile.navconfig.forms.consumers;

import android.content.ContentResolver;

import org.openhds.mobile.model.core.Individual;
import org.openhds.mobile.model.core.Location;
import org.openhds.mobile.model.core.LocationHierarchy;
import org.openhds.mobile.model.core.Membership;
import org.openhds.mobile.model.core.Relationship;
import org.openhds.mobile.model.core.SocialGroup;
import org.openhds.mobile.model.form.FormBehavior;
import org.openhds.mobile.model.update.Visit;
import org.openhds.mobile.navconfig.forms.LaunchContext;
import org.openhds.mobile.navconfig.forms.adapters.IndividualFormAdapter;
import org.openhds.mobile.navconfig.forms.adapters.LocationFormAdapter;
import org.openhds.mobile.navconfig.forms.adapters.VisitFormAdapter;
import org.openhds.mobile.navconfig.ProjectFormFields;
import org.openhds.mobile.navconfig.ProjectResources;
import org.openhds.mobile.repository.DataWrapper;
import org.openhds.mobile.repository.GatewayRegistry;
import org.openhds.mobile.repository.gateway.IndividualGateway;
import org.openhds.mobile.repository.gateway.LocationGateway;
import org.openhds.mobile.repository.gateway.LocationHierarchyGateway;
import org.openhds.mobile.repository.gateway.MembershipGateway;
import org.openhds.mobile.repository.gateway.RelationshipGateway;
import org.openhds.mobile.repository.gateway.SocialGroupGateway;
import org.openhds.mobile.repository.gateway.VisitGateway;
import org.openhds.mobile.utilities.IdHelper;

import java.util.HashMap;
import java.util.Map;

import static org.openhds.mobile.navconfig.BiokoHierarchy.HOUSEHOLD_STATE;
import static org.openhds.mobile.navconfig.BiokoHierarchy.SECTOR_STATE;

public class CensusFormPayloadConsumers {

    private static void ensureLocationSectorExists(Map<String, String> formPayload, ContentResolver contentResolver) {

        LocationHierarchyGateway locationHierarchyGateway = GatewayRegistry.getLocationHierarchyGateway();
        LocationHierarchy mapArea = locationHierarchyGateway.getFirst(contentResolver,
                locationHierarchyGateway.findById(formPayload.get(ProjectFormFields.Locations.HIERARCHY_PARENT_UUID)));
        String sectorName =  formPayload.get(ProjectFormFields.Locations.SECTOR_NAME);
        String sectorExtId = mapArea.getExtId().replaceFirst("^(M\\d+)\\b", "$1\\" + sectorName);
        LocationHierarchy sector = locationHierarchyGateway.getFirst(contentResolver,
                locationHierarchyGateway.findByExtId(sectorExtId));

        if(sector == null){
            sector = new LocationHierarchy();
            sector.setUuid(IdHelper.generateEntityUuid());
            sector.setParentUuid(mapArea.getUuid());
            sector.setExtId(sectorExtId);
            sector.setName(sectorName);
            sector.setLevel(SECTOR_STATE);
            locationHierarchyGateway.insertOrUpdate(contentResolver,sector);

            // Modify the payload to refer to the created sector
            formPayload.put(ProjectFormFields.General.NEEDS_REVIEW, ProjectResources.General.FORM_NEEDS_REVIEW);
            formPayload.put(ProjectFormFields.Locations.HIERARCHY_UUID, sector.getUuid());
            formPayload.put(ProjectFormFields.Locations.HIERARCHY_PARENT_UUID, sector.getParentUuid());
            formPayload.put(ProjectFormFields.Locations.HIERARCHY_EXTID, sector.getExtId());
        }
    }

    private static Location insertOrUpdateLocation(Map<String, String> formPayload, ContentResolver contentResolver) {
        Location location = LocationFormAdapter.fromForm(formPayload);
        GatewayRegistry.getLocationGateway().insertOrUpdate(contentResolver, location);
        return location;
    }

    private static Individual insertOrUpdateIndividual(Map<String, String> formPayLoad, ContentResolver contentResolver) {
        Individual individual = IndividualFormAdapter.fromForm(formPayLoad);
        individual.setEndType(ProjectResources.Individual.RESIDENCY_END_TYPE_NA);
        IndividualGateway individualGateway = GatewayRegistry.getIndividualGateway();
        individualGateway.insertOrUpdate(contentResolver, individual);
        return individual;
    }

    public static class AddLocation implements FormPayloadConsumer {

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {
            ContentResolver contentResolver = ctx.getContentResolver();
            ensureLocationSectorExists(formPayload, contentResolver);
            insertOrUpdateLocation(formPayload, contentResolver);
            return new ConsumerResult(true, null, null);
        }

        @Override
        public void augmentInstancePayload(Map<String, String> formPayload) {
            formPayload.put(ProjectFormFields.General.ENTITY_EXTID, formPayload.get(ProjectFormFields.Locations.LOCATION_EXTID));
        }
    }

    public static class EvaluateLocation extends DefaultConsumer {
        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {

            LocationGateway locationGateway = GatewayRegistry.getLocationGateway();
            Location location = locationGateway.getFirst(ctx.getContentResolver(),
                    locationGateway.findById(ctx.getCurrentSelection().getUuid()));

            location.setLocationEvaluationStatus(formPayload.get(ProjectFormFields.Locations.EVALUATION));

            locationGateway.insertOrUpdate(ctx.getContentResolver(), location);

            return super.consumeFormPayload(formPayload, ctx);
        }
    }

    public static class AddMemberOfHousehold extends PregnancyPayloadConsumer {

        public AddMemberOfHousehold(FormBehavior followUp) {
            super(followUp);
        }

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {

            DataWrapper selectedLocation = ctx.getHierarchyPath().get(HOUSEHOLD_STATE);

            String relationshipType = formPayload
                    .get(ProjectFormFields.Individuals.RELATIONSHIP_TO_HEAD);
            Individual individual = insertOrUpdateIndividual(formPayload, ctx.getContentResolver());
            String startDate = formPayload
                    .get(ProjectFormFields.General.COLLECTION_DATE_TIME);

            SocialGroupGateway socialGroupGateway = GatewayRegistry.getSocialGroupGateway();
            IndividualGateway individualGateway = GatewayRegistry.getIndividualGateway();
            ContentResolver contentResolver = ctx.getContentResolver();

            // get head of household by household id
            SocialGroup socialGroup = socialGroupGateway.getFirst(contentResolver,
                    socialGroupGateway.findByLocationUuid(selectedLocation.getUuid()));


            Individual currentHeadOfHousehold = individualGateway.getFirst(contentResolver,
                    individualGateway.findById(socialGroup.getGroupHeadUuid()));

            // INSERT or UPDATE RELATIONSHIP
            RelationshipGateway relationshipGateway = GatewayRegistry.getRelationshipGateway();
            Relationship relationship = new Relationship(individual, currentHeadOfHousehold, relationshipType, startDate, formPayload.get(ProjectFormFields.Individuals.RELATIONSHIP_UUID));
            relationshipGateway.insertOrUpdate(contentResolver, relationship);

            // INSERT or UPDATE MEMBERSHIP
            MembershipGateway membershipGateway = GatewayRegistry.getMembershipGateway();
            Membership membership = new Membership(individual, socialGroup, relationshipType, formPayload.get(ProjectFormFields.Individuals.MEMBERSHIP_UUID));
            membershipGateway.insertOrUpdate(contentResolver, membership);

            if(containsPregnancy(formPayload)){
                return getPregnancyResult(formPayload);
            }

            return super.consumeFormPayload(formPayload, ctx);
        }
    }

    public static class AddHeadOfHousehold extends PregnancyPayloadConsumer {

        public AddHeadOfHousehold(FormBehavior followUp) {
            super(followUp);
        }

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {

            DataWrapper selectedLocation = ctx.getHierarchyPath().get(HOUSEHOLD_STATE);

            // head of the household is always "self" to the head of household
            String relationshipType = "1";

            // Pull out useful strings from the formPayload
            String startDate = formPayload.get(ProjectFormFields.General.COLLECTION_DATE_TIME);
            Individual individual = insertOrUpdateIndividual(formPayload, ctx.getContentResolver());

            LocationGateway locationGateway = GatewayRegistry.getLocationGateway();
            ContentResolver contentResolver = ctx.getContentResolver();

            // Update the name of the location
            Location location = locationGateway.getFirst(contentResolver,
                    locationGateway.findById(selectedLocation.getUuid()));
            String locationName = individual.getLastName();
            location.setName(locationName);
            selectedLocation.setName(locationName);
            locationGateway.insertOrUpdate(contentResolver, location);

            // create social group
            SocialGroupGateway socialGroupGateway = GatewayRegistry.getSocialGroupGateway();
            SocialGroup socialGroup = new SocialGroup(selectedLocation.getUuid(), selectedLocation.getExtId(), individual, formPayload.get(ProjectFormFields.Individuals.SOCIALGROUP_UUID));
            socialGroupGateway.insertOrUpdate(contentResolver, socialGroup);

            // create membership
            MembershipGateway membershipGateway = GatewayRegistry.getMembershipGateway();
            Membership membership = new Membership(individual, socialGroup, relationshipType, formPayload.get(ProjectFormFields.Individuals.MEMBERSHIP_UUID));
            membershipGateway.insertOrUpdate(contentResolver, membership);

            // Set head of household's relationship to himself.
            RelationshipGateway relationshipGateway = GatewayRegistry.getRelationshipGateway();
            Relationship relationship = new Relationship(individual, individual, relationshipType, startDate, formPayload.get(ProjectFormFields.Individuals.RELATIONSHIP_UUID));
            relationshipGateway.insertOrUpdate(contentResolver, relationship);

            if(containsPregnancy(formPayload)) {
                return getPregnancyResult(formPayload);
            }
            return new ConsumerResult(true, null, null);
        }

        @Override
        public void augmentInstancePayload(Map<String, String> formPayload) {
            // head of the household is always "self" to the head of household
            formPayload.put(ProjectFormFields.Individuals.MEMBER_STATUS, "1");
        }

    }

    public abstract static class PregnancyPayloadConsumer extends DefaultConsumer {

        private FormBehavior followUp;

        public PregnancyPayloadConsumer(FormBehavior followUp) {
            this.followUp = followUp;
        }

        protected boolean containsPregnancy(Map<String, String> formPayload) {
            return "Yes".equals(formPayload.get(ProjectFormFields.Individuals.IS_PREGNANT_FLAG));
        }

        protected ConsumerResult getPregnancyResult(Map<String, String> payload) {
            return new ConsumerResult(false, followUp, getPregnancyHints(payload));
        }

        private Map<String, String> getPregnancyHints(Map<String, String> payload) {
            Map<String, String> hints = new HashMap<>();
            hints.put(ProjectFormFields.Individuals.INDIVIDUAL_EXTID, payload.get(ProjectFormFields.Individuals.INDIVIDUAL_EXTID));
            hints.put(ProjectFormFields.Individuals.INDIVIDUAL_UUID, payload.get(ProjectFormFields.General.ENTITY_UUID));
            hints.put(ProjectFormFields.Locations.LOCATION_EXTID, payload.get(ProjectFormFields.General.HOUSEHOLD_STATE_FIELD_NAME));
            return hints;
        }
    }

    // Used for Form Launch Sequences
    public static class ChainedVisitForPregnancyObservation extends DefaultConsumer {

        private FormBehavior followUp;

        public ChainedVisitForPregnancyObservation(FormBehavior followUp) {
            this.followUp = followUp;
        }

        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {

            Visit visit = VisitFormAdapter.fromForm(formPayload);
            VisitGateway visitGateway = GatewayRegistry.getVisitGateway();
            ContentResolver contentResolver = ctx.getContentResolver();
            visitGateway.insertOrUpdate(contentResolver, visit);

            ctx.startVisit(visit);

            ctx.getConsumerResult().getFollowUpHints().put(ProjectFormFields.General.ENTITY_UUID, formPayload.get(ProjectFormFields.Individuals.INDIVIDUAL_UUID));
            ctx.getConsumerResult().getFollowUpHints().put(ProjectFormFields.General.ENTITY_EXTID, formPayload.get(ProjectFormFields.Individuals.INDIVIDUAL_EXTID));

            return new ConsumerResult(false, followUp, ctx.getConsumerResult().getFollowUpHints());
        }
    }

    public static class ChainedPregnancyObservation extends DefaultConsumer {
        @Override
        public ConsumerResult consumeFormPayload(Map<String, String> formPayload, LaunchContext ctx) {
            ctx.finishVisit();
            return super.consumeFormPayload(formPayload, ctx);
        }
    }
}
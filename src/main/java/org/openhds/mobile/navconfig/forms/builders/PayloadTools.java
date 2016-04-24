package org.openhds.mobile.navconfig.forms.builders;

import org.openhds.mobile.navconfig.forms.LaunchContext;
import org.openhds.mobile.repository.DataWrapper;
import org.openhds.mobile.model.core.FieldWorker;
import org.openhds.mobile.navconfig.ProjectFormFields;
import org.openhds.mobile.navconfig.ProjectResources;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class PayloadTools {

    public static void flagForReview(Map<String, String> formPayload, boolean shouldReview) {
        if (shouldReview) {
            formPayload.put(ProjectFormFields.General.NEEDS_REVIEW, ProjectResources.General.FORM_NEEDS_REVIEW);
        } else {
            formPayload.put(ProjectFormFields.General.NEEDS_REVIEW, ProjectResources.General.FORM_NO_REVIEW_NEEDED);
        }
    }

    public static void addMinimalFormPayload(Map<String, String> formPayload, LaunchContext navigateActivity) {

        List<String> stateSequence = navigateActivity.getStateSequence();
        Map<String, DataWrapper> hierarchyPath = navigateActivity.getHierarchyPath();

        //TODO: Add all the hierarchy Uuids as well?
        // Add all the extIds from the HierarchyPath
        for (String state : stateSequence) {
            if (null != hierarchyPath.get(state)) {
                String fieldName = ProjectFormFields.General.getExtIdFieldNameFromState(state);
                formPayload.put(fieldName, hierarchyPath.get(state).getExtId());
            }
        }

        // add the FieldWorker's extId
        FieldWorker fieldWorker = navigateActivity.getCurrentFieldWorker();
        formPayload.put(ProjectFormFields.General.FIELD_WORKER_EXTID, fieldWorker.getExtId());
        formPayload.put(ProjectFormFields.General.FIELD_WORKER_UUID, fieldWorker.getUuid());

        // add collected DateTime
        formPayload.put(ProjectFormFields.General.COLLECTION_DATE_TIME, formatTime(Calendar.getInstance()));
    }

    public static String formatTime(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
    }

    public static String formatDate(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    public static String formatBuilding(int building, boolean includePrefix) {
        return String.format("%s%03d", includePrefix? "E" : "", building);
    }

    public static String formatFloor(int floor, boolean includePrefix) {
        return String.format("%s%02d", includePrefix? "P" : "", floor);
    }
}
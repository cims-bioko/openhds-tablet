package org.openhds.mobile.repository.gateway;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import org.openhds.mobile.OpenHDS;
import org.openhds.mobile.model.SocialGroup;
import org.openhds.mobile.repository.Converter;
import org.openhds.mobile.repository.DataWrapper;

import static org.openhds.mobile.OpenHDS.SocialGroups.COLUMN_SOCIAL_GROUP_EXTID;
import static org.openhds.mobile.OpenHDS.SocialGroups.COLUMN_SOCIAL_GROUP_HEAD_INDIVIDUAL_EXTID;
import static org.openhds.mobile.OpenHDS.SocialGroups.COLUMN_SOCIAL_GROUP_NAME;
import static org.openhds.mobile.repository.RepositoryUtils.extractString;


/**
 * Convert SocialGroup to and from database.  SocialGroup-specific queries.
 */
public class SocialGroupGateway extends Gateway<SocialGroup> {

    public SocialGroupGateway() {
        super(OpenHDS.SocialGroups.CONTENT_ID_URI_BASE, COLUMN_SOCIAL_GROUP_EXTID, new SocialGroupConverter());
    }

    private static class SocialGroupConverter implements Converter<SocialGroup> {

        @Override
        public SocialGroup fromCursor(Cursor cursor) {
            SocialGroup socialGroup = new SocialGroup();

            socialGroup.setExtId(extractString(cursor, COLUMN_SOCIAL_GROUP_EXTID));
            socialGroup.setGroupHead(extractString(cursor, COLUMN_SOCIAL_GROUP_HEAD_INDIVIDUAL_EXTID));
            socialGroup.setGroupName(extractString(cursor, COLUMN_SOCIAL_GROUP_NAME));

            return socialGroup;
        }

        @Override
        public ContentValues toContentValues(SocialGroup socialGroup) {
            ContentValues contentValues = new ContentValues();

            contentValues.put(COLUMN_SOCIAL_GROUP_EXTID, socialGroup.getExtId());
            contentValues.put(COLUMN_SOCIAL_GROUP_HEAD_INDIVIDUAL_EXTID, socialGroup.getGroupHead());
            contentValues.put(COLUMN_SOCIAL_GROUP_NAME, socialGroup.getGroupName());

            return contentValues;
        }

        @Override
        public String getId(SocialGroup socialGroup) {
            return socialGroup.getExtId();
        }

        @Override
        public DataWrapper toDataWrapper(ContentResolver contentResolver, SocialGroup socialGroup, String state) {
            DataWrapper dataWrapper = new DataWrapper();
            dataWrapper.setExtId(socialGroup.getExtId());
            dataWrapper.setName(socialGroup.getGroupName());
            dataWrapper.setCategory(state);
            return dataWrapper;
        }
    }
}

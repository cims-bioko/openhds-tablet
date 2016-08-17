package org.openhds.mobile.search;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

public class IndexingService extends IntentService {

    private static final String TAG = IndexingService.class.getSimpleName();

    private static final String REBUILD_KEY = "rebuild";
    private static final String ENTITY_TYPE = "entityType";
    private static final String ENTITY_UUID = "entityUuid";

    public enum EntityType {
        HIERARCHY,
        LOCATION,
        INDIVIDUAL
    }

    public IndexingService() {
        super("indexer");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Indexer indexer = Indexer.getInstance(getApplicationContext());
        if (intent.hasExtra(ENTITY_UUID)) {
            EntityType type = EntityType.valueOf(intent.getStringExtra(ENTITY_TYPE));
            String uuid = intent.getStringExtra(ENTITY_UUID);
            try {
                switch (type) {
                    case HIERARCHY:
                        indexer.reindexHierarchy(uuid);
                        break;
                    case LOCATION:
                        indexer.reindexLocation(uuid);
                        break;
                    case INDIVIDUAL:
                        indexer.reindexIndividual(uuid);
                        break;
                    default:
                        Log.w(TAG, "unknown entity type " + type);
                }
            } catch (IOException e) {
                Log.e(TAG, "failed during reindex", e);
            }
        } else if (intent.hasExtra(REBUILD_KEY)) {
            indexer.reindexAll();
        } else {
            indexer.indexAll();
        }
    }

    public static void queueFullIndex(Context ctx) {
        ctx.startService(new Intent(ctx, IndexingService.class));
    }

    public static void queueFullReindex(Context ctx) {
        Intent intent = new Intent(ctx, IndexingService.class);
        intent.putExtra(REBUILD_KEY, true);
        ctx.startService(intent);
    }

    public static void queueReindex(Context ctx, EntityType type, String uuid) {
        Intent intent = new Intent(ctx, IndexingService.class);
        intent.putExtra(ENTITY_TYPE, type.toString());
        intent.putExtra(ENTITY_UUID, uuid);
        ctx.startService(intent);
    }
}


package org.cimsbioko.navconfig.forms;

import org.cimsbioko.activity.HierarchyNavigatorActivity;
import org.cimsbioko.navconfig.HierarchyPath;
import org.cimsbioko.model.core.FieldWorker;
import org.cimsbioko.data.DataWrapper;
import org.cimsbioko.navconfig.UsedByJSConfig;

/**
 * This is the formal contract currently required to build a payload and launch a form. Currently,
 * {@link HierarchyNavigatorActivity} implements this contract directly. However, this ensures that
 * that dependency can be easily identified and, if necessary, decoupled.
 */
public interface LaunchContext {
    @UsedByJSConfig
    FieldWorker getCurrentFieldWorker();
    DataWrapper getCurrentSelection();
    @UsedByJSConfig
    HierarchyPath getHierarchyPath();
}

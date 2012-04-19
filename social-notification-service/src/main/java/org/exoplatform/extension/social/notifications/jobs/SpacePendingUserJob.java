/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *
 */
package org.exoplatform.extension.social.notifications.jobs;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.CronJob;

public class SpacePendingUserJob extends CronJob {

    private static final Log log = ExoLogger.getLogger(SpacePendingUserJob.class);

    public SpacePendingUserJob(InitParams params) throws Exception {
         super(params);
        ExoProperties props = params.getPropertiesParam("cronjob.info").getProperties();
        String expression = props.getProperty("expression");
        
        log.info("Space Pending User Job Configure with : "+ expression +" (You can set the system property extension.social.space.notification.job to override this)");

    }
}

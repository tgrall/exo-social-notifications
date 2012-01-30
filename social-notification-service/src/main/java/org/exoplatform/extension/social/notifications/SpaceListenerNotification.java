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
package org.exoplatform.extension.social.notifications;


import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;


/**
 *
 * @author tgrall
 */
public class SpaceListenerNotification extends SpaceListenerPlugin{
  private InitParams initParams;
  private Log log = ExoLogger.getLogger(SpaceListenerNotification.class);

  public SpaceListenerNotification(InitParams initParams) {
    this.initParams = initParams;
  }

    @Override
    public void spaceCreated(SpaceLifeCycleEvent slce) {
        System.out.println("spaceCreated");
    }

    @Override
    public void spaceRemoved(SpaceLifeCycleEvent slce) {
        System.out.println("spaceRemoved");
    }

    @Override
    public void applicationActivated(SpaceLifeCycleEvent slce) {
        System.out.println("applicationActivated");
    }

    @Override
    public void applicationAdded(SpaceLifeCycleEvent slce) {
        System.out.println("applicationAdded");
    }

    @Override
    public void applicationDeactivated(SpaceLifeCycleEvent slce) {
        System.out.println("applicationDeactivated");
    }

    @Override
    public void applicationRemoved(SpaceLifeCycleEvent slce) {
        System.out.println("applicationRemoved");
    }

    @Override
    public void grantedLead(SpaceLifeCycleEvent slce) {
        System.out.println("grantedLead");
    }

    @Override
    public void joined(SpaceLifeCycleEvent slce) {
        System.out.println("joined");
    }

    @Override
    public void left(SpaceLifeCycleEvent slce) {
        System.out.println("left");
    }

    @Override
    public void revokedLead(SpaceLifeCycleEvent slce) {
        System.out.println("revokedLead");
    }

  
  

}

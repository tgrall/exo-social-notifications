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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.groovyscript.GroovyTemplate;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 *
 * @author tgrall
 */
public class SocialNotificationService {
    
    
    private String portalUrl = null;
    private SpaceService spaceService = null;
    private IdentityManager identityManager = null;
    private TemplateService templateService = null; //TODO: to use instead of strings
    private MailService mailService = null;

    public SocialNotificationService(InitParams initParams) {
    }

    public void sendPendingUsersToSpaceManager() {
        try {
            List<Space> spaces = this.getSpaceService().getAllSpaces(); //tODO upgrade to proper method
            for (Space space : spaces) {
                String[] pendingUsers = space.getPendingUsers();
                if (pendingUsers != null) {
                    String managers[] = space.getManagers();
                    List<Profile> managerList = new ArrayList();
                    for (String manager : managers) {
                        Profile managerProfile = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, manager, false).getProfile();
                        managerList.add(managerProfile);
                    }

                    List<Profile> userList = new ArrayList();
                    for (String pendingUser : pendingUsers) {
                        Profile userProfile = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, pendingUser, false).getProfile();
                        userList.add(userProfile);

                    }

                    this.sendMailForSpace(space, managerList, userList);
                }
            }



        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private SpaceService getSpaceService() {
        
        if (spaceService != null) {
            return spaceService;
        }
        
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        spaceService = (SpaceService) containerContext.getComponentInstanceOfType(SpaceService.class);
        return spaceService;
    }

    private IdentityManager getIdentityManager() {
        if (identityManager != null ) {
            return identityManager;
        }
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        identityManager = (IdentityManager) containerContext.getComponentInstanceOfType(IdentityManager.class);
        return identityManager;
    }

    private MailService getMailService() {
        if (mailService != null) {
            return mailService;
        }
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        mailService =  (MailService) containerContext.getComponentInstanceOfType(MailService.class);
        return mailService;
    }

    private TemplateService getTemplateService() {
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        templateService = (TemplateService) containerContext.getComponentInstanceOfType(TemplateService.class);
        return templateService;
    }

    /**
     * 
     * @param space
     * @param managers
     * @param pendingUsers 
     */
    private void sendMailForSpace(Space space, List<Profile> managers, List<Profile> pendingUsers) {
        //TODO : use groovy template stored in JCR for mail information (cleaner, real templating)
        try {
            String subject = this.getSubject();
            subject = StringUtils.replace(subject, "$spaceName", space.getDisplayName());
            subject = StringUtils.replace(subject, "$numberOfPendingUser", Integer.toString(pendingUsers.size()));

            String htmlContent = this.getHtmlContent(); // TODO : replace with groovy template and binding
            StringBuilder spaceAvatarHtml = new StringBuilder();
            if (space.getAvatarUrl() != null) {
                spaceAvatarHtml.append("<img src='").append(this.getPortalUrl()).append(space.getAvatarUrl()).append("' ");
                spaceAvatarHtml.append(" height='40px' align='left' style='margin-right:10px' />");
            }
            htmlContent = StringUtils.replace(htmlContent, "$spaceAvatar", spaceAvatarHtml.toString());
            htmlContent = StringUtils.replace(htmlContent, "$spaceName", space.getDisplayName());
            htmlContent = StringUtils.replace(htmlContent, "$numberOfPendingUser", Integer.toString(pendingUsers.size()));
            StringBuilder sb = new StringBuilder();
            for (Profile profile : pendingUsers) {
                sb.append("<img height='30px'  valign='middle' src='").append(this.getPortalUrl()).append(profile.getAvatarUrl()).append("' />");
                sb.append("&nbsp;<b><a href='").append(this.getPortalUrl()).append(profile.getUrl()).append("' >").append(profile.getFullName()).append("</a></b>");
                sb.append("<br/>\n");

            }

            htmlContent = StringUtils.replace(htmlContent, "$pendingUserList", sb.toString());
            String spaceUrl = this.getPortalUrl() + "/portal/g/:spaces:" + space.getUrl() + "/" + space.getUrl() + "/settings";  //TODO: see which API to use
            htmlContent = StringUtils.replace(htmlContent, "$spaceUrl", spaceUrl);

            MailService mailService = this.getMailService();
            Session mailSession = mailService.getMailSession();
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(this.getSenderAddress());
            message.setRecipients(RecipientType.TO, this.getSpaceManagersEmail(managers));
            
            
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html ; charset=ISO-8859-1");
            mailService.sendMessage(message);
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * TODO: make it dynamic to search the value in a generic configuration in the JCR
     * @return the URL of the portal based on the exo.global.url system properties. (default is http://127.0.0.1:8080
     */
    private String getPortalUrl() {
        if (portalUrl != null ) {
            return portalUrl;
        }
        String portalUrl = System.getProperty("exo.global.url");
        if (portalUrl == null) {
            portalUrl = "http://127.0.0.1:8080";
        }        
        return portalUrl;
    }


    /**
     * Get the Manager list email addresses
     * @param managers
     * @return list of internet email addresses of the managers of the space
     * @throws UnsupportedEncodingException 
     */
    private InternetAddress[] getSpaceManagersEmail(List<Profile> managers) throws UnsupportedEncodingException {
        List<InternetAddress> addresses = new ArrayList<InternetAddress>();
        for (Profile manager : managers) {
            InternetAddress add = new InternetAddress( manager.getEmail(), manager.getFullName());
            addresses.add(add);
        }
        return addresses.toArray( new InternetAddress[ managers.size() ] );
    }
   
    
    private InternetAddress getSenderAddress() {
        try {
            return new InternetAddress("noreply@exo.intranet.com", "eXo Intranet");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    private String getSubject() {
        return "Space '$spaceName' : $numberOfPendingUser Request(s) to join";
    }

    private String getHtmlContent() {
        StringBuilder sb = new StringBuilder();


        sb.append("<html>").append("\n");
        sb.append("<body style='font-family: Verdana,Arial,sans-serif;' >").append("\n");

        sb.append("<h2>Space $spaceName : $numberOfPendingUser request(s) to join</h2>").append("\n");


        sb.append("$spaceAvatar");
        sb.append("As manager of this space you are invited to accept or reject the request at the following location :<br>").append("\n");
        sb.append("<a href='$spaceUrl'>Manage '$spaceName' Space </a>").append("\n");

        sb.append("<div style='margin:10px'><b>Pending Users :</b><br>").append("\n");
        sb.append("<div style='margin-left:30px' >").append("\n");
        sb.append("$pendingUserList").append("\n");
        sb.append("</div>").append("\n");
        sb.append("</div>").append("\n");


        sb.append("</body>").append("\n");
        sb.append("</html>").append("\n");

        return sb.toString();
    }
}

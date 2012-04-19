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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.extension.social.notifications.model.MessageTemplate;
import org.exoplatform.extension.social.notifications.model.SocialNotificationConfiguration;
import org.exoplatform.groovyscript.GroovyTemplate;
import org.exoplatform.groovyscript.text.BindingContext;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.organization.OrganizationService;
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

    private Log log = ExoLogger.getLogger(SocialNotificationService.class);
    
    
    private String portalUrl = null;
    private SpaceService spaceService = null;
    private IdentityManager identityManager = null;
    private TemplateService templateService = null; //TODO: to use instead of strings
    private MailService mailService = null;
    private OrganizationService orgService = null;

    public SocialNotificationService(InitParams initParams) {
    }

    public void spaceNotification() {
            log.info("=============== SPACE NOTIFICATION JOB : Start demo ========================");
        try {
            
            Map<String, List<Space>> invitedUsersList = new HashMap();
            
            
            
            List<Space> spaces = this.getSpaceService().getAllSpaces(); //tODO upgrade to proper method
            for (Space space : spaces) {
                String[] pendingUsers = space.getPendingUsers();                
                String[] invitedUsers = space.getInvitedUsers();
                
                if (pendingUsers != null && pendingUsers.length != 0) {
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

                    this.pendingUserNotification(space, managerList, userList);
                }
                
                
                if (invitedUsers != null) {
                    for (String invitedUser : invitedUsers) {
                        if ( invitedUsersList.containsKey(invitedUser) ) {
                            invitedUsersList.get(invitedUser).add(space);       
                        } else {
                            List newList = new ArrayList<Space>();
                            newList.add(space);
                            
                            invitedUsersList.put(invitedUser, newList );
                        }
                    }
                }
                

            }

            
            // send mail to invited users
            this.invitedUserNotification(invitedUsersList);
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("=============== SPACE NOTIFICATION JOB : End ========================");
    }

    private OrganizationService getOrganizationService() {
        if (orgService != null) {
            return orgService;
        }
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        orgService = (OrganizationService) containerContext.getComponentInstanceOfType(OrganizationService.class);
        return orgService;
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
        if (identityManager != null) {
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
        mailService = (MailService) containerContext.getComponentInstanceOfType(MailService.class);
        return mailService;
    }

    private TemplateService getTemplateService() {
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        templateService = (TemplateService) containerContext.getComponentInstanceOfType(TemplateService.class);
        return templateService;
    }
    
    private MessageTemplate getMailMessageTemplate(String type, Locale locale) {
        ExoContainer containerContext = ExoContainerContext.getCurrentContainer();
        SocialNotificationConfiguration configuration = (SocialNotificationConfiguration) containerContext.getComponentInstanceOfType(SocialNotificationConfiguration.class);
        
        
        MessageTemplate returnValue =  configuration.getMailTemplate(type, locale);
        
        return returnValue;
        
    }
    
    
    /**
     * 
     * @param invitedUsersList 
     */
    private void invitedUserNotification(Map<String, List<Space>> invitedUsersList)  {
        
        for (Map.Entry<String, List<Space>> entry : invitedUsersList.entrySet()) {
            
            try {
                String userId = entry.getKey();
                List<Space> spacesList =  entry.getValue();
                Locale locale = Locale.getDefault();
                
                // get default locale of the manager
                String userLocale = this.getOrganizationService().getUserProfileHandler().findUserProfileByName(userId).getAttribute("user.language");
                Profile userProfile = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, false).getProfile();
                
                if ( userLocale != null && !userLocale.trim().isEmpty() ) {
                    locale = new Locale(userLocale);
                }   
                
                // getMessageTemplate
                MessageTemplate messageTemplate = this.getMailMessageTemplate(SocialNotificationConfiguration.MAIL_TEMPLATE_SPACE_PENDING_INVITATIONS, locale);

                GroovyTemplate g = new GroovyTemplate( messageTemplate.getSubject() );
                
                Map binding = new HashMap();
                binding.put("userProfile",  userProfile );
                binding.put("portalUrl", this.getPortalUrl());
                binding.put("invitationUrl",  this.getPortalUrl() + "/portal/intranet/invitationSpace" ); 
                binding.put("spacesList", spacesList );
                
                String subject = g.render(binding); 
                
                g = new GroovyTemplate( messageTemplate.getHtmlContent() );
                String htmlContent = g.render(binding);  
                
                g = new GroovyTemplate( messageTemplate.getPlainTextContent() );
                String textContent = g.render(binding);
                
                MailService mailService = this.getMailService();
                Session mailSession = mailService.getMailSession();
                MimeMessage message = new MimeMessage(mailSession);
                message.setFrom(this.getSenderAddress());

                // send email to invited user
                message.setRecipient(RecipientType.TO, new InternetAddress(userProfile.getEmail(), userProfile.getFullName()));
                message.setSubject(subject);
                MimeMultipart content = new MimeMultipart("alternative");
                MimeBodyPart text = new MimeBodyPart();
                MimeBodyPart html = new MimeBodyPart();
                text.setText( textContent );
                html.setContent(htmlContent , "text/html; charset=ISO-8859-1");
                content.addBodyPart(text);
                content.addBodyPart(html);
                message.setContent( content );
                
                log.info("Sending mail to : "+ userProfile.getEmail() +" : "+ subject +"\n"+ html );
                
                mailService.sendMessage(message);
            
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        
    }
    

    /**
     * 
     * @param space
     * @param managers
     * @param pendingUsers 
     */
    private void pendingUserNotification(Space space, List<Profile> managerList, List<Profile> pendingUsers) {
        //TODO : use groovy template stored in JCR for mail information (cleaner, real templating)
    
        log.info("Sending mail to space manager : pending users");
        

        try {

            // loop on each manager and send mail
            // like that each manager will have the mail in its preferred language
            // ideally should be done in a different executor
            // TODO: see if we can optimize this to avoid do it for all user
            //       - send a mail to all the users in the same time (what about language)
            //       - cache the template result and send mail

            for (Profile manager : managerList) {
                Locale locale = Locale.getDefault();
                
                // get default locale of the manager
                String userId = manager.getIdentity().getRemoteId();
                String userLocale = this.getOrganizationService().getUserProfileHandler().findUserProfileByName(userId).getAttribute("user.language");
                if ( userLocale != null && !userLocale.trim().isEmpty() ) {
                    locale = new Locale(userLocale);
                }
                
                
                // getMessageTemplate
                MessageTemplate messageTemplate = this.getMailMessageTemplate(SocialNotificationConfiguration.MAIL_TEMPLATE_SPACE_PENDING_USERS, locale);

                GroovyTemplate g = new GroovyTemplate( messageTemplate.getSubject() );
                
                String spaceUrl = this.getPortalUrl() + "/portal/g/:spaces:" + space.getUrl() + "/" + space.getUrl() + "/settings";  //TODO: see which API to use
                String spaceAvatarUrl = null;
                if ( space.getAvatarUrl() != null ) {
                    spaceAvatarUrl = this.getPortalUrl() + space.getAvatarUrl();
                }
                
                Map binding = new HashMap();
                binding.put("space", space);
                binding.put("portalUrl", this.getPortalUrl());
                binding.put("spaceSettingUrl", spaceUrl);
                binding.put("spaceAvatarUrl", spaceAvatarUrl);
                binding.put("userPendingList",  pendingUsers );
                
                String subject = g.render(binding); 
                
                g = new GroovyTemplate( messageTemplate.getHtmlContent() );
                String htmlContent = g.render(binding);                 
                
                g = new GroovyTemplate( messageTemplate.getPlainTextContent() );
                String textContent = g.render(binding);                 

                
                MailService mailService = this.getMailService();
                Session mailSession = mailService.getMailSession();
                MimeMessage message = new MimeMessage(mailSession);
                message.setFrom(this.getSenderAddress());

                // send email to manager
                message.setRecipient(RecipientType.TO, new InternetAddress(manager.getEmail(), manager.getFullName()));
                message.setSubject(subject);
                MimeMultipart content = new MimeMultipart("alternative");
                MimeBodyPart text = new MimeBodyPart();
                MimeBodyPart html = new MimeBodyPart();
                text.setText( textContent );
                html.setContent(htmlContent , "text/html; charset=ISO-8859-1");
                content.addBodyPart(text);
                content.addBodyPart(html);
                message.setContent( content );
                

                log.info("Sending mail to"+ manager.getEmail() +" : "+ subject +" : "+ subject +"\n"+ html);
                
                //mailService.sendMessage(message);
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    
    
    /**
     * TODO: make it dynamic to search the value in a generic configuration in the JCR
     * @return the URL of the portal based on the exo.global.url system properties. (default is http://127.0.0.1:8080
     */
    private String getPortalUrl() {
        if (portalUrl != null) {
            return portalUrl;
        }
        String portalUrl = System.getProperty("exo.global.url");
        if (portalUrl == null) {
            portalUrl = "http://127.0.0.1:8080";
        }
        return portalUrl;
    }

    /**
     * 
     * @return the sender address 
     */
    private InternetAddress getSenderAddress() {
        try {
            return new InternetAddress("noreply@exo.intranet.com", "eXo Intranet");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

}

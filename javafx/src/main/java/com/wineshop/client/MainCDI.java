/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package com.wineshop.client;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialogs;
import javafx.scene.control.Dialogs.DialogOptions;
import javafx.scene.control.Dialogs.DialogResponse;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.granite.client.javafx.tide.JavaFXServerSessionStatus;
import org.granite.client.javafx.tide.ManagedEntity;
import org.granite.client.javafx.tide.TideFXMLLoader;
import org.granite.client.javafx.tide.cdi.JavaFXTideClientExtension;
import org.granite.client.javafx.tide.collections.PagedQuery;
import org.granite.client.javafx.tide.spring.Identity;
import org.granite.client.tide.ContextManager;
import org.granite.client.tide.cdi.ViewScoped;
import org.granite.client.tide.data.Conflicts;
import org.granite.client.tide.data.DataConflictListener;
import org.granite.client.tide.data.DataObserver;
import org.granite.client.tide.data.EntityManager;
import org.granite.client.tide.server.ExceptionHandler;
import org.granite.client.tide.server.ServerSession;
import org.granite.client.tide.server.SimpleTideResponder;
import org.granite.client.tide.server.TideFaultEvent;
import org.granite.client.tide.server.TideResultEvent;
import org.granite.client.tide.validation.ValidationExceptionHandler;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wineshop.client.entities.Vineyard;
import com.wineshop.client.services.VineyardRepository;


/**
 * Main application using CDI/Weld as a client container
 * 
 * @author william
 */
public class MainCDI extends Application {
	
	static final Logger log = LoggerFactory.getLogger(MainCDI.class);
    
    public static void main(String[] args) {
        Application.launch(MainCDI.class, args);
    }
        
    
    // Configuration class which only produces necessary beans
    public static class Config {
    	
    	/**
    	 * The server session which handles all communication with the server
    	 */
    	@Produces @ApplicationScoped @Named
    	public ServerSession getServerSession() throws Exception {
    		ServerSession serverSession = new ServerSession("/shop-admin-javafx", "localhost", 8080);
    		// Important: indicates the packages to scan for remotely serializable classes (mostly domain classes)
        	serverSession.addRemoteAliasPackage("com.wineshop.client.entities");
        	return serverSession;
    	}
    	
    	/**
    	 * The client identity instance which integrates with server-side security
    	 */
    	@Produces @ApplicationScoped @Named
    	public Identity getIdentity(ServerSession serverSession) throws Exception {
    		return new Identity(serverSession);
    	}
    	
    	/**
    	 * A validation exception handler which propagates server validation errors to the client 
    	 */
    	@Produces @ApplicationScoped
    	public ExceptionHandler getValidationExceptionHandler() {
    		return new ValidationExceptionHandler();
    	}
    	
    	/**
    	 * A paged collection bound to a Spring Data repository
    	 */
    	@Produces @ViewScoped @Named
    	public PagedQuery<Vineyard, Vineyard> getVineyards(ServerSession serverSession) throws Exception {
    		PagedQuery<Vineyard, Vineyard> vineyards = new PagedQuery<Vineyard, Vineyard>(serverSession);
    		vineyards.setMethodName("findByFilter");
    		vineyards.setMaxResults(25);
    		vineyards.setRemoteComponentClass(VineyardRepository.class);
    		vineyards.setElementClass(Vineyard.class);
    		vineyards.setFilterClass(Vineyard.class);
    		return vineyards;
    	}
    	
    	/**
    	 * Disposer method for all paged query components
    	 * Not sure why CDI does not call @PreDestroy on produced objects
    	 */
    	public void destroyPagedQuery(@Disposes PagedQuery<?, ?> pagedQuery) {
    		pagedQuery.clear();
    	}
        
    	/**
    	 * A managed entity that can be used in controllers or views
    	 * @param entityManager Tide entity manager
    	 * @param dataManager Tide data manager
    	 * 
    	 * @return a managed entity property 
    	 */
        @Produces @ViewScoped @Named
        public ManagedEntity<Vineyard> getVineyard(EntityManager entityManager) {
            return new ManagedEntity<Vineyard>(entityManager);
        }
    	
    	/**
    	 * A messaging topic to receive data updates from the server
    	 * @Named is here used to give a default name to the topic (wineshopTopic)
    	 */
    	@Produces @ApplicationScoped @Named
    	public DataObserver getWineshopTopic(ServerSession serverSession, EntityManager entityManager) {
    		return new DataObserver(serverSession, entityManager);
    	}
    }
    
    /**
     * Main application
     */
    public static class App {
    	
    	@Inject
    	private Identity identity;
    	
    	@Inject
    	private ContextManager contextManager;
    	
    	@Inject
    	private EntityManager entityManager;
    	
    	@Inject
    	private ServerSession serverSession;
    	
    	@Inject
    	private DataObserver wineshopTopic;
    	
    	
    	public void start(final Stage stage) throws Exception {
    	    /**
    	     * Attach stage to server session (mostly for busy cursor handling)
    	     */
    		serverSession.start();
    		wineshopTopic.start();
        	((JavaFXServerSessionStatus)serverSession.getStatus()).setStage(stage);
    		
    		/**
    		 * Attaches a listener to current authentication state to switch between Login and Home screens
    		 */
            identity.loggedInProperty().addListener(new ChangeListener<Boolean>() {
    			@Override
    			public void changed(ObservableValue<? extends Boolean> property, Boolean oldValue, Boolean newValue) {
					showView(stage, newValue);
    			}
            });
            
            /**
             * Checks for current server authentication state at startup
             */
            identity.checkLoggedIn(new SimpleTideResponder<String>() {
    			@Override
    			public void result(TideResultEvent<String> event) {
    				if (event.getResult() == null)
						showView(stage, false);
    			}
    			
    			@Override
    			public void fault(TideFaultEvent event) {
					Parent login = showView(stage, false);
					Label message = (Label)login.lookup("#labelMessage");
					message.setText(event.getFault().getFaultDescription());
					message.setVisible(true);
    			}
            });
            
    	    // Setup conflict handling
    		entityManager.addListener(new DataConflictListener() {			
    			@Override
    			public void onConflict(EntityManager entityManager, Conflicts conflicts) {
    				DialogResponse response = Dialogs.showConfirmDialog(stage, 
    						"Accept incoming data or keep local changes ?", 
    						"Conflict with another user modifications", 
    						"Conflict", DialogOptions.YES_NO
    				);
    				if (response == DialogResponse.YES)
    					conflicts.acceptAllServer();
    				else
    					conflicts.acceptAllClient();
    			}			
    		});

    	}
    	
    	public void stop() throws Exception {
    		wineshopTopic.stop();
    		serverSession.stop();
    	}
        
    	/**
    	 * Displays the requested screen
    	 */
        private Parent showView(Stage stage, boolean loggedIn) {
        	try {
	            Parent root = (Parent)TideFXMLLoader.load(contextManager.getContext(), loggedIn ? "Home.fxml" : "Login.fxml", loggedIn ? Home.class : Login.class);
	            
	            stage.setScene(new Scene(root));
	            stage.show();
	            
	            return root;
        	}
        	catch (Exception e) {
        		log.error("Could not show view", e);
        	}
        	return null;
        }
    }
    
    private Weld weld;
    private WeldContainer weldContainer;
    
    @Override
    public void start(final Stage stage) throws Exception {
    	// Bootstrap Weld with the Tide extension which registers all internal Tide beans/scopes
    	weld = new Weld();
    	weld.addExtension(new JavaFXTideClientExtension());
    	weldContainer = weld.initialize();   
    	
    	// Start the application
    	weldContainer.instance().select(ServerSession.class).get().start();
        weldContainer.instance().select(DataObserver.class).get().start();
        weldContainer.instance().select(App.class).get().start(stage);
    }
    
    @Override
    public void stop() throws Exception {
    	// Stop the application
        weldContainer.instance().select(App.class).get().stop();
        weldContainer.instance().select(DataObserver.class).get().stop();
        weldContainer.instance().select(ServerSession.class).get().stop();
    	
    	// Stop the Weld container
    	weld.shutdown();
    }
}

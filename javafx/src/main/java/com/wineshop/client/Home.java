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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

import javax.inject.Inject;

import org.granite.client.tide.data.EntityManager.UpdateKind;
import org.granite.client.tide.server.EmptyTideResponder;
import org.granite.client.tide.server.SimpleTideResponder;
import org.granite.client.tide.server.TideFaultEvent;
import org.granite.client.tide.server.TideResultEvent;
import org.granite.client.tide.spring.TideApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.wineshop.client.entities.Welcome;
import com.wineshop.client.services.WelcomeService;


/**
 * 
 * @author william
 */
@Component
public class Home implements Initializable, ApplicationListener<TideApplicationEvent> {

	@FXML 
	private TextField fieldHello;
	
	@FXML 
	private ListView<Welcome> listWelcomes;
	
	@FXML 
	private Label labelMessage;
	
	@Inject
	private WelcomeService welcomeService;
	
	
	@SuppressWarnings("unused")
	@FXML
	private void hello(ActionEvent event) {
	    /**
	     * Issue a remote call to the welcome service and display result or error
	     */
		welcomeService.hello(fieldHello.getText(), 
			new SimpleTideResponder<Welcome>() {
				@Override
				public void result(TideResultEvent<Welcome> tre) {
					fieldHello.setText("");
					labelMessage.setVisible(false);
					labelMessage.setManaged(false);
				}
				
				@Override
				public void fault(TideFaultEvent tfe) {
					labelMessage.setText("Error: " + tfe.getFault().getFaultDescription());
					labelMessage.setVisible(true);
					labelMessage.setManaged(true);
				}
			}
		);
	}
	
	
	@Override
	public void onApplicationEvent(TideApplicationEvent event) {
	    /**
	     * Handle PERSIST messages received from the real-time topic
	     */
		if (event.getType().equals(UpdateKind.PERSIST.eventName(Welcome.class)))
			listWelcomes.getItems().add((Welcome)event.getArgs()[0]);
	}
	

	@Override
	public void initialize(URL url, ResourceBundle bundle) {
	    /**
	     * Attach a bound text cell to the list view so remote updates can be done
	     * through data binding
	     */
		listWelcomes.setCellFactory(new Callback<ListView<Welcome>, ListCell<Welcome>>() {
			@Override
			public ListCell<Welcome> call(ListView<Welcome> listView) {
				return new ListCell<Welcome>() {
					protected void updateItem(Welcome welcome, boolean empty) {
						Welcome oldWelcome = getItem();
						if (oldWelcome != null && welcome != oldWelcome)
							textProperty().unbindBidirectional(oldWelcome.messageProperty());
						
						super.updateItem(welcome, empty);
						
						if (welcome != null && welcome != oldWelcome)
							textProperty().bindBidirectional(welcome.messageProperty());
					}
				};
			}
		});
		
		/**
		 * Trigger a remote call to find current list of welcome objects
		 * Use the mergeWith property to automatically merge the results with the 
		 * collection bound to the ListView
		 */
		welcomeService.findAll(EmptyTideResponder.<List<Welcome>>mergeWith(listWelcomes.getItems()));
	}
}

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
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javax.inject.Inject;

import org.granite.client.tide.data.DataObserver;
import org.granite.client.tide.events.TideEvent;
import org.granite.client.tide.events.TideEventObserver;
import org.granite.client.tide.javafx.spring.Identity;
import org.granite.client.tide.server.ServerSession;
import org.granite.client.tide.server.SimpleTideResponder;
import org.granite.client.tide.server.TideFaultEvent;
import org.granite.client.tide.server.TideResultEvent;
import org.springframework.stereotype.Component;

/**
 * 
 * @author william
 */
@Component
public class Login implements Initializable, TideEventObserver {

	@FXML
	private TextField fieldUsername;
	
	@FXML
	private PasswordField fieldPassword;
	
	@FXML
	private Label labelMessage;
	
	@Inject
	private Identity identity;
	
	@Inject
	private DataObserver welcomeTopic;
	
	
	@SuppressWarnings("unused")
	@FXML
	private void login(ActionEvent event) {
		identity.login(fieldUsername.getText(), fieldPassword.getText(),
			new SimpleTideResponder<String>() {
				@Override
				public void result(TideResultEvent<String> tre) {
					labelMessage.setVisible(false);
					labelMessage.setManaged(false);
				}
				
				@Override
				public void fault(TideFaultEvent tfe) {
					labelMessage.setVisible(true);
					labelMessage.setManaged(true);
					labelMessage.setText(tfe.getFault().getFaultDescription());
				}
			}
		);
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
	}

	@Override
	public void handleEvent(TideEvent event) {
		if (ServerSession.LOGIN.equals(event.getType())) {
			welcomeTopic.subscribe();
		}
		else if (ServerSession.LOGOUT.equals(event.getType())) {
			welcomeTopic.unsubscribe();
		}
		else if (ServerSession.SESSION_EXPIRED.equals(event.getType())) {
			labelMessage.setVisible(true);
			labelMessage.setText("Session expired");
		}
	}
}

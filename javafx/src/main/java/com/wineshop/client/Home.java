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

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.granite.client.tide.collections.javafx.PagedQuery;
import org.granite.client.tide.collections.javafx.TableViewSortAdapter;
import org.granite.client.tide.javafx.ManagedEntity;
import org.granite.client.tide.server.SimpleTideResponder;
import org.granite.client.tide.server.TideFaultEvent;
import org.granite.client.tide.server.TideResultEvent;
import org.granite.client.tide.spring.TideApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.wineshop.client.entities.Address;
import com.wineshop.client.entities.Vineyard;
import com.wineshop.client.entities.Wine;
import com.wineshop.client.services.VineyardRepository;


/**
 * 
 * @author william
 */
@Component
public class Home implements Initializable, ApplicationListener<TideApplicationEvent> {

	@FXML
	private TextField fieldSearch;

	@FXML
	private TableView<Vineyard> tableVineyards;
	
	@FXML
	private Label labelFormVineyard;
	
	@FXML
	private TextField fieldName;
	
	@FXML
	private TextField fieldAddress;

	@FXML
	private Button buttonDelete;

	@FXML
	private Button buttonCancel;

	
	@Inject
	private PagedQuery<Vineyard, Vineyard> vineyards;
	
	@Inject
	private ManagedEntity<Vineyard> vineyard;
	
	@Inject
	private VineyardRepository vineyardRepository;
	
	
	@Override
	public void onApplicationEvent(TideApplicationEvent event) {
	}
	

	@Override
	public void initialize(URL url, ResourceBundle bundle) {
		// Setup of the table view
		vineyards.setSortAdapter(new TableViewSortAdapter<Vineyard>(tableVineyards, Vineyard.class));
	    vineyards.getFilter().nameProperty()
        	.bindBidirectional(fieldSearch.textProperty());
	    
	    // Setup of the creation/edit form
		labelFormVineyard.textProperty().bind(Bindings.when(vineyard.savedProperty()).then("Edit vineyard").otherwise("Create vineyard"));
		
		vineyard.instanceProperty().addListener(new ChangeListener<Vineyard>() {
			@Override
			public void changed(ObservableValue<? extends Vineyard> observable, Vineyard oldValue, Vineyard newValue) {
				if (oldValue != null) {
					fieldName.textProperty().unbindBidirectional(oldValue.nameProperty());
					fieldAddress.textProperty().unbindBidirectional(oldValue.getAddress().addressProperty());
				}
				if (newValue != null) {
					fieldName.textProperty().bindBidirectional(newValue.nameProperty());
					fieldAddress.textProperty().bindBidirectional(newValue.getAddress().addressProperty());
				}
			}
		});
		
		buttonCancel.disableProperty().bind(Bindings.not(vineyard.savedProperty()));
		buttonDelete.visibleProperty().bind(vineyard.savedProperty());
		
		// Link the table selection and the entity instance in the form 
		select(null);
		tableVineyards.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Vineyard>() {
			@Override
			public void changed(ObservableValue<? extends Vineyard> property, Vineyard oldSelection, Vineyard newSelection) {
				select(newSelection);
			}			
		});
	}
	
	
	/**
	 * Action on search button
	 */
	@FXML @SuppressWarnings("unused")
	private void search(ActionEvent event) {
	    vineyards.refresh();
	}
		
	/**
	 * Select a vineyard for edition/creation in the form
	 * 
	 * @param vineyard vineyard to edit or null to create a new one
	 */
	private void select(Vineyard vineyard) {
		if (vineyard == this.vineyard.getInstance() && this.vineyard.getInstance() != null)
			return;
		
		if (vineyard != null)
			this.vineyard.setInstance(vineyard);
		else {
			Vineyard newVineyard = new Vineyard();
			newVineyard.setName("");
			newVineyard.setAddress(new Address());
			newVineyard.getAddress().setAddress("");			
			this.vineyard.setInstance(newVineyard);
		}
	}
	
	/**
	 * Action for Save button
	 */
	@FXML @SuppressWarnings("unused")
	private void save(ActionEvent event) {
		final boolean isNew = !vineyard.isSaved();
		vineyardRepository.save(vineyard.getInstance(), 
			new SimpleTideResponder<Vineyard>() {
				@Override
				public void result(TideResultEvent<Vineyard> tre) {
					// Once the save is done, restore creation state
					if (isNew)
						select(null);
					else
						tableVineyards.getSelectionModel().clearSelection();
				}
				
				@Override
				public void fault(TideFaultEvent tfe) {
					System.out.println("Error: " + tfe.getFault().getFaultDescription());
				}
			}
		);
	}

	/**
	 * Action for Delete button
	 */
	@FXML @SuppressWarnings("unused")
	private void delete(ActionEvent event) {
		vineyardRepository.delete(vineyard.getInstance().getId(), 
			new SimpleTideResponder<Void>() {
				@Override
				public void result(TideResultEvent<Void> tre) {
					tableVineyards.getSelectionModel().clearSelection();
				}
			}
		);
	}
	
	@SuppressWarnings("unused")
	@FXML
	private void cancel(ActionEvent event) {
		if (tableVineyards.getSelectionModel().isEmpty())
			select(null);
		else
			tableVineyards.getSelectionModel().clearSelection();
	}
}

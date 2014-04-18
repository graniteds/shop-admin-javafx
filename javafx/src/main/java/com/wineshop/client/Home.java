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
import java.util.Calendar;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.granite.client.javafx.tide.ManagedEntity;
import org.granite.client.javafx.tide.collections.PagedQuery;
import org.granite.client.javafx.tide.collections.TableViewSortAdapter;
import org.granite.client.javafx.tide.spring.Identity;
import org.granite.client.javafx.validation.FormValidator;
import org.granite.client.javafx.validation.ValidationResultEvent;
import org.granite.client.tide.server.SimpleTideResponder;
import org.granite.client.tide.server.TideFaultEvent;
import org.granite.client.tide.server.TideResultEvent;
import org.granite.client.tide.spring.TideApplicationEvent;
import org.granite.client.validation.NotifyingValidatorFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.wineshop.client.entities.Address;
import com.wineshop.client.entities.Vineyard;
import com.wineshop.client.entities.Wine;
import com.wineshop.client.entities.Wine$Type;
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
	private Parent formVineyard;
	
	@FXML
	private Label labelFormVineyard;
	
	@FXML
	private TextField fieldName;
	
	@FXML
	private TextField fieldAddress;
	
	@FXML
	private ListView<Wine> listWines;
	
	@FXML
	private Button buttonSave;
	
	@FXML
	private Button buttonDelete;

	@FXML
	private Button buttonCancel;

	
	@Inject
	private PagedQuery<Vineyard, Vineyard> vineyards;
	
	@Inject
	private ManagedEntity<Vineyard> vineyard;
	
	@Inject
	private Identity identity;
	
	@Inject
	private VineyardRepository vineyardRepository;
    
    @Inject
    private NotifyingValidatorFactory validatorFactory;
	
	private FormValidator formValidator;
	
	
	@PostConstruct
	private void init() {
	    formValidator = new FormValidator(validatorFactory);
	}
	
	
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
					listWines.setItems(null);
				}
				if (newValue != null) {
					fieldName.textProperty().bindBidirectional(newValue.nameProperty());
					fieldAddress.textProperty().bindBidirectional(newValue.getAddress().addressProperty());
					listWines.setItems(newValue.getWines());
				}
			}
		});
		
		// Define the cell factory for the list of wines 
		listWines.setCellFactory(new Callback<ListView<Wine>, ListCell<Wine>>() {
		    public ListCell<Wine> call(ListView<Wine> listView) {
		        return new WineListCell();
		    }
		});
		
		buttonDelete.visibleProperty().bind(vineyard.savedProperty());
		buttonDelete.disableProperty().bind(
				Bindings.not(identity.ifAllGranted("ROLE_ADMIN")));
		buttonSave.disableProperty().bind(Bindings.not(vineyard.dirtyProperty()));
		buttonCancel.disableProperty().bind(
				Bindings.not(Bindings.or(vineyard.savedProperty(), vineyard.dirtyProperty())));
		
		// Link the table selection and the entity instance in the form 
		select(null);
		tableVineyards.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Vineyard>() {
			@Override
			public void changed(ObservableValue<? extends Vineyard> property, Vineyard oldSelection, Vineyard newSelection) {
				select(newSelection);
			}			
		});
		
		formVineyard.addEventHandler(ValidationResultEvent.INVALID, new EventHandler<ValidationResultEvent>() {
			@Override
			public void handle(ValidationResultEvent event) {
				((Node)event.getTarget()).setStyle("-fx-border-color: red");
				if (event.getTarget() instanceof TextInputControl && event.getErrorResults() != null && event.getErrorResults().size() > 0) {
				    Tooltip tooltip = new Tooltip(event.getErrorResults().get(0).getMessage());
				    tooltip.setAutoHide(true);
				    ((TextInputControl)event.getTarget()).setTooltip(tooltip);
				}
			}
		});
        formVineyard.addEventHandler(ValidationResultEvent.VALID, new EventHandler<ValidationResultEvent>() {
            @Override
            public void handle(ValidationResultEvent event) {
                ((Node)event.getTarget()).setStyle("-fx-border-color: null");
                if (event.getTarget() instanceof TextInputControl) {
                    Tooltip tooltip = ((TextInputControl)event.getTarget()).getTooltip();
                    if (tooltip != null && tooltip.isActivated())
                        tooltip.hide();
                    ((TextInputControl)event.getTarget()).setTooltip(null);
                }
            }
        });
	}
	
	
	/**
	 * Action on search button
	 */
	@FXML
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
		
		formValidator.setForm(null);
		
		this.vineyard.reset();
		
		if (vineyard != null)
			this.vineyard.setInstance(vineyard);
		else {
			Vineyard newVineyard = new Vineyard();
			newVineyard.setName("");
			newVineyard.setAddress(new Address());
			newVineyard.getAddress().setAddress("");			
			this.vineyard.setInstance(newVineyard);
		}
		
		formValidator.setForm(formVineyard);
	}
	
	/**
	 * Action for Save button
	 */
	@FXML
	private void save(ActionEvent event) {
		if (!validatorFactory.getValidator().validate(this.vineyard.getInstance()).isEmpty())
			return;
		
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
	@FXML
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
	
	/**
	 * Action for cancel button
	 */
	@FXML
	private void cancel(ActionEvent event) {
		if (tableVineyards.getSelectionModel().isEmpty())
			select(null);
		else
			tableVineyards.getSelectionModel().clearSelection();
	}
		
	/**
	 * Action for Add wine button
	 */
	@FXML
	private void addWine(ActionEvent event) {
	    Wine wine = new Wine();
	    wine.setVineyard(this.vineyard.getInstance());
	    wine.setName("");
	    wine.setYear(Calendar.getInstance().get(Calendar.YEAR)-3);
	    wine.setType(Wine$Type.RED);
	    this.vineyard.getInstance().getWines().add(wine);
	}

	/**
	 * Action for Remove wine button
	 */
	@FXML
	private void removeWine(ActionEvent event) {
	    if (!listWines.getSelectionModel().isEmpty())
	        this.vineyard.getInstance().getWines().remove(listWines.getSelectionModel().getSelectedIndex());
	}
	
	
	
	private static class WineListCell extends ListCell<Wine> {
		 
	    private ChoiceTypeListener choiceTypeListener = null;
	 
	    protected void updateItem(Wine wine, boolean empty) {
	        Wine oldWine = getItem();
	        if (oldWine != null && wine == null) {
	            HBox hbox = (HBox)getGraphic();
	            
	            TextField fieldName = (TextField)hbox.getChildren().get(0);
	            fieldName.textProperty()
	                .unbindBidirectional(getItem().nameProperty());
	            
	            TextField fieldYear = (TextField)hbox.getChildren().get(1);
	            fieldYear.textProperty()
	                .unbindBidirectional(getItem().yearProperty());
	            
	            getItem().typeProperty().unbind();
	            getItem().typeProperty().removeListener(choiceTypeListener);
	            choiceTypeListener = null;
	            
	            setGraphic(null);
	        }
	        
	        super.updateItem(wine, empty);
	        
	        if (wine != null && wine != oldWine) {
	            TextField fieldName = new TextField();
	            fieldName.textProperty()
	                .bindBidirectional(wine.nameProperty());
	 
	            TextField fieldYear = new TextField();
	            fieldYear.setPrefWidth(40);
	            fieldYear.textProperty()
	                .bindBidirectional(wine.yearProperty(), new IntegerStringConverter());
	 
	            ChoiceBox<Wine$Type> choiceType = new ChoiceBox<Wine$Type>(
	                FXCollections.observableArrayList(Wine$Type.values())
	            );
	            choiceType.getSelectionModel()
	                .select(getItem().getType());
	            getItem().typeProperty()
	                .bind(choiceType.getSelectionModel().selectedItemProperty());
	            choiceTypeListener = new ChoiceTypeListener(choiceType);
	            getItem().typeProperty()
	                .addListener(choiceTypeListener);
	 
	            HBox hbox = new HBox();
	            hbox.setSpacing(5.0);
	            hbox.getChildren().add(fieldName);
	            hbox.getChildren().add(fieldYear);
	            hbox.getChildren().add(choiceType);
	            setGraphic(hbox);
	        }
	    }
	 
	    private final static class ChoiceTypeListener 
	    	implements ChangeListener<Wine$Type> {
	 
	        private ChoiceBox<Wine$Type> choiceBox;
	 
	        public ChoiceTypeListener(ChoiceBox<Wine$Type> choiceBox) {
	            this.choiceBox = choiceBox;
	        }
	 
	        @Override
	        public void changed(ObservableValue<? extends Wine$Type> property,
	                Wine$Type oldValue, Wine$Type newValue) {
	            choiceBox.getSelectionModel().select(newValue);
	        }
	    }
	}
}

package org.jax.cgadb.web.main;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.jax.base.WTBaseBackingBean;
import org.jax.cgadb.dao.MolecularProfileResponseDAO;
import org.jax.cgadb.dao.TherapyDAO;
import org.jax.cgadb.dao.VocabularyDAO;
import org.jax.cgadb.dto.DrugDTO;
import org.jax.cgadb.dto.EfficacyEvidenceDTO;
import org.jax.cgadb.dto.MolecularProfileResponseDTO;
import org.jax.cgadb.dto.ReferenceDTO;
import org.jax.cgadb.dto.TherapyDTO;

@ManagedBean
@SessionScoped
public class MolecularProfileResponseBean extends WTBaseBackingBean implements Serializable {
	private static final long serialVersionUID = 1L;

	// for searching MP Response
	private String searchMProfileName;
	private String searchTherapyName;
	private String searchResponseType;
	private String searchIndication;
	
	// for therapy select panel
	private String myTherapyName;
	private String selectedTherapyName;
	private ArrayList<String> therapyDisplayNames = new ArrayList<String>();
	private HashMap<String, String> therapyNamesHash = new HashMap<>();	// hash of displayName:therapyName for retrieval 
	
	private ArrayList<String> indicationsList;
	
    // for the profile response table
	private List<MolecularProfileResponseDTO> mpResponseDTOsList;
	private MolecularProfileResponseDTO selectedMPResponse;
	private String workspaceLegendText;
	private Boolean showProfileResponses = false;
	// row counts for profile response table
	private int numProfileResponses;

	private Boolean showSelectedMPResponse = false;
	
	private MolecularProfileResponseDAO mpResponseDAO = new MolecularProfileResponseDAO();
	
	public MolecularProfileResponseBean() {
		super();
		
		// indications list is very large, so pre-loading it once.
		VocabularyDAO vdao = new VocabularyDAO();
		indicationsList = vdao.getIndicationList();

		// get molecular profile and therapy names from session parameter, if present.
		infuseMPTherapyNames();
		// therapyDisplayNames = new ArrayList<String>();
	}
		
	public void infuseMPTherapyNames() {
		// get molecular profile and therapy names from session parameter, if present
		String mpName = (String)this.getSessionParameter("molecularProfileName");
		if (mpName != null && !mpName.isEmpty()) {
			this.setSearchMProfileName(mpName);
		}
		String tName = (String)this.getSessionParameter("therapyName");
		if (tName != null && !tName.isEmpty()) {
			this.setSearchTherapyName(tName);
		} 
//		this.loadMolecularProfileResponse();
	}
	
	public List<String> completeIndication(String startString) {  
		ArrayList<String> resultIndications = new ArrayList<String>();
		for (String ind : indicationsList) {
			if (ind.toUpperCase().contains(startString.toUpperCase())) {
				resultIndications.add(ind);
			}
		}
		return resultIndications;    
	}	
	
	public List<String> completeMolecularProfile(String filter) {  
		List<String> results = new ArrayList<String>();
		results = mpResponseDAO.getMolProfileNames("%"+filter+"%");
        return results;  
    }
	
	public List<String> completeTherapy(String filter) {  
		List<String> results = new ArrayList<String>();
		results = mpResponseDAO.getTherapyNames("%"+filter+"%");
        return results;  
    }
	
	public void selectTherapyValueChangeListener() {
		String nameStr = this.getMyTherapyName();
		
		try {
			// therapiesList = new TherapyDAO().findTherapiesByName(nameStr);
			
			// clear display names list and has for new search
			therapyDisplayNames.clear();
			therapyNamesHash.clear();
			List<TherapyDTO> tDTOsList = new TherapyDAO().findTherapiesByName(nameStr);
			if (tDTOsList != null && tDTOsList.size() > 0) {
				// create display name strings for list box
				for (TherapyDTO dto : tDTOsList) {
					String displayName = dto.getTherapy_name() + " : [";
					for (DrugDTO drug : dto.getDrugs()) {
						displayName += drug.getDrug_name() + " ,";
					}
					// remove the last comma
					displayName = displayName.substring(0, displayName.length()-1) + "]";
					if (therapyNamesHash.get(displayName) == null) {
						therapyNamesHash.put(displayName, dto.getTherapy_name());
					}
					therapyDisplayNames.add(displayName);
				}
			} else {
				addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to find therapies associated "
					+ "with search string '" + nameStr + "'.", null));
			}
		} catch (SQLException sqle) {
			addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error finding therapies associated "
					+ "with search string '" + nameStr + "'."+ sqle, null));
		}
	}
	
	
	public void createNewMolecularProfileResponse() {
		this.setSelectedMPResponse(new MolecularProfileResponseDTO());
		this.getSelectedMPResponse().setProfile_id(0);
		this.getSelectedMPResponse().setTherapy_id(0);
		this.getSelectedMPResponse().setIndication_id(0);
		this.getSelectedMPResponse().setResponse_type(new String());
		this.getSelectedMPResponse().setUpdate_by(this.getLoggedUser());
		this.getSelectedMPResponse().setCreate_by(this.getLoggedUser());
		this.getSelectedMPResponse().setProfile_name(new String());
		this.getSelectedMPResponse().setTherapy_name(new String());
		this.getSelectedMPResponse().setIndication_name(new String());
		this.getSelectedMPResponse().setResponse_type(new String());
		this.setShowSelectedMPResponse(true);
	}
	
	public void deleteMolecularProfileResponse(ActionEvent event) {
		// System.out.println("Delete Profile Response");

		// delete the response associated with rowindex where delete button was clicked
		// (can be different from the one that is selected in response workspace (selectedMPResponseDTO)
		Integer rowIndex = (Integer)event.getComponent().getAttributes().get("deleteResponseRowIndex");
		MolecularProfileResponseDTO deleteMPRdto = this.getMpResponseDTOsList().get(rowIndex);
		
		try {
			// it is possible that efficacy evidences in response UI were deleted (and stored in deleteQueue), but not from database. 
			// Need to remove those first before deleting profile response
			if (deleteMPRdto.getDeleteQueue() != null && deleteMPRdto.getDeleteQueue().size() > 0) {
				mpResponseDAO.deleteEfficacyEvidences(deleteMPRdto.getDeleteQueue());
			}
			this.mpResponseDAO.deleteMolecularProfileResponse(deleteMPRdto.getResponse_id());
			addMessage("profileResponsesTableMessage", new FacesMessage(FacesMessage.SEVERITY_INFO, "Deleted Profile Response <" + deleteMPRdto.getProfile_name() + 
					" : " + deleteMPRdto.getTherapy_name() + " : " + deleteMPRdto.getIndication_name() + " : " + deleteMPRdto.getResponse_type() + "> from database.", null));

			// force table update (otherwise, it updates using lingering mprDTOs, which could display inconsistent rows)
			this.setMpResponseDTOsList(mpResponseDAO.getMolecularProfileResponses(getSearchMProfileName(), getSearchTherapyName(), getSearchIndication(), getSearchResponseType(), false));	
			this.numProfileResponses = mpResponseDTOsList.size();
			// if selected response and deleted response were same, reset profile response workspace and hide it
			if (deleteMPRdto.getResponse_id() == this.getSelectedMPResponse().getResponse_id()) {
				createNewMolecularProfileResponse();
				this.setShowSelectedMPResponse(false);
			}
		} catch (SQLException sqle) {
			addMessage("profileResponsesTableMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Deletion of profile response failed. "+ sqle, null));
		}
	}
	
	public String getDeleteResponseMessage() {
		return "Delete all " + this.getSelectedMPResponse().getNumberEfficacyEvidences() + " Efficacy Evidences before deleting profile response";
	}
	
	public String navigateToQueryEEs() {
    		// Call variant bean to hide variant detail
		EfficacyEvidenceBean eeBean = (EfficacyEvidenceBean) this.getSessionParameter("efficacyEvidenceBean");
		if (eeBean == null) {
			eeBean = new EfficacyEvidenceBean();
		}
		eeBean.setMyTherapyName(this.getSearchTherapyName());
		eeBean.setMyProfileName(this.getSearchMProfileName());
		eeBean.setMyResponseType(this.getSearchResponseType());
		eeBean.handleSelectedTherapy();
		// eeBean.setMyIndication(this.getIndication());
		eeBean.setShowTherapyEfficacyEvidences(false);
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("efficacyEvidenceBean", eeBean);
		
		return "therapyQuery";
	}
	
	public void displayProfileResponses(ActionEvent event) {
		if ((this.getSearchTherapyName() == null || this.getSearchTherapyName().isEmpty()) 
				&& (this.getSearchMProfileName() == null  || this.getSearchMProfileName().isEmpty()) 
				&& (this.getSearchIndication() == null || this.getSearchIndication().isEmpty()) 
				&& (this.getSearchResponseType() == null || this.getSearchResponseType().isEmpty())) {
				addMessage("profileResponsesSelectionMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Please specify at least one search criterion.", null));			
				return;
		}

		this.setMpResponseDTOsList(mpResponseDAO.getMolecularProfileResponses(getSearchMProfileName(), getSearchTherapyName(), getSearchIndication(), getSearchResponseType(), false));	
		this.numProfileResponses = mpResponseDTOsList.size();
		this.setShowProfileResponses(true);
		// reset Selected profile response vars
		// this.setShowSelectedMPResponse(false);
	}
	
	public void displaySelectedProfileResponse(ActionEvent event) {
		MolecularProfileResponseDTO response = (MolecularProfileResponseDTO)event.getComponent().getAttributes().get("attrSelectedResponse");
		if (response == null) {
			addMessage("profileResponsesTableMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No such response. Please select another response to display.", null));
			return;
		}
		/*
		 *  DO NOT set 'response' as selectedMPResponse directly, since any change made to selectedMPResponse
		 *  in response workspace will alter the object in the MPRDTOsList and update the table even if
		 *  response is not saved in response workspace.
		 *  Instead, use this response_id and retrieve from db to set as selectedMPResponse and display in 
		 *  response workspace.
		 *  The table is forcibly updated when a save is performed on reponse edit/create.
		 */
		MolecularProfileResponseDTO mprDTOFromDb = mpResponseDAO.getMolecularProfileResponseById(response.getResponse_id()); 
		this.setSelectedMPResponse(mprDTOFromDb);
		this.setShowSelectedMPResponse(true);
	}

	public void selectTherapy(ActionEvent event) {
//		TherapyDTO therapy = (TherapyDTO)event.getComponent().getAttributes().get("attrSelectedTherapy");
//		// check for blank drug DTO (since while clearing the add drug panel, selected drug is set to blank dto.
//		// Allowing choice of this blank dto will add a blank column to table, which we don't want).
//		if (therapy != null)  { 
		
		String therapyDisplayName = (String)event.getComponent().getAttributes().get("attrSelectedTherapyName");
		if (therapyDisplayName != null && !therapyDisplayName.isEmpty()) {
			this.getSelectedMPResponse().setTherapy_name(therapyNamesHash.get(therapyDisplayName));    
			this.resetSearchTherapyDialog();
		} else {
			addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "No therapy name was selected in the 'Select Therapy' dialog. Please select a therapy name.", null));  
		}
	}

	public void cancelSelectTherapy(ActionEvent event) {
		resetSearchTherapyDialog();
	}

	private void resetSearchTherapyDialog() {
		// clear select therapy panel
		this.setMyTherapyName(new String());
		this.setTherapyDisplayNames(new ArrayList<String>());
// 		this.setTherapiesList(new ArrayList<TherapyDTO>());
	}
	
	public void saveMolecularProfileResponse(ActionEvent event) {
		// System.out.println("saveProfileResponseEdit ActionEvent ");
		Boolean isValid = true;
		
		if (this.getSelectedMPResponse() == null) {
			addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile response object is null.", null));  
			isValid = false;
		}
		
		// Required field check
		String therapyName = this.getSelectedMPResponse().getTherapy_name();
		if (therapyName == null || therapyName.isEmpty() 
				|| this.getSelectedMPResponse().getIndication_name() == null || this.getSelectedMPResponse().getIndication_name().isEmpty() 
				|| this.getSelectedMPResponse().getResponse_type() == null || this.getSelectedMPResponse().getResponse_type().isEmpty()) {
			addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Therapy name, response type, indication are required for a profile response.", null));  
			isValid = false;
		}
		
		//  check for 'duplicate' response (if db has another response with same
		//  'profile-therapy-indication' combo but different response type - not allowed)
		int response_id = getSelectedMPResponse().getResponse_id();
//		if (response_id == 0) {
			ArrayList<MolecularProfileResponseDTO> mprDTOs = mpResponseDAO.getMolecularProfileResponses(this.getSelectedMPResponse().getProfile_name(), this.getSelectedMPResponse().getTherapy_name(), this.getSelectedMPResponse().getIndication_name(), null, true);
			if (mprDTOs != null && mprDTOs.size() > 0) {
				// if there is a response, there should be only one, not more
				if (mprDTOs.size() == 1) {
					MolecularProfileResponseDTO mprDTO = mprDTOs.get(0);
					// selected profile response should not be the same as the response retrieved for given conditions - check response-Ids.
					if (response_id != mprDTO.getResponse_id()) {
						if (!mprDTO.getResponse_type().equals(this.getSelectedMPResponse().getResponse_type())) {
							addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile response with "
									+ "specified therapy, profile and indication already exists with response type '" + mprDTO.getResponse_type() 
									+ "'. Cannot have another profile response of the same combination with response type '" + getSelectedMPResponse().getResponse_type() + "'.", null));
							isValid = false;
						} else {
							// exact same profile-therapy-indication-response type already exists, cannot have another
							addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile response with "
									+ "specified therapy, profile, indication and response type already exists (response id : '" + mprDTO.getResponse_id() + "'). Cannot have another profile response with the same combination. "
									+ "Please choose a different profile/therapy/indication.", null));
							isValid = false;
						}
					}
				} else {
					addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot have more than one profile response "
							+ "with given profile-therapy-indication combo.", null));
					isValid = false;
				}
			}
//		} 

		
		if (this.getSelectedMPResponse().getEfficacyEvidences() != null)
			for (EfficacyEvidenceDTO evidence : this.getSelectedMPResponse().getEfficacyEvidences()) {
				evidence.setDefaultColor();
				if (evidence.getEfficacy_evidence() == null || evidence.getEfficacy_evidence().trim().isEmpty()) {
					addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Please enter an efficacy evidence.", null));  
					isValid = false;
					evidence.setRed();
				}
				if (evidence.getEfficacy_evidence() != null && evidence.getEfficacy_evidence().length() > 512) {
					addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, 
						 "Efficacy Evidence exceeds the 512 character limit, please shorten it.", null));
					isValid = false;
					evidence.setRed();
				}
				if (evidence.getEfficacy_evidence() != null && isDuplicateEvidence(evidence.getEfficacy_evidence())) {
					addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Duplicate efficacy evidence found.  Please enter unique efficacy evidence.", null));  
					isValid = false;
					evidence.setRed();
				}
				if (!this.isReference(evidence)) {
					addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "To save changes, please select a reference from the filtered options.", null));  
					isValid = false;
				}
			}
		
		// Save  molecular profile response
		if (isValid) {
			try {
				// set update user for DTO
				this.getSelectedMPResponse().setUpdate_by(this.getLoggedUser());
				if (this.getSelectedMPResponse().getResponse_id() == 0) {
					// set create user for DTO
					this.getSelectedMPResponse().setCreate_by(this.getLoggedUser());
					Integer pk = mpResponseDAO.insertMolecularProfileResponse(this.getSelectedMPResponse());
					this.getSelectedMPResponse().setResponse_id(pk.intValue());
				} else {
					// ----  ALLOW USER TO EDIT PROFILE-THERAPY-INDICATION for MP Response : added by anu 9/22/2014 --- //
					/*  IGNORE COMMENTS FOR NOW
					 *  The only item that can be updated in a profile response is 'response type'. 
					 *  If any of profile, therapy or indication is changed, it becomes a new response (since there is a constraint on profile-therapy-indication combo)
					 *  So to update, need to explicitly set response type on profileResponseDTO.
					 */
					// TODO: need to check if therapy/profile/indication has changed and warn user?
					// END IGNORE
					
					mpResponseDAO.updateMolecularProfileResponse(this.getSelectedMPResponse());
				}
				// force table update (otherwise, it updates using lingering mprDTOs, which could display inconsistent rows)
				this.setMpResponseDTOsList(mpResponseDAO.getMolecularProfileResponses(getSearchMProfileName(), getSearchTherapyName(), getSearchIndication(), getSearchResponseType(), false));	
				this.numProfileResponses = mpResponseDTOsList.size();
				// success message
				addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_INFO, "Profile response efficacy evidence edits saved. ", null));
			} catch (SQLException se) {
				addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile response efficacy evidence save failed. "+ se.getMessage(), null));  
				isValid = false;
			}
		}
	}
	
	private boolean isDuplicateEvidence(String efficacy_evidence) {
		int count = 0;
		for (EfficacyEvidenceDTO edto : this.getSelectedMPResponse().getEfficacyEvidences()) {
			if (edto.getEfficacy_evidence().equalsIgnoreCase(efficacy_evidence))
				count++;
		}
		
		return (count > 1);
	}

	public void cancelEfficacyEvidenceEdit() {
		// System.out.println("cancelResponseEdit ActionEvent ");
		this.getSelectedMPResponse().cancelSave();
		// restore original response 
		if (this.getSelectedMPResponse().getResponse_id() > 0) { 
			MolecularProfileResponseDTO mprdto = mpResponseDAO.getMolecularProfileResponseById(this.getSelectedMPResponse().getResponse_id());
			this.setSelectedMPResponse(mprdto);
		} else {
			createNewMolecularProfileResponse();
		}

		// this.loadMolecularProfileResponse();
		addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_INFO, "Profile response edits cancelled. ", null));  
	}

	public void addEfficacyEvidence(ActionEvent event) {
		// System.out.println("Add efficacy evidence");
		this.getSelectedMPResponse().addEfficacyEvidence(this.getLoggedUser());
	}
		
	public void deleteEfficacyEvidence(ActionEvent event)  {
		// System.out.println("Delete efficacy evidence");
		Integer rowIndex = (Integer)event.getComponent().getAttributes().get("deleteEvidenceRowIndex");
		try {
			this.getSelectedMPResponse().removeEfficacyEvidence(rowIndex.intValue());
		} catch (SQLException sqle) {
			addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Efficacy evidence delete failed. "+ sqle, null));
		}
	}
	
	public void clearEfficacyEvidenceReference(ActionEvent event) {
		// System.out.println("Clear efficacy evidence reference");
		Integer effEvRowIndex = (Integer)event.getComponent().getAttributes().get("clearEffEvidenceRowIndex");
		Integer refRowIndex = (Integer)event.getComponent().getAttributes().get("clearReferenceRowIndex"); 
		try {
			this.getSelectedMPResponse().clearEfficacyEvidenceReference(effEvRowIndex.intValue(), refRowIndex.intValue());
		} catch (SQLException sqle) {
			addMessage("evidenceEditMessage", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Clear reference failed. "+ sqle, null));
		}
	}

	public void addReferenceRow(ActionEvent event) {
		// System.out.println("Add reference row");
		Integer rowIndex = (Integer)event.getComponent().getAttributes().get("effEvidenceRowIndex");
		this.getSelectedMPResponse().getEfficacyEvidences().get(rowIndex).addReference(this.getLoggedUser());
	}
	
	/**
	 * This method is called from ReferenceBean method updateParentView() after a reference is modified by a user 
	 * working with a molecular profile response description.  This view has an option to cancel so
	 * we just can't reload the page, instead ReferenceBean is returning the update and 
	 * must be updated locally here.  This way our user will see any changes made  
	 * to PMID, URL and Title.
	 * @param descRowIndex
	 * @param refRowIndex
	 * @param refDTO
	 */
	public void updateViewReference(int descRowIndex, int refRowIndex, ReferenceDTO refDTO) {
		this.getSelectedMPResponse().getEfficacyEvidences().get(descRowIndex).getReferences().get(refRowIndex).setReference(refDTO);
	}
	
	private void addMessage(String arg0, FacesMessage message){
		FacesContext.getCurrentInstance().addMessage(arg0, message);
	}

	// ============== GETTERS, SETTERS

	// Used to disable the Select Therapy button
	public Boolean getDisableSelectTherapy() {
		// return false (enable Select Therapy button) if selectedMPResponse response_id = 0 && therapyName is empty
		// return true (disable Select Therapy button) otherwise
		Boolean bDisabled = true;
		if (this.getSelectedMPResponse().getResponse_id() == 0 || 
				(this.getSelectedMPResponse().getTherapy_name() == null || this.getSelectedMPResponse().getTherapy_name().isEmpty())) {
			bDisabled = false;
		} else {
			bDisabled = true;
		}
		return bDisabled;
	}
	
	public String getWorkspaceLegendText() {
		this.workspaceLegendText = "Molecular Profile Response Workspace for response_id : ";
		int response_id = this.getSelectedMPResponse().getResponse_id();
		if (response_id == 0) {
			this.workspaceLegendText += "New";
		} else {
			this.workspaceLegendText += response_id;
		}
		return workspaceLegendText;
	}

	
	public String getSearchMProfileName() {
		return searchMProfileName;
	}

	public void setSearchMProfileName(String searchMProfileName) {
		this.searchMProfileName = searchMProfileName;
	}

	public String getSearchTherapyName() {
		return searchTherapyName;
	}

	public void setSearchTherapyName(String searchTherapyName) {
		this.searchTherapyName = searchTherapyName;
	}

	public String getSearchResponseType() {
		return searchResponseType;
	}

	public void setSearchResponseType(String searchResponseType) {
		this.searchResponseType = searchResponseType;
	}

	public String getSearchIndication() {
		return searchIndication;
	}

	public void setSearchIndication(String searchIndication) {
		this.searchIndication = searchIndication;
	}

	public List<MolecularProfileResponseDTO> getMpResponseDTOsList() {
		return mpResponseDTOsList;
	}

	public void setMpResponseDTOsList(
			List<MolecularProfileResponseDTO> mpResponseDTOsList) {
		this.mpResponseDTOsList = mpResponseDTOsList;
	}

	public MolecularProfileResponseDTO getSelectedMPResponse() {
		return selectedMPResponse;
	}

	public void setSelectedMPResponse(MolecularProfileResponseDTO selectedMPResponse) {
		this.selectedMPResponse = selectedMPResponse;
	}

	public Boolean getShowProfileResponses() {
		return showProfileResponses;
	}

	public void setShowProfileResponses(Boolean showProfileResponses) {
		this.showProfileResponses = showProfileResponses;
	}

	public int getNumProfileResponses() {
		return numProfileResponses;
	}

	public void setNumProfileResponses(int numProfileResponses) {
		this.numProfileResponses = numProfileResponses;
	}

	public Boolean getShowSelectedMPResponse() {
		return showSelectedMPResponse;
	}

	public void setShowSelectedMPResponse(Boolean showSelectedMPResponse) {
		this.showSelectedMPResponse = showSelectedMPResponse;
	}

	public String getMyTherapyName() {
		return myTherapyName;
	}

	public void setMyTherapyName(String myTherapyName) {
		this.myTherapyName = myTherapyName;
	}

	public ArrayList<String> getTherapyDisplayNames() {
		return therapyDisplayNames;
	}
	
	public void setTherapyDisplayNames(ArrayList<String> therapyDisplayNames) {
		this.therapyDisplayNames = therapyDisplayNames;
	}
	
	public String getSelectedTherapyName() {
		return selectedTherapyName;
	}
	
	public void setSelectedTherapyName(String selectedTherapyName) {
		this.selectedTherapyName = selectedTherapyName;
	}

}

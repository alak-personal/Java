package org.jax.cgadb.dto;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.jax.base.ITBaseDTO;
import org.jax.cgadb.dao.MolecularProfileResponseDAO;

public class MolecularProfileResponseDTO extends ITBaseDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int response_id;
	private int profile_id;
	private int therapy_id;
	private int indication_id;
	private String response_type = "";
	
	private ArrayList<EfficacyEvidenceDTO> efficacyEvidences = new ArrayList<EfficacyEvidenceDTO>();
	private EfficacyEvidenceDTO selectedEfficacyEvidence = null;
	private ArrayList<EfficacyEvidenceDTO> deleteQueue = new ArrayList<EfficacyEvidenceDTO>(); 

	private String create_by;
	private Date create_date;
	private String update_by;
	private Date update_date;

	private String therapy_name;
	private String profile_name;
	private String indication_name;
	
	public MolecularProfileResponseDTO() {
		super();
	}
	
	public MolecularProfileResponseDTO(int response_id) {
		this.setResponse_id(response_id);
	}
	
	public int getNumberEfficacyEvidences() {
		if (this.getEfficacyEvidences() == null) 
			return 0;
		else
			return this.getEfficacyEvidences().size(); 
	}
	
	public boolean equals(MolecularProfileResponseDTO dto) {
		//System.out.println(this.getClass().getName() + " checking dto equality"); 
		return (this.getTherapy_id() == dto.getTherapy_id() && this.getResponse_type().equalsIgnoreCase(dto.getResponse_type())
				&& this.getProfile_id() == dto.getProfile_id() && this.indication_id == dto.getIndication_id());
	}
	
	
	public void addEfficacyEvidence(String user) {
//		if (this.getEfficacyEvidences() == null)
//			this.setEfficacyEvidences(new ArrayList<EfficacyEvidenceDTO>());
		this.getEfficacyEvidences().add(0, new EfficacyEvidenceDTO());
		this.getEfficacyEvidences().get(0).setCreate_by(user);
		this.getEfficacyEvidences().get(0).setUpdate_by(user);
	}
	
	public void removeEfficacyEvidence(int index) throws SQLException {
		EfficacyEvidenceDTO description = this.getEfficacyEvidences().get(index);
		if (description.getAnnotation_id() > 0)
			deleteQueue.add(description);
		this.getEfficacyEvidences().remove(index);
	}
	
	public void clearEfficacyEvidenceReference(int eeIndex, int refIndex) throws SQLException {
		EfficacyEvidenceDTO efficacyEvidence = this.getEfficacyEvidences().get(eeIndex);
		efficacyEvidence.getReferences().remove(refIndex);
	}
	
	public void deleteEfficacyEvidences() throws SQLException {
		MolecularProfileResponseDAO dao = new MolecularProfileResponseDAO();
		dao.deleteEfficacyEvidences(this.deleteQueue);
		this.deleteQueue = new ArrayList<EfficacyEvidenceDTO>();
	}
	
	public void cancelSave() {
		this.deleteQueue = new ArrayList<EfficacyEvidenceDTO>();
	}
	
//	================================
	
	public int getResponse_id() {
		return response_id;
	}

	public void setResponse_id(int response_id) {
		this.response_id = response_id;
	}

	public int getProfile_id() {
		return profile_id;
	}

	public void setProfile_id(int profile_id) {
		this.profile_id = profile_id;
	}

	public int getTherapy_id() {
		return therapy_id;
	}

	public void setTherapy_id(int therapy_id) {
		this.therapy_id = therapy_id;
	}

	public int getIndication_id() {
		return indication_id;
	}

	public void setIndication_id(int indication_id) {
		this.indication_id = indication_id;
	}

	public String getResponse_type() {
		return response_type;
	}

	public void setResponse_type(String response_type) {
		this.response_type = response_type;
	}

	public String getCreate_by() {
		return create_by;
	}

	public void setCreate_by(String create_by) {
		this.create_by = create_by;
	}

	public Date getCreate_date() {
		return create_date;
	}

	public void setCreate_date(Date create_date) {
		this.create_date = create_date;
	}

	public String getUpdate_by() {
		return update_by;
	}

	public void setUpdate_by(String update_by) {
		this.update_by = update_by;
	}

	public Date getUpdate_date() {
		return update_date;
	}

	public void setUpdate_date(Date update_date) {
		this.update_date = update_date;
	}

	public ArrayList<EfficacyEvidenceDTO> getEfficacyEvidences() {
		return efficacyEvidences;
	}

	public void setEfficacyEvidences(
			ArrayList<EfficacyEvidenceDTO> efficacyevidences) {
		this.efficacyEvidences = efficacyevidences;
	}

	public EfficacyEvidenceDTO getSelectedEfficacyEvidence() {
		return selectedEfficacyEvidence;
	}

	public void setSelectedEfficacyEvidence(EfficacyEvidenceDTO selectedEfficacyEvidence) {
		this.selectedEfficacyEvidence = selectedEfficacyEvidence;
	}

	public ArrayList<EfficacyEvidenceDTO> getDeleteQueue() {
		return deleteQueue;
	}

	public String getTherapy_name() {
		return therapy_name;
	}

	public void setTherapy_name(String therapy_name) {
		this.therapy_name = therapy_name;
	}

	public String getProfile_name() {
		return profile_name;
	}

	public void setProfile_name(String profile_name) {
		this.profile_name = profile_name;
	}

	public String getIndication_name() {
		return indication_name;
	}

	public void setIndication_name(String indication_name) {
		this.indication_name = indication_name;
	}

}

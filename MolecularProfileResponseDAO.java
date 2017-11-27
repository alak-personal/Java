package org.jax.cgadb.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.jstl.sql.Result;

import org.jax.base.dao.Connector;
import org.jax.base.dao.MySQLDAO;
import org.jax.cgadb.dto.CurationTaskDTO;
import org.jax.cgadb.dto.DescriptionReferenceDTO;
import org.jax.cgadb.dto.EfficacyEvidenceDTO;
import org.jax.cgadb.dto.MolecularProfileResponseDTO;

public class MolecularProfileResponseDAO extends MySQLDAO {
	private static final long serialVersionUID = 1L;
	
	public MolecularProfileResponseDAO() {
		super();
	}

	public MolecularProfileResponseDTO getMolecularProfileResponseById(int responseId) {
		ArrayList<String> filter = new ArrayList<String>();
		filter.add(Integer.toString(responseId));
		
		/* String theQuery = "SELECT * FROM profile_response WHERE response_id = ? ORDER BY response_id" ;
		 	
		 	String theQuery = "SELECT pr.*, mp.profile_name, t.therapy_name, d.name as indication "
				+ " FROM profile_response pr "
				+ " JOIN molecular_profile mp ON mp.profile_id = pr.profile_id "
				+ " JOIN therapy t ON t.therapy_id = pr.therapy_id "
				+ " JOIN disease_ontology d ON d.id = pr.indication_id "
				+ " WHERE response_id = ? ORDER BY response_id";
		*/
		
		String theQuery = "SELECT DISTINCT response_id, profile_id, profile_name, therapy_id, therapy_name, "
				+ " indication_id, indication, response_type, create_by, create_date, update_by, update_date "
				+ " FROM cga_profile_response_vw WHERE response_id = ? ORDER BY response_id";
		
		Result result = this.executePreparedQuery(Connector.CGA_DS, theQuery, filter);
		if (result != null && result.getRowCount() > 0) {
			return this.toMolecularProfileResponseArray(result).get(0);
		}
		return null;
	}
	
	/**
	 *  This method is used in 2 different circumstances : when tname should be an exact match for therapy name or 
	 *  when tname can be a therapy name, drug name, synonym or trade name. bExactTherapyName arg is used while constructing
	 *  the SQL query while including the clause for therapy name.
	 *  	
	 *  	bExactTherapyName = TRUE (exact match with therapy name) used during save when trying to determine if there is already 
	 *  	a response with the same profile-therapy-indication combo. SQL WHERE clause for therapy : therapy_name=tname 
	 *  
	 *     	bExactTherapyName = FALSE used when trying to display ALL responses for a given tName (matching 
	 *     	therapy/drug name/synonym/trade name). Used to display responses in table and update table in delete and save methods.
	 *     SQL WHERE clause for therapy : (therapy_name=tNAme OR drug_name=tName OR drug_syns=tName OR trade_name=tName)
	 *     								
	 *  
	 * @param pname
	 * @param tname
	 * @param indication
	 * @param responseType
	 * @param bExactTherapyName
	 * @return
	 */
	public ArrayList<MolecularProfileResponseDTO> getMolecularProfileResponses(String pname, String tname, String indication, String responseType, boolean bExactTherapyName) {
		ArrayList<String> filter = new ArrayList<String>();
		String theQuery = "SELECT DISTINCT response_id, therapy_name, therapy_id, indication, indication_id, "
				+ " profile_name, profile_id, response_type, create_by, create_date, update_by, update_date FROM "
				+ " cga_therapy_response_vw WHERE ";
		
		// 'therapy_name' 
		if (tname != null && !tname.isEmpty()) {
			// if bExactTherapyName, we need to match exact therapy, no need for drug related clauses
			if (bExactTherapyName) {
				filter.add(tname);		// for therapy name
				theQuery += " therapy_name = ? " ;
			} else {
				filter.add(tname);		// for therapy name
				filter.add(tname);		// for drug name name
				filter.add(tname);		// for drug synonym name
				filter.add(tname);		// for trade name
				theQuery += " (therapy_name = ? OR drug_name = ? OR trade_name = ? OR drug_syns = ?) " ;
			}
		} 
		
		// 'profile_name' 
		if (pname != null && !pname.isEmpty()) {
			filter.add(pname);		// for profile name
			// 'AND' clause only if therapy name was not empty
			if (tname != null && !tname.isEmpty()) {
				theQuery += " AND ";
			}
			theQuery += " profile_name = ? ";
		} 

		// indication
		if (indication != null && !indication.isEmpty()) {
			filter.add(indication);		// for indication name
			// 'AND' clause only if therapy name, profile name not empty
			if ((tname != null && !tname.isEmpty()) || (pname != null && !pname.isEmpty())) {
				theQuery += " AND ";
			}
			theQuery += " indication = ? ";
		} 
		
		// response type
		if (responseType != null && !responseType.isEmpty()) {
			filter.add(responseType);		// for response type
			// 'AND' clause  if any of therapy name, profile name, indication not empty
			if ((tname != null && !tname.isEmpty()) || (pname != null && !pname.isEmpty()) || (indication != null && !indication.isEmpty())) {
				theQuery += " AND ";
			}
			theQuery += " response_type = ? ";
		} 
		
		theQuery += " ORDER BY therapy_name";
        System.out.println("Query:"+theQuery);
        
		Result result = this.executePreparedQuery(Connector.CGAREADERDS, theQuery, filter);
        return this.toMolecularProfileResponseArray(result);
	}

	private ArrayList<MolecularProfileResponseDTO> toMolecularProfileResponseArray(Result result) {
		ArrayList<MolecularProfileResponseDTO> list = new ArrayList<MolecularProfileResponseDTO>();
		if (result != null) {
			 for (SortedMap row : result.getRows()) {
				 MolecularProfileResponseDTO dto = this.createMolecularProfileResponseDTO(row);
				 list.add(dto);
	         }
			 // System.out.println(list.size()+" responses found");
		}
		
		return list;
	}

	private MolecularProfileResponseDTO createMolecularProfileResponseDTO(SortedMap row) {
		MolecularProfileResponseDTO dto = new MolecularProfileResponseDTO();
		dto.setResponse_id(myGetInt("response_id", row));
		dto.setTherapy_name(myGet("therapy_name", row));
		dto.setTherapy_id(myGetInt("therapy_id", row));
		dto.setProfile_name(myGet("profile_name", row));
		dto.setProfile_id(myGetInt("profile_id", row));
		String indication_name = myGet("indication", row);
		// from getMPResponse(response_id) method, the indication name col name is 'name', not 'indication'
		if (indication_name == null || indication_name.isEmpty()) {
			indication_name = myGet("name", row);
		}
		dto.setIndication_name(indication_name);
		dto.setIndication_id(myGetInt("indication_id", row));
		dto.setResponse_type(myGet("response_type", row));
		
		dto.setCreate_by(myGet("create_by", row));
		dto.setCreate_date(myGetDate("create_date", row));
		dto.setUpdate_by(myGet("update_by", row));
		dto.setUpdate_date(myGetDate("update_date", row));
		
		dto.setEfficacyEvidences(getEfficacyEvidences(dto));

		return dto;
	}

	public Integer insertMolecularProfileResponse(MolecularProfileResponseDTO dto) throws SQLException {
        int pk = 0;
        String cmd = "INSERT INTO profile_response (profile_id, therapy_id, indication_id, response_type, "
    				+ "create_by, create_date, update_by, update_date) "
    				+ "VALUES ( "
    				+ " (SELECT profile_id FROM molecular_profile WHERE profile_name=?),"
    				+ " (SELECT therapy_id FROM therapy WHERE therapy_name=?), "
    				+ " (SELECT id FROM cga_indication_vw WHERE name=?),"
    				+ " ?, ?, ?, ?, ?) ";

		Connection con = null;
        try {
			con = this.getConnection(Connector.CGA_DS);
			if (con == null) {
	        		System.err.println("MySQLDAO.executeQuery - Database connection return null");
	        		return null;
			}
	        java.sql.Timestamp date = new java.sql.Timestamp(System.currentTimeMillis());
			con.setAutoCommit(false);
	        
	        PreparedStatement pstmt = con.prepareStatement(cmd, PreparedStatement.RETURN_GENERATED_KEYS);
	        pstmt.setString(1, dto.getProfile_name());
	        pstmt.setString(2, dto.getTherapy_name());
	        pstmt.setString(3, dto.getIndication_name());
	        pstmt.setString(4, dto.getResponse_type());
	        pstmt.setString(5, dto.getCreate_by());
	        pstmt.setTimestamp(6, date);
	        pstmt.setString(7, dto.getUpdate_by());
	        pstmt.setTimestamp(8, date);
	
	        pstmt.addBatch();
	        pstmt.executeBatch();
	        
	        ResultSet rs = pstmt.getGeneratedKeys();
	    		for (int i=0;rs.next(); i++ ) { 
	    			pk = rs.getInt(1);
	    			// System.out.println("i:"+ i +" therapy key:"+ rs.getInt(1));
	    			break;
	  		}
	
	    		con.commit();
	    		this.closeConnection(con);
	    } catch (SQLException ex) {
	        Logger.getLogger(MySQLDAO.class.getName()).log(Level.SEVERE, null, ex);  
	        this.closeConnection(con);
	        // return null;
	        throw new SQLException("Error while saving molecular profile response : "  + ex.getMessage());
	    }
        
        if (pk > 0) {
            Result result = this.executeQuery(Connector.CGA_DS, "SELECT MAX(response_id) as response_id FROM profile_response");
            pk = Integer.parseInt(this.toStringArray(result, "response_id").get(0));
            dto.setResponse_id(pk);
            this.saveEfficacyEvidence(dto);
        }

        return pk;
	}	
	
	public Integer updateMolecularProfileResponse(MolecularProfileResponseDTO dto) throws SQLException {
        String cmd = "UPDATE profile_response SET "
        		+ " profile_id = (SELECT profile_id FROM molecular_profile WHERE profile_name=?), "
        		+ " therapy_id = (SELECT therapy_id FROM therapy WHERE therapy_name=?), "
        		+ " indication_id = (SELECT id FROM cga_indication_vw WHERE name=?), "
        		+ " response_type = ?, update_by = ?, update_date = ? "
    			+ " WHERE response_id = " + dto.getResponse_id() ;
        
		Connection con = null;
		Integer key = 0;

		try {
			con = this.getConnection(Connector.CGA_DS);
			if (con == null) {
	        		System.err.println("MySQLDAO.executeQuery - Database connection return null");
	        		return null;
			}
	        java.sql.Timestamp date = new java.sql.Timestamp(System.currentTimeMillis());
			con.setAutoCommit(false);
	        
	        PreparedStatement pstmt = con.prepareStatement(cmd, PreparedStatement.RETURN_GENERATED_KEYS);
	        pstmt.setString(1, dto.getProfile_name());
	        pstmt.setString(2, dto.getTherapy_name());
	        pstmt.setString(3, dto.getIndication_name());
	        pstmt.setString(4, dto.getResponse_type());
	        pstmt.setString(5, dto.getUpdate_by());
	        pstmt.setTimestamp(6, date);
	
	        pstmt.addBatch();
	        pstmt.executeBatch();
	        
	        ResultSet rs = pstmt.getGeneratedKeys();
    			for (int i=0;rs.next(); i++ ) { 
		        key = rs.getInt(1);
	    			// System.out.println("i:"+ i +" therapy key:"+ rs.getInt(1));
	    			break;
      		}
	
	    		con.commit();
	    		this.closeConnection(con);
	     } catch (SQLException ex) {
	        Logger.getLogger(MySQLDAO.class.getName()).log(Level.SEVERE, null, ex);  
	        this.closeConnection(con);
	        // return null;
	        throw new SQLException("Error while updating molecular profile response : "  + ex.getMessage());
	    }

        dto.deleteEfficacyEvidences();
        this.saveEfficacyEvidence(dto);
		
		return key;
	}

	public Integer deleteMolecularProfileResponse(int response_id) throws SQLException {
		Integer count = 0;
		String cmd = "DELETE FROM profile_response "
				+ " WHERE response_id = "+ response_id ;
		count = this.executeUpdate(Connector.CGA_DS, cmd);
		
		return count;
	}

	public ArrayList<EfficacyEvidenceDTO> getEfficacyEvidences(MolecularProfileResponseDTO dto) {
		String cmd = "SELECT * FROM response_efficacy_evidence WHERE response_id = "+ dto.getResponse_id() ;
		Result result = this.executeQuery(Connector.CGA_DS, cmd);
		return this.toEvidenceDescriptionArray(result);
	}
	
	private ArrayList<EfficacyEvidenceDTO> toEvidenceDescriptionArray(Result result) {
		ArrayList<EfficacyEvidenceDTO> list = new ArrayList<EfficacyEvidenceDTO>();
		if (result != null) {
			 for (SortedMap row : result.getRows()) {
				 EfficacyEvidenceDTO dto = this.createEvidenceEfficacyEvidenceDTO(row);
				 list.add(dto);
	         }
			 // System.out.println(list.size()+" efficacy evidences found");
		}
		
		return list;
	}
	
	private EfficacyEvidenceDTO createEvidenceEfficacyEvidenceDTO(SortedMap row) {
		EfficacyEvidenceDTO dto = new EfficacyEvidenceDTO();
		dto.setAnnotation_id(myGetInt("annotation_id", row));
		dto.setItem(new MolecularProfileResponseDTO(myGetInt("response_id", row)));
		dto.setEfficacy_evidence(myGet("efficacy_evidence", row));
		dto.setApproval_status(myGet("approval_status", row));
		dto.setEvidence_type(myGet("evidence_type", row));

		dto.setReferences(this.getEfficacyEvidenceReferences(dto.getAnnotation_id()));

		dto.setCreate_by(myGet("create_by", row));
		dto.setUpdate_by(myGet("update_by", row));
		dto.setCreate_date(myGetDate("create_date", row));
		dto.setUpdate_date(myGetDate("update_date", row));
		
        return dto;
    }

	public ArrayList<DescriptionReferenceDTO> getEfficacyEvidenceReferences(int annotationId) {
		String cmd = "SELECT * FROM efficacy_evidence_reference WHERE annotation_id = "+ annotationId ;
		Result result = this.executeQuery(Connector.CGA_DS, cmd);
		return new ReferenceDAO().toDescriptionReferenceArray(result);
	}

	private Integer saveEfficacyEvidence(MolecularProfileResponseDTO prDTO) throws SQLException {
		Integer count = 0;
		if (prDTO.getEfficacyEvidences() != null) {
			for (EfficacyEvidenceDTO evidence : prDTO.getEfficacyEvidences()) {
				evidence.setItem(prDTO);
				evidence.setUpdate_by(prDTO.getUpdate_by());
				if (evidence.getAnnotation_id() == 0) {
					evidence.setCreate_by(prDTO.getCreate_by());
					count = this.insertEfficacyEvidence(evidence);
					evidence.setAnnotation_id(count);
				} else {
					count = this.updateEfficacyEvidence(evidence);
				}
			}
			// save references
			new ReferenceDAO().saveEfficacyDescriptionReferences(prDTO.getEfficacyEvidences());
		}
		return count;
	}
		
	private Integer insertEfficacyEvidence(EfficacyEvidenceDTO dto) throws SQLException {
	    Connection con = null;
	    Integer key = 0;
	    String cmd = "INSERT INTO response_efficacy_evidence (response_id, efficacy_evidence, approval_status, evidence_type, "
    				+ "create_by, create_date, update_by, update_date) "
    				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " ;
	    try {
	        con = this.getConnection(Connector.CGA_DS);
	        if (con == null) {
	        		System.err.println("MySQLDAO.executeQuery - Database connection return null");
	        		return null;
	        }
	        java.sql.Timestamp date = new java.sql.Timestamp(System.currentTimeMillis());
    			con.setAutoCommit(false);
	        
	        PreparedStatement pstmt = con.prepareStatement(cmd, PreparedStatement.RETURN_GENERATED_KEYS);
	        pstmt.setInt(1, ((MolecularProfileResponseDTO)dto.getItem()).getResponse_id());
	        pstmt.setString(2, dto.getEfficacy_evidence());
	        pstmt.setString(3, dto.getApproval_status());
	        pstmt.setString(4, dto.getEvidence_type());
	        pstmt.setString(5, dto.getCreate_by());
	        pstmt.setTimestamp(6, date);
	        pstmt.setString(7, dto.getUpdate_by());
	        pstmt.setTimestamp(8, date);

	        pstmt.addBatch();
	        pstmt.executeBatch();
	        
	        ResultSet rs = pstmt.getGeneratedKeys();
	    		for (int i=0;rs.next(); i++ ) { 
	    			key = rs.getInt(1);
	    			// System.out.println("i:"+ i +" evidence key:"+ rs.getInt(1));
	    			break;
      		}

	    		con.commit();
	    		this.closeConnection(con);
	        return key;
	     } catch (SQLException ex) {
	        Logger.getLogger(MySQLDAO.class.getName()).log(Level.SEVERE, null, ex);  
	        this.closeConnection(con);
	        // return null;
	        throw new SQLException("Error while saving efficacy evidences : "  + ex.getMessage());
	    }
	}
	
	private Integer updateEfficacyEvidence(EfficacyEvidenceDTO dto) throws SQLException {
		Connection con = null;
        String cmd = "UPDATE response_efficacy_evidence SET "
        		+ " response_id = ?, efficacy_evidence = ?, approval_status = ?, evidence_type = ?, update_by = ?, update_date = ?" 
        		+ "WHERE annotation_id = " + dto.getAnnotation_id();
        		
    	    try {
    	        con = this.getConnection(Connector.CGA_DS);
    	        if (con == null) {
        	        	System.err.println("MySQLDAO.executeQuery - Database connection return null");
        	        	return null;
    	        }
    	        java.sql.Timestamp date = new java.sql.Timestamp(System.currentTimeMillis());
        		con.setAutoCommit(false);
    	        
    	        PreparedStatement pstmt = con.prepareStatement(cmd);
    	        pstmt.setInt(1, ((MolecularProfileResponseDTO) dto.getItem()).getResponse_id());
    	        pstmt.setString(2, dto.getEfficacy_evidence());
    	        pstmt.setString(3, dto.getApproval_status());
    	        pstmt.setString(4, dto.getEvidence_type());
    	        pstmt.setString(5, dto.getUpdate_by());
    	        pstmt.setTimestamp(6, date);

    	        pstmt.addBatch();
    	        pstmt.executeBatch();
    	        
        		con.commit();
        		this.closeConnection(con);
    	        return 1;
    	     } catch (SQLException ex) {
    	        Logger.getLogger(MySQLDAO.class.getName()).log(Level.SEVERE, null, ex);  
    	        this.closeConnection(con);
    	        // return null;
    	        throw new SQLException("Error while updating efficacy evidences : "  + ex.getMessage());
    	    }
	}
	
	public Integer deleteEfficacyEvidences(ArrayList<EfficacyEvidenceDTO> evidenceAry) throws SQLException {
		String ids = "0";
		for (EfficacyEvidenceDTO dto : evidenceAry) {
			ids += ","+ dto.getAnnotation_id();
		}
        String cmd = "DELETE FROM response_efficacy_evidence "
    			+ "WHERE annotation_id IN (" + ids + ")";
        
        Integer count = this.executeUpdate(Connector.CGA_DS, cmd);
        
        return count;
	}

	public List<String> getMolProfileNames(String mpNameFilter) {
		ArrayList<String> filter = new ArrayList<String>();
		filter.add(mpNameFilter);
		String theQuery = "SELECT DISTINCT profile_name FROM molecular_profile WHERE profile_name LIKE ? ORDER BY profile_name" ;
		Result result = this.executePreparedQuery(Connector.CGA_DS, theQuery, filter);
		
		ArrayList<String> therapies = new ArrayList<String>();
		if (result != null) {
			 for (SortedMap row : result.getRows()) {
				 therapies.add(myGet("profile_name", row));   
	         }
		}
		
		return therapies;
	}

	public List<String> getTherapyNames(String tNameFilter) {
		ArrayList<String> filter = new ArrayList<String>();
		filter.add(tNameFilter);	// for therapy name
		filter.add(tNameFilter);	// for drug name
		filter.add(tNameFilter);	// for trade name
		filter.add(tNameFilter);	// for drug synonyms
		
		String theQuery = "SELECT DISTINCT therapy_name FROM therapy WHERE therapy_name LIKE ? "
				+ " UNION " 
				+ " (SELECT distinct drug_name FROM drug WHERE drug_name LIKE ? )"
				+ " UNION " 
				+ " (SELECT distinct trade_name FROM drug WHERE trade_name LIKE ?  AND trade_name NOT LIKE '%Not Available%') "
				+ " UNION "
				+ " (SELECT distinct synonyms FROM drug WHERE synonyms LIKE ?  AND synonyms NOT LIKE '%Not Available%') "
				+ " ORDER BY therapy_name" ;
		Result result = this.executePreparedQuery(Connector.CGA_DS, theQuery, filter);
		
		ArrayList<String> therapies = new ArrayList<String>();
		if (result != null) {
			 for (SortedMap row : result.getRows()) {
				 therapies.add(myGet("therapy_name", row));   
	         }
		}
		
		return therapies;

	}

	public ArrayList<String> createMaintenanceCurationTasks() throws SQLException {
		Connection con = null;
		con = this.getConnection(Connector.CGA_DS);
		if (con == null) {
			System.err.println("MySQLDAO.executeQuery - Database connection return null");
		}
		con.setAutoCommit(false);
		ArrayList<CurationTaskDTO> eeCurationTasks = new ArrayList<CurationTaskDTO>();
		// for a better message
		ArrayList<String> eeTaskIds = new ArrayList<String>();
		
		// find efficacy evidences that are older than 3 months
		// String cmd = "SELECT response_id, efficacy_evidence FROM response_efficacy_evidence WHERE DATE(update_date) <= DATE(NOW() - INTERVAL 3 MONTH)";
		
		String cmd = "SELECT ree.response_id, ree.efficacy_evidence, "
				+ " (SELECT profile_name FROM molecular_profile WHERE profile_id = (SELECT profile_id FROM profile_response WHERE response_id=ree.response_id)) AS profile_name "
				+ " FROM efficacy_evidence_reference eer "
				+ " JOIN response_efficacy_evidence ree ON ree.annotation_id = eer.annotation_id "
				+ " JOIN reference r ON r.reference_id = eer.reference_id "
				+ " WHERE DATE(ree.update_date) <= DATE(NOW() - INTERVAL 3 MONTH) AND r.url LIKE '%https://clinicaltrials.gov/%'";
        
		PreparedStatement pstmt = con.prepareStatement(cmd);
        pstmt.execute();
        
        ResultSet rs = pstmt.getResultSet();
        
        // found some efficacy evidences, get response id and profile name and create curation task
        if (rs.isBeforeFirst()) {
            PreparedStatement pstmt1;
            ResultSet rs1;
            for (int i=0;rs.next(); i++ ) {
                int responseId = rs.getInt(1);
                String effEvidence = rs.getString(2);
                String profileName = rs.getString(3);
                String curationId = profileName + " " + responseId;

                // @anul: 5/20/2015: removed the check for EE string in notes, since that is not a robust check
                // (if EE has been modified, the notes in the task and the EE <will> differ)

                // check if a curation task already exists for this profileName <space> responseId and not already checked-in;
                // (For an EE task, it is ok to have multiple tasks for same EE (since the EE comes up for maintenance every 3 months,
                // but all but one should be checked-in).
                cmd = "SELECT task_id  FROM curation_task WHERE curation_id = ? and curation_status not in ('check-in')";
                pstmt1 = con.prepareStatement(cmd);
                pstmt1.setString(1, curationId);

                pstmt1.execute();
                rs1 = pstmt1.getResultSet();

                if (!rs1.isBeforeFirst()) {
                    // no task found for EE that is NOT CHECKED-IN, add curation task
                    CurationTaskDTO eeTask = new CurationTaskDTO(CurationTaskDTO.TASK_TYPE_MAINTENANCE, CurationTaskDTO.DATA_ELEMENT_EFFICACY_EVIDENCE, curationId, "blank");
                    // set efficacy evidence text as notes
                    eeTask.setNotes(effEvidence);
                    eeCurationTasks.add(eeTask);
                    eeTaskIds.add(curationId);
                }
                // if a curation task is found for this EE that is not checked in, no need to add another task
	  		}       		
	    }

	    // now insert the curation tasks into db
		if (eeCurationTasks.size() > 0) {
			try {
				// save curation tasks
				cmd = "INSERT INTO curation_task (task_type, data_element, curation_id, notes, curation_status, "
						+ " create_by, create_date, update_by, update_date) "
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";
				
				pstmt = con.prepareStatement(cmd, PreparedStatement.RETURN_GENERATED_KEYS);
				// for the moment, use 'anul' as user for create_by, update_by. Update_by will be updated when curator/editor make changes
				String user = "anul";
				for (CurationTaskDTO ctDTO : eeCurationTasks) {
					pstmt.setString(1, ctDTO.getTask_type());
					pstmt.setString(2, ctDTO.getData_element());
			        pstmt.setString(3, ctDTO.getCuration_id());
			        pstmt.setString(4, ctDTO.getNotes());
			        pstmt.setString(5, ctDTO.getCuration_status());
			        pstmt.setString(6, user);
			        java.sql.Timestamp date = new java.sql.Timestamp(System.currentTimeMillis());
			        pstmt.setTimestamp(7, date);
			        pstmt.setString(8, user);
			        pstmt.setTimestamp(9, date);
			        
			        pstmt.addBatch();
				}
				
		        pstmt.executeBatch();
	        
			} catch (SQLException ex) {
				Logger.getLogger(MySQLDAO.class.getName()).log(Level.SEVERE, null, ex);  
				this.closeConnection(con);
				throw new SQLException("Error while saving efficacy evidence curation tasks : "  + ex.getMessage());
			}
		}
		
		// everything was successful ....
		con.commit();
		this.closeConnection(con);
		return eeTaskIds;
	
	}

}

package edu.villanova.cqtools.workflow;
 
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.ParticipantStepChooser;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component(label="Path Based Particpant Chooser",description="Particpant Chooser Implementation based on payload path.",metatype=true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Path Based Participant Chooser"),
        @Property(name = ParticipantStepChooser.SERVICE_PROPERTY_LABEL, value = "Path Based Participant Chooser",description="Choosed participant based on path as configured.")})
public class PathBasedParticipantChooser implements ParticipantStepChooser {
 
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Property(unbounded=PropertyUnbounded.ARRAY,description="Mappings for workflow. Example: /path/to/folder:approver_group_id",label="Payload Mappings")
    public static final String PAYLOAD_MAPPINGS = "payload.mappings";
    
    @Property(unbounded=PropertyUnbounded.DEFAULT,description="Default group if the payload path does not match any of the mappings.",value="administrators",label="Default Group")
    private static final String DEFAULT_GROUP = "default.group";
    
    private final String ADMIN_GROUP = "administrators";
    
    //Configuration properties
    private String defaultGroup;
    private List<String[]> payloadMappings = new ArrayList<String[]>();
    
    private String payloadPath;
    private JackrabbitSession session;

    private String participant;
    
    public String getParticipant(WorkItem arg0, WorkflowSession arg1, MetaDataMap arg2) throws WorkflowException {
    	this.setSession((JackrabbitSession) arg1.getSession());
        
        logger.info("Workflow Model: " + arg0.getWorkflow().getWorkflowModel().getTitle());
        logger.info("Path Based Participant Step initiated by {}",arg0.getWorkflow().getInitiator());
        
        if (arg0.getWorkflowData().getPayloadType().equals("JCR_PATH")) {
            this.setPayloadPath(arg0.getWorkflowData().getPayload().toString());
            logger.info("Selecting particpant for {}",this.getPayloadPath());
            this.setParticipant(this.chooseParticipant());
            
	    	try {
				if (this.getSession().getUserManager().getAuthorizable(this.getParticipant()) == null) {
					logger.info("Selected group " + this.getParticipant() + " does not exist. Rerouting to default group " + this.ADMIN_GROUP + ".");
					this.setParticipant(this.getDefaultGroup());
				}
				logger.info("Selected group: " + this.getParticipant());
			} catch (AccessDeniedException e) {
				logger.error("Access denied while selecting participant. " + e.getMessage());
				e.printStackTrace();
			} catch (UnsupportedRepositoryOperationException e) {
				e.printStackTrace();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
            	
        } else {
        	logger.info("Workflow type: " + arg0.getWorkflowData().getPayloadType() + " was not processed");
        }
        
        return this.getParticipant();
    }
    
    /**
     * Determines participant based on the payloadPath
     * @return ID of group to assign the workflow to
     */
    private String chooseParticipant() {
    	Iterator<String[]> mapIterator = this.getPayloadMappings().iterator();
    	String[] matchedMapping = null;
    	
    	while (mapIterator.hasNext()) {
    		// [0] = path, [1] =  approval group
    		String[] currentMapping = mapIterator.next();
    		
    		//Check for a mapping that matched the payload
    		if (this.getPayloadPath().startsWith(currentMapping[0])) {
    			matchedMapping = currentMapping;
    			
    			//Check for any matching subdirectories
    			while (mapIterator.hasNext()) {
                    currentMapping = mapIterator.next();
                    if (currentMapping[0].startsWith(matchedMapping[0]) && this.getPayloadPath().startsWith(currentMapping[0])) {
                        matchedMapping = currentMapping;
                    } 
                }
    		}
    	}  
    	
    	return (matchedMapping == null ? this.getDefaultGroup() : matchedMapping[1]);

    }
    
    @SuppressWarnings("unused")
    @Activate
    private void activate(final ComponentContext componentContext) {
        
    	BundleContext bundleContext = componentContext.getBundleContext();
    	
    	final Dictionary<?,?> properties = componentContext.getProperties();
    	
    	// default.group
    	this.defaultGroup = PropertiesUtil.toString(properties.get(DEFAULT_GROUP), this.ADMIN_GROUP);
    	
    	// payload.mappings
    	String[] mappings = PropertiesUtil.toStringArray(properties.get(PAYLOAD_MAPPINGS));
    	for (String mapping : mappings) {
    		this.getPayloadMappings().add(mapping.trim().split(":"));
    	}

    }


	/**
	 * @return the payloadPath
	 */
	public String getPayloadPath() {
		return payloadPath;
	}

	/**
	 * @param payloadPath the payloadPath to set
	 */
	public void setPayloadPath(String payloadPath) {
		this.payloadPath = payloadPath;
	}

	/**
	 * @return the session
	 */
	public JackrabbitSession getSession() {
		return session;
	}

	/**
	 * @param session the session to set
	 */
	public void setSession(JackrabbitSession session) {
		this.session = session;
	}

	/**
	 * @return the participant
	 */
	public String getParticipant() {
		return (participant==null || participant.trim().equals("") ) ? this.getDefaultGroup() : participant;
	}

	/**
	 * @param participant the participant to set
	 */
	public void setParticipant(String participant) {
		this.participant = participant;
	}

	/**
	 * @return the defaultGroup
	 */
	public String getDefaultGroup() {
		return defaultGroup;
	}

	/**
	 * @param defaultGroup the defaultGroup to set
	 */
	public void setDefaultGroup(String defaultGroup) {
		this.defaultGroup = defaultGroup;
	}

	/**
	 * @return the payloadMappings
	 */
	public List<String[]> getPayloadMappings() {
		return payloadMappings;
	}

	/**
	 * @param payloadMappings the payloadMappings to set
	 */
	public void setPayloadMappings(List<String[]> payloadMappings) {
		this.payloadMappings = payloadMappings;
	}

}

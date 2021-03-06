package cz.metacentrum.perun.core.impl.modules.attributes;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.Member;
import cz.metacentrum.perun.core.api.Resource;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.ConsistencyErrorException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.api.exceptions.WrongReferenceAttributeValueException;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.implApi.modules.attributes.ResourceMemberAttributesModuleAbstract;
import cz.metacentrum.perun.core.implApi.modules.attributes.ResourceMemberAttributesModuleImplApi;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Michal Stava stavamichal@gmail.com
 */
public class urn_perun_member_resource_attribute_def_def_filesLimit extends ResourceMemberAttributesModuleAbstract implements ResourceMemberAttributesModuleImplApi {

	private static final String A_R_defaultFilesLimit = AttributesManager.NS_RESOURCE_ATTR_DEF + ":defaultFilesLimit";
	private static final String A_R_defaultFilesQuota = AttributesManager.NS_RESOURCE_ATTR_DEF + ":defaultFilesQuota";
	private static final String A_MR_filesQuota = AttributesManager.NS_MEMBER_RESOURCE_ATTR_DEF + ":filesQuota";

	@Override
	public void checkAttributeValue(PerunSessionImpl perunSession, Resource resource, Member member, Attribute attribute) throws InternalErrorException, WrongAttributeValueException, WrongReferenceAttributeValueException, WrongAttributeAssignmentException {
		Attribute attrFilesQuota = null;
		Integer filesQuota = null;
		Integer filesLimit = null;

		//Get FilesQuotaAttribute
		try {
			attrFilesQuota = perunSession.getPerunBl().getAttributesManagerBl().getAttribute(perunSession, resource, member, A_MR_filesQuota);
		} catch (AttributeNotExistsException ex) {
			throw new ConsistencyErrorException("Attribute with filesQuota from member " + member.getId() + " and resource " + resource.getId() + " could not obtained.", ex);
		}

		//Get FilesLimit value
		if(attribute.getValue() != null) {
			filesLimit = (Integer) attribute.getValue();
		} else {
			try {
				attribute = perunSession.getPerunBl().getAttributesManagerBl().getAttribute(perunSession, resource, A_R_defaultFilesLimit);
			} catch (AttributeNotExistsException ex) {
				throw new ConsistencyErrorException("Attribute with defaultFilesLimit from resource " + resource.getId() + " could not obtained.", ex);
			}
			if(attribute != null && attribute.getValue() != null) {
				filesLimit = (Integer) attribute.getValue();
			}
		}
		if(filesLimit != null && filesLimit < 0) throw new WrongAttributeValueException(attribute, resource, member, attribute + " cannot be less than 0.");

		//Get FilesQuota value
		if(attrFilesQuota != null &&  attrFilesQuota.getValue() != null) {
			filesQuota = (Integer) attrFilesQuota.getValue();
		} else if(attrFilesQuota == null || attrFilesQuota.getValue() == null) {
			try {
				attrFilesQuota = perunSession.getPerunBl().getAttributesManagerBl().getAttribute(perunSession, resource, A_R_defaultFilesQuota);
			} catch (AttributeNotExistsException ex) {
				throw new ConsistencyErrorException("Attribute with defaultFilesQuota from resource " + resource.getId() + " could not obtained.", ex);
			}
			if(attrFilesQuota != null || attrFilesQuota.getValue() != null) {
				filesQuota = (Integer) attrFilesQuota.getValue();
			}
		}
		if(filesQuota != null && filesQuota < 0) throw new ConsistencyErrorException(attrFilesQuota + " cannot be less than 0.");

		//Compare FilesLimit with FilesQuota
		if(filesQuota == null || filesQuota == 0) {
			if(filesLimit != null && filesLimit != 0) throw new WrongReferenceAttributeValueException(attribute, attrFilesQuota, resource, member, resource, null, "Try to set limited limit, but there is still set unlimited Quota.");
		} else if((filesQuota != null && filesQuota != 0) && (filesLimit != null && filesLimit != 0))  {
			if(filesLimit < filesQuota) throw new WrongReferenceAttributeValueException(attribute, attrFilesQuota, resource, member, resource, null, attribute + " must be more than or equals to " + attrFilesQuota);
		}
	}

	@Override
	public List<String> getDependencies() {
		List<String> dependecies = new ArrayList<String>();
		dependecies.add(A_R_defaultFilesLimit);
		dependecies.add(A_R_defaultFilesQuota);
		return dependecies;
	}

	@Override
	public AttributeDefinition getAttributeDefinition() {
		AttributeDefinition attr = new AttributeDefinition();
		attr.setNamespace(AttributesManager.NS_MEMBER_RESOURCE_ATTR_DEF);
		attr.setFriendlyName("filesLimit");
		attr.setDisplayName("Files limit");
		attr.setType(Integer.class.getName());
		attr.setDescription("Hard quota for number of files.");
		return attr;
	}
}

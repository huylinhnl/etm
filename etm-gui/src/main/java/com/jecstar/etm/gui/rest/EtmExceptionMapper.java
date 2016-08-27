package com.jecstar.etm.gui.rest;


import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.EtmPrincipalRole;

@Provider
public class EtmExceptionMapper implements ExceptionMapper<Throwable> {

	@Override
	public Response toResponse(Throwable ex) {
		ErrorMessage errorMessage = new ErrorMessage();
		if (ex instanceof EtmException) {
			EtmException etmException = (EtmException) ex;
			switch(etmException.getErrorCode()) {
				case EtmException.WRAPPED_EXCEPTION:
					errorMessage.setMessage(getRootCauseMessage(ex));
					break;
				case EtmException.INVALID_LICENSE_KEY_EXCEPTION:
					errorMessage.setMessage("Invalid license key");
					break;
				case EtmException.LICENSE_EXPIRED_EXCEPTION:
					errorMessage.setMessage("License expired");
					break;
				case EtmException.CONFIGURATION_LOAD_EXCEPTION:
					errorMessage.setMessage("Error loading configuration");
					break;
				case EtmException.UNMARSHALLER_CREATE_EXCEPTION:
					errorMessage.setMessage("Error creating unmarshaller");
					break;
				case EtmException.INVALID_XPATH_EXPRESSION:
					errorMessage.setMessage("Invalid xpath expression");
					break;
				case EtmException.INVALID_XSLT_TEMPLATE:
					errorMessage.setMessage("Invalid xslt template");
					break;
				case EtmException.INVALID_JSON_EXPRESSION:
					errorMessage.setMessage("Invalid json path (not definite)");
					break;
				case EtmException.INVALID_EXPRESSION_PARSER_TYPE:
					errorMessage.setMessage("Invalid expression parser type");
					break;
				case EtmException.INVALID_PASSWORD:
					errorMessage.setMessage("Invalid password");
					break;
				case EtmException.NO_MORE_ADMINS_LEFT:
					errorMessage.setMessage("No users with the '" + EtmPrincipalRole.ADMIN.getRoleName() + "' role left");
					break;
				case EtmException.IIB_CONNECTION_ERROR:
					errorMessage.setMessage("Unable to connect to IIB node");
					break;
				default:
					break;
			}
			errorMessage.setCode(etmException.getErrorCode());
		} else {
			errorMessage.setCode(EtmException.WRAPPED_EXCEPTION);
			errorMessage.setMessage(getRootCauseMessage(ex));
		}
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMessage).type(MediaType.APPLICATION_JSON).build();
	}

	private String getRootCauseMessage(Throwable ex) {
		List<Throwable> stack = new ArrayList<Throwable>();
		return getRootCauseMessage(ex, stack);
	}

	private String getRootCauseMessage(Throwable ex, List<Throwable> stack) {
		stack.add(ex);
		if (ex.getCause() != null && !stack.contains(ex.getCause())) {
			return getRootCauseMessage(ex.getCause(), stack);
		}
		return ex.getMessage();
	}
	
	

}
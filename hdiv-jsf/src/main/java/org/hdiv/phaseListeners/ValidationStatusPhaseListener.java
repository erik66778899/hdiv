/**
 * Copyright 2005-2016 hdiv.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hdiv.phaseListeners;

import java.util.Collections;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hdiv.events.HDIVFacesEventListener;
import org.hdiv.filter.JsfValidatorHelper;
import org.hdiv.filter.ValidatorError;
import org.hdiv.filter.ValidatorErrorHandler;
import org.hdiv.logs.Logger;
import org.hdiv.util.HDIVUtil;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * {@link PhaseListener} that verifies that Hdiv validation is executed and is valid. Otherwise, the request will be stopped.
 */
public class ValidationStatusPhaseListener implements PhaseListener {

	private static final long serialVersionUID = -5951308353665763734L;

	private static final Log log = LogFactory.getLog(ValidationStatusPhaseListener.class);

	private Logger logger;

	private ValidatorErrorHandler validatorErrorHandler;

	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	public void beforePhase(final PhaseEvent event) {

		if (logger == null) {

			if (log.isDebugEnabled()) {
				log.debug("Initialize ValidationStatusPhaseListener dependencies.");
			}

			WebApplicationContext wac = FacesContextUtils.getRequiredWebApplicationContext(event.getFacesContext());
			logger = wac.getBean(Logger.class);
			validatorErrorHandler = wac.getBean(ValidatorErrorHandler.class);
		}

		if (event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION) || event.getPhaseId().equals(PhaseId.RENDER_RESPONSE)) {
			// Check if the request was validated

			Map<String, Object> params = event.getFacesContext().getExternalContext().getRequestMap();
			Boolean isRequestValidated = (Boolean) params.get(HDIVFacesEventListener.REQUEST_VALIDATED);
			Boolean isViewStateRequest = (Boolean) params.get(JsfValidatorHelper.IS_VIEW_STATE_REQUEST);

			if (isViewStateRequest && (isRequestValidated == null || !isRequestValidated)) {
				if (log.isErrorEnabled()) {
					log.error("This request is not validated.");
				}

				ValidatorError validatorError = log(event.getFacesContext());

				forwardToErrorPage(event.getFacesContext(), validatorError);
			}
		}

	}

	public void afterPhase(final PhaseEvent event) {
	}

	/**
	 * Helper method to write an attack in the log
	 * 
	 * @param context Request context
	 */
	private ValidatorError log(final FacesContext context) {

		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		ValidatorError errorData = new ValidatorError("REQUEST_NOT_VALIDATED", HDIVUtil.getRequestURI(request));
		logger.log(errorData);
		return errorData;
	}

	/**
	 * Redirects the execution to the HDIV error page
	 * 
	 * @param context Request context
	 * @param validatorError validation error data
	 */
	private void forwardToErrorPage(final FacesContext context, final ValidatorError validatorError) {

		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

		validatorErrorHandler.handleValidatorError(request, response, Collections.singletonList(validatorError));
	}

}

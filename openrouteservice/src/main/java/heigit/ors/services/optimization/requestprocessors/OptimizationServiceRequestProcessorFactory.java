/*
 *  Licensed to GIScience Research Group, Heidelberg University (GIScience)
 *
 *   http://www.giscience.uni-hd.de
 *   http://www.heigit.org
 *
 *  under one or more contributor license agreements. See the NOTICE file 
 *  distributed with this work for additional information regarding copyright 
 *  ownership. The GIScience licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in compliance 
 *  with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package heigit.ors.services.optimization.requestprocessors;

import javax.servlet.http.HttpServletRequest;

import heigit.ors.common.StatusCode;
import heigit.ors.exceptions.StatusCodeException;
import heigit.ors.exceptions.UnknownParameterValueException;
import heigit.ors.optimization.OptimizationErrorCodes;
import heigit.ors.routing.RoutingProfileManagerStatus;
import heigit.ors.services.matrix.MatrixServiceSettings;
import heigit.ors.services.optimization.OptimizationServiceSettings;
import heigit.ors.services.optimization.requestprocessors.json.JsonOptimizationRequestProcessor;
import heigit.ors.servlet.http.AbstractHttpRequestProcessor;

import com.graphhopper.util.Helper;

public class OptimizationServiceRequestProcessorFactory {

	public static AbstractHttpRequestProcessor createProcessor(HttpServletRequest request) throws Exception  
	{
		if (!(OptimizationServiceSettings.getEnabled() && MatrixServiceSettings.getEnabled()))
			throw new StatusCodeException(StatusCode.SERVICE_UNAVAILABLE, OptimizationErrorCodes.UNKNOWN,  "Optimizaiton service is not enabled.");

		if (!RoutingProfileManagerStatus.isReady())
			throw new StatusCodeException(StatusCode.SERVICE_UNAVAILABLE, OptimizationErrorCodes.UNKNOWN, "Optimizaiton service is not ready yet.");

		String formatParam = request.getParameter("format");

		if (Helper.isEmpty(formatParam))
			formatParam = "json";

		if (formatParam.equalsIgnoreCase("json"))
			return new JsonOptimizationRequestProcessor(request);
		else 
			throw new UnknownParameterValueException(OptimizationErrorCodes.INVALID_PARAMETER_VALUE, "format", formatParam);
	}
}

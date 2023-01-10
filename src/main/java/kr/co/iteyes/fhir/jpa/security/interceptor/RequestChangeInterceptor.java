package kr.co.iteyes.fhir.jpa.security.interceptor;

/*
 * #%L
 * HAPI FHIR - Client Framework
 * %%
 * Copyright (C) 2014 - 2022 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;

/**
 * HTTP interceptor to be used for adding HTTP basic auth username/password tokens
 * to requests
 * <p>
 * See the <a href="https://hapifhir.io/hapi-fhir/docs/interceptors/built_in_client_interceptors.html">HAPI Documentation</a>
 * for information on how to use this class.
 * </p>
 */
public class RequestChangeInterceptor implements IClientInterceptor {


	public RequestChangeInterceptor() {
	}

	@Override
	public void interceptRequest(IHttpRequest theRequest) {
//		theRequest.addHeader(Constants.HEADER_AUTHORIZATION, myHeaderValue);
		theRequest.removeHeaders(Constants.HEADER_ACCEPT_ENCODING);
		theRequest.addHeader(Constants.HEADER_ACCEPT_ENCODING, "lz4");
		theRequest.removeHeaders(Constants.HEADER_ACCEPT);
		theRequest.addHeader(Constants.HEADER_ACCEPT, "application/json+encrypt");
		theRequest.removeHeaders("Encrypt-Type");
		theRequest.addHeader("Encrypt-Type", "rsa");
	}

	@Override
	public void interceptResponse(IHttpResponse theResponse) throws IOException {
		// nothing
	}

}

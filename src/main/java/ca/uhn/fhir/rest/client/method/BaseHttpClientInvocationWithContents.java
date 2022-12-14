package ca.uhn.fhir.rest.client.method;

/*-
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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.client.api.IHttpClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.impl.BaseHttpClientInvocation;
import kr.co.iteyes.fhir.jpa.security.config.CryptoConfig;
import kr.co.iteyes.fhir.jpa.security.util.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author James Agnew
 * @author Doug Martin (Regenstrief Center for Biomedical Informatics)
 */
abstract class BaseHttpClientInvocationWithContents extends BaseHttpClientInvocation {

	private final BundleTypeEnum myBundleType;
	private final String myContents;
	private Map<String, List<String>> myIfNoneExistParams;
	private String myIfNoneExistString;
	private boolean myOmitResourceId = false;
	private Map<String, List<String>> myParams;
	private final IBaseResource myResource;
	private final List<IBaseResource> myResources;
	private final String myUrlPath;
	private IIdType myForceResourceId;


	public BaseHttpClientInvocationWithContents(FhirContext theContext, IBaseResource theResource, String theUrlPath) {
		super(theContext);
		myResource = theResource;
		myUrlPath = theUrlPath;
		myResources = null;
		myContents = null;
		myBundleType = null;
	}

	public BaseHttpClientInvocationWithContents(FhirContext theContext, List<? extends IBaseResource> theResources, BundleTypeEnum theBundleType) {
		super(theContext);
		myResource = null;
		myUrlPath = null;
		myResources = new ArrayList<>(theResources);
		myContents = null;
		myBundleType = theBundleType;
	}

	public BaseHttpClientInvocationWithContents(FhirContext theContext, Map<String, List<String>> theParams, String... theUrlPath) {
		super(theContext);
		myResource = null;
		myUrlPath = StringUtils.join(theUrlPath, '/');
		myResources = null;
		myContents = null;
		myParams = theParams;
		myBundleType = null;
	}

	public BaseHttpClientInvocationWithContents(FhirContext theContext, String theContents, boolean theIsBundle, String theUrlPath) {
		super(theContext);
		myResource = null;
		myUrlPath = theUrlPath;
		myResources = null;
		myContents = theContents;
		myBundleType = null;
	}

	public MymdLz4Util lz4Utils = new MymdLz4Util();

	String publicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApon6bkuuTvA8loXvOu+ZUC97U/0cfSbzz/m8etBaiLwTQaSVhnpPpQuQyb6RG2lH+wGVFQqauLqHlb9jxUyAIHkN+t9T0QjbkkkWtVlg74R3922UtNH9gjckFsKIGULhDi2xfLtJy6Cg0U4ZkXqSEKo0ZuOdw1NvBGsl8ujQD2uWwMw4TBUFtFt0xMajMUczDv5PNmO7yPIni0PCT2dbGJo3Kp5IChgvmUUbnWUmVuPmVxJtM0yUCUleFfHHmk/9xvMINu60wY1FMmzoXhStZa1jbIjPQufYHA/6wS37uoHs+Vtes+AiJNVlzpd9ZsRlapemeeVYejU4eb/NgDC/YQIDAQAB";


	@Override
	public IHttpRequest asHttpRequest(String theUrlBase, Map<String, List<String>> theExtraParams, EncodingEnum theEncoding, Boolean thePrettyPrint) throws DataFormatException {
		StringBuilder url = new StringBuilder();
//		System.out.println("asHttpRequest ?????? ?????????? = ");
		if (myUrlPath == null) {
			url.append(theUrlBase);
		} else {
			if (!myUrlPath.contains("://")) {
				url.append(theUrlBase);
				if (!theUrlBase.endsWith("/")) {
					url.append('/');
				}
			}
			url.append(myUrlPath);
		}

		appendExtraParamsWithQuestionMark(theExtraParams, url, url.indexOf("?") == -1);
		IHttpClient httpClient = getRestfulClientFactory().getHttpClient(url, myIfNoneExistParams, myIfNoneExistString, getRequestType(), getHeaders());

		if (myResource != null && IBaseBinary.class.isAssignableFrom(myResource.getClass())) {
			IBaseBinary binary = (IBaseBinary) myResource;
			if (isNotBlank(binary.getContentType()) && EncodingEnum.forContentTypeStrict(binary.getContentType()) == null) {
				if (binary.hasData()) {
					return httpClient.createBinaryRequest(getContext(), binary);
				}
			}
		}

		EncodingEnum encoding = theEncoding;
		if (myContents != null) {
			encoding = EncodingEnum.detectEncoding(myContents);
		}


		if (myParams != null) {
			return httpClient.createParamRequest(getContext(), myParams, encoding);
		}


		encoding = ObjectUtils.defaultIfNull(encoding,  EncodingEnum.JSON);
		String contents = encodeContents(thePrettyPrint, encoding);
		String contentType = getContentType(encoding);
		System.out.println("?????? ?????????? = " + encoding + " = " + contents);

		IvParameterSpec ivParameterSpec = AESCryptoUtil.getIv();

		String specName = CryptoConfig.CRYPTO_SPEC_NAME;
		byte[] cryptoByteKey = CryptoConfig.CRYPTO_BYTE_KEY;

		SecretKey secretKey = new SecretKeySpec(cryptoByteKey, 0 , cryptoByteKey.length, "AES");

		try{
//			System.out.println("originalContents = " + contents);
//			String encryptedContents = AESCryptoUtil.encrypt(specName, secretKey, ivParameterSpec, contents);
//			System.out.println("encryptedContents1 = " + encryptedContents);
//			String jsonContents = "{\"cryptedData\":\""+encryptedContents +"\"}";
//			System.out.println("jsonContents = " + jsonContents);
//			System.out.println("encryptedContents2 = " + encryptedContents);

//			String sampleBundle = "{\"resourceType\":\"Bundle\"}";
			String sampleBundle = contents;
			System.out.println("sampleBundle = " + sampleBundle);
			byte[] compressedBundle = lz4Utils.compress(sampleBundle.getBytes((StandardCharsets.UTF_8)));
			System.out.println("compressedBundle = " + compressedBundle);
			String strCompressedBundle = DatatypeConverter.printBase64Binary(compressedBundle);
			System.out.println("strCompressedBundle = " + strCompressedBundle);
//        byte[] decompressBundle = lz4Utils.decompress(Base64.getDecoder().decode(strCompressedBundle));
//        System.out.println("decompressBundle = " + new String(decompressBundle));
			try {
				String encryptedText = MymdRsaUtil.encrypt(strCompressedBundle, publicKey);
				System.out.println("encryptedText = " + encryptedText);
				return httpClient.createByteRequest(getContext(), encryptedText, contentType, encoding);
			}catch (Exception e){
				e.printStackTrace();
				return httpClient.createByteRequest(getContext(), contents, contentType, encoding);
			}
//            String encryptedText = RSAUtils.encryptRSA(new String(compressedBundle), publicKey.substring(0,8));




//			String decryptContents = AESCryptoUtil.decrypt(specName, secretKey, ivParameterSpec, encryptedContents);

//			System.out.println("decryptContents = " + decryptContents);
//			String decryptContents = AESCryptoUtil.decrypt(specName, secretKey, ivParameterSpec, encryptedContents);

//			return httpClient.createByteRequest(getContext(), jsonContents, contentType, encoding);

		}catch (Exception e){
			e.printStackTrace();
			return httpClient.createByteRequest(getContext(), contents, contentType, encoding);
		}

//		return httpClient.createByteRequest(getContext(), contents, contentType, encoding);
	}

	private String getContentType(EncodingEnum encoding) {
		if (getContext().getVersion().getVersion().isOlderThan(FhirVersionEnum.DSTU3)) {
			// application/xml+fhir
			return encoding.getResourceContentType();
		} else {
			// application/fhir+xml
			return encoding.getResourceContentTypeNonLegacy();
		}
	}

	/**
	 * Get the HTTP request type.
	 */
	protected abstract RequestTypeEnum getRequestType();

	private String encodeContents(Boolean thePrettyPrint, EncodingEnum encoding) {
		IParser parser;

		if (encoding == EncodingEnum.JSON) {
			parser = getContext().newJsonParser();
		} else {
			parser = getContext().newXmlParser();
		}

		if (thePrettyPrint != null) {
			parser.setPrettyPrint(thePrettyPrint);
		}

		if (myForceResourceId != null) {
			parser.setEncodeForceResourceId(myForceResourceId);
		}
		
		parser.setOmitResourceId(myOmitResourceId);
		if (myResources != null) {
			IVersionSpecificBundleFactory bundleFactory = getContext().newBundleFactory();
			bundleFactory.addTotalResultsToBundle(myResources.size(), myBundleType);
			bundleFactory.addResourcesToBundle(myResources, myBundleType, null, null, null);
			IBaseResource bundleRes = bundleFactory.getResourceBundle();
			return parser.encodeResourceToString(bundleRes);
		} else if (myContents != null) {
			return myContents;
		} else {
			return parser.encodeResourceToString(myResource);
		}
	}

	public void setForceResourceId(IIdType theId) {
		myForceResourceId = theId;
	}

	public void setIfNoneExistParams(Map<String, List<String>> theIfNoneExist) {
		myIfNoneExistParams = theIfNoneExist;
	}

	public void setIfNoneExistString(String theIfNoneExistString) {
		myIfNoneExistString = theIfNoneExistString;
	}

	public void setOmitResourceId(boolean theOmitResourceId) {
		myOmitResourceId = theOmitResourceId;
	}

}

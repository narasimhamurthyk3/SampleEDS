package com.ibm.rbs;



import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.ReferentialContainmentRelationshipSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.DynamicReferentialContainmentRelationship;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Factory.ContentElement;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Folder;

import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.core.UpdatingBatch;
import com.filenet.api.property.Properties;
import com.filenet.api.property.Property;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import javax.security.auth.Subject;
import javax.xml.bind.DatatypeConverter;

public class ReferenceRemoveAddCopyValues
{
	public static String result = null;
	private static final String characterSet = "UTF-8";
	private static String CE_URI = "http://localhost:9080/wsi/FNCEWS40MTOM";
	private static String userName = "p8admin";
	private static String password = "Password1";
	private static String jaasLoginModule = null;
	GetEnvrionmentPropertiesUtil getEnvrionmentPropertiesUtil = null;


	public String updateDocumentProperties(String documentName) {
		System.out.println("INSIDE UPDATEDOCUMENTPROPERTIES");
		Connection conn = Factory.Connection.getConnection(CE_URI);
		Subject subject = UserContext.createSubject(conn, userName, password, jaasLoginModule);
		UserContext.get().pushSubject(subject);
		Domain domain = Factory.Domain.fetchInstance(conn, null, null);
		String objectStoreName="OS1";
		ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, null);
		Document document = Factory.Document.fetchInstance(objectStore, new Id("{70BB4E78-0000-C317-B997-E2532C3A4746}"),null);		
		String DocumentName=document.getProperties().getStringValue("DocumentTitle");
		document.getProperties().putValue("DocumentTitle", "TEST11");
		document.save(RefreshMode.REFRESH);
		System.out.println("");
		System.out.println("Document Updated sucessfully");		
		return "Success";		
	}


	public String removeReferenceandAddObsoleteDocument(String oldCaseId, String newCaseId, String caseType, String lastMajorVersionOfDocumentId, String ProcedureNumber, String proceduredocumentClassName, String obsoletedocumentClassName, String objectStoreName)
	{
		try
		{
			this.getEnvrionmentPropertiesUtil = new GetEnvrionmentPropertiesUtil();

			byte[] decodedCE_URI = 
					DatatypeConverter.parseBase64Binary(this.getEnvrionmentPropertiesUtil
							.getFilenetCE_URI());
			CE_URI = new String(decodedCE_URI, "UTF-8");

			byte[] decodedUserName = 
					DatatypeConverter.parseBase64Binary(this.getEnvrionmentPropertiesUtil
							.getFilenetUserName());
			userName = new String(decodedUserName, "UTF-8");

			byte[] decodedPassword = 
					DatatypeConverter.parseBase64Binary(this.getEnvrionmentPropertiesUtil
							.getFilenetPassword());
			password = new String(decodedPassword, "UTF-8");

			jaasLoginModule = this.getEnvrionmentPropertiesUtil
					.getJaasLoginModule();
			Connection conn = Factory.Connection.getConnection(CE_URI);
			Subject subject = UserContext.createSubject(conn, userName, password, jaasLoginModule);
			UserContext.get().pushSubject(subject);
			Domain domain = Factory.Domain.fetchInstance(conn, null, null);
			ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, null);
			Document lastMajorVersionDocument = Factory.Document.getInstance(objectStore, proceduredocumentClassName, new Id(lastMajorVersionOfDocumentId));

			lastMajorVersionDocument.refresh();

			addReferenceOfDocument(objectStore, domain, oldCaseId, caseType, obsoletedocumentClassName, ProcedureNumber, lastMajorVersionDocument);
			lastMajorVersionDocument.refresh();

			removeReferenceOfDocument(oldCaseId, lastMajorVersionDocument);
		}
		catch (UnsupportedEncodingException unsupportedEncodingException)
		{
			unsupportedEncodingException.printStackTrace();
			System.out.println("Error in removeReferenceandAddObsoleteDocument:" + unsupportedEncodingException.getMessage());
			result = unsupportedEncodingException.getMessage();
			return result;
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			System.out.println("Error in removeReferenceandAddObsoleteDocument:" + exception.getMessage());
			result = exception.getMessage();
			return result;
		}
		return result = "Operation Completed Successfully";
	}

	private boolean removeReferenceOfDocument(String caseId, Document document)
	{
		boolean removeRef = false;
		try
		{
			document.refresh();
			String[] caseTypeName = caseId.split("_");
			String caseNumber = caseTypeName[2];
			System.out.println("caseNumber: " + caseNumber);
			ReferentialContainmentRelationshipSet containers = document.get_Containers();
			Iterator iterator = containers.iterator();
			while (iterator.hasNext())
			{
				ReferentialContainmentRelationship rcr = (ReferentialContainmentRelationship)iterator.next();
				PropertyFilter pf = new PropertyFilter();
				pf.addIncludeProperty(1, null, null, "ContainmentName", null);
				pf.addIncludeProperty(1, null, null, "Tail", null);
				rcr.fetchProperties(pf);
				Folder tail = (Folder)rcr.get_Tail();
				tail.refresh();
				String PathName = tail.get_PathName();
				System.out.println("path:" + PathName);
				if (PathName.contains(caseNumber))
				{
					System.out.println("PathName: " + PathName);
					System.out.println("caseNumber: " + caseNumber);
					rcr.delete();
					rcr.save(RefreshMode.NO_REFRESH);
					break;
				}
			}
			removeRef = true;
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			System.out.println("Error in removeReferenceOfDocument:" + exception.getMessage());
			removeRef = false;
		}
		return removeRef;
	}

	private boolean addReferenceOfDocument(ObjectStore objectStore, Domain domain, String oldCaseId, String caseType, String obsoletedocumentClass, String procedureNumber, Document lastMajorVersionDocument)
	{
		try
		{
			String tempPath = null;
			String folderPath = this.getEnvrionmentPropertiesUtil.getobsoleteWaterMarkDirectory() + "/" + oldCaseId + "/";

			String searchSQLFromCase = "select SubFolders from " + caseType + " where CmAcmCaseIdentifier='" + oldCaseId + "'";
			SearchSQL caseSQL = new SearchSQL(searchSQLFromCase);
			SearchScope caseSearchScope = new SearchScope(objectStore);
			FolderSet srcFolders = (FolderSet)caseSearchScope.fetchObjects(caseSQL, new Integer(50), null, Boolean.valueOf(true));
			Iterator itrIterator = srcFolders.iterator();
			Folder caseSubFolder = null;
			while (itrIterator.hasNext()) {
				caseSubFolder = (Folder)itrIterator.next();
			}
			caseSubFolder.refresh();

			Document obsoleteDocument = Factory.Document.createInstance(objectStore, obsoletedocumentClass);
			ContentTransfer ct = Factory.ContentTransfer.createInstance();

			tempPath = this.getEnvrionmentPropertiesUtil.getobsoleteWaterMarkDirectory() + "/" + oldCaseId + "/" + oldCaseId + ".docx";
			System.out.println("tempPath: " + tempPath);
			System.out.println("DocumentPath:" + tempPath);

			String documentName = procedureNumber + ".docx";
			InputStream is = new FileInputStream(tempPath);
			ct.setCaptureSource(is);
			ct.set_RetrievalName(documentName);
			ct.set_ContentType(lastMajorVersionDocument.get_MimeType());
			ContentElementList cel = Factory.ContentElement.createList();
			cel.add(ct);

			obsoleteDocument.getProperties().putValue("DocumentTitle", procedureNumber);
			obsoleteDocument.set_ContentElements(cel);
			obsoleteDocument.save(RefreshMode.REFRESH);
			obsoleteDocument.checkin(AutoClassify.AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			DynamicReferentialContainmentRelationship rcr = Factory.DynamicReferentialContainmentRelationship.createInstance((com.filenet.api.core.ObjectStore) objectStore, null, AutoUniqueName.AUTO_UNIQUE, 
					DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);

			rcr.set_Tail(caseSubFolder);
			rcr.set_Head(obsoleteDocument);
			rcr.set_ContainmentName("Document Uploaded");
			UpdatingBatch ub = UpdatingBatch.createUpdatingBatchInstance(domain, RefreshMode.REFRESH);
			ub.add(obsoleteDocument, null);
			ub.add(rcr, null);
			ub.updateBatch();

			obsoleteDocument.refresh();
			lastMajorVersionDocument.refresh();
			Property obsoleteProperty = null;
			Iterator obsoleteDocumentiterator = obsoleteDocument.getProperties().iterator();
			Properties sourceDocumentiterator = lastMajorVersionDocument.getProperties();

			String documentPropertyName = null;
			String[] chunks = lastMajorVersionDocument.getClassName().split("_");
			String Documentprefix = chunks[0];
			while (obsoleteDocumentiterator.hasNext())
			{
				obsoleteProperty = (Property)obsoleteDocumentiterator.next();
				if (obsoleteProperty.getPropertyName().contains(Documentprefix))
				{
					documentPropertyName = obsoleteProperty.getPropertyName();
					if (documentPropertyName.equalsIgnoreCase(Documentprefix + "_WIStatus"))
					{
						obsoleteDocument.getProperties().putObjectValue(documentPropertyName, "Obsolete");
						System.out.println("Obsolete Document Property :-" + obsoleteProperty.getPropertyName() + "Case vaule :-" + sourceDocumentiterator.getObjectValue(documentPropertyName));
						obsoleteDocument.save(RefreshMode.NO_REFRESH);
					}
					else if (documentPropertyName.equalsIgnoreCase(Documentprefix + "_DocumentStatus"))
					{
						obsoleteDocument.getProperties().putObjectValue(documentPropertyName, "Obsolete");
						System.out.println("Obsolete Document Property :-" + obsoleteProperty.getPropertyName() + "Case vaule :-" + sourceDocumentiterator.getObjectValue(documentPropertyName));
						obsoleteDocument.save(RefreshMode.NO_REFRESH);
					}
					else
					{
						obsoleteDocument.getProperties().putObjectValue(documentPropertyName, sourceDocumentiterator.getObjectValue(documentPropertyName));
						System.out.println("Obsolete Document Property :-" + obsoleteProperty.getPropertyName() + "Case vaule :-" + sourceDocumentiterator.getObjectValue(documentPropertyName));
						obsoleteDocument.save(RefreshMode.NO_REFRESH);
					}
				}
			}
			if (is != null) {
				try
				{
					is.close();
				}
				catch (IOException exIoException)
				{
					exIoException.printStackTrace();
				}
			}
			removeDirectory(folderPath);
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			System.out.println("Error in addReferenceOfDocument:" + exception.getMessage());
			return false;
		}
		return true;
	}

	private boolean removeDirectory(String tempFilePath)
	{
		boolean result = false;
		boolean isDeleted = false;
		File file = new File(tempFilePath);
		try
		{
			File[] listOfFile = file.listFiles();
			for (int i = 0; i < listOfFile.length; i++) {
				if (listOfFile[i].isDirectory())
				{
					removeDirectory(listOfFile[i].getAbsolutePath());
				}
				else
				{
					listOfFile[i].setExecutable(true);
					listOfFile[i].setReadable(true);
					listOfFile[i].setWritable(true);
					isDeleted = listOfFile[i].delete();
				}
			}
			result = file.delete();

			result = true;
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		return result;
	}

	private Folder retriveFolderFromCaseFolder(Folder caseFolder, String nameCaseSubFolder)
	{
		Folder folder = null;
		FolderSet caseSubFolderSet = caseFolder.get_SubFolders();
		Iterator<FolderSet> srcIterator = caseSubFolderSet.iterator();
		while (srcIterator.hasNext())
		{
			Folder srcFolder = (Folder)srcIterator.next();
			String folderName = srcFolder.get_Name();
			if (folderName.equalsIgnoreCase(nameCaseSubFolder)) {
				folder = srcFolder;
			}
		}
		return folder;
	}


	public static void main(String args[]){

		Connection conn = Factory.Connection.getConnection(CE_URI);
		Subject subject = UserContext.createSubject(conn, userName, password, jaasLoginModule);
		UserContext.get().pushSubject(subject);
		Domain domain = Factory.Domain.fetchInstance(conn, null, null);
		String objectStoreName="OS1";
		ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, null);
		Document document = Factory.Document.fetchInstance(objectStore, new Id("{70BB4E78-0000-C317-B997-E2532C3A4746}"),null);		
		String DocumentName=document.getProperties().getStringValue("DocumentTitle");
		document.getProperties().putValue("DocumentTitle", "TEST11");
		document.save(RefreshMode.REFRESH);
		System.out.println("Document Updated sucessfully");

	}

}

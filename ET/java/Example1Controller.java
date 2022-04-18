package com.cognizant.ecmservice.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/download")
public class Example1Controller {

	private static final String DIRECTORY = "C:/fileDownloadExample/";
	@Autowired
	private ServletContext servletContext;

	// http://localhost:8080/download1?fileName=abc.zip
	// Using ResponseEntity<InputStreamResource>
	@RequestMapping("/file/{fileName:.+}")
	public ResponseEntity downloadFile(@PathVariable("fileName") String fileName)
			throws IOException {
		try {
			MediaType mediaType = getMediaTypeForFileName(this.servletContext, fileName);
			System.out.println("fileName: " + fileName);
			System.out.println("mediaType: " + mediaType);
			File file = new File(DIRECTORY + "/" + fileName);
			InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
			System.out.println("File Doesnot exits.....");
			return ResponseEntity.ok()
					// Content-Disposition
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
					// Content-Type
					.contentType(mediaType)
					// Contet-Length
					.contentLength(file.length()) //
					.body(resource);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("EXEPTION:"+e.getMessage());
			e.printStackTrace(); // see note 2
			return ResponseEntity
					.status(HttpStatus.FORBIDDEN)
					.body(e.getMessage());			
		}
	}

	// abc.zip
	// abc.pdf,..
	public static MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
		// application/pdf
		// application/xml
		// image/gif, ...
		String mineType = servletContext.getMimeType(fileName);
		try {
			MediaType mediaType = MediaType.parseMediaType(mineType);
			return mediaType;
		} catch (Exception e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

}
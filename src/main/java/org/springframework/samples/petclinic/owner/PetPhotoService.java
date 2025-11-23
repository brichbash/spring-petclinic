/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.jspecify.annotations.Nullable;

/**
 * Service for handling pet photo storage and retrieval operations.
 *
 * @author Spring PetClinic Team
 */
@Service
public class PetPhotoService {

	private final Path photoStorageLocation;

	public PetPhotoService(@Value("${petclinic.photo.storage-path:./pet-photos}") String storagePath)
			throws IOException {
		this.photoStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
		Files.createDirectories(this.photoStorageLocation);
	}

	/**
	 * Stores a photo file and returns the generated file name.
	 * @param file the multipart file to store
	 * @return the generated file name
	 * @throws IOException if storage fails
	 */
	public String storePhoto(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Cannot store empty file");
		}

		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || originalFilename.contains("..")) {
			throw new IllegalArgumentException("Invalid file name: " + originalFilename);
		}

		String fileExtension = getFileExtension(originalFilename);
		String fileName = UUID.randomUUID() + fileExtension;
		Path targetLocation = this.photoStorageLocation.resolve(fileName);

		Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

		return fileName;
	}

	/**
	 * Loads a photo file as a Resource.
	 * @param fileName the name of the file to load
	 * @return the resource containing the photo
	 * @throws IOException if loading fails
	 */
	public Resource loadPhotoAsResource(String fileName) throws IOException {
		Path filePath = this.photoStorageLocation.resolve(fileName).normalize();
		Resource resource = new UrlResource(filePath.toUri());

		if (resource.exists() && resource.isReadable()) {
			return resource;
		}
		throw new IOException("Photo not found: " + fileName);
	}

	/**
	 * Deletes a photo file.
	 * @param fileName the name of the file to delete
	 * @throws IOException if deletion fails
	 */
	public void deletePhoto(String fileName) throws IOException {
		if (fileName != null && !fileName.isEmpty()) {
			Path filePath = this.photoStorageLocation.resolve(fileName).normalize();
			Files.deleteIfExists(filePath);
		}
	}

	private String getFileExtension(@Nullable String filename) {
		if (filename == null || filename.isEmpty()) {
			return "";
		}
		int lastDotIndex = filename.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return "";
		}
		return filename.substring(lastDotIndex);
	}

}

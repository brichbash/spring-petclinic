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
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * REST controller for managing pet photos.
 *
 * @author Spring PetClinic Team
 */
@RestController
@RequestMapping("/api/pets")
@Tag(name = "Pet Photos", description = "API for managing pet photos")
public class PetRestController {

	private final OwnerRepository ownerRepository;

	private final PetPhotoService petPhotoService;

	public PetRestController(OwnerRepository ownerRepository, PetPhotoService petPhotoService) {
		this.ownerRepository = ownerRepository;
		this.petPhotoService = petPhotoService;
	}

	/**
	 * Uploads a photo for a specific pet.
	 * @param petId the ID of the pet
	 * @param file the photo file to upload
	 * @return response entity with success or error message
	 */
	@PostMapping(value = "/{petId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a pet photo", description = "Uploads a photo for the specified pet")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Photo uploaded successfully",
					content = @Content(schema = @Schema(implementation = PhotoUploadResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid file or request",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Pet not found",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internal server error",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<?> uploadPetPhoto(
			@Parameter(description = "ID of the pet", required = true) @PathVariable("petId") int petId,
			@Parameter(description = "Photo file to upload",
					required = true) @RequestParam("file") MultipartFile file) {
		try {
			Pet pet = findPetById(petId);
			if (pet == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse("Pet not found with id: " + petId));
			}

			if (file.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("File is empty"));
			}

			String contentType = file.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("File must be an image"));
			}

			String oldPhotoPath = pet.getPhotoPath();
			if (oldPhotoPath != null) {
				try {
					petPhotoService.deletePhoto(oldPhotoPath);
				}
				catch (IOException e) {
				}
			}

			String fileName = petPhotoService.storePhoto(file);
			pet.setPhotoPath(fileName);
			saveOwnerWithPet(pet);

			return ResponseEntity.ok(new PhotoUploadResponse("Photo uploaded successfully", fileName));
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse(message != null ? message : "Invalid request"));
		}
		catch (IOException e) {
			String message = e.getMessage();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("Failed to upload photo" + (message != null ? ": " + message : "")));
		}
	}

	/**
	 * Retrieves the photo of a specific pet.
	 * @param petId the ID of the pet
	 * @return response entity with the photo resource
	 */
	@GetMapping("/{petId}/photo")
	@Operation(summary = "Get pet photo", description = "Retrieves the photo for the specified pet")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Photo retrieved successfully",
					content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)),
			@ApiResponse(responseCode = "404", description = "Pet or photo not found",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internal server error",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<?> getPetPhoto(
			@Parameter(description = "ID of the pet", required = true) @PathVariable("petId") int petId) {
		try {
			Pet pet = findPetById(petId);
			if (pet == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse("Pet not found with id: " + petId));
			}

			String photoPath = pet.getPhotoPath();
			if (photoPath == null || photoPath.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse("No photo available for this pet"));
			}

			Resource resource = petPhotoService.loadPhotoAsResource(photoPath);

			String contentType = determineContentType(photoPath);

			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
		}
		catch (IOException e) {
			String message = e.getMessage();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("Failed to load photo" + (message != null ? ": " + message : "")));
		}
	}

	/**
	 * Deletes the photo of a specific pet.
	 * @param petId the ID of the pet
	 * @return response entity with success or error message
	 */
	@DeleteMapping("/{petId}/photo")
	@Operation(summary = "Delete pet photo", description = "Deletes the photo for the specified pet")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Photo deleted successfully",
					content = @Content(schema = @Schema(implementation = MessageResponse.class))),
			@ApiResponse(responseCode = "404", description = "Pet not found",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "500", description = "Internal server error",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<?> deletePetPhoto(
			@Parameter(description = "ID of the pet", required = true) @PathVariable("petId") int petId) {
		try {
			Pet pet = findPetById(petId);
			if (pet == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponse("Pet not found with id: " + petId));
			}

			String photoPath = pet.getPhotoPath();
			if (photoPath != null) {
				petPhotoService.deletePhoto(photoPath);
				pet.setPhotoPath(null);
				saveOwnerWithPet(pet);
			}

			return ResponseEntity.ok(new MessageResponse("Photo deleted successfully"));
		}
		catch (IOException e) {
			String message = e.getMessage();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("Failed to delete photo" + (message != null ? ": " + message : "")));
		}
	}

	private @Nullable Pet findPetById(int petId) {
		return ownerRepository.findAll()
			.stream()
			.flatMap(owner -> owner.getPets().stream())
			.filter(pet -> pet.getId() != null && pet.getId().equals(petId))
			.findFirst()
			.orElse(null);
	}

	private void saveOwnerWithPet(Pet pet) {
		Optional<Owner> ownerOpt = ownerRepository.findAll()
			.stream()
			.filter(owner -> owner.getPets().stream().anyMatch(p -> p.getId() != null && p.getId().equals(pet.getId())))
			.findFirst();

		ownerOpt.ifPresent(ownerRepository::save);
	}

	private String determineContentType(String filename) {
		String lowerCaseFilename = filename.toLowerCase();
		if (lowerCaseFilename.endsWith(".png")) {
			return MediaType.IMAGE_PNG_VALUE;
		}
		else if (lowerCaseFilename.endsWith(".gif")) {
			return MediaType.IMAGE_GIF_VALUE;
		}
		else if (lowerCaseFilename.endsWith(".webp")) {
			return "image/webp";
		}
		return MediaType.IMAGE_JPEG_VALUE;
	}

	static class PhotoUploadResponse {

		private final String message;

		private final String fileName;

		PhotoUploadResponse(String message, String fileName) {
			this.message = message;
			this.fileName = fileName;
		}

		public String getMessage() {
			return message;
		}

		public String getFileName() {
			return fileName;
		}

	}

	static class MessageResponse {

		private final String message;

		MessageResponse(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

	}

	static class ErrorResponse {

		private final String error;

		ErrorResponse(String error) {
			this.error = error;
		}

		public String getError() {
			return error;
		}

	}

}

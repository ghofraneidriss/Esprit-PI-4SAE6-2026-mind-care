package tn.esprit.medical_report_service.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tn.esprit.medical_report_service.DTOs.FileDTO;
import tn.esprit.medical_report_service.Enteties.File;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper {
    @Mapping(target = "id", source = "fileid")
    @Mapping(target = "uploadedBy", source = "caregiverid")
    @Mapping(target = "uploadedAt", source = "createdAt")
    FileDTO toDTO(File file);

    @Mapping(target = "fileid", source = "id")
    @Mapping(target = "caregiverid", source = "uploadedBy")
    @Mapping(target = "createdAt", source = "uploadedAt")
    File toEntity(FileDTO fileDTO);
}

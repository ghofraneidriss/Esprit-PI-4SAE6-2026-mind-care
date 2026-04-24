package tn.esprit.medical_report_service.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import tn.esprit.medical_report_service.DTOs.FileDTO;
import tn.esprit.medical_report_service.Enteties.File;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper {
    FileDTO toDTO(File file);

    File toEntity(FileDTO fileDTO);
}
